# US010 — Domain Model Revision (Sprint 3)

## 1. Context

US010 (Domain Model) is a living artefact meant to evolve with each sprint. This document records every change made to the domain model during Sprint 3 implementation, the reasoning behind each decision, and the impact on the glossary.

The Sprint 2 domain model is preserved intact in `docs/Sprint2/us_010/`. This document and the accompanying `domain_model_sprint3.puml` (in this folder) represent the **authoritative Sprint 3 state** of the model. Any future sprint revision must follow the same pattern: leave prior sprint docs unchanged and produce a new revision document.

**Sprint 3 EAPLI scope:** US042, US043, US073–US077, US080, US082, US085, US078, US086, US111, US112.

---

## 2. Methodology

Every change listed below was driven by one of three sources:

1. **Client clarification** — official answers from the product owner that add or restrict requirements.
2. **Implementation constraint** — a design decision that was conceptually valid in a previous sprint but proved impractical or incorrect when confronted with the EAPLI/JPA framework.
3. **Requirements gap** — a field or concept missing from the prior model that a Sprint 3 use case explicitly requires.

Each change is tagged with its source and the use case(s) that motivated it.

---

## 3. Changes from Sprint 2 Design to Sprint 3 Implementation

### 3.1 EngineModel Aggregate

#### Change 1 — `fuelType` converted to `FuelType` Enum

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint / Domain accuracy |
| Motivation | US056, Aircraft Physics (Section 3.3) |

In Sprint 2, `fuelType` was modelled as a standard attribute inside the `EngineModel` entity. During Sprint 3, to better align with Domain-Driven Design principles and support strict validation, it was refactored into a dedicated `FuelType` Enum.

The enum explicitly defines recognized fuel sources (e.g., `JET_A1`). This provides type safety and directly supports the physics requirements outlined in Section 3.3, which explicitly singles out the density (0.804 kg/l) and specific energy (42.80 MJ/kg) of JET A-1 fuel for calculation purposes.

---

### 3.2 Pilot Aggregate

#### Change 2 — `Pilot.certifications` added (C07)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C07 |
| Motivation | US075, US080, US085, US121 |

A Pilot must store a `Set<AircraftModelCode>` representing the aircraft models they are certified to fly. This was confirmed by the client in C07: "A pilot is certified to pilot one or more aircraft models." The certification set is loaded by the `PilotRepository` during flight plan validation (R7 in US085) and during flight plan creation (US080, US121).

---

### 3.3 Flight Aggregate Updates

#### Change 3 — `FlightPlanStatus` renamed to DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED

| Attribute | Value |
|-----------|-------|
| Source | Client clarifications C03, C07, C14 |
| Motivation | US080, US082, US085 |

Sprint 2 used `draft`, `validated`, `rejected`. Sprint 3 renames to match the lifecycle required by US085:

- **DRAFT** — flight plan created but not yet tested (US080, US081)
- **IN_TEST** — validation in progress (US085)
- **TEST_PASSED** — all validation phases passed (US085)
- **TEST_FAILED** — at least one validation phase failed (US085)

Weather data changes (US082) revert the status back to **DRAFT**.

---

#### Change 4 — `FlightPlan.dslContent` added

| Attribute | Value |
|-----------|-------|
| Source | Client clarifications C03, C10 |
| Motivation | US080, US081, US085 |

FlightPlan stores the raw DSL content as a String so that US085 can re-validate it. The content comes from US080 (entered as text/file, not validated) or US081 (imported file, validated on import).

---

### 3.4 Simulation Aggregate

#### Change 5 — `SimulationReport.content` confirmed as persistent (C14)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C14 |
| Motivation | US109, US111 |

The client confirmed the report file is "not transient." The `SimulationReport` value object already existed in Sprint 2 with `filePath` and `content`. The team decided to store both: `filePath` for filesystem reference, `content` (TEXT/CLOB) in the database for durability and historical queries.

---

## 4. Updated Glossary Entries

The following entries are **new or changed** relative to the Sprint 2 glossary (`docs/Sprint2/us_010/glossary.md`). All other entries remain valid.

| **Term** | **Type** | **Sprint 3 Change** |
|:---|:---|:---|
| **FuelType** | Enum | **NEW.** Replaces the basic `fuelType` attribute. Defines the specific energy source used by an `EngineModel`. Values include `JET_A1` (widely used, explicit in sec. 3.3), `AVGAS`, `ELECTRIC`, and `SAF`. |
| **FlightPlanStatus** | Enum | **CHANGED.** Sprint 2: `draft/validated/rejected`. Sprint 3: `DRAFT/IN_TEST/TEST_PASSED/TEST_FAILED`. |
| **FlightPlan.dslContent** | Attribute | **NEW.** Raw DSL text string stored for re-validation (US085). |
| **Pilot.certifications** | Attribute | **NEW.** `Set<AircraftModelCode>` — models the pilot is certified to fly (C07). |
| **SimulationReport.content** | Attribute | **CONFIRMED.** Persistent TEXT/CLOB in database, not transient (C14). |

---

## 5. Domain Model Diagram

The updated PlantUML diagram is at `domain_model_sprint3.puml`. It supersedes the Sprint 2 full model for Sprint 3.

**Key conventions in the Sprint 3 diagram:**
- `EngineModel` now features a directed association to the `FuelType` enum (`consumes`).
- `FlightPlanStatus` uses Sprint 3 values: `DRAFT`, `IN_TEST`, `TEST_PASSED`, `TEST_FAILED`.
- `FlightPlan.dslContent` is documented.
- `FlightRoute` notes soft-delete only (C11).
- `Pilot` notes `Set<AircraftModelCode> certifications` (C07).
- `SimulationReport` notes permanent storage (C14).
- Aggregates implemented in Sprint 3 (FlightRoute, Flight, Pilot) have their *"Sprint 3 — Not yet implemented"* notes removed as development progresses.

---

## 6. Implementation Architecture Notes

### Key Integration Decisions (Sprint 3)

| Decision | Detail | Source |
|----------|--------|--------|
| FlightPlan is an **entity inside Flight** aggregate | Not a standalone aggregate. Accessed via `FlightRepository`. | Professor confirmation |
| Pilot certifications stored as `Set<AircraftModelCode>` | Loaded via `PilotRepository` for R7 validation | C07 |
| DSL is **re-validated every time** in US085 | No caching — ensures latest grammar | Team decision |
| Report stored as **filePath + content (TEXT)** | Both filesystem and DB for durability | C14 |
| Database: **PostgreSQL** | Already configured in persistence.xml | C01 + existing setup |
| C invocation via **SimulationRunner interface** | Abstracted behind `SimulationRunner` — supports `ProcessBuilderSimulationRunner` (local) and `SocketSimulationRunner` (TCP to sim_server) | Sprint 3 evolution |
| Package: `eapli.aisafe.flightplan.domain` | Not `flight` — avoids confusion with Flight aggregate | Team decision |