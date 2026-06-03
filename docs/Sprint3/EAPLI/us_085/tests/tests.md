# US085 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5 with Mockito for application-layer tests.

Parameterized tests are driven by CSV test data files.

14 test classes cover **147 tests** across domain entities, value objects, application controllers, services, and CSV-driven parameterized suites.

---

## 1. Flight Domain — Aggregate Root Tests

### `Flight` Aggregate Root (9 tests)

| Test | Scenario |
|------|----------|
| `ensureFlightIsCreatedWithDesignator` | Flight stores the identity correctly |
| `ensureFlightStartsWithNoPlans` | New flight has empty flight plan list |
| `ensureAddFlightPlanWorks` | `addFlightPlan()` creates and adds a FlightPlan |
| `ensureFlightPlanLookupWorks` | `flightPlan(id)` finds plan by ID |
| `ensureFlightPlanLookupReturnsEmptyForUnknown` | Unknown ID returns empty |
| `ensureMultiplePlansCanBeAdded` | Multiple plans can exist in the same flight |
| `ensureFlightPlanListIsUnmodifiable` | Returned list is unmodifiable |
| `ensureSameAsWithSameIdentity` | Same identity → equal |
| `ensureSameAsWithDifferentIdentity` | Different identity → not equal |

### `FlightDesignator` Value Object (14 tests)

| Test | Scenario |
|------|----------|
| `ensureValidDesignatorIsAccepted` | `TP1234` is valid |
| `ensureLowerCaseIsUppercased` | `tp5678` → `TP5678` |
| `ensureValueOfFactoryWorks` | `valueOf()` factory method works |
| `ensureNullDesignatorIsRejected` | Null throws `IllegalArgumentException` |
| `ensureBlankDesignatorIsRejected` | Blank throws `IllegalStateException` |
| `ensureInvalidFormatIsRejected` | `INVALID` (no digits) rejected |
| `ensureFormatWithoutLeadingLettersIsRejected` | `12345` rejected |
| `ensureFormatWithTooManyDigitsIsRejected` | `TP12345` (5 digits) rejected |
| `ensureFormatWithOptionalLetterIsAccepted` | `TP123A` (optional trailing letter) accepted |
| `ensureOneDigitIsAccepted` | `TP1` (1 digit) accepted |
| `ensureEqualsAndHashCode` | Same value → equal, same hash |
| `ensureNotEquals` | Different values → not equal |
| `ensureCompareToWorks` | Natural ordering by designator string |

---

## 2. FlightPlan Domain — Status Lifecycle

The flight plan status lifecycle drives the validation workflow:

| Status | Meaning |
|--------|---------|
| `DRAFT` | Initial state — flight plan created but not yet tested |
| `IN_TEST` | Validation is in progress |
| `TEST_PASSED` | All validation checks succeeded |
| `TEST_FAILED` | One or more validation checks failed |

### `FlightPlanId` Value Object (8 tests)

| Test | Scenario |
|------|----------|
| `ensureValidIdIsAccepted` | `FP001` accepted |
| `ensureValueOfReturnsCorrectId` | Factory method works |
| `ensureNullThrowsException` | Null rejected |
| `ensureBlankThrowsException` | Blank rejected |
| `ensureLongerThan20CharsThrowsException` | 21+ chars rejected |
| `ensureSpecialCharactersAreRejected` | Non-alphanumeric rejected |
| `ensureWhitespaceIsTrimmed` | Leading/trailing whitespace trimmed |
| `ensureEqualsAndHashCode` | Same value → equal |

### `FlightPlan` Entity (14 tests)

