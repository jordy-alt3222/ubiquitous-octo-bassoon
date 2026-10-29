# NSW speed cameras for Highway Radar

A camera database of New South Wales fixed speed cameras and red-light speed cameras for the
Android app [Highway Radar](https://highwayradar.com), in its
[ExCam format](https://highwayradar.com/excam).

**File to import:** [`output/nsw_speed_cameras.excam`](output/nsw_speed_cameras.excam)

Copy it to your phone and import it in Highway Radar as a custom camera database.

## What's in it

The database has 403 camera points. They cover 355 of the 365 fixed and red-light sites that Transport for NSW (TfNSW) lists as active, plus one average-speed section.

| Type | Points | ExCam `flg` |
|---|---|---|
| Fixed speed camera (incl. 38 school-zone sites) | 122 | 1 (speed) |
| Red-light speed camera | 277 | 3 (speed + red light) |
| Average-speed section Kew–Port Macquarie (Pacific Hwy), both directions | 4 | 4 (section start) / 8 (section end) |

Each point has:
- **Location:** `lat` and `lon`.
- **Speed limit:** `spd`, the posted limit from TfNSW speed zone data at that point.
- **Description:** `str`, the road and suburb. School-zone cameras are labelled
  "(school zone, 40 km/h school days)", because ExCam has no school-zone flag and `spd` holds the normal limit.

Direction (`dir`) is set only where OpenStreetMap records it: the average-speed cameras and three red-light cameras. Everywhere
else it is left out, so the app alerts in both directions.

### Where the positions come from

| Source | Points |
|---|---|
| TfNSW Open Data GPS locations (2021), matched to the current lists | 330 |
| Intersection of the two named roads in the NSW Spatial Services road network (red-light sites added since 2021) | 59 |
| Midpoint of the "between A and B" segment in the NSW road network (fixed sites added or renamed since 2021) | 5 |
| OpenStreetMap camera nodes or crossings (`data/osm/`): 5 red-light sites the road network couldn't place, plus the average-speed cameras | 9 |

[`output/nsw_speed_cameras.csv`](output/nsw_speed_cameras.csv) lists every point with its source. The 64
road-network points were checked against both named roads (all within 13 m) and against
OpenStreetMap reverse geocoding.

### Not included

- **10 active sites with no position** ([`output/not_included.csv`](output/not_included.csv)). Nine are tunnel cameras:
  M5, M4 East, M8, M4-M8 Link, Rozelle Interchange, NorthConnex, Lane Cove, Sydney Harbour and Cross City. No source places
  them; OpenStreetMap maps the tunnels only as whole enforcement sections, and a point inside a long tunnel would be a
  guess. The tenth is Pacific Highway at Cowan, whose segment starts at an underpass rather than a street.
- **Other average-speed sections.** TfNSW publishes the sections (`sources/crs_average_speed_cameras_map.pdf`) but
  not where the cameras are. Of the two sections that fine cars, only Kew–Port Macquarie has both ends mapped in
  OpenStreetMap. Coolac–Gundagai is missing its southbound Gundagai camera. The other 30 sections fine heavy
  vehicles only.
- **Live Traffic NSW cameras** (livetraffic.com/traffic-cameras). These are traffic-view webcams, not enforcement
  cameras.
- **Mobile speed cameras.** They are published as street names only, with no positions.
- **Cameras TfNSW marks as removed or decommissioned.**

## Sources

- **Current location lists** (`sources/`), downloaded from transport.nsw.gov.au:
  - Fixed speed cameras, Dec 2025.
  - Red-light speed cameras, Sep 2026.
  - Fixed speed cameras – school zones, Jun 2023.
  - Average speed cameras, Oct 2025.
  - Mobile speed cameras, Sep 2026.
- **[TfNSW Open Data – NSW Speed Cameras](https://opendata.transport.nsw.gov.au/data/dataset/nsw-speed-cameras)**
  (`data/opendata/`): GPS locations, 2021.
- **NSW Spatial Services** suburb boundaries and road network (`portal.spatial.nsw.gov.au`).
- **TfNSW speed zones** (`TFNSW_Speed_Zones_Public` on portal.data.nsw.gov.au).
- **[OpenStreetMap](https://www.openstreetmap.org)** camera nodes and roads (`data/osm/`, with node IDs).

Contains data from Transport for NSW and NSW Spatial Services, licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Also contains data © OpenStreetMap contributors, available
under the [ODbL](https://opendatacommons.org/licenses/odbl/). If you share the file, keep these credits. Camera locations change; this file is
a guide only, so obey the posted speed limit.

## Rebuilding

When TfNSW publishes new PDFs, replace the files in `sources/` (keep the file names) and run:

```sh
pip install -r requirements.txt
python3 scripts/extract_pdfs.py   # sources/*.pdf -> data/tfnsw/*.csv
python3 scripts/build.py          # -> output/
```

The first run takes about 10 minutes. Map service responses are cached in `data/cache/`.
