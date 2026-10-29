"""Lookups against NSW Government map services, cached in data/cache/ so builds are repeatable.

- Suburb boundaries and named road segments: NSW Spatial Services (DCS), NSW Administrative
  Boundaries and NSW Transport themes.
- Speed limits: Transport for NSW speed zones (TFNSW_Speed_Zones_Public).
"""
import hashlib
import json
import math
import re
import time
import urllib.parse
import urllib.request

from shapely.geometry import LineString, MultiPoint, Point, Polygon, shape
from shapely.ops import linemerge, nearest_points, unary_union

from camlib import DATA

SPATIAL = "https://portal.spatial.nsw.gov.au/server/rest/services"
SUBURB_LAYER = f"{SPATIAL}/NSW_Administrative_Boundaries_Theme/MapServer/2/query"
ROAD_LAYER = f"{SPATIAL}/NSW_Transport_Theme/MapServer/5/query"
SPEED_LAYER = "https://portal.data.nsw.gov.au/arcgis/rest/services/Hosted/TFNSW_Speed_Zones_Public/FeatureServer/0/query"

CACHE_DIR = DATA / "cache"
ROAD_TYPES = {
    "street", "road", "highway", "avenue", "drive", "parade", "way", "motorway", "boulevard",
    "crescent", "lane", "place", "circuit", "close", "terrace", "esplanade", "parkway",
    "freeway", "expressway", "distributor", "tunnel", "grove", "square", "link",
}


