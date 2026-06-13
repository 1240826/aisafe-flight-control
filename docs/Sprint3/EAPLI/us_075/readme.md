# US075 — Add a Pilot

## 1. Context

This task was assigned in Sprint 3 within the Applications Engineering (EAPLI) scope. The objective is to allow an Air Transport Company Collaborator (ATCC) to add a pilot to their company's roster, associating the pilot — who must already be a system user — with one or more certified aircraft models.

**Assigned to:** Dinis Silva

### 1.1 List of Issues

- Analysis: #77
- Design: #77
- Implement: #77
- Test: #77

---

## 2. Requirements

**US075** As an Air Transport Company Collaborator, I want to add a pilot to my company.

### Acceptance Criteria

- **US075.1** The pilot must already be a registered system user (created via US061).
- **US075.2** The pilot must be certified to pilot at least one aircraft model registered in the system.
- **US075.3** The pilot is added to the company of the authenticated ATCC — the collaborator cannot add a pilot to a different company.
- **US075.4** A pilot already active in the same company cannot be added again.
- **US075.5** Access must be restricted to users with the `ATC_COLLABORATOR` role.

### Dependencies/References

- US030 — Authentication and authorization infrastructure
- US055 — Create an aircraft model (pilot must be certified for at least one model)
- US060 — Register an air transport company
- US061 — Add a customer's collaborator (the pilot must already exist as a system user)

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "In a DDD Java application, when adding a pilot to a company, should the Pilot aggregate hold a reference to the AirTransportCompany aggregate root, or should the company hold a collection of pilots? What are the trade-offs?"

**LLM suggestions adopted:**
- The `Pilot` aggregate holds a reference to the `AirTransportCompany` by its ID (not a direct object reference), keeping aggregates properly decoupled and avoiding large collection loading on the company side
- Certified aircraft models are stored as a collection of `AircraftModelId` value objects inside the `Pilot` aggregate, avoiding a hard dependency on the `AircraftModel` aggregate

**Decisions made by the team:**
- A pilot is scoped to one company at a time; adding the same user as a pilot to a second company is out of scope for this user story
- The controller resolves the ATCC's company from the session's authenticated user, so the collaborator never manually selects which company to assign the pilot to
- If the selected user already has an active `Pilot` record in this company, the operation is rejected with a clear error message

### 3.1 Domain Connections

The operation creates a new `Pilot` entity within the ATCC's `AirTransportCompany`. It cross-references the `SystemUser` aggregate (to confirm the user exists) and the `AircraftModel` aggregate (to validate that the certified models exist in the system). All lookups go through their respective repositories; no direct aggregate-to-aggregate object reference is created.

---

## 4. Design

### 4.1 Realization

**Classes created/modified:**

| Class | Package | Responsibility |
|-------|---------|----------------|
| `Pilot` | `eapli.aisafe.pilot.domain` | Aggregate root — holds `PilotId`, `CompanyIATA`, `Set<AircraftModelCode>`, `certificationDate`, `active` flag; enforces invariants in constructor |
| `PilotId` | `eapli.aisafe.pilot.domain` | Value Object — license number `[A-Z][0-9]{4,10}`; trims and uppercases before validating |
| `PilotRepository` | `eapli.aisafe.pilot.repositories` | Interface — `findByLicenseNumber(PilotId)`, `findByCompany(CompanyIATA)`, `findActiveByCompany(CompanyIATA)`, `hasAssignedFlights(PilotId)` |
| `AddPilotController` | `eapli.aisafe.pilot.application` | `@UseCaseController` — authorises with `ATC_COLLABORATOR`, calls `PilotId.valueOf()`, instantiates `Pilot`, saves via `PilotRepository`; also exposes `allCompanies()` and `allAircraftModels()` |
| `AirTransportCompanyRepository` | `eapli.aisafe.company.repositories` | Interface — `findAll()` used to populate company selection in the UI |
| `AircraftModelRepository` | `eapli.aisafe.aircraftmodel.repositories` | Interface — `findAll()` used to populate model multi-select in the UI |

**Sequence Diagram — Add Pilot:**

![Sequence Diagram — Add Pilot](sds/images/SD_US075_AddPilot.png)

### 4.2 Acceptance Tests

**AT1 — Pilot successfully added**

Given an authenticated ATCC,
And a system user with username "jpilot@airline.com" exists and is not yet a pilot in the ATCC's company,
And aircraft model "B737" exists in the system,
When the ATCC adds the user as a pilot certified for "B737",
Then the system creates the pilot record and confirms the operation with a success message.

**AT2 — Pilot already active in the company**

