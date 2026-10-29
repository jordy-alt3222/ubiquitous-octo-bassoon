# NSW Point-to-Point (Average Speed) Camera Lengths, 2026

Research date: 29 October 2026. Scope: NSW only.

Access notes (they affect how far each source can be trusted):
- www.transport.nsw.gov.au returned HTTP 403 to plain curl for the average speed cameras page, the enforcement map PDF (`/system/files/media/documents/2025/crs_average_speed_cameras_map_1.pdf`) and the trial snapshot PDF (`/system/files/media/documents/2026/crs_asc_trial_snapshot_01052026-30062026.pdf`). The 403 bodies are saved at `.../scratchpad/research_p2p/web/tfnsw_avg.html`, `tfnsw_map.pdf` and `tfnsw_snapshot.pdf`; each is a 919-byte CloudFront error page. I made no attempt to get around the block. **The official TfNSW list and map were therefore NOT read directly.** The length table below comes from a third-party site that says it copied TfNSW's enforcement map. Where possible it is cross-checked against Revenue NSW and NSW Government data.
- WebFetch could not resolve any host in this session. All pages were fetched with plain curl through the session proxy.
- The OpenStreetMap Overpass API (overpass-api.de, overpass.kumi.systems, overpass.private.coffee) was reset or timed out, so OSM enforcement relations could not be checked for coordinates.
- Downloaded files are in `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/research_p2p/`. They were treated as untrusted and parsed with `python3 -I`:
  - `search_*.json`: TfNSW Open Data CKAN package_search results for "point to point", "average speed", "safe-t-cam" and "speed camera"
  - `pkg_speedcams.json`: package_show for `nsw-speed-cameras`
  - `xlsx/nsw-speed-cameras-gps-locations-november-2021.xlsx`, from https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/a5c27c2d-5511-4fe8-863d-06a2d1a5ed10/download/nsw-speed-cameras-gps-locations-november-2021.xlsx
  - `revenue/dsf010.xlsx`, from https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/all-speeding-and-red-light-camera-detected-offences-dsf010.xlsx (data at COB 01/10/2026, offences 01/01/2018–30/09/2026). The pivot cache was unzipped to `revenue/x/`.
  - `web/speedcam_avg.html` (https://www.speedcam.com.au/blog/average-speed-cameras-nsw/), `web/carexplore.html`, `web/manofmany.html`, `web/nswgov_safetyswitch.html`, `web/tz_hub.html`, `web/tz_flyer.pdf`, `web/nota_aug2026.html`

## Which point-to-point lengths exist in NSW in 2026, and which enforce all vehicles versus heavy vehicles only?

### Takeaway
As of October 2026, TfNSW's enforcement map (as reproduced by speedcam.com.au, checked 20 Oct 2026) lists **32 lengths in operation**, numbered 1–33 with no number 19. Nine more are at planning stage. **Only two lengths enforce all vehicles**: No. 8, Hume Highway Coolac–Gundagai (16 km), and No. 18, Pacific Highway Kew–Lake Innes/Port Macquarie (15 km). Both have fined light vehicles since 1 July 2025, and that continues after the trial's scheduled end on 30 June 2026. The other 30 lengths enforce heavy vehicles only (over 4.5 t GVM).

### Cited Findings
- "Transport for NSW's average speed enforcement map lists 32 lengths in operation, numbered 1 to 33 with no number 19. Lengths 8 and 18 are the two that also fine cars." The article was published 19 Oct 2026, says its facts were checked on 20 Oct 2026, and cites the TfNSW map PDF `crs_average_speed_cameras_map_1.pdf` — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/)
- Full list of lengths in operation (No. / road / section / approximate km / start of warning-letter period), as published by speedcam.com.au from the TfNSW map — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/):

