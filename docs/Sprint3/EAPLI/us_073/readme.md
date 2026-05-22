# US073 — Create a Flight Route

## 1. Context

This task was assigned in Sprint 3 within the Applications Engineering (EAPLI) scope. It establishes the foundational flight routes that will later be instantiated into actual flight plans.

**Assigned to:** Jaime Simões

### 1.1 List of Issues

- Analysis: #68
- Design: #68
- Implement: #68
- Test: #68

---

## 2. Requirements

**US073** As an Air Transport Company Collaborator, I want to add a flight route for my company.

### Acceptance Criteria

- **US073.1** A route is between two airports (start and end).
- **US073.2** The route name must be formatted with exactly 2 letters (representing the company's initials) followed by up to 4 numbers (e.g., "TP123").
- **US073.3** The route's name must be strictly unique within the system.
- **US073.4** The user performing the action must have the `ATC_COLLABORATOR` role.

### Dependencies/References

- US052 — Create an airport (Start and end airports must exist).
- US060 — Register an air transport company (To validate company initials).

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "[Insert LLM Prompt used for regex validation or route domain modeling]"

**LLM suggestions adopted:**
- [Insert adopted suggestion, e.g., Value Object design for the Route Name]

**Decisions made by the team:**
- [Insert specific team decisions, e.g., how to retrieve the user's associated company to validate the 2-letter prefix]

### 3.1 Domain Connections

The `FlightRoute` aggregate root will need to reference two `Airport` entities (origin and destination) and belong to an `AirTransportCompany`. The `RouteName` must be an enforced Value Object utilizing regex for format validation.

---

## 4. Design

### 4.1 Realization

**Classes to create:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `CreateFlightRouteUI` | `aisafe.app.atc.console` | Captures route details from the user |
| `CreateFlightRouteController` | `aisafe.core` | Coordinates creation, validates uniqueness |
| `FlightRoute` | `aisafe.core` | Aggregate root representing the route |
| `RouteName` | `aisafe.core` | Value Object enforcing the 2-letter, 1 to 4-number format |
| `FlightRouteRepository` | `aisafe.core` | Interface for persistence |
| `JpaFlightRouteRepository`| `aisafe.persistence.impl`| JPA implementation |

**Sequence Diagram — Create Flight Route:**

![Sequence Diagram — Create Flight Route]([Insert Sequence Diagram File Name])

### 4.2 Acceptance Tests

**AT1 — Route name formatting enforcement**
Given an Air Transport Company Collaborator for company "TP",
When the user attempts to create a route named "T123" or "TP12345",
Then the system rejects the input due to invalid formatting.

**AT2 — Route uniqueness enforcement**
Given a flight route named "TP123" already exists,
When the user attempts to create a new route with the name "TP123",
Then the system rejects the creation stating the name must be unique.

**AT3 — Successful route creation**
Given valid start and end airports,
When the user creates a route named "TP123",
Then the system successfully saves the `FlightRoute` and it becomes available for flight plans.

---

## 5. Implementation

**Key new files:**

- `[List relevant files created or altered]`

*Major commits: [Insert links or hashes]*

---

## 6. Integration/Demonstration

1. Log in as an Air Transport Company Collaborator.
2. Navigate to the Route Management menu and select "Create Flight Route".
3. Select an origin and destination airport from the available lists.
4. Input a route name matching the company's initials and a number (e.g., TP123).
5. Verify the route is successfully saved to the database.

---

## 7. Observations

[Insert any technical debt, difficulties encountered, or architectural notes here]