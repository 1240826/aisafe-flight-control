# US072 — List Company Fleet

## 1. Context

This task was assigned in Sprint 2. The objective is to allow an ATC Collaborator or Flight Control Operator to list all aircraft belonging to an air transport company's fleet, with optional filters by model, maker, or capacity.

**Assigned to:** André Barcelos

### 1.1 List of Issues

- Analysis: #(to be assigned)
- Design: #(to be assigned)
- Implement: #(to be assigned)
- Test: #(to be assigned)

---

## 2. Requirements

**US072** As an Air Transport Company Collaborator, I want to list my company's aircraft fleet.

### Acceptance Criteria

- **US072.1** The system must require the `ATC_COLLABORATOR` or `FLIGHT_CONTROL_OPERATOR` role.
- **US072.2** The user must be able to filter aircraft by company, or show all aircraft across all companies.
- **US072.3** The list must show at minimum: registration number, aircraft model code, operational status, total capacity (number of passengers), and number of flight crew members.
- **US072.4** If the company has no aircraft, an appropriate message must be shown.
- **US072.5** Both `ACTIVE` and `DECOMMISSIONED` aircraft must be shown (full fleet history).
- **US072a** The user must be able to filter the fleet by aircraft model code.
- **US072b** The user must be able to filter the fleet by aircraft maker/manufacturer name.
- **US072c** The user must be able to filter the fleet by minimum passenger capacity.

### Dependencies/References

- US030 — auth infrastructure.
- US060 — company must exist.
- US070 — aircraft must have been added.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.

**LLM suggestions adopted:**
- `AbstractListUI<Aircraft>` — company selection first, then filter selection, then `findByCompanyId()`
- Inline `Visitor<Aircraft>` lambda formats each row
- US072b filter done in memory in the controller (maker is stored on `AircraftModel`, not on `Aircraft`)
- US072c filter done in memory in the controller (capacity is computed from `CabinConfiguration`, not a stored column)

**Decisions made by the team:**
- Both ACTIVE and DECOMMISSIONED aircraft shown (full fleet history, US072.5)
- US072b and US072c filters applied in memory after fetching fleet from repository
- US072a filter delegated to repository (`findByCompanyIdAndModel`)

### 3.1 Domain Model

| Concept | Type | Description |
|---------|------|-------------|
| `Aircraft` | Aggregate Root | Registration, model code, company, cabin config, status |
| `CabinConfiguration` | Value Object | List of `SeatClass` VOs; `totalCapacity()` = sum of seats |
| `RegistrationNumber` | Value Object | Unique worldwide; number + country |
| `AircraftModelCode` | Value Object | Cross-aggregate ref to `AircraftModel` |
| `CompanyIATA` | Value Object | Cross-aggregate ref to `AirTransportCompany` |

---

## 4. Design

### 4.1 Realization

| Class | Module | Responsibility |
|-------|--------|----------------|
| `ListCompanyFleetUI` | `aisafe.app` | Selects company and filter; extends `AbstractListUI<Aircraft>` |
| `ListCompanyFleetController` | `aisafe.core` | Auth; lists companies; queries and filters fleet |
| `AircraftRepository` | `aisafe.core` | `findByCompanyId`, `findByCompanyIdAndModel`, `findAllActive` |
| `AircraftModelRepository` | `aisafe.core` | Used by US072b to resolve manufacturer name |

**Sequence Diagram:**

![Sequence Diagram](sd_us072_list_company_fleet.svg)

### 4.2 Acceptance Tests

**AT1 — Only aircraft of the selected company are returned (US072.2)**

Given two companies each with at least one aircraft in the system,
When the user selects one company and requests the fleet list,
Then the system returns only aircraft registered to that company.

**AT2 — Empty fleet message shown when company has no aircraft (US072.4)**

Given a company that has been registered but has no aircraft added to its fleet,
When the user requests the fleet list for that company,
Then the system displays an appropriate message indicating no aircraft are found.

**AT3 — Both ACTIVE and DECOMMISSIONED aircraft are included (US072.5)**

Given a company that has both an active and a decommissioned aircraft,
When the user requests the fleet list for that company,
Then the system returns both aircraft regardless of operational status.

**AT4 — Filter by model returns only matching aircraft (US072a)**

Given a company fleet with aircraft of different models,
When the user filters by model code "A320",
Then only aircraft of that model are returned.

**AT5 — Filter by maker returns only matching aircraft (US072b)**

Given a company fleet with aircraft from different manufacturers,
When the user filters by maker "Airbus",
Then only aircraft whose model is manufactured by Airbus are returned.

**AT6 — Filter by capacity excludes aircraft below minimum (US072c)**

Given a company fleet with aircraft of different capacities,
When the user filters by minimum capacity 200,
Then only aircraft with total capacity >= 200 are returned.

---

## 5. Implementation

**Key files:**

- `eapli.aisafe.aircraft.application.ListCompanyFleetController`
- `eapli.aisafe.ui.aircraft.ListCompanyFleetUI`
- `eapli.aisafe.aircraft.repositories.AircraftRepository`
- `eapli.aisafe.persistence.jpa.JpaAircraftRepository`
- `eapli.aisafe.persistence.inmemory.InMemoryAircraftRepository`

*Major commits: (to be filled after implementation)*

---

## 6. Integration/Demonstration

1. Log in as ATC Collaborator or Flight Control Operator
2. Select "Aircraft" → "List Company Fleet"
3. Select a company (or 0 for all)
4. Select a filter: none, by model (US072a), by maker (US072b), or by minimum capacity (US072c)
5. System displays matching aircraft with registration, model, status, capacity and crew

---

## 7. Observations

US072b and US072c filters are applied in memory in the controller because the manufacturer name is stored on `AircraftModel` (not on `Aircraft`), and capacity is computed from `CabinConfiguration` rather than stored as a column. US072a is delegated to the repository via a JPQL query on `aircraftModelCode.code`.
