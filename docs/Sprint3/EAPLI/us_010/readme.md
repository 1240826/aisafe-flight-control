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

#### Change 1 — `fuelType` validated against `FuelType` constants

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint / Domain accuracy |
| Motivation | US056, Aircraft Physics (Section 3.3) |

In Sprint 2, `fuelType` was modelled as a plain attribute inside `EngineModel` without validation. During Sprint 3 a `FuelType` constants class was introduced (`FuelType.ALL = {"Jet-A1", "AvGas 100LL", "SAF"}`) to validate the attribute at construction time. The DM retains `fuelType` as a plain attribute (not a UML enum) — consistent with the JPA mapping (`private String fuelType`).

---

### 3.2 Pilot Aggregate

#### Change 2 — `Pilot.certifications` added (C07)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C07 |
| Motivation | US075, US080, US085, US121 |

A Pilot must store a `Set<AircraftModelCode>` representing the aircraft models they are certified to fly. The certification set is loaded by `PilotRepository` during flight plan validation (R7 in US085) and during flight plan creation (US080, US121).

---

#### Change 3 — `PilotId` changed from email to `licenseNumber` (C01, C05)

| Attribute | Value |
|-----------|-------|
| Source | Client clarifications C01, C05 |
| Motivation | US075, US076, US077 |

Sprint 2 treated Pilot as a subclass of Collaborator (inheriting `SystemUser.email` as identity). Client clarification C05 confirmed: **"Pilot does NOT extend Collaborator."** Pilot is now a separate aggregate root with its own identity — `PilotId` wrapping a `licenseNumber`.

This aligns with client clarification C01: the pilot's unique identifier is the professional license number, not an email.

---

#### Change 4 — `Pilot extends Collaborator` relation removed (C05)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C05 |
| Motivation | US075, US076, US077 |

The inheritance link `Pilot "*" --> "1" Collaborator : extends` present in Sprint 2 has been removed. Pilot is now a standalone aggregate with its own lifecycle, invariant (cannot be deactivated with assigned flights), and direct reference from Flight. Common collaborator attributes (name, contact) are not inherited — Pilot defines its own.

---

### 3.3 Flight Aggregate

#### Change 5 — `FlightPlanStatus` renamed to DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED

| Attribute | Value |
|-----------|-------|
| Source | Client clarifications C03, C07, C14 |
| Motivation | US080, US082, US085 |

Sprint 2 used `draft`, `validated`, `rejected`. Sprint 3 renames to:

- **DRAFT** — flight plan created but not yet tested (US080, US081)
- **IN_TEST** — validation in progress (US085)
- **TEST_PASSED** — all validation phases passed (US085)
- **TEST_FAILED** — at least one validation phase failed (US085)

Weather data changes (US082) revert the status back to **DRAFT**.

---

#### Change 6 — `FlightPlan.dslContent` added

| Attribute | Value |
|-----------|-------|
| Source | Client clarifications C03, C10 |
| Motivation | US080, US081, US085 |

FlightPlan stores the raw DSL content as a String so that US085 can re-validate it. The content comes from US080 (entered as text/file, not validated) or US081 (imported file, validated on import).

---

#### Change 7 — `DepartureSchedule` hierarchy replaced by `Flight.departureTime` string (C07)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C07 |
| Motivation | US080, US085 |

Sprint 2 modelled a polymorphic `DepartureSchedule` hierarchy (`RegularSchedule` with `ScheduleEntry` entries, `CharterSchedule` with date+time). This was removed because:

1. C07 clarified the schedule belongs to the **flight route**, not to individual flights.
2. The time-of-day at which a specific flight departs is a simple attribute on `Flight`.

Now `Flight` carries `departureTime` (String — format TBD, e.g. "HH:mm" or ISO-8601 time). The full schedule (days of week for regular routes, specific date for charters) is a Route concept, not a Flight concept.

---

#### Change 8 — `scheduledArrivalTime` removed (C15)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C15 |
| Motivation | Domain accuracy |

The `scheduledArrivalTime` attribute on Flight was removed per C15: "time of departure and time of arrival are not used for any business rule; they are just informative." The departure time is retained as `departureTime` for route-reference purposes.

---

#### Change 9 — `FlightDesignator` simplified to single string (C11)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C11 |
| Motivation | US080, US085 |

Sprint 2 modelled `FlightDesignator` with three attributes: `airlineDesignator`, `flightNumber`, `operationalSuffix`. Per C11, the designator is now a **single string** field — saving, loading, and displaying one value is simpler than managing three.

---

#### Change 10 — `FlightPlan` expanded: `FlightPlanId`, report fields

| Attribute | Value |
|-----------|-------|
| Source | Requirements gap |
| Motivation | US080, US082, US085 |

Sprint 2 had a minimal `FlightPlan` with only `FuelQuantity`. Sprint 3 expands it:

- **FlightPlanId** — new value object wrapping the auto-generated numeric ID.
- **FlightPlan.reportFilePath** — path to the validation report file.
- **FlightPlan.reportContent** — content of the report (TEXT/CLOB in DB).
- **FlightPlan.createdAt** — timestamp of creation.
- **FlightPlan.lastTestedAt** — timestamp of last test execution.

