# US011 — Aggregate Justification

## 1. Context

This task was assigned in Sprint 1 (Sprint A). It is the first time this task is being developed. It depends directly on US010 (Domain Model) and provides explicit justification for each aggregate design decision, demonstrating the invariant each aggregate enforces.

### 1.1 List of Issues

- Analysis: #20
- Design: #20
- Implement: N/A — design artefact
- Test: N/A

---

## 2. Requirements

*US011* As Project Manager, I want the team to illustrate one representative scenario per aggregate with a sequence diagram and a short explanation, so that the aggregate's responsibility is demonstrated through an invariant it enforces. This justification will make aggregate design decisions more explicit and easier to review by the project manager.

*Acceptance Criteria:*

- US011.1 One sequence diagram per aggregate must be provided.
- US011.2 Each sequence diagram must demonstrate a business invariant enforced by the aggregate root.
- US011.3 A short justification must accompany each diagram explaining the aggregate boundary and the invariant shown.

*Dependencies/References:*

- US010 — The domain model must be complete before aggregate justification can be produced.

---

## 3. Analysis

### 3.1 What is an Aggregate?

An aggregate is a cluster of entities and value objects with a single root entity that controls all access to the internal objects. The boundary is defined by business invariants that must be enforced atomically.

The rules applied in the design:

- Nothing outside the aggregate boundary can hold a reference to anything inside — only roots are referenced externally.
- Only aggregate roots can be obtained directly with database queries (via Repositories).
- A delete operation removes everything within the aggregate boundary at once.
- When any change within the aggregate is committed, all invariants of the whole aggregate must be satisfied.
- One use case should only update one aggregate (ACID within aggregate, BASE between aggregates).

### 3.2 Aggregate Justification

See the dedicated document: docs/Sprint1/us_011/aggregate-justification.md

---

## 4. Design

### 4.1. Realization

Sequence diagrams illustrating the invariant enforcement for each aggregate:

| Aggregate | Sequence Diagram | Invariant Demonstrated |
|---|---|---|
| Manufacturer | docs/Sprint1/us_011/sd_manufacturer.puml | Name must not be empty |
| EngineModel | docs/Sprint1/us_011/sd_enginemodel.puml | Name + manufacturer unique (US056) |
| AircraftModel | docs/Sprint1/us_011/sd_aircraftmodel.puml | Same engine cannot be added twice (US057) |
| Aircraft | docs/Sprint1/us_011/sd_aircraft.puml | Registration number unique worldwide (US070) |
| AirControlArea | docs/Sprint1/us_011/sd_aircontrolarea.puml | AreaCode unique in system (US050) |
| Airport | docs/Sprint1/us_011/sd_airport.puml | IATA code unique worldwide (US052) |
| AirTransportCompany | docs/Sprint1/us_011/sd_airtransportcompany.puml | Company name unique (US060) |
| Collaborator | docs/Sprint1/us_011/sd_collaborator.puml | SecurityClearance must be active |
| Pilot | docs/Sprint1/us_011/sd_pilot.puml | Cannot deactivate with assigned flights (US077) |
| FlightRoute | docs/Sprint1/us_011/sd_flightroute.puml | RouteName unique with format (US073) |
| Flight | docs/Sprint1/us_011/sd_flight.puml | FlightDesignator unique; schedule matches FlightType |
| WeatherData | docs/Sprint1/us_011/sd_weatherdata.puml | recordedDateTime valid |
| Simulation | docs/Sprint1/us_011/sd_simulation.puml | SimulationTimeRange start < end (US100) |

### 4.2. Acceptance Tests

*Test 1 — Each aggregate enforces its invariant*

For each aggregate, verify that attempting to violate the invariant results in an exception thrown by the root entity, not by an external service or controller.

---

## 5. Implementation

This user story produces design artefacts only.

Major commits: #20 

---

## 6. Integration/Demonstration

The aggregate justifications inform the implementation of domain classes in subsequent sprints. Each justification defines:

- Which classes belong inside the aggregate boundary
- Which invariant the root enforces
- How external aggregates reference this aggregate (root only, by ID)

These patterns must be followed consistently in code to satisfy CO3 (DDD Tactical Patterns).

---

## 7. Observations

Each aggregate in this system modifies only one aggregate per use case, consistent with the rule: "One use case should only update one aggregate." Cross-aggregate references are read-only — for validation or navigation — never for writing to two aggregates in the same transaction. This ensures ACID within the aggregate and BASE between aggregates.