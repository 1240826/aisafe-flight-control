# US121 – Create a Valid Flight Plan from a File

## 1. Context

This user story integrates the Flight DSL (developed in US120) with the EAPLI domain layer, enabling a Pilot to import a `.flightplan` file and automatically create the corresponding `FlightPlan` entity inside the `Flight` aggregate in the system.

The DSL pipeline — lexer, parser, semantic validation — was already implemented in US120. This user story adds the final step: building EAPLI domain entities from the parsed AST and persisting them.

**Assigned to:** Whole team (LPROG)

### 1.1 List of Issues

- Analysis: #86
- Design: #86
- Implement: #86
- Test: #86

---

## 2. Requirements

**US121** – As a Pilot, I want to create a valid flight plan from a file, so that I can import flight plans defined in the Flight DSL.

### Acceptance Criteria

- **US121.1** The system must accept a `.flightplan` file as input.
- **US121.2** The file must be processed through the existing DSL pipeline (lexer, parser, semantic validation).
- **US121.3** The system must validate that all referenced entities exist in the database: airports, aircraft registration, pilot ID, and flight route.
- **US121.4** The pilot referenced in the file must belong to the same air transport company as the aircraft.
- **US121.5** On success, the `FlightPlan` is persisted with status `DRAFT`.
- **US121.6** On validation failure, the system must report all errors — both DSL errors and cross-reference errors — without persisting anything.
- **US121.7** Access must be restricted to users with the `FLIGHT_CONTROL_OPERATOR` or `ATC_COLLABORATOR` role.

### Dependencies / References

- US120 — Flight DSL specification and validation (grammar, lexer, parser, semantic validation)
- US073 — Create a flight route (origin/destination airports must exist)
- US075 — Add a pilot (pilot must exist and belong to the correct company)
- US080 — Create a flight plan (shared `FlightPlan` aggregate design)
- US070 — Add an aircraft (aircraft must exist and be active)

---

## 3. Analysis

### Conceptual Model

The import pipeline combines the DSL processing chain with EAPLI domain construction:

```
.flightplan file
    ↓
FlightPlanRunner.parse()      ← US120: lexical + syntactic + semantic validation
    ↓
ParseTree / Visitor output
    ↓
FlightPlanAssembler           ← NEW: builds domain entities from parsed data
    ↓
Cross-reference validation    ← NEW: queries repositories for existence/consistency
    ↓
FlightPlanRepository.save()
```

The `Flight` aggregate root contains `FlightPlan` as an entity:

```
Flight (Aggregate Root)
├── FlightDesignator designator
├── FlightType type                        ← REGULAR | CHARTER
├── DepartureSchedule schedule             ← RegularSchedule or CharterSchedule
├── Pilot pilotId                          ← cross-ref to Pilot (assigned)
├── Aircraft aircraftReg                   ← cross-ref to Aircraft
├── FlightRoute routeId                    ← cross-ref to FlightRoute
└── List<FlightPlan> flightPlans
        └── FlightPlan (Entity)
            ├── FlightPlanId flightId
            ├── FlightPlanStatus status (DRAFT → IN_TEST → TEST_PASSED/TEST_FAILED)
            ├── dslContent (raw String)            ← stored for re-validation (US085)
            ├── FuelQuantity fuel
            ├── int cruiseAltitude
            ├── LocalDateTime departureTime
            ├── LocalDateTime arrivalTime
            └── List<Leg> legs
                    ├── LegId legId
                    ├── AirportIATA depAirport
                    ├── LocalDateTime depDatetime
                    ├── DayOfWeek depDay (regular only)
                    ├── AirportIATA arrAirport
                    ├── LocalDateTime arrDatetime
                    ├── FuelQuantity fuelKg
                    └── List<Segment> segments
                            ├── SegmentId segId
                            ├── Coordinates from
                            ├── Coordinates to
                            └── List<AltitudeSlot> altitudes
```

### Domain Connections

This US bridges two modules:

