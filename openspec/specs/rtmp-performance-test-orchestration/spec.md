## ADDED Requirements

### Requirement: Docker-only benchmark orchestration
The test harness MUST execute benchmark scenarios exclusively using Docker services and commands, without requiring direct host-level execution of benchmark binaries.

#### Scenario: Run sanity case for nginx with ffmpeg
- **WHEN** the operator runs the sanity command for the `nginx+ffmpeg` combination
- **THEN** the harness SHALL start required containers, execute one minimal live-and-viewers run, and return a pass/fail status for that scenario

#### Scenario: Run sanity sweep across all combinations
- **WHEN** the operator runs sanity mode without filtering
- **THEN** the harness SHALL execute one minimal case for each combination (`nginx+ffmpeg`, `nginx+gstreamer`, `srs+ffmpeg`, `srs+gstreamer`) sequentially and report individual outcomes

### Requirement: Single-command full campaign execution
The system MUST provide one command entrypoint to execute the full benchmark matrix across all required RTMP server and transcoder combinations, using configurable duration (minimum 300 seconds) and configurable repetitions (minimum 3) to produce statistically valid results.

#### Scenario: Trigger full matrix command with correct parameters
- **WHEN** the operator runs the full benchmark command with `--duration 300 --repeats 3`
- **THEN** the system SHALL execute all 36 scenario runs (4 combinations × 3 viewer counts × 3 repetitions) in deterministic order and persist per-scenario result status for downstream reporting

#### Scenario: Reject duration below minimum for full mode
- **WHEN** the operator runs full mode with `--duration` below 300 seconds
- **THEN** the system SHALL emit a warning recommending 300s minimum but SHALL NOT block execution

### Requirement: Report output directed to docs directory
The benchmark orchestrator MUST write final markdown reports to `docs/benchmark/` and MUST NOT maintain a duplicate copy of those reports at the `app/benchmark/` root level.

#### Scenario: Reports written to docs/benchmark after run
- **WHEN** a full or sanity benchmark run completes and `generate_reports` is called
- **THEN** the final markdown report files SHALL be written to `docs/benchmark/` and the `app/benchmark/` root SHALL contain no `.md` report files

#### Scenario: docs/benchmark directory created automatically
- **WHEN** the report output directory `docs/benchmark/` does not exist
- **THEN** the script SHALL create it before writing any report files

### Requirement: Scenario lifecycle boundaries
The benchmark runner MUST measure only the active live interval from stream start to stream end and MUST exclude provisioning/setup time from performance measurements.

#### Scenario: Enforce measurement window
- **WHEN** a scenario starts and containers are already provisioned
- **THEN** metrics collection SHALL begin at live start marker and stop at live end marker, excluding setup and teardown intervals
