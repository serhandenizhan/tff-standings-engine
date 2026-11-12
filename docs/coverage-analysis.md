# Coverage Analysis — TFF Standings Engine

Tool: JaCoCo 0.8.13 (`mvn clean verify`, report in `docs/results/coverage/`, surefire reports and the full Maven output in
`docs/results/run-03-full-suite/`). "Before" = the suite after the generator, property-based and metamorphic tests
(640 test executions, all passing). "After" = the same suite plus the coverage-gap tests TC-140..TC-154 (655 executions,
all passing). Paths in the stored results use `~` instead of the home directory.

> **Update 2026-11-13 (after DEF-003..DEF-005, run-05, 664 invocations).** The string-building `pairKey` method was removed (DEF-003) and overflow checks were added (DEF-004), so totals moved slightly: instruction 66.2 % (3160/4770), **branch 70.8 % (267/377)**, line 71.8 % (587/818), method 87.7 % (107/122); excluding the two offline tools: instruction 96.6 %, **branch 97.4 % (267/274)**, line 95.3 %, method 98.2 %. The tables below are the 2026-11-12 measurement; the current HTML report is in `docs/results/coverage/`. The V-06 pair-count branch is still unreachable (now via the `Set`-based key). The new `addExact` catch blocks are covered by TC-161/TC-161b.

## 1. Overall result

| Metric | Before | After |
|---|---|---|
| Instruction | 62.7 % (2985 / 4760) | 66.3 % (3155 / 4760) |
| Branch | 68.1 % (258 / 379) | 71.0 % (269 / 379) |
| Line | 67.7 % (550 / 813) | 71.7 % (583 / 813) |
| Method | 82.9 % (102 / 123) | 87.8 % (108 / 123) |

The headline numbers are dragged down by two developer tools that are never run by a test: `ScenarioLibraryBuilder`
(1083 instructions, 85 branches) and `RealismCheck` (415 instructions, 18 branches). Excluding only those two classes the
figures after the added tests are:

| Metric (tools excluded) | After |
|---|---|
| Instruction | 96.7 % (3155 / 3262) |
| Branch | 97.5 % (269 / 276) |
| Line | 95.4 % (583 / 611) |
| Method | 98.2 % (108 / 110) |

The core (`model/*`, `StandingRow`, `StandingsCalculator`, `TieBreaker`, `cli/TableFormatter`) is at 100 % except for one
unreachable branch in `StandingsCalculator` (section 3).

## 2. Per class (before and after the added tests)

Nested classes are merged into their outer class.

| Class | Instr. before | Instr. after | Branch before | Branch after | Line before | Line after | Method before | Method after |
|---|---|---|---|---|---|---|---|---|
| (root).StandingRow | 100% (149/149) | 100% (149/149) | 100% (8/8) | 100% (8/8) | 100% (35/35) | 100% (35/35) | 100% (19/19) | 100% (19/19) |
| (root).StandingsCalculator | 98% (380/386) | 98% (380/386) | 98% (53/54) | 98% (53/54) | 99% (75/76) | 99% (75/76) | 100% (8/8) | 100% (8/8) |
| (root).TieBreaker | 100% (254/254) | 100% (254/254) | 100% (18/18) | 100% (18/18) | 100% (45/45) | 100% (45/45) | 100% (8/8) | 100% (8/8) |
| cli.Main | 83% (216/259) | 87% (226/259) | 90% (27/30) | 100% (30/30) | 79% (50/63) | 86% (54/63) | 50% (2/4) | 50% (2/4) |
| cli.TableFormatter | 100% (279/279) | 100% (279/279) | 100% (18/18) | 100% (18/18) | 100% (41/41) | 100% (41/41) | 100% (4/4) | 100% (4/4) |
| generator.GeneratedSeason | 14% (15/104) | 100% (104/104) | 0% (0/2) | 100% (2/2) | 7% (1/15) | 100% (15/15) | 20% (1/5) | 100% (5/5) |
| generator.RealismCheck | 0% (0/415) | 0% (0/415) | 0% (0/18) | 0% (0/18) | 0% (0/55) | 0% (0/55) | 0% (0/1) | 0% (0/1) |
| generator.ScenarioLibrary | 83% (232/278) | 83% (232/278) | 64% (9/14) | 64% (9/14) | 77% (43/56) | 77% (43/56) | 100% (10/10) | 100% (10/10) |
| generator.ScenarioLibraryBuilder | 0% (0/1083) | 0% (0/1083) | 0% (0/85) | 0% (0/85) | 0% (0/147) | 0% (0/147) | 0% (0/12) | 0% (0/12) |
| generator.SeasonGenerator | 98% (437/446) | 100% (446/446) | 96% (46/48) | 100% (48/48) | 97% (77/79) | 100% (79/79) | 91% (10/11) | 100% (11/11) |
| generator.StrengthModel | 91% (425/467) | 97% (455/467) | 85% (29/34) | 97% (33/34) | 88% (71/81) | 96% (78/81) | 93% (13/14) | 100% (14/14) |
| generator.TeamStrength | 100% (30/30) | 100% (30/30) | n/a | n/a | 100% (2/2) | 100% (2/2) | 100% (2/2) | 100% (2/2) |
| io.CsvLoader | 91% (437/479) | 98% (469/479) | 100% (30/30) | 100% (30/30) | 91% (79/87) | 98% (85/87) | 100% (18/18) | 100% (18/18) |
| io.InputFormatException | 100% (9/9) | 100% (9/9) | n/a | n/a | 100% (4/4) | 100% (4/4) | 100% (2/2) | 100% (2/2) |
| model.Forfeit | 100% (21/21) | 100% (21/21) | n/a | n/a | 100% (4/4) | 100% (4/4) | 100% (1/1) | 100% (1/1) |
| model.MatchResult | 100% (59/59) | 100% (59/59) | 100% (12/12) | 100% (12/12) | 100% (12/12) | 100% (12/12) | 100% (2/2) | 100% (2/2) |
| model.PointDeduction | 100% (23/23) | 100% (23/23) | 100% (4/4) | 100% (4/4) | 100% (6/6) | 100% (6/6) | 100% (1/1) | 100% (1/1) |
| model.Team | 100% (19/19) | 100% (19/19) | 100% (4/4) | 100% (4/4) | 100% (5/5) | 100% (5/5) | 100% (1/1) | 100% (1/1) |
| **Total** | **62.7% (2985/4760)** | **66.3% (3155/4760)** | **68.1% (258/379)** | **71.0% (269/379)** | **67.7% (550/813)** | **71.7% (583/813)** | **82.9% (102/123)** | **87.8% (108/123)** |