| Module | Role |
|--------|------|
| `aisafe.lprog` (LPROG) | Provides the DSL parsing pipeline — `FlightPlanRunner`, generated lexer/parser, `SemanticValidationListener` |
| `eapli.aisafe.flightplan.domain` (EAPLI) | Defines the `FlightPlan` entity, value objects, and `FlightPlanAssembler` service |
| `eapli.aisafe.flight.domain` (EAPLI) | Defines the `Flight` aggregate root — contains `FlightPlan` entities, accessed via `FlightRepository` |
| `eapli.aisafe.pilot.domain` (EAPLI) | Provides `PilotRepository` for pilot existence, company, and certification validation |
| `eapli.aisafe.aircraft.domain` (EAPLI) | Provides `AircraftRepository` for aircraft existence validation |
| `eapli.aisafe.airport.domain` (EAPLI) | Provides `AirportRepository` for airport code validation |
| `eapli.aisafe.flightroute.domain` (EAPLI) | Provides `FlightRouteRepository` for route existence validation |

### Key Design Decisions

- **Entity inside Flight aggregate:** `FlightPlan` is an entity inside the `Flight` aggregate root (not a standalone aggregate). Access is always via `FlightRepository`. Shared with US080 (manual creation) — one domain model, two input paths.
- **Cross-references by identity:** All references to other aggregates use their identity VOs (e.g., `RegistrationNumber`, `RouteName`), never direct object references — consistent with the DDD pattern used throughout the project.
- **Pilot certification validation (C07):** During import, the system validates that the pilot is certified for the assigned aircraft model (`pilot.certifications.contains(aircraftModelCode)`). This aligns with US085 R7.
- **DSL content stored:** The raw DSL content is stored as `dslContent` in `FlightPlan` (C03, C10), enabling re-validation in US085.
- **Validation separation:** DSL-level validation (syntax, semantics) is handled by US120 code. Cross-reference validation (entity existence, company consistency, pilot certification) is handled by the `FlightPlanAssembler` in the application layer.
- **Assembler pattern:** A `FlightPlanAssembler` service converts the DSL visitor output into domain entities, keeping the controller lean and testable.

---

## 4. Design

### 4.1 Realization

**Classes to create/modify:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `Flight` | `eapli.aisafe.flight.domain` | Aggregate root — contains FlightPlan entities |
| `FlightDesignator` | `eapli.aisafe.flight.domain` | Value Object — flight identifier (e.g. "TP0123") |
| `FlightPlan` | `eapli.aisafe.flightplan.domain` | **Entity** inside Flight aggregate |
| `FlightPlanId` | `eapli.aisafe.flightplan.domain` | Value Object wrapping the flight plan identifier |
| `FlightPlanStatus` | `eapli.aisafe.flightplan.domain` | Enum: `DRAFT`, `IN_TEST`, `TEST_PASSED`, `TEST_FAILED` |
| `FuelQuantity` | `eapli.aisafe.flightplan.domain` | Value Object: amount + unit |
| `FlightType` | `eapli.aisafe.flight.domain` | Enum: `REGULAR`, `CHARTER` |
| `Leg` | `eapli.aisafe.flightplan.domain` | Local entity within FlightPlan |
| `LegId` | `eapli.aisafe.flightplan.domain` | Value Object for leg identification |
| `Segment` | `eapli.aisafe.flightplan.domain` | Local entity within Leg |
| `SegmentId` | `eapli.aisafe.flightplan.domain` | Value Object for segment identification |
| `AltitudeSlot` | `eapli.aisafe.flightplan.domain` | Value Object: altitude + corridor width |
| `FlightRepository` | `eapli.aisafe.flight.repositories` | Repository interface for Flight aggregate |
| `FlightPlanAssembler` | `eapli.aisafe.flightplan.application` | Converts DSL parse tree → domain entities |
| `ImportFlightPlanFromFileController` | `eapli.aisafe.flightplan.application` | Controller orchestrating the import |
| `ImportFlightPlanFromFileUI` | `eapli.aisafe.app.pilot.console` | Console UI for the Pilot |

**Sequence Diagram — Import Flight Plan from File:**

![Sequence Diagram — Import Flight Plan from File](sd_us121_import_flightplan.svg)

### 4.2 Acceptance Tests

#### Acceptance Test 1 — Valid regular flight plan is imported successfully

**Objective:** Validate that a correctly formatted regular `.flightplan` file creates a `FlightPlan`.

