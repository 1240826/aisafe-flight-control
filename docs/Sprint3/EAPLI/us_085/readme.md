# US085 — Test/Validate Flight Plan

## 1. Context

This task is assigned in **Sprint 3** as part of EAPLI (with cross-cutting dependencies on LPROG and SCOMP).
It is the first time this feature is being developed. The objective is to allow a Pilot (Flight Control Operator) to test/validate
a flight plan they have created, combining DSL re-validation (LPROG) with physics simulation (SCOMP/C).

**Assigned to:** Fábio Costa (EAPLI + LAPR4)

### 1.1 List of Issues

- Analysis: #74
- Design: #74
- Implement: #74
- Test: #74

---

## 2. Requirements

**US085** As Pilot, I want to test/validate a flight I've made.

### Acceptance Criteria

- **US085.1** The validation must include validation of the flight plan described using the DSL and its test.
- **US085.2** The test component must be implemented in the C language.
- **US085.3** The system must re-validate the stored DSL (lexical, syntactic, semantic) before running the C simulator.
- **US085.4** If DSL validation fails, the C simulator is not invoked and the flight plan status is set to TEST_FAILED with the DSL errors.
- **US085.5** If DSL validation passes, the flight plan data is exported to JSON, the C simulator is invoked, and its output is read to determine the final result.
- **US085.6** The validation result (pass/fail) is recorded and the flight plan status is updated accordingly.

### Dependencies/References

- **US080** — Flight plan must exist (Jaime Simões)
- **US083/US120** — DSL specification and validation (LPROG, whole team)
- **US082** — Weather data may be added before testing (Cláudio Pinto)
- **US100-US103** — C simulation engine (SCOMP)
- **US030** — Authentication and authorization infrastructure

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.

**Prompt 1:** "In a DDD Java system, a Pilot wants to validate a flight plan. The validation has two
steps: (1) re-validate the stored DSL using an ANTLR-based validator, and (2) export the flight plan
to JSON and invoke an external C simulator. How should this be structured?"

**Decisions made by the team:**
- The DSL is re-validated every time (not cached) to ensure the latest grammar is applied
- `TestFlightPlanController` orchestrates the full pipeline; dedicated services handle each concern (DSL validation, JSON export, C invocation, report parsing)
- Three communication modes are supported (see `communication-architecture.md`): local `ProcessBuilder`, TCP socket to a remote `sim_server`, and WSL bridge via `aisafe-simulator.cmd`

### 3.1 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| FlightPlan is an `@Entity` inside Flight aggregate (per Sprint 3 domain model) | FlightPlan is not a standalone aggregate root; Flight owns FlightPlan lifecycle |
| Flight aggregate is minimal for US085 | Only `FlightDesignator` + `@OneToMany FlightPlan`; US080 adds Pilot, Aircraft, Route later |
| FlightPlan stores raw DSL content as String | Needed for US085 re-validation and export |
| DSL validation uses ANTLR grammar (`FlightPlan.g4`) in a **3-phase pipeline** (lexical, syntactic, semantic) | Matches LPROG US083/US120 specification; integrated from `aisafe.dsl` module |
| Import UI uses `ImportFlightPlanController` for ANTLR-based validation at import time | US081/121: only valid DSL may be imported into the system |
| FlightPlan → structured JSON export via `FlightPlanToScenarioConverter` | C simulator expects detailed scenario JSON with coordinates, segments, flight profile |
| `FlightPlanExporter` falls back to simple `{ID, FlightPlanDSL}` for non-ANTLR DSL | Backwards compatibility with existing test plans |
| C invocation abstracted via `SimulationRunner` interface | Supports 3 modes: local ProcessBuilder, TCP socket to sim_server, WSL bridge (see `communication-architecture.md`) |
| `ProcessBuilderSimulationRunner` — local subprocess | Direct simulator execution on same machine (Linux / WSL) |
| `SocketSimulationRunner` — TCP to remote sim_server | Simulator runs on a separate VM; JSON sent over TCP, report received |
| Runner selection via `createRunner()` | Checks `aisafe.simulator.host` system property; if set → socket mode, else → ProcessBuilder |
| Weather file support via `buildWeatherFile()` | US082 weather data is exported to JSON and sent alongside scenario |
| Departure time mismatch check | Before simulation, validates the DSL departure time matches the Flight entity's departure time |
| Multi-flight scenario testing via `testScenario()` | Allows testing several flight plans in a single C simulator run |
| Two-phase validation: DSL first, then C | Fail fast: don't run C simulator if DSL is invalid |
| `FlightPlanExporter` dedicated to JSON serialization | Isolates format changes |
| `ProcessBuilderSimulationRunner` / `SocketSimulationRunner` use configurable timeout | Prevents hanging if simulator crashes |
| Role-gated to `FLIGHT_CONTROL_OPERATOR` | Follows US030 authorization infrastructure |
| FlightPlan status machine: DRAFT → IN_TEST → TEST_PASSED/TEST_FAILED | Clear lifecycle for validation workflow |
| Controller dual API (flightDesignator+flightPlanId / flightPlanId only) | UI lists flights first, then flight plans; remote API may only have flightPlanId |
| View past results via `ViewTestResultsUI` | Menu option 3 in Flight Plans — browses flight plans with TEST_PASSED/TEST_FAILED status and non-null report content |

