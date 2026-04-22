# Sprint Planning – Semester 4 Integrative Project
## AISafe Flight Control System — Sprint 2

| | |
|---|---|
| **Sprint Duration** | 6 weeks (1 week interrupted) |
| **Scrum Master** | LAPR4 PL Teacher |
| **Team** | Jaime Simões, Cláudio Pinto, Dinis Silva, Fábio Costa, André Barcelos |
| **Deadline** | 17 May 2026, 20:00 (GitHub commit) |

---

## Sprint Goal

Implement core system functionality across EAPLI, LPROG, and SCOMP course units. This includes developing authentication and authorization systems, configuring basic entities (users, airports, aircraft models, air companies), implementing the Flight DSL parser and validator, and developing the flight simulation engine with safety violation detection. Deliver a functional prototype with integrated components from EAPLI, LPROG, and SCOMP ready for Sprint 3.

---

## Participating Courses & User Stories

| Course | User Stories | Focus |
|--------|-------------|-------|
| EAPLI | US030–US033, US041, US050, US052, US055–US057, US060–US064, US070–US072 | Configuration, user management, weather data, fleet operations, bootstrap |
| LPROG | US081, US083 | Flight DSL parser, lexical/syntactic/semantic validation |
| SCOMP | US100, US101, US102, US103, US109 | Flight simulation, process management, safety detection, reporting |
| LAPR4 | Integration, Scrum methodology | Process management, integration testing, CI/CD |

---

## User Story Distribution

### EAPLI (16 Mandatory + 3 Extra)

#### Mandatory User Stories

| US | Description |
|----|-------------|
| US030 | Authentication and authorization *(shared)* |
| US031 | Register users |
| US032 | Disable/enable users |
| US033 | List users |
| US041 | Register weather data |
| US050 | Register air control area |
| US052 | Create airport |
| US055 | Create aircraft model |
| US056 | Create aircraft engine model |
| US057 | Add engine model to aircraft model |
| US060 | Register air transport company |
| US061 | Add customer's collaborator |
| US062 | List customer's collaborators |
| US070 | Add aircraft to air transport company |
| US071 | Decommission aircraft |
| US072 | List company fleet |

#### Extra User Stories

| US | Description |
|----|-------------|
| US058 | Remove engine model from aircraft model |
| US063 | Edit customer's collaborator |
| US064 | Disable customer's collaborator |

#### Individual Assignment

| Member | Mandatory | Extra |
|--------|-----------|-------|
| Jaime Simões | US030 *(shared)*, US031, US050, US052 | — |
| Cláudio Pinto | US030 *(shared)*, US055, US056, US060 | — |
| Dinis Silva | US030 *(shared)*, US057, US061, US070 | US058 |
| Fábio Costa | US030 *(shared)*, US032, US062, US071 | US063 |
| André Barcelos | US030 *(shared)*, US033, US041, US072 | US064 |

---

### LPROG (2 Total — Team-wide)

| US | Description |
|----|-------------|
| US081 | Create flight plan from file |
| US083 | Flight DSL specification and validation |

All team members participate in LPROG implementation.

---

### SCOMP (5 Total — Theory First, Then Implementation)

| Member | US | Description |
|--------|----|-------------|
| Jaime Simões | US100 | Simulate flights in a given area |
| Cláudio Pinto | US101 | Capture and process flight movements |
| Dinis Silva | US102 | Detect aircraft safety violations in real time |
| Fábio Costa | US103 | Synchronize flight execution with a time step |
| André Barcelos | US109 | Generate and store final simulation report |

---

## Weekly Plan

### Week 1 — 13 April – 19 April

**Sprint 1 Review & Defense**
- EAPLI Sprint 1 defense: review domain model, aggregate design, receive feedback
- LAPR4 Sprint 1 review: evaluate commits, issue closure, documentation adherence

**EAPLI — Framework Exploration & Setup**
- Explore EAPLI framework structure (entities, services, repositories)
- Review domain-driven design patterns used in framework
- Plan entity implementation strategy for configuration entities

**LPROG — DSL Foundation & Grammar Design**
- Analyze Flight DSL requirements in detail
- Design ANTLR grammar structure for Core Flight DSL
- Plan semantic validation rules and prototype lexer/parser approach

**SCOMP — Theory & Fundamentals**
- Study inter-process communication (pipes), signal handling (SIGUSR1, SIGTERM), shared memory and semaphores
- Practice with basic C exercises on processes and signals
- Foundation for US100–US103, US109

