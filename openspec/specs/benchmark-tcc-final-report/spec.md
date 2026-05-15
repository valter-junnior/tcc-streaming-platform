## ADDED Requirements

### Requirement: TCC-ready comparative report generation
The system MUST generate a markdown report from aggregated results that presents the benchmark data in a format suitable for direct inclusion in the TCC document, covering all 4 server+transcoder combinations across all viewer loads.

#### Scenario: Generate report from aggregated CSV
- **WHEN** the operator runs the report generator with the aggregated CSV as input
- **THEN** it SHALL produce a `report-tcc-final.md` file containing formatted markdown tables with mean values and standard deviations per combination

#### Scenario: Report covers all defined metrics
- **WHEN** the report is generated
- **THEN** it SHALL include tables for: Startup HLS, CPU médio, Memória média, Bitrate efetivo, Taxa de erros de segmento and Reinicializações

### Requirement: Acceptance criteria evaluation in report
The report MUST include a section that evaluates each test result against the TCC acceptance criteria and marks each combination/metric as PASS or FAIL.

#### Scenario: Evaluate criteria thresholds
- **WHEN** a metric value for a combination exceeds its defined threshold
- **THEN** the report SHALL mark that cell or row as FAIL and include the threshold value as reference

### Requirement: Methodological notes section
The report MUST include a dedicated section documenting known methodological constraints, including the shared-host CPU measurement context and the absence of automated end-to-end latency measurement.

#### Scenario: Include CPU methodology note
- **WHEN** the report is generated and CPU values exceed 80%
- **THEN** the report SHALL include a note explaining that CPU is measured on a shared host running all services simultaneously