**Procedure:**
1. Log in as a Pilot.
2. Select "Import Flight Plan from File".
3. Provide a valid regular `.flightplan` file (e.g., `valid_direct_flight.flightplan`).
4. Confirm.

**Expected Result:** The system reports success and the new `FlightPlan` appears with status `DRAFT`.

**Refers to Acceptance Criteria:** US121.1, US121.2, US121.5

---

#### Acceptance Test 2 — Valid charter flight plan is imported successfully

**Objective:** Validate that a charter `.flightplan` file is accepted.

**Procedure:**
1. Log in as a Pilot.
2. Import a valid charter `.flightplan` file (datetime only, no day).

**Expected Result:** The system reports success and creates a `FlightPlan` with `CHARTER` type.

**Refers to Acceptance Criteria:** US121.1, US121.2, US121.5

---

#### Acceptance Test 3 — Non-existent aircraft is rejected

**Objective:** Validate cross-reference check for aircraft.

**Procedure:**
1. Create a `.flightplan` file referencing a non-existent aircraft registration (e.g., `XX-XXX`).
2. Attempt to import the file.

**Expected Result:** The system rejects with an error: "Aircraft 'XX-XXX' not found in the system".

**Refers to Acceptance Criteria:** US121.3

---

#### Acceptance Test 4 — Non-existent pilot is rejected

**Objective:** Validate cross-reference check for pilot.

**Procedure:**
1. Create a `.flightplan` file referencing a non-existent pilot ID.
2. Attempt to import the file.

**Expected Result:** The system rejects with an error: "Pilot not found".

**Refers to Acceptance Criteria:** US121.3

---

#### Acceptance Test 5 — Pilot company mismatch is rejected

**Objective:** Validate company consistency between pilot and aircraft.

**Procedure:**
1. Create a `.flightplan` file where the pilot belongs to company "TP" but the aircraft belongs to company "FR".
2. Attempt to import the file.

**Expected Result:** The system rejects with: "Pilot does not belong to the same company as the aircraft".

**Refers to Acceptance Criteria:** US121.4

---

#### Acceptance Test 6 — Invalid DSL file is rejected with errors

**Objective:** Validate that DSL validation errors are reported.

**Procedure:**
1. Create a `.flightplan` file with a syntax error (e.g., missing semicolon).
2. Attempt to import the file.

**Expected Result:** The system reports DSL validation errors and nothing is persisted.

**Refers to Acceptance Criteria:** US121.2, US121.6

---

#### Acceptance Test 7 — Unauthorized role is blocked

**Objective:** Validate that only authorized roles can import files.

**Procedure:**
1. Log in as a `BACKOFFICE_OPERATOR`.
2. Attempt to access the Import Flight Plan feature.

**Expected Result:** The system rejects with an authorization error.

**Refers to Acceptance Criteria:** US121.7

---

#### Acceptance Test 8 — Pilot not certified for aircraft model is rejected (C07)

**Objective:** Validate pilot certification against aircraft model.

**Procedure:**
1. Create a `.flightplan` file referencing a pilot who is NOT certified for the aircraft model (e.g., pilot certified only for A320 but aircraft is B738).
2. Attempt to import the file.

**Expected Result:** The system rejects with: "Pilot is not certified for aircraft model B738".

**Refers to Acceptance Criteria:** C07, US075

---

#### Acceptance Test 9 — Missing DSL content is rejected

**Objective:** Validate that the flight plan stores DSL content for future re-validation.

**Procedure:**
1. Create an empty `.flightplan` file.
2. Attempt to import the file.

**Expected Result:** The system rejects with a DSL validation error and nothing is persisted.

**Refers to Acceptance Criteria:** US121.2, US085

---

## 5. Implementation

### Main Files

