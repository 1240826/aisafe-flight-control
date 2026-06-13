# US111 — Unit Tests

## Domain Tests

### `SimulationTest` — Domain invariants

File: `aisafe.base/core/src/test/java/eapli/aisafe/simulation/domain/SimulationTest.java`

Parameterized tests using CSV data:

| Test Case | Description | Expected |
|-----------|-------------|----------|
| Valid simulation | All fields valid | Created with PENDING result |
| Null parameters | null area/time/threshold | Throws exception |
| End before start | endDateTime before startDateTime | Throws exception |
| Negative threshold | SafetyThreshold with negative value | Throws exception |

### `GenerateSimulationReportControllerTest` — Controller orchestration

File: `aisafe.base/core/src/test/java/eapli/aisafe/simulation/application/GenerateSimulationReportControllerTest.java`

| Test | Description | Expected |
|------|-------------|----------|
| Valid report generation | Find simulation, write file | File path returned |
| No simulation found | Unknown area code | NoSuchElementException |
| Null area code | null input | IllegalArgumentException |
| Blank area code | " " input | IllegalArgumentException |
| Authorization | allSimulations() and generateReport() | Auth check enforced |

### `SimulationReportFileWriterTest` — File writing

File: `aisafe.base/core/src/test/java/eapli/aisafe/simulation/application/SimulationReportFileWriterTest.java`

Parameterized tests with temporary files (@TempDir):

| Test | Description | Expected |
|------|-------------|----------|
| Write to file | Write report content to temp path | File exists with correct content |
| Long content | Write multi-line report | All lines preserved |
