"""Build a Highway Radar (ExCam) camera database for NSW fixed and red-light speed cameras.

Inputs (see README.md):
  data/tfnsw/*.csv      current TfNSW location lists, extracted from sources/*.pdf
  data/opendata/*.csv   TfNSW Open Data GPS locations (2021)
Outputs:
  output/nsw_speed_cameras.excam   the file to import into Highway Radar
  output/nsw_speed_cameras.csv     every camera point, with how its position was found
  output/not_included.csv          current sites that could not be placed, and why
"""
import csv
import datetime
import json
import lzma

from shapely.geometry import Point

import nswgeo
from camlib import DATA, ROOT, load_2021, match_2021

OUT = ROOT / "output"
FLAG_SPEED = 1
FLAG_RED_LIGHT = 2
MAX_OFFSET_M = 3000  # a 2021 point this far outside its suburb is treated as an error
MAX_SEGMENT_M = 3000  # longest "between A and B" segment we will place a midpoint on


def current_sites():
    for r in csv.DictReader(open(DATA / "tfnsw" / "fixed_speed_cameras.csv")):
        yield dict(kind="fixed", suburb=r["location"], road=r["road"], comment=r["comment"],
                   removed=r["removed"] == "1", school_zone=False)
    for r in csv.DictReader(open(DATA / "tfnsw" / "fixed_speed_cameras_school_zones.csv")):
        yield dict(kind="fixed", suburb=r["location"], road=r["road"], comment=r["comment"],
                   removed=r["removed"] == "1", school_zone=True)
    for r in csv.DictReader(open(DATA / "tfnsw" / "red_light_speed_cameras.csv")):
        yield dict(kind="red", suburb=r["location"], road=r["roads"], comment=r["comment"],
                   removed=r["removed"] == "1", school_zone=r["school_zone"] == "1")


def original_names(text):
    """Road names as written ('Hume Highway, between A and B' -> 'Hume Highway', ['A', 'B'])."""
    import re
    m = re.match(r"(.*?),?\s+between\s+(.*?)\.?$", text, re.I)
    if m:
        return m.group(1).strip(), [x.strip() for x in re.split(r"\s+and\s+", m.group(2))]
    parts = [x.strip() for x in re.split(r"\s+and\s+|\s+at\s+|\s*/\s*", text) if x.strip()]
    return None, [re.sub(r"^the\s+", "", p, flags=re.I) for p in parts]


def locate(site, area):
    """Position a site from the NSW road network; returns (Point, note) or (None, reason)."""
    if area is None:
        return None, "suburb not found in NSW suburb boundaries"
    main, others = original_names(site["road"])
    if main is None:
        if len(others) == 3 and " at " in site["road"]:
            # "Hume Highway at Sherman Street / Shellcote Road": the cross streets are
            # opposite legs of one intersection, so the first one is enough.
            others = others[:2]
        if len(others) != 2:
            return None, "not a two-road intersection"
        a, b = (nswgeo.road_lines(n, area) for n in others)
        if a is None or b is None:
            return None, "road name not found in NSW road network"
        p = nswgeo.intersection(a, b, area)
        return (p, "intersection from NSW road network") if p else (None, "roads do not meet clearly")
    if len(others) != 2:
        return None, "no cross streets given"
    road = nswgeo.road_lines(main, area)
    if road is None:
        return None, "road name not found in NSW road network"
    # A cross street can meet the main road more than once (loops, service roads), so
    # take the pair of crossings closest to each other.
    options = [nswgeo.crossings(road, nswgeo.road_lines(n, area), area) for n in others]
    pairs = [(nswgeo.metres(p1, p2), p1, p2) for p1 in options[0] for p2 in options[1]]
    if not pairs:
        return None, "segment ends not found in NSW road network"
    length, p1, p2 = min(pairs, key=lambda t: t[0])
    if length > MAX_SEGMENT_M:
        return None, "segment ends too far apart"
    return nswgeo.midpoint_between(road, p1, p2), "midpoint of segment from NSW road network"


def main():
    OUT.mkdir(exist_ok=True)
    gps = {"fixed": load_2021("fixed"), "red": load_2021("red")}
    points, skipped = [], []
    for site in current_sites():
        if site["removed"]:
            continue
        area = nswgeo.suburb_area(site["suburb"])
        found = []
        for row in match_2021(site["suburb"], site["road"], gps[site["kind"]]):
            for lat, lon in row["points"]:
                p = Point(lon, lat)
                if area is not None and area.distance(p) * 111_320 > MAX_OFFSET_M:
                    continue
                found.append((lat, lon, "TfNSW Open Data GPS (2021)"))
            if found:
                break
        if not found:
            p, note = locate(site, area)
            if p is None:
                skipped.append({**site, "reason": note})
                continue
            found = [(round(p.y, 6), round(p.x, 6), note)]
        for lat, lon, source in found:
            points.append({**site, "lat": lat, "lon": lon, "source": source,
                           "speed_limit": nswgeo.speed_limit(lat, lon)})

    today = datetime.date.today().isoformat()
    with lzma.open(OUT / "nsw_speed_cameras.excam", "wt", encoding="utf-8") as f:
        f.write(json.dumps({"_meta": {"name": "NSW speed cameras (Transport for NSW data)", "date": today}}) + "\n")
        for p in points:
            label = f"{p['road']}, {p['suburb']}"
            if p["school_zone"]:
                label += " (school zone, 40 km/h school days)"
            cam = {"lat": p["lat"], "lon": p["lon"],
                   "flg": FLAG_SPEED | (FLAG_RED_LIGHT if p["kind"] == "red" else 0), "str": label}
            if p["speed_limit"]:
                cam["spd"] = p["speed_limit"]
            f.write(json.dumps(cam) + "\n")

    fields = ["kind", "suburb", "road", "school_zone", "lat", "lon", "speed_limit", "source", "comment"]
    with open(OUT / "nsw_speed_cameras.csv", "w", newline="") as f:
        w = csv.DictWriter(f, fields, extrasaction="ignore")
        w.writeheader()
        w.writerows(points)
    with open(OUT / "not_included.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["kind", "suburb", "road", "comment", "reason"], extrasaction="ignore")
        w.writeheader()
        w.writerows(skipped)

    by_source = {}
    for p in points:
        by_source[p["source"]] = by_source.get(p["source"], 0) + 1
    print(f"{len(points)} camera points written; {len(skipped)} sites not placed")
    for k, v in sorted(by_source.items()):
        print(f"  {v:4d}  {k}")


if __name__ == "__main__":
    main()
