# Research Component (draft)

> Draft for the SVV report. Project: TFF Standings Engine (Java 21, Maven, JUnit 5, JaCoCo). Section 6 is a placeholder for the student. Project-specific statements refer to `docs/test-plan.md`, `docs/defect-log.md` and `src/test/java`.

## 1. The role of software testing in software quality assurance

Software testing and quality assurance (QA) are related but not identical. The ISTQB Foundation Level syllabus describes testing as a product-oriented activity that supports reaching appropriate quality levels and as one major form of quality control. It describes QA as process-oriented and preventive: if a good process is followed correctly, it should generate a good product [3, Sec. 1.2.2]. Test results serve both purposes. In testing they are used to fix defects. In QA they show how well the development and test processes perform [3, Sec. 1.2.2].

Testing cannot show that software is correct. The syllabus lists exhaustive testing as impossible, so test techniques and risk-based prioritisation are used to focus effort. It also lists the "absence-of-defects fallacy": verifying all specified requirements and fixing all defects found can still produce a system that does not meet users' needs, which is why validation is needed in addition to verification [3, Sec. 1.3]. This is the reason for the "V&V" in the course name. Verification asks whether the product was built right against the specification. Validation asks whether it is the right product.

In this project the specification is `docs/requirements.md` (FR-01..FR-15, validation rules V-01..V-08), derived from the TFF Futbol Müsabaka Talimatı. The process followed the sequence that QA recommends: requirements, first code version, test plan, tests, defect log, fix, regression run. The test plan states that test cases are derived black-box from the requirements first, and that coverage data is used only afterwards to find untested code (test plan, Sec. 1.2). Testing therefore acts as quality control on the product, and the traceability matrix plus the defect log act as process evidence. Testing found two real defects (DEF-001, DEF-002); DEF-001 had been suspected during code review, DEF-002 had not (defect log, Notes).

## 2. Purpose and benefits of automated unit testing

A unit test executes a small piece of code, such as a class or method, and compares the observed result with an expected result written beforehand. Automating this means the comparison is done by a tool and the suite can be rerun at any time. The ISTQB syllabus notes that automated regression testing is one case where repeating the same tests remains beneficial [3, Sec. 1.3, principle 5], and it requires confirmation and regression testing after every change that fixes a defect or adds a feature [3, Sec. 2.2.3].

The benefits I can support from this project are modest and concrete:

- **Repeatability.** The suite of 125 test invocations runs with one Maven command. The regression run after the fixes (`docs/results/run-02-after-fixes`) used the same command as the detection run, which makes the before/after comparison fair.
- **Fast feedback on a fix.** After DEF-001 and DEF-002 were fixed, one command showed that the previously failing tests passed and that nothing else broke (125/125).
- **Executable documentation.** Each test carries an identifier and a rationale from the test plan (for example "TC-020 away forfeit: winner gets max(3, goals)"), so the test states a requirement.
- **Early, localised failure.** Unit-level tests of `Team`, `MatchResult` and `PointDeduction` fail at the faulty class instead of showing up as a wrong table later.

There are costs. Tests must be maintained, a wrong expected value gives a false alarm, and automated checks only detect what their authors thought to check [3, Sec. 1.3]. In this project expected values were computed by hand in the test plan so that the oracle is independent of the code under test (test plan, Sec. 1.2).

## 3. JUnit 5: main features and how they are used in this project

### 3.1 Architecture

The JUnit 5 User Guide describes JUnit 5 as three parts [1]. The **Platform** is the foundation for launching test frameworks on the JVM and defines the `TestEngine` API. **Jupiter** is the programming model and extension model for writing tests. **Vintage** runs JUnit 3 and 4 tests on the Platform. This project depends only on `junit-jupiter`, managed through the `junit-bom` at version 5.11.4 in `pom.xml`, so Vintage is not needed (there are no JUnit 4 tests).

### 3.2 Maven Surefire integration

The Maven Surefire plugin selects a test engine from the test dependencies: if JUnit 5 Platform artifacts are present, it uses the JUnit Platform with the Jupiter engine [8]. The project configures `maven-surefire-plugin` 3.5.2 in `pom.xml`, so `mvn test` discovers and runs all Jupiter tests and writes reports to `target/surefire-reports`. `mvn verify` additionally runs JaCoCo and creates the HTML coverage report. Copies of these reports are kept in `docs/results/` as evidence.

### 3.3 Features and project examples

