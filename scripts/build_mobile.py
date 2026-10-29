"""Build a Highway Radar (ExCam) file of NSW mobile speed camera streets.

TfNSW publishes mobile camera locations as streets within suburbs, not points. A mobile
camera can be anywhere on the listed street, so each street gets a point every SPACING_M
along its length inside the listed suburbs (NSW Spatial Services roads and boundaries).

Outputs:
  output/nsw_mobile_speed_camera_streets.excam
  output/nsw_mobile_speed_camera_streets.csv
  output/mobile_not_included.csv
"""
import csv
import datetime
import json
import lzma
import math
import re
from concurrent.futures import ThreadPoolExecutor

from shapely.geometry import LineString
from shapely.ops import linemerge

import nswgeo
from camlib import DATA, ROOT

OUT = ROOT / "output"
SPACING_M = 1000
DEDUPE_M = 300  # drop a point this close to one already placed
MIN_PART_M = 50  # ignore slivers of road clipped at a suburb edge
WORKERS = 4


def line_metres(line):
    k = math.cos(math.radians(line.centroid.y))
    xs, ys = zip(*line.coords)
    return sum(math.hypot((xs[i + 1] - xs[i]) * k, ys[i + 1] - ys[i]) for i in range(len(xs) - 1)) * 111_320


def sample(line):
    """Points every SPACING_M along a line, or its midpoint if it is shorter."""
    length = line_metres(line)
    if length < MIN_PART_M:
        return []
    n = max(1, round(length / SPACING_M))
    return [line.interpolate((i + 0.5) / n, normalized=True) for i in range(n)]


def place(entry):
    suburbs, street = entry
    area = nswgeo.suburb_area(suburbs.replace(",", "/"))
    if area is None:
        return entry, [], "suburbs not found in NSW suburb boundaries"
    road = nswgeo.road_lines(street, area)
    if road is None:
        return entry, [], "street not found in NSW road network"
    road = road.intersection(area.buffer(150 / 111_320))
    merged = linemerge(road) if road.geom_type == "MultiLineString" else road
    parts = [g for g in getattr(merged, "geoms", [merged]) if isinstance(g, LineString)]
    points = [p for part in parts for p in sample(part)]
    return entry, points, None if points else "street not inside the listed suburbs"


def main():
    entries = []
    for r in csv.DictReader(open(DATA / "tfnsw" / "mobile_speed_camera_locations.csv")):
        for street in r["streets"].split(","):
            if street.strip():
                entries.append((r["suburbs"], re.sub(r"\s+", " ", street.strip())))

    with ThreadPoolExecutor(WORKERS) as pool:
        results = list(pool.map(place, entries))

    grid, placed, skipped = {}, [], []
    cell = DEDUPE_M / 111_320
    for (suburbs, street), points, reason in results:
        if reason:
            skipped.append({"suburbs": suburbs, "street": street, "reason": reason})
            continue
        for p in points:
            gx, gy = int(p.x // cell), int(p.y // cell)
            near = [q for dx in (-1, 0, 1) for dy in (-1, 0, 1) for q in grid.get((gx + dx, gy + dy), [])]
            if any(nswgeo.metres(p, q) < DEDUPE_M for q in near):
                continue
            grid.setdefault((gx, gy), []).append(p)
            placed.append({"suburbs": suburbs, "street": street,
                           "lat": round(p.y, 6), "lon": round(p.x, 6)})

    today = datetime.date.today().isoformat()
    with lzma.open(OUT / "nsw_mobile_speed_camera_streets.excam", "wt", encoding="utf-8") as f:
        f.write(json.dumps({"_meta": {"name": "NSW mobile speed camera streets (Transport for NSW list)",
                                      "date": today}}) + "\n")
        for p in placed:
            f.write(json.dumps({"lat": p["lat"], "lon": p["lon"], "flg": 1,
                                "str": f"Possible mobile speed camera: {p['street']}, {p['suburbs']}"}) + "\n")
    with open(OUT / "nsw_mobile_speed_camera_streets.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["suburbs", "street", "lat", "lon"])
        w.writeheader()
        w.writerows(placed)
    with open(OUT / "mobile_not_included.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["suburbs", "street", "reason"])
        w.writeheader()
        w.writerows(skipped)
    print(f"{len(entries)} street entries: {len(placed)} points written, {len(skipped)} entries not placed")


if __name__ == "__main__":
    main()
