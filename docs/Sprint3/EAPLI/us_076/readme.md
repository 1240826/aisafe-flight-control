# US076 — List Pilot Roster

## 1. Context

This task was assigned in Sprint 3 within the Applications Engineering (EAPLI) scope. The objective is to allow an Air Transport Company Collaborator (ATCC) to view the full list of pilots currently associated with their company, supporting fleet and crew management decisions.

**Assigned to:** Dinis Silva

### 1.1 List of Issues

- Analysis: #78
- Design: #78
- Implement: #78
- Test: #78

---

## 2. Requirements

**US076** As an Air Transport Company Collaborator, I want to list my company's pilot roster.

### Acceptance Criteria

- **US076.1** The system must display all pilots currently associated with the authenticated ATCC's company.
- **US076.2** The list must only include active pilots — inactive pilots (removed via US077) must not appear.
- **US076.3** The authenticated ATCC can only view pilots from their own company; pilots from other companies must not be listed.
- **US076.4** Access must be restricted to users with the `ATC_COLLABORATOR` role.
- **US076.5** If the company has no active pilots, the system must display a clear message indicating the roster is empty.

### Dependencies/References

- US030 — Authentication and authorization infrastructure
- US060 — Register an air transport company
- US075 — Add a pilot (pilots must exist to be listed)
- US077 — Remove a pilot (inactive pilots must be excluded)

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "In a DDD Java application, what is the cleanest way to list all active pilots scoped to a specific company, given that the Pilot aggregate holds a reference to the company by ID?"

**LLM suggestions adopted:**
- The repository query filters by both `companyId` and `status = ACTIVE`, keeping the filtering logic in the persistence layer rather than in the service or controller
- The result is returned as a read-only DTO or a list of `Pilot` aggregate roots — no lazy-loaded collections are exposed to the UI layer

**Decisions made by the team:**
- The ATCC's company is resolved automatically from the authenticated session — the collaborator does not manually select a company
- The list is displayed in a simple tabular format showing at minimum the pilot's name, username, and certified aircraft models
- Sorting is alphabetical by pilot name for consistency and readability

### 3.1 Domain Connections

The operation queries the `Pilot` aggregate filtered by the `AirTransportCompany` identifier extracted from the authenticated ATCC's session. No modification of any aggregate occurs — this is a pure read operation. The `AircraftModel` aggregate may be referenced to resolve model names for display purposes.

---

## 4. Design

### 4.1 Realization

**Classes to create/modify:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `ListPilotRosterUI` | `aisafe.app.atcc.console` | Triggers the listing and renders the pilot roster in a tabular format |
| `ListPilotRosterController` | `aisafe.core` | Resolves the ATCC's company from the session and delegates to the service |
| `PilotService` | `aisafe.core` | Contains business logic for retrieving active pilots by company |
| `PilotRepository` | `aisafe.core` | Declares the query method (e.g., `findActiveByCompany`) |
| `JpaPilotRepository` | `aisafe.persistence.impl` | Implements the filtered database query |

**Sequence Diagram — List Pilot Roster:**

![Sequence Diagram — List Pilot Roster](SD_US076_ListPilotRoster.png)

### 4.2 Acceptance Tests

**AT1 — Roster with active pilots is displayed correctly**

Given an authenticated ATCC whose company has two active pilots,
When the ATCC requests the pilot roster,
Then the system displays both pilots with their names, usernames, and certified aircraft models.

**AT2 — Inactive pilots are excluded from the list**

Given an authenticated ATCC whose company has one active pilot and one inactive pilot (removed via US077),
When the ATCC requests the pilot roster,
Then only the active pilot is displayed and the inactive one does not appear.

**AT3 — Empty roster displays a clear message**

Given an authenticated ATCC whose company has no active pilots,
When the ATCC requests the pilot roster,
Then the system displays a message indicating the roster is currently empty.

**AT4 — Pilots from other companies are not shown**

Given an authenticated ATCC from company "AirAlpha",
And company "AirBeta" has its own active pilots registered in the system,
When the ATCC from "AirAlpha" requests the pilot roster,
Then only pilots belonging to "AirAlpha" are displayed.

**AT5 — Unauthorized role is blocked**

Given an authenticated user with the `BACKOFFICE_OPERATOR` role,
When the user attempts to access the List Pilot Roster feature,
Then the system rejects the operation with an authorization error.

---

## 5. Implementation

**Key new/modified files:**

- `[List relevant files created or altered]`

*Major commits: [Insert links or hashes]*

---

## 6. Integration/Demonstration

1. Log in as an Air Transport Company Collaborator.
2. Add at least one pilot via the Add Pilot feature (US075).
3. Navigate to the Pilots menu and select "List Pilot Roster".
4. Verify that all active pilots for the company are displayed with correct information.
5. Remove a pilot via US077 and confirm they no longer appear in the roster.

---

## 7. Observations

[Insert any technical debt, difficulties encountered, or architectural notes here]