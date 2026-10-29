# NSW fixed speed cameras and red-light speed cameras: 2026 site list

Research date: 29 Oct 2026. Scope: NSW fixed (digital) speed cameras and red-light speed cameras only (no mobile, point-to-point/average or bus/T-way cameras).

**How this list was built.** The Transport for NSW (TfNSW) camera pages return HTTP 403 to automated requests. So did the NSW Government media-release site, data.gov.au and council sites when fetched; WebFetch reported their hosts as unreachable from this session. I did not try to get around any of this. Instead I combined three official machine-readable sources that *were* reachable:
1. **Revenue NSW penalty-notice open data (`dfosf-001`)**. This is the only 2026-current official source. It has one row per camera location code × offence × month, with `CAMERA_TYPE`, `LOCATION_CODE` and `LOCATION_DETAILS` ("street and suburb ... and the direction in which it operates"). It covers offences to 30 Sep 2026 (notes sheet dated 1 Oct 2026, 266,109 records). A site counts as "active in 2026" if it issued penalty notices in Jul–Sep 2026. **It has no coordinates and no cross streets.**
2. **TfNSW Open Data "NSW Speed Cameras" (GPS locations November 2021)**. This is the only official source of coordinates. I matched the Revenue NSW sites to its rows by road and suburb names.
3. **TfNSW Open Data "Speed Zones"** (updated 28 Oct 2026). I spatially joined each 2021 camera coordinate to the speed-zone polylines within 30 m to get a posted limit. **This speed limit is a derived value, not one the camera sources state.**

Local files (all treated as untrusted; Python run with `-I`):
- `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/research_fixed/ckan1/fixed.csv`, `redlight.csv`, `gps.xlsx`: TfNSW Nov 2021 dataset
- `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/research_fixed/revenue/penalty-notice-data-20YY-20YY-dfosf-001-20YY.xlsx` (FY2022-23 to FY2026-27) and `penalty-notice-data-notes-dfosf-001-notes.xlsx`
- `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/research_fixed/speedzones/speedzones.zip` (shapefile, Web Mercator; 447,542 zone records), unzipped in `x/`
- Derived (by me): `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/scripts/agg_all.json` (per location code/direction: first and last offence month, counts, offence mix), `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/scripts/szjoin.json` (speed zones within 30 m of each 2021 coordinate), `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/scripts/table.md`, `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/scripts/unmatched.md`. Scripts: `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/scripts/agg.py`, `match2.py`, `szjoin.py`, `build.py`.

(SP = `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad`)

Source URLs:
- TfNSW dataset page: https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44 ; CKAN `package_show?id=nsw-speed-cameras` (metadata_modified 2023-08-21; resources still the Nov-2021 files)
  - XLSX: https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/a5c27c2d-5511-4fe8-863d-06a2d1a5ed10/download/nsw-speed-cameras-gps-locations-november-2021.xlsx
  - Fixed CSV: https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/bcf2f6f4-ecfb-40e1-a807-0d5eb5f51507/download/fixed-speed-cameras_1.csv
  - Red-light CSV: https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/debd70a9-f9f4-471c-81ae-c84098576ea6/download/red-light-speed-cameras_1.csv
- Revenue NSW statistics index: https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics ; files e.g. https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/penalty-notice-data-2026-2027-dfosf-001-2026.xlsx , ...-2025-2026-dfosf-001-2025.xlsx , ...-2024-2025-dfosf-001-2024.xlsx , ...-2023-2024-dfosf-001-2023.xlsx , ...-2022-2023-dfosf-001-2022.xlsx , ...penalty-notice-data-notes-dfosf-001-notes.xlsx
- TfNSW Speed Zones: https://opendata.transport.nsw.gov.au/data/dataset/4253a054-b377-4b5b-83d1-71385bb6ff33/resource/72cd0f29-f231-4c8c-a885-595946f3f202/download/speedzones.zip (CSV/GeoJSON/KML variants are listed in `package_show?id=speed-zones`)

## What fixed speed camera and red-light speed camera sites are current in 2026?

### Takeaway
Revenue NSW data to 30 Sep 2026 shows **146 fixed-speed-camera location codes** (138 issued notices in Jul–Sep 2026) and **278 red-light-speed-camera location codes** (270 active in Jul–Sep 2026). Each code is one camera or approach with its own direction of travel. This is many more than the 2021 dataset's 102 fixed and 201 red-light *site rows*, because a 2021 row can cover 2–3 cameras. It also includes new tunnel cameras that the 2021 file never listed. Coordinates exist only for sites that match a 2021 row.

### Cited Findings
- Revenue NSW `LOCATION_DETAILS` is defined as "The street and suburb of a Speed, Red light, Bus and T-way, Over-dimension or Point to point camera, and the direction in which it operates"; `LOCATION_CODE` is "A unique identifier for the location". The file includes "Penalty notices loaded to the fines system on or before 30/09/2026" — [Revenue NSW penalty notice data notes (dfosf-001-notes.xlsx)](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/penalty-notice-data-notes-dfosf-001-notes.xlsx)
- Camera types in FY2025-26 + FY2026-27 data: 'Red Light / Speed Camera', 'Fixed Digital Speed Camera', 'Mobile Digital Speed Camera', 'Average Speed Camera - Heavy/Light', 'Fixed Digital Transit-way Camera', 'Fixed Digital Bus Lane Camera', 'Over Dimension Camera', 'Pennant Hills Road Over Dimension', 'Seatbelt Detection', 'Mobile Phone Detection' — [Revenue NSW penalty notice data 2025-26](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/penalty-notice-data-2025-2026-dfosf-001-2025.xlsx), [2026-27](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/penalty-notice-data-2026-2027-dfosf-001-2026.xlsx)
- The 2021 TfNSW fixed CSV has 122 rows, of which 19 are blank, plus one row with no suburb, leaving **102 site rows** (125 cameras; 39 school-zone rows). The red-light CSV has 204 rows, 3 blank, leaving **201 site rows** (222 cameras; 12 school-zone rows). Columns: `SZ?, SUBURB/TOWN, ROAD/S, Cameras, Lat(1), Long(1), Lat(2), Long(2)[, Lat(3), Long(3)]` — [TfNSW Open Data NSW Speed Cameras](https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44)
- Revenue NSW codes the school-zone fixed cameras with "(Z)" in `LOCATION_DETAILS`, and their offence descriptions mention school zones — [Revenue NSW dfosf-001](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics)

### Master list (one row per Revenue NSW location code)
How to read the columns:
- **Dir**: direction(s) of travel enforced, from Revenue NSW `LOCATION_DETAILS` (N/S/E/W = north/south/east/westbound). A code with two letters had offences recorded in both directions under that code.
- **SZ**: Y if Revenue NSW marks the code "(Z)" or it has school-zone offences.
- **Offences seen**: the first and last month with penalty notices, within the data window Jul 2022 – Sep 2026. A first month of 2022-07 means the site already existed when the data starts. A later first month is the best official sign of a new site's start date; the warning-letter month is not included, because warnings are not penalty notices.
- **Status**: ACTIVE = notices issued in Jul–Sep 2026.
- **2021 match / Match**: my name-based match to the TfNSW Nov-2021 row. *exact* = all road and suburb words match one row. *ambiguous* = several 2021 rows match, all are listed and separated by `OR`. *probable* or *possible* = I matched it by hand, for example where an old Pacific Highway alignment was renamed (Giinagay Way, Hinterland Way, River Street Woodburn, Blackwall Drive Wardell) or the suburb name differs. *none* = no 2021 row, so the site has no official coordinates.
- **Lat,Lon**: copied verbatim from the 2021 dataset, rounded to 6 decimals. Where a 2021 row lists 2–3 cameras, all of its points are given and the dataset does not say which point serves which direction. For ambiguous matches, the coordinates of every candidate are listed.
- **Speed zone at coords**: limits of the TfNSW Speed Zones (Oct 2026) polylines within 30 m of each coordinate, in the form `limit@distance`. Suffixes: HP = High Pedestrian zone, V = variable zone, LT = local traffic zone, WW = wet-weather limit. `SZ40` means a school zone lies within 30 m. **These limits are derived:** at intersections the nearest polyline may be the cross street, so treat the first value as the most likely limit on the camera road and check it before use.