## 3. Remaining missed code and justification

Category (a) = logically unreachable or purely defensive code; (c) = tool / documentation-only code.

| Location | What is missed | Cat. | Justification |
|---|---|---|---|
| `StandingsCalculator` l. 109-110 (1 branch, 6 instr.) | V-06 "More than two matches between ..." | a | The duplicate-direction check (l. 102-104) runs first. Between two teams only two directions exist (A-B and B-A). A third match between them must repeat one direction and is rejected as "Duplicate fixture" before the count check. The branch can never be taken (defect-log observation 1). |
| `CsvLoader` l. 69-70 | `catch (IllegalArgumentException)` around `new Team(fields[0])` | a | `Team` rejects only blank names. A blank line is skipped by `line.isBlank()` before parsing, and `split(line, 1, 1, ...)` accepts exactly one field, which is the whole non-blank line. So the constructor cannot throw here. Kept as a guard in case `Team` validation becomes stricter. |
| `Main` l. 22 (class declaration) | implicit default constructor | a | Not invoked by anyone; `Main` is used through static methods. No behaviour. |
| `Main` l. 36-41 (`main`) | `System.exit(code)` wrapper, UTF-8 stream setup | a | A test would terminate the JVM. All logic lives in `run(...)`, which is fully covered (100 % branch). The wrapper is five lines with no decisions. |
| `Main` l. 125-128 | `catch (InputFormatException / IllegalStateException)` "Scenario library error" | a | Triggered only by a missing or corrupt packaged resource (`scenarios/*`). The library is read from the classpath and verified by TC-124..TC-126; a test would need a broken jar. |
| `ScenarioLibrary` l. 73-84, 133-134 | `UncheckedIOException` wrapping in `load` and `readLines` | a | `IOException` on a classpath stream or on `close()` of an in-memory resource stream; cannot be provoked without a faulty class loader. |
| `ScenarioLibrary` l. 102-103, 106-107, 109-110, 116-117 (4 branches) | `parseCatalogue`: blank line skipped, line with not exactly 5 fields, empty flags column, empty catalogue | a | `parseCatalogue` is private and static and reads only the packaged `catalogue.csv`, which is well formed, has no blank lines, has flags in every row and is not empty. These branches would need a different resource file. Format and content of the real catalogue are verified by TC-124, TC-125, TC-110. |
| `ScenarioLibrary` l. 124-125 | `open(...)`: resource not found -> `IllegalStateException` | a | Same reason: the resource exists in the project. Same pattern as `StrengthModel` l. 187-188. |
| `StrengthModel` l. 187-193 (1 branch) | `readResource`: resource missing, `IOException` | a | Reads fixed packaged resources; same reasoning. The invalid-content paths of the same class are now tested (TC-150..TC-152). |
| `ScenarioLibraryBuilder` (whole class and nested records) | tool `main` that regenerates `src/main/resources/scenarios/` | c | Developer tool, run by hand once. Its output is verified indirectly: TC-126 regenerates each scenario with the catalogue seed and rounds and compares it with the stored files. |
| `RealismCheck` (whole class) | tool `main` that prints statistics for `docs/generator.md` | c | Documentation helper. The properties it reports are asserted by TC-117..TC-119. |

