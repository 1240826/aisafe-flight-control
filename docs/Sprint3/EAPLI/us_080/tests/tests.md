# US080 — Unit Tests

## Domain Tests

### `FlightPlanTest` — FlightPlan creation invariants

File: `aisafe.base/core/src/test/java/eapli/aisafe/flightplan/domain/FlightPlanTest.java`

Parameterized tests using CSV data:

| Test Case | Description | Expected |
|-----------|-------------|----------|
| Valid plan creation | All fields valid | Created with DRAFT status |
| Null parameters | null id or content | Throws exception |
| Status lifecycle | DRAFT → IN_TEST → TEST_PASSED/TEST_FAILED | Valid transitions |

### `ImportFlightPlanControllerTest` — Controller tests (DSL import)

File: `aisafe.base/core/src/test/java/eapli/aisafe/flightplan/application/ImportFlightPlanControllerTest.java`

File-based tests using CSV flight plan data:

| Test | Description | Expected |
|------|-------------|----------|
| Valid DSL import | Correct DSL content | Flight + FlightPlan created |
| Invalid DSL syntax | Malformed DSL | Lexical/syntactic errors reported |
| Invalid semantic | Valid syntax, bad domain data | Semantic errors reported |
| Authorization | Non-FCO user | Auth check enforced |

### `FlightPlanDataValidationTest` — Parameterized data validation

File: `aisafe.base/core/src/test/resources/us080/flight_plan_test_data.csv`

Tests flight plan data validation rules from CSV file.
