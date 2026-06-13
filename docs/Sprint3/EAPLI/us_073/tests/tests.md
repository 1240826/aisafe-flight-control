# US073 — Unit Tests

## Domain Tests

### `FlightRouteTest` — Domain invariants

File: `aisafe.base/core/src/test/java/eapli/aisafe/flightroute/domain/FlightRouteTest.java`

Parameterized tests using CSV data for `FlightRoute` creation invariants:

| Test Case | Description | Expected |
|-----------|-------------|----------|
| Valid route creation | Name "TP123", origin OPO, destination LIS | Created as active |
| Origin equals destination | origin LIS, destination LIS | Throws exception |
| Null parameters | null name/company/origin/destination | Throws exception |

### `FlightRouteNameTest` — Value Object format validation

Tests the regex `[A-Z]{2}\d{1,4}`:
- Valid: "TP123", "RY42", "AA1"
- Invalid: "T12", "TP12345", "tp123", ""

### `CreateFlightRouteControllerTest` — Controller tests

File: `aisafe.base/core/src/test/java/eapli/aisafe/flightroute/application/CreateFlightRouteControllerTest.java`

Mock-based tests for controller orchestration:
- Valid creation delegates to repository save
- Duplicate route name throws IllegalArgumentException
- Authorization check is enforced
- All airports/companies methods delegate to repositories