### 3.2 Validation Pipeline — Single Flight

```
TestFlightPlanController.testFlightPlan(flightDesignator, flightPlanId)
    │
    ├── 1. Authorize — ensure user has FLIGHT_CONTROL_OPERATOR role
    │
    ├── 2. Load Flight from repository (or throw if not found)
    │
    ├── 3. Load FlightPlan from Flight (or throw if not found)
    │
    ├── 4. Check status == DRAFT (or return failure)
    │
    ├── 5. Departure time check (checkDepartureTime)
    │   ├── Parse departure time from DSL content (regex)
    │   ├── Compare with Flight.departureTime
    │   ├── If mismatch → markAsInTest() + recordTestResult(false) → save → return failure
    │   └── If match → proceed
    │
    ├── 6. DSL Validation (DslValidator.validate)
    │   ├── If INVALID → markAsInTest() + recordTestResult(false) → save → return failure
    │   └── If VALID → proceed
    │
    ├── 7. markAsInTest() + save
    │
    ├── 8. JSON Export (FlightPlanExporter.exportForSimulator)
    │
    ├── 9. Build weather file (buildWeatherFile) if flight has weatherDataId
    │
    ├── 10. C Simulation (SimulationRunner.run)
    │   ├── Chosen mode: SocketSimulationRunner or ProcessBuilderSimulationRunner
    │   ├── If fails → resetToDraft() + save → return failure
    │   └── If succeeds → proceed
    │
    ├── 11. Parse Report (ReportParser.parse)
    │
    └── 12. Record Result (FlightPlan.recordTestResult) + save → return result
```

### 3.3 Validation Pipeline — Multi-Flight Scenario

```
TestFlightPlanController.testScenario(entries)
    │
    ├── 1. Authorize — ensure user has FLIGHT_CONTROL_OPERATOR role
    │
    ├── 2. For each entry:
    │   ├── Skip if status is not DRAFT/TEST_PASSED/TEST_FAILED
    │   ├── resetToDraft() if not DRAFT
    │   ├── Departure time check (skip if mismatch)
    │   ├── DSL validation (skip if invalid)
    │   └── markAsInTest() + save
    │
    ├── 3. If no valid entries → return failure
    │
    ├── 4. Build combined JSON array (merge individual exports)
    │
    ├── 5. Build weather file from first entry's flight (buildWeatherFile)
    │
    ├── 6. C Simulation (runner.run(json, weatherFilePath))
    │   ├── If fails → resetToDraft() for all entries
    │   └── If succeeds → proceed
    │
    ├── 7. Parse per-flight results from report (ReportParser.parse)
    │
    └── 8. For each entry: recordTestResult(parsedPerFlight) + save
```

---

## 4. Design

### 4.1 Sequence Diagrams

- **Happy path:** `sds/uml/sd_us085_test_flight_plan.puml` — DSL passes → JSON export → C simulator → report parsed → TEST_PASSED
- **Failure paths:** `sds/uml/sd_us085_validation_failures.puml` — DSL fails, C simulator reports violations

