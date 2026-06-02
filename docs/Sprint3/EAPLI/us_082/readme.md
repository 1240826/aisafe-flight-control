# US082 — Insert Weather Data in a Flight

## 1. Context

This task was assigned in Sprint 3. The objective is to allow a Pilot to associate weather data
with their flight plan. If the flight plan had been previously tested, adding weather data voids
the test result and resets the flight plan status to draft.

**Assigned to:** Cláudio Pinto

### 1.1 List of Issues

- Analysis: #72 
- Design: #72 
- Implement: #72 
- Test: #72 

---

## 2. Requirements

**US082** As a Pilot, I want to add weather data to my flight plan. If the flight plan has been
previously tested, the test is deemed void because of the new weather data.

### Acceptance Criteria

- **US082.1** The system must require the PILOT role.
- **US082.2** The Pilot may only add weather data to their own flight plans.
- **US082.3** The selected WeatherData must already exist in the system.
- **US082.4** If the flight plan status is TESTED, adding weather data resets it to DRAFT.
- **US082.5** A flight plan in DRAFT status remains in DRAFT after weather data is added.

### Dependencies/References

- US030 — auth infrastructure.
- US080 — flight plan must exist.
- US041/US042 — WeatherData must exist in the system.
- US010 — domain model: WeatherData reference rises to the Flight root (Decision 14).

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — Domain behaviour for adding weather data

> "We are implementing US082 in a DDD Java system. A Pilot adds weather data to their flight plan.
> If the flight plan was previously tested, the test is voided. The WeatherData reference is on the
> Flight root (not on the internal FlightPlan entity). Where should the logic live and how should
> the status transition be handled?"

**LLM suggestions adopted:**
- `Flight.addWeatherData(WeatherData)` as the domain method — consistent with the WeatherData
  reference being on the Flight root (domain model Decision 14)
- Status transition handled inside the same method: if `flightPlan.status == TESTED`, reset to
  `DRAFT` before associating the weather data

**Decisions made by the team / deviations from LLM output:**
- The LLM placed the status reset in the controller — moved into the `Flight` domain method to
  keep the invariant inside the aggregate
- The LLM suggested a separate `voidTest()` method — merged into `addWeatherData()` since the
  void is a direct consequence of the operation, not an independent action

---

### 3.1 Key Design Decisions

**WeatherData reference on Flight root** — per domain model Decision 14, the cross-aggregate
reference to WeatherData rises to the Flight root. The controller loads the Flight and calls
`addWeatherData()` directly.

**Status invariant inside the aggregate** — `Flight.addWeatherData()` enforces the rule: if the
internal FlightPlan is in TESTED status, it is reset to DRAFT before the association is made. The
controller does not need to know about this transition.

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `AddWeatherDataToFlightUI` | `aisafe.app.backoffice.console` | Lists pilot's flights; lists available weather data; calls controller |
| `AddWeatherDataToFlightController` | `aisafe.core` | Auth; loads Flight and WeatherData; calls domain method; saves |
| `Flight` (modified) | `aisafe.core` | Adds `addWeatherData(WeatherData)` enforcing the status invariant |
| `FlightRepository` | `aisafe.core` | Finds flights by pilot |
| `WeatherDataRepository` | `aisafe.core` | Lists available WeatherData for selection |

**Sequence Diagram:**

![Sequence Diagram](sd_us082_add_weather_data_to_flight.svg)

### 4.2 Acceptance Tests

**AT1 — Weather data added to DRAFT flight plan (US082.4, US082.5)**

Given a flight plan in DRAFT status,
When the Pilot adds weather data,
Then the WeatherData is associated with the flight and the status remains DRAFT.

**AT2 — Weather data added to TESTED flight plan voids the test (US082.4)**

Given a flight plan in TESTED status,
When the Pilot adds weather data,
Then the WeatherData is associated with the flight and the flight plan status is reset to DRAFT.

**AT3 — Pilot cannot add weather data to another pilot's flight (US082.2)**

Given a flight plan that belongs to a different pilot,
When the Pilot attempts to add weather data,
Then the system rejects the operation.

---

## 5. Implementation

- `eapli.aisafe.flight.domain.Flight` — add `addWeatherData(WeatherData)`
- `eapli.aisafe.flight.application.AddWeatherDataToFlightController`
- `eapli.aisafe.app.backoffice.console.presentation.flight.AddWeatherDataToFlightUI`

---

## 6. Integration/Demonstration

To demonstrate this user story:

1. Bootstrap or manually create a flight with a flight plan in DRAFT status (US080).
2. Bootstrap or manually register weather data for the relevant air control area (US041/US042).
3. Log in as the Pilot assigned to the flight.
4. Select "Add Weather Data to Flight", choose the flight and a weather data record.
5. Verify the weather data is associated and the flight plan status remains DRAFT.

To demonstrate the void behaviour (US082.4):

1. Start from a flight plan in TESTED status (run US085 first).
2. Add weather data via this use case.
3. Verify the flight plan status is reset to DRAFT.

---

## 7. Observations

The status reset from TESTED to DRAFT is an invariant of the Flight aggregate and must not be
enforced outside it. The controller is responsible only for loading the correct aggregate instances
and persisting the result.