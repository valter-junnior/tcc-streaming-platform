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
The system MUST provide one command entrypoint to execute the full benchmark matrix across all required RTMP server and transcoder combinations.

#### Scenario: Trigger full matrix command
- **WHEN** the operator runs the full benchmark command
- **THEN** the system SHALL execute all configured scenarios in deterministic order and persist per-scenario result status for downstream reporting

### Requirement: Scenario lifecycle boundaries
The benchmark runner MUST measure only the active live interval from stream start to stream end and MUST exclude provisioning/setup time from performance measurements.

#### Scenario: Enforce measurement window
- **WHEN** a scenario starts and containers are already provisioned
- **THEN** metrics collection SHALL begin at live start marker and stop at live end marker, excluding setup and teardown intervals
