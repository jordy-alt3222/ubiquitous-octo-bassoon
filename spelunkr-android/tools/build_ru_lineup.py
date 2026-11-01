#!/usr/bin/env python3
"""Builds ru-splunker's lineup: USSR-era channels (up to 1991) from playable archive.org video.

    python3 tools/build_ru_lineup.py app/src/ru/assets/ru-lineup.json
    python3 tools/build_ru_lineup.py app/src/ru/assets/ru-lineup.json --only=goluboy-ekran   # one channel

For each channel it searches archive.org, keeps only items that are not access-restricted, have
an MP4 between 2 minutes and 6 hours, look Russian (language, Cyrillic title or Soviet subject)
and date from 1991 or earlier (or, undated, say they are Soviet). It records each tape's length and the size
and place of its MP4 index, then shuffles each channel with a fixed seed. Every copy of the app
reads the same file and the same epoch, so every viewer sees the same tape at the same second.
"""
import json, random, re, struct, sys, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

EPOCH_MS = 1793491200000  # 2026-11-01T00:00:00Z: when the schedule begins, for everyone.
PER_CHANNEL = 120
BASE = 'mediatype:movies AND NOT collection:(tvnews OR tvarchive) AND NOT access-restricted-item:true'
RUS = 'language:(rus OR russian)'

# Central Television's programmes, by name, for ГОЛУБОЙ ЭКРАН.
SOVIET_TV = ('"советское телевидение" OR "soviet television" OR "soviet tv" OR "центральное телевидение" OR "цт ссср" '
             'OR гостелерадио OR "песня года" OR "голубой огонёк" OR телеспектакль OR телефильм OR кинопанорама '
             'OR "кабачок 13 стульев" OR "клуб кинопутешествий" OR "в мире животных" OR "утренняя почта" OR "вокруг смеха" '
             'OR "что? где? когда?" OR квн OR "ну-ка, девушки" OR "очевидное-невероятное" OR "международная панорама" '
             'OR "программа время" OR "в гостях у сказки" OR "служу советскому союзу" OR абвгдейка OR "весёлые ребята" '
             'OR "до 16 и старше" OR "музыкальный ринг" OR "камера смотрит в мир" OR виа OR эстрада')

CHANNELS = [
    dict(id="multiki", dial=1, name="МУЛЬТИКИ", family="cartoons", color="#E53935",
         description="Soviet cartoons: Soyuzmultfilm and friends, from the hand-drawn classics to the clay and puppet ones.",
         q='subject:(союзмультфильм OR soyuzmultfilm OR "советские мультфильмы" OR "soviet animation" OR "soviet cartoon" OR мультфильм)'),
    dict(id="kinozal", dial=2, name="КИНОЗАЛ", family="film", color="#8D6E63",
         description="Soviet cinema: Mosfilm, Lenfilm and the studios of the republics. Comedies, war films, the ones everyone quotes.",
         q=f'subject:("советское кино" OR "soviet film" OR "soviet movie" OR мосфильм OR mosfilm OR ленфильм OR lenfilm) OR (subject:(фильм OR кино) AND {RUS})'),
    dict(id="skazki", dial=3, name="СКАЗКИ", family="kids", color="#7E57C2",
         description="Fairy-tale films and children's television: Rowe's skazki, Yeralash, and the evening cartoon slot.",
         q='subject:(ералаш OR "спокойной ночи" OR "детское кино" OR "фильм-сказка" OR сказка OR "fairy tale")'),
    dict(id="fantastika", dial=4, name="ФАНТАСТИКА", family="film", color="#00ACC1",
         description="Soviet science fiction: spaceships, distant planets and quiet philosophical robots.",
         q=f'{RUS} AND subject:(фантастика OR "science fiction" OR sci-fi)'),
    dict(id="goluboy-ekran", dial=5, name="ГОЛУБОЙ ЭКРАН", family="tv", color="#1E88E5",
         description="Soviet Central Television: TV plays, KVN, Club of Film Travels, Morning Mail, Song of the Year, the New Year Blue Light and the evening programmes.",
         q=" OR ".join(f"{field}:({SOVIET_TV})" for field in ("subject", "title", "description"))),
    dict(id="khronika", dial=6, name="ХРОНИКА", family="educational", color="#78909C",
         description="Newsreels and documentaries: Novosti Dnya, science films, and the century on film.",
         q=f'{RUS} AND subject:(кинохроника OR документальный OR documentary OR newsreel OR "новости дня")'),
]

NOT_RETRO = re.compile(r"tom and jerry|том и джерри|silent hill|angry birds|фиксики|smeshariki|смешарики|маша и медведь|masha and the bear|"
                       r"yiddish ?book ?center|monster high|школа монстров|варкентин|уральские пельмени|плохие новости", re.I)


def same_title(title):
    """For spotting the same film uploaded twice: lower case, without years, bracketed notes or punctuation.
    Episode numbers stay, so part 1 and part 3 of a series are different tapes."""
    t = re.sub(r"[\(\[].*?[\)\]]", " ", title.lower())
    t = re.sub(r"\b(1[89]\d\d|20\d\d)\b", " ", t)
    return re.sub(r"[\W_]+", " ", t).strip()
