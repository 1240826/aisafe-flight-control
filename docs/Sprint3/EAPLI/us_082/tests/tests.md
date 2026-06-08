# US082 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
The actual Java test files are under `core/src/test/java/eapli/aisafe/flight/`.

---

## 1. AddWeatherToFlightController

**File:** `core/src/test/java/eapli/aisafe/flight/application/AddWeatherToFlightControllerTest.java`
**Tests:** 10

Uses Mockito mocks for `AuthorizationService`, `FlightRepository`, `WeatherDataRepository`,
and `AirControlAreaRepository`.

### allFlights

| # | Test method | What it validates |
|---|-------------|-------------------|
| 1 | `ensureAllFlightsDelegatesToRepo` | `flightRepo.findAll()` is invoked and flights returned |
| 2 | `ensureAllFlightsChecksAuthorization` | `authz.ensureAuthenticatedUserHasAnyOf(PILOT)` is called |

### flightByDesignator

| # | Test method | What it validates |
|---|-------------|-------------------|
| 3 | `ensureFlightByDesignatorReturnsFlight` | Known designator returns the matching `Flight` |
| 4 | `ensureFlightByDesignatorThrowsForUnknown` | Unknown designator throws `IllegalArgumentException` |
| 5 | `ensureFlightByDesignatorChecksAuthorization` | Authorization check is performed |

### assignWeather

| # | Test method | What it validates |
|---|-------------|-------------------|
| 6 | `ensureAssignWeatherSavesFlight` | Weather data assigned via `flight.assignWeatherData()`, flight persisted |
| 7 | `ensureAssignWeatherWithUnknownFlightThrows` | Non-existent flight designator → `IllegalArgumentException` |
| 8 | `ensureAssignWeatherWithUnknownWeatherThrows` | Non-existent weather ID → `IllegalArgumentException` |
| 9 | `ensureAssignWeatherChecksAuthorization` | Authorization check is performed |
| 10 | `ensureAssignWeatherIsIdempotent` | Re-assigning same weather data succeeds without error |

---

## 2. Flight Domain — assignWeatherData()

**File:** `core/src/test/java/eapli/aisafe/flight/domain/FlightTest.java`
**Tests:** 3 related to assignWeatherData (out of 18 total in FlightTest)

| # | Test method | What it validates |
|---|-------------|-------------------|
| 1 | `ensureAssignWeatherDataAssociatesWeatherToFlight` | After `assignWeatherData(id)`, `weatherDataId()` returns the expected value (US082.3) |
| 2 | `ensureAssignWeatherDataResetsTestedFlightPlansToDraft` | Flight plans in TEST_PASSED/TEST_FAILED are reset to DRAFT (US082.4) |
| 3 | `ensureAssignWeatherDataDoesNotChangeDraftFlightPlans` | Flight plans already in DRAFT remain DRAFT (US082.5) |
