# US072 — List Company Fleet

## 1. Context

This task was assigned in Sprint 2. The objective is to allow an Admin to list all aircraft belonging to an air transport company's fleet. Pure query use case using `AbstractListUI`.

**Assigned to:** André Barcelos

### 1.1 List of Issues

- Analysis: #(to be assigned)
- Design: #(to be assigned)
- Implement: #(to be assigned)
- Test: #(to be assigned)

---

## 2. Requirements

**US072** As Admin, I want to list the fleet of an air transport company so that I can see which aircraft the company operates.

### Acceptance Criteria

- **US072.1** The system must require the `ADMIN` role.
- **US072.2** The Admin must be able to filter aircraft by company.
- **US072.3** The list must show at minimum: registration number, aircraft model, operational status, number of flight crew members, cabin configuration summary. Capacity = total seats across all `SeatClass` VOs. *(Client clarification: capacity = number of passengers.)*
- **US072.4** If the company has no aircraft, an appropriate message must be shown.
- **US072.5** Both `ACTIVE` and `DECOMMISSIONED` aircraft must be shown (full fleet history).

### Dependencies/References

- US030 — auth infrastructure.
- US060 — company must exist.
- US070 — aircraft must have been added.

---

## 3. Analysis

### 3.0 LLM Assistance

**LLM suggestions adopted:**
- `AbstractListUI<Aircraft>` — company selection first, then `findByCompanyId()`
- `AircraftPrinter` as `Visitor<Aircraft>` formats each row

**Decisions:**
- Both ACTIVE and DECOMMISSIONED aircraft shown (full fleet history, US072.5)
- Cabin summary shows total seats per class

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `ListFleetUI` | `aisafe.app.backoffice.console` | Selects company; extends `AbstractListUI<Aircraft>` |
| `ListFleetController` | `aisafe.core` | Auth; lists companies; queries fleet by company |
| `AircraftPrinter` | `aisafe.app.backoffice.console` | `Visitor<Aircraft>` — formats each row |

**Sequence Diagram:**

![Sequence Diagram](sd_us072_list_company_fleet.svg)

### 4.2 Acceptance Tests

**Test 1:** Only aircraft of the specified company are returned.

```java
@Test
public void ensureOnlyAircraftOfCompanyAreReturned() {
    Iterable<Aircraft> result = controller.fleetOfCompany(companyId);
    for (Aircraft a : result) {
        assertEquals(companyId, a.companyId());
    }
}
```

**Test 2:** Empty list when company has no aircraft.

```java
@Test
public void ensureEmptyListForCompanyWithNoAircraft() {
    Iterable<Aircraft> result = controller.fleetOfCompany(newCompanyId);
    assertFalse(result.iterator().hasNext());
}
```

**Test 3:** Both ACTIVE and DECOMMISSIONED aircraft are included.

```java
@Test
public void ensureFleetIncludesDecommissionedAircraft() {
    // given: one active, one decommissioned aircraft for companyId
    Iterable<Aircraft> result = controller.fleetOfCompany(companyId);
    long count = StreamSupport.stream(result.spliterator(), false).count();
    assertEquals(2, count);
}
```

---

## 5. Implementation

- `eapli.aisafe.aircraft.application.ListFleetController`
- `eapli.aisafe.app.backoffice.console.presentation.aircraft.ListFleetUI`
- `eapli.aisafe.app.backoffice.console.presentation.aircraft.AircraftPrinter`
- `eapli.aisafe.aircraft.repositories.AircraftRepository` — add `findByCompanyId(companyId)`

---

## 7. Observations

Pure query — no aggregate modified. `AircraftPrinter` computes capacity as the sum of `numberOfSeats` across all `SeatClass` VOs in `CabinConfiguration` (client: "capacity = number of passengers"). `AircraftRepository.findByCompanyId()` requires a custom JPQL query.
