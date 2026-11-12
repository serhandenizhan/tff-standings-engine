# TFF Standings Engine: Verification and Validation of a League Table Calculator with JUnit 5

| | |
|---|---|
| **Student** | Ozan Serhan Denizhan |
| **Student No** | 220911758 |
| **Course** | SWE411 - Software Verification and Validation — individual JUnit assignment |
| **Date** | 13.11.2026 |
| **Repository** | https://github.com/serhandenizhan/tff-standings-engine |

> Draft status: full report draft written from the repository files on 2026-11-12. Every number below comes from a file in the repository (see the Appendix). Places marked `[STUDENT ...]` must be completed or reviewed by the student.

---

## Abstract

The project is a small Java 21 library with a command-line front end. It takes teams, played match results and point deductions and produces a league table ordered according to Article 9 of the Turkish Football Federation's Football Competition Instruction (FMT). Tie-breaking is the difficult part. Two level teams and three or more level teams follow different procedures, and in the 3+ case the rules forbid going back to the mutual matches of a sub-group that is still level. The software was verified with a black-box test plan of 133 test cases (equivalence partitioning, boundary value analysis, decision tables, negative and regression testing, plus property-based and metamorphic tests over generated seasons). These were implemented as JUnit 5 tests that expand to 655 test invocations. The first run of the hand-designed suite (125 invocations) had 3 failing invocations caused by 2 defects. Both were fixed, and the final suite passes 655/655. Branch coverage is 71.0 % overall and 97.5 % when two developer tools that are never run by tests are excluded. The report also discusses what these results do not show.

---

## 1. Introduction and Problem Definition

### 1.1 Problem

A league table looks like a trivial computation: 3 points for a win, 1 for a draw, sort by points. The difficulty starts when teams finish level on points. The Turkish Süper Lig statute for 2026–27 defines the competition format (18 teams, double round robin) but contains no tie-break rule of its own, so the general rule of the TFF Football Competition Instruction (Futbol Müsabaka Talimatı, FMT) applies [1], [2]. FMT Article 9/2 defines two different procedures [2]:

- **Two teams level (9/2-a):** (I) points in their mutual matches, (II) goal difference in their mutual matches, with no extra weight for away goals, (III) overall goal difference, (IV) overall goals scored, (V) the team without a forfeit loss ranks higher, (VI) a single play-off match.
- **Three or more teams level (9/2-b):** a mini-table is built only from the matches among the tied teams, and teams are ordered by mini-table points, mini-table goal difference, mini-table goals scored, then overall goal difference, overall goals scored, the forfeit criterion, and finally match(es) decided by the TFF. The text adds in parentheses that if some of the tied teams are still level, their matches are **not looked at again**. The procedure does not fall back to the two-team rule for a remaining pair.

Two further articles change the input data. FMT 23/4 converts a forfeited match into a 3-0 win, or keeps the winner's actual goals if it scored more than 3 [2]. FMT 24/1-a and Article 33 of the Disciplinary Instruction (FDT) allow point deductions, which can push a club's total below zero [2], [3].

These rules interact in ways that are easy to get wrong. The same pair of teams can be ordered differently depending on whether a third team is level with them. A forfeit changes both points and goal differences, including the mini-table. A deduction can create a tie that would not otherwise exist.

### 1.2 Motivation: a real disagreement

During data collection for this project (see `docs/data-sources.md`), a real example came up. In the 2024–25 Süper Lig, Alanyaspor, Kayserispor and Gaziantep FK all finished on 45 points. At the time of collection, English Wikipedia listed them as Gaziantep FK 12th, Alanyaspor 13th, Kayserispor 14th, while Turkish Wikipedia listed Alanyaspor 12th, Kayserispor 13th, Gaziantep FK 14th [4], [5]. The mini-table of the three clubs, computed from the results matrix on the English page itself, gives Alanyaspor 7 points, Kayserispor 5 and Gaziantep FK 4. That supports the Turkish order. All other figures (W/D/L/GF/GA/points) were identical in both sources. Two widely read sources thus disagreed on a real final table because of exactly the 3+-team procedure this project implements. This is the motivation for a tool whose tie-breaking has been verified. (The project did not run its own engine on the full 2024–25 results, because only final tables, not match-level data, were collected; see Section 8.3.)

### 1.3 Proposed solution

The solution is a pure Java library (`edu.svv.standings`) that:

1. accepts teams, played (or forfeit-awarded) match results and point deductions, and validates them;
2. computes each team's statistics with the forfeit adjustment of FMT 23/4 and the deductions;
3. orders teams by points and resolves ties with FMT 9/2-a/b, including the "no re-look" rule;
4. flags teams that remain level after all computable criteria as *unresolved — requires play-off*, instead of ordering them arbitrarily.

A thin command-line program reads the data from CSV files and prints the table. A seeded season generator, based on ten real Süper Lig seasons, provides realistic full-size test data. The emphasis of the assignment, and of this report, is verification: a test plan derived from the rules, its JUnit implementation, coverage and defect analysis.

---

## 2. Background and Research

### 2.1 The role of software testing in software quality assurance

Software testing and quality assurance (QA) are related but not identical. The ISTQB Foundation Level syllabus describes testing as a product-oriented activity that supports reaching appropriate quality levels and as one major form of quality control. It describes QA as process-oriented and preventive: if a good process is followed correctly, it should generate a good product [6, Sec. 1.2.2]. Test results serve both purposes. In testing they are used to fix defects. In QA they show how well the development and test processes perform [6, Sec. 1.2.2].

Testing cannot show that software is correct. The syllabus lists exhaustive testing as impossible, so test techniques and risk-based prioritisation are used to focus effort. It also lists the "absence-of-defects fallacy": verifying all specified requirements and fixing all defects found can still produce a system that does not meet users' needs, which is why validation is needed in addition to verification [6, Sec. 1.3]. This is the reason for the "V&V" in the course name. Verification asks whether the product was built right against the specification. Validation asks whether it is the right product.

In this project the specification is `docs/requirements.md` (FR-01..FR-15, validation rules V-01..V-08), derived from the FMT. The process followed the order QA recommends: requirements, first code version, test plan, tests, defect log, fix, regression run. The commit history shows this order (Section 6.4). Testing acted as quality control on the product. The traceability matrix and the defect log act as process evidence. Testing found two real defects (DEF-001, DEF-002); DEF-001 had been suspected during code review, DEF-002 had not (Section 7.3).

### 2.2 Purpose and benefits of automated unit testing

A unit test executes a small piece of code, such as a class or method, and compares the observed result with an expected result written beforehand. Automating this means a tool does the comparison and the suite can be rerun at any time. The ISTQB syllabus notes that automated regression testing is one case where repeating the same tests stays useful [6, Sec. 1.3, principle 5]. It requires confirmation and regression testing after every change that fixes a defect or adds a feature [6, Sec. 2.2.3].

The benefits that this project can support are modest and concrete:

- **Repeatability.** The suite runs with one Maven command. The regression run after the fixes (`docs/results/run-02-after-fixes`) used the same command as the detection run, which makes the before/after comparison fair.
- **Fast feedback on a fix.** After DEF-001 and DEF-002 were fixed, one command showed that the previously failing tests passed and that nothing else broke (125/125).
- **Executable documentation.** Each test carries its test case identifier and a short rationale from the test plan, e.g. `@DisplayName("TC-073 FR-11: no re-look at A vs B, overall GD decides")`, so the test states a requirement.
- **Early, localised failure.** Unit tests of `Team`, `MatchResult` and `PointDeduction` fail at the faulty class instead of showing up later as a wrong table.

There are costs. Tests must be maintained, a wrong expected value gives a false alarm, and automated checks only detect what their authors thought to check [6, Sec. 1.3]. In this project expected values were computed by hand in the test plan so that the oracle does not depend on the code under test (Section 5.1).

### 2.3 JUnit 5: main features and their use in this project

