## MODIFIED Requirements

### Requirement: Consolidated CSV matrix output
The benchmark system MUST generate a consolidated CSV file containing one row per executed scenario run and normalized columns for cross-comparison. When multiple repetitions exist for the same combination, the repeat_index column SHALL distinguish each run, enabling downstream aggregation.

#### Scenario: Produce CSV after full run with repeats
- **WHEN** the full campaign finishes with 3 repetitions per combination
- **THEN** the system SHALL write a CSV matrix file with 36 rows (4 combinations × 3 viewer counts × 3 repetitions), each row with an explicit `repeat_index` value and all metric fields populated

#### Scenario: Preserve individual repetition rows
- **WHEN** multiple repetitions of the same combination complete
- **THEN** each repetition SHALL appear as a separate row in the CSV, not averaged, so that the aggregation step can compute statistics from raw data
