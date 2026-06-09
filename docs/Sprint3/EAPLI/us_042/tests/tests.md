# US042 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
The actual Java test files are under `core/src/test/java/eapli/aisafe/weatherdata/`.

---

## 1. WindCondition Value Object

**File:** `core/src/test/java/eapli/aisafe/weatherdata/domain/WindConditionTest.java`
**Tests:** 27

Validates all constraints of the `WindCondition` value object:

| Category | Tests | Constraint |
|----------|-------|------------|
| `speedKnots` | 3 | strictly > 0 (zero and negative throw) |
| `directionDegrees` | 4 | [0, 360) — 0 and 359 valid, 360 and -1 throw |
| `latitude` | 4 | [-90, 90] inclusive |
| `longitude` | 4 | [-180, 180] inclusive |
| `altitudeMetres` | 2 | >= 0 |

---

## 2. WeatherData Aggregate Root

**File:** `core/src/test/java/eapli/aisafe/weatherdata/domain/WeatherDataTest.java`
**Tests:** 16

Validates the `WeatherData` domain entity:

| Tests | Description |
|-------|-------------|
| 5 | `areaCode` / `windCondition` / `sourceProvider` / `recordedDateTime` cannot be null |
| 2 | `sourceProvider` cannot be blank or empty |
| 1 | `sourceProvider` is trimmed |
| 1 | `temperatureCelsius` negative is valid |
| 1 | `temperatureCelsius` positive is valid |
| 6 | All accessors return correct values |

---

## 3. ImportBulkWeatherDataController

**File:** `core/src/test/java/eapli/aisafe/weatherdata/application/ImportBulkWeatherDataControllerTest.java`
**Tests:** 13

Uses Mockito mocks for `AuthorizationService`, `WeatherDataRepository`, and `AirControlAreaRepository`.
Creates temporary CSV files via `@TempDir`.

| # | Test method | What it validates |
|---|-------------|-------------------|
| 1 | `ensureImportFromCsvSavesWeatherData` | Happy path: valid CSV line with ACA header is parsed and saved |
| 2 | `ensureImportChecksAuthorization` | Authorization check is invoked |
| 3 | `ensureMultipleValidLinesAreAllImported` | Two valid lines with different ACAs, both saved (`times(2)`) |
| 4 | `ensureBlankLinesAreSkipped` | Blank lines ignored, only non-blank line imported |
| 5 | `ensureInvalidColumnCountIsSkipped` | Row with 6 columns (expected 12) → error "Expected 12 columns" |
| 6 | `ensureMissingAcaHeaderIsSkipped` | No `# ACA N = COD` header → error "Unknown ACA ID" |
| 7 | `ensureInvalidNumberFormatIsSkipped` | Non-numeric latitude → error "Invalid lat1" |
| 8 | `ensureInvalidDateIsSkipped` | Malformed date → error "Invalid date/time" |
| 9 | `ensureUnknownAcaInHeaderThrows` | Unknown ACA code in header → `IllegalArgumentException` |
| 10 | `ensureEuropeanDecimalIsParsedCorrectly` | Comma as decimal separator `15,5` → parsed correctly |
| 11 | `ensureResultToString` | `ImportResult.toString()` → "Imported: 5 | Skipped: 2 | Errors: 2" |
| 12 | `ensureResultHasErrorsReturnsTrueWhenErrorsExist` | Non-empty error list → `hasErrors()` returns true |
| 13 | `ensureResultHasErrorsReturnsFalseWhenNoErrors` | Empty error list → `hasErrors()` returns false |

---

## 4. Test CSV Files

Located in `docs/Sprint3/EAPLI/us_042/tests/`:

| File | Purpose |
|------|---------|
| `weather_data_test.csv` | Mixed valid/invalid (52 lines): 14 valid + 15 invalid rows |
| `weather_data_valid_only.csv` | All valid (13 lines: header + 12 records) |
| `weather_data_headers_only.csv` | Only header line (1 line) |
| `weather_data_empty.csv` | Empty file (0 lines) |
