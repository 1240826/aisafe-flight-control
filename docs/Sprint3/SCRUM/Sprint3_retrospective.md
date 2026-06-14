# Sprint 3 Retrospective Report
## AISafe Flight Control System

**Sprint Duration:** 18 May – 14 June 2026  
**Team:** Jaime Simões, Cláudio Pinto, Dinis Silva, Fábio Costa, André Barcelos  
**Scrum Master:** LAPR4 PL Teacher

---

## Deliverables Completed

### EAPLI

| US | Description | Owner |
|----|-------------|-------|
| US042 | Import bulk weather data from CSV | Cláudio Pinto |
| US043 | Consult weather data | Jaime Simões |
| US073 | Create a flight route | Jaime Simões |
| US074 | Deactivate a flight route | André Barcelos |
| US075 | Add a pilot | Dinis Silva |
| US076 | List pilot roster | Dinis Silva |
| US077 | Remove a pilot | Dinis Silva |
| US080 | Create a flight plan | Jaime Simões |
| US082 | Insert weather data for a flight | Cláudio Pinto |
| US085 | Test/validate flight plan (LAPR4 demo) | Fábio Costa |
| US086 | Pilot remote access — EAPLI side | Fábio Costa |
| US111 | Generate simulation report | André Barcelos |
| US112 | Monthly report generation | Cláudio Pinto |

**Key technical decisions:**
- Domain model revised with 13 documented changes from Sprint 2 baseline (US010 readme + glossary)
- `Pilot` separated from `Collaborator` — now a standalone aggregate root with `licenseNumber` as identity, `certifications` (`Set<AircraftModelCode>`), and reference to `AirTransportCompany` by ID (per client clarifications C05, C07, C16)
- `FlightPlan` embedded inside `Flight` aggregate root (`CascadeType.ALL`) per Sprint 3 domain model
- `FlightPlanStatus` lifecycle: `DRAFT` → `IN_TEST` → `TEST_PASSED` / `TEST_FAILED`
- Flight route soft-delete only (never physical DELETE) — deactivation date blocks future flight creation (client clarification C11)
- `FlightPlan.dslContent` stored as raw String for re-validation and JSON export
- Weather data import: inline CSV parsing in controller with ACA header mapping (`# ACA N = COD`), European decimal format (`,`, `;`), invalid rows reported without aborting valid ones
- Monthly report: calendar month, DB-sourced, plain text, single Air Control Area; foundational design for future report types (Strategy pattern)
- All controllers follow TDD with full coverage of constructor invariants, business rules, and authorization guards
- PostgreSQL migration completed (NFR08) — `H2` replaced with remote `PostgreSQL` via `persistence.xml`

---

### LPROG

| US | Description |
|----|-------------|
| US120 | Flight DSL specification and validation — full ANTLR grammar, 3-phase pipeline (lexical → syntactic → semantic), listeners/visitors, internal AST representation, 11 semantic validation rules |
| US121 | Create a valid flight plan from a file — DSL pipeline integration with EAPLI domain layer, cross-entity validation (airports, aircraft, pilot, route) and `FlightPlan` persistence |

**Key technical decisions:**
- ANTLR4 grammar (`FlightPlan.g4`) extended from Sprint 2 baseline with: regular/charter scheduling, ISO-8601 timestamps with timezone offsets, aircraft and pilot declarations, leg structure with departure/arrival/fuel/segments
- 3-phase pipeline (`FlightPlanRunner`): `FlightPlanErrorListener` (lexer+parser errors with line:column), `SemanticValidationListener` (11 semantic rules R1–R11), `FlightPlanPrinterVisitor` (AST summary)
- Semantic rules cover: altitude ranges, speed limits, waypoint validity, engine model compatibility, wake turbulence, passenger capacity, non-ASCII detection
- Grammar integrated as `aisafe.dsl` Maven module with `<visitor>true</visitor>` and `<listener>true</listener>` ANTLR plugin configuration
- `ImportFlightPlanController` (US121) uses full ANTLR pipeline at import time; returns `DslValidationResult` record with per-phase error lists, extracted `FlightDesignator`, and created `FlightPlan`
- Cross-entity validation: referenced airports, aircraft registration, pilot ID, and flight route must all exist in the database; pilot must belong to same company as aircraft
- Only valid DSL may be imported — system rejects `.flightplan` files that fail any validation phase

