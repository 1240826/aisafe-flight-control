# US080 — Create a Flight Plan

## 1. Context

This task was assigned in Sprint 3 within the Applications Engineering (EAPLI) scope. This is a core operational feature that transitions an abstract route into a scheduled flight event.

**Assigned to:** Jaime Simões

### 1.1 List of Issues

- Analysis: #69
- Design: #69
- Implement: #69
- Test: #69

---

## 2. Requirements

**US080** As Pilot, I want to register a flight plan for a route.

### Acceptance Criteria

- **US080.1** The user must select a route and add the aircraft, departure date/time, fuel quantity, and pilot.
- **US080.2** The assigned pilot must be of the route's company.
- **US080.3** Flight plan status is set to "draft" when created.
- **US080.4** The flight plan must undergo a multi-step validation process.

### Dependencies/References

- US070 — Add an aircraft to an air transport company
- US073 — Create a flight route
- US075 — Add a pilot

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "In a flight planning system, I need to create a FlightPlan aggregate root. A pilot registers it for a specific route. Acceptance criteria specifies: 'The assigned pilot must be of the route's company'. The route has a CompanyIATA reference, and the Pilot has a CompanyIATA reference. How should I structure the domain validation in FlightPlan to enforce this rule during creation?"

**LLM suggestions adopted:**
- Enforce company alignment directly in the FlightPlan constructor or as a validation guard inside the controller to prevent invalid pilot assignments.

**Decisions made by the team:**
- The status of the flight plan must be hardcoded to FlightPlanStatus.DRAFT during initialization.

### 3.1 Domain Connections

The `FlightPlan` aggregate will act as the operational instantiation of a `FlightRoute`. It must maintain references to the `Aircraft`, the `Pilot`, and include specific departure timing and fuel load data.

---

## 4. Design

### 4.1 Realization

**Classes to create:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `CreateFlightPlanUI` | `eapli.aisafe.ui.flightplan` | Captures flight plan inputs from the Pilot |
| `CreateFlightPlanController` | `eapli.aisafe.flightplan.application` | Validates inputs, company rules, and creates the plan |
| `FlightPlan` | `eapli.aisafe.flightplan.domain` | Aggregate root |
| `FlightPlanStatus` | `eapli.aisafe.flightplan.domain` | Enum containing `DRAFT` (and future states like `VALIDATED`) |
| `FlightPlanRepository` | `eapli.aisafe.flightplan.repositories` | Interface for persistence |
| `JpaFlightPlanRepository` | `eapli.aisafe.persistence.jpa`| JPA implementation |

**Sequence Diagram — Create Flight Plan:**

![Sequence Diagram — Create Flight Plan]([sd_us080_create_flight_plan.png)

### 4.2 Acceptance Tests

**AT1 — Pilot company validation**
Given a Pilot belonging to company "RY",
When the user attempts to assign this pilot to a flight plan on a route owned by company "TP",
Then the system rejects the creation due to a company mismatch.

**AT2 — Default draft status**
Given all valid inputs for a flight plan,
When the flight plan is successfully saved,
Then its internal status is automatically initialized as "DRAFT".

---

## 5. Implementation

**Key new files:**

- `[List relevant files created or altered]`

*Major commits: [Insert links or hashes]*

---

## 6. Integration/Demonstration

1. Log in as a Pilot.
2. Select "Register Flight Plan".
3. Choose an existing Route and Aircraft from the company's fleet.
4. Enter departure date/time and fuel quantity.
5. Confirm creation and verify the new plan appears in the system with a "DRAFT" status.

---

## 7. Observations

[Insert any technical debt, difficulties encountered, or architectural notes here]