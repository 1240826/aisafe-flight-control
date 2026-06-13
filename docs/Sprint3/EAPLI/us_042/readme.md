# US042 — Import Bulk Weather Data

## 1. Context

This task was assigned in Sprint 3. The objective is to allow a Weather Person to import bulk
weather data from a CSV file into the system. The CSV uses European formatting (`,` as decimal
separator, `;` as column separator) and supports a header-based mapping of numeric ACA IDs to
AreaCodes.

**Assigned to:** Cláudio Pinto

### 1.1 List of Issues

- Analysis: #71 
- Design: #71 
- Implement: #71 
- Test: #71 

---

## 2. Requirements

**US042** As a Weather Person, I want to import bulk weather data into the system so that weather
information from multiple external providers can be aggregated for better accuracy.

### Acceptance Criteria

- **US042.1** The system must require the WEATHER_PERSON role.
- **US042.2** The system must support CSV as the import format.
- **US042.3** Each record in the file must be validated before being persisted.
- **US042.4** Invalid records must be reported without interrupting the import of valid ones.
- **US042.5** The system must be extensible to new data formats without modifying existing code.
- **US042.6** The source provider must be recorded for each imported WeatherData record.

### Dependencies/References

- US030 — auth infrastructure.
- US041 — Register weather data (Sprint 2): WeatherData domain object and repository already exist.
- US050 — AirControlArea must exist (WeatherData is registered for an area).

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — CSV import design

> "We are implementing a bulk weather data import feature in Java using DDD and the EAPLI framework.
> The initial format is CSV with European decimal separators and a header-based ACA mapping. Suggest
> a design that supports extensibility to new formats."

**LLM suggestions adopted:**
- Invalid records are collected and reported at the end rather than aborting the whole import

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested a `WeatherDataImporter` interface with separate implementations per format
  (Strategy pattern). The team opted for inline parsing in the controller because the CSV format
  is tightly coupled to the ACA header-mapping logic and the 12-column layout, and a separate
  importer abstraction would add complexity without immediate benefit. If a new format is needed
  in a future sprint, the import logic can be extracted at that point.
- The LLM proposed reading the source provider from a separate column — the team reads it from a
  configuration in the CSV header (`# ACA N = COD`), consistent with the `sourceProvider`
  attribute already on the WeatherData domain object (US041).
- The LLM proposed a batch `saveAll()` — replaced with individual `save()` per record so that
  partial imports succeed even when some records are invalid.

---

### 3.1 Key Design Decisions

**Inline CSV parsing in the controller** — the `ImportBulkWeatherDataController` reads the file
line by line, parses headers (`# ACA N = COD`), validates each data row (12 columns, European
decimal format), and persists valid records. Invalid rows are caught and counted without stopping
the import.

**ACA header mapping** — CSV lines starting with `#` followed by `ACA <numericId> = <AreaCode>`
establish a mapping between the numeric ID used in the file and the system's `AreaCode`. The
controller validates the AreaCode against the `AirControlAreaRepository` before importing.

**Validation before persistence** — each parsed record is validated by the `WeatherData` domain
constructor before being passed to the repository. Invalid records are skipped and collected for
reporting.

**ImportResult value object** — an inner class that encapsulates `imported`, `skipped`, and
`errors` (list of error messages) returned to the UI for display.

**Reuse of existing domain objects** — `WeatherData`, `WindCondition`, and
`WeatherDataRepository` were created in Sprint 2 (US041) and are reused directly. No domain
changes are required.

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `ImportBulkWeatherDataUI` | `aisafe.app.backoffice.console` | Prompts for file path; displays import summary |
| `ImportBulkWeatherDataController` | `aisafe.core` | Auth; reads CSV; parses headers and data rows; saves valid records; returns `ImportResult` |
| `AirControlAreaRepository` | `aisafe.core` | Validates that AreaCodes in header mapping exist |
| `WeatherDataRepository` | `aisafe.core` | Persists each valid `WeatherData` record (existing) |
| `ImportResult` (inner class) | `aisafe.core` | DTO with `imported`, `skipped`, `errors` |

**Sequence Diagram:**

![Sequence Diagram](sds/images/sd_us042_import_weather_data.svg)

### 4.2 Acceptance Tests

**AT1 — Valid CSV imported successfully (US042.2, US042.3)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureImportFromCsvSavesWeatherData`
- Given a valid CSV line with correct ACA header mapping,
- When the Weather Person imports the file,
- Then the record is persisted and the system reports 1 imported, 0 skipped.

**AT2 — Multiple valid lines are all imported**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureMultipleValidLinesAreAllImported`
- Given a CSV with two valid data lines and a correct ACA header,
- When the Weather Person imports the file,
- Then both records are persisted (`times(2)`).

