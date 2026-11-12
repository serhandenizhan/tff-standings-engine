# Test Plan — TFF Standings Engine

> Version 1.0 — 2026-11-12. Basis: `docs/requirements.md` v1.1 (FR-01..FR-15, V-01..V-08, A-01..A-04, D-01..D-03) and the TFF rule sources quoted in `CLAUDE.md` (FMT 9/1, 9/2-a, 9/2-b, 23/4, 24/1-a, FDT 33).
> All expected results in this plan were derived **by hand from the requirements and the TFF rules**, not from the implementation (black-box). The source code was consulted only for public class / method names.

**Notation used in all test data**

- `A–B 2-1` = match with home team A, away team B, recorded (on-pitch) score 2-1. `F=HOME` / `F=AWAY` = that side forfeited (FR-06); no suffix = `Forfeit.NONE`.
- `(T,n)` = point deduction of n points for team T.
- Expected tables: `pos Team pts` ; `U` = flagged unresolved (FR-12). GD = goal difference (A-01), GF = goals for, h2h = head-to-head (mutual matches), mini = mini-table of the tied teams (FR-10).
- Team input order is given only where it matters (D-02 display order tests); it is deliberately **not** alphabetical there.

---

## 1. Test strategy

### 1.1 Levels

| Level | Object under test | Goal |
|---|---|---|
| Unit | `model.Team`, `model.MatchResult`, `model.PointDeduction` | Object-level validation (V-01, V-02, V-04, V-05, V-07), D-01 name semantics |
| Component | `StandingsCalculator.calculate(teams, matches, deductions)` → `List<StandingRow>` (stats, points, forfeit, deductions, cross-object validation, full FMT 9/2 tie-breaking) | Core business rules FR-04..FR-13, V-01, V-03, V-06, V-07 |
| Component | `io.CsvLoader` (+ `io.InputFormatException`) | FR-14, V-08 |
| CLI / system | `cli.TableFormatter`, `cli.Main` (end-to-end: CSV files → stdout) | FR-14, FR-15, V-08, wrong arguments |
| Property / metamorphic | `generator.SeasonGenerator` (seeded) + calculator | Invariants over many generated seasons; tie scenarios TWO_TEAM_TIE / THREE_TEAM_TIE / FULLY_UNRESOLVED |

### 1.2 Approach

- **Black-box first.** Test cases come from equivalence partitioning (§2), boundary value analysis (§3), decision tables (§4), negative testing (§5) and random/metamorphic testing (§6). White-box information (JaCoCo) is used **only after** execution, to find untested branches and justify any extra case.
- **Discriminating data.** Every tie-break case is built so that the *wrong* criterion (or wrong order of criteria) would give a *different* table. Typical trick: the team that must win the tie has the *worse* overall goal difference, or is alphabetically later. The rationale column names the fault each case would expose.
- **Hand oracle.** Expected values are written in this plan with short arithmetic, so the student can defend them orally and the JUnit test can be reviewed against the plan line by line.

### 1.3 Tools

- JUnit 5 (Jupiter): `@Test`, `@ParameterizedTest` with `@CsvSource` (small tabular rows: goals, forfeit boundaries, deduction points) and `@MethodSource` (league fixtures, seeds), `@Nested` (group by requirement: Stats / Forfeit / Deductions / Validation), `@DisplayName("TC-0xx …")` so the surefire report maps 1:1 to this plan, `assertThrows` (+ message check), `assertAll` (one row, many fields), `@TempDir` (CSV/CLI tests).
- JaCoCo 0.8.13 via `mvn verify` → `target/site/jacoco/index.html` (line + branch coverage, reported, **not** a target in itself).
- Maven Surefire for execution reports.

### 1.4 Entry criteria

- Requirements v1.1 approved (done).
- Code compiles (`mvn compile`), public API of the planned classes exists (for `io`, `cli`, `generator` as soon as those agents deliver).
- This plan reviewed by the student.

### 1.5 Exit criteria

- Every TC in §7 implemented and executed; every FR, V and decision-table rule has ≥ 1 executed case (§9).
- Every failing case has a defect entry in `docs/defect-log.md`; no open defect of severity *High* (wrong ranking / wrong points) at hand-in.
- Full suite green after the last fix (regression, §1.7). Coverage report generated and uncovered branches explained.

### 1.6 Recording results and defects

- The TC table (§7) has an **Actual** column, left as `—` now; filled after execution with `Pass` or `Fail → DEF-nn`.
- A failing test is committed **red first** (project decision), then logged in `docs/defect-log.md` with: ID, TC, requirement, input, expected, actual, root cause, severity, fix commit, regression test.
- A test that fails because the *plan* was wrong (hand-calculation error) is not a defect: the plan is corrected and the correction noted in the log as "test defect".

### 1.7 Regression strategy

- After every fix the **full** suite is re-run (`mvn verify`), not only the failing class — tie-breaking changes easily break unrelated orderings.
- Each defect gets a dedicated regression test (either the original TC, or a new minimal TC named `TC-Rnn` referencing the DEF ID) that stays in the suite.
- The property-based class (§6) re-runs with the same fixed seeds, so a regression is reproducible.

---

## 2. Equivalence partitioning

Valid classes are prefixed **V**, invalid **I** in the "Type" column.

### 2.1 Team name / team list (FR-01, V-01, V-02, D-01)

| ID | Partition | Type | Representative | TC |
|---|---|---|---|---|
| EP-01 | Non-blank ASCII name | V | `Sivasspor`, `X` | TC-001 |
| EP-02 | Name with Turkish letters (Ç Ğ İ I Ö Ş Ü ı) | V | `Beşiktaş`, `İstanbul Başakşehir` | TC-001, TC-080 |
| EP-03 | Name with internal spaces | V | `Çaykur Rizespor` | TC-001 |
| EP-04 | Name with leading/trailing whitespace (same team as trimmed name, D-01) | V | `" Beşiktaş "` | TC-003, TC-045, TC-082 |
| EP-05 | Empty / whitespace-only name | I | `""`, `" "`, `"\t"` | TC-002 |
| EP-06 | Null name | I | `null` | TC-002 |
| EP-07 | Exact duplicate name in team list | I | `[A, A]` | TC-042 |
| EP-08 | Duplicate after trimming | I | `[Beşiktaş, " Beşiktaş "]` | TC-042 |
| EP-09 | Names differing only in case (distinct teams, D-01) | V | `Beşiktaş`, `beşiktaş` | TC-003, TC-043 |
| EP-10 | Empty team list | V (assumption) | `[]` | TC-016 |
| EP-11 | Null team list / list containing null | I | `null`, `[A, null]` | TC-040 |

### 2.2 Goals (FR-02, V-05)

| ID | Partition | Type | Representative | TC |
|---|---|---|---|---|
| EP-12 | Zero goals | V | 0 | TC-004, TC-011 |
| EP-13 | Positive goals | V | 1..9 | TC-010 |
| EP-14 | Negative goals (home or away) | I | -1 | TC-005, TC-088 |

### 2.3 Match participants (FR-02, V-01, V-03, V-04)

| ID | Partition | Type | TC |
|---|---|---|---|
| EP-15 | Two distinct registered teams | V | TC-010 |
| EP-16 | Home or away team not registered | I | TC-044 |
| EP-17 | Same team on both sides (also after trimming) | I | TC-006 |
| EP-18 | Null home / away team | I | TC-007 |

### 2.4 Forfeit (FR-06, FMT 23/4)

| ID | Partition | Type | TC |
|---|---|---|---|
| EP-19 | NONE (explicit or 4-arg constructor / 4-field CSV line) | V | TC-004, TC-081 |
| EP-20 | HOME forfeited, away (winner) actual goals ≤ 3 | V | TC-021 |
| EP-21 | HOME forfeited, away actual goals > 3 | V | TC-021 |
| EP-22 | AWAY forfeited, home (winner) actual goals ≤ 3 | V | TC-020 |
| EP-23 | AWAY forfeited, home actual goals > 3 | V | TC-020 |
| EP-24 | Forfeiting side was leading / level on the pitch | V | TC-022 |
| EP-25 | Null forfeit value | V (treated as NONE, Q3) | TC-007 |