### 4.2 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `Flight` | `eapli.aisafe.flight.domain` | Aggregate root `@Entity` — holds `@OneToMany FlightPlan`, identity `FlightDesignator` |
| `FlightDesignator` | `eapli.aisafe.flight.domain` | `@Embeddable` Value Object — format `xxn(n)(n)(n)(a)` (e.g. TP1234) |
| `FlightPlan` | `eapli.aisafe.flightplan.domain` | `@Entity` inside Flight aggregate — stores DSL, status, validation result |
| `FlightPlanStatus` | `eapli.aisafe.flightplan.domain` | Enum: DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED |
| `FlightPlanId` | `eapli.aisafe.flightplan.domain` | `@Embeddable` Value Object — alphanumeric ID (max 20 chars) |
| `ValidationResult` | `eapli.aisafe.flightplan.domain` | Value Object — pass/fail + reasons list |
| `FlightRepository` | `eapli.aisafe.flight.repositories` | Domain repository interface (with `findByFlightPlanId`) |
| `JpaFlightRepository` | `eapli.aisafe.persistence.jpa` | JPA implementation |
| `InMemoryFlightRepository` | `eapli.aisafe.persistence.inmemory` | In-memory implementation |
| `SimulationRunner` | `eapli.aisafe.flightplan.application` | Interface — abstracts C simulator invocation (`run(json)` / `run(json, weatherPath)`) |
| `SocketSimulationRunner` | `eapli.aisafe.flightplan.application` | TCP client — sends JSON to `sim_server` over socket, receives report |
| `ProcessBuilderSimulationRunner` | `eapli.aisafe.flightplan.application` | Local subprocess — invokes C simulator via `ProcessBuilder` with temp files |
| `ImportFlightPlanController` | `eapli.aisafe.flightplan.application` | `@UseCaseController` — ANTLR 3-phase pipeline at import time; creates FlightPlan with DRAFT status |
| `TestFlightPlanController` | `eapli.aisafe.flightplan.application` | `@UseCaseController` — auth + orchestration (DSL re-validate, departure check, export, C sim, report parse) |
| `FlightPlanExporter` | `eapli.aisafe.flightplan.application` | Converts FlightPlan → JSON for C simulator; uses `FlightPlanToScenarioConverter` with legacy fallback |
| `FlightPlanToScenarioConverter` | `eapli.aisafe.flightplan.application` | Parses DSL with ANTLR; outputs structured scenario JSON (legs, coordinates, flight profile) for C simulator |
| `ReportParser` | `eapli.aisafe.flightplan.application` | Parses C simulator text output (PASS/FAIL + violation count + per-flight results) |
| `DslValidator` | `eapli.aisafe.flightplan.application` | Validates DSL content rules (altitude, speed, waypoint, engine, wake, passengers, non-ASCII) |
| `ImportFlightPlanUI` | `eapli.aisafe.ui.flightplan` | Console UI — file path + Flight Plan ID input; displays per-phase (lexer/parser/semantic) error feedback |
| `TestFlightPlanUI` | `eapli.aisafe.ui.flightplan` | Console UI — extends `AbstractUI`, role-gated to FLIGHT_CONTROL_OPERATOR |
| `ViewTestResultsUI` | `eapli.aisafe.ui.flightplan` | Console UI — browse past test reports (menu option 3 in Flight Plans) |

### 4.3 Acceptance Tests

**AT1 — Valid flight plan passes both validation phases (US085.1, US085.2)**

Given a flight plan with valid DSL content,
When the Pilot requests validation,
Then the DSL is re-validated successfully, the C simulator runs, and the final status is TEST_PASSED.

**AT2 — Invalid DSL is rejected before C simulation (US085.4)**

Given a flight plan with syntactically invalid DSL content,
When the Pilot requests validation,
Then the DSL re-validation fails, the C simulator is NOT invoked, and the status is TEST_FAILED
with DSL error messages.

**AT3 — Insufficient fuel / physics failure detected by C simulator (US085.5)**

Given a flight plan with valid DSL but that fails the physics simulation,
When the Pilot requests validation,
Then the DSL passes, the C simulator runs, and the report indicates failure → status TEST_FAILED.

**AT4 — Unauthorized user is rejected (US030)**

Given a user without the FLIGHT_CONTROL_OPERATOR role,
When they attempt to validate a flight plan,
Then the system denies access with an authorization error.

---