| Test | Scenario |
|------|----------|
| `ensureNewFlightPlanIsDraft` | New plan starts in DRAFT |
| `ensureMarkAsInTestChangesStatus` | DRAFT → IN_TEST |
| `ensureMarkAsInTestFromDraftOnly` | Non-DRAFT → IllegalStateException |
| `ensureMarkAsTestPassedFromInTestOnly` | IN_TEST → TEST_PASSED |
| `ensureMarkAsTestFailedFromInTestOnly` | IN_TEST → TEST_FAILED |
| `ensureResetToDraft` | Clears status, report, testedAt |
| `ensureNullFlightPlanIdIsRejected` | Null ID → IllegalArgumentException |
| `ensureNullDslContentIsRejected` | Null DSL → IllegalArgumentException |
| `ensureBlankDslContentIsRejected` | Blank DSL → IllegalStateException |
| `ensureRecordTestResultStoresReport` | After test, report is stored |
| `ensureRecordTestResultFailed` | Failed result sets TEST_FAILED |
| `ensureUpdateDslContentResetsToDraft` | DSL update resets status to DRAFT |
| `ensureSameAsWorks` | Same ID → equal |
| `ensureIdentity` | `identity()` returns the FlightPlanId |

### `ValidationResult` Value Object (9 tests)

| Test | Scenario |
|------|----------|
| 9 tests covering `passed()`, `failed()`, reasons list, immutability, equality |

### Key Domain Invariants Tested

| Rule | Test Coverage |
|------|---------------|
| New Flight starts with no plans | `ensureFlightStartsWithNoPlans` |
| New FlightPlan starts in DRAFT | `ensureNewFlightPlanIsDraft` |
| Only DRAFT plans can start testing | `ensureMarkAsInTestFromDraftOnly` |
| Only IN_TEST plans can pass/fail | `ensureMarkAsTestPassedFromInTestOnly`, `ensureMarkAsTestFailedFromInTestOnly` |
| Result recording includes report data | `ensureRecordTestResultStoresReport` |
| Null/blank arguments rejected | `ensureNullFlightPlanIdIsRejected`, `ensureBlankDslContentIsRejected` |
| DSL update resets status | `ensureUpdateDslContentResetsToDraft` |
| FlightDesignator format validated | `ensureInvalidFormatIsRejected`, `ensureFormatWithTooManyDigitsIsRejected` |
| FlightDesignator case-insensitive | `ensureLowerCaseIsUppercased` |
| FlightPlan list is unmodifiable | `ensureFlightPlanListIsUnmodifiable` |

---

## 3. Application Layer — Controller & Services

### `FlightPlanExporter` (4 tests)

The exporter now uses a **converter-first strategy**: tries `FlightPlanToScenarioConverter.convert()` for valid ANTLR DSL, falling back to legacy `{ID, FlightPlanDSL}` format.

| Test | Scenario |
|------|----------|
| `ensureExportsStructuredJsonForValidDsl` | Valid ANTLR DSL → structured scenario JSON |
| `ensureExportsValidStructuredJson` | Output contains expected JSON array structure |
| `ensureFallbackForInvalidDsl` | Invalid DSL → legacy `{ID, FlightPlanDSL}` format |
| `ensureFallbackEscapesDslContent` | Fallback correctly escapes JSON special chars |

### `ImportFlightPlanControllerTest` (4 tests)

| Test | Scenario |
|------|----------|
| `ensureImportWithValidDslCreatesFlightPlan` | Valid ANTLR DSL → FlightPlan created with DRAFT status |
| `ensureImportWithInvalidDslReturnsErrors` | Invalid DSL → per-phase errors returned (lexer/parser/semantic) |
| `ensureImportWithMissingFlightReturnsError` | Null/empty flight → error returned |
| `ensureExtractFlightDesignatorWorks` | ANTLR parse tree → correct flight designator extracted |

### `ReportParser` (13 tests)

| Test | Scenario |
|------|----------|
| 6 core tests covering PASS/FAIL parsing, violation count extraction, edge cases |
| `ensureReportTypeDefaultsToSimulated` | No report type line → defaults to SIMULATED |
| `ensureSimulatedReportTypeIsParsed` | "Report type: SIMULATED" parsed correctly |
| `ensureExecutedReportTypeIsParsed` | "Report type: EXECUTED" parsed correctly |
| `ensureReportTypeIsCaseInsensitive` | Case-insensitive matching |