| No. | Road | Section | Approx. km | Warnings began | Vehicles enforced (2026) |
|---|---|---|---|---|---|
| 1 | M1 Pacific Motorway | Harwood to New Italy | 35 | Mar 2010 | Heavy only |
| 2 | Great Western Highway | Meadow Flat to Raglan | 26 | May 2010 | Heavy only |
| 3 | Federal Highway | Goulburn to Collector | 20 | May 2011 | Heavy only |
| 4 | Blackwall Drive to Langs Way (former Pacific Highway) | Woodburn to Wardell | 18 | Jun 2011 | Heavy only |
| 5 | Mitchell Highway | Molong to Cundumbul | 28 | Aug 2011 | Heavy only |
| 6 | Pacific Highway | Nabiac to Kiwarrak | 17 | Aug 2011 | Heavy only |
| 7 | Hume Highway | Coolac to Bowning | 72 | Aug 2011 | Heavy only |
| 8 | Hume Highway | Gundagai to Coolac | 16 | Aug 2011 | **All vehicles** (light vehicles fined since 1 Jul 2025) |
| 9 | Monaro Highway | Bredbo to Cooma | 28 | Sep 2011 | Heavy only |
| 10 | Gwydir Highway | Glen Innes to Inverell | 57 | Oct 2011 | Heavy only |
| 11 | Oxley Highway | Gunnedah to Tamworth | 60 | Nov 2011 | Heavy only |
| 12 | Pacific Highway | Port Macquarie to Kempsey | 36 | Dec 2011 | Heavy only |
| 13 | New England Highway | Muswellbrook to Aberdeen | 7 | Dec 2011 | Heavy only |
| 14 | New England Highway | Singleton to Muswellbrook | 36 | Dec 2011 | Heavy only |
| 15 | Newell Highway | Eumungerie to Gilgandra | 30 | Dec 2011 | Heavy only |
| 16 | Newell Highway | Tomingley (Peak Hill to Tomingley) | 10 | Dec 2011 | Heavy only |
| 17 | Golden Highway | Sandy Hollow to Merriwa | 28 | Mar 2012 | Heavy only |
| 18 | Pacific Highway | Kew to Port Macquarie (Lake Innes) | 15 | Aug 2012 | **All vehicles** (light vehicles fined since 1 Jul 2025) |
| 20 | Picton Road | Wilton to Mt Keira | 17 | Oct 2012 | Heavy only |
| 21 | Newell Highway | Forbes to (West) Wyalong | 94 | Mar 2013 | Heavy only |
| 22 | Princes Highway / M1 Princes Motorway | Bulli to North Wollongong | 15 | Jan 2014 | Heavy only |
| 23 | Pacific Motorway | Ballina to Wardell | 16 | Feb 2014 | Heavy only |
| 24 | M1 Pacific Motorway | Tyndale to Harwood | 21 | Feb 2014 | Heavy only |
| 25 | Great Western Highway | Mount Victoria to Lithgow | 11 | Jan 2015 | Heavy only |
| 26 | WestConnex M8 Tunnel | St Peters to Kingsgrove | 9 | Aug 2020 | Heavy only |
| 27 | M5 East Tunnel | Arncliffe to Kingsgrove | 4 | Oct 2020 | Heavy only |
| 28 | WestConnex M4 Tunnel | Ashfield to Lidcombe | 7 | Oct 2020 | Heavy only |
| 29 | NorthConnex | Pennant Hills to Normanhurst | 9 | Nov 2020 | Heavy only |
| 30 | WestConnex M4-M8 Link Tunnel | Ashfield to St Peters | 10 | Jan 2023 | Heavy only |
| 31 | Rozelle Interchange | Rozelle to Annandale/Leichhardt | 24 | Mar 2024 | Heavy only |
| 32 | M4 Motorway | Eastern Creek to South Penrith | 15 | Mar 2025 | Heavy only |
| 33 | M2 Motorway | Naremburn to Baulkham Hills | 26 | Oct 2025 | Heavy only |

  (The "Peak Hill to Tomingley" and "West Wyalong" wording comes from the older carexplore table below.)
