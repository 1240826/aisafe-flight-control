# Sprint 3 Planning — AISafe Flight Control System

**Sprint duration:** 18 May – 14 June 2026  
**Deadline:** 14 June 2026, 20:00 (GitHub commit)  
**Scrum Master:** LAPR4 PL Teacher  
**Team:** Jaime Simões, Cláudio Pinto, Dinis Silva, Fábio Costa, André Barcelos (team 2DC1)
 
---

## Sprint Goal

Deliver a fully integrated prototype encompassing all four course units for the first time.

- **EAPLI** completes the remaining business features: weather management, flight routes, pilots, flight plans and reporting.
- **LPROG** extends the DSL to full semantic validation and integrates it with the flight plan import flow.
- **RCOMP** implements TCP-based remote access for all three client roles, plus UDP event logging and HTTP visualisation.
- **SCOMP** evolves the simulation engine into a hybrid architecture using shared memory, threads, semaphores and condition variables.
---

## User Story Distribution

> Legend: USs marked with `*` span two course units and require coordination between the relevant team members.

### Jaime Simões

| US # | Description | CU |
|------|-------------|----|
| US043 | Consult weather data | EAPLI |
| US073 | Create a flight route | EAPLI |
| US080 | Create a flight plan | EAPLI |
| US78  | Air Transport Co. Collaborator remote access — TCP client | RCOMP |
| US105 | Initialise hybrid simulation environment with shared memory | SCOMP |

### Cláudio Pinto

| US # | Description | CU |
|------|-------------|----|
| US042 | Import bulk weather data | EAPLI |
| US082 | Insert weather data for a flight | EAPLI |
| US112 | Monthly report generation | EAPLI |
| US91  | Remote accesses logging visualisation (HTTP + AJAX) | RCOMP |
| US106 | Implement function-specific threads in the parent process | SCOMP |

### Dinis Silva

| US # | Description | CU |
|------|-------------|----|
| US075 | Add a pilot | EAPLI |
| US076 | List pilot roster | EAPLI |
| US077 | Remove a pilot | EAPLI |
| US44  | Weather Person remote access — TCP client | RCOMP |
| US107 | Notify report thread via condition variables upon safety violation | SCOMP |

### Fábio Costa

| US # | Description | CU |
|------|-------------|----|
| US085 | Test/validate flight plan *(EAPLI + LPROG + C)* | EAPLI / LAPR4 |
| US086 | Pilot remote access *(EAPLI + RCOMP)* `*` | EAPLI / RCOMP |
| US108 | Enforce step-by-step simulation synchronisation with semaphores | SCOMP |
| US120 | Flight DSL specification and validation *(all team)* | LPROG |
| US121 | Create a valid flight plan from a file *(all team)* | LPROG |

### André Barcelos

| US # | Description | CU |
|------|-------------|----|
| US074 | Delete (deactivate) a flight route | EAPLI |
| US078 | Air Transport Co. Collaborator remote access — EAPLI side `*` | EAPLI / RCOMP |
| US111 | Generate a simulation report | EAPLI |
| US90  | External logging of remote accesses (UDP datagrams) | RCOMP |
| US109 | Generate and store final simulation report | SCOMP |

### Whole team

| US # | Description | CU |
|------|-------------|----|
| US110 | Integrate environmental influences (wind) into simulation | SCOMP |
| US120 | Flight DSL specification and validation | LPROG |
| US121 | Create a valid flight plan from a file | LPROG |

> US110 is mandatory for 5-student SCOMP teams. US120 and US121 require involvement from all team members enrolled in LPROG; coordination with the LPROG PL teacher is required for work distribution.
 
---

## Cross-unit USs — Responsibility Split

| US # | EAPLI side | RCOMP side |
|------|-----------|-----------|
| US086 Pilot remote access | Fábio | Fábio |
| US078 ATCC remote access | André | Jaime |
| US085 Test/validate flight plan | Fábio (business logic) | — (C component, SCOMP context) |

> No client may interact directly with the database — TCP connection only (NFR, RCOMP).
 
---

## Dependencies

