# US077 — Remove a Pilot

## 1. Context

This task was assigned in Sprint 3 within the Applications Engineering (EAPLI) scope. The objective is to allow an Air Transport Company Collaborator (ATCC) to make a pilot inactive in their company's roster, preventing the pilot from being assigned to future flights while preserving historical data integrity.

**Assigned to:** Dinis Silva

### 1.1 List of Issues

- Analysis: #79
- Design: #79
- Implement: #79
- Test: #79

---

## 2. Requirements

**US077** As an Air Transport Company Collaborator, I want to make a pilot inactive in my company's roster.

### Acceptance Criteria

- **US077.1** The ATCC can only deactivate pilots belonging to their own company.
- **US077.2** A pilot with flight plans currently assigned cannot be deactivated — the operation must be rejected with a clear error message.
- **US077.3** The pilot record is not deleted from the system — it is marked as inactive, preserving historical data.
- **US077.4** Once deactivated, the pilot must no longer appear in the active roster (US076).
- **US077.5** Access must be restricted to users with the `ATC_COLLABORATOR` role.

### Dependencies/References

- US030 — Authentication and authorization infrastructure
- US060 — Register an air transport company
- US075 — Add a pilot (pilot must exist to be removed)
- US076 — List pilot roster (deactivated pilots must be excluded)
- US080 — Create a flight plan (pilots with assigned flight plans cannot be deactivated)

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "In a DDD Java application, when deactivating a pilot, should the check for assigned flight plans be performed in the domain, the application service, or the repository layer? What are the trade-offs?"

**LLM suggestions adopted:**
- The check for assigned flight plans is performed in the application service layer, querying the `FlightPlanRepository` before invoking the state change on the `Pilot` aggregate — this keeps the domain focused on invariants it owns, while cross-aggregate coordination stays in the service
- The deactivation is modelled as a state transition on the `Pilot` aggregate (`pilot.deactivate()`), rather than a delete, so that historical records remain intact

**Decisions made by the team:**
- "Assigned flight plans" means flight plans in a non-terminal state (e.g., `DRAFT`, `VALIDATED`) — cancelled or completed flight plans do not block deactivation
- The ATCC selects the pilot from the active roster, so the UI reuses the listing logic from US076 to present only deactivatable candidates
- The pilot's username is shown in the confirmation prompt to avoid accidental deactivation

### 3.1 Domain Connections

The operation performs a state transition on the `Pilot` aggregate, setting its status to `INACTIVE`. Before doing so, the application service cross-checks the `FlightPlanRepository` to ensure no active flight plans reference this pilot. The `AirTransportCompany` identifier from the authenticated ATCC's session is used to scope the lookup, ensuring the collaborator cannot deactivate pilots from other companies.

---

## 4. Design

### 4.1 Realization

**Classes created/modified:**

| Class | Package | Responsibility |
|-------|---------|----------------|
| `RemovePilotController` | `eapli.aisafe.pilot.application` | `@UseCaseController` — authorises with `ATC_COLLABORATOR`; exposes `allPilots()` for selection, `deactivatePilot(PilotId)` with cross-aggregate guard, `activatePilot(PilotId)` for re-activation |
| `Pilot` | `eapli.aisafe.pilot.domain` | Added `deactivate()` (throws if already inactive), `activate()` (throws if already active), `isActive()` |
| `FlightRepository` | `eapli.aisafe.flight.repositories` | Interface — added `existsByPilotLicense(PilotId)` used as cross-aggregate guard before deactivation |

**Reused without modification:**

| Class | Role in US077 |
|-------|---------------|
| `PilotRepository` | `findByLicenseNumber(PilotId)` to load the pilot; `findAll()` to list all pilots; `save(Pilot)` to persist the state change |
| `PilotId` | Passed as parameter to `deactivatePilot()` / `activatePilot()` |

**Sequence Diagram — Remove Pilot:**

![Sequence Diagram — Remove Pilot](sds/images/sd_us077_remove_pilot.png)

### 4.2 Acceptance Tests

**AT1 — Pilot successfully deactivated**

Given an authenticated ATCC whose company has an active pilot with no assigned flight plans,
When the ATCC selects that pilot and confirms deactivation,
Then the system marks the pilot as inactive and displays a success message.

**AT2 — Deactivated pilot no longer appears in the active roster**

Given an authenticated ATCC who has successfully deactivated a pilot,
When the ATCC requests the pilot roster (US076),
Then the deactivated pilot does not appear in the list.