**Team Activities**
- Sprint 1 retrospective
- Review Sprint 2 objectives and constraints
- Create detailed task breakdown per US and establish integration points

---

### Week 2 — 20 April – 26 April

**EAPLI — Core Configuration Entities**
- US031 – Register users *(Jaime)*
- US050 – Register air control area *(Jaime)*
- US052 – Create airport *(Jaime)*
- Apply framework patterns: aggregates, value objects, services

**LPROG — Lexer & Parser Implementation**
- Implement ANTLR lexer rules for Core Flight DSL
- Begin parser rules and parse tree traversal foundations
- Test lexer with sample tokens
- *(All team members)*

**SCOMP — Process Management & Flight Simulation**

| Member | Task |
|--------|------|
| Jaime | US100 — process forking and flight simulation structure |
| Cláudio | US101 — inter-process communication with pipes |
| Dinis | US102 — begin safety violation detection logic |
| Fábio | US103 — begin time-step synchronization design |
| André | US109 — begin report generation structure |

**User Stories in Scope:** US031, US050, US052 · US083 (phase 1) · US100, US101

---

### Week 3 — 27 April – 2 May

**EAPLI — Company & Personnel Management**

| Member | User Stories |
|--------|-------------|
| Cláudio | US055, US056, US060 |
| Dinis | US057, US061 |
| Fábio | US062 |

**LPROG — Parser Completion & Semantic Validation**
- Complete parser rules for Core Flight DSL
- Implement parse tree traversal (listeners and visitors)
- Build flight plan internal representation
- *(All team members)*

**SCOMP — Safety Violation Detection**

| Member | Task |
|--------|------|
| Jaime | US100 — expand simulation capabilities |
| Cláudio | US101 — enhance pipe communication |
| Dinis | US102 — safety violation detection with signals |
| Fábio | US103 — time-step synchronization implementation |
| André | US109 — report data structures |

**User Stories in Scope:** US055, US056, US057, US060, US061, US062 · US083 · US102

---

### Week 4 — 11 May – 17 May

**EAPLI — Fleet & User Management (+ Extra USs)**

| Member | User Stories |
|--------|-------------|
| Dinis | US070, US058 (extra) |
| Fábio | US071, US032, US063 (extra) |
| André | US072, US033, US064 (extra) |

Also: bootstrap data loading using framework.

**LPROG — Flight Plan File Import**
- Complete semantic validation and implement US081
- Error message generation and integration with EAPLI entities
- *(All team members)*

**SCOMP — Flight Simulation Completion & Integration**

| Member | Task |
|--------|------|
| Jaime | Finalize US100 |
| Cláudio | Finalize US101 |
| Dinis | Finalize US102 |
| Fábio | Complete US103 |
| André | Complete US109 |

Integration testing of complete simulation engine.

**User Stories in Scope:** US070, US071, US072, US032, US033, US058, US063, US064 · US081, US083 · US103, US109

---

## Key Dependencies & Integration Points

```
EAPLI ──► LPROG    Flight plan data structure accessible to DSL parser
                   User authentication required for flight plan submission

EAPLI ──► SCOMP    Aircraft and flight data retrievable for simulation
                   Authentication required before accessing simulation

LPROG ──► SCOMP    Parsed flight plans convertible to simulation format
                   DSL validation must complete before simulation starts

All   ──► DB (JPA) All data persisted via JPA
                   Bootstrap initializes all required entities
```

---

## Non-Functional Requirements

| NFR | Requirement |
|-----|-------------|
| NFR03 | Unit test coverage ≥ 90% for Java domain and controller packages (EAPLI/LPROG) |
| NFR06 | All commits must maintain valid, compilable state |
| NFR02 | All development documented in `docs/` using the Analysis → Design → Implementation → Tests template |
| NFR07 | Build and deployment scripts functional on Linux |
| SCOMP | Oral explanations and technical depth heavily evaluated in lab classes |
| SCOMP | **LLM-generated code strictly prohibited — immediate zero score if detected** |

---

## Notes

- **EAPLI:** Use framework patterns (entities, repositories, services); follow the Analysis → Design → Implementation → Tests template
- **LPROG:** Start from scratch with ANTLR grammar; implement without LLM assistance; follow documentation template
- **SCOMP:** Week 1 — theory and C exercises; Weeks 2–4 — implement assigned USs; **NO LLM CODE (zero score)**
- Regular commits expected from all members (multiple times per week minimum)
- All code must compile and pass tests on every commit
- Peer reviews required before merging to main
- Daily synchronization recommended to prevent integration conflicts
- Product Owner (LAPR4 RUC) available for functional clarifications