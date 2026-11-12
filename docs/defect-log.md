# Defect Log

This log records every defect found by the JUnit test suite (`src/test/java`) derived from `docs/test-plan.md`.
Each defect gets an ID (`DEF-nnn`), is traced to the test cases that detected it and to the requirement it violates,
and is analysed down to the faulty source line. Fields per entry: detected by (test cases and test design technique),
date, severity (impact on the user of the library) and priority (urgency of the fix), requirement violated, steps to
reproduce, expected vs. actual result (verbatim from the test run), root cause, fix, status and regression evidence.

Detection run: `docs/results/run-01-initial/mvn-test-output.txt` (commit f8e8d6b): 125 test invocations, 3 failed (2 failures, 1 error), 2 distinct defects.
Regression run after the fixes: `docs/results/run-02-after-fixes/mvn-test-output.txt`: 125 run, 0 failures, 0 errors.

Severity scale: High (wrong table or crash on valid, common input) / Medium (wrong result or rejection of valid input in a narrower case) / Low (cosmetic).
Priority scale: P1 (fix before anything else) / P2 (fix in this cycle) / P3 (can wait).

---

## DEF-001: Teams that cannot be separated are displayed in Unicode code-point order instead of Turkish alphabetical order

| Field | Value |
|---|---|
| ID | DEF-001 |
| Detected by | TC-078, TC-079 (`TieBreakMultiTeamTest$TurkishAlphabeticalOrder`); equivalence partitioning, class "team names containing Turkish letters (Ç, İ, Ş, Ü)" |
| Date | 2026-11-12 |
| Severity | Medium: the table content and the unresolved flags are right, but the display order of unresolved teams breaks decision D-02 for any club whose name starts with a Turkish letter |
| Priority | P2: not a data-loss problem, but the library is built for Turkish club names, so the case is realistic and the fix is small |
| Requirement violated | D-02 (alphabetical display order of unresolved teams, Turkish alphabet); FR-12 (unresolved teams are marked, display order per D-02) |
| Status | Closed — fixed and verified by run-02-after-fixes |

**Steps to reproduce.** Calculate a table for the teams `Denizlispor` and `Çaykur Rizespor` with one match `Denizlispor;Çaykur Rizespor;1;1` (TC-079). Both teams have the same points, the same head-to-head record, the same goal difference and goals scored, so no criterion separates them and the display order decides. TC-078 uses 11 teams without matches, entered in reverse alphabetical order.

**Expected.** `[Çaykur Rizespor!, Denizlispor!]` (Ç sorts directly after C, before D).

**Actual (verbatim).**

```
TieBreakMultiTeamTest$TurkishAlphabeticalOrder.tc079:191 sıra / çözülemedi bayrağı ==> expected: <[Çaykur Rizespor!, Denizlispor!]> but was: <[Denizlispor!, Çaykur Rizespor!]>
TieBreakMultiTeamTest$TurkishAlphabeticalOrder.tc078:179 sıra / çözülemedi bayrağı ==> expected: <[Ankaragücü!, Beşiktaş!, Çaykur Rizespor!, Denizlispor!, Iğdır FK!, İstanbul Başakşehir!, Kasımpaşa!, Şanlıurfaspor!, Trabzonspor!, Ümraniyespor!, Vanspor!]> but was: <[Ankaragücü!, Beşiktaş!, Denizlispor!, Iğdır FK!, Kasımpaşa!, Trabzonspor!, Vanspor!, Çaykur Rizespor!, Ümraniyespor!, İstanbul Başakşehir!, Şanlıurfaspor!]>
```

**Root cause.** `src/main/java/edu/svv/standings/TieBreaker.java:54` (original numbering), in `order(...)`:
`ordered.sort(criteria.thenComparing(r -> r.getTeam().name()));`. `thenComparing` with a key extractor uses the natural
order of `String`, i.e. `String.compareTo`, which compares UTF-16 code units. The Turkish letters Ç (U+00C7), İ (U+0130),
Ş (U+015E) and Ü (U+00DC) have code points above `Z`, so they are placed after all ASCII letters. The same applies to
the dotless/dotted I pair. The hypothesis was checked by reading the code before changing it: the actual order in TC-078
(plain ASCII names first, then Ç, Ü, İ, Ş) is exactly the code-point order, and no other comparison of team names exists in the
display path. The FMT criteria comparator (`criteria`) compares numbers only and is not involved.

**Fix.** In `TieBreaker.order(...)` the final display tiebreak now uses `Collator.getInstance(Locale.forLanguageTag("tr-TR"))`
as the comparator for the name key: `criteria.thenComparing(r -> r.getTeam().name(), turkish)`. The FMT `criteria` comparator
and the unresolved-marking loop (which uses `criteria.compare`) are unchanged, so only the display order is affected.
`StandingsCalculator.pairKey` also uses `compareTo`, but only as an internal, consistent key and not for display, so it is left as is.
Only `TieBreaker.java` is changed.

