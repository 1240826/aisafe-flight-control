# Sprint 2 Retrospective Report
## AISafe Flight Control System

**Sprint Duration:** 6 April – 17 May 2026  
**Team:** Jaime Simões, Cláudio Pinto, Dinis Silva, Fábio Costa, André Barcelos  
**Scrum Master:** LAPR4 PL Teacher

---

## Deliverables Completed

### EAPLI

| US | Description | Owner |
|----|-------------|-------|
| US030 | Authentication and authorization (shared) | All |
| US031 | Register users | Jaime Simões |
| US032 | Disable/enable users | Fábio Costa |
| US033 | List users | André Barcelos |
| US041 | Register weather data | André Barcelos |
| US050 | Register air control area | Jaime Simões |
| US052 | Create airport | Jaime Simões |
| US055 | Create aircraft model | Cláudio Pinto |
| US056 | Create aircraft engine model | Cláudio Pinto |
| US057 | Add engine model to aircraft model | Dinis Silva |
| US058 | Remove engine model from aircraft model *(extra)* | Dinis Silva |
| US060 | Register air transport company | Cláudio Pinto |
| US061 | Add customer's collaborator | Dinis Silva |
| US062 | List customer's collaborators | Fábio Costa |
| US063 | Edit customer's collaborator *(extra)* | Fábio Costa |
| US064 | Disable customer's collaborator *(extra)* | André Barcelos |
| US070 | Add aircraft to air transport company | Dinis Silva |
| US071 | Decommission aircraft | Fábio Costa |
| US072 | List company fleet | André Barcelos |

**Test results:** 425 tests, 0 failures — BUILD SUCCESS

**Key technical decisions:**
- Collaborator refactored from class inheritance (`ATCCollaborator`, `FlightControlOperator`, `WeatherPerson`) to factory method pattern (`Collaborator.ofATC()`, `.ofFlightControlOperator()`, `.ofWeatherPerson()`) with `CollaboratorType` enum stored as `@Enumerated(EnumType.STRING)` — following professor feedback against abstract base classes
- Domain model revised and kept coherent with code throughout sprint — 20+ documented changes from Sprint 1 baseline
- `SecurityClearance` and `SkillsAssessment` implemented as immutable VOs; updating replaces the reference, never mutates the object
- Client clarifications captured and tracked in `docs/Sprint2/client_clarifications.md` (20 entries)

---

### LPROG

| US | Description |
|----|-------------|
| US081 | Create a flight plan from a file — lexical + syntactic validation (Phase 1) |
| US083 | Flight DSL specification and formal ANTLR4 grammar |

**Key technical decisions:**
- ANTLR4 grammar (`FlightPlan.g4`) covers full flight plan structure: flights, legs, routes, segments, departure, arrival, fuel, coordinates, altitudes, wind
- Keywords case-insensitive via ANTLR fragment rules (pattern from LPROG slides)
- ICAO code rule placed before IATA to ensure longest-match wins (`EDDF` → ICAO, not IATA + remainder)
- Custom `FlightPlanErrorListener` collects all lexer and parser errors before reporting — all errors surfaced at once, not only the first
- `WIDTH` keyword and inline comments added as extensions to Core DSL; route placed at `flightDecl` level (not inside `legDecl`) to match spec section 3.4.1
- Semantic validation deferred to Phase 2 (Sprint 3); ANTLR plugin configured with `<visitor>true</visitor>` and `<listener>true</listener>` — base classes generated and ready

---

### SCOMP

| US | Description | Owner |
|----|-------------|-------|
| US100 | Simulate flights in a given area — process forking, pipe layout, area tracking | Jaime Simões |
| US101 | Capture and process flight movements — position integration, history buffer | Cláudio Pinto |
| US102 | Detect aircraft safety violations in real time — breach + predictive detection, SIGUSR1 | Dinis Silva |
| US103 | Synchronize flight execution with a time step — two-phase pipe barrier | Fábio Costa |
| US109 | Generate and store final simulation report — per-flight status, safety events, PASS/FAIL | André Barcelos |

**Key technical decisions:**
- One child process per flight; all pipes created before any `fork()` so every child inherits all descriptors
- Timestep = 1 second — professor clarification confirmed that larger steps (30s) would allow converging aircraft to enter and leave the 5 NM safety cylinder in a single step without detection
- ICAO safety cylinder: horizontal 5 NM (9 260 m) and vertical 305 m (ICAO Doc 4444, PANS-ATM Ch. 8) defined as `#define` in `common.h` — no hardcoded values
- US102 implements both current breach detection and predictive detection (`LOOKAHEAD_S = 30` seconds) using linear extrapolation of velocity vectors; lower aircraft climbs `ALT_ADJUST_M = 400 m` to restore separation
- US103 two-phase pipe barrier: parent writes `GoToken{step, safe, alt_adjust}` to each child (Phase 1), then blocks on `read()` from each child before advancing (Phase 2) — no semaphores or mutexes needed
- Position history: circular buffer of 600 snapshots (10 minutes at 1s/step) per flight — used by predictive detector and final report
- US109 report generated after all children reaped with `waitpid()` — no concurrent access; Sprint 3 will replace with a thread (professor clarification: "thread should be read as process in Sprint 2")

---

## LAPR4 Feedback

**Assessment:** Awaiting formal Sprint 2 review

**Actions from Sprint 1 feedback maintained:**
- Issues properly closed and linked to commits
- Regular distributed commits from all team members
- Documentation follows Analysis → Design → Implementation → Tests template throughout

---

## EAPLI Assessment

**Sprint 1 defense result:** Positive  
**Sprint 2 defense:** Scheduled at sprint end

**Professor feedback incorporated:**
- Abstract class inheritance for Collaborator variants rejected — replaced with factory methods and `CollaboratorType` enum
- Domain model kept as a living artefact — Sprint 1 docs unchanged, Sprint 2 revision in separate document

---

## Team Performance

### What Worked Well

* Clear assignment of USs per member — no overlaps or gaps at delivery
* Client clarifications tracked centrally (`client_clarifications.md`) and applied consistently
* Domain model revised iteratively alongside implementation — coherence maintained
* SCOMP integration — five separate US implementations (process, pipe, signals, detection, report) merged into a single working simulation binary
* Extra USs (US058, US063, US064) delivered on schedule

### What Could Improve

* Commit more frequently
* Close issues promptly at delivery
* Continue improving documentation — more diagrams, clearer explanations of design decisions

---

## Action Items for Sprint 3

* **EAPLI:** Implement FlightPlan domain objects and LPROG integration; add remaining USs per Sprint 3 backlog
* **LPROG:** Phase 2 — semantic validation, visitor pattern for internal representation, integration with EAPLI `FlightPlan` entity
* **SCOMP:** Replace process-based report generation with a thread (US109); implement Sprint 3 simulation USs
* **LAPR4:** Maintain commit frequency; close all Sprint 3 issues at sprint end; continue documentation template

---

## Sprint 3

**Focus Areas:**
* EAPLI: Flight plan persistence and management, user workflows, Sprint 3 US backlog
* LPROG: DSL Phase 2 — semantic validation, visitor, EAPLI integration
* SCOMP: Threading (replace processes where specified), extended simulation scenarios
* LAPR4: Integration testing of full EAPLI + LPROG + SCOMP pipeline

---

**Date:** 17 May 2026