The test result is tracked by `FlightPlanStatus` (TEST_PASSED / TEST_FAILED) plus the report fields. A `ValidationResult` value object (with `passed` boolean + `reasons` list) is used by the validation service as a return type — it is not persisted as an embedded field in FlightPlan.

---

#### Change 11 — `FuelQuantity` removed (C15)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C15 |
| Motivation | Domain accuracy |

The `FuelQuantity` value object (with `value` and `unit`) was removed per C15: fuel quantity is not required by the system. This aligns with the domain scope — the system validates flight plans and simulates flights, but fuel management is out of scope.

---

#### Change 12 — `FlightType` enum values uppercased

| Attribute | Value |
|-----------|-------|
| Source | Implementation consistency |
| Motivation | US080 |

Sprint 2 used lower-case values (`regular`, `charter`). Sprint 3 uses upper-case (`REGULAR`, `CHARTER`) for consistency with all other enum values in the model (AircraftType, MotorizationType, OperationalStatus, FlightPlanStatus, etc.).

---

### 3.4 Simulation Aggregate

#### Change 13 — `SimulationReport.content` confirmed as persistent (C14)

| Attribute | Value |
|-----------|-------|
| Source | Client clarification C14 |
| Motivation | US109, US111 |

The client confirmed the report file is "not transient." The `SimulationReport` value object already existed in Sprint 2 with `filePath` and `content`. The team decided to store both: `filePath` for filesystem reference, `content` (TEXT/CLOB) in the database for durability and historical queries.

---

## 4. Updated Glossary Entries

The full Sprint 3 glossary is at `glossary.md` (in this directory). The following table highlights the most important changes relative to Sprint 2:

| **Term** | **Type** | **Sprint 3 Change** |
|:---|:---|:---|
| **fuelType** (EngineModel) | Attribute | **VALIDATED** — now validated against `FuelType.ALL` constants class. Valid values: Jet-A1, AvGas 100LL, SAF. |
| **PilotId** | VO | **NEW** — wraps `licenseNumber`. Pilot identity is the professional license, not email (C01). |
| **Pilot extends Collaborator** | Relation | **REMOVED** (C05). Pilot is now a standalone aggregate root. |
| **FlightDesignator** | VO | **SIMPLIFIED** — single `designator` string instead of three attributes (C11). |
| **DepartureSchedule** hierarchy | VOs | **REMOVED** — `DepartureSchedule`, `RegularSchedule`, `CharterSchedule`, `ScheduleEntry` all removed (C07). |
| **Flight.departureTime** | Attribute | **NEW** — replaces the entire hierarchy. Simple string on Flight. |
| **Flight.scheduledArrivalTime** | Attribute | **REMOVED** (C15). Not used in any business rule. |
| **FuelQuantity** | VO | **REMOVED** (C15). Fuel management out of scope. |
| **FlightPlanStatus** | Enum | **CHANGED** — `draft/validated/rejected` → `DRAFT/IN_TEST/TEST_PASSED/TEST_FAILED`. |
| **FlightType** | Enum | **UPPERCASED** — `REGULAR`, `CHARTER` for consistency. |
| **FlightPlanId** | VO | **NEW** — wraps auto-generated ID for FlightPlan. |
| **FlightPlan.reportFilePath** | Attribute | **NEW** — path to validation report file. |
| **FlightPlan.reportContent** | Attribute | **NEW** — validation report content stored as TEXT/CLOB. |
| **FlightPlan.createdAt** | Attribute | **NEW** — creation timestamp. |
| **FlightPlan.lastTestedAt** | Attribute | **NEW** — last test execution timestamp. |

| **Pilot.certifications** | Attribute | **NEW** — `Set<AircraftModelCode>` — models pilot certifications (C07). |
| **SimulationReport.content** | Attribute | **CONFIRMED** — persistent TEXT/CLOB in database, not transient (C14). |

---

## 5. Domain Model Diagram

The updated PlantUML diagram is at `domain_model_sprint3.puml`. It supersedes the Sprint 2 full model for Sprint 3.

**Key conventions in the Sprint 3 diagram:**
- `Pilot` is a standalone aggregate with its own `PilotId` — no inheritance from `Collaborator`.
- `FlightDesignator` is a single-string value object.
- `Flight` has `departureTime` (string) — no `DepartureSchedule` hierarchy, no `scheduledArrivalTime`.
- `Flight.flightType` (`REGULAR`/`CHARTER`) is extracted from the DSL content at import time (US080) — the DSL file header declares `flight <id> : regular` or `flight <id> : charter`.
- `FlightPlanStatus` uses Sprint 3 values: `DRAFT`, `IN_TEST`, `TEST_PASSED`, `TEST_FAILED`.
- `FlightPlan` includes `dslContent`, `reportFilePath`, `reportContent`, `createdAt`, `lastTestedAt`.
- `FlightPlanId` is a new value object; a `ValidationResult` VO (passed + reasons) supports the validation service.
- `FuelQuantity` is absent.
- Inter-aggregate relations only from aggregate roots (DDD rule). AircraftVariant is a local entity inside AircraftModel — its cross-aggregate reference to EngineModel is by value (EngineModelCode), not a direct arrow.
- Aggregates implemented in Sprint 3 (FlightRoute, Flight, Pilot) have their *"Sprint 3 — Not yet implemented"* notes removed.

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
