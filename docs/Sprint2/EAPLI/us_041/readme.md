# US041 — Register Weather Data

## 1. Context

This task was assigned in Sprint 2. It is the first time this task is being developed. The objective is to allow a Weather Person to register meteorological data (wind, temperature, validity period) for a sub-area within an Air Control Area (ACA). Weather data is consumed by Flight Control Operators when assessing flight safety.

**Assigned to:** Fábio Costa

### 1.1 List of Issues

- Analysis: #(to be assigned)
- Design: #(to be assigned)
- Implement: #(to be assigned)
- Test: #(to be assigned)

---

## 2. Requirements

**US041** As Weather Person (or Backoffice Operator), I want to register weather data for an air control area so that flight control operators can assess meteorological conditions.

### Acceptance Criteria

- **AC 041.1** The system must require the `WEATHER_PERSON` or `BACKOFFICE_OPERATOR` role.
- **AC 041.2** Weather data must be linked to an existing ACA by its `AreaCode`.
- **AC 041.3** A `WeatherSubArea` must be defined by `minLat`, `maxLat`, `minLon`, `maxLon`, `minAlt`, `maxAlt`; where `minLat < maxLat`, `minLon < maxLon`, `minAlt ≥ 0`, `minAlt < maxAlt`.
- **AC 041.4** Wind data must include `windSpeedKnots > 0` and `windDirectionDeg ∈ [0, 360)`.
- **AC 041.5** `temperatureCelsius` must be provided (no range restriction).
- **AC 041.6** `validFrom` and `validTo` must be provided; `validTo > validFrom`.
- **AC 041.7** Registered data must be persisted and retrievable by `AreaCode`.

### Dependencies/References

- US030 — auth infrastructure.
- US050 — Air Control Area must exist before weather data can be registered.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.

**Prompt 1:** "How do I model weather data as a DDD aggregate in the EAPLI framework? What value objects are needed?"

**LLM suggestions adopted:**
- `WeatherData` as the aggregate root — linked to an `AreaCode`
- `WeatherSubArea` as a value object — encapsulates the geographic bounding box and altitude range
- `WindCondition` as a value object — encapsulates speed and direction
- All invariants enforced in constructors

**Decisions made by the team:**
- `WeatherSubArea` holds coordinates as `double` and altitudes as `int` (metres)
- `WindCondition` direction is `int` degrees; speed is `double` knots
- `validFrom`/`validTo` are `LocalDateTime`

### 3.1 Domain Model

| Concept | Type | Description |
|---------|------|-------------|
| `WeatherData` | Aggregate Root | Links ACA code, sub-area, wind, temperature, validity |
| `WeatherSubArea` | Value Object | Geographic bounding box + altitude range |
| `WindCondition` | Value Object | Speed (knots) + direction (degrees) |
| `AreaCode` | Value Object | Identifies the Air Control Area |

### 3.2 Invariants

- `WeatherSubArea`: `minLat < maxLat`, `minLon < maxLon`, `minAlt >= 0`, `minAlt < maxAlt`
- `WindCondition`: `speed > 0`, `direction ∈ [0, 360)`
- `WeatherData`: `validTo > validFrom`

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `RegisterWeatherDataUI` | `aisafe.app.backoffice.console` | Collects all inputs; calls controller |
| `RegisterWeatherDataController` | `aisafe.core` | Auth; creates VOs; delegates to repository |
| `WeatherData` | `aisafe.core` | Aggregate root holding all weather info |
| `WeatherSubArea` | `aisafe.core` | Value object — bounding box + altitudes |
| `WindCondition` | `aisafe.core` | Value object — speed + direction |
| `WeatherDataRepository` | `aisafe.core` | Repository interface |

**Sequence Diagram:**

![Sequence Diagram](sd_us041_register_weather_data.svg)

### 4.2 Acceptance Tests

**Test 1:** WeatherSubArea rejects invalid bounds.

**Refers to:** AC 041.3

```java
@Test(expected = IllegalArgumentException.class)
public void ensureWeatherSubAreaRejectsInvalidBounds() {
    new WeatherSubArea(10.0, 5.0, -10.0, -20.0, 0, 1000); // minLat > maxLat
}
```

**Test 2:** WindCondition rejects direction out of range.

**Refers to:** AC 041.4

```java
@Test(expected = IllegalArgumentException.class)
public void ensureWindConditionRejectsDirectionOutOfRange() {
    new WindCondition(10.0, 360); // 360 is out of [0,360)
}
```

**Test 3:** WeatherData rejects validTo before validFrom.

**Refers to:** AC 041.6

```java
@Test(expected = IllegalArgumentException.class)
public void ensureWeatherDataRejectsInvalidValidityPeriod() {
    LocalDateTime now = LocalDateTime.now();
    new WeatherData(AreaCode.valueOf("LPPT"), sub, wind, 15.0, now, now.minusHours(1));
}
```

---

## 5. Implementation

**Key files:**

- `eapli.aisafe.weatherdata.application.RegisterWeatherDataController`
- `eapli.aisafe.weatherdata.domain.WeatherData`
- `eapli.aisafe.weatherdata.domain.WeatherSubArea`
- `eapli.aisafe.weatherdata.domain.WindCondition`
- `eapli.aisafe.weatherdata.repositories.WeatherDataRepository`
- `eapli.aisafe.app.backoffice.console.presentation.weatherdata.RegisterWeatherDataUI`

*Major commits: (to be filled after implementation)*

---

## 6. Integration/Demonstration

1. Log in as Weather Person or Backoffice Operator
2. Select "Register Weather Data" from menu
3. Enter ACA code, sub-area bounds, wind data, temperature, validity period
4. System validates and persists — confirms success
5. Flight Control Operator can then query weather data for that ACA

---

## 7. Observations

`WeatherSubArea` and `WindCondition` are pure value objects — all their validation logic is in their constructors. `WeatherData` delegates geographic scoping to `WeatherSubArea` and wind data to `WindCondition`, keeping the aggregate root thin. The `AreaCode` links to an ACA but does not reference the full `AirControlArea` aggregate — it holds only the code string, avoiding cross-aggregate references.