| Dependency | Notes |
|-----------|-------|
| `US120` → `US121` → `US085` | Critical chain: DSL must be complete before file import, which must be complete before flight plan validation. US085 is the LAPR4 Sprint 3 demonstration US. |
| `US030` (Sprint 2) → `US086`, `US078`, `US44`, `US78`, `US86` | Authentication and authorisation framework must be working before any remote access US can be integrated. |
| `US073` → `US074` | A route must exist before it can be deactivated. André depends on Jaime completing US073 first. |
| `US080` → `US082`, `US085` | A flight plan must exist before weather data can be added or the plan can be tested. |
| `US105` → `US106`, `US107`, `US108`, `US109`, `US110` | Shared memory segment must be initialised before any thread or semaphore US can be implemented. |
| `US109` (SCOMP) → `US111`, `US112` (EAPLI) | Simulation report output integrates with EAPLI reporting features. |
| `NFR08` | Final deployment must use a remote persistent RDBMS — coordinate RCOMP ↔ EAPLI before Week 4. |
 
---

## Weekly Plan

### Week 1 — 18–24 May · Review & Design

- Sprint 2 defence (EAPLI) and debrief (SCOMP oral in lab)
- Sprint 2 retrospective
- **Design shared memory segment structure and thread architecture** — all SCOMP (Jaime leads US105)
- **Design TCP protocol and message format** — all RCOMP
- **Design extended ANTLR grammar for US120** — all LPROG
- **Define integration contracts** — EAPLI ↔ LPROG ↔ RCOMP ↔ SCOMP interfaces agreed before any implementation begins
- Plan entity design for Route, Pilot and FlightPlan aggregates — all EAPLI
- Populate Scrum board with Sprint 3 issues and story point estimates
### Week 2 — 25–31 May · Core Implementation

- **EAPLI:** US043 (Jaime), US042 (Cláudio), US073 (Jaime), US075 (Dinis), US076 (Dinis)
- **LPROG:** US120 — full ANTLR grammar, listeners/visitors, semantic validation (all team)
- **RCOMP:** TCP server embedded in application + US44 (Dinis) + US78 (Jaime)
- **SCOMP:** US105 (Jaime), US106 (Cláudio), begin US107 (Dinis), begin US108 (Fábio)
### Week 3 — 1–7 June · Integration

- **EAPLI:** US080 (Jaime), US082 (Cláudio), US074 (André), US077 (Dinis), US085 (Fábio)
- **LPROG:** US121 — flight plan file import integrated with EAPLI (all team)
- **RCOMP:** US86 (Fábio), US78 (André — EAPLI side), US90 (André), US91 (Cláudio)
- **SCOMP:** complete US107 (Dinis), US108 (Fábio), US109 (André), US110 (all team)
- End-to-end integration test: EAPLI ↔ LPROG ↔ RCOMP
- Java test coverage check — domain and controller packages ≥ 90% (NFR03)
### Week 4 — 8–14 June · Hardening & Closure

- **EAPLI:** US111 (André), US112 (Cláudio), bootstrap verification
- **LPROG:** final grammar review and complete documentation
- **RCOMP:** NFR08 remote RDBMS, end-to-end remote access tests, AJAX page validation
- **SCOMP:** final simulation integration test, report output validation, oral preparation
- PlantUML diagrams generated (PNG + source) for all new artefacts
- `docs/` folder complete (Analysis → Design → Implementation → Tests) for all USs
- All acceptance criteria verified for every US
- Sprint Review and Retrospective
---

## Key Risks

| Risk | Severity | Mitigation |
|------|----------|-----------|
| US085 depends on US120 being done — it is the LAPR4 demo US | High | US120 must be complete by end of Week 2; unblock US085 early in Week 3 |
| NFR06 — any non-compiling commit = zero in LAPR4 | High | Define integration contracts in Week 1; never merge broken branches to main |
| NFR03 — Java test coverage must stay ≥ 90% at all times | High | Write tests alongside implementation, not at the end |
| US086 and US078 span two course units — risk of incomplete coverage | Medium | Explicitly assign EAPLI side and RCOMP side per person in the Scrum board |
| SCOMP — LLM-generated C code is strictly prohibited | Medium | Document in each US README that code was written manually |
| NFR08 — remote RDBMS deployment | Medium | Coordinate RCOMP ↔ EAPLI by Week 3; do not leave for the final days |
 
---

## Non-Functional Requirements (active this sprint)

- **NFR03** — Java domain and controller test coverage ≥ 90% at all times.
- **NFR06** — Every commit must leave the system in a compilable, test-passing state. Violation = zero in LAPR4 for that sprint.
- **NFR08** — Final deployment must use a remote persistent RDBMS.
- **NFR09** — Authentication and authorisation enforced for all remote access USs.
- **SCOMP rule** — LLM-generated code is strictly prohibited. Immediate zero score if detected.
- **RCOMP rule** — No client may interact directly with the database. TCP only.
- **Commit frequency** — Multiple commits per week required. Single weekly commits negatively affect assessment.