---

### RCOMP

| US | Description | Owner |
|----|-------------|-------|
| US044 | Weather Person remote access — TCP client application | Dinis Silva |
| US078 | ATCC remote access — TCP client application | Jaime Simões / André Barcelos |
| US086 | Pilot remote access — TCP client application (RCOMP side) | Fábio Costa |
| US090 | External logging of remote accesses — UDP datagrams to cloud logging server | André Barcelos |
| US091 | Remote accesses logging visualization — HTTP server + AJAX pages (events + active users) | Cláudio Pinto |

**Key technical decisions:**
- TCP server embedded in main EAPLI application — one handler per role (Weather Person, ATCC, Pilot)
- Request-response protocol: `COMMAND:ARGS` → server processes via `RemoteService` wrappers → `RESPONSE_CODE:payload`
- `RemotePilotService`, `RemoteAtccService`, `RemoteWeatherService` wrap existing controllers and enforce authz via `AuthzRegistry` — no direct database access from client (RCOMP rule)
- UDP event logging (US090): every login/logout/disconnect event sent via UDP datagram to a dedicated `log_server` running on a cloud node; includes timestamp, username, client IP/port, service identifier
- HTTP+AJAX visualization (US091): embedded HTTP server in `log_server` serving two pages — recent events list and active users list — both auto-refreshed via AJAX without page reload
- Three dedicated TCP client applications (Weather Person, ATCC, Pilot) — each connects to the main application, authenticates, and provides a menu of available commands matching their role-specific USs
- All remote access subjected to authentication and authorization (NFR09)

---

### SCOMP

| US | Description | Owner |
|----|-------------|-------|
| US105 | Initialise hybrid simulation environment — shared memory segment, parent threads, child flight processes | Jaime Simões |
| US106 | Implement function-specific threads (violation detector + report generator) | Cláudio Pinto |
| US107 | Notify report thread via condition variables upon safety violation | Dinis Silva |
| US108 | Enforce step-by-step synchronisation with POSIX semaphores (lockstep barrier) | Fábio Costa |
| US109 | Generate and store final simulation report — per-flight status, timestamps, violation vectors, PASS/FAIL | André Barcelos |
| US110 | Integrate environmental influences (wind) — environment thread, JSON weather file, velocity perturbation | All team |

**Key technical decisions:**
- Hybrid architecture: parent process spawns 3–4 threads (violation detector, report generator, environment, main loop) + N child processes (one per flight)
- Shared memory (`shm_open` + `mmap` with `MAP_SHARED`) for `SimulationState`, `FlightData[N]`, and thread coordination data
- POSIX process-shared mutexes (`PTHREAD_PROCESS_SHARED`) for safe access to shared memory from parent threads and child processes
- POSIX named semaphores for step barrier: parent opens N+1 semaphores (`sem_open`), posts N `start` semaphores → children compute → each child posts 1 `done` semaphore → parent waits on all N → detects violations → advances
- US106: dedicated `violation_detector` thread scans shared memory every step for conflicts; `report_generator` thread waits on condition variable (US107) for real-time violation logging
- US107: `pthread_cond_signal` + `pthread_cond_wait` with `PTHREAD_COND_INITIALIZER` (process-shared attribute) — report thread wakes on violation, logs to cumulative report buffer
- US108: semaphore-based lockstep replaces Sprint 2's two-phase pipe barrier — no reads/writes on pipes; named semaphores (`/sim_start_N`, `/sim_done_N`) inherently process-shared via kernel namespace
- US109: final report includes per-flight status (COMPLETED/VIOLATION), safety violation events with timestamps, 3D positions, velocity vectors; deterministic PASS/FAIL output
- US110: environment thread reads weather JSON at startup (zone-based wind speed and direction), applies wind drift to each flight's position and velocity per step; fallback synthetic wind generator when no weather file provided
- SCOMP binary (`simulation`) built via Makefile with strict compilation flags (`-Wall -Wextra -pedantic`)
- Standalone `sim_server` executable listens on TCP port for Java `SocketSimulationRunner` connections, forks+execs `./simulation` per request

