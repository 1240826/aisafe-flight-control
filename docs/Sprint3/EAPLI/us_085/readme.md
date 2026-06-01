# US085 — Test/Validate Flight Plan

## 1. Context

This task is assigned in **Sprint 3** as part of EAPLI (with cross-cutting dependencies on LPROG and SCOMP).
It is the first time this feature is being developed. The objective is to allow a Pilot to test/validate
a flight plan they have created, combining DSL re-validation (LPROG) with physics simulation (SCOMP/C).

**Issue:** #85
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
to JSON and invoke an external C simulator via ProcessBuilder. How should this be structured?"

**LLM suggestions adopted:**
- `FlightPlanValidationService` orchestrates the two-phase validation pipeline
- DSL re-validation calls the existing `FlightPlanRunner` from the `aisafe.dsl` module
- JSON export is handled by a dedicated `FlightPlanExporter` service
- C simulator invocation is isolated behind a `SimulationRunner` interface (testable with mocks)

**Decisions made by the team:**
- The C simulator is invoked via `ProcessBuilder` — no JNI or socket integration
- The C simulator output file path is configurable via application properties
- The DSL is re-validated every time (not cached) to ensure the latest grammar is applied

### 3.1 Key Design Decisions

| Decision | Rationale | Source |
|----------|-----------|--------|
| FlightPlan stores raw DSL content as String | Needed for US085 re-validation and export | C03 |
| DSL re-validation uses existing LPROG `FlightPlanRunner` | Reuses proven validation pipeline, avoids duplication | C03 |
| FlightPlan → JSON export for C simulator | C simulator reads JSON, not DSL | C14 |
| C invocation via `ProcessBuilder` | Looser coupling than JNI; matches file-based handoff | C14 |
| Two-phase validation: DSL first, then C | Fail fast: don't run C simulator if DSL is invalid | C03 |
| `SimulationRunner` interface | Enables unit testing of the controller without a real C binary | C14 |
| Pilot certification validated as part of US085 | Pilot must be certified for assigned aircraft model | C07 |
| Report file stored permanently in filesystem + content in DB | Not transient per client | C14 |
| Use PostgreSQL with PostGIS for final deployment | Simplifies coordinate queries | C01 |

### 3.2 Validation Pipeline

```
FlightPlanValidationService.validate(flightPlan, pilotCertifications)
    │
    ├── Phase 0 — Pilot Certification Check (C07)
    │   ├── Verify pilot is certified for FlightPlan.aircraftModel
    │   ├── If NOT certified → abort with certification error (status remains DRAFT)
    │   └── If certified → proceed to Phase 1
    │
    ├── Phase 1 — DSL Re-validation (LPROG)
    │   ├── Call FlightPlanRunner.run(dslContent)
    │   ├── If INVALID → return TEST_FAILED with DSL errors
    │   └── If VALID → proceed to Phase 2
    │
    ├── Phase 2 — JSON Export + C Simulation (EAPLI + SCOMP)
    │   ├── FlightPlanExporter.toJson(flightPlan) → scenario.json
    │   ├── SimulationRunner.run(scenario.json) → report file
    │   ├── Parse report file → extract pass/fail + violation details
    │   └── Return TEST_PASSED or TEST_FAILED
    │
    ├── Phase 3 — Report Persistence (C14)
    │   ├── Store report file permanently in filesystem
    │   ├── Persist filePath + content in Simulation aggregate (DB)
    │   └── Report is NOT transient — available for historical consultation
    │
    └── Record result on FlightPlan
        ├── Update status (TEST_PASSED / TEST_FAILED)
        ├── Store ValidationResult with reasons
        └── Save to repository
```

---

## 4. Design

### 4.1 Sequence Diagrams

- **Happy path:** `sd_us085_test_flight_plan.puml` — pilot certified → DSL passes → JSON export → C simulator → report parsed → TEST_PASSED
- **Failure paths:** `sd_us085_validation_failures.puml` — pilot not certified, DSL fails, C simulator reports violations

### 4.2 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `Flight` | `eapli.aisafe.flight.domain` | Aggregate root — identity: `FlightDesignator`. Contains flight plans (Sprint 2 domain model) |
| `FlightPlan` | `eapli.aisafe.flightplan.domain` | **Entity** inside Flight aggregate — stores DSL, status, validation result |
| `FlightPlanStatus` | `eapli.aisafe.flightplan.domain` | Enum: DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED |
| `FlightPlanId` | `eapli.aisafe.flightplan.domain` | Value Object — flight designator |
| `FuelQuantity` | `eapli.aisafe.flightplan.domain` | Value Object — amount + unit |
| `ValidationResult` | `eapli.aisafe.flightplan.domain` | Value Object — pass/fail + reasons list |
| `FlightPlanValidationService` | `eapli.aisafe.flightplan.application` | Orchestrates multi-phase validation (R1-R7) |
| `FlightPlanExporter` | `eapli.aisafe.flightplan.application` | Converts FlightPlan → JSON for C simulator |
| `SimulationRunner` | `eapli.aisafe.flightplan.application` | Interface for invoking C simulator (real or mock) |
| `ProcessBuilderSimulationRunner` | `eapli.aisafe.flightplan.application` | Real implementation using ProcessBuilder |
| `SimulationReport` | `eapli.aisafe.simulation.domain` | Value Object — path, rawOutput, isPassed, violationCount (exists in domain model) |
| `ReportParser` | `eapli.aisafe.flightplan.application` | Parses C simulator report file → SimulationReport |
| `TestFlightPlanController` | `eapli.aisafe.flightplan.application` | Auth + orchestration |
| `FlightRepository` | `eapli.aisafe.flight.repositories` | Repository interface for Flight aggregate |
| `JpaRepositoryFactory` | `eapli.aisafe.persistence.jpa` | Add `flightRepository()` method |
| `InMemoryRepositoryFactory` | `eapli.aisafe.persistence.inmemory` | Add `flightRepository()` method |
| `PilotRepository` | `eapli.aisafe.pilot.repositories` | For loading pilot certifications (R7) |

