# Data sources for the strength model

Collected on 2026-11-12. Output files:

- `src/main/resources/strength/super-lig-seasons.csv` (189 rows, 10 seasons)
- `src/main/resources/strength/super-lig-2026-27-clubs.txt` (18 clubs)

## Primary source (final tables)

English Wikipedia season pages, final league table (rendered HTML, `action=render`):

| Season | URL |
|---|---|
| 2016-17 | https://en.wikipedia.org/wiki/2016%E2%80%9317_S%C3%BCper_Lig |
| 2017-18 | https://en.wikipedia.org/wiki/2017%E2%80%9318_S%C3%BCper_Lig |
| 2018-19 | https://en.wikipedia.org/wiki/2018%E2%80%9319_S%C3%BCper_Lig |
| 2019-20 | https://en.wikipedia.org/wiki/2019%E2%80%9320_S%C3%BCper_Lig |
| 2020-21 | https://en.wikipedia.org/wiki/2020%E2%80%9321_S%C3%BCper_Lig |
| 2021-22 | https://en.wikipedia.org/wiki/2021%E2%80%9322_S%C3%BCper_Lig |
| 2022-23 | https://en.wikipedia.org/wiki/2022%E2%80%9323_S%C3%BCper_Lig |
| 2023-24 | https://en.wikipedia.org/wiki/2023%E2%80%9324_S%C3%BCper_Lig |
| 2024-25 | https://en.wikipedia.org/wiki/2024%E2%80%9325_S%C3%BCper_Lig |
| 2025-26 | https://en.wikipedia.org/wiki/2025%E2%80%9326_S%C3%BCper_Lig |

Secondary source (independent cross-check of every row): Turkish Wikipedia pages
`https://tr.wikipedia.org/wiki/<YYYY-YY>_S%C3%BCper_Lig` (hyphen in the title, e.g. `2016-17_Süper_Lig`) for all ten seasons.

## 2026-27 club list

- https://en.wikipedia.org/wiki/2026%E2%80%9327_S%C3%BCper_Lig (Teams section: 18 clubs)
- https://tr.wikipedia.org/wiki/2026-27_S%C3%BCper_Lig (club table: same 18 clubs)

The 2026-27 season is in progress (11 matchdays played when fetched). Only the club list is used, not the partial table.
The English page states that Erzurumspor was promoted after 5 seasons of absence, and that Amed and Çorum were promoted for the first time in their histories.

## Checks run (all on the final data)

For every season:

1. Number of rows equals the number of teams, positions run 1..N without gaps.
2. W + D + L = played for every club.
3. Sum of GF = sum of GA. PASS in all 10 seasons.
4. Sum of W = sum of L, and sum of D is even. PASS in all 10 seasons.
5. Sum of played = N x (N - 1), i.e. a full double round robin. PASS in all 10 seasons
   (2022-23 and 2024-25 had 19 clubs, 2020-21 had 21, 2021-22 and 2023-24 had 20, the rest 18).
6. Points = 3W + D. PASS everywhere except the four documented deductions below.
7. Every row (position, P, W, D, L, GF, GA, Pts) compared against Turkish Wikipedia. All 189 rows match except the 2024-25 tie-break issue below.

## Point deductions found (points are stored as shown in the table, i.e. after the deduction)

| Season | Club | Table points | 3W + D | Deduction |
|---|---|---|---|---|
| 2022-23 | Kayserispor | 47 | 50 | -3 (decision of the Turkish Football Federation) |
| 2023-24 | Kayserispor | 42 | 45 | -3 |
| 2023-24 | İstanbulspor | 16 | 19 | -3 |
| 2024-25 | Adana Demirspor | 2 | 14 | -12 |

## Discrepancy resolved: 2024-25 positions 12 to 14

Alanyaspor, Kayserispor and Gaziantep FK all finished on 45 points. English Wikipedia lists them as Gaziantep FK 12th,
Alanyaspor 13th, Kayserispor 14th. Turkish Wikipedia lists Alanyaspor 12th, Kayserispor 13th, Gaziantep FK 14th.
The English page's own results matrix gives the head-to-head mini-table (the first tie-break in the rules shown on the page):
Alanyaspor 7 pts, Kayserispor 5 pts, Gaziantep 4 pts. That agrees with Turkish Wikipedia, so the CSV uses
Alanyaspor 12, Kayserispor 13, Gaziantep FK 14. W/D/L/GF/GA/points are identical in both sources.

## Club name normalisation (Wikipedia name -> CSV name)

- Başakşehir / Medipol Başakşehir -> İstanbul Başakşehir
- Rizespor -> Çaykur Rizespor
- Gaziantep (2019-20 onward; Gazişehir Gaziantep in some seasons) -> Gaziantep FK
- Gaziantepspor (2016-17) is a different, defunct club and is kept as `Gaziantepspor`
- Akhisar Belediyespor -> Akhisarspor
- Kardemir Karabükspor -> Karabükspor
- BB Erzurumspor (2018-19, 2020-21) -> Erzurumspor FK. The Wikipedia pages link both names to the article "Erzurumspor F.K.", and the 2026-27 page says Erzurumspor returns after 5 seasons of absence, so they are treated as the same club.
- Bodrum -> Bodrumspor
- Çorum -> Çorum FK; Amed -> Amed SFK (the form used on Turkish Wikipedia for 2026-27)
- Unchanged: Fatih Karagümrük, Eyüpspor, Kocaelispor, Samsunspor, Pendikspor, İstanbulspor, Ümraniyespor, Giresunspor, Altay, Denizlispor, Yeni Malatyaspor, Osmanlıspor, Adanaspor, Ankaragücü, Gençlerbirliği and the rest.

## Other notes and items not verifiable

- 2022-23: the English page says Gaziantep and Hatayspor withdrew on 12 February 2023 after the earthquake and were not relegated. Their table rows still show 36 played and were kept as published (both sources agree). I did not verify how those matches were recorded.
- Positions come from the tables as published; tie-breaks were not recomputed except for the 2024-25 case above.
- Only Wikipedia (English and Turkish) was used. tff.org was not fetched, so the figures are not checked against the federation's own pages.
- The 2026-27 club list reflects Wikipedia at fetch time; the whole page for that season is a live, evolving source.
