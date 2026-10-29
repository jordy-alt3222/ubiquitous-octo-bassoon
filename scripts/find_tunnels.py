"""Approximate positions for tunnel speed cameras, from OpenStreetMap tunnel geometry.

TfNSW does not publish where cameras sit inside tunnels, so each tunnel gets one point at
the middle of its OpenStreetMap ways, searched (via Nominatim) within the area spanned by
the suburbs TfNSW lists. Prints CSV rows for data/osm/site_positions.csv.
"""
import csv
import json
import sys
import time
import urllib.parse
import urllib.request

from shapely.geometry import shape
from shapely.ops import nearest_points, unary_union

import nswgeo

# (TfNSW location, TfNSW road, OSM names to search, suburbs that bound the search)
TUNNELS = [
    ("Bardwell Park / Arncliffe", "M5 Tunnel, between Bexley Road and Marsh Street", ["M5 East Tunnel"], "Bardwell Park / Arncliffe"),
    ("Haberfield / St Peters", "WestConnex M4-M8 Link Tunnel", ["M4-M8 Link", "M8 Motorway Tunnel"], "Haberfield / St Peters"),
    ("Homebush / Haberfield", "WestConnex M4 East Tunnel", ["M4 East Tunnel"], "Homebush / Haberfield"),
    ("Kingsgrove / St Peters", "WestConnex M8 Tunnel", ["M8 Motorway Tunnel", "M8 Motorway"], "Kingsgrove / St Peters"),
    ("Lane Cove Tunnel", "Lane Cove Tunnel, between Mowbray Road and the Pacific Highway", ["Lane Cove Tunnel"], "Lane Cove North / Artarmon"),
    ("Normanhurst/Pennant Hills", "NorthConnex Tunnel", ["NorthConnex"], "Normanhurst / Pennant Hills"),
    ("Rozelle / Annandale / Leichhardt", "Rozelle Interchange", ["Rozelle Interchange"], "Rozelle / Annandale / Leichhardt"),
    ("Sydney", "Sydney Harbour Tunnel, between Cahill Expressway and Warringah Freeway", ["Sydney Harbour Tunnel"], "Sydney / North Sydney"),
    ("Woolloomooloo / East Sydney", "Cross City Tunnel, between McLachlan Avenue and Harbour Street", ["Cross City Tunnel"], "Sydney / Woolloomooloo / Darlinghurst"),
]


def nominatim(query, box):
    minx, miny, maxx, maxy = box
    url = "https://nominatim.openstreetmap.org/search?" + urllib.parse.urlencode({
        "q": query, "format": "jsonv2", "limit": 40, "polygon_geojson": 1,
        "viewbox": f"{minx},{maxy},{maxx},{miny}", "bounded": 1,
    })
    req = urllib.request.Request(url, headers={"User-Agent": "nsw-camera-db/1.0 (one-off lookup)"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read())
    time.sleep(1.1)
    return data


def main():
    w = csv.writer(sys.stdout)
    for location, road, names, bound in TUNNELS:
        area = nswgeo.suburb_area(bound)
        box = area.buffer(500 / 111_320).bounds
        geoms, used = [], None
        for name in names:
            hits = [h for h in nominatim(name, box) if h.get("geojson")]
            geoms = [shape(h["geojson"]) for h in hits if name.lower() in h["display_name"].lower()]
            if geoms:
                used = name
                break
        if not geoms:
            print(f"# no OSM match for {location}", file=sys.stderr)
            continue
        merged = unary_union(geoms)
        point = nearest_points(merged, merged.centroid)[0]
        w.writerow(["fixed", location, road, round(point.y, 6), round(point.x, 6), "", "",
                    f"approximate: middle of OSM '{used}' ({len(geoms)} ways)"])


if __name__ == "__main__":
    main()