**AT3 — Blank lines are skipped (US042.4)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureBlankLinesAreSkipped`
- Given a CSV with a blank line between valid records,
- When the Weather Person imports the file,
- Then blank lines are ignored and valid records are persisted.

**AT4 — Invalid column count is reported (US042.4)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureInvalidColumnCountIsSkipped`
- Given a CSV row with fewer than 12 columns,
- When the Weather Person imports the file,
- Then the row is skipped with error "Expected 12 columns".

**AT5 — Missing ACA header is reported**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureMissingAcaHeaderIsSkipped`
- Given a CSV with no `# ACA N = COD` header and a data row referencing ACA ID "1",
- When the Weather Person imports the file,
- Then the row is skipped with error "Unknown ACA ID: 1".

**AT6 — Invalid number format is reported (US042.4)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureInvalidNumberFormatIsSkipped`
- Given a CSV row with a non-numeric latitude value,
- When the Weather Person imports the file,
- Then the row is skipped with error "Invalid lat1".

**AT7 — Invalid date format is reported (US042.4)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureInvalidDateIsSkipped`
- Given a CSV row with a malformed date,
- When the Weather Person imports the file,
- Then the row is skipped with error "Invalid date/time".

**AT8 — Unknown ACA code in header throws**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureUnknownAcaInHeaderThrows`
- Given a CSV header with an AreaCode not registered in the system,
- When the Weather Person imports the file,
- Then the system throws `IllegalArgumentException`.

**AT9 — European decimal format is parsed correctly**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureEuropeanDecimalIsParsedCorrectly`
- Given a CSV row using comma as decimal separator (`28,75`),
- When the Weather Person imports the file,
- Then the wind speed is parsed as 28.75.

**AT10 — ImportResult structure (US042.4)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureResultToString`
- Given an `ImportResult` with 5 imported, 2 skipped, 2 errors,
- Then `toString()` returns "Imported: 5 | Skipped: 2 | Errors: 2".

**AT11 — Unauthorised user rejected (US042.1)**
- **Test:** `ImportBulkWeatherDataControllerTest.ensureImportChecksAuthorization`
- Given a user without the WEATHER_PERSON role,
- When they attempt to import,
- Then `authz.ensureAuthenticatedUserHasAnyOf(WEATHER_PERSON)` is invoked and the operation
  is rejected.

### 4.3 Domain Tests

- **WeatherDataTest (14 tests):** validates null/blank constraints on areaCode, windCondition,
  sourceProvider, recordedDateTime; accessors return correct values.
- **WindConditionTest (27 tests):** validates speed > 0, direction [0, 360), lat/lon ranges,
  altitude >= 0.

---

## 5. Implementation

- `eapli.aisafe.weatherdata.application.ImportBulkWeatherDataController` — main controller with
  inline CSV parsing and `ImportResult` inner class
- `eapli.aisafe.app.backoffice.console.presentation.weatherdata.ImportBulkWeatherDataUI` — UI
  prompting for file path and displaying results
- Test filenames used for acceptance tests are under `docs/Sprint3/EAPLI/us_042/tests/`:
  - `weather_data_test.csv` (mixed valid/invalid)
  - `weather_data_valid_only.csv`
  - `weather_data_headers_only.csv`
  - `weather_data_empty.csv`

---

## 6. Integration/Demonstration

To demonstrate this user story:

1. Bootstrap or manually register an air control area (US050).
2. Prepare a valid CSV file with at least one `# ACA N = COD` header and 12-column data rows
   (semicolon-separated, comma as decimal separator).
3. Log in as a Weather Person.
4. Select "Import Bulk Weather Data from CSV (US042)", provide the CSV file path.
5. Verify that valid records are persisted and the import summary lists the invalid ones without
   interrupting the import.

Example CSV:
```
# ACA 121 = LPPC
121;43,840454;-9,795711;40,225;-7,9501;0;1000;90;28,75;22/06/2026;05:00;08:15
```

---

## 7. Observations

The `ImportBulkWeatherDataController` handles the CSV format (European decimal, 12 columns,
ACA header mapping) entirely within the controller. If a new format is required in a future
sprint, the common file-reading and validation logic can be extracted into a strategy/hierarchy
at that point, following the Open/Closed Principle.
