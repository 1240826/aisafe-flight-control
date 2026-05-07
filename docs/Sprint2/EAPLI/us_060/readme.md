# US060 — Register Air Transport Company

## 1. Context

This task was assigned in Sprint 2. It is the first time this task is being developed. The objective is to allow an Admin to register an air transport company in the system. Companies are linked to collaborators (US061) and aircraft (US070).

**Assigned to:** Cláudio Pinto

### 1.1 List of Issues

- Analysis: #(to be assigned)
- Design: #(to be assigned)
- Implement: #(to be assigned)
- Test: #(to be assigned)

---

## 2. Requirements

**US060** As Admin, I want to register an air transport company so that it can be linked to collaborators and aircraft.

### Acceptance Criteria

- **US060.1** The system must require the `ADMIN` role.
- **US060.2** Company name must be unique in the system. *(Client clarification: "strange to have two companies with the same name — any of them must be unique.")*
- **US060.3** IATA code (2 letters) must be unique in the system. *(Client clarification: "no two airlines can have the same IATA code.")*
- **US060.4** ICAO code (2–3 letters) must be unique in the system. *(Client clarification: "no two airlines can have the same ICAO code.")*
- **US060.5** Each of the three fields (`CompanyName`, `IATACode`, `ICAOCode`) is individually unique — not just in combination.

### Dependencies/References

- US030 — auth infrastructure.
- US061 — collaborators will be linked to companies after creation.
- US070 — aircraft will be linked to companies after creation.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.

**Prompt 1:** "Design RegisterAirTransportCompany for EAPLI. Three individually-unique fields: CompanyName (VO), IATACode (VO, 2 letters), ICAOCode (VO, 2-3 letters). Controller checks all three uniqueness constraints."

**LLM suggestions adopted:**
- Three separate repository queries before creation: `findByName`, `findByIata`, `findByIcao`
- `CompanyName`, `IATACode`, `ICAOCode` as VOs validating format in constructors

**Decisions made by the team:**
- Three independent uniqueness checks — client confirmed each field must be individually unique
- `IATACode` for companies is 2 letters (different from airport IATA which is 3)
- `ICAOCode` for companies is 2–3 letters (different from airport ICAO which is 4)

### 3.1 Domain Model Navigation

**Aggregate: AirTransportCompany**
- Root: `AirTransportCompany`
- VO: `CompanyName` — validates non-empty; unique
- VO: `IATACode` — validates 2 uppercase letters; unique
- VO: `ICAOCode` — validates 2–3 uppercase letters; unique

### 3.2 Invariants

| VO | Invariant |
|----|-----------|
| `CompanyName` | not null, not empty; unique in system |
| `IATACode` (company) | exactly 2 uppercase letters; unique |
| `ICAOCode` (company) | 2–3 uppercase letters; unique |

---

## 4. Design

### 4.1 Realization

**Classes to create:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `RegisterAirTransportCompanyUI` | `aisafe.app.backoffice.console` | Collects input; calls controller |
| `RegisterAirTransportCompanyController` | `aisafe.core` | Auth; 3 uniqueness checks; creates company; saves |
| `AirTransportCompany` | `aisafe.core` | Aggregate root |
| `CompanyName` | `aisafe.core` | VO — validates non-empty |
| `IATACode` | `aisafe.core` | VO — validates 2 uppercase letters (company version) |
| `ICAOCode` | `aisafe.core` | VO — validates 2–3 uppercase letters (company version) |
| `AirTransportCompanyRepository` | `aisafe.core` | Repository interface |
| `JpaAirTransportCompanyRepository` | `aisafe.persistence.impl` | JPA implementation |
| `InMemoryAirTransportCompanyRepository` | `aisafe.persistence.impl` | In-memory implementation |

**Sequence Diagram:**

![Sequence Diagram](sd_us060_register_air_transport_company.svg)

### 4.2 Acceptance Tests

**Test 1:** `IATACode` (company) rejects wrong length.

**Refers to:** US060.3 / invariant

```java
@Test(expected = IllegalArgumentException.class)
public void ensureCompanyIATACodeRejectsWrongLength() {
    new IATACode("TAP"); // 3 letters — company IATA must be 2
}
```

**Test 2:** `CompanyName` rejects empty.

**Refers to:** US060.2 / invariant

```java
@Test(expected = IllegalArgumentException.class)
public void ensureCompanyNameRejectsEmpty() {
    new CompanyName("");
}
```

**Test 3:** `ICAOCode` (company) rejects code longer than 3 letters.

**Refers to:** US060.4 / invariant

```java
@Test(expected = IllegalArgumentException.class)
public void ensureCompanyICAOCodeRejectsTooLong() {
    new ICAOCode("TAPC"); // 4 chars — company ICAO max is 3
}
```

---

## 5. Implementation

**Key new files:**

- `eapli.aisafe.airtransportcompany.domain.AirTransportCompany` — aggregate root
- `eapli.aisafe.airtransportcompany.domain.CompanyName` — VO
- `eapli.aisafe.airtransportcompany.domain.IATACode` — VO (2 letters, company version)
- `eapli.aisafe.airtransportcompany.domain.ICAOCode` — VO (2–3 letters, company version)
- `eapli.aisafe.airtransportcompany.repositories.AirTransportCompanyRepository` — interface
- `eapli.aisafe.airtransportcompany.application.RegisterAirTransportCompanyController` — controller
- `eapli.aisafe.app.backoffice.console.presentation.airtransportcompany.RegisterAirTransportCompanyUI` — UI
- JPA + InMemory implementations

*Major commits: (to be filled after implementation)*

---

## 6. Integration/Demonstration

1. Log in as admin
2. Select "Register Air Transport Company"
3. Enter company name, IATA code (2 letters), ICAO code (2–3 letters)
4. System performs three independent uniqueness checks and confirms registration
5. Company available for US061 (add collaborator) and US070 (add aircraft)

---

## 7. Observations

The IATA and ICAO codes for air transport companies have different lengths than airport IATA/ICAO codes (airport: 3 + 4 letters; company: 2 + 2–3 letters). These are different VO classes despite the same concept name.

All three fields are individually unique — three separate controller-level checks with three separate repository queries.
