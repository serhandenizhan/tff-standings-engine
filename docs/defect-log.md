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
| Status | Fixed (pending commit); verified by run-02-after-fixes |

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

**Regression evidence.** `docs/results/run-02-after-fixes/`: TC-078 and TC-079 pass, full suite 125/125. Fix commit: TBD.

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
| Status | Fixed (pending commit); verified by run-02-after-fixes |

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

**Regression evidence.** `docs/results/run-02-after-fixes/`: TC-087 passes, TC-086 still passes, full suite 125/125. Fix commit: TBD.

---

## Notes

- **How DEF-001 was found.** DEF-001 was first suspected during code review of the first code version; the suspicion
  ("D-02 alphabetical sort with `String.compareTo` may order Ç, İ, Ş wrongly") was written into `CLAUDE.md` before any test
  was run. It was then confirmed, not discovered, by TC-078 and TC-079, which were designed from decision D-02 and the
  equivalence class "names with Turkish letters". DEF-002 was not suspected beforehand; it was found by TC-087 alone,
  which exists because the test plan listed the empty fifth field as an ambiguity (Q9) and resolved it before testing.
- Three test invocations failed but they belong to two defects (TC-078 and TC-079 share DEF-001).
- No test was changed to make it pass; both defects were fixed in the production code. The tests were correct.

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