**Architecture.** The JUnit 5 User Guide describes JUnit 5 as three parts [7]. The **Platform** is the foundation for launching test frameworks on the JVM and defines the `TestEngine` API. **Jupiter** is the programming and extension model for writing tests. **Vintage** runs JUnit 3 and 4 tests on the Platform. This project depends only on `junit-jupiter`, managed through the `junit-bom` at version 5.11.4 in `pom.xml`, so Vintage is not needed.

**Maven Surefire.** The Maven Surefire plugin selects a test engine from the test dependencies: if JUnit 5 Platform artifacts are present, it uses the JUnit Platform with the Jupiter engine [8]. The project configures `maven-surefire-plugin` 3.5.2, so `mvn test` discovers and runs all Jupiter tests and writes reports to `target/surefire-reports`. `mvn verify` also runs JaCoCo 0.8.13 and creates the HTML coverage report. Copies are kept in `docs/results/`.

| Feature | What it does [7] | Where this project uses it |
|---|---|---|
| `@Test`, `@DisplayName` | marks a test method; readable report name | 119 `@Test` methods; every name starts with the TC number, so report lines map to the test plan |
| `@Nested` | non-static inner class to group related tests | 14 nested classes, e.g. `StandingsCalculatorTest` → `Stats`, `ForfeitAdjustment`, `Deductions`, `Validation`; `TieBreakMultiTeamTest` → `CriterionOrder`, `ForfeitAndUnresolved`, `NoReLookAtSubTies`, `ExtraRules`, `TurkishAlphabeticalOrder` |
| `@ParameterizedTest` | one method, many argument sets | 34 parameterised methods; together they produce most of the 655 invocations |
| `@ValueSource`, `@NullSource`, `@CsvSource`, `@MethodSource` | argument sources | `TeamTest` (valid / blank / null names); `MatchResultTest`, `PointDeductionTest` (boundary rows); `StandingsCalculatorTest.awayForfeitCases()` (forfeit boundary around 3 goals); generator tests take seasons from `GeneratorTestSupport` |
| `assertEquals`, `assertTrue`, `assertAll` | assertions; `assertAll` reports every failing field of a row at once | 25 `assertAll` blocks, mainly for statistics rows |
| `assertThrows` | asserts the exception type and returns it for message checks | 48 call sites in negative tests, e.g. TC-035 checks that the message names the unknown team |
| `@TempDir` | temporary directory deleted after the test | `CsvLoaderTest`, `MainTest` and the three `CoverageGapTest` classes write CSV files there |

**Lifecycle and extensions.** By default Jupiter creates a new instance of the test class for each test method [7]. The project relies on this default. Tests share no mutable state and use no `@BeforeEach`/`@BeforeAll` callbacks, because the model objects are immutable records and each test builds its own data. The project writes no extension of its own; it only uses the built-in extensions behind `@TempDir` and `@ParameterizedTest` [7].

### 2.4 Test design techniques and their applicability

The techniques come from the black-box family described in the ISTQB syllabus [6, Sec. 4.2] and in the classic testing literature [9]. Equivalence partitioning (EP) divides data into partitions that are expected to be processed the same way. Boundary value analysis (BVA) exercises the edges of those partitions. Decision tables list combinations of conditions and the resulting action, which makes missing combinations visible [6, Sec. 4.2]. Negative testing checks that invalid input is rejected. The ISTQB syllabus separates confirmation testing (the original defect is fixed) from regression testing (nothing else broke) [6, Sec. 2.2.3]. Section 5 shows how each technique was applied.

Hand-made cases are small and only check what the designer thought of. Random testing checks general properties over many generated inputs. QuickCheck popularised this approach: the tester states properties that should hold, and the tool checks them on randomly generated data [10]. Metamorphic testing addresses the *oracle problem*: when the exact expected output is unknown, one checks relations between the outputs of related inputs [11]. A full 18-team season has no hand-computed expected table, so this project uses both ideas (Section 5.7). The 515 generator, property and metamorphic invocations all passed on their first run; Section 7.4 discusses what that means.

### 2.5 Code coverage and its limitations

Coverage measures how much of the code the tests execute. JaCoCo reports several counters. Instruction coverage counts bytecode instructions. A line counts as covered if at least one of its instructions ran. Branch coverage counts the executed and missed branches of `if` and `switch` statements [12]. The ISTQB syllabus defines statement and branch coverage in the same spirit [6, Sec. 4.3]. Coverage is useful as a negative signal: code that no test executes is certainly untested. The project used JaCoCo for exactly this, after the black-box design, and added 15 tests for the gaps it found (Section 7.1).

Coverage is a weak positive signal. The syllabus warns that executing a statement does not always detect a defect, for example a data-dependent one [6, Sec. 4.3]. Inozemtseva and Holmes found that, once suite size is controlled for, the correlation between coverage and test-suite effectiveness is low to moderate. They conclude that coverage alone is not a reliable quality target, though it can help find under-tested code [13]. This is the finding of one study, not a general law. The project shows the same limits. DEF-001 sat in a statement that every table-building test executed, but only tests with Turkish letters revealed it. And one remaining uncovered branch is unreachable by design, not a testing gap (Section 7.2). Coverage also says nothing about missing code: an unimplemented requirement has no code to cover. For that reason the traceability matrix links every requirement to test cases, independently of the coverage figures.

### 2.6 Challenges learning Java and implementing tests

`[STUDENT TO WRITE]` Personal reflection, about 150–250 words. Possible prompts:

- What was new about Java compared with languages you knew (records, immutability, generics, checked versus unchecked exceptions, streams)?
- Which JUnit feature was hardest at first (`@Nested` instances, `@MethodSource` factory methods, `@TempDir`, why `assertThrows` needs a lambda)?
- How did you check expected values by hand, and did any hand calculation turn out to be wrong rather than the code?
- What was it like to see a defect you had suspected (DEF-001) confirmed, compared with one nobody had suspected (DEF-002)?
- What did the Turkish-locale environment (tr_TR) or UTF-8 handling teach you?
- What would you do differently next time?

---

## 3. Requirements Specification

The full specification is `docs/requirements.md` (v1.1, approved by the student on 2026-11-12). This section summarises it.

### 3.1 Scope

A Java application that takes a list of teams, played match results and point deductions and produces the league table ordered according to FMT Art. 9. The core is a pure library. A small command-line front end reads the inputs from CSV files and prints the table. There is no GUI, database or network I/O.

### 3.2 Rule source chain

| Level | Document | What it provides |
|---|---|---|
| 1 | 2026–27 Süper Lig Müsabakaları Statüsü [1] | Art. 3: 18 teams, double round robin. No tie-break rule of its own, no contrary provision, so the general instruction applies. |
| 2 | FMT Art. 9/1 [2] | win 3, draw 1, loss 0 |
| 2 | FMT Art. 9/2-a, 9/2-b [2] | two-team and 3+-team tie-break procedures (Section 1.1) |
| 2 | FMT Art. 23/4 [2] | forfeit score: winner gets 3 goals, loser's goals void, winner keeps its goals if more than 3 (5-2 → 5-0; 1-2 → 3-0) |
| 2 | FMT Art. 24/1-a [2] | not appearing or leaving the pitch: forfeit loss and deduction of 3 points |
| 3 | FDT Art. 33 [3] | deduction from points "current or to be earned", so a total can become negative |

No article number in the project was taken from a secondary source. Rules without a source are recorded as assumptions (Section 3.4).

### 3.3 Requirements summary