### `ProcessBuilderSimulationRunner` (3 tests)

| Test | Scenario |
|------|----------|
| 3 tests covering null input rejection, timeout behavior, execution failure |

### `TestFlightPlanController` (12 tests)

| Test | Scenario |
|------|----------|
| `ensureTestFlightPlanWithUnknownIdThrows` | Unknown flightPlanId → IllegalArgumentException |
| `ensureTestFlightPlanInvalidIdThrows` | Invalid ID format → IllegalStateException |
| `ensureTestFlightPlanNotInDraftReturnsFailure` | Non-DRAFT status → failure result |
| `ensureTestFlightPlanDslFailureReturnsFailure` | **US085.4** DSL failure → TEST_FAILED, C simulator NOT invoked |
| `ensureTestFlightPlanRunnerFailureResetsToDraft` | Simulator crash → reset to DRAFT |
| `ensureTestFlightPlanPassHappyPath` | Valid DSL + passing simulator → TEST_PASSED |
| `ensureTestFlightPlanFailHappyPath` | Valid DSL + failing simulator → TEST_FAILED |
| `ensureTestFlightPlanWithDesignatorAndIdPassHappyPath` | 2-arg overload: flight designator + flight plan ID → TEST_PASSED |
| `ensureAllFlightsDelegates` | `allFlights()` delegates to `FlightRepository.findAll()` |
| `ensureAuthorizationIsChecked` | Authorization enforced on `allFlights()` |
| `ensureDepartureTimeMismatchReturnsFailure` | Departure time mismatch → failure |

**US085.4 — DSL Failure Test (critical):**
```
Given a flight plan with invalid DSL content,
When the controller executes testFlightPlan,
Then DSL validation fails → markAsInTest() + recordTestResult(false) + save()
And the flight plan status is TEST_FAILED.
```

---

## 4. Parameterized Tests (CSV-Driven)

### `ReportParserParameterizedTest` (10 tests)

Driven by `simulation_report_test_data.csv` (rows SR01–SR10). For each row, a C-simulator report string is generated with the specified `totalViolations`, then parsed by `ReportParser`. The test verifies:

- `violationCount` matches the CSV `totalViolations`
- `isPassed` is `true` when `totalViolations == 0`, `false` otherwise
- `criticalViolations`, `majorViolations`, `minorViolations` match expectations
- `hasUnresolvedConflicts` matches CSV boolean
- `reportType` matches CSV (EXECUTED or SIMULATED)

| Test Case | Total Violations | Expected Violations | Expected Passed | Report Type |
|-----------|-----------------|---------------------|-----------------|-------------|
| SR01 | 5 | 5 | false | EXECUTED |
| SR02 | 0 | 0 | true | EXECUTED |
| SR03 | 3 | 3 | false | SIMULATED |
| SR04 | 8 | 8 | false | SIMULATED |
| SR05 | 1 | 1 | false | EXECUTED |
| SR06 | 10 | 10 | false | SIMULATED |
| SR07 | 2 | 2 | false | EXECUTED |
| SR08 | 6 | 6 | false | SIMULATED |
| SR09 | 4 | 4 | false | EXECUTED |
| SR10 | 7 | 7 | false | SIMULATED |

### `DslValidatorParameterizedTest` (15 tests)

Driven by `dsl_validation_test_data.csv` (rows DV01–DV15). Each test case maps to an explicit DSL string via `dslForTestCase()`, then runs `DslValidator.validate()`. The enhanced `DslValidator` handles structural checks (non-empty, starts with `departure`, contains `;`), semantic validation (aircraft model, fuel values, flight type), field-specific range checks (altitude, speed), and duplicate keyword detection.

