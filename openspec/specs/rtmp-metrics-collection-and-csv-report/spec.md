## ADDED Requirements

### Requirement: Per-scenario metrics capture
For each executed scenario, the benchmark system MUST capture a consistent metric set including scenario identity, server type, transcoder type, viewers count, test duration, and core runtime indicators.

#### Scenario: Capture metrics during active live
- **WHEN** a scenario enters active live interval
- **THEN** the collector SHALL record metrics samples and derive summary values associated with that scenario execution ID

### Requirement: Consolidated CSV matrix output
The benchmark system MUST generate a consolidated CSV file containing one row per executed scenario run and normalized columns for cross-comparison.

#### Scenario: Produce CSV after full run
- **WHEN** the full campaign finishes (with success or partial failures)
- **THEN** the system SHALL write a CSV matrix file with completed rows and explicit error/status fields for failed runs

### Requirement: Reproducibility metadata
The CSV output MUST include metadata fields sufficient to reproduce runs, including timestamp, image/tag identifiers, and execution mode (sanity/full).

#### Scenario: Persist reproducibility fields
- **WHEN** a scenario result row is written
- **THEN** the row SHALL include reproducibility metadata and parameter values used for that run
