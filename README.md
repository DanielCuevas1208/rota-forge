# Rota Forge

Rota Forge builds staff rosters for fixed shifts.
It reads staff, shifts, and rules from CSV files.
It plans the roster with simulated annealing.
It scores every hard and soft rule.
It writes the roster as CSV and as a printable HTML report.

## What it does

The solver decides who works each shift on each day.
It starts with a feasible schedule built by a greedy pass.
It then uses simulated annealing to lower the soft score.
The final schedule always satisfies the hard rules.

The bundled example solves a two-week clinic roster.
Twelve nurses cover day, evening, and night shifts.
The roster applies rest and fairness rules.
The HTML report shows the score of each constraint.

## Features

- Hard rules never break. The solver returns a feasible schedule.
- Rest and fairness rules shape the work pattern.
- The report shows the score of every constraint.
- Runs are reproducible with a fixed seed.
- The solver needs no external services and no secrets.

## Repository layout

| Path | Purpose |
| --- | --- |
| `data/clinic` | The bundled clinic example (CSV input) |
| `src/main/scala/rotaforge/io` | CSV read and write |
| `src/main/scala/rotaforge/core` | Schedule model, greedy start, annealing |
| `src/main/scala/rotaforge/score` | Hard and soft constraint scoring |
| `src/main/scala/rotaforge/report` | HTML report |
| `src/main/scala/rotaforge` | Solver pipeline and command line entry point |
| `src/test/scala/rotaforge` | Unit and integration tests |

## How it works

1. Load the CSV files into a roster model.
2. Build a first feasible schedule with a greedy pass.
3. Run simulated annealing to lower the soft score.
4. Write the roster CSV and the HTML report.

The annealing loop uses two kinds of moves.
A shift swap changes the shift types of two staff on one day.
A day swap exchanges the working days of two staff.
Both moves keep the shift coverage constant.
The solver restarts the search with different random streams.
It keeps the best schedule that it has seen.

### Constraints

Hard rules have the highest weight. A violation stops the schedule.

| Constraint | Meaning |
| --- | --- |
| Shift coverage | Each slot has the required number of staff |
| Skill match | Staff have the skill that the shift needs |
| Availability | No one works on a blocked day |
| Max shifts | No one exceeds the per-person shift limit |

Soft rules shape the quality of the schedule.

| Constraint | Meaning |
| --- | --- |
| Rest between shifts | Minimum hours between two shifts |
| Consecutive days | Limit on days worked in a row |
| Workload fairness | Spread the shifts evenly |
| Staff preferences | Staff get the shifts they request |
| Weekend balance | Share the weekend work evenly |

A lower score is better.
The report lists the score of each constraint.

## Requirements

- JDK 21
- sbt 1.11.7 (the launcher downloads what the build needs)

## Setup and run

Open a terminal in the repository root.

Run the bundled clinic example:

```sh
sbt "run data/clinic out/clinic"
```

The command writes two files:

- `out/clinic/roster.csv`
- `out/clinic/report.html`

Open the HTML report in a browser.
Print it with the browser print function.

## Command line options

| Option | Default | Purpose |
| --- | --- | --- |
| `<inputDir>` | (required) | Directory that holds the input CSV files |
| `[outputDir]` | `out` | Directory for the output files |
| `--seed <n>` | `42` | Random seed for reproducibility |
| `--iterations <n>` | `200000` | Total annealing iterations |
| `--startTemp <t>` | `200` | Starting temperature |
| `--endTemp <t>` | `0.5` | Final temperature |
| `--help` | | Show the usage message |

## Input format

The input directory holds five CSV files.
Each file has a header row.

| File | Columns | Purpose |
| --- | --- | --- |
| `staff.csv` | `id,name,skill` | The people who work |
| `days.csv` | `date` | The dates of the roster |
| `shifts.csv` | `id,name,start,end,skill,count` | The shift types |
| `preferences.csv` | `staff,date,shift,weight` | Requests and blocked days |
| `rules.csv` | `key,value` | Numeric rule settings |

Use times in 24-hour form, such as `07:00`.
A shift that crosses midnight keeps its full length.
A preference weight of zero blocks the shift for that day.
A weight from one to five records how much the person wants it.
The rules file is optional. It uses the defaults when missing.

## Sample output

The clinic example produces this score summary:

```
Rota Forge 0.1.0
Input:   .../data/clinic
Output:  .../out/clinic
Seed:    42
Iterations: 200000

Roster
  Staff:     12
  Days:      14 (2026-05-04 to 2026-05-17)
  Shifts:    D, E, N
  Assignments: 84

Score
  Hard violations: 0
  Total score:     58  (initial 75)

Constraint results
  Shift coverage        HARD  weight 1000000  violations 0  penalty 0
  Skill match           HARD  weight 1000000  violations 0  penalty 0
  Availability          HARD  weight 1000000  violations 0  penalty 0
  Max shifts per person HARD  weight 1000000  violations 0  penalty 0
  Rest between shifts   soft  weight 5  violations 0  penalty 0
  Max consecutive days  soft  weight 4  violations 0  penalty 0
  Workload fairness     soft  weight 3  violations 0  penalty 0
  Staff preferences     soft  weight 1  violations 58  penalty 58
  Weekend balance       soft  weight 3  violations 0  penalty 0
```

The report page shows the same numbers in a table.
It marks each hard rule as pass or fail.
It also shows the full roster grid.
Weekend columns have a shaded header.

## Tests

Run the full suite:

```sh
sbt test
```

The suite has 38 tests.
It covers CSV parsing, loading, scoring, annealing, and reporting.
It asserts zero hard-rule violations for the clinic example.
It asserts that the final score stays in a stable range.
It asserts that the solver is deterministic for a fixed seed.

## Limitations

- The solver handles fixed shift types only.
- It does not model part-time or on-call patterns.
- It does not prove optimality. It finds a good schedule.
- The search is random but seeded. Results are reproducible.
- The example uses one skill pool (registered nurses).

## Roadmap

- Release 0.2: multiple skill pools and skill-based coverage.
- Release 0.3: contract types, such as part-time and fixed days.
- Release 0.4: incremental scoring for large rosters.
- Release 0.5: a configuration file for constraint weights.

## License

MIT. See the `LICENSE` file.
