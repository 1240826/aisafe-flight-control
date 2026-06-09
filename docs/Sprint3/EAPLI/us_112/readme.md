# US112 — Monthly Report Generation

## 1. Context

This task was assigned in Sprint 3. The objective is to allow a Flight Control Operator to generate
a monthly statistics report scoped to their Air Control Area. This implementation must be
foundational — the report structure and data collection must support future report types without
requiring redesign.

**Assigned to:** Cláudio Pinto

### 1.1 List of Issues

- Analysis: #74 
- Design: #74 
- Implement: #74 
- Test: #74 

---

## 2. Requirements

**US112** As a Flight Control Operator, I want to generate a monthly statistics report.
This will be one of a number of reports the system will have to generate in the future and this
work should be foundational to guarantee all follow a consistent branding and structure. Each
report type has a specific way of collecting data, specific sections and graphics.

### Acceptance Criteria

- **US112.1** The system must require the FLIGHT_CONTROL_OPERATOR role.
- **US112.2** The operator must select the target month and year.
- **US112.3** The report must include monthly flight statistics.
- **US112.4** The report must follow a consistent branding and structure reusable by future report
  types.
- **US112.5** The report must be saved to a file.

### Dependencies/References

- US030 — auth infrastructure.
- US061 — Collaborator domain with areaCode association (resolves FCO's ACA).
- US100/US111 — simulation and flight data that feeds the statistics.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — Extensible report generation design

> "We are implementing a monthly statistics report in Java. The design must be foundational for
> future report types (compliance, incident, etc.), each with specific data collection and sections,
> but all sharing consistent structure. Suggest a design pattern that enforces the shared structure
> while allowing each report type to define its own data collection."

**LLM suggestions adopted:**
- Strategy Pattern: `MonthlyReportDataProvider` interface defines the data collection contract;
  each report type has its own implementation without affecting the controller or other report types
- Report output saved to a file via the UI (console print + .txt file)

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested Template Method with an abstract `ReportGenerator` — the team chose Strategy
  instead because the professor explicitly required the Strategy Pattern ("the key") for US112.
  Strategy allows data providers to be swapped at runtime and composed more flexibly.
- The LLM proposed a separate `ReportWriter` abstraction — the team integrated file saving
  directly in the UI layer (`GenerateMonthlyReportUI`) since the output mechanism is trivial
  (text + write to file) and does not need its own abstraction.
- The LLM suggested generating HTML — the team opted for plain text (console + `.txt` file) as
  confirmed by the professor: "Text file is enough."
- The LLM did not mention ACA scoping — the team added `CollaboratorRepository` to resolve the
  authenticated FCO's area code, ensuring each FCO sees only their ACA's data.

---

### 3.1 Key Design Decisions

**Strategy Pattern for report data** — `MonthlyReportDataProvider` is an interface with a single
method `generateForMonth(YearMonth, AreaCode)`. `DatabaseMonthlyReportDataProvider` is the first
concrete implementation. Adding a new data source (e.g. file-based, external API) only requires
implementing the interface — no changes to the controller.

**ACA scoping via FCO's collaborator profile** — each Flight Control Operator is associated with
an Air Control Area through their `Collaborator` profile (US061). On report generation, the
controller resolves the current user's area code via `CollaboratorRepository.findBySystemUser()`
and passes it to the provider, which filters weather data by that area code.

**MonthlyReport value object** — encapsulates all statistics: total flights and flight plans,
flight plan status breakdown (DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED), test pass rate
(percentage), weather records (scoped to ACA), active pilots, total aircraft, and a flights-per-week
distribution.

**No external library for file output** — the report is printed to the console via
`MonthlyReport.toString()` and saved to `reports/monthly-report-YYYY-MM.txt` using
`java.nio.file.Files`. No PDF/HTML generation is required.

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `GenerateMonthlyReportUI` | `aisafe.app.backoffice.console` | Prompts for month/year; calls controller; prints report; saves to .txt file |
| `GenerateMonthlyReportController` | `aisafe.core` | Auth; resolves FCO's ACA via CollaboratorRepository; delegates to MonthlyReportDataProvider |
| `MonthlyReportDataProvider` (interface) | `aisafe.core` | Strategy contract — `generateForMonth(period, areaCode): MonthlyReport` |
| `DatabaseMonthlyReportDataProvider` | `aisafe.core` | Concrete strategy — aggregates data from FlightRepository, WeatherDataRepository, PilotRepository, AircraftRepository |
| `MonthlyReport` | `aisafe.core` | Domain value object with all statistics and formatted `toString()` |
| `CollaboratorRepository` | `aisafe.core` | Resolves the current FCO's area code (existing from US061) |
| `FlightRepository` | `aisafe.core` | Provides all flights for the period |
| `WeatherDataRepository` | `aisafe.core` | Provides weather data filtered by area code |
| `PilotRepository` | `aisafe.core` | Provides all pilots (active count) |
| `AircraftRepository` | `aisafe.core` | Provides all aircraft (total count) |

**Sequence Diagram:**

![Sequence Diagram](sd_us112_monthly_report_generation.svg)

### 4.2 Controller Tests

**AT1 — Report generated for valid month with correct ACA (US112.1, US112.2)**
- **Test:** `GenerateMonthlyReportControllerTest.ensureGeneratesReportForValidMonth`
- Given a logged-in FCO with a collaborator profile containing an area code,
- When `generateForMonth(period)` is called,
- Then the controller resolves the ACA via `CollaboratorRepository.findBySystemUser()`,
  delegates to `provider.generateForMonth(period, areaCode)`, and returns the report.

**AT2 — Report generation checks authorization (US112.1)**
- **Test:** `GenerateMonthlyReportControllerTest.ensureGenerateForMonthChecksAuthorization`
- Given a user without the FLIGHT_CONTROL_OPERATOR role,
- When `generateForMonth(period)` is called,
- Then `authz.ensureAuthenticatedUserHasAnyOf(FLIGHT_CONTROL_OPERATOR)` is invoked.

**AT3 — No authenticated user throws**
- **Test:** `GenerateMonthlyReportControllerTest.ensureGenerateForMonthThrowsWithoutSession`
- Given no authenticated user session,
- When `generateForMonth(period)` is called,
- Then `IllegalStateException` is thrown.

**AT4 — No collaborator profile throws**
- **Test:** `GenerateMonthlyReportControllerTest.ensureGenerateForMonthThrowsWithoutCollaborator`
- Given an authenticated user without a collaborator profile,
- When `generateForMonth(period)` is called,
- Then `IllegalStateException` is thrown.

### 4.3 Provider Tests

**AT5 — Provider aggregates flights and flight plans by period**
- **Test:** `DatabaseMonthlyReportDataProviderTest.ensureAggregatesFlightsAndFlightPlans`
- Given flights with flight plans in the target month,
- When `generateForMonth(period, areaCode)` is called,
- Then the report contains the correct total flights and flight plans.

**AT6 — Provider counts flight plans by status**
- **Test:** `DatabaseMonthlyReportDataProviderTest.ensureCountsFlightPlanStatuses`
- Given flight plans in DRAFT, IN_TEST, TEST_PASSED, and TEST_FAILED statuses,
- When the report is generated,
- Then each status count is correctly reflected.

**AT7 — Provider filters flights by month**
- **Test:** `DatabaseMonthlyReportDataProviderTest.ensureFiltersByMonth`
- Given flights in the target month and flights in a different month,
- When the report is generated,
- Then only flights in the target month are counted.

**AT8 — Provider filters weather data by area code**
- **Test:** `DatabaseMonthlyReportDataProviderTest.ensureFiltersWeatherByAreaCode`
- Given weather data for different area codes in the same period,
- When the report is generated,
- Then only weather data matching the given area code is counted.

**AT9 — Provider counts only active pilots**
- **Test:** `DatabaseMonthlyReportDataProviderTest.ensureCountsOnlyActivePilots`
- Given a mix of active and inactive pilots,
- When the report is generated,
- Then only active pilots are counted.

**AT10 — Empty data produces zero-filled report**
- **Test:** `DatabaseMonthlyReportDataProviderTest.ensureEmptyDataProducesZeroReport`
- Given no flights, no weather data, no pilots, no aircraft,
- When the report is generated,
- Then all counters are zero and no exception is thrown.

### 4.4 Domain Tests

**AT11 — MonthlyReport preserves all fields (US112.3)**
- **Test:** `MonthlyReportTest.ensureCreationPreservesFields`
- Given a `MonthlyReport` created with test data,
- Then all accessors return the correct values (period, totalFlights, totalFlightPlans, etc.).

**AT12 — MonthlyReport contains flight plan breakdown**
- **Test:** `MonthlyReportTest.ensureFlightPlanBreakdownIsCorrect`
- Given counts for DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED,
- When the report is created,
- Then each breakdown value is stored correctly.

**AT13 — All-zero report is valid**
- **Test:** `MonthlyReportTest.ensureAllZerosCanBePassed`
- Given all counters set to zero,
- Then the report is created without error.

**AT14 — Large values are handled**
- **Test:** `MonthlyReportTest.ensureLargeValuesAreStored`
- Given large counter values (millions),
- Then values are stored correctly.

**AT15 — toString contains all sections**
- **Test:** `MonthlyReportTest.ensureToStringContainsAllSections`
- Given a complete report,
- Then `toString()` contains the period, all statuses, pass rate, weather records,
  active pilots, aircraft count, and flights-per-week breakdown.

---

## 5. Implementation

- `eapli.aisafe.report.application.MonthlyReportDataProvider` — Strategy interface
- `eapli.aisafe.report.application.DatabaseMonthlyReportDataProvider` — Concrete strategy
- `eapli.aisafe.report.application.GenerateMonthlyReportController` — Controller with ACA resolution
- `eapli.aisafe.report.domain.MonthlyReport` — Domain value object with all statistics + `toString()`
- `eapli.aisafe.app.backoffice.console.presentation.report.GenerateMonthlyReportUI` — UI with
  console output and `.txt` file save

---

## 6. Integration/Demonstration

To demonstrate this user story:

1. Ensure the authenticated user has a Collaborator profile with an AreaCode (US061).
2. Ensure flight and flight plan data exists for a given month (US080/US085).
3. Ensure weather data exists for the FCO's Air Control Area (US041/US042).
4. Log in as a Flight Control Operator.
5. Select "Generate Monthly Report (US112)" and provide a month and year (e.g. 2026-06).
6. Verify the report is printed to the console and saved to
   `reports/monthly-report-YYYY-MM.txt`.

To demonstrate the Strategy Pattern (US112.4):

1. Create a new class implementing `MonthlyReportDataProvider` (e.g. for a different data source).
2. Plug it into `GenerateMonthlyReportController` (via constructor).
3. Verify that no existing class was modified and the new provider is used.

---

## 7. Observations

`MonthlyReportDataProvider` is the extension point for all future report data sources. Any new
data provider (file-based, external API, cached) must implement this interface — the controller
is unchanged.

The report is scoped to the FCO's Air Control Area via the `Collaborator` profile. This ensures
each operator only sees data relevant to their area.

The composite `toString()` formatting defines the report layout. Adding new sections to the report
only requires adding fields to `MonthlyReport` and updating `toString()` — the controller and
provider remain unchanged.
