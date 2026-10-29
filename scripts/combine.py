"""Merge the camera and mobile-street ExCam files into one file for Highway Radar.

Mobile-street points within NEAR_M of a camera are dropped so the app doesn't alert twice.
Output: output/nsw_all_cameras.excam
"""
import datetime
import json
import lzma

from shapely.geometry import Point

import nswgeo
from camlib import ROOT

OUT = ROOT / "output"
NEAR_M = 150


def read(name):
    with lzma.open(OUT / name, "rt", encoding="utf-8") as f:
        lines = [json.loads(line) for line in f]
    return lines[1:]


def main():
    cameras = read("nsw_speed_cameras.excam")
    mobile = read("nsw_mobile_speed_camera_streets.excam")

    cell = NEAR_M / 111_320
    grid = {}
    for c in cameras:
        grid.setdefault((int(c["lon"] // cell), int(c["lat"] // cell)), []).append(Point(c["lon"], c["lat"]))
    kept = []
    for m in mobile:
        p = Point(m["lon"], m["lat"])
        gx, gy = int(p.x // cell), int(p.y // cell)
        near = [q for dx in (-1, 0, 1) for dy in (-1, 0, 1) for q in grid.get((gx + dx, gy + dy), [])]
        if not any(nswgeo.metres(p, q) < NEAR_M for q in near):
            kept.append(m)

    meta = {"_meta": {"name": "NSW speed cameras incl. mobile camera streets (Transport for NSW, OpenStreetMap)",
                      "date": datetime.date.today().isoformat()}}
    with lzma.open(OUT / "nsw_all_cameras.excam", "wt", encoding="utf-8") as f:
        for line in [meta] + cameras + kept:
            f.write(json.dumps(line) + "\n")
    print(f"{len(cameras)} cameras + {len(kept)} mobile street points "
          f"({len(mobile) - len(kept)} dropped near cameras) -> nsw_all_cameras.excam")


if __name__ == "__main__":
    main()