#### Fixed digital speed cameras (146 location codes)
| Rev. code | Location (Revenue NSW, 2022-26) | Dir | SZ | Offences seen | Status | 2021 TfNSW match (suburb - road/segment) | Match | Lat,Lon (TfNSW Nov 2021) | Speed zone at coords (derived, TfNSW Speed Zones Oct 2026) |
|---|---|---|---|---|---|---|---|---|---|
| 9815 | AVOCA STREET RANDWICK (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Randwick - Avoca Street, between Howard Street and Barker Road (school zone) | exact | -33.922843,151.240047 | 50@1m/SZ40 |
| 9530 | BANGALOW ROAD CLUNES | NS | N | 2022-07 to 2026-09 | ACTIVE | Clunes - Bangalow Road, between Flatley Drive and Johnston Road | exact | -28.734510,153.404620 | 50@3m |
| 9638 | BELLS LINE OF ROAD KURRAJONG HEIGHTS | ENW (as recorded) | N | 2022-07 to 2026-09 | ACTIVE | Kurrajong - Bells Line of Road, between Queen Street and Bellbird Avenue | probable | -33.533743,150.632662 | 60@4m |
| 9617 | BEXLEY ROAD BEXLEY NORTH | EW | N | 2022-07 to 2026-09 | ACTIVE | Bexley North - Bexley Road, between Kingsland Road North and Miller Avenue | exact | -33.942427,151.118774 | 60@8m |
| 9670 | BIG RIVER WAY ULMARRA | NS | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9821 | BIGGE STREET LIVERPOOL (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Liverpool - Bigge Street, between Elizabeth Drive and Campbell Street (school zone) | exact | -33.919167,150.927500 | 40HP@8m/SZ30 |
| 9570 | BLACKWALL DRIVE WARDELL | S | N | 2022-07 to 2026-09 | ACTIVE | Wardell - Pacific Highway, between Riverside Drive and Carlisle Street | probable | -28.945801,153.467568 | 80@8m |
| 9554 | BLAXLAND ROAD RYDE | E | N | 2022-07 to 2026-09 | ACTIVE | Ryde - Blaxland Road, between Reservoir Lane and North Road | exact | -33.803066,151.101937 | 60@10m |
| 9594 | BOLONG ROAD BOMADERRY | EW | N | 2023-04 to 2026-09 | ACTIVE; NEW since data start? first 2023-04 | Bomaderry - Bolong Road, between Beinda Street and Coomea Street | exact | -34.857839,150.605479 | 50@6m |
| 9816 | BOTANY ROAD ALEXANDRIA (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Rosebery/Alexandria - Botany Road, between Gardeners Road and Gillespie Street (school zone) | exact | -33.920970,151.197099; -33.920855,151.196872 | 50@12m/SZ40; 50@13m/60@27m/SZ40 |
| 9817 | BOTANY ROAD ROSEBERY (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Rosebery/Alexandria - Botany Road, between Gardeners Road and Gillespie Street (school zone) | exact | -33.920970,151.197099; -33.920855,151.196872 | 50@12m/SZ40; 50@13m/60@27m/SZ40 |
| 9571 | BRUXNER HWY WOLLONGBAR | EW | N | 2022-07 to 2026-09 | ACTIVE | Wollongbar - Bruxner Highway, between Convernys Lane and McLeans Ridges Road | exact | -28.812412,153.390786 | 80@3m |
| 9811 | BUNNERONG ROAD EASTGARDENS(Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Eastgardens / Maroubra - Bunnerong Road, between Fitzgerald Avenue and Smith Street | exact | -33.947093,151.228876; -33.946985,151.229010 | 60@2m/SZ40; 60@5m/SZ40 |
| 9812 | BUNNERONG ROAD MAROUBRA (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Eastgardens / Maroubra - Bunnerong Road, between Fitzgerald Avenue and Smith Street | exact | -33.947093,151.228876; -33.946985,151.229010 | 60@2m/SZ40; 60@5m/SZ40 |
| 9832 | CABRAMATTA ROAD BONNYRIGG (Z) | EW | Y | 2022-07 to 2026-08 | ACTIVE | Bonnyrigg - Cabramatta Road, between Katinka Street and Tarlington Parade (school zone) | exact | -33.893729,150.894911 | 60@2m/SZ40 |
| 9651 | CANTERBURY ROAD CANTERBURY | N | N | 2022-07 to 2026-09 | ACTIVE | Canterbury - Canterbury Road, between Gould Street and Jeffrey Street | exact | -33.913881,151.114813 | 60@9m |
| 9626 | CAPTAIN COOK DRIVE CARINGBAH | E | N | 2022-07 to 2026-09 | ACTIVE | Caringbah - Captain Cook Drive, between Cawarra Road and Gannons Road | exact | -34.035603,151.131347 | 70@7m/50@18m |
| 9604 | CARLINGFORD ROAD EPPING (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Epping - Carlingford Road, between Midson Road and Duntroon Ave (school zone) | exact | -33.773790,151.068150 | 60@2m/50@27m/SZ40 |
| 9560 | CASTLE HILL ROAD WEST PENNANT HILLS | W | N | 2022-07 to 2026-09 | ACTIVE | West Pennant Hills - Castle Hill Road, between Pennant Hills Road and Coonara Avenue | exact | -33.740118,151.038662 | 60@7m |
| 9550 | CENTENNIAL AVENUE LANE COVE | S | N | 2022-07 to 2026-09 | ACTIVE | Lane Cove - Centennial Avenue, between Gentle Street and Figtree Street | exact | -33.812868,151.158240 | 60@1m |
| 9837 | CLEVELAND STREET MOORE PARK (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Moore Park - Cleveland Street, between Anzac Parade and South Dowling Street (school zone) | exact | -33.893056,151.218889; -33.893056,151.218611 | 50@22m/SZ40; 50@6m/SZ40 |
| 9838 | CLEVELAND STREET MOORE PARK (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Moore Park - Cleveland Street, between Anzac Parade and South Dowling Street (school zone) | exact | -33.893056,151.218889; -33.893056,151.218611 | 50@22m/SZ40; 50@6m/SZ40 |
| 9625 | CONCORD ROAD CONCORD WEST | NS | N | 2022-07 to 2026-09 | ACTIVE | Concord West - Concord Road, between Nirranda Street and Mepunga Street | exact | -33.849520,151.089993 | 60@8m |
| 9614 | CROSS CITY TUNNEL EAST SYDNEY | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9613 | CROSS CITY TUNNEL WOOLLOOMOOLOO | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9640 | EASTERN ARTERIAL ROAD GORDON | NS | N | 2022-07 to 2026-09 | ACTIVE | Gordon - Eastern Arterial Road, between Koola Avenue and Nicholson Avenue | exact | -33.752060,151.170780 | 60@1m |
| 9635 | EASTERN DISTRIBUTOR DARLINGHURST | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9641 | EASTERN DISTRIBUTOR DARLINGHURST | S | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9642 | EASTERN VALLEY WAY NORTH WILLOUGHBY | NS | N | 2022-08 to 2026-09 | ACTIVE | North Willoughby - Eastern Valley Way, between Megalong Avenue and McClelland Street | exact | -33.794060,151.207950 | 60@2m |
| 9851 | EDGAR STREET CONDELL PARK (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Condell Park - Edgar Street, between Augusta Street and Upper Railway Parade (school zone) | exact | -33.925148,151.012729 | 60@9m/SZ40 |
| 9645 | ELIZABETH DRIVE MOUNT PRITCHARD | EW | N | 2022-07 to 2026-09 | ACTIVE | Bonnyrigg - Elizabeth Drive, between Brown Road and Humphries Road | possible | -33.897972,150.894463 | 60@10m |
| 9561 | FAIRFIELD STREET FAIRFIELD EAST | W | N | 2022-07 to 2026-09 | ACTIVE | Fairfield East - Fairfield Street, between Scott Street and Mandarin Street | exact | -33.869726,150.979134 | 60@7m |
| 9803 | FITZWILLIAM ROAD OLD TOONGABBIE (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Toongabbie - Fitzwilliam Road, between Reynolds Street and Binalong Road (school zone) | probable | -33.786295,150.965702; -33.786538,150.966049 | 60@11m/50@25m/SZ40; 60@11m/50@29m/SZ40 |
| 9802 | FITZWILLIAM ROAD TOONGABBIE (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Toongabbie - Fitzwilliam Road, between Reynolds Street and Binalong Road (school zone) | exact | -33.786295,150.965702; -33.786538,150.966049 | 60@11m/50@25m/SZ40; 60@11m/50@29m/SZ40 |
| 9830 | FOREST ROAD HURSTVILLE (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Hurstville - Forest Road, between Lily Street and Cronulla Street (school zone) | exact | -33.964232,151.111588 | 60@1m/SZ40 |
| 9829 | FOREST ROAD PENSHURST (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Penshurst - Forest Road, between Penshurst Street and St Georges Street (school zone) | exact | -33.963333,151.085833 | 60@5m/SZ40 |
| 9547 | GEORGE STREET SOUTH WINDSOR | NS | N | 2022-07 to 2026-09 | ACTIVE | South Windsor - George Street, between Rickaby Street and Yarrawonga Street | exact | -33.628409,150.795006 | 60@9m |
| 9646 | GIBSON AVENUE PADSTOW | NS | N | 2022-07 to 2026-09 | ACTIVE | Padstow - Gibson Avenue, between Turvey Street and Bryant Street | exact | -33.939877,151.027461 | 60@7m |
| 9665 | GIINAGAY WAY NORTH MACKSVILLE | EW | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9578 | GIINAGAY WAY NORTH MACKSVILLE | NS | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9632 | GIINAGAY WAY URUNGA | NS | N | 2022-07 to 2026-09 | ACTIVE | Urunga - Pacific Highway, between Ranger Street and Hillside Drive | probable | -30.504280,153.008460 | 50@1m |
| 9567 | GIINAGAY WAY VALLA | NS | N | 2022-07 to 2024-03 | INACTIVE? last offence 2024-03 | - | none | - | - |
| 9538 | GIINAGAY WAY VALLA | NS | N | 2022-07 to 2024-03 | INACTIVE? last offence 2024-03 | - | none | - | - |
| 9666 | GREAT WESTERN HIGHWAY MOUNT VICTORIA | EW | N | 2022-07 to 2026-03 | INACTIVE? last offence 2026-03 | Victoria Pass - Great Western Highway, between Mitchells Lookout Road and Ambermere Drive | probable | -33.582870,150.240470 | 60@7m |
| 9548 | GREAT WESTERN HIGHWAY SUN VALLEY | E | N | 2022-07 to 2026-09 | ACTIVE | Valley Heights - Great Western Highway, between The Valley Road and Sun Valley Road | exact | -33.711832,150.591937 | 80@8m |
| 9543 | GREYSTANES ROAD GREYSTANES | N | N | 2022-07 to 2026-09 | ACTIVE | Greystanes - Greystanes Road, between Merrylands Road and Old Prospect Road | exact | -33.823208,150.932614 | 60@10m |
| 9831 | HARBORD ROAD NORTH CURL CURL (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | North Curl Curl - Harbord Road, between Abbott Road and Brighton Street (school zone) | exact | -33.764167,151.279722 | 60@3m/SZ40 |
| 9669 | HEATHCOTE ROAD ENGADINE | EW | N | 2022-07 to 2026-09 | ACTIVE | Engadine - Heathcote Road, between Forum Drive and New Illawarra Road | exact | -34.073281,151.005560 | 70@11m |
| 9573 | HENRY LAWSON DRIVE PEAKHURST | W | N | 2022-07 to 2026-09 | ACTIVE | Peakhurst - Henry Lawson Drive, between Belmore Road and Ogilvy Street | exact | -33.961024,151.049526 | 60@6m |
| 9644 | HENRY LAWSON DRIVE PICNIC POINT | EW | N | 2022-07 to 2026-05 | INACTIVE? last offence 2026-05 | Picnic Point - Henry Lawson Drive, between Carinya Road and The River Road | exact | -33.979022,151.009847 | 60@8m |
| 9606 | HENRY PARRY DRIVE NORTH GOSFORD (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Wyoming - Henry Parry Drive, between Glennie Street and Dwyer Street (school zone) | probable | -33.413743,151.349571 | 50@8m/SZ40 |
| 9623 | HINTERLAND WAY EWINGSDALE | NS | N | 2022-08 to 2026-09 | ACTIVE | Ewingsdale - Pacific Highway, between St Helena Road and Ewingsdale Road. | probable | -28.647485,153.548529 | 80@10m |
| 9820 | HUME HIGHWAY ASHFIELD (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Ashfield - Hume Highway, between Murrell Street and Queen Street (school zone) | exact | -33.890177,151.127985 | 60@6m/SZ40 |
| 9818 | HUME HIGHWAY BANKSTOWN (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Bankstown - Hume Highway, between Rookwood Road and Stacey Street (school zone) | exact | -33.905919,151.038598; -33.906183,151.038915 | 60@6m/SZ40; 60@6m/SZ40 |
| 9819 | HUME HIGHWAY BANKSTOWN (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Bankstown - Hume Highway, between Rookwood Road and Stacey Street (school zone) | exact | -33.905919,151.038598; -33.906183,151.038915 | 60@6m/SZ40; 60@6m/SZ40 |
| 9522 | HUME HIGHWAY BURWOOD HEIGHTS | W | N | 2022-07 to 2026-09 | ACTIVE | Burwood Heights - Hume Highway, between Kelso Street and Appian Way | exact | -33.887825,151.104718 | 60@11m |
| 9546 | HUME HIGHWAY LANSVALE | S | N | 2022-07 to 2026-09 | ACTIVE | Lansvale - Hume Highway, between Henry Lawson Drive and Knight Street | exact | -33.891433,150.965596 | 70@8m |
| 9542 | HUME HIGHWAY YAGOONA | E | N | 2022-07 to 2026-09 | ACTIVE | Yagoona - Hume Highway, between Smith Street and Brennan Avenue | exact | -33.904663,151.008674 | 70@8m |
| 9540 | JAMES RUSE DRIVE CAMELLIA | S | N | 2022-07 to 2026-09 | ACTIVE | Camellia - James Ruse Drive, between Victoria Road and Grand Avenue North | exact | -33.815385,151.022514 | 70@8m |
| 9822 | KING GEORGES ROAD BEVERLY HILLS (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Beverly Hills - King Georges Road, between Stoney Creek Road and Edgbaston Road (school zone) | exact | -33.950556,151.081667; -33.950556,151.081944 | 60@2m/SZ40; 60@6m/SZ40 |
| 9823 | KING GEORGES ROAD BEVERLY HILLS (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Beverly Hills - King Georges Road, between Stoney Creek Road and Edgbaston Road (school zone) | exact | -33.950556,151.081667; -33.950556,151.081944 | 60@2m/SZ40; 60@6m/SZ40 |
| 9833 | KINGSWAY MIRANDA (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Miranda - Kingsway, between Sylva Avenue and University Road (school zone) | exact | -34.033572,151.096954; -34.033796,151.096936 | 60@6m/SZ40; 60@6m/SZ40 |
| 9834 | KINGSWAY MIRANDA (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Miranda - Kingsway, between Sylva Avenue and University Road (school zone) | exact | -34.033572,151.096954; -34.033796,151.096936 | 60@6m/SZ40; 60@6m/SZ40 |
| 9655 | LANE COVE TUNNEL (EASTBOUND) LANE COVE NORTH | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9656 | LANE COVE TUNNEL LANE COVE WEST | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9616 | M1 PACIFIC MOTORWAY BAR POINT | N | N | 2022-07 to 2026-09 | ACTIVE | Bar Point - M1, between Jolls Bridge and Mt White Exit Ramp | exact | -33.488208,151.183867 | 100@7m/90WW@7m |
| 9535 | M1 PACIFIC MOTORWAY OURIMBAH | N | N | 2022-07 to 2026-09 | ACTIVE | Ourimbah - M1 Pacific Motorway, between Dogtrap Road Overpass and Ourimbah Creek Road | exact | -33.358092,151.342957 | 110@9m |
| 9597 | M1 PRINCES MOTORWAY GWYNNEVILLE | N | N | 2022-07 to 2026-09 | ACTIVE | Gwynneville - M1 Princes Motorway (Northbound), between Northern Distrbutor Overpass and Gipps Road Overpass OR Gwynneville - M1 Princes Motorway (Southbound), between University Avenue Overpass and Mount Ousley Road | ambiguous | -34.414770,150.881317 OR -34.406248,150.883864 | 80@6m OR 80@8m |
| 9662 | M1 PRINCES MOTORWAY NORTH WOLLONGONG | S | N | 2022-07 to 2025-04 | INACTIVE? last offence 2025-04 | - | none | - | - |
| 9551 | M5 MOTORWAY (EASTBOUND) BARDWELL PARK | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9552 | M5 MOTORWAY (WESTBOUND) ARNCLIFFE | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9602 | MACPHERSON STREET MOSMAN (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Mosman - Macpherson Street, between Ourimbah Road and Montague Road (school zone) | exact | -33.822391,151.232049 | 50@15m/SZ40 |
| 9813 | MALABAR ROAD MAROUBRA (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Maroubra - Malabar Road, between Mons Avenue and Duncan Street (school zone) | exact | -33.946832,151.251671; -33.946867,151.251895 | 50@2m/SZ40; 50@7m/SZ40 |
| 9814 | MALABAR ROAD MAROUBRA (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Maroubra - Malabar Road, between Mons Avenue and Duncan Street (school zone) | exact | -33.946832,151.251671; -33.946867,151.251895 | 50@2m/SZ40; 50@7m/SZ40 |
| 9586 | MCCAFFREY DRIVE RANKIN PARK | EW | N | 2022-07 to 2026-09 | ACTIVE | Rankin Park - McCaffrey Drive, between Duval Street and Orara Street | exact | -32.925096,151.682014 | 60@6m |
| 9810 | MERRYLANDS ROAD MERRYLANDS (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Merrylands - Merrylands Road, between Chetwynd Road and Davies Street (school zone) | exact | -33.835000,150.982222 | 60@1m/50@26m/SZ40 |
| 9579 | NEW ENGLAND HIGHWAY BLANDFORD | NS | N | 2022-07 to 2026-08 | ACTIVE | Blandford - New England Highway, between Hayles Street and Mills Street | exact | -31.783599,150.895553 | 70@10m |
| 9563 | NEW ENGLAND HIGHWAY GLENCOE | NS | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9636 | NEW ENGLAND HIGHWAY LOCHINVAR (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Lochinvar - New England Highway, between Robert Road and Station Lane (school zone) | exact | -32.699608,151.458112 | 60@9m/SZ40 |
| 9568 | NEW ENGLAND HIGHWAY TENTERFIELD | EW | N | 2022-07 to 2026-09 | ACTIVE | Tenterfield - New England Highway, between Duncan Street and George Street | exact | -29.040066,152.016098 | default 50@6m |
| 9549 | NEW SOUTH HEAD ROAD EDGECLIFF | E | N | 2022-07 to 2026-09 | ACTIVE | Edgecliff - New South Head Road, between Waratah Street and New Beach Road | exact | -33.876419,151.230378 | 60@9m |
| 9592 | NORTHCLIFFE DRIVE WARRAWONG | E | N | 2022-07 to 2026-09 | ACTIVE | Warrawong - Northcliffe Drive, between Griffin Street and Kully Street | exact | -34.491750,150.876114 | default 50@13m |
| 9853 | NORTHCONNEX TUNNEL NORMANHURST | S | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9852 | NORTHCONNEX TUNNEL PENNANT HILLS | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9804 | OLD NORTHERN ROAD CASTLE HILL (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Castle Hill - Old Northern Road, between Telfer Road and Brisbane Road (school zone) | exact | -33.731824,151.013355 | 60@9m/SZ40 |
| 9622 | PACIFIC HIGHWAY CHARMHAVEN | NS | N | 2022-07 to 2026-09 | ACTIVE | Charmhaven - Pacific Highway, between Wallarah Creek and Lowana Avenue | exact | -33.220370,151.506044 | 70@8m |
| 9673 | PACIFIC HIGHWAY COWAN | EW | N | 2025-08 to 2026-09 | ACTIVE; NEW since data start? first 2025-08 | - | none | - | - |
| 9637 | PACIFIC HIGHWAY GATESHEAD (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Gateshead - Pacific Highway, between Sydney Street and Macquarie Avenue (school zone) | exact | -32.983675,151.692176 | 60@7m/SZ40 |
| 9824 | PACIFIC HIGHWAY LINDFIELD (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Lindfield - Pacific Highway, between Eton Road and Gladstone Parade (school zone) | exact | -33.781144,151.170351; -33.781263,151.170161 | 60@5m/SZ40; 60@7m/SZ40 |
| 9825 | PACIFIC HIGHWAY LINDFIELD (Z) | W | Y | 2022-07 to 2026-08 | ACTIVE | Lindfield - Pacific Highway, between Eton Road and Gladstone Parade (school zone) | exact | -33.781144,151.170351; -33.781263,151.170161 | 60@5m/SZ40; 60@7m/SZ40 |
| 9582 | PACIFIC HIGHWAY MAYFIELD WEST | E | N | 2022-07 to 2026-09 | ACTIVE | Mayfield West - Pacific Highway, between Werribie Street and Tourle Street | exact | -32.892232,151.729852 | 60@10m |
| 9583 | PACIFIC HIGHWAY NORDS WHARF | N | N | 2022-07 to 2026-09 | ACTIVE | Nords Wharf - Pacific Highway, between Nords Wharf Road and Flowers Drive | exact | -33.133930,151.613190 | 80@9m |
| 9660 | PACIFIC HIGHWAY OURIMBAH (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Ourimbah - Pacific Highway, between Yates Road and Dog Trap Road (school zone) | exact | -33.353403,151.369606; -33.353408,151.369643 | 60@4m/SZ40; 60@4m/SZ40 |
| 9661 | PACIFIC HIGHWAY OURIMBAH (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Ourimbah - Pacific Highway, between Yates Road and Dog Trap Road (school zone) | exact | -33.353403,151.369606; -33.353408,151.369643 | 60@4m/SZ40; 60@4m/SZ40 |
| 9580 | PACIFIC HIGHWAY SANDGATE | N | N | 2022-07 to 2026-09 | ACTIVE | Sandgate - Pacific Highway, between Wallsend Road and Ironbark Creek | exact | -32.856201,151.703884; -32.857687,151.704872 | 80@8m; 80@14m |
| 9581 | PACIFIC HIGHWAY SANDGATE | S | N | 2022-07 to 2026-09 | ACTIVE | Sandgate - Pacific Highway, between Wallsend Road and Ironbark Creek | exact | -32.856201,151.703884; -32.857687,151.704872 | 80@8m; 80@14m |
| 9843 | PACIFIC HIGHWAY WAHROONGA (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Wahroonga - Pacific Highway, between Gilda Avenue and Woodville Avenue (school zone) | exact | -33.722880,151.118455; -33.722473,151.118365 | 60@0m/SZ40; 60@0m/SZ40 |
| 9844 | PACIFIC HIGHWAY WAHROONGA (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Wahroonga - Pacific Highway, between Gilda Avenue and Woodville Avenue (school zone) | exact | -33.722880,151.118455; -33.722473,151.118365 | 60@0m/SZ40; 60@0m/SZ40 |
| 9845 | PARKER STREET KINGSWOOD (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Kingswood - Parker Street, between Copeland Street and Gascoigne Street (school zone) | exact | -33.753285,150.712860; -33.752854,150.713134 | 70@0m/SZ40; 70@0m/50@10m/SZ40 |
| 9846 | PARKER STREET KINGSWOOD (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Kingswood - Parker Street, between Copeland Street and Gascoigne Street (school zone) | exact | -33.753285,150.712860; -33.752854,150.713134 | 70@0m/SZ40; 70@0m/50@10m/SZ40 |
| 9534 | PARRAMATTA RD AUBURN | E | N | 2022-07 to 2026-09 | ACTIVE | Auburn - Parramatta Road, between Harbord Street and Duck Street | exact | -33.835963,151.023621 | 60@10m |
| 9541 | PENNANT HILLS RD NORTH PARRAMATTA | E | N | 2022-07 to 2026-09 | ACTIVE | North Parramatta - Pennant Hills Road, between Castle Street and Bellevue Street | probable | -33.800577,151.008280 | 60@7m/50@26m |
| 9553 | PENNANT HILLS ROAD CARLINGFORD | W | N | 2022-07 to 2026-09 | ACTIVE | Carlingford - Pennant Hills Road, between Evans Road and Coleman Avenue | exact | -33.782671,151.049147 | 60@10m |
| 9657 | PENNANT HILLS ROAD NTH PARRAMATTA (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | North Parramatta / Oatlands - Pennant Hills Road, between Masons Drive and Suttor Avenue (school zone) | probable | -33.793412,151.018726; -33.793574,151.018831 | 60@5m/SZ40; 60@4m/SZ40 |
| 9658 | PENNANT HILLS ROAD OATLANDS (Z) | S | Y | 2022-12 to 2026-09 | ACTIVE; NEW since data start? first 2022-12 | North Parramatta / Oatlands - Pennant Hills Road, between Masons Drive and Suttor Avenue (school zone) | exact | -33.793412,151.018726; -33.793574,151.018831 | 60@5m/SZ40; 60@4m/SZ40 |
| 9826 | PITTWATER ROAD NARRABEEN (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Narrabeen - Pittwater Road, between Ocean Street and Devitt Street (school zone) | exact | -33.720004,151.298177; -33.720578,151.298017 | 60@5m/40HP@20m/SZ40; 60@6m/SZ40 |
| 9827 | PITTWATER ROAD NARRABEEN (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Narrabeen - Pittwater Road, between Ocean Street and Devitt Street (school zone) | exact | -33.720004,151.298177; -33.720578,151.298017 | 60@5m/40HP@20m/SZ40; 60@6m/SZ40 |
| 9847 | PITTWATER ROAD NORTH NARRABEEN (Z) | N | Y | 2022-08 to 2026-09 | ACTIVE | North Narrabeen - Pittwater Road, between Garden Street and Namona Street (school zone) | exact | -33.700941,151.299187; -33.702165,151.298422 | 70@0m/SZ40; 70@0m/SZ40 |
| 9848 | PITTWATER ROAD NORTH NARRABEEN (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | North Narrabeen - Pittwater Road, between Garden Street and Namona Street (school zone) | exact | -33.700941,151.299187; -33.702165,151.298422 | 70@0m/SZ40; 70@0m/SZ40 |
| 9603 | PRESIDENT AVENUE GYMEA (Z) | EW | Y | 2022-07 to 2026-09 | ACTIVE | Gymea - President Avenue, between Koorabell Road and Gymea Bay South Road (school zone) | exact | -34.037500,151.083610 | 60@7m/SZ40 |
| 9600 | PRINCES HIGHWAY (NORTHBOUND) KOGARAH (Z) | N | Y | 2022-07 to 2025-08 | INACTIVE? last offence 2025-08 | Kogarah - Princes Highway, between Gray Street and President Avenue (school zone) | exact | -33.967854,151.136347; -33.967061,151.136748 | 60@4m/SZ40; 60@8m/SZ40 |
| 9601 | PRINCES HIGHWAY (SOUTHBOUND) KOGARAH (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Kogarah - Princes Highway, between Gray Street and President Avenue (school zone) | exact | -33.967854,151.136347; -33.967061,151.136748 | 60@4m/SZ40; 60@8m/SZ40 |
| 9612 | PRINCES HIGHWAY BROGO | NS | N | 2022-07 to 2024-03 | INACTIVE? last offence 2024-03 | - | none | - | - |
| 9509 | PRINCES HIGHWAY BULLI | NS | N | 2022-07 to 2026-09 | ACTIVE | Bulli - Princes Highway, between Grevillea Park Road and Black Diamond Place | exact | -34.331546,150.913769 | 60@10m/50@18m |
| 9639 | PRINCES HIGHWAY NORTH WOLLONGONG | S | N | 2022-07 to 2026-09 | ACTIVE | North Wollongong - Princes Highway, between Ajax Avenue and Exeter Avenue | exact | -34.411419,150.892612 | 60@6m |
| 9610 | PRINCES HIGHWAY WEST WOLLONGONG (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Wollongong - Princes Highway, between Mount Keira Road and Highway Avenue (school zone) | probable | -34.427122,150.869797 | 60@9m/SZ40 |
| 9643 | RICHMOND ROAD BERKSHIRE PARK | EW | N | 2022-07 to 2026-09 | ACTIVE | Berkshire Park - Richmond Road, between Llandilo Road and Sanctuary Drive | exact | -33.661322,150.797290 | 80@8m |
| 9648 | RIVER STREET WOODBURN | NS | N | 2022-08 to 2026-09 | ACTIVE | Woodburn - Pacific Highway, between Wagner Street and Norman Street | probable | -29.067438,153.351425 | 50@9m |
| 9861 | ROZELLE INTERCHANGE ROZELLE | E | N | 2024-01 to 2026-09 | ACTIVE; NEW since data start? first 2024-01 | - | none | - | - |
| 9859 | ROZELLE INTERCHANGE ROZELLE | N | N | 2024-02 to 2026-09 | ACTIVE; NEW since data start? first 2024-02 | - | none | - | - |
| 9858 | ROZELLE INTERCHANGE ROZELLE | S | N | 2024-01 to 2026-09 | ACTIVE; NEW since data start? first 2024-01 | - | none | - | - |
| 9860 | ROZELLE INTERCHANGE ROZELLE | W | N | 2024-01 to 2026-09 | ACTIVE; NEW since data start? first 2024-01 | - | none | - | - |
| 9650 | SPIT ROAD BEAUTY POINT | N | N | 2022-07 to 2026-05 | INACTIVE? last offence 2026-05 | The Spit/Beauty Point - Spit Road, between Parriwi Road and Upper Spit Road and between Central Avenue and Medusa Street | exact | -33.813950,151.243860; -33.809950,151.245310 | 60@1m; 60@3m |
| 9615 | SPIT ROAD MOSMAN | S | N | 2022-08 to 2026-09 | ACTIVE | The Spit/Beauty Point - Spit Road, between Parriwi Road and Upper Spit Road and between Central Avenue and Medusa Street | exact | -33.813950,151.243860; -33.809950,151.245310 | 60@1m; 60@3m |
| 9800 | SYDNEY HARBOUR TUNNEL (NORTH) SYDNEY | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9801 | SYDNEY HARBOUR TUNNEL (SOUTH) SYDNEY | S | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9849 | THE BOULEVARDE STRATHFIELD (Z) | NS | Y | 2022-07 to 2026-09 | ACTIVE | Strathfield - The Boulevarde, between Torrington Parade and Russell Street (school zone) | exact | -33.877923,151.093196 | 60@0m/SZ40 |
| 9647 | THE GRAND PARADE BRIGHTON-LE-SANDS | N | N | 2023-12 to 2026-09 | ACTIVE; NEW since data start? first 2023-12 | - | none | - | - |
| 9649 | THE GRAND PARADE BRIGHTON-LE-SANDS | S | N | 2024-02 to 2026-09 | ACTIVE; NEW since data start? first 2024-02 | - | none | - | - |
| 9653 | THE GRAND PARADE SANS SOUCI | NS | N | 2024-01 to 2026-09 | ACTIVE; NEW since data start? first 2024-01 | - | none | - | - |
| 9652 | TWEED VALLEY WAY STOKERS SIDING | NS | N | 2022-07 to 2026-09 | ACTIVE | Burringbar - Tweed Valley Way, between Blakeneys Road and Cooradilla Road | possible | -28.401083,153.438162 | 80@1m |
| 9839 | VICTORIA ROAD RYDALMERE (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Rydalmere - Victoria Road, between Park Road and John Street (school zone) | exact | -33.812845,151.045128; -33.812644,151.044190 | 70@0m/SZ40; 70@0m/SZ40 |
| 9840 | VICTORIA ROAD RYDALMERE (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Rydalmere - Victoria Road, between Park Road and John Street (school zone) | exact | -33.812845,151.045128; -33.812644,151.044190 | 70@0m/SZ40; 70@0m/SZ40 |
| 9835 | VICTORIA ROAD RYDE (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Ryde - Victoria Road, between Margaret Street and Cressy Road (school zone) | exact | -33.819599,151.117412; -33.819877,151.117690 | 60@3m/SZ40; 60@3m/SZ40 |
| 9836 | VICTORIA ROAD RYDE (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Ryde - Victoria Road, between Margaret Street and Cressy Road (school zone) | exact | -33.819599,151.117412; -33.819877,151.117690 | 60@3m/SZ40; 60@3m/SZ40 |
| 9667 | WARRINGAH ROAD NARRAWEENA | E | N | 2022-07 to 2026-09 | ACTIVE | Narraweena - Warringah Road, near the intersection with May Road | exact | -33.756043,151.276273 | 60@7m/70@20m |
| 6216 | WESTCONNEX M4 EAST TUNNEL CROYDON | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 6217 | WESTCONNEX M4 EAST TUNNEL CROYDON | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9854 | WESTCONNEX M8 EAST TUNNEL BARDWELL PARK | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 9857 | WESTCONNEX M8 TUNNEL NEWTOWN | N | N | 2023-02 to 2026-09 | ACTIVE; NEW since data start? first 2023-02 | - | none | - | - |
| 9856 | WESTCONNEX M8 TUNNEL NEWTOWN | S | N | 2023-01 to 2026-09 | ACTIVE; NEW since data start? first 2023-01 | - | none | - | - |
| 9855 | WESTCONNEX M8 WEST TUNNEL ARNCLIFFE | W | N | 2022-07 to 2026-07 | ACTIVE | - | none | - | - |
| 9842 | WOODVILLE ROAD CHESTER HILL (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Old Guilford - Woodville Road, between Orchardleigh Street and Middleton Road (school zone) | probable | -33.867660,150.987598; -33.867396,150.987915 | 70@0m/SZ40; 70@0m/SZ40 |
| 9806 | WOODVILLE ROAD GUILDFORD (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Guildford - Woodville Road, between Kenelda Avenue and Osgood Street (school zone) | exact | -33.848889,150.996944; -33.848986,150.997190 | 70@9m/SZ40; 70@6m/SZ40 |
| 9807 | WOODVILLE ROAD GUILDFORD (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Guildford - Woodville Road, between Kenelda Avenue and Osgood Street (school zone) | exact | -33.848889,150.996944; -33.848986,150.997190 | 70@9m/SZ40; 70@6m/SZ40 |
| 9841 | WOODVILLE ROAD OLD GUILDFORD (Z) | N | Y | 2022-07 to 2026-08 | ACTIVE | Old Guilford - Woodville Road, between Orchardleigh Street and Middleton Road (school zone) | probable | -33.867660,150.987598; -33.867396,150.987915 | 70@0m/SZ40; 70@0m/SZ40 |

#### Red-light speed cameras (278 location codes)
| Rev. code | Location (Revenue NSW, 2022-26) | Dir | SZ | Offences seen | Status | 2021 TfNSW match (suburb - road/segment) | Match | Lat,Lon (TfNSW Nov 2021) | Speed zone at coords (derived, TfNSW Speed Zones Oct 2026) |
|---|---|---|---|---|---|---|---|---|---|
| 7273 | ABBOTT ROAD SEVEN HILLS | W | N | 2022-07 to 2026-09 | ACTIVE | Seven Hills - Abbott Road and Prospect Highway | exact | -33.768286,150.943411 | 60@15m |
| 7316 | ABERCROMBIE STREET CHIPPENDALE | N | N | 2022-07 to 2026-09 | ACTIVE | Ultimo - Abercrombie Street and Broadway | probable | -33.884408,151.198648 | 50@6m |
| 7198 | ACACIA ROAD KIRRAWEE | S | N | 2022-07 to 2026-09 | ACTIVE | Kirrawee - President Avenue and Acacia Road | exact | -34.034763,151.066413 | 70@5m |
| 7285 | ALISON ROAD RANDWICK | W | N | 2022-07 to 2026-09 | ACTIVE | Randwick - Alison Road and Avoca Street | exact | -33.913259,151.242287 | 50@6m/60@25m |
| 7208 | ANZAC PARADE KENSINGTON | N | N | 2022-07 to 2026-09 | ACTIVE | Kensington - Anzac Parade and Todman Avenue | exact | -33.909919,151.223291 | 50@9m/40LT@17m |
| 7308 | ANZAC PARADE MAROUBRA | S | N | 2022-07 to 2026-09 | ACTIVE | Maroubra Junction - Anzac Parade and Boyce Road | exact | -33.939880,151.239244 | 50@9m |
| 7140 | ANZAC PARADE MOORE PARK | N | N | 2022-07 to 2024-06 | INACTIVE? last offence 2024-06 | - | none | - | - |
| 7178 | ANZAC PARADE MOORE PARK | S | N | 2022-07 to 2026-09 | ACTIVE | Moore Park - Anzac Parade and Cleveland Street / Lang Road | possible | -33.894640,151.221637 | 60@10m/SZ40 |
| 7232 | ARCHBOLD ROAD LINDFIELD | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7260 | ARCHBOLD ROAD ROSEVILLE | S | N | 2022-08 to 2026-09 | ACTIVE | - | none | - | - |
| 7154 | ARTHUR STREET STRATHFIELD | E | N | 2022-07 to 2026-09 | ACTIVE | Homebush West / Strathfield - Arthur Street and Centenary Drive | exact | -33.867599,151.063807 | 60@15m/80@20m |
| 7424 | AUSTRALIA AVENUE SYDNEY OLYMPIC PARK | N | N | 2025-12 to 2026-09 | ACTIVE; NEW since data start? first 2025-12 | - | none | - | - |
| 7199 | AVOCA STREET RANDWICK | N | N | 2022-07 to 2026-09 | ACTIVE | Randwick - Alison Road and Avoca Street OR Randwick - Darley Road and Avoca Street / York Road | ambiguous | -33.913259,151.242287 OR -33.903022,151.244762 | 50@6m/60@25m OR 50@2m/40LT@22m |
| 7354 | BANKS ROAD HINCHINBROOK | N | N | 2024-04 to 2026-09 | ACTIVE; NEW since data start? first 2024-04 | - | none | - | - |
| 7175 | BESTIC STREET ROCKDALE | W | N | 2022-07 to 2025-09 | INACTIVE? last offence 2025-09 | Rockdale - West Botany Street and Bestic Street | exact | -33.949753,151.148993 | 60@0m/SZ40 |
| 7206 | BEXLEY ROAD KINGSGROVE | N | N | 2022-07 to 2026-09 | ACTIVE | Earlwood / Kingsgrove - Bexley Road and William Street | exact | -33.928021,151.108939 | 60@4m |
| 7200 | BLAXLAND ROAD EASTWOOD | N | N | 2022-07 to 2026-09 | ACTIVE | Eastwood - Blaxland Road and May Street | exact | -33.788626,151.085751 | 60@0m/50@27m |
| 7225 | BORELLA ROAD ALBURY (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Albury - Borella Road and Young Street (school zone) | possible | -36.075180,146.925250 | 50@0m/60@4m/SZ40 |
| 7134 | BOURKE STREET WOOLLOOMOOLOO | S | N | 2022-07 to 2026-09 | ACTIVE | Wolloomooloo - Bourke Street and William Street | exact | -33.874590,151.218080 | 40@3m/50@6m/80V@6m |
| 7106 | BOURKE STREET ZETLAND | W | N | 2022-07 to 2026-09 | ACTIVE | Alexandria /Zetland - Bourke Street and Botany Road | exact | -33.905448,151.203116 | 40LT@7m/50@28m |
| 7283 | BRIENS ROAD NORTHMEAD | W | N | 2022-08 to 2026-09 | ACTIVE | Northmead - Briens Road and Redbank Road | exact | -33.795427,150.990153 | 70@9m |
| 7252 | BROUGHTON STREET CONCORD | S | N | 2022-07 to 2026-09 | ACTIVE | Concord - Broughton Street and Crane Street | exact | -33.861433,151.106230 | 50@28m |
| 7160 | BUNGARRIBEE ROAD BLACKTOWN | E | N | 2022-07 to 2026-09 | ACTIVE | Blacktown - Bungarribee Road and Balmoral Street / Reservoir Road | exact | -33.776519,150.901258 | 60@6m |
| 7277 | BURWOOD ROAD BELFIELD | N | N | 2022-07 to 2026-09 | ACTIVE | Belfield - Burwood Road and Punchbowl Road | exact | -33.902363,151.083603 | 60@19m |
| 7413 | BURWOOD ROAD CROYDON PARK | S | N | 2024-09 to 2026-09 | ACTIVE; NEW since data start? first 2024-09 | - | none | - | - |
| 7237 | CAMDEN VALLEY WAY PRESTONS | N | N | 2022-08 to 2026-09 | ACTIVE | - | none | - | - |
| 7246 | CANTERBURY ROAD CAMPSIE | E | N | 2022-07 to 2026-09 | ACTIVE | Campsie - Canterbury Road and Bexley Road OR Campsie - Canterbury Road and Thorncraft Parade | ambiguous | -33.918154,151.104657 OR -33.920186,151.099487 | 60@3m OR 60@6m/50@21m |
| 7167 | CANTERBURY ROAD CAMPSIE | W | N | 2022-07 to 2026-09 | ACTIVE | Campsie - Canterbury Road and Bexley Road OR Campsie - Canterbury Road and Thorncraft Parade | ambiguous | -33.918154,151.104657 OR -33.920186,151.099487 | 60@3m OR 60@6m/50@21m |
| 7115 | CANTERBURY ROAD ROSELANDS | W | N | 2023-08 to 2026-09 | ACTIVE; NEW since data start? first 2023-08 | Rosleands / Wiley Park - Canterbury Road and King Georges Road | exact | -33.929356,151.073011; -33.929551,151.071556 | 60@5m; 60@7m |
| 7114 | CANTERBURY ROAD WILEY PARK | E | N | 2023-07 to 2026-09 | ACTIVE; NEW since data start? first 2023-07 | Rosleands / Wiley Park - Canterbury Road and King Georges Road | exact | -33.929356,151.073011; -33.929551,151.071556 | 60@5m; 60@7m |
| 7228 | CAPTAIN COOK DRIVE (EASTBOUND) WOOLOOWARE | E | N | 2022-07 to 2026-09 | ACTIVE | Woolooware - Captain Cook Drive and Foreshore Boulevarde | exact | -34.039352,151.138967; -34.039054,151.138353 | 70@5m; 70@5m |
| 7229 | CAPTAIN COOK DRIVE (WESTBOUND) WOOLOOWARE | W | N | 2022-07 to 2026-09 | ACTIVE | Woolooware - Captain Cook Drive and Foreshore Boulevarde | exact | -34.039352,151.138967; -34.039054,151.138353 | 70@5m; 70@5m |
| 7303 | CARLINGFORD ROAD EPPING | W | N | 2022-07 to 2026-09 | ACTIVE | Epping - Carlingford Road and Rawson Street | exact | -33.771150,151.080575 | 60@6m/50@28m |
| 7207 | CHALMERS STREET REDFERN (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Surry Hills / Redfern - Cleveland Street and Chalmers Street (school zone) | exact | -33.889880,151.206100 | 40LT@6m/50@22m/SZ40 |
| 7405 | CHAPEL ROAD BANKSTOWN | N | N | 2024-10 to 2026-09 | ACTIVE; NEW since data start? first 2024-10 | - | none | - | - |
| 7340 | CHARLESTOWN ROAD CHARLESTOWN | N | N | 2022-07 to 2026-09 | ACTIVE | Charlestown - Charlestown Road and Hillsborough Road | exact | -32.956672,151.686657 | 60@0m |
| 7187 | CHARLESTOWN ROAD KOTARA | N | N | 2022-07 to 2026-09 | ACTIVE | Kotara - Charlestown Road and Carnley Avenue | exact | -32.940540,151.688140 | 70@0m |
| 7250 | CHISHOLM ROAD AUBURN | N | N | 2023-03 to 2026-09 | ACTIVE; NEW since data start? first 2023-03 | Auburn - Chisholm Road and Mona Street | exact | -33.848376,151.021343 | 50@7m |
| 7251 | CITY WEST LINK LEICHHARDT | W | N | 2022-07 to 2026-09 | ACTIVE | Lilyfield - City West Link Road and James Street | probable | -33.875086,151.155788 | 70@1m/50@1m |
| 7105 | CLEVELAND STREET SURRY HILLS | E | N | 2022-07 to 2026-09 | ACTIVE | Surry Hills / Redfern - Cleveland Street and Chalmers Street (school zone) OR Redfern / Surry Hills - Cleveland Street and South Dowling Street | ambiguous | -33.889880,151.206100 OR -33.891974,151.216463 | 40LT@6m/50@22m/SZ40 OR 50@9m/40@19m/60@26m |
| 7287 | COLLINS STREET ALEXANDRIA | E | N | 2022-07 to 2026-09 | ACTIVE | Beaconsfield / Alexandria - Collins Street and O'Riordan Street | exact | -33.911362,151.197403 | 40@7m/50@23m |
| 7218 | CORRIMAL STREET WOLLONGONG | N | N | 2022-07 to 2026-09 | ACTIVE | Wollongong - Corrimal Street and Burelli Street | exact | -34.427145,150.899935 | 50@14m |
| 7234 | COWARD STREET MASCOT | W | N | 2022-07 to 2026-09 | ACTIVE | Mascot - O'Riordan Street and Coward Street | exact | -33.924603,151.190366 | 50@7m |
| 7269 | COX'S ROAD NORTH RYDE (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | North Ryde - Lane Cove Road and Coxs Road (school zone) | exact | -33.793378,151.119951; -33.793277,151.119479; -33.793034,151.119952 | 70@17m/SZ40; 70@7m/SZ40; 70@7m/SZ40 |
| 7139 | CRAIGEND STREET DARLINGHURST | W | N | 2022-07 to 2026-09 | ACTIVE | Rushcutters Bay - Craigend Street and McLachlan Avenue | probable | -33.876918,151.228798 | 60@5m |
| 7332 | CROWN STREET SURRY HILLS | N | N | 2022-07 to 2026-09 | ACTIVE | Surry Hills - Crown Street and Albion Street OR Surry Hills - Foveaux Street and Crown Street | ambiguous | -33.883634,151.214255 OR -33.885198,151.214246 | 40@8m/40LT@10m OR 40LT@5m/40@19m |
| 7109 | CUMBERLAND HIGHWAY CABRAMATTA | N | N | 2022-07 to 2026-09 | ACTIVE | Cabramatta West - Cumberland Highway and Cabramatta Road West OR Canley Heights / Cabramatta - Cumberland Highway and St Johns Road | ambiguous | -33.898886,150.919473; -33.899725,150.918936 OR -33.888111,150.923039 | 70@0m; 70@8m/60@19m OR 70@5m/60@19m/SZ40 |
| 7108 | CUMBERLAND HIGHWAY CABRAMATTA | S | N | 2022-07 to 2026-09 | ACTIVE | Cabramatta West - Cumberland Highway and Cabramatta Road West OR Canley Heights / Cabramatta - Cumberland Highway and St Johns Road | ambiguous | -33.898886,150.919473; -33.899725,150.918936 OR -33.888111,150.923039 | 70@0m; 70@8m/60@19m OR 70@5m/60@19m/SZ40 |
| 7324 | CUMBERLAND HIGHWAY CANLEY HEIGHTS (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Canley Heights / Cabramatta - Cumberland Highway and St Johns Road OR Canley Heights - Cumberland Highway and Canley Vale Road (school zone) | ambiguous | -33.888111,150.923039 OR -33.883836,150.923628 | 70@5m/60@19m/SZ40 OR 70@9m/SZ40 |
| 7159 | CUMBERLAND HIGHWAY CANLEY HEIGHTS | S | N | 2022-07 to 2026-09 | ACTIVE | Canley Heights / Cabramatta - Cumberland Highway and St Johns Road OR Canley Heights - Cumberland Highway and Canley Vale Road (school zone) | ambiguous | -33.888111,150.923039 OR -33.883836,150.923628 | 70@5m/60@19m/SZ40 OR 70@9m/SZ40 |
| 7403 | CUMBERLAND HIGHWAY FAIRFIELD WEST | N | N | 2023-12 to 2026-09 | ACTIVE; NEW since data start? first 2023-12 | - | none | - | - |
| 7335 | CUMBERLAND HIGHWAY GREYSTANES | S | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7280 | CUMBERLAND HIGHWAY SMITHFIELD | S | N | 2022-07 to 2026-09 | ACTIVE | Smithfield - Cumberland Highway and The Horsley Drive | exact | -33.853333,150.941174 | 70@1m |
| 7179 | CUMBERLAND HIGHWAY SOUTH WENTWORTHVILLE | S | N | 2022-07 to 2026-09 | ACTIVE | South Wentworthville - Cumberland Highway and Old Prospect Road | exact | -33.817305,150.962803 | 70@3m |
| 7418 | DAVIS ROAD WETHERILL PARK | E | N | 2025-06 to 2026-09 | ACTIVE; NEW since data start? first 2025-06 | - | none | - | - |
| 7171 | DOBROYD PARADE HABERFIELD | W | N | 2022-07 to 2026-09 | ACTIVE | Haberfield - Dobroyd Pde and Timbrell Drive / Mortley Avenue | exact | -33.870756,151.142217 | 60@1m |
| 7255 | DONALD STREET HAMILTON | W | N | 2022-11 to 2026-09 | ACTIVE; NEW since data start? first 2022-11 | - | none | - | - |
| 7415 | DONNISON STREET GOSFORD | E | N | 2025-12 to 2026-09 | ACTIVE; NEW since data start? first 2025-12 | - | none | - | - |
| 7227 | DURHAM STREET BATHURST | S | N | 2022-07 to 2026-09 | ACTIVE | Bathurst - Durham Street and Bentinck Street | exact | -33.417700,149.585070 | 50@2m/60@4m |
| 7347 | EAST STREET NOWRA | N | N | 2022-07 to 2026-09 | ACTIVE | Nowra - East Street and Moss Street | exact | -34.872208,150.605812 | 70@6m |
| 7429 | EAST STREET NOWRA | S | N | 2026-08 to 2026-09 | ACTIVE; NEW since data start? first 2026-08 | Nowra - East Street and Moss Street | exact | -34.872208,150.605812 | 70@6m |
| 7272 | EDGAR STREET BANKSTOWN | S | N | 2022-07 to 2026-09 | ACTIVE | Condell Park - Edgar Street and Marion Street | possible | -33.915916,151.014023 | 60@6m |
| 7422 | ELIZABETH DRIVE ASHCROFT | N | N | 2025-07 to 2026-09 | ACTIVE; NEW since data start? first 2025-07 | - | none | - | - |
| 7352 | ELIZABETH DRIVE LIVERPOOL | E | N | 2023-09 to 2026-09 | ACTIVE; NEW since data start? first 2023-09 | Liverpool - Copeland Street / Hume Highway and Elizabeth Drive | exact | -33.919025,150.919600 | 60@1m |
| 7295 | ELIZABETH STREET SYDNEY | N | N | 2022-07 to 2026-09 | ACTIVE | Sydney - Elizabeth Street and Park Street | exact | -33.873458,151.209802 | 40HP@7m/80V@16m |
| 7297 | EPPING ROAD LANE COVE | W | N | 2022-07 to 2026-09 | ACTIVE | Lane Cove North - Epping Road and Centennial Avenue | exact | -33.810788,151.163786 | 60@2m/80@15m |
| 7186 | FAIRLIGHT STREET FIVE DOCK | E | N | 2022-07 to 2026-09 | ACTIVE | Five Dock - Ramsay Road and Fairlight Street | exact | -33.870113,151.130631 | 60@10m |
| 7305 | FALCON STREET NEUTRAL BAY | W | N | 2022-07 to 2026-09 | ACTIVE | North Sydney / Neutral Bay - Falcon Street and Merlin Street | exact | -33.829902,151.215360 | 60@5m |
| 7133 | FIGTREE STREET LANE COVE | N | N | 2022-07 to 2026-09 | ACTIVE | Lane Cove - Figtree Street and Centennial Avenue | exact | -33.813940,151.155360 | 60@1m |
| 7411 | FIVE ISLANDS ROAD PORT KEMBLA | N | N | 2024-10 to 2026-09 | ACTIVE; NEW since data start? first 2024-10 | - | none | - | - |
| 7141 | FLINDERS STREET PADDINGTON | S | N | 2022-07 to 2026-08 | ACTIVE | - | none | - | - |
| 7323 | FORESHORE ROAD BANKSMEADOW | E | N | 2022-07 to 2026-09 | ACTIVE | Banksmeadow - Foreshore Road and Botany Road | exact | -33.961967,151.214850 | 70@4m/50@24m |
| 7183 | FOREST ROAD BEXLEY | S | N | 2022-07 to 2026-09 | ACTIVE | Bexley - Forest Road and Harrow Road | exact | -33.948321,151.127750 | 60@6m |
| 7230 | FOSTER STREET LEICHHARDT | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7329 | FOVEAUX STREET SURRY HILLS | W | N | 2022-07 to 2026-09 | ACTIVE | Surry Hills - Foveaux Street and Crown Street | exact | -33.885198,151.214246 | 40LT@5m/40@19m |
| 7180 | FREDERICK STREET ASHFIELD | S | N | 2022-07 to 2026-09 | ACTIVE | Ashfield - Hume Highway and Fredrick Street | exact | -33.888045,151.117876 | 50@6m |
| 7143 | GEORGE STREET MOUNT DRUITT | S | N | 2022-07 to 2026-09 | ACTIVE | Minchinbury - George Street and Great Western Highway | possible | -33.781440,150.823950 | 80@4m |
| 7244 | GEORGE STREET HAYMARKET | E | N | 2022-07 to 2026-09 | ACTIVE | Haymarket - George Street and Pitt Street | exact | -33.882861,151.203981; -33.882643,151.204378 | 40HP@5m; 40HP@5m/20@21m |
| 7243 | GEORGE STREET HAYMARKET | S | N | 2022-07 to 2022-12 | INACTIVE? last offence 2022-12 | - | none | - | - |
| 7275 | GEORGES RIVER ROAD CROYDON PARK | E | N | 2022-07 to 2026-09 | ACTIVE | Croydon Park - Georges River Road and Croydon Avenue | exact | -33.894633,151.110990 | 60@8m |
| 7216 | GLADSTONE AVENUE WOLLONGONG | N | N | 2022-07 to 2026-09 | ACTIVE | Wollongong - Princes Highway and Gladstone Avenue | exact | -34.425962,150.887237 | 50@22m |
| 7189 | GOLD COAST HIGHWAY TWEED HEADS WEST | S | N | 2022-07 to 2026-09 | ACTIVE | Tweed Heads West - Gold Coast Highway and Pacific Highway | exact | -28.178620,153.521320 | 60@3m |
| 7157 | GORDON STREET PETERSHAM | S | N | 2022-07 to 2026-09 | ACTIVE | Petersham - Gordon Street and New Canterbury Road | exact | -33.896422,151.152442 | 50@7m/60@19m |
| 7224 | GORDON STREET PORT MACQUARIE | E | N | 2022-07 to 2026-09 | ACTIVE | Port Macquarie - Gordon Street and Grant Street | exact | -31.433010,152.914900 | 60@3m |
| 7168 | GREAT WESTERN HIGHWAY BLACKTOWN | W | N | 2022-07 to 2026-09 | ACTIVE | Blacktown - Great Western Highway and Reservoir Road | exact | -33.798404,150.897611 | 80@3m/60@9m |
| 7137 | GREAT WESTERN HIGHWAY CLAREMONT MEADOWS | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7319 | GREAT WESTERN HIGHWAY EASTERN CREEK | E | N | 2022-08 to 2026-09 | ACTIVE | Eastern Creek - Great Western Highway and Rooty Hill Road South | exact | -33.788493,150.853669 | 80@8m |
| 7119 | GREAT WESTERN HIGHWAY MERRYLANDS | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7274 | GREAT WESTERN HIGHWAY MOUNT DRUITT | E | N | 2022-07 to 2026-09 | ACTIVE | Mount Druitt - Great Western Highway and Carlisle Avenue | exact | -33.779018,150.814018 | 80@9m |
| 7333 | GREAT WESTERN HIGHWAY PARRAMATTA | E | N | 2022-07 to 2026-09 | ACTIVE | Parramatta - Great Western Highway and Marsden Street | exact | -33.819269,150.999915 | 60@7m |
| 7334 | GREAT WESTERN HIGHWAY PROSPECT | W | N | 2022-07 to 2026-09 | ACTIVE | Prospect - Great Western Highway and Clunies Ross Street | exact | -33.805152,150.927916 | 80@7m |
| 7122 | GREAT WESTERN HIGHWAY ST MARYS | E | N | 2022-07 to 2026-09 | ACTIVE | St Marys - Great Western Highway and Charles Hackett Drive | exact | -33.769462,150.768572; -33.769671,150.768902 | 60@9m; 60@4m |
| 7123 | GREAT WESTERN HIGHWAY ST MARYS | W | N | 2022-07 to 2026-09 | ACTIVE | St Marys - Great Western Highway and Charles Hackett Drive | exact | -33.769462,150.768572; -33.769671,150.768902 | 60@9m; 60@4m |
| 7164 | GREAT WESTERN HIGHWAY WENTWORTHVILLE | E | N | 2022-07 to 2026-08 | ACTIVE | Wentworthville - Great Western Highway and Station Street | exact | -33.815378,150.969323 | 80@6m/50@30m |
| 7118 | GREAT WESTERN HIGHWAY WESTMEAD | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7213 | GRIFFITHS ROAD LAMBTON | E | N | 2022-07 to 2026-09 | ACTIVE | Lambton - Griffiths Road and Turton Road | exact | -32.914583,151.725936 | 60@3m |
| 7166 | HAMILTON ROAD FAIRFIELD | W | N | 2022-08 to 2026-09 | ACTIVE | Fairfield - Sackville Street and Hamilton Road | possible | -33.872813,150.945605 | 60@5m/50@13m |
| 7261 | HARRIS STREET ULTIMO | S | N | 2022-12 to 2026-09 | ACTIVE; NEW since data start? first 2022-12 | - | none | - | - |
| 7304 | HARROW ROAD BEXLEY | S | N | 2022-07 to 2026-09 | ACTIVE | Bexley - Forest Road and Harrow Road OR Bexley - Harrow Road and Watkin Street | ambiguous | -33.948321,151.127750 OR -33.952566,151.131741 | 60@6m OR 60@7m |
| 7236 | HENRY LAWSON DR M5 MWY ONRAMP MILPERRA | S | N | 2022-07 to 2026-09 | ACTIVE | Milperra - Milperra Road / Newbridge Road and Henry Lawson Drive | probable | -33.929359,150.979692 | 70@1m/60@28m |
| 7128 | HENRY LAWSON DRIVE LANSDOWNE | N | N | 2022-07 to 2026-09 | ACTIVE | Milperra - Milperra Road / Newbridge Road and Henry Lawson Drive | possible | -33.929359,150.979692 | 70@1m/60@28m |
| 7267 | HENRY LAWSON DRIVE MILPERRA | N | N | 2022-12 to 2026-09 | ACTIVE; NEW since data start? first 2022-12 | Milperra - Milperra Road / Newbridge Road and Henry Lawson Drive | exact | -33.929359,150.979692 | 70@1m/60@28m |
| 7326 | HOMEBUSH BAY DRIVE SYDNEY OLYMPIC PARK | N | N | 2022-07 to 2026-09 | ACTIVE | Homebush West - Homebush Bay Drive and M4 Eastbound On Ramp | probable | -33.855944,151.071924 | 80@7m |
| 7135 | HOXTON PARK ROAD HOXTON PARK | W | N | 2022-07 to 2026-09 | ACTIVE | West Hoxton - Hoxton Park Road and Cowpasture Road | exact | -33.924600,150.848480 | 70@1m/60@26m |
| 7348 | HUME HIGHWAY BASS HILL | W | N | 2022-07 to 2026-09 | ACTIVE | Bass Hill - Hume Highway and Chester Hill Road | exact | -33.899032,150.996470 | 70@1m/50@23m |
| 7407 | HUME HIGHWAY CABRAMATTA | N | N | 2024-10 to 2026-09 | ACTIVE; NEW since data start? first 2024-10 | - | none | - | - |
| 7408 | HUME HIGHWAY CASULA | S | N | 2025-01 to 2026-09 | ACTIVE; NEW since data start? first 2025-01 | - | none | - | - |
| 7239 | HUME HIGHWAY GREENACRE | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7315 | HUME HIGHWAY LIVERPOOL | S | N | 2022-07 to 2026-09 | ACTIVE | Liverpool - Hume Highway and Hoxton Park Road OR Liverpool - Copeland Street / Hume Highway and Elizabeth Drive | ambiguous | -33.928038,150.918027 OR -33.919025,150.919600 | 60@8m OR 60@1m |
| 7148 | HUME HIGHWAY LIVERPOOL | S | N | 2022-07 to 2026-09 | ACTIVE | Liverpool - Hume Highway and Hoxton Park Road OR Liverpool - Copeland Street / Hume Highway and Elizabeth Drive | ambiguous | -33.928038,150.918027 OR -33.919025,150.919600 | 60@8m OR 60@1m |
| 7342 | HUME HIGHWAY WARWICK FARM | E | N | 2022-07 to 2026-09 | ACTIVE | Warwick Farm - Hume Highway and Bigge Street OR Warwick Farm - Hume Highway and Mannix Parade | ambiguous | -33.914318,150.928652 OR -33.913820,150.931716 | 70@6m OR 70@6m |
| 7176 | HUME HIGHWAY WARWICK FARM | W | N | 2022-07 to 2026-09 | ACTIVE | Warwick Farm - Hume Highway and Bigge Street OR Warwick Farm - Hume Highway and Mannix Parade | ambiguous | -33.914318,150.928652 OR -33.913820,150.931716 | 70@6m OR 70@6m |
| 7406 | HUME HIGHWAY YAGOONA (Z) | W | Y | 2024-11 to 2026-09 | ACTIVE; NEW since data start? first 2024-11 | - | none | - | - |
| 7142 | JERSEY ROAD PLUMPTON | W | N | 2024-09 to 2026-09 | ACTIVE; NEW since data start? first 2024-09 | Plumpton - Jersey Road and Hyatts Road | exact | -33.744070,150.834810 | 60@0m |
| 7419 | JOHNSTON STREET ANNANDALE | S | N | 2026-05 to 2026-09 | ACTIVE; NEW since data start? first 2026-05 | - | none | - | - |
| 7110 | KELLICAR ROAD CAMPBELLTOWN | E | N | 2022-07 to 2026-09 | ACTIVE | Campbelltown - Kellicar Road and New Narellan Road | exact | -34.072236,150.805293; -34.072035,150.805940 | 60@4m; 60@6m |
| 7111 | KELLICAR ROAD CAMPBELLTOWN | W | N | 2022-07 to 2026-09 | ACTIVE | Campbelltown - Kellicar Road and New Narellan Road | exact | -34.072236,150.805293; -34.072035,150.805940 | 60@4m; 60@6m |
| 7313 | KING GEORGES ROAD BLAKEHURST | S | N | 2022-07 to 2026-09 | ACTIVE | Blakehurst - King Georges Road and Terry Street | exact | -33.983135,151.109302 | 70@6m |
| 7169 | KING GEORGES ROAD ROSELANDS | N | N | 2022-07 to 2026-09 | ACTIVE | Roselands - King Georges Road and Moorefields Road OR Rosleands / Wiley Park - Canterbury Road and King Georges Road | ambiguous | -33.939975,151.075464 OR -33.929356,151.073011; -33.929551,151.071556 | 60@7m/SZ40 OR 60@5m; 60@7m |
| 7290 | KING STREET WARRAWONG | S | N | 2022-07 to 2026-09 | ACTIVE | Warrawong - Cowper Street and King Street / Grand Pacific Drive | exact | -34.485159,150.887656 | 60@4m/40HP@24m |
| 7278 | KINGSGROVE ROAD KINGSGROVE | N | N | 2022-08 to 2026-09 | ACTIVE | Earlwood / Kingsgrove - Bexley Road and William Street OR Belmore / Kingsgrove - Kingsgrove Road and Forsyth Street | ambiguous | -33.928021,151.108939 OR -33.929450,151.098558 | 60@4m OR 50@3m |
| 7162 | KINGSWAY CARINGBAH | W | N | 2022-07 to 2026-09 | ACTIVE | Caringbah - The Kingsway and Gannons Road | exact | -34.045942,151.133228 | 70@4m |
| 7126 | KINGSWAY MIRANDA | E | N | 2022-07 to 2026-09 | ACTIVE | Miranda - The Kingsway and Port Hacking Road | exact | -34.035067,151.112817 | 60@10m |
| 7117 | LAKEMBA STREET WILEY PARK | W | N | 2022-07 to 2026-09 | ACTIVE | Wiley Park - Lakemba Street and King Georges Road | exact | -33.921320,151.067600 | 60@0m/50@0m |
| 7196 | LANCASTER STREET BLACKTOWN (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | Blacktown - Kildare Street and Lancaster Street (school zone) | exact | -33.768824,150.892578 | 60@10m/SZ40 |
| 7270 | LANE COVE ROAD (NORTHBOUND) NORTH RYDE (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | North Ryde - Lane Cove Road and Coxs Road (school zone) | exact | -33.793378,151.119951; -33.793277,151.119479; -33.793034,151.119952 | 70@17m/SZ40; 70@7m/SZ40; 70@7m/SZ40 |
| 7254 | LANE COVE ROAD (SOUTHBOUND) NORTH RYDE (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | North Ryde - Lane Cove Road and Coxs Road (school zone) | exact | -33.793378,151.119951; -33.793277,151.119479; -33.793034,151.119952 | 70@17m/SZ40; 70@7m/SZ40; 70@7m/SZ40 |
| 7425 | LANE COVE ROAD NORTH RYDE | S | N | 2025-09 to 2026-09 | ACTIVE; NEW since data start? first 2025-09 | North Ryde - Lane Cove Road and Coxs Road (school zone) | exact | -33.793378,151.119951; -33.793277,151.119479; -33.793034,151.119952 | 70@17m/SZ40; 70@7m/SZ40; 70@7m/SZ40 |
| 7350 | LANYON DRIVE JERRABOMBERRA | E | N | 2023-06 to 2026-09 | ACTIVE; NEW since data start? first 2023-06 | - | none | - | - |
| 7223 | M4 WESTERN MOTORWAY OFF-RAMP AUBURN | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7282 | MACQUARIE STREET PARRAMATTA | E | N | 2022-07 to 2026-09 | ACTIVE | Parramatta - Marsden Street and Macquarie Street | exact | -33.814854,151.001910 | 40HP@3m |
| 7325 | MACQUARIE STREET WINDSOR | S | N | 2022-07 to 2026-09 | ACTIVE | Windsor - Macquarie Street and Hawkesbury Valley Way | exact | -33.612150,150.815459 | 60@6m |
| 7284 | MAMRE ROAD ST MARYS (Z) | S | Y | 2022-07 to 2026-09 | ACTIVE | St Marys - Mamre Road and Saddington Street (school zone) | exact | -33.772929,150.773105 | 60@8m/SZ40 |
| 7238 | MAMRE ROAD ST MARYS | S | N | 2022-07 to 2026-09 | ACTIVE | St Marys - Mamre Road and Saddington Street (school zone) | exact | -33.772929,150.773105 | 60@8m/SZ40 |
| 7306 | MARCH STREET RICHMOND | E | N | 2022-07 to 2026-09 | ACTIVE | Richmond - March Street and East Market Street | exact | -33.599044,150.751741 | 60@7m |
| 7102 | MCEVOY STREET ALEXANDRIA | E | N | 2022-07 to 2026-09 | ACTIVE | Alexandria / Waterloo - McEvoy Street and Botany Road | exact | -33.901483,151.200850; -33.901451,151.201546 | 50@9m; 50@6m |
| 7331 | MCEVOY STREET WATERLOO | E | N | 2022-07 to 2026-09 | ACTIVE | Alexandria / Waterloo - McEvoy Street and Botany Road OR Waterloo - McEvoy Street and Elizabeth Street | ambiguous | -33.901483,151.200850; -33.901451,151.201546 OR -33.900255,151.205728 | 50@9m; 50@6m OR 50@12m |
| 7101 | MCEVOY STREET WATERLOO | W | N | 2022-07 to 2026-09 | ACTIVE | Alexandria / Waterloo - McEvoy Street and Botany Road OR Waterloo - McEvoy Street and Elizabeth Street | ambiguous | -33.901483,151.200850; -33.901451,151.201546 OR -33.900255,151.205728 | 50@9m; 50@6m OR 50@12m |
| 7253 | MEMORIAL AVENUE LIVERPOOL | E | N | 2022-07 to 2026-09 | ACTIVE | Liverpool - Memorial Avenue and Bathurst Street | exact | -33.924521,150.920698 | 50@4m |
| 7172 | MEMORIAL DRIVE CORRIMAL | S | N | 2022-07 to 2026-09 | ACTIVE | Towradgi - Memorial Drive and Towradgi Road | probable | -33.826305,151.199270 | 60@3m |
| 7346 | MEMORIAL DRIVE TOWRADGI | N | N | 2022-07 to 2026-09 | ACTIVE | Towradgi - Memorial Drive and Towradgi Road | exact | -33.826305,151.199270 | 60@3m |
| 7155 | MEREDITH STREET BANKSTOWN (Z) | N | Y | 2022-07 to 2026-09 | ACTIVE | Bankstown - Meredith Street and Hume Highway (school zone) | exact | -33.907435,151.033270 | 60@4m/SZ40 |
| 7299 | MERRYLANDS ROAD MERRYLANDS WEST | W | N | 2022-07 to 2026-09 | ACTIVE | Merrylands West - Cumberland Highway and Merrylands Road OR Merrylands West - Merrylands Road and Sherwood Road | ambiguous | -33.830034,150.960491 OR -33.832451,150.969880 | 70@5m OR 60@4m |
| 7205 | MILITARY ROAD MOSMAN | W | N | 2022-09 to 2026-07 | ACTIVE | Mosman - Military Road and Cowles Road | exact | -33.824447,151.238658 | 60@3m |
| 7353 | MILPERRA ROAD MILPERRA | E | N | 2024-02 to 2026-09 | ACTIVE; NEW since data start? first 2024-02 | Milperra - Milperra Road / Newbridge Road and Henry Lawson Drive | exact | -33.929359,150.979692 | 70@1m/60@28m |
| 7197 | MINJUNGBAL DRIVE TWEED HEADS SOUTH | N | N | 2022-07 to 2026-09 | ACTIVE | South Tweed Heads - Minjungbal Drive and Rivendell Street | exact | -28.205800,153.544530 | 60@0m |
| 7151 | MOORE PARK ROAD PADDINGTON | E | N | 2022-07 to 2026-09 | ACTIVE | Paddington - Moore Park Road and Oxford Street / Lang Road | exact | -33.889736,151.232499 | 40@8m/60@19m |
| 7338 | MOORE STREET LIVERPOOL | E | N | 2022-07 to 2026-09 | ACTIVE | Liverpool - Moore Street and Bathurst Street | exact | -33.922154,150.921151 | 50@5m/40HP@18m |
| 7276 | MOORE-OXLEY BYPASS BRADBURY | S | N | 2022-07 to 2026-09 | ACTIVE | Campbelltown - Moore-Oxley Bypass / Appin Road and The Parkway | probable | -34.074459,150.808577 | 60@1m |
| 7104 | MOORE-OXLEY BYPASS CAMPBELLTOWN | E | N | 2022-07 to 2026-09 | ACTIVE | Campbelltown - Moore-Oxley Bypass and Broughton Street | possible | -34.066640,150.819252; -34.066599,150.819836 | 60@7m; 60@5m |
| 7103 | MOORE-OXLEY BYPASS CAMPBELLTOWN | W | N | 2022-07 to 2026-09 | ACTIVE | Campbelltown - Moore-Oxley Bypass and Broughton Street | possible | -34.066640,150.819252; -34.066599,150.819836 | 60@7m; 60@5m |
| 7351 | MULGOA ROAD PENRITH | N | N | 2023-08 to 2026-09 | ACTIVE; NEW since data start? first 2023-08 | - | none | - | - |
| 7423 | NARELLAN ROAD CAMPBELLTOWN | N | N | 2026-06 to 2026-09 | ACTIVE; NEW since data start? first 2026-06 | Campbelltown - Kellicar Road and New Narellan Road | exact | -34.072236,150.805293; -34.072035,150.805940 | 60@4m; 60@6m |
| 7259 | NEW ENGLAND HIGHWAY DUNOLLY | W | N | 2022-10 to 2026-09 | ACTIVE; NEW since data start? first 2022-10 | - | none | - | - |
| 7149 | NEWBRIDGE ROAD MILPERRA | E | N | 2022-07 to 2026-09 | ACTIVE | Milperra - Milperra Road / Newbridge Road and Henry Lawson Drive | exact | -33.929359,150.979692 | 70@1m/60@28m |
| 7181 | NEWBRIDGE ROAD MOOREBANK | W | N | 2022-07 to 2026-09 | ACTIVE | Moorebank - Newbridge Road and Stockton Avenue | exact | -33.929234,150.948529 | 70@2m |
| 7170 | NEWTON ROAD BLACKTOWN (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Blacktown - Newton Road and Flushcombe Road (school zone) | probable | -33.773527,150.908426 | 60@5m/SZ40 |
| 7249 | O'CONNELL STREET PARRAMATTA | S | N | 2022-07 to 2026-09 | ACTIVE | Parramatta - O'Connell Street and Argyle Street | exact | -33.815943,150.999395 | 60@4m/40HP@13m |
| 7158 | O'RIORDAN STREET MASCOT | N | N | 2022-07 to 2025-04 | INACTIVE? last offence 2025-04 | - | none | - | - |
| 7190 | O'RIORDAN STREET MASCOT | S | N | 2022-07 to 2026-09 | ACTIVE | Mascot - O'Riordan Street and Coward Street OR Mascot - O'Riordan Street and Gardeners Road | ambiguous | -33.924603,151.190366 OR -33.921053,151.191684 | 50@7m OR 50@4m/60@26m |
| 7192 | OLD SOUTH HEAD ROAD BONDI JUNCTION | W | N | 2022-07 to 2026-09 | ACTIVE | Bondi Junction - Old South Head Road and Bondi Road | exact | -33.890842,151.253824 | 50@2m |
| 7311 | OLD WINDSOR ROAD GLENWOOD | N | N | 2026-01 to 2026-09 | ACTIVE; NEW since data start? first 2026-01 | Parklea / Glenwood - Old Windsor Road and Sunnyholt Road | exact | -33.721085,150.937967 | 80@4m |
| 7182 | OLD WINDSOR ROAD NORTHMEAD | E | N | 2022-07 to 2026-09 | ACTIVE | Old Toongabbie / Northmead - Old Windsor Road and Cumberland Highway | exact | -33.793959,150.983149 | 70@8m |
| 7153 | OLYMPIC DRIVE LIDCOMBE | N | N | 2022-07 to 2026-09 | ACTIVE | Lidcombe - Olympic Drive and Vaughan Street | exact | -33.865136,151.041569 | 70@4m/50@26m |
| 7318 | OXFORD STREET DARLINGHURST | E | N | 2022-07 to 2026-09 | ACTIVE | Darlinghurst - Oxford Street and Crown Street | exact | -33.879455,151.215372; -33.879094,151.215134 | 40HP@0m/40LT@18m; 40HP@1m/40LT@26m |
| 7302 | OXFORD STREET DARLINGHURST | W | N | 2022-08 to 2026-09 | ACTIVE | Darlinghurst - Oxford Street and Crown Street | exact | -33.879455,151.215372; -33.879094,151.215134 | 40HP@0m/40LT@18m; 40HP@1m/40LT@26m |
| 7256 | PACIFIC HIGHWAY CHARLESTOWN | W | N | 2022-09 to 2026-09 | ACTIVE | - | none | - | - |
| 7344 | PACIFIC HIGHWAY COFFS HARBOUR | N | N | 2022-07 to 2026-09 | ACTIVE | Coffs Harbour - Pacific Highway and Bray Street OR Coffs Harbour - Pacific Highway and Combine Street | ambiguous | -30.283770,153.126717 OR -30.300783,153.110611 | 60@7m OR 60@0m |
| 7343 | PACIFIC HIGHWAY COFFS HARBOUR | N | N | 2022-07 to 2026-09 | ACTIVE | Coffs Harbour - Pacific Highway and Bray Street OR Coffs Harbour - Pacific Highway and Combine Street | ambiguous | -30.283770,153.126717 OR -30.300783,153.110611 | 60@7m OR 60@0m |
| 7412 | PACIFIC HIGHWAY COFFS HARBOUR | N | N | 2024-11 to 2026-05 | INACTIVE? last offence 2026-05; NEW since data start? first 2024-11 | Coffs Harbour - Pacific Highway and Bray Street OR Coffs Harbour - Pacific Highway and Combine Street | ambiguous | -30.283770,153.126717 OR -30.300783,153.110611 | 60@7m OR 60@0m |
| 7174 | PACIFIC HIGHWAY CROWS NEST | N | N | 2022-07 to 2026-09 | ACTIVE | Crows Nest - Pacific Highway and Hume Street | exact | -34.373540,150.900630 | 80@2m |
| 7212 | PACIFIC HIGHWAY HAMILTON EAST | N | N | 2022-07 to 2025-11 | INACTIVE? last offence 2025-11 | Hamilton East - Pacific Highway and Parry Street | exact | -32.926876,151.757796 | 60@6m |
| 7417 | PACIFIC HIGHWAY HEATHERBRAE | N | N | 2025-12 to 2026-09 | ACTIVE; NEW since data start? first 2025-12 | - | none | - | - |
| 7258 | PACIFIC HIGHWAY LAKE MUNMORAH | W | N | 2022-09 to 2026-09 | ACTIVE | - | none | - | - |
| 7414 | PACIFIC HIGHWAY LANE COVE NORTH | N | N | 2024-11 to 2026-09 | ACTIVE; NEW since data start? first 2024-11 | - | none | - | - |
| 7214 | PACIFIC HIGHWAY MAYFIELD WEST | W | N | 2022-07 to 2026-09 | ACTIVE | Mayfield West - Pacific Highway / Maitland Road and Maud Street | exact | -32.890059,151.726633 | 60@8m |
| 7307 | PACIFIC HIGHWAY WAITARA (Z) | E | Y | 2022-07 to 2026-09 | ACTIVE | Waitara - Pacific Highway and Romsey Street (school zone) | exact | -33.710292,151.102728 | 60@8m/SZ40 |
| 7416 | PACIFIC HWY HAMLYN TERRACE | E | N | 2026-04 to 2026-09 | ACTIVE; NEW since data start? first 2026-04 | - | none | - | - |
| 7211 | PARK AVENUE ADAMSTOWN | W | N | 2022-07 to 2026-09 | ACTIVE | Kotara / Adamstown - Park Avenue and Northcott Drive | exact | -32.940335,151.712118 | default 50@7m |
| 7195 | PARK ROAD KOGARAH BAY | N | N | 2022-07 to 2026-09 | ACTIVE | Allawah / Kogarah Bay - Princes Highway and Park Rd | exact | -33.978668,151.122791 | 60@2m/70@22m |
| 7191 | PARKER STREET SOUTH PENRITH | N | N | 2025-09 to 2026-09 | ACTIVE; NEW since data start? first 2025-09 | - | none | - | - |
| 7130 | PARRAMATTA ROAD BURWOOD | W | N | 2022-07 to 2026-09 | ACTIVE | Burwood - Parramatta Road and Burwood Road | exact | -33.869033,151.105583; -33.868767,151.105200 | 60@13m; 60@11m/50@26m |
| 7129 | PARRAMATTA ROAD CONCORD | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7241 | PARRAMATTA ROAD FIVE DOCK | E | N | 2022-07 to 2026-09 | ACTIVE | Five Dock - Parramatta Road and Arlington Street | exact | -33.872838,151.125958 | 60@7m |
| 7242 | PARRAMATTA ROAD GRANVILLE | E | N | 2022-07 to 2026-09 | ACTIVE | Granville - Parramatta Road and Good Street | exact | -33.830543,151.012097 | 60@4m/50@23m |
| 7210 | PARRAMATTA ROAD HABERFIELD | E | N | 2022-07 to 2026-09 | ACTIVE | Haberfield - Parramatta Road and Sloane Street | exact | -33.887387,151.141724 | 60@4m |
| 7262 | PARRAMATTA ROAD HOMEBUSH | W | N | 2022-11 to 2026-09 | ACTIVE; NEW since data start? first 2022-11 | - | none | - | - |
| 7156 | PARRAMATTA ROAD PETERSHAM | W | N | 2022-07 to 2026-09 | ACTIVE | Leichhardt / Petersham - Parramatta Road and West Street | exact | -33.889916,151.149349 | 60@4m/50@28m |
| 7161 | PENNANT HILLS ROAD PENNANT HILLS (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Pennant Hills - Pennant Hills Road and Beecroft Road (school zone) | probable | -33.740444,151.060871 | 70@4m/60@16m/80@28m/SZ40 |
| 7177 | PENNANT HILLS ROAD THORNLEIGH | N | N | 2022-07 to 2026-09 | ACTIVE | Thornleigh - Pennant Hills Road and The Comenarra Parkway / Parkes Street | exact | -33.731005,151.080715 | 70@5m |
| 7421 | PENNANT HILLS ROAD WAHROONGA | S | N | 2025-07 to 2026-09 | ACTIVE; NEW since data start? first 2025-07 | - | none | - | - |
| 7165 | PENNANT HILLS ROAD WEST PENNANT HILLS | N | N | 2022-07 to 2026-09 | ACTIVE | West Pennant Hills - Pennant Hills Road and Eaton Road | exact | -33.754204,151.047587 | 70@6m/50@23m |
| 7309 | PENNANT STREET CASTLE HILL | N | N | 2022-07 to 2026-09 | ACTIVE | Castle Hill - Pennant St and Castle Street | exact | -33.731368,151.003974 | 60@2m/50@27m |
| 7426 | PITT STREET HAYMARKET | W | N | 2025-01 to 2026-09 | ACTIVE; NEW since data start? first 2025-01 | Haymarket - George Street and Pitt Street | exact | -33.882861,151.203981; -33.882643,151.204378 | 40HP@5m; 40HP@5m/20@21m |
| 7194 | PITTWATER ROAD DEE WHY | S | N | 2022-07 to 2026-09 | ACTIVE | Dee Why - Pittwater Road and Harbord Road | exact | -33.758421,151.280916 | 60@0m |
| 7337 | POLDING STREET SMITHFIELD | E | N | 2022-07 to 2026-09 | ACTIVE | Fairfield Heights - Polding Street and The Boulevarde | possible | -33.861903,150.938796 | 60@6m/50@20m |
| 7219 | PRINCES HIGHWAY FIGTREE | N | N | 2022-07 to 2026-09 | ACTIVE | Figtree - Princes Highway and O'Briens Rd | exact | -34.438881,150.857774 | 60@9m |
| 7132 | PRINCES HIGHWAY ROCKDALE | N | N | 2022-07 to 2026-09 | ACTIVE | Rockdale - Princes Highway and Bay Street | exact | -33.953070,151.137407; -33.952803,151.137707 | 60@7m; 60@4m |
| 7131 | PRINCES HIGHWAY ROCKDALE | S | N | 2022-07 to 2026-09 | ACTIVE | Rockdale - Princes Highway and Bay Street | exact | -33.953070,151.137407; -33.952803,151.137707 | 60@7m; 60@4m |
| 7317 | PRINCES HIGHWAY ST PETERS | NW | N | 2022-07 to 2026-09 | ACTIVE | St Peters - Princes Highway and Sydney Park Road | exact | -33.907357,151.181345 | 40HP@7m |
| 7217 | PRINCES HIGHWAY UNANDERRA | S | N | 2022-07 to 2026-09 | ACTIVE | Unanderra - Princes Highway and Five Islands Road | exact | -34.449572,150.849659 | 60@13m |
| 7400 | PRINCES HIGHWAY WOLLONGONG | W | N | 2023-12 to 2026-09 | ACTIVE; NEW since data start? first 2023-12 | Wollongong - Princes Highway and Gladstone Avenue | exact | -34.425962,150.887237 | 50@22m |
| 7257 | PRINCES HWY DAPTO | N | N | 2022-08 to 2026-09 | ACTIVE | - | none | - | - |
| 7136 | PROSPECT HIGHWAY BLACKTOWN | N | N | 2026-04 to 2026-09 | ACTIVE; NEW since data start? first 2026-04 | Prospect - Prospect Highway and Blacktown Road | exact | -33.792650,150.919140 | 60@2m |
| 7271 | PUNCHBOWL ROAD LAKEMBA | W | N | 2022-07 to 2026-09 | ACTIVE | Lakemba - Punchbowl Road and Wangee Road | exact | -33.911377,151.073162 | 60@6m |
| 7409 | PUNCHBOWL ROAD WILEY PARK | W | N | 2024-08 to 2026-09 | ACTIVE; NEW since data start? first 2024-08 | - | none | - | - |
| 7288 | QUEEN STREET WOOLLAHRA | E | N | 2022-07 to 2026-09 | ACTIVE | Woollahra - Queen Street and Ocean Street | exact | -33.887909,151.239713 | 50@7m |
| 7293 | RAILWAY PARADE BURWOOD | W | N | 2024-06 to 2026-09 | ACTIVE; NEW since data start? first 2024-06 | Burwood - Railway Parade and Burwood Road | exact | -33.877479,151.103949 | 40HP@2m |
| 7240 | RAMSAY STREET HABERFIELD | S | N | 2022-07 to 2026-09 | ACTIVE | Haberfield - Ramsay Street and Wattle Street | exact | -33.874850,151.134598 | 60@4m |
| 7233 | RAWSON ROAD GUILDFORD | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7336 | RAWSON STREET AUBURN | E | N | 2022-07 to 2026-09 | ACTIVE | Auburn - Rawson Street and Station Road OR Auburn - Rawson Street and Northumberland Road | ambiguous | -33.850038,151.034960 OR -33.849101,151.033268 | 50@9m OR 50@8m |
| 7300 | RAWSON STREET AUBURN | W | N | 2022-07 to 2026-09 | ACTIVE | Auburn - Rawson Street and Station Road OR Auburn - Rawson Street and Northumberland Road | ambiguous | -33.850038,151.034960 OR -33.849101,151.033268 | 50@9m OR 50@8m |
| 7312 | RICHMOND ROAD DEAN PARK | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7263 | RICKARD ROAD BANKSTOWN | E | N | 2022-09 to 2026-09 | ACTIVE | - | none | - | - |
| 7322 | ROBERTS ROAD GREENACRE | N | N | 2022-07 to 2026-09 | ACTIVE | Greenacre - Roberts Road and Juno Parade | exact | -33.908191,151.064180 | 70@3m |
| 7235 | ROBERTS ROAD GREENACRE | S | N | 2022-07 to 2026-09 | ACTIVE | Greenacre - Roberts Road and Juno Parade | exact | -33.908191,151.064180 | 70@3m |
| 7286 | ROCKY POINT ROAD RAMSGATE | S | N | 2022-07 to 2026-09 | ACTIVE | Ramsgate - Rocky Point Road and Ramsgate Road | exact | -33.984013,151.136393 | 60@5m |
| 7321 | ROOKWOOD ROAD YAGOONA | N | N | 2022-07 to 2026-09 | ACTIVE | Yagoona - Rookwood Road and Brunker Road | exact | -33.898708,151.037513 | 60@8m/70@9m |
| 7294 | ROOKWOOD ROAD YAGOONA | S | N | 2022-07 to 2026-09 | ACTIVE | Yagoona - Rookwood Road and Brunker Road | exact | -33.898708,151.037513 | 60@8m/70@9m |
| 7301 | RUTLEDGE STREET EASTWOOD (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Eastwood - Rutledge Street and Shaftsbury Road (school zone) | exact | -33.793398,151.078279 | 60@5m/SZ40 |
| 7204 | RYDE ROAD HUNTERS HILL | W | N | 2022-07 to 2026-09 | ACTIVE | Gladesville /Hunters Hill - Ryde Road and Pittwater Road | exact | -33.824305,151.132611 | 60@8m/50@23m |
| 7184 | RYDE ROAD WEST PYMBLE | S | N | 2022-07 to 2026-09 | ACTIVE | West Pymble - Ryde Road and Lady Game Drive | exact | -33.773127,151.135317 | 70@6m/60@21m |
| 7209 | SACKVILLE STREET CANLEY VALE | N | N | 2022-07 to 2026-09 | ACTIVE | Canley Vale - Canley Vale Road and Sackville Street | exact | -33.885554,150.936860 | 60@9m |
| 7314 | SACKVILLE STREET FAIRFIELD | S | N | 2022-07 to 2026-09 | ACTIVE | Fairfield - Sackville Street and Hamilton Road | exact | -33.872813,150.945605 | 60@5m/50@13m |
| 7410 | SHELLHARBOUR ROAD WARILLA | S | N | 2025-01 to 2026-09 | ACTIVE; NEW since data start? first 2025-01 | - | none | - | - |
| 7349 | SHERBROOKE STREET ROOTY HILL | E | N | 2022-07 to 2026-09 | ACTIVE | Rooty Hill - Sherbrooke Street and Railway Street | exact | -33.770214,150.835065 | 60@0m |
| 7150 | SHOWGROUND ROAD CASTLE HILL | W | N | 2022-07 to 2026-09 | ACTIVE | Castle Hill - Showground Road and Victoria Avenue | exact | -33.724767,150.977632 | 60@0m |
| 7112 | SILVERWATER ROAD AUBURN | N | N | 2022-07 to 2026-09 | ACTIVE | Silverwater / Auburn - Silverwater Road and M4 Westbound On Ramp | exact | -33.842082,151.044495; -33.843557,151.043127 | 70@7m/90V@9m; 70@10m |
| 7113 | SILVERWATER ROAD AUBURN | S | N | 2022-07 to 2026-09 | ACTIVE | Silverwater / Auburn - Silverwater Road and M4 Westbound On Ramp | exact | -33.842082,151.044495; -33.843557,151.043127 | 70@7m/90V@9m; 70@10m |
| 7345 | SILVERWATER ROAD SILVERWATER | S | N | 2022-07 to 2026-09 | ACTIVE | Silverwater / Auburn - Silverwater Road and M4 Westbound On Ramp OR Silverwater - M4 Eastbound Off Ramp and Silverwater Road OR Silverwater - Silverwater Road and Holker Street | ambiguous | -33.842082,151.044495; -33.843557,151.043127 OR -33.842261,151.044364 OR -33.830189,151.049643 | 70@7m/90V@9m; 70@10m OR 70@7m/90V@12m OR 70@1m/60@7m/50@20m |
| 7147 | SMITHFIELD RD GREENFIELD PARK | E | N | 2022-07 to 2026-09 | ACTIVE | St Johns Park - Smithfield Road and Transitway/Myrtle Road and Canley Vale Road | possible | -33.872680,150.900030 | 60@0m/50@15m |
| 7121 | SOUTH DOWLING STREET PADDINGTON | S | N | 2022-07 to 2026-09 | ACTIVE | Surry Hills - South Dowling Street and Fitzroy Street / Moore Park Road | probable | -33.886275,151.217811; -33.885784,151.218139 | 60@10m/80@16m/40@20m/40LT@22m; 60@2m/80@8m |
| 7266 | SOUTH DOWLING STREET SURRY HILLS | N | N | 2022-07 to 2026-09 | ACTIVE | Surry Hills - South Dowling Street and Fitzroy Street / Moore Park Road OR Redfern / Surry Hills - Cleveland Street and South Dowling Street | ambiguous | -33.886275,151.217811; -33.885784,151.218139 OR -33.891974,151.216463 | 60@10m/80@16m/40@20m/40LT@22m; 60@2m/80@8m OR 50@9m/40@19m/60@26m |
| 7120 | SOUTH DOWLING STREET SURRY HILLS | N | N | 2022-07 to 2026-09 | ACTIVE | Surry Hills - South Dowling Street and Fitzroy Street / Moore Park Road OR Redfern / Surry Hills - Cleveland Street and South Dowling Street | ambiguous | -33.886275,151.217811; -33.885784,151.218139 OR -33.891974,151.216463 | 60@10m/80@16m/40@20m/40LT@22m; 60@2m/80@8m OR 50@9m/40@19m/60@26m |
| 7291 | SPRINGHILL ROAD (NORTHBOUND) SPRING HILL | N | N | 2022-07 to 2026-09 | ACTIVE | Coniston - Springhill Road and Masters Road | probable | -34.446146,150.875730; -34.446028,150.876095 | 80@5m; 80@3m |
| 7292 | SPRINGHILL ROAD (SOUTHBOUND) SPRING HILL | S | N | 2022-07 to 2026-09 | ACTIVE | Coniston - Springhill Road and Masters Road | probable | -34.446146,150.875730; -34.446028,150.876095 | 80@5m; 80@3m |
| 7248 | STACEY STREET BANKSTOWN | N | N | 2022-07 to 2026-09 | ACTIVE | Bankstown - Stacey Street and Hume Highway | exact | -33.906498,151.039902 | 60@28m |
| 7226 | STEWART ST BATHURST | N | N | 2022-07 to 2026-09 | ACTIVE | Bathurst - Stewart Street/Great Western Highway and Keppel Street | exact | -33.415180,149.572990 | 60@1m/50@1m |
| 7320 | STONEY CREEK ROAD KINGSGROVE | E | N | 2022-07 to 2026-09 | ACTIVE | Hurstville - Stoney Creek Road and Patrick Street | possible | -33.951196,151.091380 | 60@3m |
| 7193 | SUNNYHOLT ROAD BLACKTOWN | S | N | 2022-07 to 2026-09 | ACTIVE | Blacktown - Sunnyholt Road and Newton Road | exact | -33.771465,150.911757 | 60@3m |
| 7202 | SYDENHAM ROAD MARRICKVILLE | E | N | 2022-07 to 2026-09 | ACTIVE | Marrickville - Sydenham Road and Victoria Road | exact | -33.909568,151.162081 | 50@8m/40@21m |
| 7245 | THE BOULEVARDE WILEY PARK (Z) | W | Y | 2022-07 to 2026-09 | ACTIVE | Wiley Park - The Boulevarde and King Georges Road (school zone) | exact | -33.922829,151.068784 | 60@25m/SZ40 |
| 7279 | THE HORSLEY DRIVE FAIRFIELD | N | N | 2022-07 to 2026-09 | ACTIVE | Fairfield - The Horsley Drive and Polding Street | exact | -33.864415,150.954514 | 60@7m |
| 7281 | THE HORSLEY DRIVE SMITHFIELD | W | N | 2022-07 to 2026-09 | ACTIVE | Smithfield - Cumberland Highway and The Horsley Drive OR Smithfield - The Horsley Drive and Gipps Street | ambiguous | -33.853333,150.941174 OR -33.850998,150.922517 | 70@1m OR 60@13m |
| 7310 | THIRD AVENUE BLACKTOWN | E | N | 2022-07 to 2026-09 | ACTIVE | Blacktown - Third Avenue and Richmond Road OR Blacktown - Third Avenue and Prince Street | ambiguous | -33.766371,150.905782 OR -33.765399,150.910773 | 60@2m OR 60@8m/50@28m |
| 7327 | THIRD AVENUE BLACKTOWN | S | N | 2022-07 to 2026-09 | ACTIVE | Blacktown - Third Avenue and Richmond Road OR Blacktown - Third Avenue and Prince Street | ambiguous | -33.766371,150.905782 OR -33.765399,150.910773 | 60@2m OR 60@8m/50@28m |
| 7289 | THOMAS STREET WALLSEND | W | N | 2022-07 to 2026-09 | ACTIVE | Wallsend - Thomas Street and Metcalfe Street | exact | -32.906082,151.671472 | 60@7m/50@27m |
| 7215 | TUDOR STREET HAMILTON | W | N | 2022-07 to 2026-09 | ACTIVE | Hamilton - Tudor Street and Beaumont Street | exact | -32.924204,151.747260 | 60@6m/50@20m |
| 7330 | VICTORIA ROAD DRUMMOYNE | N | N | 2022-07 to 2026-09 | ACTIVE | Drummoyne - Victoria Road and Lyons Road | exact | -33.851198,151.154595 | 60@10m/50@27m |
| 7298 | VICTORIA ROAD ERMINGTON | E | N | 2022-07 to 2026-09 | ACTIVE | Ermington / Rydalmere - Victoria Road and Spurway Street | exact | -33.814233,151.055999 | 70@5m/50@29m |
| 7107 | VICTORIA ROAD PARRAMATTA | W | N | 2024-08 to 2026-09 | ACTIVE; NEW since data start? first 2024-08 | Parramatta - Victoria Road and Church Street | exact | -33.808757,151.005638 | 60@2m/40HP@30m |
| 7163 | VICTORIA ROAD ROZELLE | E | N | 2022-07 to 2026-09 | ACTIVE | Rozelle - Victoria Road and Evans Street OR Rozelle - Victoria Road and Wellington Street | ambiguous | -33.865183,151.172069 OR -33.862815,151.168722 | 60@6m/40HP@24m OR 60@14m/40HP@29m |
| 7404 | VICTORIA ROAD ROZELLE | E | N | 2024-03 to 2026-09 | ACTIVE; NEW since data start? first 2024-03 | Rozelle - Victoria Road and Evans Street OR Rozelle - Victoria Road and Wellington Street | ambiguous | -33.865183,151.172069 OR -33.862815,151.168722 | 60@6m/40HP@24m OR 60@14m/40HP@29m |
| 7203 | VICTORIA ROAD RYDE | W | N | 2022-07 to 2026-09 | ACTIVE | Ryde - Victoria Road and Devlin Street | exact | -33.816027,151.103451 | 60@2m/50@29m |
| 7231 | VICTORIA ROAD WEST RYDE | E | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7201 | VICTORIA STREET SMITHFIELD | W | N | 2022-07 to 2026-09 | ACTIVE | Smithfield - Victoria Street and Hassall Street | exact | -33.848982,150.922953 | 60@7m |
| 7124 | WALDRON ROAD CHESTER HILL | E | N | 2022-07 to 2026-09 | ACTIVE | Chester Hill - Waldron Road and Hector Street | exact | -33.883675,151.006490; -33.883855,151.007272 | 60@3m/50@11m; 60@3m |
| 7125 | WALDRON ROAD SEFTON | W | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7401 | WALLARAH ROAD KANWAL | E | N | 2023-12 to 2026-09 | ACTIVE; NEW since data start? first 2023-12 | - | none | - | - |
| 7222 | WARRINGAH FWY 75M STH-MOUNT ST NORTH SYDNEY | S | N | 2023-11 to 2023-11 | INACTIVE? last offence 2023-11; NEW since data start? first 2023-11 | - | none | - | - |
| 7138 | WATERLOO ROAD GREENACRE | N | N | 2022-07 to 2026-09 | ACTIVE | Greenacre - Waterloo Road and Rawson Street | exact | -33.902790,151.057920 | 60@1m/50@1m/SZ40 |
| 7188 | WATTLE STREET ULTIMO | N | N | 2022-07 to 2026-09 | ACTIVE | Ultimo - Wattle Street and William Henry Street | exact | -34.383540,150.900310 | 80@0m |
| 7152 | WELLINGTON STREET ROZELLE | S | N | 2022-07 to 2026-09 | ACTIVE | Rozelle - Victoria Road and Wellington Street | exact | -33.862815,151.168722 | 60@14m/40HP@29m |
| 7402 | WENTWORTH AVENUE EASTGARDENS | W | N | 2023-11 to 2026-09 | ACTIVE; NEW since data start? first 2023-11 | Mascot - Wentworth Avenue and Sutherland Steet | possible | -33.933940,151.199250 | 60@3m |
| 7116 | WENTWORTH AVENUE MASCOT | W | N | 2022-07 to 2026-09 | ACTIVE | Mascot - Wentworth Avenue and Sutherland Steet | exact | -33.933940,151.199250 | 60@3m |
| 7185 | WICKS ROAD NORTH RYDE | N | N | 2022-07 to 2026-09 | ACTIVE | North Ryde - Epping Road and Wicks Road | exact | -33.791496,151.130017 | 60@13m/50@23m |
| 7296 | WILLIAM STREET DARLINGHURST | W | N | 2022-07 to 2026-09 | ACTIVE | Woolloomooloo - William Street and Crown Street | probable | -33.874377,151.216514 | 80V@2m/80@6m/50@6m/40@23m |
| 7247 | WILLOUGHBY ROAD NAREMBURN | S | N | 2022-07 to 2026-09 | ACTIVE | Naremburn - Willoughby Road and Chandos Street | exact | -33.821865,151.201011 | default 50@6m |
| 7220 | WINDANG ROAD WINDANG | N | N | 2022-07 to 2026-09 | ACTIVE | Windang - Windang Road and Boronia Avenue | exact | -34.530135,150.868979 | 60@7m |
| 7265 | WINDSOR ROAD (NORTHBOUND) KELLYVILLE RIDGE | N | N | 2022-07 to 2026-09 | ACTIVE | - | none | - | - |
| 7146 | WINDSOR ROAD BAULKHAM HILLS | E | N | 2022-07 to 2026-09 | ACTIVE | Baulkham Hills - Windsor Road and Seven Hills Road | exact | -33.761517,150.992617; -33.762283,150.993083 | 60@5m; 60@5m/50@26m |
| 7145 | WINDSOR ROAD BAULKHAM HILLS | W | N | 2022-07 to 2025-04 | INACTIVE? last offence 2025-04 | - | none | - | - |
| 7264 | WINDSOR ROAD ROUSE HILL | S | N | 2022-10 to 2026-09 | ACTIVE; NEW since data start? first 2022-10 | - | none | - | - |
| 7328 | WOODSTOCK AVENUE ROOTY HILL | W | N | 2022-07 to 2026-09 | ACTIVE | Rooty Hill - Woodstock Avenue and M7 Northbound Off Ramp | exact | -33.760375,150.846828 | 60@7m/100@13m |
| 7144 | WOODVILLE ROAD GRANVILLE | N | N | 2023-06 to 2026-09 | ACTIVE; NEW since data start? first 2023-06 | Granville - Woodville Road and M4 On Ramp | exact | -33.827880,151.004610 | 60@6m |
| 7173 | WOODVILLE ROAD GUILDFORD | N | N | 2022-07 to 2026-09 | ACTIVE | Guildford - Woodville Road and Guildford Road | exact | -33.855902,150.994868 | 70@8m |
| 7341 | WOODVILLE ROAD MERRYLANDS | N | N | 2022-07 to 2026-09 | ACTIVE | Merrylands - Woodville Road and Louis Street | exact | -33.839817,150.999356 | 70@4m |
| 7127 | WOODVILLE ROAD VILLAWOOD | S | N | 2022-07 to 2026-09 | ACTIVE | Lansdowne / Villawood - Woodville Road and Hume Highway | exact | -33.888925,150.973789; -33.889992,150.972628 | 60@8m; 60@8m/70@14m |


### Inferences
- For the app, the safest current fixed and red-light database is: every Revenue NSW code with Status ACTIVE and Match = exact or probable, using the 2021 coordinates. Codes with Match = none (mostly tunnel cameras and new 2022–26 intersections) need coordinates from another official source, or should be left without coordinates.
- Several 2021 red-light rows are matched by more than one Revenue code. This is expected: each approach to an intersection has its own code and direction (for example Coffs Harbour, Pacific Highway, has codes 7343, 7344 and 7412).
- Tunnel fixed cameras (Cross City Tunnel 9613/9614, Eastern Distributor 9635/9641, Lane Cove Tunnel 9655/9656, M5 East 9551/9552, Sydney Harbour Tunnel 9800/9801, NorthConnex 9852/9853, WestConnex M4 East 6216/6217, M8 9854–9857, Rozelle Interchange 9858–9861) are active in 2026 but were **not in the Nov-2021 GPS file**, so there are no official coordinates for them.

### Gaps
- No official 2026 coordinates or cross streets. The TfNSW pages that carry them (speed-camera-locations-map and the fixed and red-light list pages) returned HTTP 403, and I did not use Wayback, as instructed.
- Revenue NSW suburb names are sometimes the neighbouring suburb (Old Toongabbie/Toongabbie, Chester Hill/Old Guildford, Kurrajong Heights/Kurrajong), and red-light codes name the *approach road*, not the intersection. That is why about 25% of codes remain ambiguous or unmatched.
- For two-camera 2021 rows, the dataset does not say which coordinate belongs to which direction.
- A code with no notices in a month is not proof the camera was off: low-violation sites can go a month without a notice. "INACTIVE?" flags are therefore indicative only.

## Which sites were added, decommissioned or relocated since the Nov-2021 dataset?

### Takeaway
I found no official 2022–26 change log; TfNSW pages were blocked. The Revenue NSW data shows **12 fixed codes and 49 red-light codes** whose first penalty notice falls after the data starts (Jul 2022). They are listed below and in the master table as "NEW since data start? first YYYY-MM". The new fixed sites include the Grand Parade (Brighton-Le-Sands/Sans Souci), Pacific Highway Cowan, Bolong Road Bomaderry, and the M8 and Rozelle Interchange tunnels. About 11 fixed and 8 red-light 2021 rows have no 2022–26 notices at all, and 16 codes stopped issuing notices before Jul 2026; these are probably decommissioned or relocated.

### Cited Findings
New or first-seen sites, with date of first penalty notice from [Revenue NSW dfosf-001 FY2022-23..FY2026-27](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics) and corroboration where found:
- Fixed: The Grand Parade, Brighton-Le-Sands (9647 NB, from 2023-12; 9649 SB, from 2024-02) and Sans Souci (9653 N+S, from 2024-01). Search snippet: "Three fixed cameras" announced, "expected to be fully operational in early 2024" — [NSW Gov media release (search result only; host unreachable)](https://www.nsw.gov.au/media-releases/new-fixed-speed-cameras-for-grand-parade)
- Fixed: Pacific Highway, Cowan (9673 E+W, from 2025-08). Search snippet of a TfNSW April 2025 notice: a fixed camera "will be operational in May 2025" — [Hornsby Council community notification PDF (search result only)](https://www.hornsby.nsw.gov.au/__data/assets/pdf_file/0007/283498/Fixed-Speed-Camera-Installation-Old-Pac-Hwy-community-notification-April-2025.pdf)
- Fixed: Rozelle Interchange tunnels (9858 S, 9859 N, 9860 W, 9861 E, from 2024-01/02); WestConnex M8 tunnel, Newtown (9856 S from 2023-01, 9857 N from 2023-02); Bolong Road, Bomaderry (9594 E+W, from 2023-04); Pennant Hills Road, Oatlands (Z) (9658 S, from 2022-12) — [Revenue NSW](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics)
- Red-light: Milperra Road, Milperra (7353 E, from 2024-02); the search snippet names Milperra Road and Nancy Ellis Leebold Drive, from 6 Feb 2024 — [TfNSW media release (search result; 403)](https://www.transport.nsw.gov.au/news-and-events/media-releases/dangerous-driving-given-red-light-milperra-1)
- Red-light: Punchbowl Road, Wiley Park (7409 W, from 2024-08); snippet: Punchbowl Road and King Georges Road, start 9 Jul 2024 — [TfNSW media release](https://www.transport.nsw.gov.au/news-and-events/media-releases/dangerous-driving-given-red-light-wiley-park-0)
- Red-light: Chapel Road, Bankstown (7405 N, from 2024-10); snippet: Chapel Road and Macauley Avenue, start 3 Sep 2024 — [TfNSW media release](https://www.transport.nsw.gov.au/news-and-events/media-releases/dangerous-driving-given-red-light-bankstown-1)
- Red-light: Five Islands Road, Port Kembla (7411 N, from 2024-10); snippet: Five Islands Road and Flinders Street, installed Oct 2024 — [Region Illawarra, 6 Feb 2025](https://regionillawarra.com.au/new-red-light-camera-busts-thousands-of-drivers-in-first-months/50574/)
- Red-light: Elizabeth Drive, Ashcroft (7422 N, from 2025-07); snippet: Elizabeth Drive and Maxwells Avenue, June 2025 — [Big Rigs, 3 Jun 2025](https://bigrigs.com.au/2025/06/03/new-red-light-speed-camera-for-busy-intersection-in-sydneys-south-west/)
- Red-light: Pennant Hills Road, Wahroonga (7421 S, from 2025-07); snippet: Pennant Hills Road and Pacific Motorway, from June 2025 — [TfNSW media release](https://www.transport.nsw.gov.au/news-and-events/media-releases/dangerous-driving-given-red-light-wahroonga)
- Red-light: Lane Cove Road, North Ryde (7425 S, from 2025-09); snippet: Lane Cove Road and Twin Road, from Aug 2025 — [TfNSW media release](https://www.transport.nsw.gov.au/news-and-events/media-releases/dangerous-driving-given-red-light-north-ryde%C2%A0)
- Red-light: Pacific Highway, Heatherbrae (7417 N, from 2025-12); snippet: Pacific Highway and Hank Street, from 5 Dec 2025 — [News Of The Area, 20 Jan 2026](https://portstephens.newsofthearea.com.au/?p=167273)
- Red-light: Victoria Road, Rozelle (7404 E, from 2024-03); snippet: Victoria Road and Darling Street, starting "December 18" (year not given) — [Fully Loaded](https://www.fullyloaded.com.au/transport-for-nsw-installs-red-light-speed-camera-in-rozelle/)
- Red-light: New England Highway, Dunolly (Singleton) (7259 W, from 2022-10); snippet: New England Highway and Bridgman Road, start 28 Oct 2022 — [Dave Layzell MP](https://www.davelayzell.com.au/post/dangerous-driving-given-the-red-light-at-singleton), [Hunter River Times](https://hunterrivertimes.com.au/red-light-speed-camera-installed-at-singleton/)
- Other red-light codes first seen in 2022–26, with no corroborating article found (Revenue NSW only): 7424 Australia Ave, Sydney Olympic Park (2025-12); 7354 Banks Rd, Hinchinbrook (2024-04); 7413 Burwood Rd, Croydon Park (2024-09); 7115 Canterbury Rd, Roselands (2023-08); 7114 Canterbury Rd, Wiley Park (2023-07); 7250 Chisholm Rd, Auburn (2023-03); 7403 Cumberland Hwy, Fairfield West (2023-12); 7418 Davis Rd, Wetherill Park (2025-06); 7255 Donald St, Hamilton (2022-11); 7415 Donnison St, Gosford (2025-12); 7429 East St, Nowra (2026-08); 7352 Elizabeth Dr, Liverpool (2023-09); 7261 Harris St, Ultimo (2022-12); 7267 Henry Lawson Dr, Milperra (2022-12); 7407 Hume Hwy, Cabramatta (2024-10); 7408 Hume Hwy, Casula (2025-01); 7406 Hume Hwy, Yagoona (Z) (2024-11); 7142 Jersey Rd, Plumpton (2024-09); 7419 Johnston St, Annandale (2026-05); 7350 Lanyon Dr, Jerrabomberra (2023-06); 7351 Mulgoa Rd, Penrith (2023-08); 7423 Narellan Rd, Campbelltown (2026-06); 7311 Old Windsor Rd, Glenwood (2026-01); 7412 Pacific Hwy, Coffs Harbour (2024-11); 7414 Pacific Hwy, Lane Cove North (2024-11); 7416 Pacific Hwy, Hamlyn Terrace (2026-04); 7191 Parker St, South Penrith (2025-09); 7262 Parramatta Rd, Homebush (2022-11); 7426 Pitt St, Haymarket (2025-01); 7400 Princes Hwy, Wollongong (2023-12); 7136 Prospect Hwy, Blacktown (2026-04); 7293 Railway Pde, Burwood (2024-06); 7410 Shellharbour Rd, Warilla (2025-01); 7107 Victoria Rd, Parramatta (2024-08); 7401 Wallarah Rd, Kanwal (2023-12); 7402 Wentworth Ave, Eastgardens (2023-11); 7264 Windsor Rd, Rouse Hill (2022-10); 7144 Woodville Rd, Granville (2023-06). Codes active in 2026 but not in the 2021 list, and without a date marker (already active Jul 2022): for example 7232/7260 Archbold Rd, Lindfield/Roseville; 7237 Camden Valley Way, Prestons; 7265 Windsor Rd, Kellyville Ridge; 7312 Richmond Rd, Dean Park; 7257 Princes Hwy, Dapto; 7258 Pacific Hwy, Lake Munmorah — [Revenue NSW](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics)
- Codes that stopped issuing notices before Jul 2026 (last month shown): fixed 9567 and 9538 Giinagay Way, Valla (2024-03); 9612 Princes Hwy, Brogo (2024-03); 9662 M1 Princes Mwy, North Wollongong SB (2025-04); 9600 Princes Hwy NB, Kogarah (Z) (2025-08; 9601 continues); 9644 Henry Lawson Dr, Picnic Point (2026-05); 9650 Spit Rd, Beauty Point (2026-05); 9666 Great Western Hwy, Mount Victoria (2026-03). Red-light 7243 George St, Haymarket S (2022-12); 7222 Warringah Fwy, North Sydney (one month only, 2023-11); 7140 Anzac Pde, Moore Park N (2024-06); 7158 O'Riordan St, Mascot N (2025-04); 7145 Windsor Rd, Baulkham Hills W (2025-04); 7175 Bestic St, Rockdale W (2025-09); 7212 Pacific Hwy, Hamilton East N (2025-11); 7412 Pacific Hwy, Coffs Harbour N (2026-05) — [Revenue NSW](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics)

#### 2021 TfNSW rows with no matching Revenue NSW code (Jul 2022 – Sep 2026)
These are probably decommissioned, renamed or relocated before or during 2022–26. They are **not** confirmed current, and the coordinates date from Nov 2021. Some may actually be among the unmatched or "none" Revenue codes above, for example Fixed Ben Lomond against 9563 New England Hwy, Glencoe, or Red Mays Hill against 7118/7119 Great Western Hwy, Westmead/Merrylands.

| Type | 2021 suburb | 2021 road/segment | SZ | Lat,Lon (2021) | Speed zone at coords (derived) |
|---|---|---|---|---|---|
| Fix | Ben Lomond | New England Highway, between Ross Road and Ben Lomond Road | N | -30.006089,151.715005 | 100@18m |
| Fix | Brogo | Princes Highway, between Pioneer Close and Brogo River | N | -36.519191,149.829115 | 100@2m |
| Fix | Hartley | Great Western Highway, between Mid Hartley Road and Blackmans Creek Road | N | -33.543896,150.170696 | 80@7m |
| Fix | Hungry Head | Pacific Highway, between Boundary Road and Ballards Road | N | -30.554871,153.006310 | 90@8m |
| Fix | Korora | Pacific Highway, between Bruxner Park Road and Korora Basin Road | N | -30.256407,153.129346 | 60V@3m |
| Fix | New Italy | Pacific Highway, between New Italy Road and Turners Road | N | -29.158792,153.293078; -29.158693,153.293021 | 110@6m; 110@6m/100@22m |
| Fix | North Macksville | Pacific Highway, between Bellview Drive and Ferry Street | N | -30.696966,152.965212 | 100@7m |
| Fix | North Macksville | Pacific Highway, between Florence Wilmont Drive and Watt Creek | N | -30.703546,152.923003 | 50@12m |
| Fix | Queanbeyan | Lanyon Drive, between Tompsitt Drive and Hoover Road | N | -35.373707,149.194403 | 80@8m |
| Fix | Terrigal | Terrigal Drive, between Brunswick Road and Bellbird Avenue (school zone) | Y | -33.439516,151.431087 | 60@3m/SZ40 |
| Fix | Valla Beach | Pacific Highway, between Valla Beach Road and Oyster Creek | N | -30.583500,153.002483 | 90@9m |
| Red | Chullora | Rookwood Road and Muir Road | N | -33.891965,151.038551 | 80@5m/60@27m |
| Red | Werrington | Great Western Highway and Werrington Road | N | -33.768190,150.757890 | 80@1m/60@2m |
| Red | Oakhurst | Richmond Road and M7 Southbound On Ramp | N | -33.733478,150.849438 | 70@4m/100@9m |
| Red | Fairfield Heights | Hamilton Road and The Boulevarde | N | -33.872385,150.937200 | 50@1m |
| Red | Mays Hill | Great Western Highway and Coleman Street / Hawkesbury Road | N | -33.818278,150.982002; -33.817640,150.981714 | 60@0m/50@23m; 60@7m/50@27m |
| Red | Moore Park | Anzac Parade and Moore Park Rd | N | -33.886839,151.219003; -33.886409,151.219003 | 60@2m/80@6m/40@22m; 60@8m/40@22m |
| Red | Penrith | Parker Street and Jamison Street | N | -33.764347,150.709649 | 70@10m |
| Red | Albury | Borella Road and Young Street | N | -33.879200,151.195705 | 50@8m/40LT@14m/40@18m |

### Inferences
- The Pacific Highway fixed sites at Hungry Head, Valla Beach, Korora, New Italy and North Macksville (Bellview Drive) sit on alignments that the Pacific Highway upgrade has bypassed. Their absence from 2022–26 data, and Valla's last notices in Mar 2024, suggest they were removed or are not enforcing. This is an inference; no decommissioning notice was found.
- **Data-quality warning:** the 2021 red-light row "Albury | Borella Road and Young Street" (no school zone) has coordinates -33.879200,151.195705, which is in inner Sydney (Ultimo/Haymarket), not Albury. Do not use them. The school-zone Albury row (-36.07518,146.92525) is plausible.
- Lanyon Drive: the 2021 *fixed* camera (Queanbeyan, between Tompsitt Drive and Hoover Road) has no fixed-camera notices in 2022–26. A *red-light* code 7350 "Lanyon Drive Jerrabomberra EB" starts in Jun 2023. They may be different installations; this is unverified.

### Gaps
- I could not reach any official decommissioning list. TfNSW publishes changes on the camera-locations pages, which were blocked (403); nsw.gov.au, data.gov.au and council hosts were unreachable from this session.
- Most media-release corroboration above comes from search-result snippets, not full page reads. Dates are as the snippets reported them.

## For each site, what is the speed limit and the direction of enforcement?

### Takeaway
**Direction:** Revenue NSW states the enforced direction for every code (the Dir column, from 2026 data). **Speed limit:** none of the camera sources gives it. The master table's limit comes from my spatial join of the 2021 coordinates to the TfNSW Speed Zones layer (28 Oct 2026), so treat it as derived. School-zone status comes from both the 2021 `SZ?` flag and Revenue NSW "(Z)" codes.

### Cited Findings
- `LOCATION_DETAILS` gives "the direction in which it operates", e.g. "CONCORD ROAD CONCORD WEST SOUTHBOUND", "MULGOA ROAD PENRITH NORTHBOUND" — [Revenue NSW notes](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/penalty-notice-data-notes-dfosf-001-notes.xlsx)
- The Speed Zones dataset has fields `Type` (Ordinary Permanent, Shared, High Pedestrian, School, Variable, Local Traffic, Truck & bus, Wet Weather, School Bus, Toll Plaza, Default), `Status`, `Direction` ('Both Directions'/'One Way') and `Speed`. The ZIP was updated 2026-10-28 and holds 447,542 records in Web Mercator (EPSG:3857) — [TfNSW Open Data Speed Zones](https://opendata.transport.nsw.gov.au/data/dataset/4253a054-b377-4b5b-83d1-71385bb6ff33/resource/72cd0f29-f231-4c8c-a885-595946f3f202/download/speedzones.zip)
- All 348 coordinates in the 2021 dataset had at least one speed-zone polyline within 30 m. Examples: Ashfield, Hume Hwy (SZ): 60 permanent + School 40 at 6 m; Bar Point, M1: 100 One Way (wet weather 90) at 7 m — derived from [Speed Zones](https://opendata.transport.nsw.gov.au/dataset/speed-zones) + [NSW Speed Cameras 2021](https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44)
- A separate TfNSW "School Zones data" ZIP (updated 2026-10-04) and a "school-zone-timetables" dataset exist and could confirm school-zone status — [package_show speed-zones](https://opendata.transport.nsw.gov.au/data/api/3/action/package_show?id=speed-zones)

### Inferences
- At red-light sites the coordinate is at the intersection, so a 30 m buffer often catches the cross street's limit. I list all limits in order of distance; the camera road's limit is usually, but not always, the first.
- Revenue NSW records 2-direction codes (for example 9530 Bangalow Road, Clunes, N and S) as bidirectional enforcement at one site. Single-letter codes are one-way.

### Gaps
- No source gives a speed limit for sites without 2021 coordinates (tunnels and new sites). The tunnel limits are signposted, but I found no citable official figure for them in this session.
- Which 2021 coordinate belongs to which direction, at two-camera sites, is unknown.

## Are there newer versions of the dataset or other official lists?

### Takeaway
No. The TfNSW "NSW Speed Cameras" dataset still holds only the Nov-2021 GPS XLSX and CSVs (CKAN metadata_modified 2023-08-21), and Data.NSW mirrors the same files. The only 2026-current official machine-readable source of *sites* is the Revenue NSW penalty-notice dataset, which has no coordinates. For speed limits, the current source is the TfNSW Speed Zones layer.

### Cited Findings
- CKAN `package_search q=speed camera` on the TfNSW hub returned only `nsw-speed-cameras` (resources: Nov-2021 GPS XLSX, fixed CSV, red-light CSV, and mobile Jan-2022 CSV; all created 2023-08-21) plus fines datasets — [TfNSW CKAN API](https://opendata.transport.nsw.gov.au/data/api/3/action/package_search?q=speed%20camera)
- Data.NSW has `2-nsw-speed-cameras` (modified 2025-02-04), whose resources point back to the same Nov-2021 TfNSW files. The older `list-of-fixed-speed-camera-locations-in-nsw` (2016) links to the retired roadsafety.transport.nsw.gov.au page — [Data.NSW CKAN API](https://data.nsw.gov.au/data/api/3/action/package_search?q=speed%20camera)
- A data.gov.au mirror, nsw-2-nsw-speed-cameras (resource 822d0c13…), appeared in search results; a search summary described its file name as the 2021 one. data.gov.au was unreachable, so this is unverified — [data.gov.au](https://data.gov.au/data/dataset/nsw-2-nsw-speed-cameras/resource/822d0c13-7d40-4520-a294-13e0bb8041c1)
- Revenue NSW publishes `penalty-notice-data-YYYY-YYYY-dfosf-001-YYYY.xlsx` for FY2022-23 to FY2026-27, plus older summary files such as DSF010 for all speeding and red-light camera offences — [Revenue NSW statistics](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics)
- The official TfNSW camera-location pages (https://www.transport.nsw.gov.au/roadsafety/topics-tips/speeding/enforcement-cameras/speed-camera-locations-map and its list pages) returned HTTP 403 to plain curl; not bypassed.

### Inferences
- If the user can open the TfNSW speed-camera-locations-map in a normal browser, it is the authoritative source to fill in coordinates for the "Match = none" codes and to settle the ambiguous matches. A manual export is outside what this session could do.

### Gaps
- I could not check whether the TfNSW map page offers a downloadable KML or GeoJSON (403).