| ID | Summary | Source |
|---|---|---|
| FR-01 | Accept a set of teams; names non-blank and unique | — |
| FR-02 | Accept results between registered teams, non-negative goals | SLS Art. 3 |
| FR-03 | At most two matches per pair, at most one per home/away direction | SLS Art. 3(1) |
| FR-04 | 3 / 1 / 0 points | FMT 9/1 |
| FR-05 | Compute P, W, D, L, GF, GA, GD, points | FMT 9/1–2 |
| FR-06 | Forfeit adjustment, used everywhere (stats, head-to-head, mini-table) | FMT 23/4 |
| FR-07 | Deductions subtracted, cumulative, total may be negative | FMT 24/1-a, FDT 33 |
| FR-08 | Order by points descending | FMT 9/2 |
| FR-09 | Two-team tie: h2h points, h2h GD (no away goals), overall GD, overall GF, forfeit | FMT 9/2-a I–V |
| FR-10 | 3+-team tie: mini points, mini GD, mini GF, overall GD, overall GF, forfeit | FMT 9/2-b I–VII |
| FR-11 | In the 3+ procedure the mini-table is **not** rebuilt for a remaining sub-group | FMT 9/2-b I |
| FR-12 | Teams still level after all criteria are flagged "unresolved — requires playoff" | FMT 9/2-a VI, 9/2-b VIII |
| FR-13 | Output: position, FR-05 statistics, points after deduction, unresolved flag | — |
| FR-14 | CLI reads UTF-8, `;`-separated, header-less CSV (teams, matches with optional forfeit field, optional deductions) | — |
| FR-15 | CLI prints aligned columns and marks unresolved teams | — |

| ID | Invalid input that must be rejected with a clear message |
|---|---|
| V-01 | null team list, match list, team, match or deduction |
| V-02 | blank or duplicate team name |
| V-03 | match referencing an unregistered team |
| V-04 | same team on both sides |
| V-05 | negative goal count |
| V-06 | duplicate fixture, or more than two matches between two teams |
| V-07 | deduction of zero or negative points, or for an unregistered team |
| V-08 | malformed CSV line or missing file, reported with file name and line number |

### 3.4 Assumptions

| ID | Assumption | Why |
|---|---|---|
| A-01 | Goal difference = goals for − goals against | FMT does not define it; standard Turkish usage |
| A-02 | Deductions affect only total points; h2h and mini-table points come from match results | Not stated in FMT/FDT |
| A-03 | Same ranking rules during and at the end of the season; teams that have not met have equal mutual-match criteria | FMT defines one system; confirmed by the student |
| A-04 | Only played (or forfeit-awarded) matches are entered; postponed matches are absent | — |

### 3.5 Out of scope

FMT 9/3 (TFF deciding not to apply goal difference); playing the play-off itself (FR-12 only flags it); withdrawn or expelled teams and the fate of their earlier results; relegation and European places, fair-play tables, drawing of lots.

### 3.6 Decisions

| ID | Question | Decision |
|---|---|---|
| D-01 | Are team names case-sensitive? | Exact match after trimming |
| D-02 | How are unresolved teams listed? | Consecutive positions, alphabetical order for display only, flagged |
| D-03 | How is data entered? | CSV files + command-line front end; only played results, no fixture generation for the library input |

Thirteen further ambiguities found while writing the test plan (Q1–Q13, e.g. whether a forfeit automatically deducts 3 points, or whether an empty fifth CSV field means `NONE`) were resolved and recorded in the appendix of `docs/test-plan.md`.

---

## 4. Implementation

### 4.1 Architecture

| Package / class | Responsibility |
|---|---|
| `model.Team`, `MatchResult`, `PointDeduction` (records), `Forfeit` (enum) | Input values with object-level validation |
| `StandingsCalculator` | Public entry point `calculate(teams, matches, deductions)` → `List<StandingRow>`; cross-object validation, statistics, forfeit adjustment, deductions, grouping by points |
| `StandingRow` | One table row (FR-05, FR-13); public getters, package-private mutators |
| `TieBreaker` (package-private) | Orders one group of teams level on points (FR-09..FR-12) |
| `io.CsvLoader`, `io.InputFormatException` | FR-14 parsing; V-08 errors with file name and 1-based line number |
| `cli.Main`, `cli.TableFormatter` | FR-15 output; exit codes 0 (success), 1 (input error), 2 (usage) |
| `generator.*` | `StrengthModel`, `TeamStrength`, `SeasonGenerator`, `GeneratedSeason`, `ScenarioLibrary`; developer tools `ScenarioLibraryBuilder`, `RealismCheck` |

Main source: 18 files, 1,857 lines (including comments). Following a project rule, code and identifiers are in English and comments in Turkish.

### 4.2 Key design decisions

**Immutable input records.** `Team`, `MatchResult` and `PointDeduction` are Java records, so once created they cannot change, and equality is value-based (two `Team("Beşiktaş")` objects are the same team). The compact constructor is the single place for object-level validation:

```java
public record Team(String name) {
    public Team {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Team name must not be blank");
        name = name.trim();   // D-01: trimmed, otherwise exact
    }
}
```

`StandingRow` is deliberately *not* immutable, because statistics are accumulated match by match. Its mutators (`addMatch`, `addDeduction`, `setPosition`, `setUnresolved`) are package-private, so code outside the package sees a read-only row.

**Validation split.** Rules that need only one object (V-01 null fields, V-02 blank name, V-04 same team twice, V-05 negative goals, V-07 non-positive points) live in the record constructors. Rules that need the whole input (V-02 duplicate names, V-03 unknown team, V-06 duplicate fixture, V-07 deduction for an unknown team) live in `StandingsCalculator`. An invalid `MatchResult` therefore cannot even be constructed, and the calculator only checks relations between valid objects.

**Forfeit adjustment in one place.** `MatchResult` stores the score as played on the pitch. FMT 23/4 is applied by two small static methods, which both the statistics code and the mini-table code call:

```java
static int adjustedHomeGoals(MatchResult match) {
    if (match.forfeit() == Forfeit.HOME) return 0;
    if (match.forfeit() == Forfeit.AWAY) return Math.max(3, match.homeGoals());
    return match.homeGoals();
}
```

This removes a typical inconsistency, where the adjusted score is used for the statistics but the raw score leaks into head-to-head. TC-024 and TC-077 test exactly that.

**A single mini-table per group (FR-11).** `TieBreaker.order` is called once for each group of teams level on points. It builds one mini-table from the matches among *all* teams of the group and sorts with a fixed comparator chain (shortened):

```java
Comparator<StandingRow> criteria = Comparator
    .comparingInt((StandingRow r) -> mini.get(r.getTeam()).points).reversed()
    .thenComparing(comparingInt(r -> mini.get(r.getTeam()).goalDifference()).reversed())
    .thenComparing(comparingInt(r -> mini.get(r.getTeam()).goalsFor).reversed())
    .thenComparing(comparingInt(StandingRow::getGoalDifference).reversed())
    .thenComparing(comparingInt(StandingRow::getGoalsFor).reversed())
    .thenComparing(StandingRow::hasForfeitLoss);           // false (no forfeit loss) first
```

The method never recurses into a sub-group, so "no re-look at sub-ties" holds by construction. Teams for which this comparator returns 0 with a neighbour are flagged unresolved (FR-12).

**Why the two-team case needs no separate code.** For a group of two, the mini-table *is* the mutual matches, so 9/2-b's mini points and mini GD are 9/2-a's criteria I and II. The only extra step in 9/2-b is mini goals scored. In the mutual matches of A and B, every goal A scores is conceded by B and vice versa, so GD_A = GF_A − GF_B and GD_B = GF_B − GF_A = −GD_A. If the mini GDs are equal, then GD_A = −GD_A, so GD_A = 0 and GF_A = GF_B. The mini-GF step therefore can never separate two teams that reached it. With two teams the 9/2-b chain gives the same result as the 9/2-a chain, and away goals play no role in either. The same argument explains boundary BV-06 in the test plan (Section 5.3). Both orders are tested separately (TC-050..TC-061 for two teams, TC-065..TC-077 for three or more), and TC-074 checks the switch between them.