| Feature | What it does [1] | Where this project uses it |
|---|---|---|
| `@Test`, `@DisplayName` | marks a test method; gives a readable report name | every test, e.g. `@DisplayName("TC-079 Ç sorts before D in a two-team tie")` in `TieBreakMultiTeamTest`; the TC number links report lines to the test plan |
| `@Nested` | non-static inner class to group related tests | `StandingsCalculatorTest` groups tests by requirement area (`ForfeitAdjustment`, `Validation`, and so on); `TieBreakMultiTeamTest` groups by FMT 9/2-b rule (mini-table criteria, no re-look at sub-ties, Turkish alphabetical order) |
| `@ParameterizedTest` with `@ValueSource` | one test method, several string or number inputs | `TeamTest` (valid names such as "Çaykur Rizespor", invalid blank names); `CsvLoaderTest` (malformed CSV lines) |
| `@CsvSource` | tabular argument rows | `MatchResultTest` (goal counts -1/0, boundary of FR-02), `PointDeductionTest` (1, 0, -1; boundary of V-07) |
| `@MethodSource` | arguments from a factory method | `StandingsCalculatorTest.awayForfeitCases()` (forfeit boundary around 3 goals) |
| `@NullSource` | passes `null` as an argument | `TeamTest` (null team name, V-01) |
| `assertEquals`, `assertTrue`, `assertFalse` | basic assertions | used throughout |
| `assertAll` | grouped assertions; all are evaluated and all failures reported together | `StandingsCalculatorTest` checks played/won/points/goals of a row in one `assertAll`, so one run shows every wrong field |
| `assertThrows` | asserts the exception type and returns it for further checks | 35 occurrences in negative tests, e.g. TC-035 checks that the message of the `IllegalArgumentException` contains the unknown team name |
| `@TempDir` | injects a temporary directory that is deleted after the test | `CsvLoaderTest` and `MainTest` write CSV files to a fresh directory instead of touching the repository |

**Lifecycle.** By default Jupiter creates a new instance of the test class for each test method [1]. This project relies on that default: `CsvLoaderTest` has an instance field `private final CsvLoader loader = new CsvLoader();`, and the tests share no mutable state. No `@BeforeEach`, `@BeforeAll` or `@AfterEach` callbacks are used, because the model objects are immutable records and each test builds its own data. `@TempDir` also provides per-test isolation for files.

**Extensions.** Jupiter's extension model is the mechanism behind features such as `@TempDir` [1]. The project does not write its own extension and does not use `@ExtendWith` directly; it only benefits from the built-in extensions behind `@TempDir` and `@ParameterizedTest`.

## 4. Test design techniques and their applicability

The techniques below come from the black-box family described in the ISTQB syllabus [3, Sec. 4.2] and in classic testing literature [4]. The syllabus defines equivalence partitioning (EP) as dividing data into partitions that are expected to be processed in the same way, and boundary value analysis (BVA) as exercising the boundaries of those partitions [3, Sec. 4.2]. The test plan applies them as follows.

**Equivalence partitioning (test plan, Sec. 2).** Inputs are divided into classes: team names (valid, blank, duplicate, with Turkish letters), goals, forfeit values, deduction points, CSV line shapes. One representative per class is tested. The class "names containing Ç, İ, Ş, Ü" was chosen because the library targets Turkish clubs. TC-078 and TC-079 come from this class and exposed DEF-001, where unresolved teams were displayed in Unicode code-point order instead of Turkish alphabetical order (defect log). EP also covered the CSV field "forfeit present but empty" (EP-52), which led to TC-087 and DEF-002.

**Boundary value analysis (Sec. 3).** Defects often sit at the edge of a class, so the plan tests values on both sides: goals -1/0/1 (BV-01), forfeit winner goals 2/3/4 (BV-02, the rule is max(3, g)), deduction -1/0/1 (BV-08), tied group size 2 versus 3 (BV-10), CSV fields 3/4/5/6 (BV-13). BV-06 shows that boundaries need thought: the mutual goal differences of two teams are always opposite numbers, so a difference of 1 is impossible and 2 is the real boundary.

**Decision tables (Sec. 4).** Tie-breaking has several conditions that combine (points equal, mutual goal difference equal, overall goal difference equal, forfeit losses, and so on). Table 4.1 lists rules R1..R7 for FMT 9/2-a, each with its deciding criterion and a test case. A decision table makes missing combinations visible, and the syllabus lists it as a standard technique for combinations of conditions [3, Sec. 4.2].

**Negative testing (Sec. 5).** These tests check that invalid input is rejected with a clear message: null lists, unregistered teams, a third match between two teams, malformed CSV lines. They use `assertThrows`. DEF-002 was found in this area, because a valid file with a trailing empty field was wrongly rejected.

**Regression testing.** The ISTQB syllabus separates confirmation testing (the original defect is fixed) from regression testing (nothing else broke) [3, Sec. 2.2.3]. The project did both: the failing tests from the first run served as confirmation tests, and the full re-run (125/125) was the regression test. Failing tests were committed before the fix, and no test was changed to make it pass (defect log, Notes).

**Property-based and metamorphic random testing (test plan, Sec. 6).** Hand-made cases are small and only check what the designer thought of. Random testing checks general properties over many generated inputs. QuickCheck popularised this approach: the tester states properties that should hold, and the tool checks them on randomly generated test data [7]. Metamorphic testing addresses the oracle problem: when the exact expected output is unknown, one checks relations between the outputs of related inputs [6]. The plan defines invariants (conservation of wins and losses, points = 3W + D − deductions) and metamorphic relations: shuffling input order must not change the table (TC-104), swapping home and away in every match must not change the table (TC-106), and adding a deduction must lower only that team's points (TC-107). Seeds are fixed so that a failure can be reproduced. At the time of this draft these generated-season tests (TC-100..TC-111) are planned and the season generator is still being rewritten, so no results can be reported yet. [STUDENT: update this paragraph once those tests have run.]