- Planning-stage lengths on the TfNSW map (not operating): Cross City Tunnel (Sydney–Darlinghurst, 2 km), Eastern Distributor (Zetland–Sydney, 6 km), M7 Motorway (Baulkham Hills–Prestons, 40 km), Sydney Harbour Tunnel (Sydney–North Sydney, 1 km), M5 Motorway (Kingsgrove–Casula, 23 km), Hume Motorway (Campbelltown–Wilton), Newell Highway (Moree–Boggabilla), Newell Highway (Coonabarabran–Narrabri) and Mitchell Highway (Vittoria–Windradyne) — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/); the same nine "(Planning stage)" entries appear in [Man of Many, updated 20 Nov 2025](https://manofmany.com/auto/new-average-speed-camera-locations-nsw)
- "On the other 30 average speed lengths in the state, the cameras only fine heavy vehicles over 4.5 tonnes" — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/)
- Light-vehicle trial sites: "Pacific Highway between Kew and Lake Innes (Port Macquarie) – 15 km between cameras" and "Hume Highway between Coolac and Gundagai – 16 km between cameras" — [Towards Zero Collaboration Hub (TfNSW), 3 Jul 2025](https://collaborationhub.towardszero.nsw.gov.au/news-activities/news/average-speed-camera-light-vehicle-trial-enforcement-began-1-july-2025)
- Revenue NSW's DSF010 dataset (data at 01/10/2026) has a separate camera type, "Average Speed Camera - Light". It appears ONLY at four locations: HUME HIGHWAY COOLAC NORTHBOUND, HUME HIGHWAY GUNDAGAI SOUTHBOUND, PACIFIC HIGHWAY KEW SOUTHBOUND and PACIFIC HIGHWAY LAKE INNES NORTHBOUND. All four have notices in FY 2024-25 through 2026-27, which confirms light-vehicle enforcement is still running in FY 2026-27 (July–Sep 2026) — [Revenue NSW DSF010](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/all-speeding-and-red-light-camera-detected-offences-dsf010.xlsx); parsed locally from the pivot cache
- Locations with "Average Speed Camera - Heavy" notices in DSF010 (2018–Sep 2026), which identifies the camera sites that issue penalties: Blackwall Drive Wardell NB; Federal Hwy Collector SB; Golden Hwy Merriwa NB/WB and Sandy Hollow EB; Great Western Hwy Little Hartley EB/WB, Hartley EB/WB and Raglan WB; Gwydir Hwy Inverell WB; Hume Hwy Bowning NB, Coolac NB/SB and Gundagai SB; M1 Princes Motorway North Wollongong SB (last notices FY 2024-25); M4 East Tunnel WestConnex Croydon WB; M4 Motorway Minchinbury EB; M4 Motorway WestConnex Lidcombe EB; M4 Western Motorway Orchard Hills WB; M5 East Motorway Arncliffe WB and Earlwood EB; Macleay Valley Way South Kempsey NB/SB; Mitchell Hwy Cundumbul NB and Molong SB; Monaro Hwy Bredbo NB and Cooma SB; New England Hwy Aberdeen NB, Muswellbrook NB/SB and Singleton SB; Newell Hwy Eumungerie SB, Forbes NB, Gilgandra NB, Tomingley NB/SB and Wyalong SB; NorthConnex Tunnel Normanhurst SB and Pennant Hills NB; Oxley Hwy Bective EB/WB, Gunnedah WB and Westdale EB; Pacific Hwy Kempsey NB, Kew SB, Kiwarrak NB, Lake Innes NB, Nabiac SB, Urunga NB (FY 2019-21 only), Verges Creek NB/SB, Wardell SB and Woodburn SB; Picton Road Cordeaux EB/WB; Princes Motorway Mount Ousley SB; Rozelle Interchange Rozelle EB/NB/SB; WestConnex M4 Tunnel Haberfield EB/SB; WestConnex M8 Tunnel Arncliffe WB, Bardwell Park EB, Newtown EB and Newtown NB — [Revenue NSW DSF010](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/all-speeding-and-red-light-camera-detected-offences-dsf010.xlsx)
- Heavy vehicles across all 32 lengths received 5,803 average speed notices in 2025-26. Light vehicles received 29,743 notices at the two trial lengths (speedcam's analysis of DSF010) — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/)
- The TfNSW Open Data Hub has NO point-to-point or average speed dataset. CKAN searches for "average speed" returned 0 results, and "point to point" and "safe-t-cam" returned only unrelated datasets. The only speed camera dataset, `nsw-speed-cameras` (resources last modified Aug 2023; GPS file dated November 2021), has just two sheets, "Fixed Speed Cameras" and "Red Light Speed Cameras", plus mobile camera CSVs. It has no average speed sheet — [TfNSW Open Data CKAN](https://opendata.transport.nsw.gov.au/data/api/3/action/package_search?q=average%20speed); [package_show nsw-speed-cameras](https://opendata.transport.nsw.gov.au/data/api/3/action/package_show?id=nsw-speed-cameras)

### Inferences
- In DSF010, the M2 Motorway (No. 33, Oct 2025) has no "Average Speed Camera" notices under any obvious M2 location name. That fits either a warning-letter phase (warnings are not penalty notices) or a location label not recognised here. Treat the M2 as operating but unverified in penalty data.
- No. 22 (Bulli–North Wollongong) shows heavy-vehicle notices only up to FY 2024-25 (M1 Princes Motorway North Wollongong SB). It may have paused or been relabelled. It is still on the Oct 2026 map list.
- Each length has one Revenue NSW "location" per direction, named after the end of the length where the offence is recorded. For example, Hume Coolac NB and Gundagai SB both have light-vehicle notices, so No. 8 enforces in both directions.

### Gaps
- The official TfNSW average speed camera page and map PDF could not be read (403), so the list has not been checked against TfNSW directly.
- No authoritative per-length posted speed limit was found. Sources give none, and TfNSW says that where a length has several posted limits, an "average speed limit" is calculated with a legislated formula ([TfNSW flyer, Sept 2025](https://collaborationhub.towardszero.nsw.gov.au/ArticleDocuments/630/ASC%20Flyer_How%20it%20works_SEPT%202025_WEB.pdf.aspx)). Speed limits must come from another source, such as the TfNSW Open Data "Speed Zones" dataset (`speed-zones`, modified 2026-10-28, speedzones.zip), which this researcher did not process.
- The Safe-T-Cam network is not documented on Open Data, and nothing found shows it as a separate active system in 2026.

## What are the start and end locations and coordinates of each length?

### Takeaway
**No authoritative GPS coordinates for any NSW point-to-point camera were found.** TfNSW Open Data publishes none. The official map PDF and location page were blocked (403). OSM Overpass was unreachable. Start and end place names come from the TfNSW map list (via speedcam.com.au). The Revenue NSW location labels give each camera site's locality and direction of travel. No coordinates are recorded here, and none should be estimated for the Highway Radar database until an authoritative source is obtained.

### Cited Findings
- The start and end place names are in the table above (TfNSW map list via [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/)).
- Directional camera-site names from Revenue NSW, matched to lengths. Matching is by place name; this is an inference. Source: [Revenue NSW DSF010](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/all-speeding-and-red-light-camera-detected-offences-dsf010.xlsx).
  - No. 8 Hume, Coolac–Gundagai: "HUME HIGHWAY COOLAC NORTHBOUND" (NB travel, Gundagai → Coolac) and "HUME HIGHWAY GUNDAGAI SOUTHBOUND" (SB travel, Coolac → Gundagai). Both directions are enforced for light and heavy vehicles.
  - No. 18 Pacific, Kew–Lake Innes: "PACIFIC HIGHWAY LAKE INNES NORTHBOUND" (NB, Kew → Lake Innes) and "PACIFIC HIGHWAY KEW SOUTHBOUND" (SB, Lake Innes → Kew). Both directions are enforced for light and heavy vehicles.
  - No. 7 Hume, Coolac–Bowning: "HUME HIGHWAY BOWNING NORTHBOUND" and "HUME HIGHWAY COOLAC SOUTHBOUND".
  - No. 6 Pacific, Nabiac–Kiwarrak: "PACIFIC HIGHWAY KIWARRAK NORTHBOUND" and "PACIFIC HIGHWAY NABIAC SOUTHBOUND".
  - No. 12 Pacific, Port Macquarie–Kempsey: "PACIFIC HIGHWAY KEMPSEY NORTHBOUND" and "MACLEAY VALLEY WAY SOUTH KEMPSEY SOUTHBOUND/NORTHBOUND".
  - No. 3 Federal, Goulburn–Collector: "FEDERAL HIGHWAY COLLECTOR SOUTHBOUND" (no NB notice location seen).
  - No. 5 Mitchell: "MITCHELL HIGHWAY CUNDUMBUL NORTHBOUND" and "MITCHELL HIGHWAY MOLONG SOUTHBOUND".
  - No. 9 Monaro: "MONARO HIGHWAY BREDBO NORTHBOUND" and "MONARO HIGHWAY COOMA SOUTHBOUND".
  - No. 11 Oxley, Gunnedah–Tamworth: Bective EB/WB, Gunnedah WB and Westdale EB. Bective and Westdale suggest the Tamworth-end sites.
  - No. 15 Newell, Eumungerie–Gilgandra: "EUMUNGERIE SOUTHBOUND" and "GILGANDRA NORTHBOUND".
  - No. 16 Newell, Tomingley: "TOMINGLEY NORTHBOUND/SOUTHBOUND".
  - No. 21 Newell, Forbes–Wyalong: "FORBES NORTHBOUND" and "WYALONG SOUTHBOUND".
  - No. 17 Golden: "SANDY HOLLOW EASTBOUND" and "MERRIWA WESTBOUND/NORTHBOUND".
  - No. 2 and No. 25 Great Western: Raglan WB, Hartley EB/WB and Little Hartley EB/WB.
  - No. 20 Picton Road: "CORDEAUX EASTBOUND/WESTBOUND".
  - No. 22 and the Mount Ousley site: "M1 PRINCES MOTORWAY NORTH WOLLONGONG SOUTHBOUND" and "PRINCES MOTORWAY MOUNT OUSLEY ROAD MOUNT OUSLEY SOUTHBOUND".
  - No. 4: "BLACKWALL DRIVE WARDELL NORTHBOUND" and "PACIFIC HIGHWAY WOODBURN SOUTHBOUND".
  - No. 23: "PACIFIC HIGHWAY WARDELL SOUTHBOUND".
  - No. 1 and No. 24 (M1 Harwood/New Italy/Tyndale): probably "PACIFIC HIGHWAY VERGES CREEK NB/SB", but this is uncertain.
  - No. 26 M8: Arncliffe WB, Bardwell Park EB, Newtown EB/NB.
  - No. 27 M5 East: Arncliffe WB and Earlwood EB.
  - No. 28 M4 Tunnel: Croydon WB and Lidcombe EB.
  - No. 29 NorthConnex: Normanhurst SB and Pennant Hills NB.
  - No. 30 M4-M8 Link: Haberfield EB/SB.
  - No. 31 Rozelle: Rozelle EB/NB/SB.
  - No. 32 M4 Eastern Creek–South Penrith: "M4 MOTORWAY MINCHINBURY EASTBOUND" and "M4 WESTERN MOTORWAY ORCHARD HILLS WESTBOUND". Both directions.
- M2 length: "a 26km route between Baulkham Hills at the start of the M2 and Naremburn on the M1" — [Fully Loaded](https://www.fullyloaded.com.au/more-speed-cameras-for-m2-motorway-and-lane-cove-tunnel/) (as quoted in search results; a direct curl got HTTP 202 with no body)
- TfNSW: "Average speed cameras record photographs of vehicles as they pass the start and end points of an enforcement length", and the distance used is "the shortest practicable distance" — [TfNSW ASC flyer, Sept 2025](https://collaborationhub.towardszero.nsw.gov.au/ArticleDocuments/630/ASC%20Flyer_How%20it%20works_SEPT%202025_WEB.pdf.aspx)

### Inferences
- In each Revenue NSW label, the locality names the camera where the offence is recorded, so it marks the END of the length for that direction of travel. Example: "Lake Innes Northbound" means travel from Kew to Lake Innes. This fits the naming pattern but is not stated by any source.
- Most regional lengths show notice locations in both directions, so enforcement is probably bidirectional. Some show only one direction in the data, such as Federal Collector SB and Gwydir Inverell WB. That may only reflect low offence counts, not one-way enforcement.

### Gaps
- No GPS coordinates for any start or end camera were found in an authoritative or accessible source. Candidates for a later pass: the TfNSW average speed map PDF and location page (blocked here); a TfNSW data request; OSM `type=enforcement` + `enforcement=average_speed` relations (Overpass unreachable here; community data, not authoritative).
- The exact start and end points of No. 1 and No. 24 (M1 Harwood/New Italy/Tyndale) are not confirmed in any source.

## What changes, new lengths, or light-vehicle enforcement rollouts happened from 2023 to 2026?

### Takeaway
2023: WestConnex M4-M8 Link opened as a length (Jan 2023). 2024: Rozelle Interchange (Mar 2024); the Road Transport Act was amended in Oct 2024 to allow average speed enforcement of all vehicles; the trial was announced in Sept 2024. 2025: M4 Eastern Creek–South Penrith (Mar 2025); the light-vehicle trial began in warning mode on 1 May 2025 and moved to full enforcement on 1 July 2025; M2 Naremburn–Baulkham Hills (Oct 2025). 2026: the trial ended as scheduled on 30 June 2026, but light-vehicle enforcement continues at both sites pending an independent review due to be tabled in Parliament in October 2026. As of 20 Oct 2026 it had not been published. The Staysafe committee recommended (17 Sept 2026) a regional rollout to light vehicles, subject to the trial results, with the government response due by mid-March 2027.

### Cited Findings
- New lengths and their warning-letter start dates: M4-M8 Link Tunnel Ashfield–St Peters 21/01/2023 ([carexplore](https://www.carexplore.com.au/nsw-average-speed-cameras-2025-locations-revealed/); "Jan 2023" in [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/)); Rozelle Interchange Mar 2024; M4 Eastern Creek–South Penrith Mar 2025; M2 Naremburn–Baulkham Hills Oct 2025 ([speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/)). Man of Many gives "M4 Between Eastern Creek and South Penrith 15 (installed 22/03/2025)" and "M2 Motorway Between Naremburn and Baulkham Hills (installed 11/10/2025)", both "currently only in enforcement mode for heavy vehicles" — [Man of Many, updated 20 Nov 2025](https://manofmany.com/auto/new-average-speed-camera-locations-nsw)
- "The Road Transport Act 2013 (the Act) was amended in October 2024 so that average speed cameras can enforce speeding by all vehicle types." Warning mode from 1 May and enforcement from 1 July; the trial runs 14 months; the government will report to Parliament in 2026; "Existing enforcement of heavy vehicle offences at these sites will be unaffected." The release also refers to "all existing 31 average speed camera lengths" — [NSW Government ministerial release, 16 Mar 2025](https://www.nsw.gov.au/ministerial-releases/safety-switch-date-set-for-average-speed-camera-trial)
- The trial was announced in September 2024. From 1 May 2025, warning letters went to drivers up to 30 km/h over the limit, and those more than 30 km/h over were fined from day one. Full enforcement began 1 July 2025. The trial was scheduled to end 30 June 2026, when TfNSW reported a 76% improvement in light-vehicle compliance and said "enforcement would continue at both sites while an independent review is completed". The outcomes were to be tabled in Parliament in October 2026 and were not published as of 20 Oct 2026 — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/), citing the TfNSW release "Light vehicle average speed camera enforcement continues at two sites" of 30 June 2026 ([TfNSW URL](https://www.transport.nsw.gov.au/news-and-events/media-releases/light-vehicle-average-speed-camera-enforcement-continues-at-two), 403 here, not read directly)
- During the warning period, "1 in every 122 light vehicle drivers was detected speeding and issued with a warning letter" — [Towards Zero Collaboration Hub, 3 Jul 2025](https://collaborationhub.towardszero.nsw.gov.au/news-activities/news/average-speed-camera-light-vehicle-trial-enforcement-began-1-july-2025)
- 13,637 infringements were issued on the Coolac–Gundagai section between 1 Jul 2025 and 30 Jun 2026 (TfNSW figures) — [About Regional, 5 Aug 2026](https://aboutregional.com.au/more-than-13600-speeding-infringements-recorded-between-coolac-and-gundagai/512235/) (from search snippet). This is consistent with speedcam's Revenue NSW counts of 8,802 SB plus 4,824 NB.
- On 17 Sept 2026 the Staysafe committee recommended that "subject to the results of its trial, Transport for NSW roll out average speed cameras for light vehicles across regional NSW"; the government response is due by mid-March 2027 — [speedcam.com.au](https://www.speedcam.com.au/blog/average-speed-cameras-nsw/); [Lithgow Mercury (ACM)](https://www.lithgowmercury.com.au/story/9353940/nsw-regional-roads-average-speed-cameras-urged-for-cars/)
- An older TfNSW-derived table (carexplore, 2025) lists "Pacific Highway Between Urunga and Valla, 13 km, 29/09/2012". It also lists the M2 as "Between Artarmon and Baulkham Hills, 20 km" at planning stage, and No. 6 as "Nabiac and Taree" — [carexplore](https://www.carexplore.com.au/nsw-average-speed-cameras-2025-locations-revealed/). The 2026 list has no Urunga–Valla length (no number 19), and Revenue NSW shows Urunga NB heavy-vehicle notices only in FY 2019-20 and 2020-21 — [Revenue NSW DSF010](https://www.revenue.nsw.gov.au/help-centre/resources-library/statistics/all-speeding-and-red-light-camera-detected-offences-dsf010.xlsx)

### Inferences
- Urunga–Valla (former No. 19) appears to have been decommissioned around 2021, probably after the Pacific Highway upgrade. Do NOT include it in a 2026 database. The decommissioning date and reason are not stated by any source found.
- The M2 length was redefined between planning and operation, from Artarmon–Baulkham Hills (20 km) to Naremburn–Baulkham Hills (26 km). Use the 2025–26 description.
- The release's count of "31 existing lengths" (Mar 2025) against 32 in operation (Oct 2026) fits the addition of the M2 in Oct 2025.

### Gaps
- The independent trial review and any decision to switch more lengths to light-vehicle enforcement had not been published as of the latest sources (20 Oct 2026). Re-check TfNSW media releases after this date.
- No source confirms whether the M2 length had moved from warning mode to penalties by late 2026.
