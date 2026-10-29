# NSW mobile speed camera locations and school zone cameras (as of October 2026)

Research date: 2026-10-29. Scope: NSW only.

Access notes (these affect how reliable the notes are):
- `www.transport.nsw.gov.au` (TfNSW / Centre for Road Safety) returned HTTP 403 to plain curl for the 2026 mobile camera PDF. WebFetch could not resolve the host because it is outside this session's allowed network domains. I made no attempt to get around either block: no spoofed user agent, no Wayback Machine. **The current official PDF was NOT read directly.** Everything said about it below comes from search-engine snippets and secondary sources.
- WebFetch also failed (DNS) on www.nsw.gov.au, www.newcastleherald.com.au and www.speedcam.com.au. Plain curl worked for these and for mynrma.com.au, service.nsw.gov.au and speedcameralocations.com.au. drive.com.au returned 403 to curl, so it was not read. parliament.nsw.gov.au returned 403 to curl, so it was not read.
- Downloaded files were all treated as untrusted and parsed with `python3 -I`. They are in `/tmp/claude-0/-home-user-ubiquitous-octo-bassoon/acd3d066-8362-52cb-9ddb-82ccf7cccf7a/scratchpad/research_mobile/`:
  - `mobile-jan2022.csv` from https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/b4e0c74e-10a6-48e8-8421-2cd0294af6ae/download/mobile-speed-camera-locations-january-2022.csv (sha256 9624d4ab…24ed)
  - `gps-nov2021.xlsx` from https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/a5c27c2d-5511-4fe8-863d-06a2d1a5ed10/download/nsw-speed-cameras-gps-locations-november-2021.xlsx (sha256 46b4a96a…f156)
  - `fixed.csv` from https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/bcf2f6f4-ecfb-40e1-a807-0d5eb5f51507/download/fixed-speed-cameras_1.csv (sha256 0ee92c45…5b1)
  - `search.json`, `rsd.json`, `sz.json`: responses from the CKAN API (package_search / package_show)
  - HTML snapshots: `nh.html`, `speedcam_blog.html`, `speedcam_nsw.html`, `szblog.html`, `snsw.html`, `nrma.html`, `tynan.html`, `scl.html`, `speedcamsau_sz.html`, `zeroes.html`, `signs.html`, `fmcams.html`
  - `tfnsw2026.pdf`: this is only the 919-byte 403 error body, not the PDF.

## Q1. Where is the current NSW mobile speed camera location list published, and how many locations does it have in 2026?

### Takeaway
The official list is a Transport for NSW / Centre for Road Safety PDF titled "Mobile speed camera locations", organised by suburb and street. Its current file path is under `/documents/2026/`. Secondary sources describe it as about 53 pages and say it is updated every few months. I could not open the 2026 PDF, so I cannot give an authoritative 2026 location count. The only machine-readable official copy, on the Open Data Hub, is dated January 2022 and has 1,166 rows.

