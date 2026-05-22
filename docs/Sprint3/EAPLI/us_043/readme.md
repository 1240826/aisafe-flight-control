# US043 — Consult Weather Data

## 1. Context

This task was assigned in Sprint 3 within the Applications Engineering (EAPLI) scope. The objective is to allow authorized users to query and view weather data for a specific area and time, which is critical for flight planning and simulation.

**Assigned to:** Jaime Simões

### 1.1 List of Issues

- Analysis: #67
- Design: #67
- Implement: #67
- Test: #67

---

## 2. Requirements

**US043** As a Weather Person, a Pilot, or a Flight Control operator, I want to consult weather data in the system in a given day and in a specific air control area.

### Acceptance Criteria

- **US043.1** The system must allow querying weather data by providing a specific date and a specific air control area.
- **US043.2** Access must be restricted to users with the `WEATHER_PERSON`, `PILOT`, or `FLIGHT_CONTROL_OPERATOR` roles.
- **US043.3** The returned data must accurately reflect the information previously registered or imported for that specific area and day.

### Dependencies/References

- US041 — Register weather data
- US042 — Import bulk weather data
- US050 — Register an air control area

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "[Insert LLM Prompt used for querying strategies or UI design]"

**LLM suggestions adopted:**
- [Insert adopted suggestion, e.g., how to structure the date/area query in the repository]

**Decisions made by the team:**
- [Insert specific team decisions, e.g., formatting of the output or handling days with no recorded data]

### 3.1 Domain Connections

The query requires traversing or filtering the `WeatherData` entity (or aggregate) using the `AirControlArea` identifier and a standard `Date` object.

---

## 4. Design

### 4.1 Realization

**Classes to create/modify:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `ConsultWeatherUI` | `aisafe.app.common.console` | Prompts user for area and date, displays results |
| `ConsultWeatherController` | `aisafe.core` | Orchestrates the query, enforces authorization |
| `WeatherService` | `aisafe.core` | Contains business logic for retrieving weather |
| `WeatherDataRepository` | `aisafe.core` | Declares the query method (e.g., `findByAreaAndDate`) |
| `JpaWeatherDataRepository` | `aisafe.persistence.impl` | Implements the database query |

**Sequence Diagram — Consult Weather Data:**

![Sequence Diagram — Consult Weather Data]([Insert Sequence Diagram File Name])

### 4.2 Acceptance Tests

**AT1 — Authorized user successfully queries weather data**
Given an authenticated user with the `PILOT` role,
And weather data exists for Area "A1" on "2026-10-12",
When the user queries weather for Area "A1" on "2026-10-12",
Then the system displays the correct weather parameters for that day and area.

**AT2 — Unauthorized access is blocked**
Given an authenticated user with the `BACKOFFICE_OPERATOR` role,
When the user attempts to access the Consult Weather Data feature,
Then the system rejects the operation with an authorization error.

**AT3 — Querying a day with no data**
Given an authenticated user with the `WEATHER_PERSON` role,
When the user queries weather for an area and date that has no recorded data,
Then the system displays a clear message indicating no data is available.

---

## 5. Implementation

**Key new/modified files:**

- `[List relevant files created or altered]`

*Major commits: [Insert links or hashes]*

---

## 6. Integration/Demonstration

1. Log in as a Pilot, Weather Person, or Flight Control Operator.
2. Navigate to the Weather menu and select "Consult Weather Data".
3. Input an existing Air Control Area code and a valid Date.
4. Verify the output matches the expected registered data.

---

## 7. Observations

[Insert any technical debt, difficulties encountered, or architectural notes here]