**Turkish collation for display.** D-02 lists unresolved teams alphabetically. The first version used `String`'s natural order, which compares UTF-16 code units and puts Ç, İ, Ş and Ü after Z. This was defect DEF-001. The fix uses `Collator.getInstance(Locale.forLanguageTag("tr-TR"))` only for the final display tie-break. The FMT comparator, which decides the unresolved flags, is unchanged.

**CLI testability.** `Main.main` only sets up UTF-8 streams and calls `System.exit` with the result of `run(args, out, err)`. All logic is in `run`, which returns the exit code and writes to the streams it receives, so tests can call it directly.

### 4.3 Data-driven season generator

The student set the requirements: no constructed ties, realistic random seasons, a small fixed library of pre-generated seasons, team strengths from real data (CLAUDE.md, decisions). The design is documented in `docs/generator.md`.

**Strength model.** Final tables of the last ten Süper Lig seasons (2016–17 to 2025–26, 189 rows) were collected from English Wikipedia and checked row by row against Turkish Wikipedia [4], [5]. Every season passed the consistency checks (ΣGF = ΣGA, ΣW = ΣL, full double round robin). Four documented point deductions were found and explained (`docs/data-sources.md`). For each of the 18 clubs of 2026–27, attack = total goals scored ÷ matches played and defence = total goals conceded ÷ matches played, pooled over the club's Süper Lig seasons in the window. Amed SFK and Çorum FK have no Süper Lig season in the window, so they get the average of the 27 promoted-club rows (attack 1.22, defence 1.45). The league mean is μ = 1.407 goals per team per match.

**Match model.** Each side's goals are independent Poisson variables, λ_home = μ·(att_home/μ)·(def_away/μ)·H and λ_away = μ·(att_away/μ)·(def_home/μ)/H. The home advantage H = 1.10 is an **assumption**: the collected tables have no home/away split, so H could not be measured. For equal teams it gives about 55 % of goals to the home side. Poisson sampling uses Knuth's multiplication method with `java.util.Random(seed)`, so the same seed gives the same season. The schedule is a circle-method double round robin (34 rounds, 306 matches). 1 % of matches are forfeited, and each club has a 2 % chance per season of a 3-point deduction. Known simplifications: no score correlation, no draw inflation, fixed strengths.

**Realism check.** In 200 simulated seasons (`RealismCheck`) the simulated goals per match were 2.68 (real window 2.81), the home share was 55.0 %, and the five strongest clubs won all titles except one (Samsunspor once). Points per game were close to the real values for clubs with many seasons. Clubs with only one or two real seasons deviate more (Kocaelispor, Erzurumspor FK). `docs/generator.md` explains these deviations.

**Scenario library S01–S12.** `ScenarioLibraryBuilder` scans seeds from 1 upwards (up to 20,000). It keeps the first unused seed whose season matches each scenario's rule, so the library consists of random seasons filtered for variety, not constructed seasons. The library contains 5 full seasons (S01–S05) and 7 mid-season tables (after rounds 1, 2, 3, 10, 17, 25, 30). It covers: no tie (S01); only two-team ties (S02, S05, S09); 3+-team ties (S03, S04, S06–S08, S10–S12); forfeits and deductions; and four seasons flagged "likely unresolved" (S04, S06, S08, S11).

**Independence from the code under test.** The builder classifies seasons with its own small classifier. This classifier recomputes raw points, re-implements FMT 23/4 in a few lines and detects groups that no criterion can separate. It does **not** call `StandingsCalculator` or `TieBreaker`, so the choice of test data does not depend on the code being tested. The classifier is conservative: in S06 and S08 the calculator flags more teams unresolved than the classifier predicted, and every group the classifier flags is also flagged by the calculator (TC-108).

### 4.4 Build and run

Requirements: Java 21, Maven 3.9 or newer.

```
mvn verify                                   # compile, run all tests, JaCoCo report in target/site/jacoco/
mvn package -DskipTests                      # build the jar only
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar examples/teams.csv examples/matches.csv examples/deductions.csv
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario S04
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario random 42
```

The table has the columns Pos, Team, P, W, D, L, GF, GA, GD, Pts; unresolved teams are marked with `*`. The sample data in `examples/` uses real club names with invented results.

---

## 5. Test Plan and Test Design

The full plan is `docs/test-plan.md` (v1.1). It defines 133 test cases: TC-001..TC-099 (83 hand-designed cases, numbering has gaps), TC-100..TC-134 (35 generator, property-based, metamorphic and CLI scenario cases) and TC-140..TC-154 (15 cases added after the coverage analysis).

### 5.1 Strategy

- **Levels.** Unit (model records), component (`StandingsCalculator`, `CsvLoader`), CLI/system (`Main` end-to-end from CSV files to stdout), and generator/property/metamorphic tests.
- **Black-box first.** Test cases were derived from the requirements and the FMT text. The source code was consulted only for public class and method names. JaCoCo was used only *after* execution, to find untested code.
- **Expected values by hand.** Each expected table is written in the plan with its arithmetic (e.g. "A 6 (GF3 GA2 GD+1), B 6 (GF5 GA2 GD+3) …"), so it can be checked line by line and defended orally.
- **Discriminating data.** Every tie-break case is built so that applying the wrong criterion, or the right criteria in the wrong order, gives a *different* table. A typical trick: the team that must win the tie has the worse overall goal difference, or comes later in the alphabet.
- **Independent oracle check.** According to `docs/ai-usage-log.md` (phase 3), the hand-computed expected values were also checked with a separate rule calculator written in Python. That script is not part of the repository, so this check cannot be repeated from the submitted files. `[STUDENT: confirm or remove this sentence.]`
- **Exit criteria.** Every TC executed; every FR, V-rule and decision-table rule has at least one executed case; every failure logged; full suite green after the last fix; uncovered branches explained.

### 5.2 Equivalence partitioning

The plan defines 59 partitions (EP-01..EP-59) for team names, goals, match participants, forfeit, deductions, tie situations, fixtures per pair, CSV line shapes and CLI arguments. Examples:

| Partition | Type | Example | TC |
|---|---|---|---|
| EP-02 names with Turkish letters (Ç Ğ İ I Ö Ş Ü ı) | valid | `İstanbul Başakşehir` | TC-001, TC-080 |
| EP-09 names differing only in case | valid, distinct teams (D-01) | `Beşiktaş` / `beşiktaş` | TC-043 |
| EP-34 / EP-35 / EP-36 two / three / four or more teams level | valid | — | TC-050.., TC-065.., TC-076 |
| EP-39 mutual matches between two tied teams: 0 / 1 / 2 | valid | — | TC-060 / TC-051 / TC-050 |
| EP-52 matches line with empty fifth field `A;B;1;0;` | ambiguous → valid (Q9) | — | TC-087 |

**Justification.** EP keeps the number of cases small while each class the rules treat differently gets a representative. EP-02 was chosen deliberately because the library targets Turkish club names. It produced TC-078 and TC-079, which exposed DEF-001. EP-52 came from listing every shape of the optional forfeit field and produced TC-087, which exposed DEF-002.

### 5.3 Boundary value analysis

The plan has 16 boundaries (BV-01..BV-16). Examples:

| ID | Variable | Values | Why |
|---|---|---|---|
| BV-01 | goal count | −1 / 0 / 1 | 0 is the smallest legal value; catches `> 0` vs `>= 0` |
| BV-02 | forfeit winner's actual goals | 2 / 3 / 4 (and 0, 5) | the rule is max(3, g); behaviour changes between 3 and 4 |
| BV-03 | points difference | 0 / 1 | 1 point must beat every tie criterion (TC-014: a team with better GD but 1 point less ranks lower) |
| BV-08 | deduction points | −1 / 0 / 1 | V-07 "zero or negative" |
| BV-09 | points after deduction | 1 / 0 / −1 | negative totals allowed (FDT 33); clamping at 0 is a likely fault |
| BV-10 | tied group size | 2 → 3 | algorithm switch (TC-073 vs TC-074) |
| BV-13 | CSV field count (matches) | 3 / 4 / 5 / 6 | exactly 4 or 5 allowed |