### 4.3 Acceptance Tests

**AT1 — Valid flight plan passes both validation phases (US085.1, US085.2)**

Given a flight plan with valid DSL content, valid fuel quantity, and valid altitude,
When the Pilot requests validation,
Then the DSL is re-validated successfully, the C simulator runs, and the final status is TEST_PASSED.

**AT2 — Invalid DSL is rejected before C simulation (US085.4)**

Given a flight plan with syntactically invalid DSL content,
When the Pilot requests validation,
Then the DSL re-validation fails, the C simulator is NOT invoked, and the status is TEST_FAILED
with DSL error messages.

**AT3 — Insufficient fuel detected by C simulator (US085.5)**

Given a flight plan with valid DSL but insufficient fuel for the route,
When the Pilot requests validation,
Then the DSL passes, the C simulator runs, and the report indicates failure → status TEST_FAILED.

**AT4 — Unauthorized user is rejected (US030)**

Given a user without the PILOT role,
When they attempt to validate a flight plan,
Then the system denies access with an authorization error.

**AT5 — Pilot not certified for aircraft model is rejected (C07)**

Given a flight plan assigned to a pilot who is NOT certified for the aircraft model,
When the Pilot requests validation,
Then the validation fails before DSL/C simulation with a certification error,
and the status remains DRAFT.

**AT6 — Report is persisted for historical consultation (C14)**

Given a validated flight plan that passed the C simulator,
When the validation completes,
Then the C simulator's report is stored both in the filesystem and in the database
(file path + content as CLOB) for future reference.

---

## 5. Implementation

**Key new files:**

- `eapli.aisafe.flight.domain.Flight` — aggregate root (may already exist from Sprint 2)
- `eapli.aisafe.flightplan.domain.FlightPlan` — entity inside Flight aggregate
- `eapli.aisafe.flightplan.domain.FlightPlanStatus` — enum
- `eapli.aisafe.flightplan.domain.FlightPlanId` — value object
- `eapli.aisafe.flightplan.domain.FuelQuantity` — value object
- `eapli.aisafe.flightplan.domain.ValidationResult` — value object
- `eapli.aisafe.flightplan.application.FlightPlanValidationService` — validation orchestrator
- `eapli.aisafe.flightplan.application.FlightPlanExporter` — JSON export
- `eapli.aisafe.flightplan.application.SimulationRunner` — C invocation interface
- `eapli.aisafe.flightplan.application.ProcessBuilderSimulationRunner` — real implementation
- `eapli.aisafe.flightplan.application.ReportParser` — parses C report
- `eapli.aisafe.flightplan.application.TestFlightPlanController` — application controller
- `eapli.aisafe.pilot.domain.Pilot` — aggregate root (add `Set<AircraftModelCode> certifications`)
- `eapli.aisafe.pilot.repositories.PilotRepository` — repository interface
- `eapli.aisafe.flight.repositories.FlightRepository` — repository interface (add query for flight plans by pilot)

---

## 6. Integration/Demonstration

```
┌──────────┐   DSL text   ┌──────────────┐   JSON file   ┌────────────┐
│  US080/  │─────────────>│  US085 Java  │──────────────>│  C Sim     │
│  US081   │  (stored)    │  Validation  │  (export)     │  (SCOMP)   │
└──────────┘              └──────────────┘               └────────────┘
                                   │                            │
                                   │ report file                │ report file
                                   │ (parsed)                   │ (written)
                                   v                            v
                           ┌──────────────┐
                           │  FlightPlan  │
                           │  status +    │
                           │  result      │
                           └──────────────┘
```

1. Bootstrap or create a flight plan via US080 (DRAFT status).
2. Log in as the Pilot assigned to the flight.
3. Select "Test/Validate Flight Plan" and choose the flight.
4. System re-validates the DSL → exports to JSON → invokes C simulator.
5. C simulator runs physics and produces report.
6. System parses the report and updates the flight plan status.
7. Pilot sees the validation result (PASS/FAIL with reasons).

---

## 7. Observations

- The C simulator binary path should be configurable (application.properties).
- The JSON export format must match what the C simulator's `json_parser.h` expects.
- The `SimulationRunner` interface allows unit testing without a real C binary.
- DSL re-validation depends on the `aisafe.dsl` module — ensure it is a Maven dependency.
- Pilot certification check (R7, from C07) requires FlightPlan to expose the assigned pilot's certifications.
- The C simulator report file is stored permanently (not transient) — both filesystem path and content are persisted (C14).
- PostgreSQL with PostGIS is recommended for the final deployment to simplify coordinate queries (C01).
- The FlightPlan aggregate must store `dslContent` as a raw String (from US080/US081) for re-validation in Phase 1.
- Weather data changes (US082) void the previous test result — the flight plan returns to DRAFT status.
- Team coordination: Jaime (US080) creates FlightPlan with DSL; Cláudio (US082) adds weather data; LPROG team (US120) provides `FlightPlanRunner`; SCOMP team provides the C simulator binary.
