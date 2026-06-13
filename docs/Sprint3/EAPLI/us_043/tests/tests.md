# US043 — Unit Tests

## Domain Tests

### `ConsultWeatherDataControllerTest` — Parameterized file-based tests

File: `aisafe.base/core/src/test/resources/us043/consult_weather_data_test.csv`

Tests controller authorization, repository delegation, and input validation using parameterized CSV data.

| Test Case | Description | Expected |
|-----------|-------------|----------|
| Valid area code and date | `consultWeatherData("LPPC", 2026-06-15)` | Returns matching weather data |
| Null area code | `consultWeatherData(null, date)` | Throws exception |
| Empty area code | `consultWeatherData("", date)` | Throws exception |
| Null date | `consultWeatherData("LPPC", null)` | Throws exception |
| Unauthorized role | User without WEATHER_PERSON/PILOT/FCO role | Authorization check fails |

### `WeatherDataTest` — Domain invariants (existing)

Tests for `WeatherData` domain invariants: null fields, blank fields, wind condition validation, etc.

### `WindConditionTest` — Value object invariants (existing)

Tests for `WindCondition`: speed > 0, direction [0, 360), lat/lon ranges, altitude >= 0.