### 2.5 Point deductions (FR-07, V-07, FDT 33)

| ID | Partition | Type | TC |
|---|---|---|---|
| EP-26 | Points ≥ 1 for a registered team | V | TC-008, TC-030 |
| EP-27 | Points = 0 | I | TC-008, TC-089 |
| EP-28 | Points < 0 | I | TC-008 |
| EP-29 | Deduction for unregistered team | I | TC-035 |
| EP-30 | Several deductions for the same team (accumulate) | V | TC-030 |
| EP-31 | Null deduction / null team in deduction | I | TC-009, TC-040 |
| EP-32 | Resulting points > 0 / = 0 / < 0 | V | TC-030, TC-031 |

### 2.6 Ties (FR-08..FR-12)

| ID | Partition | Type | TC |
|---|---|---|---|
| EP-33 | No tie (all point totals distinct) | V | TC-012 |
| EP-34 | 2 teams level (FMT 9/2-a) | V | TC-050..TC-061 |
| EP-35 | 3 teams level (FMT 9/2-b) | V | TC-065..TC-075, TC-077 |
| EP-36 | ≥ 4 teams level | V | TC-076, TC-078 |
| EP-37 | Several separate tie groups in one table | V | TC-061 |
| EP-38 | Tie fully resolved / partially resolved / unresolved | V | TC-065 / TC-070 / TC-071 |
| EP-39 | Mutual matches between 2 tied teams: 0 / 1 / 2 | V | TC-060 / TC-051 / TC-050 |
| EP-40 | Mini-table incomplete (some tied teams have not met, A-03) | V | TC-075 |

### 2.7 Fixtures per pair (FR-03, V-06)

| ID | Partition | Type | TC |
|---|---|---|---|
| EP-41 | 0, 1 or 2 matches, at most one per direction | V | TC-047 |
| EP-42 | Same direction twice | I | TC-046 |
| EP-43 | Three or more matches | I | TC-048 |

### 2.8 CSV line shapes and files (FR-14, V-08)

| ID | File | Partition | Type | TC |
|---|---|---|---|---|
| EP-44 | teams | 1 field | V | TC-080 |
| EP-45 | teams | ≥ 2 fields (`A;B`) | I (assumption) | TC-084 |
| EP-46 | matches | 4 fields (forfeit omitted → NONE) | V | TC-081 |
| EP-47 | matches | 5 fields, forfeit ∈ {NONE, HOME, AWAY} | V | TC-081 |
| EP-48 | matches | ≤ 3 or ≥ 6 fields | I | TC-084 |
| EP-49 | matches | non-integer goals (`x`, `1.5`, empty) | I | TC-085 |
| EP-50 | matches | unknown forfeit value (`XYZ`) | I | TC-086 |
| EP-51 | matches | lowercase forfeit (`home`) | ambiguous (Q9) | TC-086 |
| EP-52 | matches | empty 5th field (`A;B;1;0;`) | ambiguous (Q9) | TC-087 |
| EP-53 | deductions | 2 fields, positive integer | V | TC-089 |
| EP-54 | deductions | 1 or ≥ 3 fields / non-integer | I | TC-084, TC-085 |
| EP-55 | any | blank or whitespace-only line | V (ignored) | TC-083 |
| EP-56 | any | LF vs CRLF line endings | V | TC-083 |
| EP-57 | any | file missing | I | TC-090, TC-098 |
| EP-58 | CLI | 2 args (no deductions) / 3 args | V | TC-095 / TC-096 |
| EP-59 | CLI | 0, 1, ≥ 4 args | I | TC-097 |

---

## 3. Boundary value analysis

| ID | Variable | Boundary values tested | Expected | Why this boundary | TC |
|---|---|---|---|---|---|
| BV-01 | Goal count | -1 / 0 / 1 | reject / accept / accept | 0 is the smallest legal value (FR-02); off-by-one `> 0` vs `>= 0` check is a classic fault | TC-004, TC-005, TC-011 |
| BV-02 | Forfeit winner's actual goals | 2 / 3 / 4 (also 0, 5) | 3 / 3 / 4 (3, 5) | FMT 23/4 = max(3, g): the rule changes at g = 3; `>` vs `>=` and "always 3" faults are caught at 3 and 4 | TC-020, TC-021 |
| BV-03 | Overall points difference | 0 / 1 | tie-break applies / pure points order | 1 point is the smallest difference that must override every tie criterion (FR-08) | TC-014, TC-054 |
| BV-04 | Overall GD difference in a 2-team tie | 0 / 1 | go to GF / decided by GD | smallest decisive difference for FMT 9/2-a III | TC-054, TC-055 |
| BV-05 | Overall GF difference | 0 / 1 | go to forfeit criterion / decided by GF | smallest decisive difference for 9/2-a IV | TC-055, TC-059 |
| BV-06 | h2h GD difference (2 teams) | 0 / 2 | next criterion / decided | Mutual GDs are always opposite numbers, so the difference is always even: **1 is impossible**, 2 (one-goal margin) is the real boundary | TC-052, TC-053 |
| BV-07 | h2h points difference (2 teams) | 0 / 3 | next criterion / decided | Possible h2h point pairs (3-3, 1-1, 2-2, 4-1, 3-0, 6-0) → smallest non-zero difference is 3 | TC-050, TC-051, TC-054 |
| BV-08 | Deduction points | -1 / 0 / 1 | reject / reject / accept | V-07: "zero or negative" invalid; 1 is the smallest valid | TC-008 |
| BV-09 | Points after deduction | 1 / 0 / -1 | ranked by value; -1 below 0 | FR-07 allows negative totals (FDT 33); sign handling / clamping at 0 is a likely fault | TC-030, TC-031 |
| BV-10 | Tied group size | 2 → 3 | 9/2-a → 9/2-b | Algorithm switch: the same pair can be ordered differently by the 2-team rule and the mini-table rule (TC-073 vs TC-074 differ in one match only) | TC-073, TC-074 |
| BV-11 | Tied group size | 1 / 2 / 3 / 4 / 11 | — | 1 = no tie; 4 and 11 check that the 3+ procedure is not hard-coded for exactly 3 | TC-012, TC-076, TC-078 |
| BV-12 | Matches between two teams | 0 / 1 / 2 / 3 | valid / valid / valid / reject | FR-03 maximum is 2; 0 exercises A-03 (no h2h data) | TC-060, TC-051, TC-047, TC-048 |
| BV-13 | CSV field count (matches) | 3 / 4 / 5 / 6 | reject / accept / accept / reject | FR-14 allows exactly 4 or 5 (optional forfeit) | TC-081, TC-084 |
| BV-14 | CSV field count (deductions / teams) | 1 / 2 / 3 ; 1 / 2 | reject / accept / reject ; accept / reject | exact field counts in FR-14 | TC-084, TC-089 |
| BV-15 | Name length after trim | 0 / 1 | reject / accept | blank check must be done after trimming | TC-001, TC-002 |
| BV-16 | CLI argument count | 1 / 2 / 3 / 4 | reject / accept / accept / reject | 2 mandatory files + 1 optional (FR-14) | TC-095..TC-097 |

---

## 4. Decision tables

### 4.1 (a) Two teams level on points — FMT 9/2-a (FR-09, FR-12)

`Y` = condition true, `N` = false, `–` = don't care.

