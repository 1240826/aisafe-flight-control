# US074 — Unit Tests

## Domain Tests

### `FlightRouteTest` — Deactivation invariants

File: `aisafe.base/core/src/test/java/eapli/aisafe/flightroute/domain/FlightRouteTest.java`

Parameterized tests using CSV data for deactivation:

| Test Case | Description | Expected |
|-----------|-------------|----------|
| Active route deactivation | `deactivate(FUTURE_DATE)` | Becomes inactive |
| Already inactive route | `deactivate()` twice | Throws IllegalStateException |
| Null deactivation date | `deactivate(null)` | Throws exception |

### `DeleteFlightRouteControllerTest` — Controller orchestration

File: `aisafe.base/core/src/test/java/eapli/aisafe/flightroute/application/DeleteFlightRouteControllerTest.java`

Mock-based tests (with file-based parameterized scenarios):

| Test | Description | Expected |
|------|-------------|----------|
| No planned flights | `deactivateRoute("TP123", date)` | Saved as inactive |
| Planned flights exist | Future flights on route | Throws IllegalStateException |
| Route not found | Non-existent route name | Throws IllegalArgumentException |
| Already inactive | Deactivate twice | Throws IllegalStateException |
| Null parameters | null route name or date | Throws exception |
| Authorization | Active routes listing | Auth check enforced |