Given an authenticated ATCC,
And "jpilot@airline.com" is already an active pilot in the ATCC's company,
When the ATCC attempts to add the same user again,
Then the system rejects the operation with a clear error indicating the pilot is already registered.

**AT3 — Selected user does not exist in the system**

Given an authenticated ATCC,
When the ATCC provides a username that does not correspond to any system user,
Then the system rejects the operation with a user-not-found error and no pilot record is created.

**AT4 — No certified aircraft model provided**

Given an authenticated ATCC,
And a valid system user is selected,
When the ATCC submits the form without selecting any aircraft model,
Then the system rejects the operation, as at least one certified model is required.

**AT5 — Certified model does not exist**

Given an authenticated ATCC,
And a valid system user is selected,
When the ATCC provides an aircraft model ID that does not exist in the system,
Then the system rejects the operation with a model-not-found error.

**AT6 — Unauthorized role is blocked**

Given an authenticated user with the `BACKOFFICE_OPERATOR` role,
When the user attempts to access the Add Pilot feature,
Then the system rejects the operation with an authorization error.

---

## 5. Implementation

**New files:**

| Package | File | Role |
|---------|------|------|
| `eapli.aisafe.pilot.domain` | `Pilot.java` | Aggregate root — identity (`PilotId`), company (`CompanyIATA`), certified models (`Set<AircraftModelCode>`), certification date, active flag |
| `eapli.aisafe.pilot.domain` | `PilotId.java` | Value Object — license number format `[A-Z][0-9]{4,10}`, trims and uppercases on creation |
| `eapli.aisafe.pilot.repositories` | `PilotRepository.java` | Domain repository interface — `findByLicenseNumber`, `findByCompany`, `findActiveByCompany`, `hasAssignedFlights` |
| `eapli.aisafe.pilot.application` | `AddPilotController.java` | `@UseCaseController` — authorises (`ATC_COLLABORATOR`), creates `PilotId` VO, instantiates `Pilot`, delegates save to repository |

**Modified files:**

| File | Change |
|------|--------|
| `RepositoryFactory` (interface) | Added `pilots()` factory method |
| `JpaRepositoryFactory` | Implemented `pilots()` returning `JpaPilotRepository` |
| `InMemoryRepositoryFactory` | Implemented `pilots()` returning `InMemoryPilotRepository` |
| `persistence.xml` | Added `Pilot` and `PilotId` as managed entities |

**Unit tests:**

| Test class | Tests | Scope |
|------------|-------|-------|
| `AddPilotControllerTest` | 9 | Controller unit tests: happy path, auth guard, domain invariant rejection, support queries (`allCompanies`, `allAircraftModels`) |
| `PilotIdValidationTest` | 38 | CSV-driven (`pilot_id_test_data.csv`, 20 scenarios) — valid/invalid license format, edge cases (trim, case, boundary lengths) |
| `PilotCreationTest` | 12 | CSV-driven (`pilot_creation_test_data.csv`, 12 scenarios) — full `Pilot` construction including empty model set, future certification date |

---

## 6. Integration/Demonstration

1. Log in as an Air Transport Company Collaborator (`atcc1` / `Password1`).
2. Navigate to the **Pilots** menu and select **Add Pilot**.
3. The system displays all registered companies — select the appropriate one.
4. The system displays all registered aircraft models — select one or more to certify.
5. Enter the pilot's license number (format: one letter followed by 4–10 digits, e.g. `P12345`).
6. Enter the certification date (must not be in the future).
7. The pilot is created as active and immediately appears in the pilot roster (US076).

---

## 7. Observations

- **Cross-aggregate references by identity VO only:** `Pilot` holds a `CompanyIATA` value object (not an `AirTransportCompany` reference) and a `Set<AircraftModelCode>` (not `AircraftModel` references). This is the DDD rule for cross-aggregate coupling.
- **License format:** `PilotId` accepts `[A-Z][0-9]{4,10}` after trimming whitespace and uppercasing. The CSV tests PID18/PID19 confirm that `"P12345 "` and `" P12345"` are valid (whitespace is stripped before validation).
- **Authorization:** the controller enforces `ATC_COLLABORATOR` via `authz.ensureAuthenticatedUserHasAnyOf(...)`. No role check is done in the domain entity itself.
- **No SystemUser link in this sprint:** acceptance criterion US075.1 (pilot must already be a system user) is noted in the design. The `Pilot` entity in this implementation records the license number and company; the SystemUser association is left for the responsible colleague to integrate via US061.
- **Package-private testing constructor:** `AddPilotController` exposes a package-private constructor that accepts all four dependencies — used by `AddPilotControllerTest` to inject mocks without requiring a running persistence context.