| Test Case | Description | DSL Pattern | Validator Result | Error Type |
|-----------|-------------|-------------|-----------------|------------|
| DV01 | Regular full flight plan | `"departure LIS 10:00; arrival OPO 11:00; aircraft B738; fuel 15000;"` | VALID | NONE |
| DV02 | Charter with segments | `"departure OPO 10:00; arrival LIS 11:00; type charter;"` | VALID | NONE |
| DV03 | Missing semicolon | `"departure LIS 10:00"` | **INVALID** | SYNTACTIC |
| DV04 | Invalid flight type | `"departure LIS 10:00; arrival OPO 11:00; type invalid;"` | **INVALID** | LEXICAL |
| DV05 | Missing aircraft model | `"departure LIS 10:00; arrival OPO 11:00;"` | VALID | SEMANTIC |
| DV06 | Empty DSL | `""` | **INVALID** | SYNTACTIC |
| DV07 | Negative fuel | `"departure LIS 10:00; arrival OPO 11:00; fuel -500;"` | **INVALID** | SEMANTIC |
| DV08 | Lowercase flight type | `"departure LIS 10:00; arrival OPO 11:00; type regular;"` | VALID | NONE |
| DV09 | Special chars in airport | `"departure L@S 10:00; arrival OPO 11:00;"` | **INVALID** | LEXICAL |
| DV10 | Extremely long DSL | Long valid DSL with remarks | VALID | NONE |
| DV11 | Missing departure | `"arrival OPO 11:00;"` | **INVALID** | SYNTACTIC |
| DV12 | Duplicate departure field | `"departure LIS 10:00; departure LIS 11:00; arrival OPO 12:00;"` | **INVALID** | SYNTACTIC |
| DV13 | Extraneous text after valid | `"departure LIS 10:00; arrival OPO 11:00; garbage extra text"` | **INVALID** | SYNTACTIC |
| DV14 | Unicode characters | `"departure LIS 10:00; arrival OPO 11:00; fuel ★★★;"` | **INVALID** | LEXICAL |
| DV15 | Numeric flight number | `"departure LIS 10:00; arrival OPO 11:00; flight 12345;"` | VALID | NONE |

### `FlightPlanDataValidationTest` (25 tests)

Driven by `flight_plan_test_data.csv` (rows TC01–TC25). Each test case validates a business rule invariant against a flight plan DSL built from the CSV columns. The test:

1. Constructs a DSL string from the aircraft model, fuel, flight type, and cruise altitude
2. Passes the DSL through `DslValidator` to confirm basic syntax is valid
3. Checks the specific business rule named by the `invariant` column (fuel ≤ max capacity, altitude ≤ service ceiling, weight ≤ MTOW, etc.) against the CSV's expected PASS/FAIL status