**The BV-06 insight.** For the head-to-head goal difference of two teams, the naive boundary would be a difference of 0 versus 1. But the mutual GDs of two teams are always opposite numbers (GD_B = −GD_A, as shown in Section 4.2), so their difference GD_A − GD_B = 2·GD_A is always even. A difference of 1 is impossible, and the real boundary is 0 versus 2, which is a one-goal margin (TC-052, TC-053). Similarly, the possible h2h point pairs (3-3, 1-1, 2-2, 4-1, 3-0, 6-0) make 3 the smallest non-zero difference (BV-07). BVA here required analysing the domain, not just taking "value ± 1".

### 5.4 Decision tables

Tie-breaking is a chain of conditions, which is what decision tables are designed for. The plan has four tables.

**(a) Two teams, FMT 9/2-a**: rules R1–R7 plus R2'. Each rule has its deciding criterion and a test (TC-050..TC-059). R2' checks that away goals do **not** decide: in TC-053 both teams have h2h goal difference 0, but only B scored an away goal. The correct result (A first, by overall GD) is the opposite of what an away-goals rule would give.

**(b) Three or more teams, FMT 9/2-b**: rules M1–M9 (TC-065..TC-073). M8 and M9 contain the condition "re-applying the two-team rule to the remaining pair would change the result", which tests FR-11 directly.

**Key FR-11 case, TC-073.** Matches: A–B 1-0, C–A 2-1, B–C 2-1, A–D 1-0, B–D 3-0, C–D 1-0. A, B and C have 6 points each. In the mini-table all three have 3 points and goal difference 0; mini goals scored are A 2, B 2, C 3, so C is first. A and B are still level. FMT 9/2-b says their matches are not looked at again, so the next criterion is overall GD: B +3 > A +1. Expected order: **C, B, A, D**. A "recursive" implementation that rebuilds a two-team table for A and B sees A's 1-0 win and gives C, A, B. An implementation that skips mini goals scored gives B, C, A. One case thus separates three plausible implementations. TC-074 changes one result (C–D 1-1), which leaves only A and B level, so the two-team rule applies and the order becomes A, B, C, D.

**(c) Forfeit adjustment, FMT 23/4**: rules F1–F6 by forfeiting side, winner's goals ≤ 3 or > 3, and the forfeiter leading on the pitch (e.g. 5-2 with the home side forfeiting → 0-3).

**(d) Deduction interaction**: D1–D5. The key case is TC-034. A deduction of 6 creates a tie. If the deduction wrongly leaked into the head-to-head points, A would have −2 h2h points and drop below B; by A-02 the expected result is A above B.

### 5.5 Negative testing

Each validation rule V-01..V-08 has negative cases (plan Sec. 5): null lists and null elements, blank and duplicate names (including duplicates after trimming), unregistered home or away team, `A–A` and `A–" A "`, goals −1, the same fixture twice, a third match, deductions 0 and −1, wrong CSV field counts, non-integer numbers, unknown forfeit values (`XYZ`, and lowercase `home`), missing files, and 0/1/4 CLI arguments. The tests use `assertThrows` and check that the message names the offending value, file or line, because V-rules require "a clear message". DEF-002 was found on the CSV side, in the opposite direction: a *valid* line was rejected.

### 5.6 Regression testing

The plan required the **full** suite to be re-run after every fix, not only the failing class, because tie-breaking changes easily break unrelated orderings. Every defect keeps its detecting test in the suite as a regression test. Failing tests were committed red before the fix (commit `f8e8d6b`), and no test was changed to make it pass. Property-based tests use fixed seeds, so a regression is reproducible.

### 5.7 Property-based and metamorphic testing

Full seasons have no hand-computed expected table, so three other oracles were used (plan Sec. 6):

- **Invariants (TC-100..TC-103)** over the 12 library seasons and 50 seeded random seasons (seeds 1..50; every third one a mid-season table). Conservation (ΣW = ΣL, ΣGF = ΣGA, Σplayed = 2·matches). Per row, played = W + D + L and points = 3W + D − deducted, compared with a **test-side oracle** that recomputes every row from the raw match list, applying FMT 23/4 itself. Positions 1..n with non-increasing points. Unresolved teams appear only in groups of at least two with equal points.
- **Metamorphic relations (TC-104..TC-107, TC-129)** over the 12 library seasons and seeds 1..20. Shuffling the input lists leaves the table unchanged. Consistent renaming changes only names. Mirroring every match (swap home/away and HOME↔AWAY forfeit) leaves the table unchanged, because no criterion depends on venue. Adding a deduction of d lowers only that team's points, by exactly d. Removing one round lowers every team's played count by 1. TC-107 deliberately does **not** assert that the other teams keep their relative order, because a new 3-team tie created by the deduction can legally reorder them; FMT 9/2-b is not monotonic.
- **Generator correctness (TC-111..TC-128)**, because the generator is first-version code too: determinism per seed, schedule structure (306 matches, every ordered pair once, second half mirrors the first), mid-season prefix property, round-count boundaries (0, 1, 34, 35), forfeit and deduction rates over 200 seasons, Poisson mean and variance over 200,000 draws, home goal share within 53–57 %, strength values recomputed independently from the CSV, and library files that match a fresh regeneration from the catalogue seeds.
- **Catalogue flags vs calculator (TC-108..TC-110)**: the independent classifier's flags must agree with the calculator's result.

### 5.8 Traceability and distribution

The plan's traceability matrix (Sec. 9) maps every FR, V-rule, assumption and decision to test cases. Every decision-table rule has at least one case. By primary technique: EP 30, BVA 9, decision table 25, negative 19, property/metamorphic/generator 35 (118 cases), plus 15 white-box coverage-gap cases (133). Many cases carry a secondary technique; for example TC-050/051/054/055 are also BVA rows.

---

## 6. Test Implementation and Results

### 6.1 JUnit structure

All tests are in `src/test/java/edu/svv/standings/` (21 files, 2,960 lines). There are 153 test methods (119 `@Test`, 34 `@ParameterizedTest`). Every method's `@DisplayName` starts with its TC number. Small helpers keep fixtures identical to the plan's notation. `TieFixtures.m("A","B",2,1)` creates a match. `TieFixtures.assertTable(table, "C", "B!", …)` checks the full order, the unresolved flag (`!`) and consecutive positions in one assertion. `GeneratorTestSupport` provides the season sources and the test-side oracle. Example (TC-073):

```java
@Test
@DisplayName("TC-073 FR-11: no re-look at A vs B, overall GD decides")
void tc073() {
    assertTable(calculate(teams("A", "B", "C", "D"),
                    m("A", "B", 1, 0), m("C", "A", 2, 1), m("B", "C", 2, 1),
                    m("A", "D", 1, 0), m("B", "D", 3, 0), m("C", "D", 1, 0)),
            "C", "B", "A", "D");
}
```

Test invocations per class in the final run (`docs/results/run-03-full-suite`):

| Test class | Invocations | TCs |
|---|---|---|
| `model.TeamTest` / `MatchResultTest` / `PointDeductionTest` | 10 / 6 / 4 | TC-001..TC-009 |
| `StandingsCalculatorTest` (`Stats` 7, `ForfeitAdjustment` 10, `Deductions` 6, `Validation` 14) | 37 | TC-010..TC-048 |
| `TieBreakTwoTeamTest` (5 nested classes) | 12 | TC-050..TC-061 |
| `TieBreakMultiTeamTest` (5 nested classes) | 15 | TC-065..TC-079 |
| `io.CsvLoaderTest` | 29 | TC-080..TC-090 |
| `cli.TableFormatterTest` / `cli.MainTest` | 5 / 7 | TC-091..TC-099 |
| `generator.PropertyBasedStandingsTest` | 248 | TC-100..TC-103 |
| `generator.MetamorphicStandingsTest` | 167 | TC-104..TC-107, TC-129 |
| `generator.CatalogueFlagsTest` | 17 | TC-108..TC-110 |
| `generator.SeasonGeneratorTest` / `StrengthModelTest` | 33 / 8 | TC-111..TC-123 |
| `generator.ScenarioLibraryTest` | 34 | TC-124..TC-128 |
| `cli.MainScenarioTest` | 8 | TC-130..TC-134 |
| `cli.` / `io.` / `generator.CoverageGapTest` | 4 / 4 / 7 | TC-140..TC-154 |
| **Total** | **655** | |