| Condition / Rule | R1 | R2 | R2' | R3 | R4 | R5 | R6 | R7 |
|---|---|---|---|---|---|---|---|---|
| C1 h2h points equal? | N | Y | Y | Y | Y | Y | Y | Y |
| C2 h2h GD equal? | – | N | Y (away goals differ) | Y | Y | Y | Y | Y |
| C3 overall GD equal? | – | – | N | N | Y | Y | Y | Y |
| C4 overall GF equal? | – | – | – | – | N | Y | Y | Y |
| C5 forfeit losses | – | – | – | – | – | only one team | both | neither |
| **Decided by** | I h2h pts | II h2h GD | III (away goals ignored) | III overall GD | IV overall GF | V no-forfeit team higher | unresolved | unresolved |
| TC | TC-050, TC-051 | TC-052 | TC-053 | TC-054 | TC-055 | TC-056 | TC-058 | TC-059 |

Extra rules: forfeit criterion is applied **last** even if one team has a forfeit loss (TC-057); 0 mutual matches → C1, C2 true by A-03 (TC-060); independent tie groups (TC-061); h2h uses the forfeit-adjusted score (TC-024).

### 4.2 (b) Three or more teams level — FMT 9/2-b (FR-10, FR-11, FR-12)

| Condition / Rule | M1 | M2 | M3 | M4 | M5 | M6 | M7 | M8 | M9 (key) |
|---|---|---|---|---|---|---|---|---|---|
| Mini points separate all? | Y | N (all eq.) | N | N | N | N | N | partly (1 team) | N (all eq.) |
| Mini GD separates (remaining)? | – | Y | N | N | N | N | N | Y (other two) | N |
| Mini GF separates? | – | – | Y | N | N | N | N | – | partly (1 team) |
| Overall GD separates? | – | – | – | Y | N | N | N | – | Y (other two) |
| Overall GF separates? | – | – | – | – | Y | N | N | – | – |
| Forfeit criterion separates? | – | – | – | – | – | 1 team | N | – | – |
| Re-applying h2h to the 2 remaining teams would change the result? | – | – | – | – | – | no | – | **yes** (h2h equal → overall GD favours other team) | **yes** (h2h winner ≠ overall-GD winner) |
| **Result** | mini pts | mini GD | mini GF | overall GD | overall GF | 1 placed last, 2 unresolved | all 3 unresolved | mini pts + mini GD, no rebuild | mini GF, then overall GD, no rebuild |
| TC | TC-065 | TC-066 | TC-067 | TC-068 | TC-069 | TC-070 | TC-071 | TC-072 | TC-073 |

Additional rules: incomplete mini-table (A-03) TC-075; 4-team tie TC-076; mini-table uses forfeit-adjusted scores TC-077; group size boundary 2 vs 3 TC-074.

**Key FR-11 case (TC-073) explained.** A, B, C each 6 points. Mini-table: everybody 3 mini points and 0 mini GD; C has mini GF 3, A and B mini GF 2 → C first. A and B are still level. FMT 9/2-b says *do not look at their matches again*: go on with V (overall GD): B +3 > A +1 → **C, B, A**. A wrong "recursive" implementation rebuilds a 2-team table for A and B, sees A beat B 1-0, and gives **C, A, B**. An implementation that skips mini GF gives B, C, A. So this single case separates three plausible implementations.

### 4.3 (c) Forfeit score adjustment — FMT 23/4 (FR-06)

| Rule | Forfeiting side | Winner's actual goals g | Adjusted score (home-away) | Example (recorded → adjusted) | TC |
|---|---|---|---|---|---|
| F1 | HOME | g ≤ 3 | 0 - 3 | 1-2 → 0-3 ; 0-0 → 0-3 | TC-021 |
| F2 | HOME | g > 3 | 0 - g | 2-5 → 0-5 | TC-021 |
| F3 | AWAY | g ≤ 3 | 3 - 0 | 2-1 → 3-0 ; 3-1 → 3-0 | TC-020 |
| F4 | AWAY | g > 3 | g - 0 | 4-1 → 4-0 ; 5-2 → 5-0 (FMT example) | TC-020 |
| F5 | HOME / AWAY | forfeiter was leading or level | per F1–F4 | 5-2 F=HOME → 0-3 ; 2-2 F=AWAY → 3-0 | TC-022 |
| F6 | NONE | – | unchanged | 0-0 → 0-0 | TC-011 |

In all F1–F5: forfeiter gets a loss, 0 points and `hasForfeitLoss() = true`; adjusted score is used in stats, h2h (TC-024) and mini-table (TC-077). No automatic 3-point deduction (assumption, TC-023, ambiguity Q1).

### 4.4 (d) Deduction interaction (FR-07, A-02)

| Rule | Match points before deduction | Deduction effect | Expected | TC |
|---|---|---|---|---|
| D1 | A = B, A wins h2h | deduction on A breaks the tie | B above A, no tie-break used | TC-032 |
| D2 | A = B + 3, B wins h2h, A better GD | (A,3) creates a tie | tie-break applies; h2h → B above A | TC-033 |
| D3 | A = B + 6, A wins h2h 4-1 pts | (A,6) creates a tie | h2h computed from results (A-02): A above B. If the deduction leaked into h2h, A would have −2 h2h points → B above A | TC-034 |
| D4 | – | deductions accumulate; total may be 0 or negative | 0 / −1 handled as numbers; equal 0 → tie-break | TC-030, TC-031 |
| D5 | – | team unregistered | V-07 exception | TC-035 |

---

## 5. Negative tests

| Rule | Negative case(s) | TC |
|---|---|---|
| V-01 | `calculate(null, …)`, `calculate(…, null, …)`, team list with null, match list with null, deduction list with null; `MatchResult` with null home/away/forfeit; `PointDeduction(null, 3)`; null deductions list (ambiguous) | TC-007, TC-009, TC-040, TC-041 |
| V-02 | name `null`, `""`, `" "`, `"\t"`; duplicate `[A, A]`; duplicate after trim | TC-002, TC-042 |
| V-03 | unregistered home team; unregistered away team (message names the team) | TC-044, TC-098 |
| V-04 | `A–A`; `A–" A "` (same after trim) | TC-006 |
| V-05 | home −1; away −1; `-1` in CSV | TC-005, TC-088 |
| V-06 | same fixture twice (different score, with forfeit); three matches between two teams | TC-046, TC-048 |
| V-07 | deduction 0, −1; deduction for unregistered team; `Beşiktaş;0` in CSV | TC-008, TC-035, TC-089 |
| V-08 | wrong field counts (teams 2, matches 3/6, deductions 1/3); non-integer numbers; unknown forfeit; missing file — all with file name (+ line number) | TC-084, TC-085, TC-086, TC-090 |
| CLI | 0 / 1 / 4 arguments; missing file; malformed line; invalid data | TC-097, TC-098 |

---

## 6. Random, property-based and metamorphic tests

Class `generator/PropertyBasedStandingsTest`, driven by `generator.SeasonGenerator` with **fixed seeds** (e.g. seeds 1..100 via `@MethodSource`; failing seed printed in the `@DisplayName`), so every run is reproducible and any failure becomes a hand-checked regression case.

**Why it complements the hand-made cases.** Hand cases are small (3–5 teams) and only check what the designer thought of. Generated full seasons (18 teams, double round-robin, random forfeits and deductions) exercise combinations nobody writes by hand (several tie groups at once, ties at negative points, forfeits inside mini-tables). They do not need an exact expected table: the oracle is a set of **invariants** that must hold for every correct table, and **metamorphic relations** (change the input in a way whose effect on the output is known). This addresses the oracle problem for large inputs.