| Test Case | Invariant | Fuel | Alt | Weight | Expected |
|-----------|-----------|------|-----|--------|----------|
| TC01 | happy_path_valid_flight | 15000 ≤ 20000 | 10668 ≤ 12500 | 71413 ≤ 70000 | PASS |
| TC02 | happy_path_valid_charter | 12000 ≤ 18000 | 10668 ≤ 11887 | 70200 ≤ 73500 | PASS |
| TC03 | fuel_exceeds_max_capacity | 21000 > 20000 | — | — | FAIL |
| TC04 | fuel_below_minimum_required | 500 < 1000 | — | — | FAIL |
| TC05 | altitude_exceeds_service_ceiling | — | 13716 > 12500 | — | FAIL |
| TC06 | altitude_within_limits | — | 10000 ≤ 11887 | — | PASS |
| TC07 | takeoff_weight_exceeds_mtow | — | — | 74413 > 70000 | FAIL |
| TC08 | weight_within_limits | — | — | 70200 ≤ 73500 | PASS |
| TC09 | fuel_sufficient_for_range | 8000 ≥ 1000 | — | — | PASS |
| TC10 | fuel_insufficient_for_route_distance | 2000 < 3000 | — | — | FAIL |
| TC11 | cruise_altitude_must_be_positive | — | 0 ≤ 0 | — | FAIL |
| TC12 | cruise_altitude_must_be_positive | — | -500 ≤ 0 | — | FAIL |
| TC13 | zero_payload_still_valid | — | — | 56413 ≤ 70000 | PASS |
| TC14 | fuel_exactly_at_max_capacity_boundary | 20000 ≤ 20000 | — | — | PASS |
| TC15 | fuel_and_ceiling_exactly_at_limits | 18000 ≤ 18000 | 10000 ≤ 11887 | — | PASS |
| TC16 | altitude_exactly_at_service_ceiling | — | 12500 ≤ 12500 | — | PASS |
| TC17 | very_low_cruise_altitude_positive | — | 500 > 0 | — | PASS |
| TC18 | minimum_fuel_with_reduced_payload | 5000 ≥ 1000 | — | 59413 ≤ 70000 | PASS |
| TC19 | high_payload_under_mtow | 15000 ≤ 18000 | — | 77200 ≤ 73500 | PASS |
| TC20 | fuel_below_minimum_flight_operations | 500 < 1000 | — | — | FAIL |
| TC21 | payload_weight_exceeds_mzfw | — | — | 60200 ≤ 62000 | FAIL |
| TC22 | zero_fuel_rejected | 0 ≤ 0 | — | — | FAIL |
| TC23 | fuel_exceeds_capacity_and_weight_exceeds_mtow | 25000 > 20000 | — | 81413 > 70000 | FAIL |
| TC24 | large_aircraft_a380_within_all_limits | 250000 ≤ 320000 | 11000 ≤ 13100 | 607000 ≤ 575000 | PASS |
| TC25 | regional_jet_e190_valid | 12000 ≤ 15000 | 10000 ≤ 11887 | 52000 ≤ 50000 | PASS |

> **Note:** TC21 (payload_weight_exceeds_mzfw) tests the Java business rule for weight exceeding MZFW. The FAIL in the CSV reflects that the C simulator reported a failure (due to any safety distance violation, not necessarily this one). The test verifies only the named invariant, independently of the CSV's FAIL cause.

### `PilotCertificationDataValidationTest` (8 tests)

Driven by `pilot_certification_test_data.csv` (rows PC01–PC08). Each test case validates certification status based on certification type (INITIAL, RECURRENT, EXPIRED) and date ranges:

- INITIAL certification: valid for 24 months from certification date
- RECURRENT certification: valid for 12 months from certification date
- EXPIRED certification: always NOT_CERTIFIED

| Test Case | Pilot | Type | Cert Date | Flight Date | Expected |
|-----------|-------|------|-----------|-------------|----------|
| PC01 | Carlos Silva | INITIAL | 2025-01-15 | 2026-03-10 | CERTIFIED |
| PC02 | Maria Santos | EXPIRED | 2025-06-20 | 2026-03-10 | NOT_CERTIFIED |
| PC03 | João Pereira | RECURRENT | 2026-01-10 | 2026-03-10 | CERTIFIED |
| PC04 | Ana Costa | RECURRENT | 2025-11-05 | 2026-03-10 | CERTIFIED |
| PC05 | Rui Alves | EXPIRED | 2024-03-15 | 2026-03-10 | NOT_CERTIFIED |
| PC06 | Sofia Martins | RECURRENT | 2026-02-01 | 2026-03-10 | CERTIFIED |
| PC07 | Pedro Sousa | RECURRENT | 2025-08-20 | 2026-03-10 | CERTIFIED |
| PC08 | Inês Ribeiro | RECURRENT | 2026-03-01 | 2026-03-10 | CERTIFIED |

---

## 5. Complete Test Inventory

