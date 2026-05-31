# US010 — Domain Model Revision (Sprint 3)

## 1. Context

US010 (Domain Model) is a living artefact meant to evolve with each sprint. This document records every change made to the domain model during Sprint 3 implementation, the reasoning behind each decision, and the impact on the glossary.

The Sprint 2 domain model is preserved intact in `docs/Sprint2/us_010/`. This document and the accompanying `domain_model_sprint3.puml` represent the **authoritative Sprint 3 state** of the model. Any future sprint revision must follow the same pattern: leave prior sprint docs unchanged and produce a new revision document.

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

## 4. Updated Glossary Entries

The following entries are **new or changed** relative to the Sprint 2 glossary (`docs/Sprint2/us_010/glossary.md`). All other entries remain valid.

| **Term** | **Type** | **Sprint 3 Change** |
|:---|:---|:---|
| **FuelType** | Enum | **NEW.** Replaces the basic `fuelType` attribute. Defines the specific energy source used by an `EngineModel`. Values include `JET_A1` (widely used, explicit in sec. 3.3), `AVGAS`, `ELECTRIC`, and `SAF`. |

---

## 5. Domain Model Diagram

The updated PlantUML diagram is at `domain_model_sprint3.puml`. It supersedes the Sprint 2 full model for Sprint 3.

**Key conventions in the Sprint 3 diagram:**
- `EngineModel` now features a directed association to the `FuelType` enum (`consumes`).
- Aggregates implemented in Sprint 3 (FlightRoute, Flight, Pilot) will have their *"Sprint 3 — Not yet implemented"* notes removed as development progresses.

---

## 6. Implementation Architecture Notes

*(To be populated as Sprint 3 implementation details, JPA mapping decisions, and controller patterns for the new Use Cases are solidified by the team).*