## 5. Implementation

**All files (24 source files):**

| Package | Files |
|---------|-------|
| `eapli.aisafe.flight.domain` | `Flight`, `FlightDesignator` |
| `eapli.aisafe.flight.repositories` | `FlightRepository` |
| `eapli.aisafe.flightplan.domain` | `FlightPlan`, `FlightPlanId`, `FlightPlanStatus`, `ValidationResult` |
| `eapli.aisafe.flightplan.application` | `ImportFlightPlanController`, `TestFlightPlanController`, `FlightPlanExporter`, `FlightPlanToScenarioConverter`, `ProcessBuilderSimulationRunner`, `ReportParser`, `DslValidator` |
| `eapli.aisafe.persistence.jpa` | `JpaFlightRepository` |
| `eapli.aisafe.persistence.inmemory` | `InMemoryFlightRepository` |
| `eapli.aisafe.ui.flightplan` | `ImportFlightPlanUI`, `TestFlightPlanUI` |
| `eapli.aisafe.ui` | `MainMenu` (updated) |
| `eapli.aisafe.bootstrap` | `AISafeDemoDataBootstrapper` (updated) |
| `eapli.aisafe.infrastructure.persistence` | `RepositoryFactory` (updated) |
| `eapli.aisafe.persistence.jpa` | `JpaRepositoryFactory` (updated) |
| `eapli.aisafe.persistence.inmemory` | `InMemoryRepositoryFactory` (updated) |
| `persistence.xml` | `META-INF/persistence.xml` (updated) |

**Unit tests (14 test classes, 147 tests):**

| Test Class | Tests |
|------------|-------|
| `FlightDesignatorTest` | 13 |
| `FlightTest` | 9 |
| `FlightPlanIdTest` | 8 |
| `FlightPlanTest` | 14 |
| `ValidationResultTest` | 9 |
| `FlightPlanDataValidationTest` | 25 |
| `FlightPlanExporterTest` | 4 |
| `ImportFlightPlanControllerTest` | 4 |
| `TestFlightPlanControllerTest` | 12 |
| `ReportParserTest` | 13 |
| `ReportParserParameterizedTest` | 10 |
| `ProcessBuilderSimulationRunnerTest` | 3 |
| `PilotCertificationDataValidationTest` | 8 |
| `DslValidatorParameterizedTest` | 15 |

---

## 6. Integration/Demonstration

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Backoffice Console (Flight Control Operator)                           │
│                                                                          │
│  MainMenu > Flights                                                     │
│       │                                                                  │
│       ├── 1. Import Flight Plan                                          │
│       │      │                                                          │
│       │      v                                                          │
│       │  ImportFlightPlanUI                                              │
│       │      │  enter file path + FlightPlan ID                         │
│       │      │  ImportFlightPlanController.importFlightPlan(dsl, id)    │
│       │      v                                                          │
│       │  ImportFlightPlanController                                      │
│       │      │                                                          │
│       │      ├── ANTLR Lexer (FlightPlanLexer)                          │
│       │      ├── ANTLR Parser (FlightPlanParser)                        │
│       │      ├── Semantic Validation (SemanticValidationListener)       │
│       │      ├── Extract FlightDesignator from parse tree               │
│       │      └── FlightRepository.save(flight) ──→ DB                   │
│       │                                                                  │
│       └── 2. Test Flight Plan                                            │
│              │                                                          │
│              v                                                          │
│          TestFlightPlanUI                                                │
│              │  allFlights() → list flights                             │
│              │  select flight → list its flight plans                   │
│              │  controller.testFlightPlan(flightDesig, planId)          │
│              v                                                          │
│          TestFlightPlanController                                        │
│              │                                                          │
│              ├── FlightRepository.findByDesignator(flight)              │
│              ├── Flight.flightPlan(flightPlanId) → FlightPlan           │
│              ├── DslValidator.validate(dslContent)                      │
│              ├── FlightPlanExporter.exportForSimulator(flightPlan)      │
│              │     ├── FlightPlanToScenarioConverter.convert() (ANTLR)  │
│              │     └── fallback: {ID, FlightPlanDSL} (legacy)           │
│              ├── SimulationRunner.run(json, weatherFile) ──→ C Sim     │
│              │     ├── SocketSimulationRunner — TCP to sim_server      │
│              │     └── ProcessBuilderSimulationRunner — local subprocess│
│              ├── ReportParser.parse(reportContent)                      │
│              └── FlightRepository.save(flight) ──→ DB (cascade to plans)│
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

