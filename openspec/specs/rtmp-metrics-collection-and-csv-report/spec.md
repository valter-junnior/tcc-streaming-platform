## ADDED Requirements

### Requirement: Per-scenario metrics capture
For each executed scenario, the benchmark system MUST capture a consistent metric set including scenario identity, server type, transcoder type, viewers count, test duration, and core runtime indicators.

#### Scenario: Capture metrics during active live
- **WHEN** a scenario enters active live interval
- **THEN** the collector SHALL record metrics samples and derive summary values associated with that scenario execution ID

### Requirement: Consolidated CSV matrix output
The benchmark system MUST generate a consolidated CSV file containing one row per executed scenario run and normalized columns for cross-comparison. When multiple repetitions exist for the same combination, the repeat_index column SHALL distinguish each run, enabling downstream aggregation.

#### Scenario: Produce CSV after full run with repeats
- **WHEN** the full campaign finishes with 3 repetitions per combination
- **THEN** the system SHALL write a CSV matrix file with 36 rows (4 combinations × 3 viewer counts × 3 repetitions), each row with an explicit `repeat_index` value and all metric fields populated

#### Scenario: Preserve individual repetition rows
- **WHEN** multiple repetitions of the same combination complete
- **THEN** each repetition SHALL appear as a separate row in the CSV, not averaged, so that the aggregation step can compute statistics from raw data

### Requirement: Reproducibility metadata
The CSV output MUST include metadata fields sufficient to reproduce runs, including timestamp, image/tag identifiers, and execution mode (sanity/full).

#### Scenario: Persist reproducibility fields
- **WHEN** a scenario result row is written
- **THEN** the row SHALL include reproducibility metadata and parameter values used for that run