| TC | Property / relation | Basis |
|---|---|---|
| TC-100 | Conservation: Σ won = Σ lost; Σ drawn is even; Σ GF = Σ GA | every match has one W + one L or two D; goals counted once each side |
| TC-101 | Per row: played = W + D + L; GD = GF − GA; points = 3W + D − deductedPoints | FR-04, FR-05, FR-07, A-01 |
| TC-102 | Positions are exactly 1..n in list order; points non-increasing down the table | FR-08, FR-13 |
| TC-103 | Every unresolved row has an adjacent unresolved row with equal points; each unresolved block is in Turkish alphabetical order; a team with a unique points total is never unresolved | FR-12, D-02 |
| TC-104 | MR shuffle: random permutation of team list and match list → identical sequence of (team, position, unresolved) | ranking must not depend on input order |
| TC-105 | MR consistent renaming (bijection, e.g. reversed alphabetical mapping) → same positions for resolved teams; unresolved blocks contain the renamed same teams, re-sorted by the new names | names only matter for D-02 display |
| TC-106 | MR mirroring: swap home/away and goals of **every** match, HOME↔AWAY forfeit → identical table | FR-09 II: away goals have no extra weight; no criterion depends on venue |
| TC-107 | MR deduction: add `(T,d)` → T's points −d; all P/W/D/L/GF/GA unchanged; every team that was above T is still above T. (The relative order of *other* teams is deliberately **not** asserted: a new 3-team tie created by the deduction can legally reorder them — FMT 9/2-b is not monotonic.) | FR-07, A-02 |
| TC-108 | Scenario TWO_TEAM_TIE: the two designated teams have equal points, adjacent positions; their order agrees with a test-side oracle that evaluates 9/2-a I–V from the raw match list | FR-09 |
| TC-109 | Scenario THREE_TEAM_TIE: ≥ 3 designated teams with equal points on consecutive positions; order agrees with a test-side mini-table oracle (9/2-b II–VII, no rebuild) | FR-10, FR-11 |
| TC-110 | Scenario FULLY_UNRESOLVED: all designated teams flagged unresolved, consecutive positions, Turkish alphabetical order | FR-12, D-02 |
| TC-111 | Determinism and validity: same seed → identical season and table; every generated season passes validation (no V-xx exception, ≤ 2 matches per pair, one per direction) | generator contract, FR-03 |

---

## 7. Concrete test cases

Columns: **Tech** = EP (equivalence partition), BVA, DT (decision table rule), NEG (negative), MR/PBT (metamorphic / property). Actual = filled after execution.

### 7.1 Model — `model/TeamTest`, `model/MatchResultTest`, `model/PointDeductionTest`

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-001 | EP, BVA | FR-01, EP-01..03 | `Team("X")`, `Team("Sivasspor")`, `Team("Beşiktaş")`, `Team("Çaykur Rizespor")`, `Team("İstanbul Başakşehir")` | created; `name()` equals input exactly | valid partitions incl. Turkish letters, internal space, 1-char boundary | — |
| TC-002 | NEG, BVA | V-01, V-02 | `Team(null)`, `Team("")`, `Team(" ")`, `Team("\t")` | exception with message | blank check must happen after trim (length 0 after trim) | — |
| TC-003 | EP | D-01 | `Team(" Beşiktaş ")` vs `Team("Beşiktaş")`; `Team("beşiktaş")` vs `Team("Beşiktaş")` | first pair denotes the same team (equal, name `Beşiktaş`); second pair different | D-01 "exact match after trimming": trimmed, case-sensitive (assumption on *where* trimming happens, Q6) | — |
| TC-004 | EP, BVA | FR-02, FR-06 | `MatchResult(A,B,0,0)`; `MatchResult(A,B,0,0,HOME)`; `…AWAY` | created; 4-arg constructor gives `forfeit() == NONE` | 0 is valid lower bound; default forfeit | — |
| TC-005 | NEG, BVA | V-05 | `(A,B,-1,0)`, `(A,B,0,-1)` | exception | −1 is the first invalid value on both sides | — |
| TC-006 | NEG | V-04, D-01 | `MatchResult(A,A,1,0)`; `MatchResult(Team("A"),Team(" A "),1,0)` | exception | same team, also when only whitespace differs | — |
| TC-007 | NEG | V-01 | null home; null away; forfeit `null` | null home / null away → exception; forfeit `null` → accepted as NONE (Q3) | null checks per field | — |
| TC-008 | BVA, NEG | V-07 | `PointDeduction(A,1)`, `(A,0)`, `(A,-1)` | 1 accepted; 0 and −1 rejected | boundary of "zero or negative" | — |
| TC-009 | NEG | V-01 | `PointDeduction(null,3)` | exception | null team | — |

### 7.2 Stats and points — `StandingsCalculatorTest` (`@Nested Stats`)

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-010 | EP | FR-04, FR-05, FR-13 | (a) `A–B 2-1`; (b) `A–B 0-3` | (a) 1 A P1 W1 D0 L0 GF2 GA1 GD+1 3 pts; 2 B P1 L1 GF1 GA2 GD−1 0. (b) 1 B W1 GF3 GA0 3; 2 A L1 GF0 GA3 GD−3 0 | win/loss, home vs away not confused | — |
| TC-011 | EP, BVA | FR-04, FR-12 | (a) `A–B 1-1`; (b) `A–B 0-0`; teams `[B, A]` | both P1 D1 1 pt; GF=GA=1 (a) / 0 (b); h2h, GD, GF, forfeit all equal → 1 A U, 2 B U | draw = 1 pt each; 0-0 lower bound; a single draw already produces an unresolved pair | — |
| TC-012 | EP | FR-04, FR-05, FR-08 | `A–B 2-0, B–A 1-1, A–C 0-1, C–A 2-2, B–C 3-1, C–B 0-1` | 1 B P4 W2 D1 L1 GF5 GA4 GD+1 **7**; 2 A P4 W1 D2 L1 GF5 GA4 GD+1 **5**; 3 C P4 W1 D1 L2 GF4 GA6 GD−2 **4** | full double round-robin, no tie; order not alphabetical. Check: ΣGF 14 = ΣGA 14 | — |
| TC-013 | EP | FR-05, A-03, A-04 | teams `[A,B,C]`; `A–B 1-0` | 1 A 3; 2 **C** P0 all zeros 0 pts GD 0; 3 B 0 pts GD −1 | team without matches is listed; B/C never met → h2h equal → GD 0 > −1 | — |
| TC-014 | BVA | FR-08 | `A–X 1-0, A–Y 0-0, B–X 5-0, Y–B 3-0` | 1 Y 4 (GD+3); 2 A 4 (GD+1); 3 B 3 (GD+2); 4 X 0. (A/Y: h2h 0-0 equal → overall GD 3>1) | 1-point difference beats better GD (B GD +2 > A +1) | — |
| TC-015 | EP | FR-12, A-03, D-02 | teams `[B, A]`, no matches | 1 A 0 U; 2 B 0 U | start of season: all criteria equal (ambiguity Q12) | — |
| TC-016 | EP | FR-01 | teams `[]`, matches `[]`, deductions `[]` | empty list, no exception | assumption (Q4) | — |

