## ADDED Requirements

### Requirement: Per-combination statistical aggregation
The post-processing tool MUST aggregate all repetitions for each `(server, transcoder, viewers)` combination and produce mean and standard deviation for every numeric metric.

#### Scenario: Aggregate three repetitions
- **WHEN** the aggregator receives a CSV with 3 rows sharing the same `(server, transcoder, viewers)` values
- **THEN** it SHALL produce one output row with `<metric>_mean` and `<metric>_stddev` columns for each numeric metric

#### Scenario: Handle single repetition gracefully
- **WHEN** only 1 repetition exists for a combination
- **THEN** the aggregator SHALL output the values as mean and `stddev = 0`, without failing

### Requirement: Aggregated CSV output format
The aggregator MUST write a CSV file with one row per `(server, transcoder, viewers)` combination containing all aggregated metrics and with column names following the pattern `<metric>_mean` and `<metric>_stddev`.

#### Scenario: Produce aggregated CSV file
- **WHEN** aggregation completes
- **THEN** a file `results-aggregated.csv` SHALL be written to the same results directory containing one row per combination

### Requirement: Standalone execution without external dependencies
The aggregator script MUST run using only Python standard library modules (csv, statistics, sys, os) so it can be executed in the benchmark environment without additional package installation.

#### Scenario: Run without pip install
- **WHEN** the operator runs `python3 aggregate-results.py <results.csv>`
- **THEN** the script SHALL complete successfully without requiring any third-party packages
