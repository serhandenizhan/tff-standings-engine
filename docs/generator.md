# Random season generator and scenario library

Status: first version (2026-11-12). Replaces the earlier draft that constructed guaranteed ties (rejected by the student, see CLAUDE.md, Kararlar).

## Goal

Generate realistic, random Süper Lig seasons for the 18 clubs of 2026-27, keep a small fixed library of 12 such seasons in the repository, and let the program and the tests pick one of them (seeded). Ties are **never constructed**: whatever ties appear come from the random results.

## Classes (package `edu.svv.standings.generator`)

| Class | Role |
|---|---|
| `TeamStrength` | record: club, attack, defence, points per game, average position, number of window seasons, `fromPromotedAverage` |
| `StrengthModel` | loads `strength/super-lig-seasons.csv` and `strength/super-lig-2026-27-clubs.txt` from the classpath and computes the strengths and the league mean μ |
| `SeasonGenerator` | `new SeasonGenerator(seed).generate(rounds)`; double round robin, Poisson goals, forfeits, deductions |
| `GeneratedSeason` | record: teams, matches, deductions, description; `writeCsv(dir)` writes the project CSV format |
| `ScenarioLibraryBuilder` | `main`: searches seeds, classifies seasons with its own small classifier, writes the 12 scenarios and the catalogue |
| `ScenarioLibrary` | reads the catalogue and the scenarios from the classpath (via `CsvLoader`), `pickRandomId(seed)` |
| `RealismCheck` | `main`: validation run used for the table at the end of this document (the only place that calls `StandingsCalculator`) |

## Modelling choices and assumptions

### Strength (data: last 10 seasons, 2016-17 to 2025-26)

- **Attack** of a club = total goals scored / total matches played over the club's Süper Lig seasons in the window; **defence** = total goals conceded / total matches played (higher = weaker defence). Totals are pooled (match-weighted) because the seasons had 34 to 40 matches.
- Clubs that went up and down use only their Süper Lig seasons in the window (student decision). Number of window seasons per club is shown in the realism table.
- **Clubs with no Süper Lig season in the window** (Amed SFK, Çorum FK) get the **promoted-club average**: all rows of clubs that were in season *t* but not in season *t*−1, for *t* ≥ 2017-18 (2016-17 is the first season, so its predecessor is unknown). That gives 27 rows: Göztepe, Sivasspor, Yeni Malatyaspor (2017-18); Çaykur Rizespor, Ankaragücü, Erzurumspor FK (2018-19); Gaziantep FK, Gençlerbirliği, Denizlispor (2019-20); Hatayspor, Fatih Karagümrük, Erzurumspor FK (2020-21); Adana Demirspor, Giresunspor, Altay (2021-22); Ankaragücü, İstanbulspor, Ümraniyespor (2022-23); Çaykur Rizespor, Samsunspor, Pendikspor (2023-24); Eyüpspor, Göztepe, Bodrumspor (2024-25); Kocaelispor, Gençlerbirliği, Fatih Karagümrük (2025-26). Result: attack 1.22, defence 1.45, 1.19 points per game, average position 12.1.
- **League mean μ** = total goals / total matches over all 189 rows = **1.407 goals per team per match** (2.81 per match).
- Points per game (3W + D, i.e. before deductions, because a deduction is a disciplinary matter and not playing strength) and average final position are kept only for documentation and validation; they are not used to generate results.
- Data are used as published. The 2022-23 rows of Gaziantep FK and Hatayspor include the matches after their withdrawal (see data-sources.md); this lowers Gaziantep FK's strength a little. Accepted as is (assumption).

### Match model

- Goals of each side are independent Poisson variables:
  λ_home = μ · (att_home/μ) · (def_away/μ) · H, λ_away = μ · (att_away/μ) · (def_home/μ) / H.