CYRILLIC = re.compile(r"[А-Яа-яЁё]")
# USSR era only: a year up to 1991, or (undated) something that says it is Soviet.
SOVIET_MARK = re.compile(r"soviet|советск|ссср|ussr|союзмультфильм|soyuzmultfilm|мосфильм|mosfilm|ленфильм|lenfilm|"
                         r"гостелерадио|госкино|центральное телевидение|\bцт\b|новости дня|киножурнал|"
                         r"беларусьфильм|киностудия им\.? ?горького|свердловская киностудия", re.I)
MAX_YEAR = 1991


def get_json(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


def search(q, rows=PER_CHANNEL * 4):
    url = "https://archive.org/advancedsearch.php?" + urllib.parse.urlencode(
        {"q": f"{BASE} AND ({q})", "rows": rows, "fl[]": ["identifier"], "sort[]": "downloads desc", "output": "json"}, doseq=True)
    return [d["identifier"] for d in get_json(url)["response"]["docs"]]


def seconds(length):
    if length in (None, ""):
        return None
    if ":" in str(length):
        total = 0.0
        for part in str(length).split(":"):
            total = total * 60 + float(part)
        return total
    return float(length)


def first(value):
    return value[0] if isinstance(value, list) else value


def index_of(url, size):
    """Walks the MP4's top-level atoms to the moov: (offset, size), or None."""
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


def tape(identifier):
    try:
        m = get_json(f"https://archive.org/metadata/{urllib.parse.quote(identifier)}")
        md = m.get("metadata", {})
        if md.get("access-restricted-item") in (True, "true") or m.get("is_dark"):
            return None
        title = first(md.get("title")) or identifier
        year = re.search(r"(1[89]\d\d|20\d\d)", str(first(md.get("year")) or first(md.get("date")) or ""))
        year = int(year.group(1)) if year else None
        if year and year > MAX_YEAR:
            return None
        # Metadata years can be wrong; a later year in the title gives a tape away.
        if any(int(y) > MAX_YEAR for y in re.findall(r"\b(19\d\d|20\d\d)\b", str(title))):
            return None
        subjects = " ".join(md.get("subject") if isinstance(md.get("subject"), list) else [str(md.get("subject") or "")])
        language = " ".join(md.get("language") if isinstance(md.get("language"), list) else [str(md.get("language") or "")])
        russian = re.search(r"rus", language, re.I) or CYRILLIC.search(title) or re.search(r"soviet|советск|ссср|ussr", subjects + title, re.I)
        if not russian or NOT_RETRO.search(title + " " + subjects):
            return None
        description = first(md.get("description")) or ""
        if not year and not SOVIET_MARK.search(" ".join([str(title), subjects, str(description)[:500]])):
            return None
        best = None
        for f in m.get("files", []):
            if not f["name"].lower().endswith(".mp4"):
                continue
            length = seconds(f.get("length"))
            if not length or not 120 <= length <= 6 * 3600:
                continue
            # Prefer the Archive's own H.264 derivative, then the larger original.
            rank = (f.get("format", "").startswith("h.264"), int(f.get("size", 0)))
            if best is None or rank > best[0]:
                best = (rank, f, length)
        if not best:
            return None
        _, f, length = best
        url = f"https://{m['d1']}{m['dir']}/{urllib.parse.quote(f['name'])}"
        index = index_of(url, int(f.get("size", 0)))
        description = first(md.get("description")) or ""
        description = re.sub(r"<[^>]+>", " ", str(description))
        description = re.sub(r"\s+", " ", description).strip()[:300]
        return {
            "id": identifier, "file": f["name"], "duration": int(length), "title": str(title)[:120],
            "year": str(year) if year else None, "moov": index[1] if index else None, "description": description or None,
        }
    except Exception as e:  # one bad item never stops the build
        print(f"  skip {identifier}: {e}", file=sys.stderr)
        return None


def main(out, only=None):
    """Builds every channel, or with [only] rebuilds that one channel inside the existing file."""
    existing = {}
    if only:
        with open(out, encoding="utf-8") as f:
            existing = {c["id"]: c for c in json.load(f)["channels"]}
    channels, seen = [], set()
    if only:
        seen = {t["id"] for cid, c in existing.items() if cid != only for t in c["items"]}
    with ThreadPoolExecutor(8) as pool:
        for spec in CHANNELS:
            if only and spec["id"] != only:
                channels.append(existing[spec["id"]])
                continue
            ids = [i for i in search(spec["q"]) if i not in seen]
            tapes, titles = [], set()
            for t in pool.map(tape, ids):
                if t and same_title(t["title"]) not in titles and len(tapes) < PER_CHANNEL:
                    titles.add(same_title(t["title"]))
                    tapes.append(t)
            seen.update(t["id"] for t in tapes)
            random.Random(spec["id"]).shuffle(tapes)
            hours = sum(t["duration"] for t in tapes) / 3600
            print(f"{spec['dial']:02d} {spec['name']}: {len(tapes)} tapes, {hours:.0f} h")
            channels.append({k: spec[k] for k in ("id", "dial", "name", "family", "color", "description")} | {"items": tapes})
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"epochMs": EPOCH_MS, "channels": channels}, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--only=")]
    only = next((a.split("=", 1)[1] for a in sys.argv[1:] if a.startswith("--only=")), None)
    main(args[0] if args else "ru-lineup.json", only)