| File | Location | Status |
|------|----------|--------|
| `Flight.java` | `eapli.aisafe.flight.domain/` | New (or extend if exists from Sprint 2) |
| `FlightDesignator.java` | `eapli.aisafe.flight.domain/` | New |
| `FlightPlan.java` | `eapli.aisafe.flightplan.domain/` | New |
| `FlightPlanId.java` | `eapli.aisafe.flightplan.domain/` | New |
| `FlightPlanStatus.java` | `eapli.aisafe.flightplan.domain/` | New |
| `FuelQuantity.java` | `eapli.aisafe.flightplan.domain/` | New |
| `FlightType.java` | `eapli.aisafe.flight.domain/` | New |
| `Leg.java` | `eapli.aisafe.flightplan.domain/` | New |
| `LegId.java` | `eapli.aisafe.flightplan.domain/` | New |
| `Segment.java` | `eapli.aisafe.flightplan.domain/` | New |
| `SegmentId.java` | `eapli.aisafe.flightplan.domain/` | New |
| `AltitudeSlot.java` | `eapli.aisafe.flightplan.domain/` | New |
| `FlightRepository.java` | `eapli.aisafe.flight.repositories/` | New |
| `FlightPlanAssembler.java` | `eapli.aisafe.flightplan.application/` | New |
| `ImportFlightPlanFromFileController.java` | `eapli.aisafe.flightplan.application/` | New |
| `ImportFlightPlanFromFileUI.java` | `eapli.aisafe.app.pilot.console/` | New |

### Changes to existing files

| File | Change |
|------|--------|
| `RepositoryFactory.java` | Add `flightRepository()` method |
| `JpaRepositoryFactory.java` | Wire `JpaFlightRepository` (Flight aggregate) |
| `InMemoryRepositoryFactory.java` | Wire `InMemoryFlightRepository` |
| `FlightPlanRunner.java` | May need a method that returns structured data instead of printing |

---

## 6. Integration / Demonstration

1. Log in as a Pilot (FLIGHT_CONTROL_OPERATOR) — e.g., `fco1` / `Password1`.
2. Navigate to "Import Flight Plan from File".
3. Provide a path to a `.flightplan` file (e.g., one of the example files in `aisafe.dsl/src/main/resources/examples/`).
4. The system:
   - Parses the file through the DSL pipeline.
   - Validates cross-references against the database.
   - Persists the `FlightPlan` with status `DRAFT`.
5. Verify the flight plan appears in the system.

### Demonstration prerequisites

- Bootstrap must load demo data: airports, aircraft, pilots, routes.
- US073 (FlightRoute), US075 (Pilot), US070 (Aircraft) must be implemented.
- US120 (DSL) must be complete and compiling.

### Integration points

| Component | Integration |
|-----------|-------------|
| `aisafe.lprog` | Called by `ImportFlightPlanFromFileController` via `FlightPlanRunner.run()` |
| `eapli.aisafe.flightplan.domain` | New entity (inside Flight aggregate), follows same DDD patterns |
| `eapli.aisafe.flight.domain` | Flight aggregate root — contains FlightPlan, accessed via `FlightRepository` |
| `eapli.aisafe.pilot.domain` | Queries `PilotRepository` for pilot existence, company, and certification validation (C07) |
| `eapli.aisafe.aircraft.domain` | Queries `AircraftRepository` for aircraft existence and company validation |
| `eapli.aisafe.airport.domain` | Queries `AirportRepository` for airport code validation |
| `eapli.aisafe.flightroute.domain` | Queries `FlightRouteRepository` for route existence validation |

---

## 7. Observations

- The `FlightPlan` entity is shared with US080 (manual creation via UI) and is part of the `Flight` aggregate root (confirmed by professor). Both use cases write to the same database table through separate controllers.
- The `FlightPlanAssembler` keeps the controller free of DSL concerns — if the DSL changes, only the assembler needs updating.
- Cross-reference validation is deliberately separated from DSL validation: DSL errors are caught early (before any DB queries), while cross-reference errors are caught after parsing succeeds. This gives the user clear, separated error messages.
- Pilot certification validation (C07) is performed during import: the pilot must be certified for the aircraft model referenced in the DSL file.
- The raw DSL content is stored as `dslContent` in the `FlightPlan` entity (C03, C10), enabling re-validation in US085.
- The `.flightplan` example files from `aisafe.dsl/src/main/resources/examples/` can be reused for integration testing.
- No changes to the ANTLR grammar are expected — the grammar already supports all constructs needed for flight plan creation.
- Generative AI tools (Claude) were used to support the design of the assembler pattern and the separation of validation concerns.