1. Bootstrap data creates 3 demo flights (TP1234, TP5678, TP9012), each with one flight plan using valid ANTLR grammar DSL content.
2. Log in as `fco1` / `Password1` (Flight Control Operator role).
3. Navigate: **Flights > 1. Import Flight Plan**.
4. Enter the path to a `.flightplan` file and a Flight Plan ID.
5. System runs ANTLR 3-phase validation: lexical → syntactic → semantic. Per-phase errors are displayed with line:column details.
6. On success, the flight plan is created with DRAFT status and a summary is shown.
7. Navigate: **Flights > 2. Test Flight Plan**.
8. Select a flight, then select its flight plan from the list.
9. System re-validates the DSL → exports to structured JSON (via converter or legacy fallback) → invokes C simulator (if configured).
10. If C simulator is not available, the runner throws (handled gracefully).
11. Result is displayed (PASS/FAIL with details) and the flight plan status is updated.

---

## 7. Observations

- C simulator invocation is abstracted via the `SimulationRunner` interface with two implementations:
  - **`ProcessBuilderSimulationRunner`**: local subprocess with temp files. Used when no `aisafe.simulator.host` is set.
  - **`SocketSimulationRunner`**: TCP client connecting to a remote `sim_server`. Used when `aisafe.simulator.host` is set (default in `run-backoffice.bat`). See `communication-architecture.md`.
- The `ProcessBuilderSimulationRunner` expects local binary `aisafe-simulator` (or configured via `aisafe.simulator.executable`).
- The `SocketSimulationRunner` connects to `aisafe.simulator.host:aisafe.simulator.port` (default `localhost:9999`).
- Both runners apply a configurable timeout (default 120s, set via `aisafe.simulator.timeout`).
- The `sim_server` is a standalone C executable (built with `make sim_server`) that listens on a TCP port, forks+execs `./simulation` for each request.
- DSL validation at import time uses the **full ANTLR 3-phase pipeline** (`FlightPlanRunner` from `aisafe.dsl`): `FlightPlanErrorListener` (lexer+parser errors with line:column), `SemanticValidationListener` (R2–R11 semantic rules), `FlightPlanPrinterVisitor` (summary).
- The `aisafe.dsl` dependency is declared in `aisafe.base/core/pom.xml` for access to `FlightPlanRunner`, ANTLR listeners, and visitors.
- `ImportFlightPlanController` returns a `DslValidationResult` record (record type) containing per-phase error lists, the `FlightDesignator`, and the created `FlightPlan` (or null on failure).
- `FlightPlanToScenarioConverter` produces structured JSON for the C simulator: `[{ID, Type, Route, DepartureTime, DepartureTZ, Leg[{Departure, Arrival, Fuel, Flight Profile {Climb[], Descend[], Cruise}, Segments[]}]}]`. It has a `canConvert()` predicate to gracefully fall back.
- `FlightPlanExporter.exportForSimulator()` tries `FlightPlanToScenarioConverter.convert()` first; falls back to legacy `{ID, FlightPlanDSL}` format if DSL doesn't parse.
- `FlightPlan` is an `@Entity` **inside** the `Flight` aggregate root (per Sprint 3 domain model). All persistence goes through `FlightRepository` with `CascadeType.ALL`. This was refactored from an earlier design where FlightPlan was a standalone aggregate root.
- The Flight aggregate for US085 is **minimal** — only `FlightDesignator` identity + `@OneToMany FlightPlan`. When US080 is implemented, the full Flight aggregate (with `FlightType`, `DepartureSchedule`, `Pilot`, `Aircraft`, `Route`) extends this class.
- `ValidationResult` in `eapli.aisafe.flightplan.domain` is a separate class from `eapli.aisafe.simulation.domain.ValidationResult` (which is an enum).
- Team coordination: Jaime (US080) creates FlightPlan with DSL; Cláudio (US082) adds weather data; LPROG team (US120) provides `FlightPlanRunner` + ANTLR grammar; SCOMP team provides the C simulator binary.