### 6.2 Execution history

| Run | Commit / state | Invocations | Result | Stored in |
|---|---|---|---|---|
| run-01 | `f8e8d6b` (hand-designed tests, first code version) | 125 | 122 passed, 2 failures, 1 error | `docs/results/run-01-initial/` |
| run-02 | after `37b9388` and `e03aea7` (fixes) | 125 | 125 passed | `docs/results/run-02-after-fixes/` |
| run-03 | `f5a0ad2` (+ generator, property, metamorphic, coverage-gap tests) | 655 | 655 passed | `docs/results/run-03-full-suite/` |

The generator-related tests added 515 invocations (640 in total), and all passed on their first run. The 15 coverage-gap invocations brought the total to 655.

### 6.3 Example of a failing and a successful execution

Run-01 summary (verbatim from `docs/results/run-01-initial/mvn-test-output.txt`):

```
[ERROR]   TieBreakMultiTeamTest$TurkishAlphabeticalOrder.tc079:191 sıra / çözülemedi bayrağı ==> expected: <[Çaykur Rizespor!, Denizlispor!]> but was: <[Denizlispor!, Çaykur Rizespor!]>
[ERROR] Errors: 
[ERROR]   CsvLoaderTest.tc087_emptyFifthFieldIsNone:216 » InputFormat matches.csv, line 1: unknown forfeit value '' (expected NONE, HOME or AWAY)
[ERROR] Tests run: 125, Failures: 2, Errors: 1, Skipped: 0
[ERROR] BUILD FAILURE
```

TC-079 is an assertion **failure**: the code returned a table, but the display order was wrong (the message `sıra / çözülemedi bayrağı` means "order / unresolved flag"). TC-087 is an **error**: the code threw an unexpected `InputFormatException` on valid input. TC-078 failed with the same cause as TC-079, on 11 team names.

Successful final run (verbatim from `docs/results/run-03-full-suite/mvn-test-output.txt`, excerpt):

