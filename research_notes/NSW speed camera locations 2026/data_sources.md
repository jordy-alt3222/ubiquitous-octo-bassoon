# Machine-readable data sources for NSW speed camera locations (checked 2026-10-29)

All endpoints below were fetched with a plain `curl` (no custom user agent or headers) on 2026-10-29. HTTP statuses are recorded. Downloads are in `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/research_sources/` (called `$RS/` below). They were parsed only with `python3 -I`.

## What data endpoint, ArcGIS service or file does the official TfNSW "Speed camera locations map" load?

### Takeaway
I could not find out what the official map loads. Every www.transport.nsw.gov.au URL tried returned HTTP 403 (a CloudFront "Request blocked" page), and per the constraints I did not try to get around this. The closest official machine-readable match is a public ArcGIS feature service, `TFNSW_Speed_Cameras_public`, hosted on the NSW Government spatial portal (portal.data.nsw.gov.au). It has three layers (fixed, fixed school-zone, red-light) and its content matches the Open Data Hub files. Its records are all stamped `last_updated` 2021-05-27. I found no evidence that the TfNSW map uses it.

### Cited Findings
- `https://www.transport.nsw.gov.au/roadsafety/topics-tips/speeding/enforcement-cameras/speed-camera-locations-map` → **HTTP 403**, CloudFront "Request blocked" (saved as `$RS/tfnsw_page.html`). — [TfNSW map page](https://www.transport.nsw.gov.au/roadsafety/topics-tips/speeding/enforcement-cameras/speed-camera-locations-map)
- Two other TfNSW pages are also blocked. `https://www.transport.nsw.gov.au/system/files/media/documents/2024/crs_mobile_speed_cameras.pdf` (the official mobile camera PDF, found through search) → **HTTP 403**. The old Centre for Road Safety page `https://roadsafety.transport.nsw.gov.au/speeding/speedcameras/current-locations.html` → **HTTP 403**. — [crs_mobile_speed_cameras.pdf](https://www.transport.nsw.gov.au/system/files/media/documents/2024/crs_mobile_speed_cameras.pdf)
- The Service NSW page "Check a speed camera location" (**HTTP 200**, "Last published: 24 September 2025") only links to the TfNSW map at the URL above. It describes the tool as searching "by location, road, and type of camera" across fixed / school-zone fixed, mobile and red-light cameras. It shows no data endpoint. — [Service NSW](https://www.service.nsw.gov.au/referral/check-a-speed-camera-location)
- **Official ArcGIS feature service (public, no auth):** `https://portal.data.nsw.gov.au/arcgis/rest/services/Hosted/TFNSW_Speed_Cameras_public/FeatureServer` → **HTTP 200**. ArcGIS Server 10.81, capabilities "Query,Sync", maxRecordCount 1000, native SR 3857, serviceItemId `4ecc86969f4c443f907049cd45cf5314`. `copyrightText` and `description` are empty. Its layers are listed below. — [FeatureServer](https://portal.data.nsw.gov.au/arcgis/rest/services/Hosted/TFNSW_Speed_Cameras_public/FeatureServer?f=json)
  - Layer 0 `Fixed_Speed_Cameras`: 64 features, 67 cameras (61×1 + 3×2). All `sz_`='N'.
  - Layer 1 `Fixed_Speed_Cameras_School_Zone`: 39 features, 59 cameras.
  - Layer 2 `Red_light_Speed_Camera`: 201 features, 222 cameras.
  - Fields are the same in all three layers: `sz_, location, cameras (string), road, lat_1, long_1, lat_2, long_2, suburb_town, road_s, objectid, last_updated`.
  - Every record has `last_updated` = `2021-05-27 14:07:05/06`.
  - Queries `.../FeatureServer/{0,1,2}/query?where=1%3D1&outFields=*&outSR=4326&f=geojson` → HTTP 200. Saved as `$RS/pdn_all.geojson`, `$RS/pdn_L1_all.geojson`, `$RS/pdn_L2_all.geojson`, with layer metadata in `$RS/pdn_L0.json`, `$RS/pdn_L1.json`, `$RS/pdn_L2.json`.
- The same Hosted folder (`https://portal.data.nsw.gov.au/arcgis/rest/services/Hosted?f=json`, HTTP 200, 199 services) also contains `TFNSW_Speed_Zones_Public`, `TfNSW_Traffic_Cameras_Public`, `TfNSW_Maritime_Speed_Zone_Public` and `TFNSW_RedBusLane`. No other speed or safety camera service is listed there (e.g. no point-to-point or mobile phone detection camera service). — [Hosted folder](https://portal.data.nsw.gov.au/arcgis/rest/services/Hosted?f=json)
- The service is catalogued as "TfNSW Speed Camera Public" under the organisation "Spatial Services (DCS)". Its notes say "The dataset reviews every year by TFNSW/RMS", give the source as `https://opendata.transport.nsw.gov.au/dataset/nsw-speed-cameras`, and set `license_id` = "notspecified". This comes from the data.gov.au harvest record (`package_show?id=2e89c8c9-a82f-48a1-91b1-4a3ed271f970`, HTTP 200, harvested 2025-08-03, remote_last_updated 2025-07-20). The original data.nsw.gov.au record `1-0216c44800e34c279722262ce5a941e1` returns CKAN "Authorization Error / Access denied" to anonymous `package_show` (HTTP 403). — [data.gov.au record](https://data.gov.au/data/dataset/2e89c8c9-a82f-48a1-91b1-4a3ed271f970); [data.nsw.gov.au record](https://data.nsw.gov.au/data/en/dataset/1-0216c44800e34c279722262ce5a941e1)
- The ArcGIS Online search `https://www.arcgis.com/sharing/rest/search?q=speed%20camera%20NSW&f=json&num=100` → HTTP 200, 8 results. The only NSW speed camera items are owned by `geoplexing_online` (Geoplex, a private GIS firm, not TfNSW). They are two Feature Services, `Fixed_Speed_Cameras` (item efb105ef112b4987bdb11550981cc01f) and `NSW_Speed_Cameras_GPS_Locations` (item 3bd291fda55642c5bdc7a1eb063638d0), plus a web map and the apps "NSW Speed Cameras" 1.0/2.0/3.0. Each feature service has a single layer of 103 fixed cameras, with data last edited 2020-09-25 (dataLastEditDate 1601017510182). The item has no licence or description. They are public (HTTP 200) but **not official**. — [AGOL search](https://www.arcgis.com/sharing/rest/search?q=speed%20camera%20NSW&f=json); [Geoplex FS](https://services.arcgis.com/y3GIulxK3Ox4lXG0/arcgis/rest/services/NSW_Speed_Cameras_GPS_Locations/FeatureServer/0?f=json)
- Two more AGOL searches (`q=TfNSW camera`, and `"speed camera" AND (NSW OR "New South Wales" OR transport)`) returned no TfNSW-owned speed camera item. The NSW results were City of Sydney "Street safety cameras" (CCTV), user copies of Live Traffic traffic cameras, and Maritime web cams. — [AGOL search](https://www.arcgis.com/sharing/rest/search?q=TfNSW%20camera&f=json&num=100)

### Inferences
- The TfNSW map probably uses either the `TFNSW_Speed_Cameras_public` service or a newer internal feed. I cannot confirm which without loading the page in a browser. Because of the 2021 timestamps, the map may hold newer locations than any public feed I found.
- The layer 2 sample record (objectid 908) has `road` "O'Riordan Street at Gardeners Road" but `road_s` "Wentworth Avenue and Sutherland Steet". The two street fields can disagree, so a pipeline should pick one (the CSV's `ROAD/S` equals `road_s`) and spot-check it.

### Gaps
- I could not see the actual network requests made by the TfNSW map page (blocked by the 403). The user could open the page in a normal browser with DevTools → Network and filter for "FeatureServer", "json" or "geojson" to identify it.
- I found no official KML for speed cameras.

## Is there a newer version of the Open Data Hub "nsw-speed-cameras" dataset than November 2021 / January 2022?

### Takeaway
No. As of 2026-10-29 the CKAN record still has the same 4 resources: the November 2021 GPS XLSX, two undated fixed / red-light CSVs and the January 2022 mobile CSV. Metadata was last modified 2023-08-21, which looks like a platform migration touching all resources at the same time. The data.nsw.gov.au and data.gov.au mirrors point to the same files.

### Cited Findings
- `https://opendata.transport.nsw.gov.au/data/api/3/action/package_show?id=nsw-speed-cameras` → **HTTP 200** (saved as `$RS/pkg_show.json`). id `fb34bd89-443a-448c-a4a5-7c8caab70c44`, `metadata_created` 2020-09-15, `metadata_modified` 2023-08-21T09:06:03, `license_id` "cc-by", maintainer "TfNSW Open Data Hub and Developer Portal" (opendataprogram@transport.nsw.gov.au). The notes still point to the retired roadsafety.transport.nsw.gov.au current-locations page. — [CKAN package_show](https://opendata.transport.nsw.gov.au/data/api/3/action/package_show?id=nsw-speed-cameras)
- Resources (all **HTTP 200**, no auth, `last_modified` 2023-08-21 for all). — [Dataset page](https://opendata.transport.nsw.gov.au/dataset/nsw-speed-cameras)
  1. **XLSX**, `.../resource/a5c27c2d-5511-4fe8-863d-06a2d1a5ed10/download/nsw-speed-cameras-gps-locations-november-2021.xlsx` (42,436 B), saved as `$RS/nsw-speed-cameras-gps-locations-november-2021.xlsx`. It has two sheets:
     - "Fixed Speed Cameras": 103 rows, 126 cameras, 39 in school zones.
     - "Red Light Speed Cameras": 201 rows, 222 cameras, 12 in school zones.
     - Headers: `SZ?, SUBURB/TOWN, ROAD/S, Cameras, Lat(1), Long(1), Lat(2), Long(2)` (red-light adds `Lat(3), Long(3)`).
  2. **CSV** Fixed Speed Cameras, `.../resource/bcf2f6f4-ecfb-40e1-a807-0d5eb5f51507/download/fixed-speed-cameras_1.csv` (11,948 B), saved as `$RS/fixed-speed-cameras_1.csv`. 103 locations and 126 coordinate pairs, with the same headers as the XLSX.
  3. **CSV** Red Light Speed Cameras, `.../resource/debd70a9-f9f4-471c-81ae-c84098576ea6/download/red-light-speed-cameras_1.csv` (16,717 B), saved as `$RS/red-light-speed-cameras_1.csv`. 201 locations and 222 coordinate pairs.
  4. **CSV** Mobile Speed Cameras, `.../resource/b4e0c74e-10a6-48e8-8421-2cd0294af6ae/download/mobile-speed-camera-locations-january-2022.csv` (66,584 B), saved as `$RS/mobile-speed-camera-locations-january-2022.csv`. 1,166 rows with headers `Suburb, Street` only and **no coordinates**.
- `package_search?q=camera` on the Open Data Hub → HTTP 200, 8 datasets. The only speed camera location dataset is `nsw-speed-cameras`; the others are Live Traffic Cameras, Maritime web camera, fines statistics and so on. `package_search?q=speed` → 12 datasets, again with no newer camera dataset. A boolean query (`enforcement OR "safety camera" OR "point-to-point"`) got HTTP 403 and was not retried. — [package_search](https://opendata.transport.nsw.gov.au/data/api/3/action/package_search?q=camera&rows=50)
- On data.nsw.gov.au, `package_search?q=speed camera` → HTTP 200, 21 results. Two are camera location datasets:
  - `2-nsw-speed-cameras`: a harvest mirror modified 2025-02-04 with the same 4 TfNSW URLs.
  - `list-of-fixed-speed-camera-locations-in-nsw`: from 2016, it only links to the old HTML page.
  - `3-17650-mobile-speed-cameras` is an archived OpenGov PDF, not location data.
  — [data.nsw.gov.au mirror](https://data.nsw.gov.au/data/dataset/2-nsw-speed-cameras)
- The ODH CSV and the DCS feature service hold essentially the same fixed-camera set. Comparing first-point coordinates rounded to 4 dp, 96 of 102 match exactly and the other 6 differ by 0.0001° (float32 precision in the FS). The FS has one extra point (-29.6337, 153.0274) that the CSV lacks. (My own comparison of `$RS/fixed-speed-cameras_1.csv` against `$RS/pdn_all.geojson` + `$RS/pdn_L1_all.geojson`.)
- A third-party December 2025 blog, not official, says the TfNSW mobile camera document "is typically updated every few months". It also says the public data has no precise GPS coordinates. — [Tynan blog](https://www.tynan.com.au/blog/latest-sydney-nsw-mobile-speed-camera-locations-december-2025)

### Inferences
- For a 2026 database, the official open-data coordinates are about five years old. Cameras added, moved or removed since then (and any newer camera types) will be missing.

### Gaps
- I could not check the current official mobile camera list (PDF on transport.nsw.gov.au) because of the 403. Its date and contents are unknown. In any case it has street/suburb only, no coordinates, according to the search snippet and the third-party blog.
- I found no official machine-readable source for point-to-point (average speed) cameras or mobile phone / seatbelt detection cameras.

## What fields are available for the Highway Radar / ExCam format (lat, lon, type, direction, speed limit, street)?

### Takeaway
Every official source gives coordinates, a camera category (by which file or layer it comes from), a school-zone flag (`SZ?`/`sz_`) and a free-text road/street description. **None gives direction of travel or speed limit.** Speed limit could be added from TfNSW's separate, current "Speed Zones" dataset (CC BY, updated 2026-10-28) by matching each camera to the nearest road segment. Direction would have to be inferred, or left as "any".

### Cited Findings
- XLSX/CSV fields: `SZ?` (Y/N school zone), `SUBURB/TOWN`, `ROAD/S` (e.g. "Hume Highway, between Murrell Street and Queen Street (school zone)"), `Cameras` (count), `Lat(n)`/`Long(n)` for up to 2 (fixed) or 3 (red-light) cameras per location. There is no direction or speed limit column. — [nsw-speed-cameras](https://opendata.transport.nsw.gov.au/dataset/nsw-speed-cameras)
- DCS feature service fields: `sz_, location, cameras, road, lat_1, long_1, lat_2, long_2, suburb_town, road_s, objectid, last_updated`. There is no direction or speed limit field. — [Layer 0](https://portal.data.nsw.gov.au/arcgis/rest/services/Hosted/TFNSW_Speed_Cameras_public/FeatureServer/0?f=json)
- Geoplex (unofficial) fields: `SZ_, Location, Cameras, Road, Lat_1_, Long_1_, Lat_2_, Long_2_, SUBURB_TOWN, ROAD_S`. There is no direction or speed limit field. — [Geoplex layer](https://services.arcgis.com/y3GIulxK3Ox4lXG0/arcgis/rest/services/Fixed_Speed_Cameras/FeatureServer/0?f=json)
- **Speed Zones** dataset (`package_show?id=speed-zones`, HTTP 200, saved as `$RS/tf_speed-zones.json`): CC BY, `metadata_modified` 2026-10-28. Its notes list zone categories Ordinary Permanent, Shared, High Pedestrian, School, Variable, Local Traffic, Truck & bus, Wet Weather, School Bus, Toll Plaza and Default. I did not download the files. Resources (last_modified 2026-10-28):
  - CSV `.../4253a054-b377-4b5b-83d1-71385bb6ff33/resource/d4aa7b3f-bd3a-4695-b4db-d016b6cc0832/download/speed_zones.csv`
  - JSON `.../resource/2b916d52-f956-44a5-8b22-5e57895723f7/download/speed_zones.json`
  - GeoJSON `.../resource/bc2da977-65d2-4caa-a73a-48d0c8bf1100/download/speed_zones.geojson`
  - KML `.../resource/f88175d5-6989-4325-96d0-fbb7109dd7c5/download/speed_zones.kml`
  - SHP zip `.../resource/8895ad63-3bba-4fda-9809-3cc886e249a6/download/speedzones.zip`
  - School Zones zip, last_modified 2026-10-04
  - A matching ArcGIS service `Hosted/TFNSW_Speed_Zones_Public` exists on portal.data.nsw.gov.au.
  — [Speed Zones](https://opendata.transport.nsw.gov.au/dataset/speed-zones)

### Inferences
- A possible ExCam mapping: type flag from the source layer (fixed / school-zone fixed / red-light). The street name could come from `ROAD/S` or `road_s`, perhaps cut at ", between". Each `Lat(n)/Long(n)` pair would be one record. Speed limit could be joined from Speed Zones by spatial nearest match; school-zone cameras have time-dependent limits (40 km/h in school hours). Direction would be unknown (0/"all") unless worked out from road geometry. Mobile camera rows have no coordinates and would need geocoding (street + suburb), which is approximate because some rows list several suburbs.

### Gaps
- I did not check the Speed Zones file schema (field names for the limit value), its size or its record counts in this pass.

## What licence applies, and does TfNSW require attribution?

### Takeaway
The Open Data Hub datasets are CC BY (the hub-wide default is Creative Commons Attribution 4.0), and attribution to Transport for NSW is a condition of reuse. The DCS feature service's catalogue record says "notspecified", but it names the Open Data Hub dataset as its source.

### Cited Findings
- The `nsw-speed-cameras` CKAN record shows `license_id` "cc-by", "Creative Commons Attribution", `isopen` true. — [CKAN](https://opendata.transport.nsw.gov.au/data/api/3/action/package_show?id=nsw-speed-cameras)
- The ODH "Using Our Data" page (HTTP 200) says: "Unless otherwise stated, the data on the Open Data Hub is made available under the Open Data Policy and is licensed under a Creative Commons Attribution 4.0 License… allows for redistribution and reuse of a licensed work on the condition that Transport for NSW is attributed as the source." It also asks users to read the Terms, Privacy Policy and Acceptable Use Policy. (`https://opendata.transport.nsw.gov.au/data-licence` returned 404.) — [Using Our Data](https://opendata.transport.nsw.gov.au/datalicence)
- The data.gov.au harvest of "TfNSW Speed Camera Public" (the DCS feature service) shows `license_id` "notspecified" and `isopen` false. — [data.gov.au](https://data.gov.au/data/dataset/2e89c8c9-a82f-48a1-91b1-4a3ed271f970)
- The Geoplex AGOL item has `licenseInfo`: None and `accessInformation`: None. — [AGOL item](https://www.arcgis.com/sharing/rest/content/items/3bd291fda55642c5bdc7a1eb063638d0?f=json)

### Inferences
- The safest choice for the app is to build from the ODH CSV/XLSX files, which are explicitly CC BY, and credit "Contains data from Transport for NSW, licensed under CC BY 4.0" in the app or database metadata.

### Gaps
- I did not read the ODH Terms / Acceptable Use Policy text for any extra conditions, such as restrictions on safety-critical use.

## Do TfNSW APIs (api.transport.nsw.gov.au, Live Traffic Hazards, etc.) include camera location feeds, and what keys do they need?

### Takeaway
No enforcement camera feed. The only camera feed on api.transport.nsw.gov.au is `/v1/live/cameras`, which covers Live Traffic **traffic-view** (CCTV/JPEG) cameras, not speed cameras. Like the hazards feeds, it needs a free Open Data Hub API key sent as `Authorization: apikey <TOKEN>`; without one it returns 401.

### Cited Findings
- `https://api.transport.nsw.gov.au/v1/live/hazards/incident/open` → **HTTP 401**, "The calling application is unauthenticated." `https://api.transport.nsw.gov.au/v1/live/cameras` → **HTTP 401**, same message. (Saved as `$RS/lt_hazards.json` and `$RS/lt_cams.json`.)
- The "Live Traffic Cameras" ODH dataset (CC BY, modified 2025-09-02) is described as "Image URL, GPS coordinates, and view description of traffic cameras in GeoJSON format". Its swagger file (`.../resource/cc776d1a-.../download/livetrafficcamera.json`, HTTP 200, saved as `$RS/lt_cams_public.json`) defines host `api.transport.nsw.gov.au`, basePath `/v1/live`, path `/cameras`, and security `APIKey` in header `Authorization`, "Expected Format: apikey [TOKEN]". — [Live Traffic Cameras](https://opendata.transport.nsw.gov.au/dataset/live-traffic-cameras)
- The Live Traffic NSW Developer Guide v1.9 (Aug 2025 PDF, HTTP 200, saved as `$RS/ltguide/guide.pdf`) says feeds are served "from various subdirectories of api.transport.nsw.gov.au/v1/live/", with hazards under `/v1/live/hazards/`. Its only camera section is "4. Live Traffic Cameras" (`/v1/live/cameras`; properties include `direction` N/S/…, image URL, title, view). The text has no mention of speed, red-light or enforcement cameras. — [Developer Guide PDF](https://opendata.transport.nsw.gov.au/data/dataset/b0212311-b0da-4363-8dc3-825fe10941b2/resource/59e1a78a-e0ee-400f-b89d-3a574688b01a/download/live-traffic-nsw-developer-guide-2023-10-v1.9_opendata_aug25.pdf)
- data.nsw.gov.au `roads-realtime-cameras` (2022) has a resource oddly titled "NSW Speed Cameras website link", but the dataset describes traffic cameras and the link goes to the ODH live-traffic-cameras page. — [Roads - Realtime - Cameras](https://data.nsw.gov.au/data/dataset/roads-realtime-cameras)

### Inferences
- The Live Traffic camera feed must not be mixed into a speed camera database. Its points are traffic-view cameras.

### Gaps
- I did not check whether an API key unlocks any undocumented enforcement-camera endpoint (no key was available). The developer guide documents none.