- **Home advantage H = 1.10.** For two equal teams λ_home/λ_away = H² ≈ 1.21, so the home side scores about 55 % of the goals. Assumption: typical home share in European top leagues and the Süper Lig is around 54-56 %; the CSV has no home/away split, so H could not be measured from our data.
- Poisson sampling is implemented by hand (Knuth's multiplication method) using only `java.util.Random(seed)`. Same seed and same data ⇒ identical season.
- Known simplifications: no correlation between the two scores, no draw inflation, strengths fixed for the whole season, no form or injuries.

### Schedule

- Circle method: one club stays fixed, the others rotate; 17 rounds in the first half, every pair meets once. The second half repeats the 17 rounds with home and away swapped (34 rounds, 306 matches).
- The club order fed into the circle method is shuffled with the seed, so each seed has a different fixture list.
- Home/away inside a round follows a simple parity rule (balanced overall, but no "break" minimisation; assumption: good enough for this purpose).
- **Mid-season table = first k rounds.** The generator always draws all 34 rounds and then cuts, so `generate(k)` is exactly the first k rounds of `generate(34)` for the same seed. After k rounds a pair has played 0, 1 or 2 mutual matches, as in a real season.

### Forfeits and deductions

- **Forfeit:** each match is forfeited with probability **1 %** (student decision, about 3 per full season); the forfeiting side is chosen 50/50. The on-pitch score is stored; FMT 23/4 is applied by the calculator. A generated forfeit does **not** automatically add a 3-point deduction (FMT 24/1-a): it is modelled as a forfeit declared afterwards (for example an ineligible player), not as a no-show (assumption).
- **Point deduction:** each club gets a 3-point deduction with probability **2 %** per season (about 30 % of seasons have at least one). The round in which it is announced is uniform in 1..34; a mid-season table only contains deductions announced up to that round.

## Scenario library

Built with

```
mvn -q package -DskipTests
java -cp target/classes edu.svv.standings.generator.ScenarioLibraryBuilder
```

Output: `src/main/resources/scenarios/S01..S12/` (`teams.csv`, `matches.csv`, `deductions.csv`, project CSV format) and `src/main/resources/scenarios/catalogue.csv` (`id;seed;rounds;flags;description`). The build is deterministic: re-running produces identical files.

### Selection

Each scenario has a rule (allowed round counts, required flags, forbidden flags). Seeds are scanned from 1 upwards (up to 20 000); the first seed that matches and has not been used by an earlier scenario is taken. So the library is random seasons, filtered for variety, not constructed seasons.

### Independent classifier (inside the builder)

The classifier does **not** call `StandingsCalculator` or `TieBreaker`, so the choice of test data does not depend on the code under test. It computes:

- raw points = 3W + D − deductions; in a forfeited match the forfeiting side loses and the goals are adjusted as in FMT 23/4 (re-implemented in a few lines so that raw points are right);
- `NO_TIE`: no two clubs on the same raw points; `TIE_2`: a points group of exactly 2; `TIE_3PLUS`: a points group of 3 or more;
- `FORFEIT`, `DEDUCTION`: at least one in the season;
- `LIKELY_UNRESOLVED`: a whole points group whose members have the same goal difference, goals scored and forfeit-loss status, **and** the same points, goal difference and goals in the mini table of their mutual matches (all zero if they have not met). No computable FMT 9/2 criterion can separate such a group. Partial unresolved sub-groups are deliberately not searched, so the flag is conservative: the calculator can mark more teams unresolved than the classifier (it does in S06 and S08), but every group the classifier flags was also flagged by the calculator.

### Catalogue

| ID | Seed | Rounds | Flags | Purpose | Summary |
|---|---|---|---|---|---|
| S01 | 2470 | 34 | NO_TIE | full season, no points tie at all | 306 matches, 0 two-team tie(s), 0 3+-team tie(s), 0 forfeit(s), 0 deduction(s) |
| S02 | 7 | 34 | TIE_2 | full season, only 2-team ties | 306 matches, 2 two-team tie(s), 0 3+-team tie(s), 0 forfeit(s), 0 deduction(s) |
| S03 | 6 | 34 | TIE_2, TIE_3PLUS, FORFEIT | full season with a 3+-team tie | 306 matches, 2 two-team tie(s), 1 3+-team tie(s) (largest 3), 4 forfeit(s), 0 deduction(s) |
| S04 | 12199 | 34 | TIE_2, TIE_3PLUS, FORFEIT, DEDUCTION, LIKELY_UNRESOLVED | full season containing everything | 306 matches, 2 two-team tie(s), 1 3+-team tie(s) (largest 3), 2 forfeit(s), 1 deduction(s), likely unresolved: 2 teams on 31 pts |
| S05 | 4 | 34 | NO_TIE, FORFEIT, DEDUCTION | full season, forfeit + deduction, no 3+-team tie | 306 matches, 0 two-team tie(s), 0 3+-team tie(s), 3 forfeit(s), 1 deduction(s) |
| S06 | 9 | 1 | TIE_2, TIE_3PLUS, LIKELY_UNRESOLVED | round 1, likely fully unresolved group | 9 matches, 1 two-team tie(s), 2 3+-team tie(s) (largest 8), 0 forfeit(s), 0 deduction(s), likely unresolved: 2 teams on 1 pts |
| S07 | 15 | 2 | TIE_2, TIE_3PLUS, FORFEIT | round 2, early table with a forfeit | 18 matches, 1 two-team tie(s), 3 3+-team tie(s) (largest 6), 2 forfeit(s), 0 deduction(s) |
| S08 | 89 | 3 | TIE_2, TIE_3PLUS, DEDUCTION, LIKELY_UNRESOLVED | round 3, likely unresolved group and an early deduction | 27 matches, 2 two-team tie(s), 2 3+-team tie(s) (largest 5), 0 forfeit(s), 1 deduction(s), likely unresolved: 2 teams on 2 pts |
| S09 | 1 | 10 | TIE_2 | first third, only 2-team ties | 90 matches, 5 two-team tie(s), 0 3+-team tie(s), 0 forfeit(s), 0 deduction(s) |
| S10 | 23 | 17 | TIE_2, TIE_3PLUS, FORFEIT, DEDUCTION | half season (end of first half), forfeit + deduction | 153 matches, 4 two-team tie(s), 1 3+-team tie(s) (largest 3), 3 forfeit(s), 1 deduction(s) |
| S11 | 144 | 26 | TIE_2, TIE_3PLUS, FORFEIT, LIKELY_UNRESOLVED | late mid-season, likely unresolved group | 234 matches, 2 two-team tie(s), 1 3+-team tie(s) (largest 3), 3 forfeit(s), 0 deduction(s), likely unresolved: 2 teams on 27 pts |
| S12 | 2 | 30 | TIE_2, TIE_3PLUS, FORFEIT | run-in, several ties and a forfeit | 270 matches, 2 two-team tie(s), 1 3+-team tie(s) (largest 3), 2 forfeit(s), 0 deduction(s) |

Coverage: no tie (S01, S05); only 2-team ties (S02, S09); 3+-team ties (S03, S04, S06-S08, S10-S12); forfeit + deduction (S04, S05, S10); a season with all of these plus a likely-unresolved group (S04); likely unresolved (S04, S06, S08, S11); 5 full seasons (S01-S05) and 7 mid-season tables (rounds 1, 2, 3, 10, 17, 26, 30). Regenerated on 2026-11-13 after the DEF-005 schedule fix (new seeds, same selection rules).

### Usage

- Library: `new ScenarioLibrary().ids()`, `load(id)`, `pickRandomId(seed)`.
- CLI: `java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario S04` or `--scenario random 42`.

## Realism check

`java -cp target/classes edu.svv.standings.generator.RealismCheck 200` — 200 full seasons (seeds 1..200), ranked with `StandingsCalculator` (only here). Strength rank = goals difference per game (attack − defence). "Real avg pos" is the plain mean of final positions in the window (leagues had 18-21 clubs); "Bottom 3" counts finishes in positions 16-18.

```
Seasons: 200, mu = 1.407 goals per team per match, H = 1.10
Simulated goals per match: 2.68 (real window: 2.81), home share of goals: 54.8%

| Strength rank | Club | Real seasons | Attack | Defence | Real avg pos | Real PPG | Sim avg pos | Sim PPG | Titles | Bottom 3 |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Galatasaray | 10 | 2.07 | 0.97 | 3.1 | 2.13 | 2.0 | 2.07 | 96 | 0 |
| 2 | Fenerbahçe | 10 | 2.06 | 1.08 | 3.1 | 2.03 | 2.4 | 1.98 | 58 | 0 |
| 3 | Beşiktaş | 10 | 1.86 | 1.11 | 3.5 | 1.88 | 3.5 | 1.83 | 27 | 0 |
| 4 | İstanbul Başakşehir | 10 | 1.58 | 1.06 | 4.3 | 1.75 | 4.7 | 1.69 | 11 | 0 |
| 5 | Trabzonspor | 10 | 1.71 | 1.21 | 4.1 | 1.76 | 4.7 | 1.69 | 8 | 0 |
| 6 | Samsunspor | 3 | 1.32 | 1.28 | 7.7 | 1.46 | 8.6 | 1.39 | 0 | 8 |
| 7 | Alanyaspor | 10 | 1.46 | 1.45 | 9.6 | 1.34 | 9.1 | 1.36 | 0 | 9 |
| 8 | Konyaspor | 10 | 1.25 | 1.30 | 10.3 | 1.27 | 9.9 | 1.31 | 0 | 18 |
| 9 | Göztepe | 7 | 1.32 | 1.44 | 10.7 | 1.25 | 11.4 | 1.22 | 0 | 33 |
| 10 | Eyüpspor | 2 | 1.21 | 1.36 | 10.5 | 1.23 | 11.3 | 1.23 | 0 | 32 |
| 11 | Kasımpaşa | 10 | 1.47 | 1.62 | 10.5 | 1.26 | 11.4 | 1.23 | 0 | 30 |
| 12 | Amed SFK | 0 (promoted avg) | 1.22 | 1.45 | 12.1 | 1.19 | 12.0 | 1.17 | 0 | 37 |
| 13 | Çorum FK | 0 (promoted avg) | 1.22 | 1.45 | 12.1 | 1.19 | 11.7 | 1.21 | 0 | 42 |
| 14 | Gaziantep FK | 7 | 1.27 | 1.54 | 12.4 | 1.18 | 12.3 | 1.17 | 0 | 46 |
| 15 | Çaykur Rizespor | 8 | 1.30 | 1.63 | 12.3 | 1.17 | 12.7 | 1.14 | 0 | 62 |
| 16 | Kocaelispor | 1 | 0.76 | 1.12 | 10.0 | 1.09 | 14.3 | 1.05 | 0 | 91 |
| 17 | Erzurumspor FK | 2 | 1.08 | 1.50 | 17.5 | 1.01 | 14.2 | 1.04 | 0 | 93 |
| 18 | Gençlerbirliği | 5 | 1.07 | 1.52 | 14.2 | 1.06 | 14.7 | 1.03 | 0 | 99 |

Titles won by a bottom-5-strength club: 0 of 200 seasons (0.0%)
```

Reading:

- The five strongest clubs by the model take all 200 titles; Galatasaray and Fenerbahçe win most often, as in the real window. (Numbers re-run on 2026-11-13 after the DEF-005 schedule fix; before the fix Samsunspor took one title.)
- Simulated points per game are close to the real values for clubs with many seasons (differences mostly under 0.1).
- No bottom-5-strength club won the title in 200 seasons.
- Simulated goals per match (2.68) are a little below the real window mean (2.81) because the 2026-27 field contains several low-scoring clubs (Kocaelispor's single season: 26 goals in 34 matches). The home share comes out at about 55 % (54.8 %), as intended by H.
- Kocaelispor's simulated position (14.4) is much worse than its single real season (10th) because that season was very low scoring at both ends; its strength comes from one season only. Erzurumspor FK is the opposite case (real 17.5, simulated 14.2): its two real seasons were in leagues of 18 and 21 clubs.
