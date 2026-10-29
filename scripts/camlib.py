"""Shared helpers: loading the camera lists and matching them to the 2021 GPS data."""
import csv
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "data"

ABBREV = {
    "rd": "road", "st": "street", "ave": "avenue", "av": "avenue", "hwy": "highway",
    "pde": "parade", "dr": "drive", "cres": "crescent", "pl": "place", "ln": "lane",
    "pkwy": "parkway", "blvd": "boulevard", "bvd": "boulevard", "cct": "circuit",
    "mt": "mount", "nth": "north", "sth": "south", "steet": "street", "fwy": "freeway",
    "hway": "highway", "esp": "esplanade", "tce": "terrace", "cl": "close",
    "fredrick": "frederick", "boulevarde": "boulevard",
}

# Road names that changed between the 2021 GPS file and the current lists.
RENAMED = {"new narellan road": "narellan road"}


def norm(text):
    text = text.lower().replace("&", " and ").replace("’", "'")
    text = re.sub(r"\(.*?\)", " ", text)
    text = re.sub(r"[^a-z0-9' ]", " ", text).replace("'", "")
    words = [ABBREV.get(w, w) for w in text.split()]
    text = " ".join(w for w in words if w != "the")
    return RENAMED.get(text, text)


def suburbs(text):
    return {norm(s) for s in re.split(r"[/,]| and ", text) if norm(s)}


def split_roads(text):
    """'A and B' -> {A, B}; 'A, between B and C' -> (A, {B, C})."""
    m = re.match(r"(.*?),?\s+between\s+(.*)", text, re.I)
    if m:
        main = norm(m.group(1))
        others = {norm(x) for x in re.split(r"\s+and\s+", m.group(2)) if norm(x)}
        return main, others
    parts = [norm(x) for x in re.split(r"\s+and\s+|\s+at\s+|\s*/\s*", text) if norm(x)]
    return None, set(parts)


def road_key(text):
    main, others = split_roads(text)
    return (main, frozenset(others))


def load_2021(kind):
    name = "fixed-speed-cameras-2021.csv" if kind == "fixed" else "red-light-speed-cameras-2021.csv"
    rows = []
    with open(DATA / "opendata" / name, newline="") as f:
        for r in csv.DictReader(f):
            pts = []
            for j in (1, 2, 3):
                lat, lon = (r.get(f"Lat({j})") or "").strip(), (r.get(f"Long({j})") or "").strip()
                if lat and lon:
                    pts.append((float(lat), float(lon)))
            rows.append({
                "suburb": r["SUBURB/TOWN"].strip(),
                "road": r["ROAD/S"].strip(),
                "school_zone": r["SZ?"].strip().upper() == "Y",
                "points": pts,
            })
    return rows


def match_2021(suburb, road, candidates):
    """Return the 2021 rows that describe the same site, best first."""
    key = road_key(road)
    subs = suburbs(suburb)
    exact = [c for c in candidates if road_key(c["road"]) == key]
    same_place = [c for c in exact if suburbs(c["suburb"]) & subs]
    if same_place:
        return same_place
    if len(exact) == 1:
        return exact
    # Same roads in the same suburb, but the 2021 file used different cross streets
    # or a looser description, e.g. "Hume Highway and Fredrick Street".
    main, others = key
    loose = []
    for c in candidates:
        if not suburbs(c["suburb"]) & subs:
            continue
        cmain, cothers = road_key(c["road"])
        if main is None and cmain is None and len(others & cothers) >= 2:
            loose.append(c)
        elif main and main == cmain and others & cothers:
            loose.append(c)
        elif main and cmain and others and others == cothers:
            # Same cross streets, renamed main road (e.g. the old Pacific Highway
            # through Macksville is now Giinagay Way).
            loose.append(c)
    return loose