def _get(url, params):
    """GET a JSON ArcGIS query, caching the response on disk."""
    key = hashlib.sha1((url + json.dumps(params, sort_keys=True)).encode()).hexdigest()
    path = CACHE_DIR / f"{key}.json"
    if path.exists():
        return json.loads(path.read_text())
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    full = url + "?" + urllib.parse.urlencode({**params, "f": "json"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(full, timeout=60) as resp:
                data = json.loads(resp.read())
            break
        except OSError:
            if attempt == 3:
                raise
            time.sleep(2 ** (attempt + 1))
    if "error" in data:
        raise RuntimeError(f"{url}: {data['error']}")
    path.write_text(json.dumps(data))
    time.sleep(0.2)
    return data


def _esri_to_shape(geom):
    if "rings" in geom:
        polys = [Polygon(r) for r in geom["rings"] if len(r) >= 4]
        return unary_union([p.buffer(0) for p in polys])
    if "paths" in geom:
        return unary_union([LineString(p) for p in geom["paths"] if len(p) >= 2])
    return None


def metres(a, b):
    """Approximate distance in metres between two (lon, lat) shapely points."""
    k = math.cos(math.radians((a.y + b.y) / 2))
    return math.hypot((a.x - b.x) * k, a.y - b.y) * 111_320


def suburb_area(location):
    """Union of the suburb polygons named in a location like 'Chullora/Greenacre'."""
    names = [n.strip() for n in re.split(r"[/,]|\band\b", location) if n.strip()]
    shapes = []
    for name in names:
        name = re.sub(r"\s+", " ", name).upper().replace("'", "''")
        data = _get(SUBURB_LAYER, {
            "where": f"upper(suburbname)='{name}'", "outFields": "suburbname",
            "returnGeometry": "true", "outSR": "4326",
        })
        shapes += [_esri_to_shape(f["geometry"]) for f in data.get("features", [])]
    shapes = [s for s in shapes if s is not None and not s.is_empty]
    return unary_union(shapes) if shapes else None


# Names used in the camera lists -> names in the NSW road network.
ROAD_ALIASES = {
    "M5 SOUTH WESTERN MOTORWAY": "SOUTH WESTERN MOTORWAY",
    "M4 WESTERN MOTORWAY": "WESTERN MOTORWAY",
    "M1 MOTORWAY": "PACIFIC MOTORWAY",
    "LIVERPOOL T-WAY": "LIVERPOOL-PARRAMATTA TRANSITWAY",
}


def _name_candidates(road):
    name = re.sub(r"\s+", " ", road.upper().strip(" ."))
    name = ROAD_ALIASES.get(name, name)
    # Route numbers ("M31 Hume Motorway") aren't part of the stored name, and some roads
    # are listed without the "The" they are stored with ("Bucketts Way").
    bare = re.sub(r"^[AMB]\d+\s+", "", name)
    variants = [name] + ([bare] if bare != name else [])
    variants += ["THE " + v for v in variants if not v.startswith("THE ")]
    for v in variants:
        words = v.split()
        if len(words) > 1 and words[-1].lower() in ROAD_TYPES:
            yield " ".join(words[:-1]), words[-1]
        # Some names are stored whole with no road type, e.g. "THE GRAND PARADE".
        yield v, None


def road_lines(road, area, margin_m=800):
    """Road segments named `road` within `margin_m` of `area`."""
    minx, miny, maxx, maxy = area.buffer(margin_m / 111_320).bounds
    for base, kind in _name_candidates(road):
        base = base.replace("'", "''")
        where = f"roadnamebase='{base}'" + (f" AND roadnametype='{kind}'" if kind else "")
        data = _get(ROAD_LAYER, {
            "where": where, "geometry": f"{minx},{miny},{maxx},{maxy}",
            "geometryType": "esriGeometryEnvelope", "inSR": "4326", "outSR": "4326",
            "spatialRel": "esriSpatialRelIntersects", "outFields": "roadnamebase,roadnametype",
            "returnGeometry": "true",
        })
        lines = [_esri_to_shape(f["geometry"]) for f in data.get("features", [])]
        lines = [l for l in lines if l is not None and not l.is_empty]
        if lines:
            return unary_union(lines)
    return None


def crossings(a, b, area, cluster_m=250):
    """All places roads a and b meet near `area`, merging points within cluster_m."""
    if a is None or b is None:
        return []
    hit = a.intersection(b)
    near = area.buffer(300 / 111_320)
    if hit.is_empty:
        # Side streets are sometimes drawn stopping just short of the main road.
        pa, pb = nearest_points(a, b)
        mid = Point((pa.x + pb.x) / 2, (pa.y + pb.y) / 2)
        return [mid] if metres(pa, pb) <= 40 and near.contains(mid) else []
    pts = [g if isinstance(g, Point) else g.centroid for g in getattr(hit, "geoms", [hit])]
    clusters = []
    for p in (p for p in pts if near.contains(p)):
        for c in clusters:
            if metres(p, c[0]) <= cluster_m:
                c.append(p)
                break
        else:
            clusters.append([p])
    return [MultiPoint(c).centroid for c in clusters]


def intersection(a, b, area, max_gap_m=40, max_spread_m=250):
    """Where roads a and b meet near `area`, or None if that is missing or ambiguous."""
    if a is None or b is None:
        return None
    hit = a.intersection(b)
    pts = []
    if not hit.is_empty:
        geoms = getattr(hit, "geoms", [hit])
        pts = [g.centroid if not isinstance(g, Point) else g for g in geoms]
    if not pts:
        pa, pb = nearest_points(a, b)
        if metres(pa, pb) > max_gap_m:
            return None
        pts = [Point((pa.x + pb.x) / 2, (pa.y + pb.y) / 2)]
    # Keep the cluster of crossing points nearest the suburb (dual carriageways cross twice).
    near = area.buffer(300 / 111_320)
    inside = [p for p in pts if near.contains(p)] or pts
    centre = MultiPoint(inside).centroid
    if max(metres(p, centre) for p in inside) > max_spread_m:
        return None
    if not near.contains(centre):
        return None
    return centre


def midpoint_between(main, p1, p2):
    """Midpoint along road `main` between points p1 and p2."""
    merged = linemerge(main) if main.geom_type == "MultiLineString" else main
    for line in getattr(merged, "geoms", [merged]):
        if line.distance(p1) < 30 / 111_320 and line.distance(p2) < 30 / 111_320:
            d1, d2 = line.project(p1), line.project(p2)
            return line.interpolate((d1 + d2) / 2)
    return Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)


def speed_limit(lat, lon, radius_m=25):
    """Highest posted (non-school-zone) speed limit within radius_m of the point."""
    data = _get(SPEED_LAYER, {
        "geometry": f"{lon},{lat}", "geometryType": "esriGeometryPoint", "inSR": "4326",
        "spatialRel": "esriSpatialRelIntersects", "distance": radius_m,
        "units": "esriSRUnit_Meter", "outFields": "type,status,speed", "returnGeometry": "false",
    })
    speeds = []
    for f in data.get("features", []):
        a = f["attributes"]
        if "school" in (a.get("type") or "").lower():
            continue
        m = re.match(r"(\d+)", str(a.get("speed") or ""))  # e.g. "60 km/h"
        if m:
            speeds.append(int(m.group(1)))
    return max(speeds) if speeds else None