## 5. Code coverage and its limitations

Coverage measures how much of the code the tests execute. JaCoCo, the tool used here, reports several counters. Instruction coverage counts bytecode instructions. Line coverage counts a line as executed if at least one of its instructions ran. Branch coverage counts the branches of `if` and `switch` statements that were executed or missed [2]. The ISTQB syllabus defines statement and branch coverage in the same spirit [3, Sec. 4.3]. Coverage is useful as a negative signal: code that no test executes is certainly untested, and the plan uses JaCoCo for exactly this, after the black-box design (test plan, Sec. 1.2).

Coverage is a weak positive signal. The syllabus itself warns that exercising a statement with a test does not always detect a defect, for example data-dependent defects [3, Sec. 4.3]. Inozemtseva and Holmes report that, when the size of the test suite is controlled for, the correlation between coverage and test suite effectiveness is low to moderate, and they conclude that coverage is not a reliable quality target on its own, although it can help find under-tested code [5]. I cite this as the finding of one study, not as a general law.

The project gives two examples of these limits.

1. **DEF-001 lived in fully executed code.** The faulty line, `ordered.sort(criteria.thenComparing(r -> r.getTeam().name()))`, was executed by every test that built a table. Line coverage of that statement would have been 100% while the display order of "Çaykur Rizespor" was wrong. Only tests with discriminating data (names with Ç, İ, Ş, Ü) revealed the fault. Executed lines are not verified behaviour.
2. **Unreachable branch.** The V-06 check "more than two matches between two teams" can never fire, because the earlier duplicate-direction check already rejects a third match (defect log, observation 1). No test can cover it, so it is expected to appear as uncovered in the JaCoCo report. The right reaction is to document the reason, not to force coverage with artificial tests or to delete the code without analysis. Here a gap in the number points to defensive code, not to a testing gap.

Coverage also says nothing about missing code: if a requirement is not implemented, there is no code to cover. For this reason the traceability matrix (test plan, Sec. 9) links every requirement to test cases, independent of coverage figures. [STUDENT: insert the measured line and branch percentages from `docs/results/` and the list of uncovered branches with reasons once the coverage phase is complete.]

## 6. Challenges learning Java and implementing tests

`[STUDENT TO WRITE]` Personal reflection, about 150–250 words. Possible prompts:

- What was new about Java compared with languages you knew (records, immutability, generics, checked versus unchecked exceptions, streams)?
- Which JUnit feature was hardest at first (`@Nested` instances, `@MethodSource` factory methods, `@TempDir`, why `assertThrows` needs a lambda)?
- How did you decide expected values by hand, and did any hand calculation turn out to be wrong rather than the code?
- What was it like to find a defect you had already suspected (DEF-001) versus one you had not (DEF-002)?
- What did the Turkish-locale environment (tr_TR) or UTF-8 handling teach you?
- What would you do differently next time?

## References

[1] S. Bechtold, S. Brannen, et al., *JUnit 5 User Guide*, version 5.11.4. [Online]. Available: https://docs.junit.org/5.11.4/user-guide/ (accessed Nov. 12, 2026).

[2] JaCoCo Project, "Coverage Counters," *JaCoCo Java Code Coverage Library documentation*. [Online]. Available: https://www.jacoco.org/jacoco/trunk/doc/counters.html (accessed Nov. 12, 2026).

[3] International Software Testing Qualifications Board (ISTQB), *Certified Tester Foundation Level Syllabus*, v4.0.1, Sep. 15, 2024. [Online]. Available: https://www.istqb.org/wp-content/uploads/2024/11/ISTQB_CTFL_Syllabus_v4.0.1.pdf (accessed Nov. 12, 2026).

[4] G. J. Myers, C. Sandler, and T. Badgett, *The Art of Software Testing*, 3rd ed. Wiley, 2011, ISBN 978-1-118-03196-4.

[5] L. Inozemtseva and R. Holmes, "Coverage is not strongly correlated with test suite effectiveness," in *Proc. 36th Int. Conf. Software Engineering (ICSE '14)*, Hyderabad, India, 2014, pp. 435–445, doi: 10.1145/2568225.2568271.

[6] T. Y. Chen, F.-C. Kuo, H. Liu, P.-L. Poon, D. Towey, T. H. Tse, and Z. Q. Zhou, "Metamorphic testing: A review of challenges and opportunities," *ACM Comput. Surv.*, vol. 51, no. 1, Art. 4, 2018, doi: 10.1145/3143561.

[7] K. Claessen and J. Hughes, "QuickCheck: A lightweight tool for random testing of Haskell programs," in *Proc. 5th ACM SIGPLAN Int. Conf. Functional Programming (ICFP '00)*, 2000, pp. 268–279, doi: 10.1145/351240.351266.

[8] The Apache Software Foundation, "Using JUnit 5 Platform," *Maven Surefire Plugin documentation*. [Online]. Available: https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html (accessed Nov. 12, 2026).
