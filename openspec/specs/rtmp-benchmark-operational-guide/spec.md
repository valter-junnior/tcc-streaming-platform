## ADDED Requirements

### Requirement: Single technical operation guide
The project MUST provide one concise markdown guide explaining test objective, prerequisites, commands, outputs, and continuation steps.

#### Scenario: Read and execute from guide
- **WHEN** an operator follows the documented steps
- **THEN** the operator SHALL be able to run sanity and full campaigns without consulting additional documents

### Requirement: Docker execution policy documentation
The guide MUST state that benchmark commands are executed through Docker context and MUST avoid instructions that require modifying functional application code.

#### Scenario: Validate execution constraints
- **WHEN** an operator reviews the constraints section
- **THEN** the guide SHALL explicitly indicate Docker-only execution and non-invasive operation relative to existing app behavior

### Requirement: Result interpretation baseline
The guide MUST explain how to interpret key CSV fields and define next actions when scenarios fail.

#### Scenario: Diagnose a failed scenario row
- **WHEN** the CSV contains a failed status row
- **THEN** the guide SHALL provide objective troubleshooting steps based on logs, container health, and rerun strategy
