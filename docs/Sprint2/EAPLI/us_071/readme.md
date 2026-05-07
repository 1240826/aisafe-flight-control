# US071 — Decommission Aircraft

## 1. Context

This task was assigned in Sprint 2. The objective is to allow an Admin to decommission an aircraft, changing its operational status from `ACTIVE` to `DECOMMISSIONED`. A decommissioned aircraft cannot be assigned to future flights.

**Assigned to:** Fábio Costa

### 1.1 List of Issues

- Analysis: #(to be assigned)
- Design: #(to be assigned)
- Implement: #(to be assigned)
- Test: #(to be assigned)

---

## 2. Requirements

**US071** As Admin, I want to decommission an aircraft so that it can no longer be assigned to flights.

### Acceptance Criteria

- **US071.1** The system must require the `ADMIN` role.
- **US071.2** The Admin must select an active aircraft to decommission.
- **US071.3** Decommissioning changes `OperationalStatus` from `ACTIVE` to `DECOMMISSIONED`.
- **US071.4** An already-decommissioned aircraft cannot be decommissioned again.
- **US071.5** Decommissioning does not delete the aircraft record.
- **US071.6** A decommissioned aircraft must be distinguishable in the fleet list (US072).

### Dependencies/References

- US030 — auth infrastructure.
- US070 — aircraft must exist.

---

## 3. Analysis

### 3.0 LLM Assistance

**LLM suggestions adopted:**
- `Aircraft.decommission()` sets `operationalStatus = DECOMMISSIONED`; throws `IllegalStateException` if already decommissioned
- `AircraftRepository.findAllActive()` shows only ACTIVE aircraft in selection step

**Decisions:**
- Irreversible state transition in Sprint 2 (no `recommission()`)
- Initial status `ACTIVE` enforced in `Aircraft` constructor (US070.7)

### 3.2 Invariants

| Entity | Invariant |
|--------|-----------|
| `Aircraft` | `ACTIVE → DECOMMISSIONED` only; no re-commission |

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `DecommissionAircraftUI` | `aisafe.app.backoffice.console` | Lists active aircraft; calls controller |
| `DecommissionAircraftController` | `aisafe.core` | Auth; lists active; calls `decommission()`; saves |
| `Aircraft` (modified) | `aisafe.core` | Adds `decommission()`, `isActive()`, `operationalStatus()` |
| `AircraftRepository` (modified) | `aisafe.core` | Adds `findAllActive()` |

**Sequence Diagram:**

![Sequence Diagram](sd_us071_decommission_aircraft.svg)

### 4.2 Acceptance Tests

**Test 1:** Double decommission is rejected.

```java
@Test(expected = IllegalStateException.class)
public void ensureDoubleDecommissionIsRejected() {
    Aircraft aircraft = createTestAircraft();
    aircraft.decommission();
    aircraft.decommission();
}
```

**Test 2:** Status transitions correctly.

```java
@Test
public void ensureDecommissionedAircraftIsInactive() {
    Aircraft aircraft = createTestAircraft();
    assertTrue(aircraft.isActive());
    aircraft.decommission();
    assertFalse(aircraft.isActive());
    assertEquals(OperationalStatus.DECOMMISSIONED, aircraft.operationalStatus());
}
```

**Test 3:** New aircraft starts ACTIVE.

```java
@Test
public void ensureNewAircraftIsActive() {
    Aircraft aircraft = createTestAircraft();
    assertEquals(OperationalStatus.ACTIVE, aircraft.operationalStatus());
}
```

---

## 5. Implementation

- `eapli.aisafe.aircraft.domain.Aircraft` — add `decommission()`, `isActive()`, `operationalStatus()`
- `eapli.aisafe.aircraft.repositories.AircraftRepository` — add `findAllActive()`
- `eapli.aisafe.aircraft.application.DecommissionAircraftController`
- `eapli.aisafe.app.backoffice.console.presentation.aircraft.DecommissionAircraftUI`

---

## 7. Observations

`decommission()` is irreversible in Sprint 2. `AircraftRepository.findAllActive()` filters by `operationalStatus = ACTIVE`. Decommissioned aircraft records are retained in the database.