**AT3 — Pilot with assigned flight plans cannot be deactivated**

Given an authenticated ATCC whose company has an active pilot with at least one flight plan in a non-terminal state,
When the ATCC attempts to deactivate that pilot,
Then the system rejects the operation with a clear error indicating the pilot has active flight plans assigned.

**AT4 — ATCC cannot deactivate a pilot from another company**

Given an authenticated ATCC from company "AirAlpha",
And an active pilot belonging to company "AirBeta",
When the ATCC from "AirAlpha" attempts to deactivate the pilot from "AirBeta",
Then the system rejects the operation with a not-found or authorization error.

**AT5 — Unauthorized role is blocked**

Given an authenticated user with the `BACKOFFICE_OPERATOR` role,
When the user attempts to access the Remove Pilot feature,
Then the system rejects the operation with an authorization error.

---

## 5. Implementation

**New files:**

| Package | File | Role |
|---------|------|------|
| `eapli.aisafe.pilot.application` | `RemovePilotController.java` | `@UseCaseController` — authorises (`ATC_COLLABORATOR`), exposes `allPilots()` for selection list, `deactivatePilot(PilotId)` with cross-aggregate guard, `activatePilot(PilotId)` for re-activation |

**Domain changes (Pilot — from US075):**

| Method | Description |
|--------|-------------|
| `Pilot.deactivate()` | Sets `active = false`; throws `IllegalStateException` if already inactive |
| `Pilot.activate()` | Sets `active = true`; throws `IllegalStateException` if already active |
| `Pilot.isActive()` | Returns `active` flag |

**Reused from US075/US076 (no changes needed):**

| File | Role |
|------|------|
| `PilotRepository.java` | `findByLicenseNumber(PilotId)` used to load the pilot; `save(Pilot)` used to persist the state change |
| `FlightRepository.java` | `existsByPilotLicense(PilotId)` used as cross-aggregate guard before deactivation |

**Unit tests:**

| Test class | Tests | Scope |
|------------|-------|-------|
| `RemovePilotControllerTest` | 7 | Controller unit tests: happy path, auth guard, pilot not found, already inactive, flight plans assigned guard |
| `PilotDeactivationTest` | 6 | CSV-driven (`pilot_deactivation_test_data.csv`, 6 scenarios) — deactivate once (success), deactivate twice (fail), activate after deactivate (success), activate active pilot (fail) |

---

## 6. Integration/Demonstration

1. Log in as an Air Transport Company Collaborator (`atcc1` / `Password1`).
2. Navigate to the **Pilots** menu and select **Remove Pilot**.
3. The system displays all pilots (active and inactive) — select the pilot to deactivate.
4. If the pilot has flight plans assigned, the system rejects the operation with a clear error message.
5. If no flight plans are assigned, the pilot is marked inactive and a success message is shown.
6. Navigate to **List Pilot Roster** (US076) — the deactivated pilot no longer appears in the active roster.
7. To re-activate, select the same pilot and confirm re-activation.

---

## 7. Observations

- **Soft delete, not hard delete:** `Pilot.deactivate()` sets `active = false` — the record is never removed from the database. Historical data (past flights, flight plans) referencing this pilot remains intact.
- **Cross-aggregate guard in the controller:** the rule "a pilot with assigned flight plans cannot be deactivated" spans two aggregates (`Pilot` and `Flight`). Per DDD, `Pilot` cannot hold a reference to `FlightRepository`. The check `flightRepo.existsByPilotLicense(pilotId)` is therefore placed in `RemovePilotController`, which is the correct DDD location for cross-aggregate coordination.
- **Domain-level guard for double-deactivation:** `Pilot.deactivate()` enforces `Invariants.ensure(active, "Pilot is already inactive")`. This means the controller guard (flight plans check) and the entity guard (already inactive) are both enforced independently. If a pilot passes the controller guard but is already inactive, the entity still throws.
- **`allPilots()` lists all pilots, not just active ones:** this allows the UI to display inactive pilots so they can be re-activated. The repository call is `pilotRepo.findAll()` with no active filter.
- **`activatePilot(PilotId)` included:** although not explicitly required by the acceptance criteria, the reverse operation is provided for completeness and to support the "already inactive / re-activate" scenario (tested in `PilotDeactivationTest`).
- **Package-private testing constructor:** `RemovePilotController` exposes a package-private constructor accepting `AuthorizationService`, `PilotRepository`, and `FlightRepository` — used by `RemovePilotControllerTest` to inject mocks without a running persistence context.