### 7.3 Forfeit — `StandingsCalculatorTest` (`@Nested Forfeit`)

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-020 | BVA, DT F3/F4 | FR-06 | `A–B g-x F=AWAY` for (2-1), (3-1), (4-1), (5-2) | A GF = 3, 3, 4, 5; GA 0; W, 3 pts. B GF 0, GA = 3,3,4,5; L, 0 pts, `hasForfeitLoss` true | max(3,g) boundary at 3/4; FMT example 5-2 → 5-0 | — |
| TC-021 | BVA, DT F1/F2 | FR-06 | `A–B` F=HOME: (1-2), (2-5), (0-0) | adjusted 0-3, 0-5, 0-3; B W 3 pts; A L 0 pts, forfeit loss; A GF 0 | mirror side; 0-0 recorded | — |
| TC-022 | DT F5 | FR-06 | (a) `A–B 5-2 F=HOME`; (b) `A–B 2-2 F=AWAY` | (a) 0-3: B W, A L (A's 5 goals void). (b) 3-0: A W | forfeiter was leading / level on the pitch | — |
| TC-023 | EP | FR-06, FR-07 | `A–B 1-0 F=AWAY`, no deductions | B points 0, `getDeductedPoints()` 0 | no automatic FMT 24/1-a deduction (assumption, Q1) | — |
| TC-024 | DT | FR-06, FR-09 | `A–B 2-1 F=HOME, A–X 5-0, X–A 1-0, B–X 0-4, X–B 4-0` | A: 0-3 L, W, L → 3 pts GF5 GA4 GD+1, forfeit loss. B: W 3-0, L, L → 3 pts GF3 GA8 GD−5. X: 3+6 = 9. Table: 1 X 9; 2 **B** 3 (h2h 3-0 pts); 3 A 3 | adjusted score must be used in h2h. Raw h2h (2-1) → A; overall GD → A; only correct impl gives B | — |

### 7.4 Deductions — `StandingsCalculatorTest` (`@Nested Deductions`)

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-030 | EP, BVA | FR-07 | `A–B 1-0`; (a) `(A,1)`; (b) `(A,1),(A,1)` | (a) A 2 pts, deducted 1; (b) A 1 pt, deducted 2; A still 1st, B 0 | smallest deduction; accumulation | — |
| TC-031 | BVA, DT D4 | FR-07, FR-12 | teams `[A,B,C]`, `A–B 0-0`; (a) `(A,1)`; (b) `(A,2)` | (a) 1 B 1; A 0 and C 0: never met, GD 0=0, GF 0=0 → 2 A 0 U, 3 C 0 U. (b) 1 B 1; 2 C 0; 3 A **−1** | resulting points 0 and −1; negative total allowed and ranked below 0 (FDT 33) | — |
| TC-032 | DT D1 | FR-07, FR-08 | `A–B 1-0, B–X 1-0`; `(A,1)` | 1 B 3; 2 A 2 (deducted 1); 3 X 0 | without deduction A would win via h2h | — |
| TC-033 | DT D2 | FR-07, FR-09 | `B–A 1-0, A–X 2-0, X–A 0-2, X–B 1-0, B–X 0-1`; `(A,3)` | A 6−3 = 3 (GD+3); B 3 (GD−1); X 6. Table: 1 X 6; 2 **B** 3 (h2h won); 3 A 3 | deduction creates tie → tie-break; GD alone would pick A | — |
| TC-034 | DT D3 | A-02, FR-09 | `A–B 1-0, B–A 0-0, A–X 2-0, X–A 1-0, X–B 1-0, B–X 0-1`; `(A,6)` | A 7−6 = 1, B 1, X 9. h2h from results: A 4 pts, B 1 → 1 X 9; 2 **A** 1; 3 B 1 | if deduction leaked into h2h: A 4−6 = −2 < 1 → B above A | — |
| TC-035 | NEG | V-07 | teams `[A,B]`, deduction `(Team("Z"),3)` | exception naming Z | deduction for unregistered team | — |

### 7.5 Validation — `StandingsCalculatorTest` (`@Nested Validation`)

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-040 | NEG | V-01 | rows: teams `null`; matches `null`; teams `[A,null]`; matches `[A–B 1-0, null]`; deductions `[null]` | exception each | null lists / null elements | — |
| TC-041 | NEG | V-01 | deductions list `null` | exception (planned; ambiguity Q2) | V-01 does not list a null deduction *list* explicitly | — |
| TC-042 | NEG | V-02, D-01 | `[A, A]`; `[Beşiktaş, " Beşiktaş "]` | exception (duplicate) | exact and after-trim duplicates | — |
| TC-043 | EP | FR-01, D-01 | teams `[Beşiktaş, beşiktaş]`, `Beşiktaş–beşiktaş 1-0` | accepted; Beşiktaş 3 pts, beşiktaş 0 | case-sensitive names are distinct | — |
| TC-044 | NEG | V-03 | teams `[A,B]`; (a) `Z–A 1-0`; (b) `A–Z 1-0` | exception; message contains `Z` (assumption Q5) | home and away side both checked | — |
| TC-045 | EP | D-01, V-03 | teams `[Beşiktaş, Göztepe]`, `Team("Beşiktaş ")–Team(" Göztepe") 2-0` | accepted; Beşiktaş 3 pts | trimmed names must resolve to registered teams | — |
| TC-046 | NEG | V-06 | (a) `A–B 1-0, A–B 2-2`; (b) `A–B 1-0, A–B 0-0 F=HOME` | exception | same direction twice, regardless of score/forfeit | — |
| TC-047 | BVA | FR-03 | `A–B 1-0, B–A 0-0` | accepted; A 4 pts, B 1 | 2 matches, opposite directions = maximum valid | — |
| TC-048 | NEG, BVA | V-06 | `A–B 1-0, B–A 0-0, A–B 2-0` | exception | 3 matches = first invalid count | — |

### 7.6 Two-team ties — `TieBreakTwoTeamTest` (FMT 9/2-a)

| ID | Tech | Req | Input | Expected (with arithmetic) | Rationale (fault exposed) | Actual |
|---|---|---|---|---|---|---|
| TC-050 | DT R1, BVA | FR-09 I | `A–B 2-1, B–A 1-1, X–A 1-0, B–X 3-0` | A 4 pts (GD 0), B 4 (GD+2), X 3. h2h: A 3+1 = 4, B 0+1 = 1 → 1 A, 2 B, 3 X | h2h over two legs beats better overall GD | — |
| TC-051 | DT R1, BVA | FR-09 I | `B–A 1-0, A–X 4-0` | B 3 (GD+1), A 3 (GD+3), X 0 → 1 **B**, 2 A, 3 X | single mutual match; GD would pick A | — |
| TC-052 | DT R2 | FR-09 II | `A–B 3-0, B–A 1-0, A–X 1-0, B–X 6-0` | both 6 pts; h2h pts 3-3; h2h GD A (3+0)−(0+1) = +2, B −2 → 1 A, 2 B, 3 X. Overall GD A +3 < B +4 | h2h GD before overall GD | — |
| TC-053 | DT R2' | FR-09 II | `A–B 2-1, B–A 1-0, A–X 2-0, B–X 1-0` | h2h pts 3-3; h2h GD A 2−2 = 0, B 0 (B scored 1 away goal, A 0). Overall GD A +2 > B +1 → 1 A, 2 B, 3 X | away goals must NOT decide (they would pick B) | — |
| TC-054 | DT R3, BVA | FR-09 III | `A–B 1-1, A–X 4-3, B–X 2-0` | 4 pts each; h2h equal; GD A 5−4 = +1, B 3−1 = +2 → 1 **B**, 2 A, 3 X. (GF would pick A 5 > 3) | GD before GF; GD difference 1 | — |
| TC-055 | DT R4, BVA | FR-09 IV | `A–B 1-1, A–X 1-0, B–X 2-1` | 4 pts each; GD +1 each; GF A 2, B 3 → 1 **B**, 2 A, 3 X | GF difference 1 | — |
| TC-056 | DT R5 | FR-09 V, FR-06 | `A–B 0-0, X–A 0-0 F=AWAY, Y–B 3-0, X–Y 1-0` | X 6, Y 3; A and B 1 pt, GF0 GA3 GD−3; A has forfeit loss → 1 X, 2 Y, 3 **B**, 4 A | forfeit criterion; recorded 0-0 must become 3-0 | — |
| TC-057 | DT | FR-09 order | `A–B 0-0, X–A 0-0 F=AWAY, A–Y 5-0, B–Y 1-0` | A 4 pts GF5 GA3 GD+2 (forfeit loss); B 4 GD+1; X 3; Y 0 → 1 A, 2 B, 3 X, 4 Y | forfeit criterion is last; must not be applied before GD | — |
| TC-058 | DT R6 | FR-09 V, FR-12, D-02 | teams `[B, A, X]`; `A–B 0-0, X–A 0-0 F=AWAY, X–B 0-0 F=AWAY` | X 6 (GF6); A, B 1 pt, GD −3, GF 0, both forfeit loss → 1 X, 2 A U, 3 B U | both forfeited → still level → unresolved, alphabetical display | — |
| TC-059 | DT R7 | FR-12, D-02 | teams `[B, X, A]`; `A–B 1-1, A–X 2-0, B–X 2-0` | A, B 4 pts, GF3 GA1 GD+2 → 1 A U, 2 B U, 3 X 0 (not U) | all criteria equal, no forfeit; display order ≠ input order | — |
| TC-060 | EP, BVA | FR-09, A-03 | `A–X 1-0, X–B 0-2` | A, B never met → h2h equal; GD B +2 > A +1 → 1 **B**, 2 A, 3 X | 0 mutual matches must not crash / must skip to III | — |
| TC-061 | EP | FR-08, FR-09 | `A–C 2-0, B–D 1-0, A–D 1-0, B–C 3-0, A–B 0-0, C–D 1-1` | A 7 (GD+3), B 7 (GD+4); C 1 (GF1 GA6 GD−5), D 1 (GF1 GA3 GD−2). Each pair h2h drawn → 1 B, 2 A, 3 D, 4 C | two independent tie groups in one table | — |

### 7.7 Three-or-more-team ties — `TieBreakMultiTeamTest` (FMT 9/2-b)

| ID | Tech | Req | Input | Expected (with arithmetic) | Rationale (fault exposed) | Actual |
|---|---|---|---|---|---|---|
| TC-065 | DT M1 | FR-10 II | `C–B 1-0, C–A 1-0, B–A 1-0, C–X 0-1, X–C 1-0, B–X 1-0, X–B 1-0, A–X 3-0, X–A 0-3` | A, B, C 6 pts; X 9 (W3 L3, GF3 GA7). Mini pts C 6, B 3, A 0. A has best overall GD (+4 vs 0, 0) → 1 X, 2 C, 3 B, 4 A | mini-table ignores matches vs X; overall GD would invert | — |
| TC-066 | DT M2 | FR-10 III | `A–B 3-0, B–C 1-0, C–A 1-0, A–X 1-0, B–X 6-0, C–X 2-0` | all 6 pts; mini pts 3 each; mini GD A +3−1 = +2, C −1+1 = 0, B −3+1 = −2. Overall GD A +3, B +4, C +2 → 1 A, 2 C, 3 B, 4 X | mini GD before overall GD | — |
| TC-067 | DT M3 | FR-10 IV | `A–B 1-0, B–C 3-2, C–A 2-1, A–X 5-0, B–X 2-0, C–X 1-0` | all 6; mini pts 3, mini GD 0 each; mini GF A 1+1 = 2, B 0+3 = 3, C 2+2 = 4. Overall GD A +5, B +2, C +1 → 1 C, 2 B, 3 A, 4 X | mini GF before overall criteria (exact inverse of GD order) | — |
| TC-068 | DT M4 | FR-10 V | `A–B 1-0, B–C 1-0, C–A 1-0, A–X 1-0, B–X 3-0, C–X 2-0` | mini all equal (3 pts, GD 0, GF 1); overall GD A +1, B +3, C +2 → 1 B, 2 C, 3 A, 4 X | falls through to overall GD | — |
| TC-069 | DT M5 | FR-10 VI | `A–B 1-0, B–C 1-0, C–A 1-0, A–X 2-1, B–X 1-0, C–X 3-2` | mini equal; overall GD +1 each; GF A 3, B 2, C 4 → 1 C, 2 A, 3 B, 4 X | overall GF | — |
| TC-070 | DT M6 | FR-10 VII, FR-11, FR-12, D-02 | teams `[C, A, X, B]`; `A–B 0-0, B–C 0-0, C–A 0-0, X–A 0-0 F=AWAY, X–B 3-0, X–C 3-0` | X 9; A, B, C 2 pts, GF0 GA3 GD−3; mini all 0-0; only A has forfeit loss → 1 X, 2 B U, 3 C U, 4 A (not U) | partial resolution; unresolved block in the middle of the table; A must not be flagged | — |
| TC-071 | DT M7 | FR-12, D-02 | teams `[C, B, A]`; `A–B 1-0, B–C 1-0, C–A 1-0` | all 3 pts, GD 0, GF 1 → 1 A U, 2 B U, 3 C U | perfect cycle → all unresolved | — |
| TC-072 | DT M8 | FR-10, **FR-11** | `A–B 0-0, C–A 1-0, C–B 3-0, A–X 1-0, X–A 0-1, B–X 5-0, X–B 0-3, C–X 0-0, X–C 1-0` | A 7 (GF2 GA1 GD+1), B 7 (GF8 GA3 GD+5), C 7 (GF4 GA1 GD+3), X 4. Mini pts C 6, A 1, B 1; A/B mini GD −1 vs −3 → 1 C, 2 A, 3 B, 4 X | re-applying 2-team rule to A/B (h2h 0-0 → overall GD) would give B above A | — |
| TC-073 | DT M9 (key) | **FR-11**, FR-10 | `A–B 1-0, C–A 2-1, B–C 2-1, A–D 1-0, B–D 3-0, C–D 1-0` | A 6 (GF3 GA2 GD+1), B 6 (GF5 GA2 GD+3), C 6 (GF4 GA3 GD+1), D 0. Mini: pts 3/3/3, GD 0/0/0, GF A 2, B 2, C 3 → C first; A/B still level → overall GD B +3 > A +1 → 1 C, 2 B, 3 A, 4 D | see §4.2: recursive h2h → C, A, B; skipping mini GF → B, C, A | — |
| TC-074 | BVA (size 2↔3) | FR-09 vs FR-10 | same as TC-073 but `C–D 1-1` | C 4 (GD 0), D 1; only A, B level at 6 → 2-team rule: h2h A won → 1 **A**, 2 B, 3 C, 4 D | one changed result switches algorithm and flips A/B | — |
| TC-075 | EP | FR-10, A-03 | `A–B 2-0, B–C 1-0, C–X 1-0` | A, B, C 3 pts; mini (only A–B, B–C): A 3 pts GD+2, B 3 GD−1, C 0 → 1 A, 2 B, 3 C, 4 X. (overall GD would give A, C, B) | incomplete mini-table must work | — |
| TC-076 | EP, BVA | FR-10 | `A–B 0-0, C–D 1-1, A–C 3-0, C–B 2-0, B–D 1-0, D–A 1-0` | all 4 pts (W1 D1 L1); mini = overall: GD A +2, D 0, C −1 (GF3), B −1 (GF1) → 1 A, 2 D, 3 C, 4 B | 4-team tie; procedure not hard-coded for 3 | — |
| TC-077 | DT | FR-06, FR-10 | `A–B 1-0, B–C 0-0 F=AWAY, C–A 1-0` | B–C becomes 3-0. All 3 pts; mini GD B −1+3 = +2, A 0, C −2 → 1 B, 2 A, 3 C (C forfeit loss) | mini-table must use adjusted score (raw 0-0 → C 4, A 3, B 1) | — |
| TC-078 | EP | D-02, FR-12 | 11 teams, no matches, input order reversed: Vanspor, Ümraniyespor, Trabzonspor, Şanlıurfaspor, Kasımpaşa, İstanbul Başakşehir, Iğdır FK, Denizlispor, Çaykur Rizespor, Beşiktaş, Ankaragücü | all 0 pts, all U, positions 1..11 in Turkish order: Ankaragücü, Beşiktaş, Çaykur Rizespor, Denizlispor, Iğdır FK, İstanbul Başakşehir, Kasımpaşa, Şanlıurfaspor, Trabzonspor, Ümraniyespor, Vanspor | Turkish alphabet A B C Ç D … I İ … S Ş T U Ü V. Unicode code-point order would put Ç, İ, Ş, Ü after Z | — |
| TC-079 | EP | D-02 | teams `[Denizlispor, Çaykur Rizespor]`; `Denizlispor–Çaykur Rizespor 1-1` | 1 Çaykur Rizespor U, 2 Denizlispor U | minimal 2-team version of TC-078 (Ç < D) | — |

### 7.8 CSV input — `io/CsvLoaderTest` (files created in `@TempDir`, written as UTF-8)

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-080 | EP | FR-14 | teams file: `Çaykur Rizespor` / `İstanbul Başakşehir` / `Şanlıurfaspor` | 3 teams, names byte-exact, file order | UTF-8 decoding (must not depend on platform charset) | — |
| TC-081 | EP, BVA | FR-14, FR-06 | matches: `A;B;2;1` ; `A;C;1;0;NONE` ; `B;C;0;0;HOME` ; `C;A;5;2;AWAY` | 4 results; forfeit NONE, NONE, HOME, AWAY | 4 and 5 fields valid; default NONE | — |
| TC-082 | EP | D-01, FR-14 | ` Beşiktaş ;Göztepe; 2 ;1` | Beşiktaş, Göztepe, 2, 1 | trimming of names (D-01) and numbers (assumption Q9) | — |
| TC-083 | EP | FR-14 | file with empty line, whitespace-only line, CRLF endings | same result as clean LF file | blank lines ignored; `\r` must not stick to last field (`"1\r"` not a number, `"A\r"` ≠ `A`) | — |
| TC-084 | NEG, BVA | V-08 | teams `A;B`; matches `A;B;1` and `A;B;1;0;NONE;X`; deductions `A` and `A;3;X` — bad line is physical line 3 after a valid line and a blank line | `InputFormatException`; message contains file name and `3` | field-count boundaries; 1-based physical line number (assumption) | — |
| TC-085 | NEG | V-08 | goals `x`, `1.5`, empty (`A;B;;1`); deduction points `abc` | `InputFormatException` with file + line | non-integer numbers | — |
| TC-086 | NEG | V-08 | forfeit `XYZ`; forfeit `home` | `XYZ` rejected; `home` rejected (planned, ambiguity Q9) | unknown enum value | — |
| TC-087 | EP | FR-14 | `A;B;1;0;` | forfeit NONE (planned, ambiguity Q9) | empty optional field | — |
| TC-088 | NEG | V-05, V-08 | `A;B;-1;0` | rejected with exception (type/line number: ambiguity Q10) | negative goals arriving through CSV | — |
| TC-089 | EP, NEG | FR-14, V-07 | deductions `Beşiktaş;3` and `Beşiktaş;0` | first → `PointDeduction(Beşiktaş,3)`; second rejected | valid line; zero points | — |
| TC-090 | NEG | V-08 | non-existent teams / matches / deductions path | `InputFormatException`, message contains file name | missing file | — |

### 7.9 CLI — `cli/TableFormatterTest`, `cli/MainTest`

| ID | Tech | Req | Input | Expected | Rationale | Actual |
|---|---|---|---|---|---|---|
| TC-091 | EP | FR-13, FR-15 | rows of TC-012 | header + 3 rows in order B, A, C with pos, P, W, D, L, GF, GA, GD, Pts | all FR-13 fields present | — |
| TC-092 | EP | FR-15 | rows with names `A` and `İstanbul Başakşehir` | each column starts at the same character index on every line | alignment with long / Turkish names (char width, not byte width) | — |
| TC-093 | EP | FR-15, FR-12 | rows of TC-070 | rows B and C carry the unresolved marker; X and A do not | marker only on flagged rows | — |
| TC-094 | BVA | FR-15, FR-07 | rows of TC-031(b) (A −1 pts, GD 0) and TC-010(b) (GD −3) | `-1`, `-3` printed with sign, columns still aligned | negative values widen columns | — |
| TC-095 | EP | FR-14, FR-15 | `Main teams.csv matches.csv` with TC-012 data | stdout table B, A, C; successful exit; nothing on stderr | deductions file optional | — |
| TC-096 | EP | FR-14, FR-07 | same + `deductions.csv` = `B;3` | B 7−3 = 4, C 4: h2h B–C 3-1 and C–B 0-1 → B 6 h2h pts → 1 A 5, 2 B 4, 3 C 4 | third argument used | — |
| TC-097 | NEG, BVA | FR-14 | 0, 1, 4 arguments | usage message on stderr; non-zero exit; no table | wrong argument count (assumption Q11) | — |
| TC-098 | NEG | V-08, V-03 | (a) missing file; (b) matches line `A;B;x;0` on line 2; (c) matches reference unregistered `Z` | one-line error on stderr naming file (+ line 2 for b, `Z` for c); non-zero exit; no table; no stack trace (assumption Q11) | errors reach the user cleanly | — |
| TC-099 | EP | FR-14, FR-15 | teams `İstanbul Başakşehir`, `Şanlıurfaspor`, match `İstanbul Başakşehir;Şanlıurfaspor;1;1` | output (captured as UTF-8) contains both names intact, both marked unresolved, İstanbul Başakşehir first | end-to-end Turkish characters + D-02 | — |

### 7.10 Property-based / metamorphic — `generator/PropertyBasedStandingsTest`

TC-100..TC-111 as specified in §6 (technique PBT/MR; seeds 1..100; each generated scenario type at least 10 seeds).

---

## 8. Mapping to JUnit test classes (for parallel implementation)

All under `src/test/java/edu/svv/standings/`. Each test method gets `@DisplayName("TC-xxx – short text")`.

| Test class | TCs | Count | Notes |
|---|---|---|---|
| `model/TeamTest` | TC-001..TC-003 | 3 | `@ParameterizedTest @ValueSource/@NullSource` |
| `model/MatchResultTest` | TC-004..TC-007 | 4 | `@CsvSource` for goals |
| `model/PointDeductionTest` | TC-008..TC-009 | 2 | `@CsvSource` points,valid |
| `StandingsCalculatorTest` | TC-010..TC-016 (`@Nested Stats`), TC-020..TC-024 (`Forfeit`), TC-030..TC-035 (`Deductions`), TC-040..TC-048 (`Validation`) | 27 | small helper `row(table, "A")` to find a team's row |
| `TieBreakTwoTeamTest` | TC-050..TC-061 | 12 | assert full order + `isUnresolved` per row |
| `TieBreakMultiTeamTest` | TC-065..TC-079 | 15 | same helper; TC-073/074 side by side |
| `io/CsvLoaderTest` | TC-080..TC-090 | 11 | `@TempDir`, write files with `StandardCharsets.UTF_8` |
| `cli/TableFormatterTest` | TC-091..TC-094 | 4 | |
| `cli/MainTest` | TC-095..TC-099 | 5 | capture `System.out`/`System.err` (UTF-8 PrintStream); exit status via the entry point Main offers (assumption Q11) |
| `generator/PropertyBasedStandingsTest` | TC-100..TC-111 | 12 | `@MethodSource` seeds; test-side oracle helpers for TC-108/109 |
| **Total** | | **95** | |

Shared fixture suggestion: a tiny test helper `Leagues` (test scope) with `team(String)`, `m("A","B",2,1)`, `m("A","B",2,1,Forfeit.HOME)` to keep fixtures readable and identical to this plan.

### Count per technique (primary technique of each TC)

| Technique | TCs | Count |
|---|---|---|
| Equivalence partitioning | 001, 003, 004, 010, 011, 012, 013, 015, 016, 023, 043, 045, 060, 061, 075, 076, 078, 079, 080, 081, 082, 083, 087, 089, 091, 092, 093, 095, 096, 099 | 30 |
| Boundary value analysis | 008, 014, 020, 021, 030, 031, 047, 074, 094 | 9 |
| Decision table | 022, 024, 032, 033, 034, 050..059 (10), 065..073 (9), 077 | 25 |
| Negative | 002, 005, 006, 007, 009, 035, 040, 041, 042, 044, 046, 048, 084, 085, 086, 088, 090, 097, 098 | 19 |
| Property-based / metamorphic | 100..111 | 12 |
| **Total** | | **95** |

(Many TCs carry a secondary technique, shown in the Tech column; e.g. TC-050/051/054/055 are also BVA rows, TC-005/048/084 are also BVA.)

---

## 9. Traceability matrix

| Requirement | Test cases |
|---|---|
| FR-01 | TC-001, TC-002, TC-016, TC-042, TC-043 |
| FR-02 | TC-004, TC-005, TC-044 |
| FR-03 | TC-046, TC-047, TC-048, TC-111 |
| FR-04 | TC-010, TC-011, TC-012, TC-101 |
| FR-05 | TC-010, TC-012, TC-013, TC-100, TC-101 |
| FR-06 | TC-020, TC-021, TC-022, TC-023, TC-024, TC-056, TC-058, TC-070, TC-077, TC-106 |
| FR-07 | TC-030, TC-031, TC-032, TC-033, TC-034, TC-096, TC-107 |
| FR-08 | TC-012, TC-014, TC-061, TC-102 |
| FR-09 | TC-024, TC-050..TC-061, TC-074, TC-106, TC-108 |
| FR-10 | TC-065..TC-077, TC-109 |
| FR-11 | **TC-072, TC-073**, TC-070, TC-109 |
| FR-12 | TC-011, TC-015, TC-031, TC-058, TC-059, TC-070, TC-071, TC-078, TC-103, TC-110 |
| FR-13 | TC-010, TC-012, TC-091, TC-102 |
| FR-14 | TC-080..TC-083, TC-087, TC-089, TC-095, TC-096, TC-099 |
| FR-15 | TC-091..TC-095, TC-099 |
| V-01 | TC-002, TC-007, TC-009, TC-040, TC-041 |
| V-02 | TC-002, TC-042 |
| V-03 | TC-044, TC-045, TC-098 |
| V-04 | TC-006 |
| V-05 | TC-005, TC-088 |
| V-06 | TC-046, TC-048 |
| V-07 | TC-008, TC-035, TC-089 |
| V-08 | TC-084, TC-085, TC-086, TC-088, TC-090, TC-098 |
| A-01 | all GD assertions (TC-010, TC-012, TC-101) |
| A-02 | TC-034, TC-107 |
| A-03 | TC-013, TC-015, TC-060, TC-075 |
| A-04 | TC-013 |
| D-01 | TC-003, TC-006, TC-042, TC-043, TC-045, TC-082 |
| D-02 | TC-011, TC-015, TC-058, TC-059, TC-070, TC-071, TC-078, TC-079, TC-099, TC-103, TC-105, TC-110 |
| D-03 | TC-095..TC-099 |

Decision-table coverage: 4.1 R1–R7 + R2' → TC-050..TC-059; 4.2 M1–M9 → TC-065..TC-073; 4.3 F1–F6 → TC-011, TC-020..TC-022; 4.4 D1–D5 → TC-030..TC-035. Every rule has ≥ 1 case.

---

## 10. Known risk areas

| # | Area | Why defects are likely here | Covered by |
|---|---|---|---|
| 1 | **FR-11 "no re-look at sub-ties"** | The intuitive (and UEFA-style) implementation recurses: when a sub-group stays level it rebuilds a smaller mini-table. FMT explicitly forbids this. Also easy: applying overall criteria too early. | TC-072, TC-073, TC-074 |
| 2 | **Turkish alphabetical order (D-02)** | Default `String.compareTo` sorts by UTF-16 code point → Ç, İ, Ş, Ü land after Z. Needs a Turkish `Collator`. The suspicion is also noted in CLAUDE.md. | TC-078, TC-079, TC-099, TC-103 |
| 3 | Forfeit adjustment consistency (FR-06) | Adjustment may be applied in stats but not in h2h/mini, or the max(3,g) boundary written as `>`/`>=` wrongly, or the winning side confused (HOME means *home forfeited*). | TC-020..TC-024, TC-077 |
| 4 | Tie-group detection & positions | Several tie groups, groups created by deductions, partial resolution (one team separated, two level), unresolved block in the middle of the table, numbering of consecutive positions. | TC-031, TC-061, TC-070, TC-033 |
| 5 | 2→3 algorithm switch and A-03 | Different code paths; teams that never met → missing map entries / NullPointerException; incomplete mini-table. | TC-060, TC-073/074, TC-075, TC-013 |
| 6 | Deductions (A-02, negative points) | Deduction leaking into h2h/mini points; clamping at 0; sorting negative numbers; deduction applied before vs after grouping. | TC-031, TC-033, TC-034, TC-107 |
| 7 | D-01 trimming applied in only one layer | `Team`, calculator lookup, duplicate check and CSV loader may each trim (or not) differently. | TC-003, TC-006, TC-042, TC-045, TC-082 |
| 8 | CSV robustness | CRLF `\r` sticking to the last field, physical vs logical line numbers, empty optional field, platform charset (macOS default UTF-8 hides a missing explicit charset; machine locale is tr_TR). | TC-080, TC-083, TC-084, TC-087 |
| 9 | CLI | `System.exit` inside `main` makes testing hard; column width computed before negative values / long Turkish names; stdout encoding. | TC-092, TC-094, TC-097, TC-099 |
| 10 | Order dependence | Ranking that depends on input order of teams/matches (stable sort used as a hidden tie-breaker instead of flagging unresolved). | TC-059, TC-104 |
| 11 | Generator correctness | A "guaranteed" tie scenario might not be an exact tie, or might create duplicate fixtures; property tests would then be meaningless. | TC-108..TC-111 |

---

## Appendix — open questions / ambiguities

All resolved on 2026-11-12 by the orchestrator in autonomous mode (student away); each resolution is recorded below and can be revisited by the student. Q3 was changed from the planned expectation to stay consistent with FR-14 (forfeit is optional, default NONE); every other question keeps the planned expectation.

| ID | Question | Resolution |
|---|---|---|
| Q1 | FMT 24/1-a says a no-show forfeit also removes 3 points; FR-06/FR-07 do not say whether the system does this automatically. | No automatic deduction; user enters `(T,3)` (TC-023) |
| Q2 | V-01 lists null team list, match list, team, match, deduction — but not a null *deductions list*. | Rejected (TC-041) |
| Q3 | Null `forfeit` in `MatchResult`: reject or treat as NONE? | Treated as NONE, consistent with FR-14's optional forfeit (TC-007 expects NONE, not an exception) |
| Q4 | Is an empty team list valid? | Valid → empty table (TC-016) |
| Q5 | Exception type and "clear message" content are not specified. | Any `IllegalArgumentException`/`NullPointerException` for library; `InputFormatException` for CSV; message contains the offending value |
| Q6 | D-01: is the *stored/displayed* name trimmed, and in which layer? | Team behaves as trimmed name (TC-003) |
| Q7 | D-02 "alphabetical": which collation; upper/lower case? | Turkish alphabet (TC-078) |
| Q8 | FR-09 V / FR-10 VII: number of forfeit losses (1 vs 2) is not compared; both or neither → still level. | Boolean criterion (TC-058) |
| Q9 | FR-14 details: case of forfeit values, empty 5th field, whitespace around numbers, `;` inside a team line, whitespace-only line = blank? | `home` rejected; empty 5th = NONE; numbers trimmed; `A;B` in teams rejected; whitespace-only ignored |
| Q10 | Negative goals in CSV: V-08 (with line number) or V-05? | Any rejection accepted (TC-088) |
| Q11 | CLI: argument order/count, exit codes, stderr vs stdout, unresolved marker text, header — not in FR-15. | `teams matches [deductions]`; errors on stderr, non-zero exit; any visible marker |
| Q12 | FR-12 literally makes a whole table "unresolved" when no matches are played (start of season). Intended? | Yes, literal reading (TC-015, TC-078) |
| Q13 | V-06 "more than two matches" is implied by the duplicate-direction rule (a 3rd match always repeats a direction). | Tested once (TC-048) |
