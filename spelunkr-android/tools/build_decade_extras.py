#!/usr/bin/env python3
"""Fills SPELUNKR's thin decades with tapes from archive.org's own collections.

    python3 tools/build_decade_extras.py app/src/spelunkr/assets/decade-extras.json

Spelunkr's lineup has few tapes from the early decades (on 1 Nov 2026: 3 from the 1900s-1910s, 43 from
the 1920s, 121 from the 1970s). This picks up to PER_DECADE playable tapes per decade from archive.org
collections that date their items by when they were made: silent and feature films, the Prelinger
archive, classic TV and old commercials. They only go into the decade channels (THE 20s, THE 70s...),
never into Spelunkr's own channels. The 2010s and 2020s are not filled: in these collections those
"years" are mostly upload dates, not air dates.

Every tape: not access-restricted, one MP4 of 1 minute to 3 hours, and from the decade, by the year in
its title when there is one, else by its year field. Its MP4 index is located for the app's cache.
"""
import json, re, struct, sys, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

PER_DECADE = 150
BASE = "mediatype:movies AND NOT access-restricted-item:true AND NOT collection:(tvnews OR tvarchive)"
SOURCES = {
    1900: "silent_films OR silentfilms OR feature_films OR prelinger",
    1910: "silent_films OR silentfilms OR feature_films OR prelinger",
    1920: "silent_films OR silentfilms OR feature_films OR prelinger OR classic_cartoons",
    1930: "feature_films OR prelinger OR classic_cartoons OR animationandcartoons",
    1940: "feature_films OR prelinger OR classic_tv OR classic_cartoons",
    1950: "classic_tv OR prelinger OR feature_films OR classic_tv_commercials",
    1960: "classic_tv OR prelinger OR feature_films OR classic_tv_commercials",
    1970: "classic_tv OR classic_tv_commercials OR prelinger OR feature_films OR vhsvault",
}
TITLE_YEAR = re.compile(r"\([^()]*?(?:\b(1[89]\d\d|20\d\d)\b|'(\d\d))[^()]*\)")
YEAR = re.compile(r"\b(1[89]\d\d|20\d\d)\b")
SKIP = re.compile(r"\btrailer\b|\bteaser\b|\bslideshow\b", re.I)


def get_json(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


def first(v):
    return v[0] if isinstance(v, list) else v


def seconds(length):
    if length in (None, ""):
        return None
    total = 0.0
    for part in str(length).split(":"):
        total = total * 60 + float(part)
    return total


def aired(title, year_field):
    m = TITLE_YEAR.search(title)
    if m:
        return int(m.group(1)) if m.group(1) else (1900 + int(m.group(2)) if int(m.group(2)) >= 30 else 2000 + int(m.group(2)))
    m = YEAR.search(str(year_field or ""))
    return int(m.group(1)) if m else None


def index_of(url, size):
    position = 0
    for _ in range(8):
        if position >= size:
            return None
        req = urllib.request.Request(url, headers={"Range": f"bytes={position}-{position + 15}"})
        with urllib.request.urlopen(req, timeout=60) as r:
            head = r.read(16)
        if len(head) < 8:
            return None
        atom, kind = struct.unpack(">I4s", head[:8])
        if atom == 1:
            atom = struct.unpack(">Q", head[8:16])[0]
        if kind == b"moov":
            return position, atom
        if atom < 8:
            return None
        position += atom
    return None


def tape(decade, identifier):
    try:
        m = get_json(f"https://archive.org/metadata/{urllib.parse.quote(identifier)}")
        md = m.get("metadata", {})
        if md.get("access-restricted-item") in (True, "true") or m.get("is_dark"):
            return None
        title = str(first(md.get("title")) or identifier)
        year = aired(title, first(md.get("year")) or first(md.get("date")))
        if year is None or year // 10 * 10 != decade or SKIP.search(title):
            return None
        best = None
        for f in m.get("files", []):
            if not f["name"].lower().endswith(".mp4"):
                continue
            length = seconds(f.get("length"))
            if not length or not 60 <= length <= 3 * 3600:
                continue
            rank = (f.get("format", "").startswith("h.264"), int(f.get("size", 0)))
            if best is None or rank > best[0]:
                best = (rank, f, length)
        if not best:
            return None
        _, f, length = best
        index = index_of(f"https://{m['d1']}{m['dir']}/{urllib.parse.quote(f['name'])}", int(f.get("size", 0)))
        description = re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", str(first(md.get("description")) or ""))).strip()[:300]
        collection = md.get("collection")
        return {
            "id": identifier, "file": f["name"], "duration": int(length), "title": title[:120], "year": str(year),
            "moov": index[1] if index else None, "description": description or None,
            "collection": first(collection) if collection else None,
        }
    except Exception as e:
        print(f"  skip {identifier}: {e}", file=sys.stderr)
        return None


def main(out):
    decades, seen = {}, set()
    with ThreadPoolExecutor(8) as pool:
        for decade, cols in SOURCES.items():
            url = "https://archive.org/advancedsearch.php?" + urllib.parse.urlencode(
                {"q": f"{BASE} AND collection:({cols}) AND year:[{decade} TO {decade + 9}]", "rows": PER_DECADE * 3,
                 "fl[]": ["identifier"], "sort[]": "downloads desc", "output": "json"}, doseq=True)
            ids = [d["identifier"] for d in get_json(url)["response"]["docs"] if d["identifier"] not in seen]
            tapes = [t for t in pool.map(lambda i: tape(decade, i), ids) if t][:PER_DECADE]
            seen.update(t["id"] for t in tapes)
            decades[str(decade)] = tapes
            print(f"{decade}s: {len(tapes)} tapes, {sum(t['duration'] for t in tapes) / 3600:.0f} h")
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"decades": decades}, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "decade-extras.json")