---

## Cross-Unit Integration

Sprint 3 achieved the first full integration of all four course units:

```
┌──────────────────────────────────────────────────────────────────────┐
│  EAPLI (Java)                                                        │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────────┐              │
│  │ Backoffice│   │ TCP Server   │   │ ImportFlightPlan │             │
│  │ Console   │   │ (embedded)   │   │ Controller       │             │
│  └─────┬─────┘   └──────┬───────┘   └────────┬─────────┘             │
│        │                │                     │                      │
│        │      ┌─────────▼─────────────────────▼──────────────┐       │
│        │      │  Domain Layer: Flight, FlightPlan, Pilot,    │       │
│        │      │  Route, WeatherData, SimulationReport        │       │
│        └──────┤  Persistence: PostgreSQL (NFR08)             │       │
│               └──────────────────────────────────────────────┘       │
│                                                                      │
│  ┌──────────────────────┐    LPROG (Java)                            │
│  │  aisafe.dsl module   │────────────────────────────────────────    │
│  │  ANTLR 3-phase       │   Grammar → Lexer → Parser → Semantic      │
│  │  FlightPlanRunner    │   Validation → AST → Domain Entities       │
│  └──────────────────────┘                                            │
│                                                                      │
│  RCOMP (Java + C)                                                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐    │
│  │ TCP Client (WP)  │  │ TCP Client (ATCC)│  │TCP Client (Pilot)│    │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘    │
│           │                     │                     │              │
│           └─────────────────────┼─────────────────────┘              │
│                                 │                                    │
│                    UDP Log ──► log_server (HTTP+AJAX)                │
│                                                                      │
│  SCOMP (C)                                                           │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  Parent Process (simulation)                                 │    │
│  │  ├── Violation Detector Thread                               │    │
│  │  ├── Report Generator Thread (cond vars)                     │    │
│  │  ├── Environment Thread (wind)                               │    │
│  │  └── Main Loop (semaphore sync)                              │    │
│  │                                                              │    │
│  │  Shared Memory: SimulationState + FlightData[N]              │    │
│  │                                                              │    │
│  │  Child Process 0 ... Child Process N-1                       │    │
│  │  (one per flight, semaphore-synchronized)                    │    │ 
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Integration points:                                                 │
│  • LPROG → EAPLI: FlightPlanRunner called by ImportFlightPlanCtrl    │
│  • EAPLI → SCOMP: JSON export → ProcessBuilder / TCP Socket          │
│  • SCOMP → EAPLI: Report file → SimulationReport persistence         │
│  • RCOMP ↔ EAPLI: TCP clients → embedded server → RemoteServices     │
│  • RCOMP → RCOMP: UDP events → log_server → HTTP/AJAX pages          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## LAPR4 Feedback

**Assessment:** Awaiting formal Sprint 3 review.

**Actions from Sprint 1 and Sprint 2 feedback maintained:**
- Issues properly closed and linked to commits throughout the sprint
- Regular distributed commits from all team members
- Documentation follows Analysis → Design → Implementation → Tests template for all USs
- Sprint 3 planning document defined at sprint start (issue #66)
- `docs/` folder complete with readme.md, PlantUML diagrams (PNG + source), and test documentation for every US

---

## EAPLI Assessment

**Sprint 1 defense result:** Positive  
**Sprint 2 defense result:** Positive  
**Sprint 3 defense:** Scheduled at sprint end

**Professor feedback incorporated:**
- `Pilot` separated from `Collaborator` hierarchy per client clarification C05 — Pilot is a standalone aggregate root, not a subclass
- Domain model kept as a living artefact — Sprint 2 docs unchanged, Sprint 3 revision in separate `us_010` document with 13 documented changes + glossary
- `FlightPlan` embedded inside `Flight` aggregate (not standalone) — lifecycle owned by Flight
- All aggregates reference other aggregates by ID (not direct object reference), keeping DDD boundaries clean

---

## Client Clarifications

19 client clarifications (C01–C19) captured and tracked in `docs/Sprint3/clarifications/client_clarifications.md`. Key decisions included:

- NFR08 remote persistent RDBMS (PostgreSQL + PostGIS recommended)
- CSV weather data format (European decimals, header-based ACA mapping)
- US080/US081/US085 workflow: DSL import → weather addition → validation & simulation
- Pilot distinct from Collaborator; pilot identified by license number, not email
- Route deactivation is soft-delete only (never physical DELETE)
- Flight-FlightPlan one-to-one exclusivity
- Monthly report: calendar month, DB-sourced, plain text, single ACA
- Simulation report: Java-C integration via JSON export and report file parsing

---

## Team Performance

### What Worked Well

* First-time integration of all four course units (EAPLI + LPROG + RCOMP + SCOMP) into a single working prototype
* Clear assignment of USs per member — all 28 USs across four CUs delivered
* Domain model iteratively revised alongside implementation — 13 documented changes, coherence maintained throughout sprint
* ANTLR 3-phase DSL pipeline integrated end-to-end: grammar → validation → domain entities → C simulation
* TCP remote access implemented for all three roles (Weather Person, ATCC, Pilot) with unified protocol
* SCOMP hybrid architecture (shared memory + threads + semaphores + condition variables) designed and implemented from scratch
* C simulator invoked from Java via two communication modes (local ProcessBuilder + TCP socket to sim_server)
* Client clarifications tracked centrally and applied consistently across all USs
* PostgreSQL migration completed (NFR08) — system runs against remote persistent RDBMS
* Cross-cutting US085 delivered as LAPR4 demo — DSL validation + JSON export + C simulation + report parsing
* All PlantUML diagrams generated (PNG + source) and included in documentation
* Maven build compiles and all Java tests pass

### What Could Improve

* Commit frequency could be more consistent — some periods show concentrated commits near deadlines
* More integration tests across unit boundaries (EAPLI ↔ LPROG, EAPLI ↔ SCOMP) would strengthen confidence
* C simulation could benefit from more automated test scenarios beyond the 13 documented test cases
* GUI remains console-based; a graphical interface would improve usability for demonstrations

---

## Sprint 3 vs Sprint 2 — Comparison

| Metric | Sprint 2 | Sprint 3 |
|--------|----------|----------|
| Duration | 6 April – 17 May (6 weeks) | 18 May – 14 June (4 weeks) |
| USs delivered (EAPLI) | 16 (+3 extra) | 13 |
| USs delivered (LPROG) | 2 (Phase 1) | 2 (Phase 2 + integration) |
| USs delivered (RCOMP) | 0 | 5 |
| USs delivered (SCOMP) | 5 | 6 |
| Total USs | 22 | 28 |
| Course units integrated | 3 (EAPLI + LPROG + SCOMP) | 4 (EAPLI + LPROG + RCOMP + SCOMP) |
| Architecture | Pipes + processes | Shared memory + threads + semaphores |
| Database | H2 in-memory / file | PostgreSQL (remote persistent) |
| Remote access | None | TCP clients for all 3 roles |
| DSL validation | Lexical + syntactic | Lexical + syntactic + semantic + AST |

---

## Project Conclusion

Sprint 3 marks the final delivery of the AISafe Flight Control System. Over three sprints (17 March – 14 June 2026), the team delivered a fully integrated prototype covering all four course units:

- **EAPLI:** 38 user stories — full domain model with 15+ aggregates, authentication/authorization, weather management, flight routes, pilots, flight plans, simulation reports, and monthly reporting
- **LPROG:** 4 user stories — ANTLR4 Flight DSL with 3-phase validation pipeline, semantic rules, AST construction, and EAPLI domain integration
- **RCOMP:** 5 user stories — TCP remote access for three roles, UDP event logging, HTTP+AJAX visualization, and cloud-based logging server
- **SCOMP:** 11 user stories — hybrid simulation architecture (processes + threads + shared memory + semaphores + condition variables), violation detection, wind integration, and report generation

The system respects all non-functional requirements: Scrum management (NFR01), markdown/UML documentation (NFR02), >=90% test coverage on domain and controllers (NFR03), CI/CD via GitHub Actions (NFR05), PostgreSQL remote database (NFR08), authz on all operations (NFR09), and Java as main language (NFR10).

---

**Date:** 14 June 2026