**Regression evidence.** `docs/results/run-02-after-fixes/`: TC-078 and TC-079 pass, full suite 125/125. Fix commit: `37b9388`.

---

## DEF-002: Empty optional fifth field in a matches line is rejected instead of defaulting to NONE

| Field | Value |
|---|---|
| ID | DEF-002 |
| Detected by | TC-087 (`CsvLoaderTest.tc087_emptyFifthFieldIsNone`); boundary value analysis / equivalence partitioning on the optional CSV field "forfeit" (class: field present but empty, EP-52) |
| Date | 2026-11-12 |
| Severity | Medium: a valid file is rejected with an exception, so no table is produced; there is an easy workaround (omit the trailing `;`) |
| Priority | P2: spreadsheet exports commonly write a trailing empty column, so real files will hit it, but nothing is corrupted |
| Requirement violated | FR-14 (forfeit is optional, default NONE); test plan appendix Q9 (empty fifth field means NONE) |
| Status | Closed — fixed and verified by run-02-after-fixes |

**Steps to reproduce.** Load a matches file containing the single line `A;B;1;0;` with teams `A` and `B` (the line has five fields, the last one empty).

**Expected.** One match A-B 1-0 with forfeit `NONE`.

**Actual (verbatim).**

```
CsvLoaderTest.tc087_emptyFifthFieldIsNone:216 » InputFormat matches.csv, line 1: unknown forfeit value '' (expected NONE, HOME or AWAY)
```

**Root cause.** `src/main/java/edu/svv/standings/io/CsvLoader.java:97` decides "forfeit given or not" only by the field
count (`fields.length == 5`). `split(...)` uses `limit -1`, so the trailing empty field is kept and the line has 5 fields.
`parseForfeit` (line 183, original numbering) then calls `Forfeit.valueOf("")`, which throws `IllegalArgumentException`, and this
is reported as "unknown forfeit value ''". The empty string is treated as a value instead of as an absent value. Confirmed
by reading `split` and `parseMatches` before the change; no other code path touches the forfeit field.

**Fix.** `parseForfeit` returns `Forfeit.NONE` when the (already trimmed) text is empty. All other values behave as before:
`NONE`, `HOME`, `AWAY` are accepted, anything else (including lowercase `home`, TC-086) is still rejected. Only `CsvLoader.java` is changed.

**Regression evidence.** `docs/results/run-02-after-fixes/`: TC-087 passes, TC-086 still passes, full suite 125/125. Fix commit: `e03aea7`.

---

## DEF-003: Team names containing `|` cause false "Duplicate fixture" errors

| Field | Value |
|---|---|
| ID | DEF-003 |
| Detected by | Independent code review (bug-hunt reviewer, 2026-11-13), confirmed by TC-160 and TC-160b (`ReviewDefectsTest`); equivalence class "team names containing the key separator" |
| Date | 2026-11-13 |
| Severity | Medium-Low: valid input is rejected, no table is produced; needs a `|` in a club name, which is unusual but allowed by FR-01/FR-14 |
| Priority | P3 |
| Requirement violated | V-06 (only a real duplicate fixture or a third match may be rejected), FR-02 |
| Status | Closed — fixed and verified by run-05-after-review-fixes |

**Steps to reproduce.** Teams `A|B`, `C`, `A`, `B|C`; matches `A|B`–`C` 1-0 and `A`–`B|C` 2-2.

**Expected.** Both matches accepted (they are different fixtures).

**Actual (verbatim, run-04).**

```
ReviewDefectsTest.tc160_pipeInNamesIsNotDuplicateFixture:30 Unexpected exception thrown: java.lang.IllegalArgumentException: Duplicate fixture: A|B|C
```

**Root cause.** `StandingsCalculator.validateMatches` built the fixture key as `home.name() + "|" + away.name()` and the pair key the same way. Both fixtures above produce the string `A|B|C`, so the second is reported as a duplicate. The pair counter had the same collision.

**Fix.** Keys are built from the `Team` values themselves: `List.of(home, away)` for a fixture and `Set.of(home, away)` for an unordered pair. `Team` is a record, so equality is by the trimmed name and no separator is needed. The string-building `pairKey` method was removed. Fix commit: `b35e03b`.

---

## DEF-004: Goal and deduction totals overflow `int` silently

| Field | Value |
|---|---|
| ID | DEF-004 |
| Detected by | Independent code review (bug-hunt reviewer), confirmed by TC-161 and TC-161b (`ReviewDefectsTest`); boundary value analysis at `Integer.MAX_VALUE` |
| Date | 2026-11-13 |
| Severity | Low: needs absurd values, but the output is silently wrong (a heavily penalised team shows positive points) |
| Priority | P3 |
| Requirement violated | FR-05/FR-07 (totals must be correct); the general rule that invalid input is rejected with a clear message. New rule V-09 added in requirements v1.2 |
| Status | Closed — fixed and verified by run-05-after-review-fixes |

**Steps to reproduce.** P beats Q 1-0; two deductions of 2 147 483 647 points for P.

**Expected.** Input rejected with a message naming P (an exact negative total cannot be stored in `int`).