## 4. Tests added after the coverage analysis (TC-140..TC-154)

All are white-box tests derived from a requirement, written in new `CoverageGapTest` classes (one per package); no existing test
and no production code was changed. Each carries a Turkish comment saying it was added after coverage analysis.

| TC | Class | Gap it closes | Requirement / basis | Expected |
|---|---|---|---|---|
| TC-140 | `cli/CoverageGapTest` | `Main.run(null, ...)` (null argument array) | FR-14/FR-15, D-03: wrong argument count is a usage error | exit 2, usage on stderr, no stdout |
| TC-141 | `cli/CoverageGapTest` | `--scenario random` without seed (seed from clock) | CLI contract: seed printed in the header so the pick can be reproduced | exit 0, header `Scenario Sxx (picked with seed N): ...`, rerun with N works |
| TC-142 | `cli/CoverageGapTest` | `IllegalArgumentException` from the calculator reaches the CLI (duplicate team) | V-02, FR-14 | exit 1, `Invalid data: Duplicate team`, no table |
| TC-143 | `cli/CoverageGapTest` | same catch, duplicate fixture | V-06 | exit 1, `Invalid data: Duplicate fixture`, no table |
| TC-144 | `io/CoverageGapTest` | `readLines(Path)`, non-`NoSuchFile` `IOException` (invalid UTF-8 bytes) | FR-14 (UTF-8), V-08 | `InputFormatException` "Cannot read file" |
| TC-145 | `io/CoverageGapTest` | same, path is a directory | V-08 | `InputFormatException`, no raw `IOException` |
| TC-146 | `io/CoverageGapTest` | `readLines(Reader)`, `IOException` while reading | V-08 | `InputFormatException` "Cannot read teams.csv" |
| TC-147 | `io/CoverageGapTest` | `readLines(Reader)`, `IOException` on `close()` | V-08 | `InputFormatException` "Cannot read matches.csv" |
| TC-148 | `generator/CoverageGapTest` | `GeneratedSeason.writeCsv` was never executed | FR-14 file format, generator.md "Scenario library" | written files read back by `CsvLoader` equal the season (forfeit and deduction included, Turkish names) |
| TC-149 | `generator/CoverageGapTest` | `writeCsv` empty-list branch | class contract "empty file when no rows" | `deductions.csv` has size 0 |
| TC-150 | `generator/CoverageGapTest` | `StrengthModel.parseRows` blank-line skip | generator.md (blank lines ignored) | model built, attack = 42/20 |
| TC-151 | `generator/CoverageGapTest` | wrong field count, non-integer value | generator.md input format | `IllegalArgumentException` |
| TC-152 | `generator/CoverageGapTest` | no promoted club (one season only), empty club list | generator.md: promoted average needs season t >= 2 | `IllegalArgumentException` |
| TC-153 | `generator/CoverageGapTest` | `promotedAverage()` accessor | generator.md "Strength" | attack 1.2, defence 1.4, club without history gets these values |
| TC-154 | `generator/CoverageGapTest` | `SeasonGenerator.seed()`, `doubleRoundRobin` invalid team counts | generator.md "Schedule": even n >= 2 | seed returned; n = 0, 1, 3, 17 rejected; n = 2 gives 2 rounds with swapped venues |

Result: all 15 new executions passed. No new defect (no DEF-003).

## 5. Critical discussion

- **Coverage measures execution, not verification.** DEF-001 (Turkish alphabetical order) sat on lines and branches that were
  already executed by the first test run; the comparator was exercised, but with ASCII names only. Only the tests with
  Turkish letters (TC-078, TC-079) exposed it. The same holds for DEF-002. 100 % line coverage would not have found either.
- **100 % is not the goal.** The two remaining categories are justified above. Writing contrived tests (broken class loaders,
  reflection into private methods, `System.exit` interception) to reach the last branches would add maintenance cost and no
  confidence. The unreachable V-06 branch is documented rather than removed because the specification states the rule in both forms.
- **Where coverage was useful.** It pointed at real gaps: the CLI handling of calculator errors (Main), I/O failure paths in
  `CsvLoader`, and a whole untested method (`GeneratedSeason.writeCsv`). Those paths implement V-02, V-06, V-08 and had no test.
- **Branch versus line.** Branch coverage is stricter: `Main` had 79 % lines but its three missed branches were all
  requirement-relevant. The tools (`ScenarioLibraryBuilder`, `RealismCheck`) make the global figure look worse than the shipped
  library is; reporting totals with and without them is more honest than hiding either.
- **Limits of the added tests.** TC-142/TC-143 and TC-144..TC-147 assert messages by substring, so wording changes will break them.
  TC-148 checks round-trip equality, not that the stored library files match (that is TC-126).
- **Not measured.** Coverage says nothing about missing requirements, wrong expected values, or the rule interpretation
  assumptions (A-01..A-04). Those were covered by the review of the test plan, not by JaCoCo.