```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 s -- in edu.svv.standings.TieBreakMultiTeamTest$NoReLookAtSubTies
[INFO] Tests run: 248, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.111 s -- in edu.svv.standings.generator.PropertyBasedStandingsTest
[INFO] Tests run: 655, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 6.4 Process evidence

The commit history (`git log --oneline`) records the process in order: Maven skeleton (`2df6762`) → requirements v1.0 (`fe6224d`) → model records (`79c2fad`) → requirements v1.1 (`c3378bb`) → calculator (`6973e90`) → tie-breaking (`aff5653`) → CSV/CLI (`e2ef8f4`) → test plan (`a86b88b`) → JUnit tests, 3 failing (`f8e8d6b`) → fix DEF-001 (`37b9388`) → fix DEF-002 (`e03aea7`) → defect log update (`e30e035`) → real data (`5618900`) → research draft (`3071a5f`) → generator (`e86e3fd`) → generator and property tests (`f1cccec`) → coverage analysis (`f5a0ad2`) → actual results recorded in the test plan (`fddf52a`).

---

## 7. Code Coverage and Defect Analysis

### 7.1 Coverage results

JaCoCo 0.8.13, `mvn clean verify`. The report is in `docs/results/coverage/` (`index.html`, `jacoco.csv`). The totals below were recomputed from `jacoco.csv` and agree with `docs/coverage-analysis.md`.

| Metric | Before gap tests (640 inv.) | Final (655 inv.) | Final, tools excluded¹ |
|---|---|---|---|
| Instruction | 62.7 % (2985/4760) | 66.3 % (3155/4760) | 96.7 % (3155/3262) |
| Branch | 68.1 % (258/379) | 71.0 % (269/379) | 97.5 % (269/276) |
| Line | 67.7 % (550/813) | 71.7 % (583/813) | 95.4 % (583/611) |
| Method | 82.9 % (102/123) | 87.8 % (108/123) | 98.2 % (108/110) |

¹ Excluding `ScenarioLibraryBuilder` and `RealismCheck`, two developer tools with a `main` method that are run by hand and never by a test (1,498 instructions and 103 branches between them).

| Class (final) | Instruction | Branch | Line |
|---|---|---|---|
| `model.*` (Team, MatchResult, PointDeduction, Forfeit) | 100 % | 100 % | 100 % |
| `StandingRow` | 100 % (149/149) | 100 % (8/8) | 100 % (35/35) |
| `StandingsCalculator` | 98 % (380/386) | 98 % (53/54) | 99 % (75/76) |
| `TieBreaker` | 100 % (254/254) | 100 % (18/18) | 100 % (45/45) |
| `io.CsvLoader` | 98 % (469/479) | 100 % (30/30) | 98 % (85/87) |
| `cli.TableFormatter` | 100 % (279/279) | 100 % (18/18) | 100 % (41/41) |
| `cli.Main` | 87 % (226/259) | 100 % (30/30) | 86 % (54/63) |
| `generator.SeasonGenerator` | 100 % (446/446) | 100 % (48/48) | 100 % (79/79) |
| `generator.StrengthModel` | 97 % (455/467) | 97 % (33/34) | 96 % (78/81) |
| `generator.ScenarioLibrary` | 83 % (232/278) | 64 % (9/14) | 77 % (43/56) |

**How coverage was used.** After the black-box suite had run, the JaCoCo report was examined class by class. It pointed to real gaps: the CLI's handling of calculator errors (V-02, V-06 reaching `Main`), I/O failure paths in `CsvLoader` (invalid UTF-8, a directory passed as a file, a reader failing on `read` or `close`), and `GeneratedSeason.writeCsv`, a method no test executed. Fifteen white-box tests (TC-140..TC-154), each tied to a requirement, closed these gaps. All passed, and no production code was changed.

### 7.2 Remaining gaps and why

| Location | Missed | Reason |
|---|---|---|
| `StandingsCalculator` l. 109–110, 1 branch | V-06 "more than two matches" | **Unreachable.** The duplicate-direction check runs first. Between two teams only two directions exist, so a third match always repeats one and is rejected as "Duplicate fixture" before the count check. Kept because the specification states the rule in both forms. |
| `CsvLoader` l. 69–70 | catch around `new Team(...)` | Unreachable defensive code: blank lines are skipped before parsing, so the constructor cannot throw. |
| `Main.main` (l. 36–41), implicit constructor | `System.exit` wrapper | A test would terminate the JVM; all logic is in `run`, which has 100 % branch coverage. |
| `Main` l. 125–128, `ScenarioLibrary` and `StrengthModel` resource paths | missing or corrupt packaged resource, `IOException` on classpath streams | Would need a broken jar or class loader; the real resources are verified by TC-120..TC-126. |
| `ScenarioLibrary.parseCatalogue` (4 branches) | blank / malformed / empty catalogue | Private; reads only the packaged, well-formed `catalogue.csv`. |
| `ScenarioLibraryBuilder`, `RealismCheck` | whole classes | Developer tools. Their output is checked indirectly: TC-126 regenerates each scenario and compares it with the stored files; TC-117..TC-119 assert the statistics `RealismCheck` prints. |

Writing artificial tests (broken class loaders, reflection, intercepting `System.exit`) to reach these lines would add maintenance cost and no confidence. Documenting the reason seemed more honest.

### 7.3 Defects

Both defects are recorded in full in `docs/defect-log.md`.

**DEF-001: unresolved teams displayed in Unicode code-point order instead of Turkish alphabetical order.**

- *Detected by:* TC-078 and TC-079 (EP, class "names with Turkish letters"), run-01.
- *Severity / priority:* Medium / P2. Table content and unresolved flags were right, but the D-02 display order was wrong for any club starting with Ç, İ, Ş or Ü.
- *Root cause:* `TieBreaker.order` used `criteria.thenComparing(r -> r.getTeam().name())`. Without a comparator, `thenComparing` uses `String.compareTo`, which compares UTF-16 code units, and Ç (U+00C7), İ (U+0130), Ş (U+015E) and Ü (U+00DC) all lie above `Z`. The actual order in TC-078 (ASCII names first, then Ç, Ü, İ, Ş) is exactly code-point order, which confirmed the hypothesis before the code was changed.
- *Fix:* commit `37b9388`; the name key now uses `Collator.getInstance(Locale.forLanguageTag("tr-TR"))`. Only `TieBreaker.java` changed.
- *Regression:* run-02, TC-078/079 pass, 125/125.
- *Note:* this defect had been suspected during review of the first code version and written down in the project notes before any test ran. The tests **confirmed** it rather than discovering it.

**DEF-002: empty optional fifth field in a matches line rejected instead of defaulting to `NONE`.**

- *Detected by:* TC-087 (EP/BVA on the optional forfeit field, EP-52), run-01.
- *Severity / priority:* Medium / P2. A valid file was rejected, so no table was produced. Spreadsheet exports often write a trailing empty column.
- *Root cause:* `CsvLoader` decided "forfeit given" only by field count. `split` keeps trailing empty fields, so `A;B;1;0;` had 5 fields and `Forfeit.valueOf("")` threw.
- *Fix:* commit `e03aea7`; `parseForfeit` returns `NONE` for an empty (trimmed) field. Lowercase `home` is still rejected (TC-086 still passes).
- *Regression:* run-02, 125/125.
- *Note:* this defect was **not** suspected beforehand. It was found only because the test plan listed the empty field as an ambiguity (Q9) and fixed the expected behaviour before testing.

Three failing invocations, two defects. No test was changed to make it pass. The defect log also records three observations that are not defects: the unreachable V-06 branch, FR-12 flagging the whole table at season start, and `TieBreaker` setting the flag on the rows it receives (safe because the calculator creates fresh rows on every call).

### 7.4 Discussion: how testing found or prevented errors

**What the defects show.** Neither defect would have been found by coverage. The faulty sort statement of DEF-001 was executed by every table-building test, with ASCII names. Only the deliberately chosen partition "Turkish letters" made it fail. DEF-002 lay on a path for which the code had a perfectly reasonable-looking check; only writing down the precise expected behaviour of an edge case (Q9) exposed it. In both cases, *thinking about the input domain* (EP, BVA, resolving ambiguities) mattered more than running many tests.

**Prevention.** Some of the value of the test design came before any test ran. Writing the decision tables forced questions that are not in the specification: Does a forfeit automatically deduct 3 points (Q1)? Is a null deductions list valid (Q2)? Is the whole table "unresolved" at season start (Q12)? Answering them early prevented implementation guesses from becoming silent behaviour. The two-team/3+-team analysis (Section 4.2) also led to a simpler design without a separate two-team code path.

**Only two defects, and the honesty this requires.** Only two defects were found, both of medium severity and both outside the core ranking rules. All tie-break cases (TC-050..TC-077) passed on the first run, and so did all 515 generator, property and metamorphic invocations. This **does not prove** that the core is correct. Possible explanations, which cannot be told apart from the results alone:

1. The tie-break logic is correct for the cases tested. The hand cases were built to discriminate between plausible wrong implementations (e.g. TC-073), and a correct result there is real evidence for the specific behaviours tested.
2. The tests and the code share a misreading of the rules. The same person (with the same AI assistance; Section 9) wrote both the specification and the tests. A misinterpretation of FMT 9/2 in `requirements.md` would be built into both, and no test would fail.
3. The property and metamorphic oracles are weaker than they look. The test-side oracle recomputes statistics and points, but **not** the tie-break order. Shuffling, renaming and mirroring would not detect, for example, a wrong order of criteria, because such an error is itself independent of input order and venue. These tests protect the infrastructure (statistics, forfeit adjustment, determinism, generator), not the FMT ordering.

A test suite that never fails also has unknown fault-detection power. Mutation testing (Section 8.3) would be the standard way to measure it.

---

## 8. Discussion and Conclusion

### 8.1 Effectiveness of the approach

The combination worked as intended for the parts that can be specified exactly. The FMT rules were turned into a requirements table with sources. Every rule was turned into equivalence classes, boundaries and decision-table rules, with hand-computed expected values. Each test is traceable to a requirement and to a reason. The red-then-green commit history makes the defect cycle visible. Property-based and metamorphic testing added large, realistic inputs (18 teams, up to 306 matches, forfeits and deductions) without anyone writing an expected table. The generator and its own tests ensure that these inputs are reproducible and plausible. Coverage was used for its proper purpose: it found three groups of untested, requirement-relevant code paths, and the remaining gaps could each be justified.

### 8.2 Limitations

- **One author for specification and tests.** Requirements, test plan, tests and code all come from one student working with one AI tool. There was no independent review, so shared misinterpretations of the rules (Section 7.4, point 2) would go unnoticed.
- **Generator realism is bounded by the strength model.** Scenarios are only as realistic as the model: independent Poisson goals, fixed strengths, an assumed home advantage H = 1.10, and promoted-club averages for two clubs. Ties in the library come from this model, so rarer real-world patterns may be under-represented.
- **Wikipedia as data source.** The strength data come from English and Turkish Wikipedia, cross-checked against each other but not against tff.org. The 2022–23 rows of Gaziantep FK and Hatayspor include matches after their withdrawal and were kept as published.
- **Unverified assumptions.** A-01..A-04 (goal difference definition, deductions not affecting h2h points, same rules during the season, only played matches) are not stated in the FMT or FDT. The tests verify the implementation *against* these assumptions; they cannot validate the assumptions themselves.
- **FR-12 at season start.** The literal reading flags every team as "unresolved" when no matches have been played (TC-015, TC-078). This is consistent with the rules, but a user may find it surprising. It was accepted, not validated with a real user.
- **Message checks by substring.** Several negative tests check error messages by substring and will break if the wording changes.
- **AI assistance.** Large parts of the code, plan and tests were drafted by an AI tool under the student's direction (Section 9). This speeds up the work, but it increases the risk of point 2 above and requires the student to review everything personally.

### 8.3 Possible improvements

1. **Mutation testing**, for example with PIT, to measure how many seeded faults (changed comparisons, swapped criteria, removed `reversed()`) the suite kills. This would answer "how strong are the tests?", which coverage cannot.
2. **Real full-season results as an acceptance test.** Collect the match-level results of real seasons (e.g. 2024–25 with the Alanyaspor–Kayserispor–Gaziantep FK tie) from an official source and compare the engine's table with the official final table. That would be *validation* against reality, not only verification against the specification.
3. **A tie-break oracle for generated seasons.** A second, independently written implementation of FMT 9/2 (differential testing) would let the property tests check the order too, not only the statistics.
4. **Continuous integration.** Run `mvn verify` automatically on every push (e.g. GitHub Actions) so that regressions are caught without relying on manual runs.
5. **Independent review** of the requirements by someone familiar with the TFF instructions.

### 8.4 Conclusion

The project implements the FMT Art. 9 tie-break rules, including the easily misunderstood "no re-look at sub-ties" rule, and verifies them with a planned, traceable JUnit 5 suite of 655 invocations. Testing found two medium-severity defects, both outside the core ranking logic, and the suite now passes completely, with 71.0 % branch coverage overall and 97.5 % when the two offline developer tools are excluded. The results give reasonable confidence in the behaviours that were explicitly designed into test cases. They give less confidence about the interpretation of the rules themselves, and the strength of the test suite has not yet been measured. The most valuable next steps are mutation testing and an acceptance test against real, officially published results.

---

## 9. AI Tool Usage Statement

`[STUDENT: review and adapt]`

This project was developed with Claude Code (model Claude Opus 5.5) as an AI assistant. Each step is logged in `docs/ai-usage-log.md`, and the commit history shows the order of the work.

**How the tool was used.** The main Claude Code session acted as an orchestrator. In the later phases, while I was away, it delegated tasks to sub-agents and then reviewed, compiled, ran and committed their results. The project rules (CLAUDE.md) said that sub-agents must not fix defects they noticed in the core code but report them, so that defects would be found by tests and logged. Failing tests were committed before the fix.

**What was AI-generated.** Drafts of: the requirements document; the first version of all production code (model, calculator, tie-breaker, CSV loader, CLI, generator); the test plan (95 test cases in the first version, 133 in the final version); all JUnit tests; the root-cause analysis and fixes for DEF-001 and DEF-002; the coverage analysis and the 15 coverage-gap tests; the collection and checking of the Wikipedia data; the research section draft; and this report draft. According to the log, a sub-agent also checked the hand-computed expected values with a separate Python rule calculator.

**What were my own decisions and corrections.** I chose the topic (league standings rather than the alternative topic) and found the 2026–27 Süper Lig statute myself. I provided the rule knowledge that the requirements depend on: head-to-head goal difference is the sum of both matches, a forfeit is 3-0, deducted points count, and the same ranking rule applies during the season. I supplied a rule the AI could not find in the text (the yellow-card counter resets after a suspension, relevant to the rejected alternative topic) and challenged a claim it had taken from a news site that turned out not to be in the TFF text. I approved the requirements and decisions D-01 and D-02, chose CSV with a command-line front end, chose a public repository, and decided that failing tests are committed red. I set the conditions for the generator: no constructed ties, 10–12 pre-generated realistic seasons, strengths from the last ten seasons, and promoted-club averages. The test-plan ambiguities Q1–Q13 were resolved by the orchestrator in autonomous mode and recorded so that I can revisit them.

**My responsibility.** I have reviewed the requirements, the test plan, the defect analysis and this report, and I am able to explain each test case, expected value and design decision. `[STUDENT: state honestly what you reviewed in detail, what you changed, and adapt this section to the course's AI policy.]`

---

## 10. References

[1] Türkiye Futbol Federasyonu, *2026–2027 Sezonu Süper Lig Müsabakaları Statüsü*. [Online]. Available: https://www.tff.org/Resources/TFF/Documents/STATULER/2026-2027/2026-2027-sezonu-super-lig-musabakalari-statusu.pdf (accessed Nov. 12, 2026).

[2] Türkiye Futbol Federasyonu, *Futbol Müsabaka Talimatı* (PDF dated Aug. 18, 2025). [Online]. Available: https://www.tff.org/Resources/TFF/Documents/TALIMATLAR/Futbol-Musabaka-Talimati.pdf (accessed Nov. 12, 2026).

[3] Türkiye Futbol Federasyonu, *Futbol Disiplin Talimatı*. [Online]. Available: https://www.tff.org/Resources/TFF/Documents/TALIMATLAR/Futbol-Disiplin-Talimati.pdf (accessed Nov. 12, 2026).

[4] Wikipedia contributors, "2016–17 Süper Lig" through "2025–26 Süper Lig," and "2026–27 Süper Lig," *Wikipedia, The Free Encyclopedia* (English). [Online]. Available: https://en.wikipedia.org/wiki/2024%E2%80%9325_S%C3%BCper_Lig (other seasons by the same pattern; full list in `docs/data-sources.md`) (accessed Nov. 12, 2026).

[5] Vikipedi katılımcıları, "2016-17 Süper Lig" through "2026-27 Süper Lig," *Vikipedi, Özgür Ansiklopedi* (Turkish). [Online]. Available: https://tr.wikipedia.org/wiki/2024-25_S%C3%BCper_Lig (other seasons by the same pattern) (accessed Nov. 12, 2026).

[6] International Software Testing Qualifications Board (ISTQB), *Certified Tester Foundation Level Syllabus*, v4.0.1, Sep. 15, 2024. [Online]. Available: https://www.istqb.org/wp-content/uploads/2024/11/ISTQB_CTFL_Syllabus_v4.0.1.pdf (accessed Nov. 12, 2026).

[7] S. Bechtold, S. Brannen, et al., *JUnit 5 User Guide*, version 5.11.4. [Online]. Available: https://docs.junit.org/5.11.4/user-guide/ (accessed Nov. 12, 2026).

[8] The Apache Software Foundation, "Using JUnit 5 Platform," *Maven Surefire Plugin documentation*. [Online]. Available: https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html (accessed Nov. 12, 2026).

[9] G. J. Myers, C. Sandler, and T. Badgett, *The Art of Software Testing*, 3rd ed. Hoboken, NJ, USA: Wiley, 2011, ISBN 978-1-118-03196-4.

[10] K. Claessen and J. Hughes, "QuickCheck: A lightweight tool for random testing of Haskell programs," in *Proc. 5th ACM SIGPLAN Int. Conf. Functional Programming (ICFP '00)*, 2000, pp. 268–279, doi: 10.1145/351240.351266.

[11] T. Y. Chen, F.-C. Kuo, H. Liu, P.-L. Poon, D. Towey, T. H. Tse, and Z. Q. Zhou, "Metamorphic testing: A review of challenges and opportunities," *ACM Comput. Surv.*, vol. 51, no. 1, Art. 4, 2018, doi: 10.1145/3143561.

[12] JaCoCo Project, "Coverage Counters," *JaCoCo Java Code Coverage Library documentation*. [Online]. Available: https://www.jacoco.org/jacoco/trunk/doc/counters.html (accessed Nov. 12, 2026).

[13] L. Inozemtseva and R. Holmes, "Coverage is not strongly correlated with test suite effectiveness," in *Proc. 36th Int. Conf. Software Engineering (ICSE '14)*, Hyderabad, India, 2014, pp. 435–445, doi: 10.1145/2568225.2568271.

---

## Appendix A. Supporting Materials

All paths are relative to the repository root.

| Material | Location |
|---|---|
| Build file (Java 21, JUnit 5.11.4, Surefire 3.5.2, JaCoCo 0.8.13) | `pom.xml` |
| Usage instructions | `README.md` |
| Requirements specification v1.1 | `docs/requirements.md` |
| Test plan v1.1 (EP, BVA, decision tables, negative, property/metamorphic, traceability, Q1–Q13, actual results) | `docs/test-plan.md` |
| Defect log (DEF-001, DEF-002, observations) | `docs/defect-log.md` |
| Coverage analysis | `docs/coverage-analysis.md` |
| Generator design and realism check | `docs/generator.md` |
| Data sources and checks | `docs/data-sources.md` |
| AI usage log | `docs/ai-usage-log.md` |
| Production source | `src/main/java/edu/svv/standings/` (`model/`, `io/`, `cli/`, `generator/`) |
| Strength data (189 rows) and 2026–27 club list | `src/main/resources/strength/` |
| Scenario library S01–S12 and catalogue | `src/main/resources/scenarios/` |
| JUnit tests | `src/test/java/edu/svv/standings/` |
| Sample CLI input (invented results) | `examples/` |
| Test run 1, initial (125 invocations, 3 failing) | `docs/results/run-01-initial/` |
| Test run 2, after fixes (125/125) | `docs/results/run-02-after-fixes/` |
| Test run 3, full suite (655/655) | `docs/results/run-03-full-suite/` |
| JaCoCo report (HTML, CSV, XML) | `docs/results/coverage/` (`index.html`, `jacoco.csv`, `jacoco.xml`) |
| Process history | `git log --oneline` |