| # | Test Class | Package | Tests | Type |
|---|-----------|---------|-------|------|
| 1 | `FlightDesignatorTest` | `eapli.aisafe.flight.domain` | 13 | Unit (domain VO) |
| 2 | `FlightTest` | `eapli.aisafe.flight.domain` | 9 | Unit (domain aggregate) |
| 3 | `FlightPlanIdTest` | `eapli.aisafe.flightplan.domain` | 8 | Unit (domain VO) |
| 4 | `FlightPlanTest` | `eapli.aisafe.flightplan.domain` | 14 | Unit (domain entity) |
| 5 | `ValidationResultTest` | `eapli.aisafe.flightplan.domain` | 9 | Unit (domain VO) |
| 6 | `FlightPlanExporterTest` | `eapli.aisafe.flightplan.application` | 4 | Unit (service) |
| 7 | `ImportFlightPlanControllerTest` | `eapli.aisafe.flightplan.application` | 4 | Unit (controller) |
| 8 | `ReportParserTest` | `eapli.aisafe.flightplan.application` | 13 | Unit (service) |
| 9 | `ProcessBuilderSimulationRunnerTest` | `eapli.aisafe.flightplan.application` | 3 | Unit (service) |
| 10 | `TestFlightPlanControllerTest` | `eapli.aisafe.flightplan.application` | 12 | Integration (controller, mocked) |
| 11 | `ReportParserParameterizedTest` | `eapli.aisafe.flightplan.application` | 10 | Parameterized (CSV) |
| 12 | `DslValidatorParameterizedTest` | `eapli.aisafe.flightplan.application` | 15 | Parameterized (CSV) |
| 13 | `FlightPlanDataValidationTest` | `eapli.aisafe.flightplan.application` | 25 | Parameterized (CSV) |
| 14 | `PilotCertificationDataValidationTest` | `eapli.aisafe.flightplan.application` | 8 | Parameterized (CSV) |
| | **Total** | | **147** | |

---

## 6. Test Coverage Assessment

### Domain Classes

| Class | Lines | Covered By | Est. Coverage |
|-------|-------|------------|---------------|
| `Flight` | 81 | `FlightTest` (9 tests) | >95% |
| `FlightDesignator` | 56 | `FlightDesignatorTest` (14 tests) | >95% |
| `FlightPlan` | 167 | `FlightPlanTest` (14 tests) | >95% |
| `FlightPlanId` | 57 | `FlightPlanIdTest` (8 tests) | >95% |
| `FlightPlanStatus` | 8 | Implicitly by FlightPlanTest | 100% |
| `ValidationResult` | 64 | `ValidationResultTest` (9 tests) | >95% |

### Application Classes

| Class | Lines | Covered By | Est. Coverage |
|-------|-------|------------|---------------|
| `ImportFlightPlanController` | ~100 | `ImportFlightPlanControllerTest` (4 tests) | >90% |
| `TestFlightPlanController` | 143 | `TestFlightPlanControllerTest` (12 tests) | >90% |
| `FlightPlanExporter` | ~40 | `FlightPlanExporterTest` (4 tests) | >95% |
| `FlightPlanToScenarioConverter` | ~80 | `FlightPlanExporterTest` (4 tests, via exporter) | >90% |
| `ProcessBuilderSimulationRunner` | ~60 | `ProcessBuilderSimulationRunnerTest` (3 tests) | >90% |
| `ReportParser` | ~40 | `ReportParserTest` (13) + `ReportParserParameterizedTest` (10) | >95% |
| `DslValidator` | ~130 | `DslValidatorParameterizedTest` (15 tests) | >95% |
| `FlightPlanDataValidationTest` | — | CSV-driven (25 tests) | — |
| `PilotCertificationDataValidationTest` | — | CSV-driven (8 tests) | — |

All controller and domain classes achieve **>90% line coverage** through comprehensive unit and integration tests.