### Cited Findings
- The current official PDF URL is https://www.transport.nsw.gov.au/system/files/media/documents/2026/crs_mobile_speed_cameras.pdf. The title "Mobile speed camera locations" and the first entry ("Aarons Pass, Apple Tree Flat, Cudgegong, Ilford" / Castlereagh Highway) come from a search snippet. Fetching it returned HTTP 403, so I did not read it. — [TfNSW PDF (search result only)](https://www.transport.nsw.gov.au/system/files/media/documents/2026/crs_mobile_speed_cameras.pdf)
- An older copy sits under a 2024 path: https://www.transport.nsw.gov.au/system/files/media/documents/2024/crs_mobile_speed_cameras.pdf. This shows the file is periodically replaced under a new year folder with the same name. I did not read it either. — [TfNSW 2024 PDF (search result only)](https://www.transport.nsw.gov.au/system/files/media/documents/2024/crs_mobile_speed_cameras.pdf)
- The TfNSW "reports and publications" landing page for the list is https://www.transport.nsw.gov.au/news-and-events/reports-and-publications/mobile-speed-cameras. One search-engine summary said that page listed the PDF as "published 31 Jul 2025". That date is unverified and conflicts with the 2026 file path, which suggests a 2026 revision exists. — [TfNSW reports page (not accessible)](https://www.transport.nsw.gov.au/news-and-events/reports-and-publications/mobile-speed-cameras)
- Tynan Motors (a dealer blog, 15 Dec 2025, citing drive.com.au) gave these details:
  - TfNSW "releases a detailed 53-page document outlining approved roads and suburbs where mobile speed cameras may be deployed".
  - "These locations are typically updated every few months".
  - Between October and December 2025 the document was updated once, adding just three locations statewide.
  - There is no public map of exact mobile camera positions.
  — [Tynan Motors](https://www.tynan.com.au/blog/latest-sydney-nsw-mobile-speed-camera-locations-december-2025)
- Tynan also said entries can describe whole routes. One entry covers a ~40 km stretch between Grafton and Maclean, listing 9 suburbs and 8 streets. Nearby suburbs are grouped, so "a street may be listed alongside a suburb it does not technically run through". — [Tynan Motors](https://www.tynan.com.au/blog/latest-sydney-nsw-mobile-speed-camera-locations-december-2025)
- The Open Data Hub dataset `nsw-speed-cameras` was last modified 2023-08-21. Its "Mobile Speed Cameras CSV" resource is `mobile-speed-camera-locations-january-2022.csv`. I downloaded it: 1,166 data rows, columns `Suburb,Street`, 828 distinct street strings, no coordinates. — [TfNSW Open Data CKAN API](https://opendata.transport.nsw.gov.au/data/api/3/action/package_search?q=speed%20camera)
- In April 2024 the NSW Government announced that mobile cameras "will be able to be deployed in 2,700 new sites across the state", described as "doubling roadside enforcement sites". 1,650 of these were to be regional, eligible "once the scoping period is completed over the coming 12 months". — [NSW Government media release, 19 Apr 2024](https://www.nsw.gov.au/media-releases/nsw-government-zeroes-on-road-safety)
- The May 2024 CarExpert article (syndicated by the Newcastle Herald) said the additional 2,700 sites are "approximately double the current number of locations available". — [Newcastle Herald / CarExpert, 21 May 2024](https://www.newcastleherald.com.au/story/8635930/more-speed-camera-sites-seatbelt-detection-tech-coming-to-australian-state/)
- SpeedCam.com.au, a third-party aggregator, claims "1,542 mobile camera sites" in NSW (data last updated 18 Sep 2026, "from Transport for NSW"). The same page also says "Since 2020, mobile speed cameras in NSW operate without advance warning signs", which is wrong (see Q-changes below). That error lowers confidence in its numbers. — [SpeedCam NSW](https://www.speedcam.com.au/cameras/nsw/)
- SpeedCameraLocations.com.au, a third party, republishes the government's suburb/street list. It says "A more precise location is currently not known." Its HTML table has about 1,030 rows; I did not verify which list version it mirrors. Its user-report map was last updated 3 Jan 2026. — [speedcameralocations.com.au](https://www.speedcameralocations.com.au/nsw/mobile/)
- The Service NSW "Check a speed camera location" tool lets users search by location, road and camera type. It covers fixed and school zone fixed cameras, mobile speed cameras and red-light cameras. — [Service NSW](https://www.service.nsw.gov.au/referral/check-a-speed-camera-location)

### Inferences
- The 2,700-site expansion was scoped from April 2024 to about mid-2025. The 2026 PDF should therefore be far larger than the January 2022 CSV (1,166 rows). The 53-page length is consistent with that, but the exact count is unknown. Third-party counts (about 1,030 and 1,542) are lower than "1,166 + 2,700". The likely reasons are that one PDF row groups several suburbs or streets, and that some new "sites" may sit on roads already listed. This is unconfirmed.
- Update cadence: the list is revised irregularly, roughly every few months, and the file is replaced at a new year path with the same filename. A database pipeline should re-download it from the TfNSW reports page each quarter. A user or allowlisted fetch is needed, because automated access is blocked.

### Gaps
- No authoritative 2026 row or location count. The 2026 PDF itself, its publication date and its version were not accessible (HTTP 403 / DNS block).
- I could not confirm how many of the 2,700 announced sites are now active.

## Q2. Does any official dataset give coordinates or road segments for mobile camera locations?

### Takeaway
No. Every official mobile camera source found gives suburb and street names only. The PDF and CSV have no coordinates, no chainage and no segment geometry. The only official coordinates are for fixed and red-light cameras (November 2021 GPS workbook). For Highway Radar, the mobile list can be matched only to whole street or road names (for example against a road network), not to actual camera positions.

### Cited Findings
- The `nsw-speed-cameras` dataset has four resources:
  - "NSW speed camera GPS locations" XLSX, November 2021
  - Fixed Speed Cameras CSV
  - Red Light Speed Cameras CSV
  - Mobile Speed Cameras CSV, January 2022
  The dataset description says it "includes location points of Fixe[d]…" cameras. — [CKAN package_search](https://opendata.transport.nsw.gov.au/data/api/3/action/package_search?q=speed%20camera)
- I inspected the November 2021 GPS workbook (`gps-nov2021.xlsx`). It has only two sheets, "Fixed Speed Cameras" and "Red Light Speed Cameras". Their columns are `SZ?, SUBURB/TOWN, ROAD/S, Cameras, Lat(1), Long(1), Lat(2), Long(2)[, Lat(3), Long(3)]`. **There is no mobile camera sheet.** — [GPS XLSX](https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/a5c27c2d-5511-4fe8-863d-06a2d1a5ed10/download/nsw-speed-cameras-gps-locations-november-2021.xlsx)
- The mobile CSV (January 2022) has only `Suburb,Street` columns. Many Suburb cells list several suburbs, for example "Abbotsbury, Bossley Park, Edensor Park" for Cowpasture Road. — [Mobile CSV](https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/b4e0c74e-10a6-48e8-8421-2cd0294af6ae/download/mobile-speed-camera-locations-january-2022.csv)
- "The publicly available data does not include precise GPS coordinates. Instead, locations are described using: Suburbs, Roads, Approved routes." — [Tynan Motors, Dec 2025](https://www.tynan.com.au/blog/latest-sydney-nsw-mobile-speed-camera-locations-december-2025)
- The mobile camera list is "in suburb and then street location order. A more precise location is currently not known." — [speedcameralocations.com.au](https://www.speedcameralocations.com.au/nsw/mobile/)
- The Open Data Hub "Road and speed data" index (modified 2026-09-20) lists related datasets: Speed Zones, Road Segment Data, Sydney Region Carriageway, and others. None of them is a mobile camera geometry dataset. — [CKAN package_show road-and-speed-data](https://opendata.transport.nsw.gov.au/data/api/3/action/package_show?id=road-and-speed-data)

### Inferences
- What can be georeferenced from authoritative data:
  - **Yes, points:** fixed speed cameras and red-light speed cameras (Nov 2021 lat/long; some sites have 2–3 camera points), including the school zone flag `SZ?`. Coordinates are dated 2021, and newer fixed cameras will be missing.
  - **Street name only:** all mobile speed camera locations (January 2022 CSV, and the 2026 PDF by all accounts). Any point placed on these would be invented. At most, a street or road polyline could be shown by matching the name within the listed suburbs, and that is a derived approximation, not a camera location. Per the task constraint, no point coordinates should be made up.
- Third-party maps (SpeedCam, speedcamsau, speedcameralocations' user-report map) mix government lists with OpenStreetMap or community reports. They are not authoritative point data.

### Gaps
- I could not confirm whether the 2026 PDF adds any new fields (for example direction or segment limits). It is assumed not, based on the Dec 2025 descriptions.

## Q3. Program changes 2022–2026 (hours, warning signs, sites, portable or trailer cameras)

### Takeaway
- **2020:** signs removed and hours tripled to 21,000/month.
- **2021–2022:** rooftop signs returned first, then a promise of full signage.
- **2023:** full portable signage restored in April (two signs before and one after each vehicle), and hiding vehicles banned from February. Fines fell about 88% after the signs returned.
- **2024:** sites doubled by 2,700 with hours held at about 21,000/month, and seatbelt enforcement added to the existing mobile phone detection cameras from 1 July.
- **2025:** an average speed camera trial for light vehicles began. That is a fixed point-to-point program, not mobile.

### Cited Findings
- Late 2020: warning signs 250 m ahead were removed, vehicle livery was reduced, and the 45-car fleet's hours were tripled from 7,000 to 21,000 per month. Sign removal was reversed in April 2022 (per CarExpert). From February 2023, operators were banned from hiding vehicles behind objects. — [Newcastle Herald / CarExpert, May 2024](https://www.newcastleherald.com.au/story/8635930/more-speed-camera-sites-seatbelt-detection-tech-coming-to-australian-state/)
- Government account of the signs:
  - Rollout of full signage was completed in late April 2023, and "there are now two signs placed before and one after every mobile speed camera vehicle".
  - Mobile camera fines fell from 55,387 (May–Jun 2022) to about 6,650 (May–Jun 2023), an 88% reduction. The government describes this as a result of the signs; it is the government's own claim.
  - Rooftop signs were returned first, during the "backflips in 2021 and 2022".
  - The rollout was delayed because the signs did not fit in the new vehicles' boots.
  — [NSW Government media release, 16 Jul 2023](https://www.nsw.gov.au/media-releases/return-of-warning-signs-for-mobile-speed-cameras)
- Sources conflict on dates. CarExpert says the reversal came in "April 2022". Search summaries of other reports say full portable signage was promised in October 2022 to return from 1 January 2023, and that rooftop signs returned in October 2021 or "from February" of some year. The government release confirms only that "full signage" was completed in April 2023. — [NSW Gov](https://www.nsw.gov.au/media-releases/return-of-warning-signs-for-mobile-speed-cameras); [Newcastle Herald](https://www.newcastleherald.com.au/story/8635930/more-speed-camera-sites-seatbelt-detection-tech-coming-to-australian-state/); [MyNRMA (not fetched)](https://www.mynrma.com.au/open-road/news/archived/nsw-reverses-mobile-speed-camera-warning-sign-removal)
- April 2024 announcement:
  - "There will be no increase in mobile speed camera enforcement hours… [they] will remain at around 21,000 hours per month."
  - 2,700 new sites, 1,650 of them regional.
  - The public was invited to nominate sites.
  - "Seatbelt enforcement by existing mobile phone detection cameras commencing on July 1" 2024.
  — [NSW Government, 19 Apr 2024](https://www.nsw.gov.au/media-releases/nsw-government-zeroes-on-road-safety)
- The current SpeedCam guide (third party, updated 25 Oct 2026) describes signage "250 metres before, 50 metres before, and 50 metres after the camera vehicle" plus a rooftop sign, and says signs were "reintroduced in 2023". — [SpeedCam blog](https://www.speedcam.com.au/blog/speed-cameras-nsw/)
- Average speed cameras for light vehicles were trialled on the Pacific Highway (Kew–Lake Innes) and the Hume Highway (Coolac–Gundagai). Warning letters were issued from 1 May to 30 June 2025, then penalties applied. A report to Parliament is due in 2026. This is a fixed point-to-point program, not part of the mobile list. — [Big Rigs, 17 Mar 2025](https://bigrigs.com.au/2025/03/17/nsw-average-speed-cameras-to-extend-enforcement-beyond-heavy-vehicles/)
- **Non-speed cameras.** NSW runs trailer-mounted mobile cameras that detect mobile phone use and seatbelt non-compliance. "Transport for NSW does not release location information for these cameras, and no warning signs are required." **They are not in the published mobile speed camera list and should be marked as non-speed cameras.** — [Tynan Motors, Dec 2025](https://www.tynan.com.au/blog/latest-sydney-nsw-mobile-speed-camera-locations-december-2025)
- A third-party site keeps a separate user-reported map of "phone/seatbelt camera trailers". It is unofficial. — [speedcameralocations.com.au](https://www.speedcameralocations.com.au/nsw/mobile/)
- In 2021 the Joint Standing Committee on Road Safety (Staysafe) held an inquiry into mobile speed camera enforcement programs, self-referred on 24 May 2021, with more than 1,400 submissions. The government's response refers to a Mobile Speed Camera Community Education Plan. I did not read it; parliament.nsw.gov.au returned 403. — [Inquiry page](https://www.parliament.nsw.gov.au/committees/inquiries/Pages/inquiry-details.aspx?pk=2816); [Government response PDF](https://www.parliament.nsw.gov.au/ladocs/inquiries/2816/Government%20Response%20-%20Mobile%20speed%20camera%20enforcement%20programs%20in%20NSW.pdf)
- A TfNSW "2023 enforcement camera review" PDF exists at https://www.transport.nsw.gov.au/system/files/media/documents/2025/crs_2023_enforcement_camera_review.pdf. It was not accessible.

### Inferences
- I found no evidence of a NSW **portable or trailer-mounted speed** camera program. The trailers in NSW are phone and seatbelt detection units. Victoria trialled "mobile road safety trailer" cameras from July 2025, and WA and NZ run trailer programs, but those are outside NSW. Mobile speed enforcement in NSW remains vehicle-based.
- Hours appear unchanged at about 21,000/month from 2020 through at least April 2024. No 2025–2026 change was found.

### Gaps
- I found no 2025–2026 official statement of fleet size or hours.
- The exact dates for the rooftop signs (2021 vs 2022) conflict between sources.

## Q4. School zone speed cameras in 2026: how many, what was added after 2022, and where the lists are published

### Takeaway
I found no official 2026 count of school zone speed cameras. The authoritative machine-readable source is the November 2021 TfNSW open data:
- 39 fixed speed camera sites flagged as school zone (59 camera units)
- 12 red-light speed camera sites flagged as school zone (14 units)

Third-party sites in 2026 still show "39 school zone cameras", which looks like the same 2021 data. I found no announcement of new school zone speed cameras from 2022 to 2026. The 2025 initiative was a non-enforcement "smart sign" trial.

### Cited Findings
- My parse of `fixed.csv` / `gps-nov2021.xlsx` (Nov 2021):
  - Fixed Speed Cameras CSV: 122 rows, 39 with `SZ?`=Y.
  - Workbook fixed sheet: 39 school zone sites totalling 59 cameras.
  - Red Light Speed Cameras sheet: 12 school zone sites, 14 cameras.
  - Each row has lat/long, so these **are georeferenceable** as of 2021.
  — [Fixed CSV](https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/bcf2f6f4-ecfb-40e1-a807-0d5eb5f51507/download/fixed-speed-cameras_1.csv); [GPS XLSX](https://opendata.transport.nsw.gov.au/data/dataset/fb34bd89-443a-448c-a4a5-7c8caab70c44/resource/a5c27c2d-5511-4fe8-863d-06a2d1a5ed10/download/nsw-speed-cameras-gps-locations-november-2021.xlsx)
- speedcamsau.com reports "39 school zone cameras mapped" in NSW (data checked 26 Oct 2026). Per a search summary, it uses the TfNSW open dataset with newer cameras added from OpenStreetMap. — [speedcamsau school zone](https://speedcamsau.com/cameras/school-zone)
- Official description of the program:
  - "Fixed speed cameras operate in school zones."
  - "All school zone speed cameras operate 24 hours per day, seven days a week… [and] are also programmed to detect vehicles exceeding the 40km/h school zone speed limit during sign-posted school zone times."
  - Flashing lights are installed at "all fixed speed camera sites that are located in a school zone".
  - Locations are "listed on the Centre for Road Safety website".
  — [NSW Government – Fixed and mobile speed cameras (managed by TfNSW)](https://www.nsw.gov.au/driving-boating-and-transport/driving-nsw/roads-safety-and-rules/safe-driving/speed-limits-and-cameras/fixed-and-mobile-speed-cameras)
- TfNSW has published a separate list of school zone cameras as a PDF titled "Fixed speed cameras - school zones" (2023 path). I found it only in search results and could not access it. Current fixed and red-light lists are also under 2026 paths, for example `crs_red_light_speed_cameras_3.pdf`. — [TfNSW school zone cameras PDF 2023 (not accessible)](https://www.transport.nsw.gov.au/system/files/media/documents/2023/Fixed%20speed%20cameras%20-%20school%20zones.pdf); [TfNSW red-light PDF 2026 (not accessible)](https://www.transport.nsw.gov.au/system/files/media/documents/2026/crs_red_light_speed_cameras_3.pdf)
- Older, undated figures:
  - McKillop Legal: "127 fixed digital speed cameras… of which 57 are in school zones". This likely counts camera units, not sites, and is close to my count of 59 units.
  - An older NSW Government parliamentary submission: fixed cameras issuing infringements in 36 school zones, plus 2 issuing warning letters.
  — [McKillop Legal](https://mckilloplegal.com.au/school-zones-pupil-free-days/); [NSW Gov submission](https://www.parliament.nsw.gov.au/ladocs/submissions/47956/Submission%2025%20-%20NSW%20Government.pdf)
- 2024/25 enforcement data (NRMA, 2026 press release):
  - 125,783 school zone speeding fines, about 6,000 fewer than the previous year.
  - About $35 million in school zone speed and red-light fines.
  - Top sites: Woodville Rd, Chester Hill (8,240); Victoria Rd, Ryde (6,307); Princes Hwy, Kogarah (4,754); Lane Cove Rd, North Ryde (4,391); Bunnerong Rd, Maroubra (4,027).
  - The NRMA called for physical audits of school zones, not new cameras.
  — [NRMA 2026](https://www.mynrma.com.au/about/media/press-releases/2026/cameras-failing-keeping-kids-safe-school-zones)
- Bunnerong Road, Maroubra appears in the 2024/25 top five. I did not check whether it is in the 2021 open data list. That is worth checking as a possible post-2021 camera.
- July 2025: a "nation-first trial of smart school zone signs" at Wallsend South Public School, with radar, pedestrian sensors and AI. The government said: "We are not using these signs for compliance, and they will not capture or store any images." This is **not** an enforcement camera. — [NSW Government ministerial release (search result)](https://www.nsw.gov.au/ministerial-releases/nsw-leads-way-nation-first-trial-of-smart-school-zone-signs-%E2%80%93-technology-pioneered-hunter)
- April 2024: TfNSW was to review "up to 35 school precincts". No camera additions were stated. — [NSW Government, 19 Apr 2024](https://www.nsw.gov.au/media-releases/nsw-government-zeroes-on-road-safety)
- Mobile speed cameras can also enforce school zones; the source is third-party and does not cite this. — [SpeedCam school zone blog](https://www.speedcam.com.au/blog/school-zone-speed-cameras-nsw/)

### Inferences
- For Highway Radar, the school zone fixed and red-light cameras in the November 2021 open data are the only authoritative point coordinates, and they should be tagged "school zone" via `SZ?`=Y. Any cameras installed after November 2021 would come only from the 2026 TfNSW PDFs, which give street descriptions, not coordinates. Those PDFs could not be checked here.
- The data shows no evidence of a large school zone camera expansion from 2022 to 2026. The count is about 39 sites in both 2021 and 2026 per third parties. This rests on the absence of announcements found, not on official confirmation.

### Gaps
- No official 2026 count of school zone camera sites, and no list of any installed after 2021. The TfNSW PDFs and pages were inaccessible.
- The 2023 "Fixed speed cameras - school zones" PDF could not be read to compare against 2021.
- I did not determine whether the open data `SZ?` flag is maintained or frozen at 2021. The dataset was last modified 2023-08-21.
