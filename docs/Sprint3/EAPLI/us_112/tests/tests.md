# US112 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
The actual Java test files are under `core/src/test/java/eapli/aisafe/report/`.

---

## 1. MonthlyReport Domain

**File:** `core/src/test/java/eapli/aisafe/report/domain/MonthlyReportTest.java`
**Tests:** 12

Tests the `MonthlyReport` value object directly (no mocking).

| # | Test method | What it validates |
|---|-------------|-------------------|
| 1 | `ensureMonthlyReportIsCreated` | Report is non-null after construction |
| 2 | `ensurePeriodIsPreserved` | `period()` returns the YearMonth passed to constructor |
| 3 | `ensureTotalFlightsIsPreserved` | `totalFlights()` returns correct value |
| 4 | `ensureTotalFlightPlansIsPreserved` | `totalFlightPlans()` returns correct value |
| 5 | `ensureFlightPlansBreakdownIsPreserved` | Each status count (DRAFT/IN_TEST/TEST_PASSED/TEST_FAILED) is correct |
| 6 | `ensureWeatherRecordsIsPreserved` | `totalWeatherRecords()` returns correct value |
| 7 | `ensureActivePilotsIsPreserved` | `totalActivePilots()` returns correct value |
| 8 | `ensureTotalAircraftIsPreserved` | `totalAircraft()` returns correct value |
| 9 | `ensureAllZerosCanBePassed` | All-zero arguments create a valid report |
| 10 | `ensureLargeValuesAreStored` | Values up to millions are stored correctly |
| 11 | `ensureToStringContainsPeriod` | `toString()` contains the period string ("2026-06") |
| 12 | `ensureToStringContainsAllSections` | `toString()` contains all labels: MONTHLY REPORT, Flights, Flight Plans, DRAFT, IN TEST, TEST PASSED, TEST FAILED, Weather Records, Active Pilots, Total Aircraft |

---

## 2. GenerateMonthlyReportController

**File:** `core/src/test/java/eapli/aisafe/report/application/GenerateMonthlyReportControllerTest.java`
**Tests:** 4

Uses Mockito mocks for `AuthorizationService`, `CollaboratorRepository`, and
`MonthlyReportDataProvider`. Creates a real `SystemUser` for session simulation.

| # | Test method | What it validates |
|---|-------------|-------------------|
| 1 | `ensureDelegatesToProviderWithAreaCode` | Controller resolves FCO's ACA via `CollaboratorRepository.findBySystemUser()` and delegates to `provider.generateForMonth(period, areaCode)` |
| 2 | `ensureAuthorizationIsChecked` | `authz.ensureAuthenticatedUserHasAnyOf(FLIGHT_CONTROL_OPERATOR)` is called |
| 3 | `ensureThrowsWhenNoAuthenticatedUser` | Empty session → `IllegalStateException` |
| 4 | `ensureThrowsWhenNoCollaboratorProfile` | User exists but no collaborator profile → `IllegalStateException` |

---

## 3. DatabaseMonthlyReportDataProvider

**File:** `core/src/test/java/eapli/aisafe/report/application/DatabaseMonthlyReportDataProviderTest.java`
**Tests:** 6

Uses Mockito mocks for `FlightRepository`, `WeatherDataRepository`, `PilotRepository`,
and `AircraftRepository`. Creates real `Flight` objects for aggregation testing.

| # | Test method | What it validates |
|---|-------------|-------------------|
| 1 | `ensureGenerateForMonthAggregatesData` | Happy path: flights, flight plans, weather records, active pilots, and aircraft are all aggregated correctly |
| 2 | `ensureFiltersFlightsByMonth` | Only flights in the target month are counted; flights in other months are excluded |
| 3 | `ensureFiltersWeatherByMonthAndArea` | Weather data is filtered by area code (passed to `weatherRepo.findByAreaCode()`) |
| 4 | `ensureCountsFlightPlansByStatus` | Flight plans are counted by status: DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED — each correctly tallied |
| 5 | `ensureOnlyActivePilotsAreCounted` | Only pilots where `isActive() == true` are counted; inactive pilots excluded |
| 6 | `ensureEmptyReportWhenNoData` | No flights, weather, pilots, or aircraft → report with all zeros, no exception |
