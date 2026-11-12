# Requirements Specification — TFF Standings Engine

> Status: **v1.0** — approved by student (2026-11-12).
> Sources: 2026–27 Süper Lig Müsabakaları Statüsü (SLS), TFF Futbol Müsabaka Talimatı (FMT), TFF Futbol Disiplin Talimatı (FDT).

## 1. Scope

A pure Java library that takes a list of teams, played match results and point deductions, and produces the final league table ordered according to FMT Art. 9. No GUI, no database, no file/network I/O.

## 2. Inputs

| Input | Fields |
|---|---|
| Team | name |
| MatchResult | homeTeam, awayTeam, homeGoals, awayGoals, forfeitingTeam (optional: home / away / none) |
| PointDeduction | team, points |

## 3. Functional Requirements

| ID | Requirement | Source |
|---|---|---|
| FR-01 | The system shall accept a set of teams. Team names must be non-blank and unique. | — |
| FR-02 | The system shall accept match results between registered teams, with non-negative goal counts. | SLS Art. 3 |
| FR-03 | Between any two teams at most two matches may exist, and at most one per home/away direction (double round-robin). | SLS Art. 3(1) |
| FR-04 | Each match shall award 3 points for a win, 1 point to each team for a draw, 0 points for a loss. | FMT 9/1 |
| FR-05 | For each team the system shall compute: played, won, drawn, lost, goals for, goals against, goal difference, points. | FMT 9/1–2 |
| FR-06 | Forfeit: when a match is marked as forfeited, the non-forfeiting team gets 3 goals; the forfeiting team's goals are void; if the non-forfeiting team scored more than 3, its actual goals stand. The adjusted score is used everywhere (points, stats, head-to-head). | FMT 23/4 |
| FR-07 | Point deductions shall be subtracted from a team's total points. Multiple deductions accumulate. Total points may become negative. | FMT 24/1-a, FDT 33 |
| FR-08 | Teams shall be ordered by total points, descending. | FMT 9/2 |
| FR-09 | Two teams level on points shall be ordered by, in sequence: (I) points in their mutual matches, (II) goal difference in their mutual matches (away goals have no extra weight), (III) overall goal difference, (IV) overall goals scored, (V) the team without a forfeit loss ranks higher. | FMT 9/2-a I–V |
| FR-10 | Three or more teams level on points shall be ordered using a mini-table built only from matches among those teams, by: (II) mini-table points, (III) mini-table goal difference, (IV) mini-table goals scored, (V) overall goal difference, (VI) overall goals scored, (VII) the team without a forfeit loss ranks higher. | FMT 9/2-b I–VII |
| FR-11 | Within the 3+ procedure, if some teams are still level, the mini-table shall **not** be rebuilt for them; the remaining criteria of FR-10 continue to apply. | FMT 9/2-b I (parenthesis) |
| FR-12 | If teams remain level after all criteria, the system shall mark them as "unresolved — requires playoff" instead of ordering them arbitrarily. | FMT 9/2-a VI, 9/2-b VIII |
| FR-13 | The output shall be a ranked list with position, all FR-05 statistics, points after deduction, and the unresolved flag. | — |

## 4. Validation (invalid inputs must be rejected with an exception and a clear message)

| ID | Rule |
|---|---|
| V-01 | Null team list, match list, team, match or deduction |
| V-02 | Blank or duplicate team name |
| V-03 | Match referencing an unregistered team |
| V-04 | Same team on both sides |
| V-05 | Negative goal count |
| V-06 | Duplicate fixture (same home/away pair twice) or more than two matches between two teams |
| V-07 | Point deduction of zero or negative points, or for an unregistered team |

## 5. Assumptions

| ID | Assumption | Why |
|---|---|---|
| A-01 | Goal difference = goals for − goals against. | FMT does not define it; standard Turkish usage. |
| A-02 | Point deductions affect only total points; head-to-head and mini-table points are computed from match results. | Not stated in FMT/FDT. |
| A-03 | Ranking rules are the same during and at the end of the season. If two teams have not met yet, mutual-match criteria are equal and the next criterion applies. | FMT defines one system; confirmed by student. |
| A-04 | Only played (or forfeit-awarded) matches are entered; postponed matches are simply absent. | — |

## 6. Out of Scope

- FMT 9/3 (TFF deciding not to apply goal difference).
- Playing the playoff match itself (FR-12 only flags it).
- Withdrawn/expelled teams (FMT 24 second violation) and its effect on earlier results.
- Relegation/European places, fair-play tables, lots.

## 7. Decisions

| ID | Question | Decision |
|---|---|---|
| D-01 | Are team names compared case-sensitively? | Exact match after trimming. |
| D-02 | How are unresolved teams listed? | Consecutive positions, alphabetical order for display only, flagged. |