**Actual (run-04).** No exception; P's points were shown as **5**, because `deductedPoints += points` wrapped around. Goals behaved the same way (`Integer.MAX_VALUE` goals plus 1 gave −2 147 483 648).

**Root cause.** `StandingRow.addMatch` and `addDeduction` used plain `int` addition.

**Fix.** `Math.addExact` for goals for, goals against and deductions; an `ArithmeticException` is turned into `IllegalArgumentException("... out of range for team <name>")`. Fix commit: `838b916`.

**Test correction (recorded honestly).** TC-161b originally asserted that the message names P. Every goal P scores is conceded by Q, so Q's goals-against total overflows as well, and Q is processed first in that input. The assertion was relaxed to "names P or Q" in the fix commit. The test still fails on the unfixed code (nothing was thrown), so the correction does not hide the defect.

---

## DEF-005: Round-robin schedule is not balanced (some teams play 0 home games in the first half)

| Field | Value |
|---|---|
| ID | DEF-005 |
| Detected by | Independent code review (bug-hunt reviewer), confirmed by TC-162 (`generator/ScheduleBalanceTest`) for 4, 6, 18 and 20 teams |
| Date | 2026-11-13 |
| Severity | Low: affects only the test-data generator; full seasons are still 17/17 home/away, but mid-season scenarios (S06–S12) were biased |
| Priority | P3 |
| Requirement violated | The generator's own documented contract (code comment "dengeli" = balanced) and the student's decision that generated seasons are realistic (CLAUDE.md) |
| Status | Closed — fixed and verified by run-05-after-review-fixes |

**Steps to reproduce.** `SeasonGenerator.doubleRoundRobin` with 18 teams; count home games per team in rounds 1–17.

**Expected.** 8 or 9 home games per team.

**Actual (verbatim, run-04).**

```
ScheduleBalanceTest.tc162_firstHalfHomeGamesBalanced:40 T1 ilk devrede 0 iç saha maçı oynuyor, beklenen 8..9
```

Counts ranged from 0 to 16.

**Root cause.** The circle method rotates every team except the first. The home rule `(round + slot) % 2 == 0` moved together with the rotation, so the team that started in slot 1 stayed on the "away" side in every round.

**Fix.** For non-fixed slots the home side depends on slot parity only (`slot % 2 == 1`); the fixed team still alternates by round. A simulation for 2–40 teams confirmed ⌊(n−1)/2⌋..⌈(n−1)/2⌉ home games for every team. Because library scenarios are generator output, the scenario library S01–S12 was regenerated with `ScenarioLibraryBuilder` (new seeds, same twelve selection rules; catalogue in `src/main/resources/scenarios/catalogue.csv`). Fix commit: `5af729c`.

---

## Notes

- **How DEF-001 was found.** DEF-001 was first suspected during code review of the first code version; the suspicion
  ("D-02 alphabetical sort with `String.compareTo` may order Ç, İ, Ş wrongly") was written into `CLAUDE.md` before any test
  was run. It was then confirmed, not discovered, by TC-078 and TC-079, which were designed from decision D-02 and the
  equivalence class "names with Turkish letters". DEF-002 was not suspected beforehand; it was found by TC-087 alone,
  which exists because the test plan listed the empty fifth field as an ambiguity (Q9) and resolved it before testing.
- Three test invocations failed but they belong to two defects (TC-078 and TC-079 share DEF-001).
- **DEF-003..DEF-005 were found by an independent code review** (four reviewers working separately: standards, spec, bug hunt, test quality) after the full suite was green. The original black-box plan had no equivalence class for a separator character in names, no boundary at the `int` limit and no balance property for the schedule. Each defect was first written as a failing test (red commit `bf4be97`, run-04: 663 invocations, 8 failing), then fixed in its own commit, then the full suite was rerun (run-05: 664/664).
- For DEF-001..DEF-003 and DEF-005 no test was changed; the production code was fixed. For DEF-004 one assertion message check was corrected (see DEF-004).

## Observations that are not defects

These were noticed during analysis. They do not violate a requirement, so they are not logged as defects.

1. **V-06 "more than two matches" branch is logically unreachable.** In `StandingsCalculator` the duplicate-fixture check
   (same home/away direction twice) runs first. Between two teams only two directions exist (A-B and B-A), so a third match
   between them always repeats a direction and is already rejected as "Duplicate fixture". The second check can never fire.
   It is harmless defensive code; it cannot be covered by a test and is expected to show as an uncovered branch in the coverage report.
2. **FR-12 flags the whole table at season start.** With no matches played all teams are level on every criterion, so all
   of them are marked unresolved. This is the literal reading of FR-12 and was accepted (see TC-078, which relies on it).
3. **`TieBreaker` mutates the `unresolved` flag of the rows passed in.** `order(...)` calls `setUnresolved(true)` on the given
   `StandingRow` objects. This is safe because `StandingsCalculator.calculate` creates fresh rows on every call, so no
   state leaks between calls. It would matter only if rows were reused by a future caller.
