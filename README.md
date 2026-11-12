# TFF Standings Engine

A small Java application that builds a football league table (Süper Lig style) from teams, played match results and point deductions, and orders tied teams according to the rules of the Turkish Football Federation (TFF). It was written as an individual software verification and validation (SVV) assignment with JUnit 5 and JaCoCo.

The core is a pure library (`edu.svv.standings`); a thin command-line front end reads CSV files and prints the table. There is no GUI and no database.

## Rule sources

The rules come from the 2026-27 Süper Lig Competition Statute and the TFF Football Match Instruction (FMT, Art. 9, 23/4 and 24), plus the TFF Disciplinary Instruction for point deductions. The full requirements specification, including validation rules, assumptions and decisions, is in [docs/requirements.md](docs/requirements.md). Article numbers used in code comments refer to that document.

## Requirements to build

- Java 21
- Maven 3.9 or newer

## Build and test

```
mvn verify
```

This compiles the code, runs the tests and generates the coverage report. Reports are written to:

- Test results: `target/surefire-reports/`
- Coverage (JaCoCo): `target/site/jacoco/index.html`

To build the runnable jar without tests: `mvn package -DskipTests`.

## Run the command-line application

```
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar <teams.csv> <matches.csv> [deductions.csv]
```

Example with the sample data:

```
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar examples/teams.csv examples/matches.csv examples/deductions.csv
```

The table has the columns Pos, Team, P, W, D, L, GF, GA, GD and Pts. A team that is still level with a neighbour after all tie-break criteria is marked with `*` (unresolved tie, requires a playoff).

Exit codes: `0` success, `1` input error (missing file, malformed line, unknown team, invalid data, unknown scenario), `2` wrong number of arguments. Error messages go to standard error and name the file and the 1-based line number.

The sample data in `examples/` uses real Süper Lig club names, but **all results and the point deduction are invented** for demonstration only.

### Scenario library

The program can also print the table of one of the 12 pre-generated, realistic random seasons stored in `src/main/resources/scenarios/`:

```
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario S04
java -jar target/tff-standings-engine-0.1.0-SNAPSHOT.jar --scenario random 42
```

`--scenario <id>` loads that scenario (`S01` to `S12`); `--scenario random [seed]` picks one with the given seed (without a seed the current time is used and printed, so the pick can be repeated). The first output line names the scenario and describes it. The seasons were generated from the 2026-27 clubs' real results of the last ten seasons; the results themselves are simulated. How they are generated and why these 12 were chosen is described in [docs/generator.md](docs/generator.md).

## CSV format

Files are UTF-8, semicolon-separated, without a header row. Blank lines are ignored. A leading UTF-8 byte order mark is accepted; spaces around fields are trimmed.

| File | Line format | Notes |
|---|---|---|
| teams | `name` | one team per line |
| matches | `home;away;homeGoals;awayGoals;forfeit` | `forfeit` is `NONE`, `HOME` or `AWAY` (the team that forfeits); optional, default `NONE`. Goals are the real on-pitch score; the forfeit adjustment (FMT 23/4) is applied by the engine. |
| deductions | `team;points` | optional file; `points` is a positive integer |

Team names in the matches and deductions files must match a name in the teams file exactly (after trimming).

Example `matches.csv` lines:

```
Galatasaray;Fenerbahçe;2;1
Kasımpaşa;Çaykur Rizespor;2;2;AWAY
```
