"""Extract the Transport for NSW camera location PDFs in sources/ to CSV files in data/tfnsw/."""
import csv
import pathlib
import re

import pdfplumber

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "sources"
OUT = ROOT / "data" / "tfnsw"


def clean(cell):
    return re.sub(r"\s+", " ", (cell or "").replace("\n", " ")).strip()


def table_rows(pdf_path):
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            for table in page.extract_tables():
                for row in table:
                    cells = [clean(c) for c in row if c is not None]
                    # The red-light PDF pads its first page with empty columns.
                    while len(cells) > 3 and "" in cells[1:]:
                        cells.remove("")
                    if len(cells) == 3 and cells[0] and cells[0].upper() not in ("LOCATION", "SUBURB/TOWN"):
                        yield cells


def is_removed(comment):
    return bool(re.search(r"removed|decommissioned", comment, re.I))


def write(name, header, rows):
    with open(OUT / name, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)
    print(f"{name}: {len(rows)} rows")


def main():
    OUT.mkdir(parents=True, exist_ok=True)

    school = []
    for loc, road, comment in table_rows(SRC / "fixed_speed_cameras_school_zones.pdf"):
        school.append([loc, road, comment, int(is_removed(comment))])
    write("fixed_speed_cameras_school_zones.csv", ["location", "road", "comment", "removed"], school)

    fixed = []
    for loc, road, comment in table_rows(SRC / "crs_fixed_speed_cameras.pdf"):
        fixed.append([loc, road, comment, int(is_removed(comment))])
    write("fixed_speed_cameras.csv", ["location", "road", "comment", "removed"], fixed)

    red = []
    for loc, road, comment in table_rows(SRC / "crs_red_light_speed_cameras.pdf"):
        red.append([loc, road, comment, int(is_removed(comment)),
                    int(bool(re.search(r"school zone", comment, re.I)))])
    write("red_light_speed_cameras.csv", ["location", "roads", "comment", "removed", "school_zone"], red)


if __name__ == "__main__":
    main()
