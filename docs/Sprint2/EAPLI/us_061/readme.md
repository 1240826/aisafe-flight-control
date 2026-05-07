# US061 — Add Customer's Collaborator

## 1. Context

This task was assigned in Sprint 2. It is the first time this task is being developed. The objective is to allow an Admin to add a collaborator (ATCCollaborator, FlightControlOperator, or WeatherPerson) to the system, linking them to a system user and associating them with a company or air control area.

**Assigned to:** Dinis Silva

### 1.1 List of Issues

- Analysis: #(to be assigned)
- Design: #(to be assigned)
- Implement: #(to be assigned)
- Test: #(to be assigned)

---

## 2. Requirements

**US061** As Admin, I want to add a collaborator for a customer's company or air control area so that they can perform their role in the system.

### Acceptance Criteria

- **US061.1** The system must require the `ADMIN` role.
- **US061.2** The collaborator must be one of: `ATCCollaborator`, `FlightControlOperator`, `WeatherPerson`.
- **US061.3** Each collaborator must have: name, position (= role, confirmed by client), and a linked `SystemUser`.
- **US061.4** `ATCCollaborator` must be associated with an existing `AirTransportCompany`.
- **US061.5** `FlightControlOperator` and `WeatherPerson` must be associated with an existing `AirControlArea` (one ACA per FCO/WeatherPerson). *(Client clarification: "a FCO is responsible for managing air traffic in just one ACA.")*
- **US061.6** A collaborator must have an active `SecurityClearance` (expiry date in the future).
- **US061.7** A collaborator must have a `SkillsAssessment` (assessment date, not in the future).
- **US061.8** The linked `SystemUser` must have the appropriate role: `ATC_COLLABORATOR`, `FLIGHT_CONTROL_OPERATOR`, or `WEATHER_PERSON`.

### Dependencies/References

- US030 — auth infrastructure.
- US031 — the linked `SystemUser` must be registered first.
- US060 — `AirTransportCompany` must exist for ATCCollaborator.
- US050 — `AirControlArea` must exist for FCO and WeatherPerson.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.

**Prompt 1:** "Design AddCollaborator for EAPLI. Abstract root Collaborator with ATCCollaborator/FCO/WeatherPerson. SecurityClearance (VO, expiryDate must be future). SkillsAssessment (VO, assessmentDate). FCO/WeatherPerson → AirControlArea; ATCCollaborator → AirTransportCompany."

**LLM suggestions adopted:**
- `Collaborator` is an abstract aggregate root; concrete types are subtypes
- `SecurityClearance` VO validates `expiryDate` is in the future
- Controller uses a switch on collaborator type to create the right subtype
- Cross-aggregate references by ID only

**Decisions made by the team:**
- `position` attribute = professional role (client confirmed: "Position = Role")
- `SystemUser` is created first (US031) and linked via `@OneToOne CascadeType.NONE`
- `CollaboratorRepository` covers all concrete types (JPA inheritance — single table or joined)
- FCO and WeatherPerson are linked to one `AirControlArea` (client confirmed as default for Sprint 2)

### 3.1 Domain Model Navigation

**Aggregate: Collaborator**
- Abstract root: `Collaborator` — `name`, `position`; linked to `SystemUser`
- `ATCCollaborator` — references `AirTransportCompany` by `CompanyId` only
- `FlightControlOperator` — references `AirControlArea` by `AreaCode` only
- `WeatherPerson` — references `AirControlArea` by `AreaCode` only
- VO: `SecurityClearance` — `expiryDate` (must be in the future)
- VO: `SkillsAssessment` — `assessmentDate` (not null, not in the future)

### 3.2 Invariants

| VO / Entity | Invariant |
|-------------|-----------|
| `SecurityClearance` | `expiryDate` not null; must be in the future |
| `SkillsAssessment` | `assessmentDate` not null; not in the future |
| `Collaborator` | non-empty `name`; non-empty `position` |
| `ATCCollaborator` | `companyId` not null |
| `FlightControlOperator` / `WeatherPerson` | `areaCode` not null |

---

## 4. Design

### 4.1 Realization

**Classes to create:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `AddCollaboratorUI` | `aisafe.app.backoffice.console` | Selects type; collects fields; calls controller |
| `AddCollaboratorController` | `aisafe.core` | Auth; validates refs; creates Collaborator subtype; saves |
| `Collaborator` | `aisafe.core` | Abstract aggregate root |
| `ATCCollaborator` | `aisafe.core` | Concrete — linked to AirTransportCompany |
| `FlightControlOperator` | `aisafe.core` | Concrete — linked to AirControlArea |
| `WeatherPerson` | `aisafe.core` | Concrete — linked to AirControlArea |
| `SecurityClearance` | `aisafe.core` | VO — expiryDate in future |
| `SkillsAssessment` | `aisafe.core` | VO — assessmentDate not null |
| `CollaboratorRepository` | `aisafe.core` | Repository interface |
| `JpaCollaboratorRepository` | `aisafe.persistence.impl` | JPA implementation |
| `InMemoryCollaboratorRepository` | `aisafe.persistence.impl` | In-memory implementation |

**Sequence Diagram:**

![Sequence Diagram](sd_us061_add_collaborator.svg)

### 4.2 Acceptance Tests

**Test 1:** `SecurityClearance` rejects expiry date in the past.

**Refers to:** US061.6 / invariant

```java
@Test(expected = IllegalArgumentException.class)
public void ensureSecurityClearanceRejectsPastExpiryDate() {
    new SecurityClearance(LocalDate.now().minusDays(1));
}
```

**Test 2:** `Collaborator` requires non-empty name.

**Refers to:** US061.3 / invariant

```java
@Test(expected = IllegalArgumentException.class)
public void ensureCollaboratorRequiresName() {
    new ATCCollaborator("", "Senior ATC", systemUser, companyId, validSC, validSA);
}
```

---

## 5. Implementation

**Key new files:**

- `eapli.aisafe.collaborator.domain.Collaborator` — abstract root
- `eapli.aisafe.collaborator.domain.ATCCollaborator` — concrete entity
- `eapli.aisafe.collaborator.domain.FlightControlOperator` — concrete entity
- `eapli.aisafe.collaborator.domain.WeatherPerson` — concrete entity
- `eapli.aisafe.collaborator.domain.SecurityClearance` — VO
- `eapli.aisafe.collaborator.domain.SkillsAssessment` — VO
- `eapli.aisafe.collaborator.repositories.CollaboratorRepository` — interface
- `eapli.aisafe.collaborator.application.AddCollaboratorController` — controller
- `eapli.aisafe.app.backoffice.console.presentation.collaborator.AddCollaboratorUI` — UI
- JPA + InMemory implementations

*Major commits: (to be filled after implementation)*

---

## 6. Integration/Demonstration

1. Log in as admin
2. Select "Add Collaborator"
3. Select type (ATCCollaborator / FCO / WeatherPerson)
4. Enter name, position; select linked system user; select company or area
5. Enter security clearance expiry date and skills assessment date
6. System validates and confirms

---

## 7. Observations

`Collaborator` uses JPA inheritance. Recommended strategy: `SINGLE_TABLE` with a discriminator column for simplicity. Cross-aggregate references (`companyId`, `areaCode`) are stored as plain IDs — no JPA `@ManyToOne`.

The `SystemUser` is linked with `@OneToOne CascadeType.NONE` — the system user has its own lifecycle managed by the EAPLI framework.

FCO and WeatherPerson are associated with one `AirControlArea` per the client clarification ("a FCO is responsible for managing air traffic in just one ACA"). This can be extended in future sprints.
