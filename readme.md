# AISafe — Air Traffic Control Management System

A comprehensive aircraft flight control back-office management information system developed as a **4th semester integrative project** at ISEP (Instituto Superior de Engenharia do Porto), spanning four course units: EAPLI, LPROG, SCOMP, and RCOMP.

Built by a 5-person Scrum team across three development sprints.

---

## What It Does

AISafe is a full back-office platform for air traffic operations. It allows operators to manage aircraft fleets, define flight routes, create flight plans using a custom domain-specific language, run physics-based collision simulations, and access the system remotely via TCP/IP.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    JavaFX GUI (20+ screens)                            │
│     Dashboard │ Aircraft │ Airports │ Weather │ Flights │ Admin       │
├─────────────────────────────────────────────────────────────┤
│             EAPLI Framework (DDD + Repository Pattern)                 │
├─────────────────────────────────────────────────────────────┤
│      ANTLR4 DSL       │   C Simulation     │   TCP/UDP Networking      │
│     (Flight Plans)    │  (Collision Det)   │   (Remote Access)         │
├─────────────────────────────────────────────────────────────┤
│                  JPA / Hibernate (PostgreSQL / H2)                     │
└─────────────────────────────────────────────────────────────┘
```

### Module Breakdown

#### 1. Java Enterprise Backend (`aisafe.base/`)
- **Domain-Driven Design**: Aggregates for Aircraft, Airport, Flight, FlightRoute, WeatherData, Collaborator (ATC, Pilot, WeatherPerson), AirControlArea
- **Layered Architecture**: UI → Controller → Service → Repository → JPA/Hibernate
- **Role-Based Access Control**: Admin, ATC Operator, Pilot, Weather Personnel with distinct permissions
- **20+ JavaFX Screens**: Full CRUD for all domain entities, dashboard with statistics, user management
- **JPA Inheritance**: `SINGLE_TABLE` strategy for Collaborator hierarchy (Pilot, ATC, WeatherPerson, FCO)
- **Testing**: JUnit 5 + Mockito + JaCoCo for coverage reporting

#### 2. Custom Flight Plan DSL (`aisafe.dsl/`)
- **ANTLR4 Grammar**: 22 case-insensitive keywords (FLIGHT, LEG, ROUTE, DEPARTURE, ARRIVAL, FUEL, SEGMENT, etc.)
- **3-Phase Validation Pipeline**:
  - Lexical analysis (tokenization)
  - Syntactic validation (grammar checking)
  - Semantic validation (11 business rules — unique IDs, positive fuel, leg connectivity, chronological ordering, route matching, no airport revisits, coordinate validity, altitude/width positivity, date validity, flight-type/schedule-type consistency)
- **Grammar Extensions**: Comments, altitude corridor width, multiple altitude slots per segment, 6 extra aviation units (ft, km, km/h, kt), ISO 8601 timestamps with timezone, schedule type tied to flight type
- **36 Test Files**: 14 valid + 22 invalid covering all error cases

#### 3. C Flight Simulation (`scomp/`)
- **Sprint 2 — Multi-Process Architecture**: `fork()` creates one child process per flight. Pipes for IPC. Two-phase blocking pipe barrier for time-step synchronization
- **Sprint 3 — Hybrid Architecture**: POSIX shared memory (`shm_open`/`mmap`) + `fork()` + `pthread` threads. Named semaphores for step synchronization with EINTR-safe retry loops
- **Physics Engine**: Climb/cruise/descend profile interpolation with wind drift modeling from real weather data (Open-Meteo API)
- **Collision Detection**: ICAO Doc 4444 standards — horizontal separation = 5 NM (9,260 m), vertical separation = 1,000 ft (305 m). Predictive detection (30 seconds ahead)
- **Real-time Terminal UI**: 70×20 ASCII airspace map with colored aircraft markers (`*` for violations, `.` for trails), flight status list (WAIT/IN/OUT/DONE), collision banners
- **13 Scenarios**: 7 PASS + 6 FAIL covering normal operation, boundary cases, guaranteed collisions
- **Weather Integration**: JSON weather files from 3 providers (CW, HP, Synthetic) applied as wind drift to aircraft trajectories

#### 4. TCP/UDP Networking Layer (`rcomp/`)
- **Custom Protocol**: Text-based, pipe-delimited (`COMMAND|arg1|arg2|...\n`), UTF-8 encoded
- **3 Embedded TCP Daemons** (runs as background threads in the main JavaFX app):
  - Weather Person (port 1044)
  - ATC Collaborator (port 1078)
  - Pilot/FCO (port 1086)
- **Authentication**: Client sends `AUTH|user|pass` → Server responds `AUTH_OK` or `AUTH_FAIL`
- **UDP Logging Server** (port 9090): Captures LOGIN_OK, LOGIN_FAIL, LOGOUT, DISCONNECT events
- **HTTP Web Dashboard** (port 8080): Browser-based log viewer for remote access events

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21, C (gcc), SQL |
| Framework | EAPLI Framework v25.0.0 |
| GUI | JavaFX 21, FXML, Custom CSS |
| Persistence | JPA 3.x (Hibernate), PostgreSQL 42.7, H2 (dev/test) |
| DSL | ANTLR 4.13.1 |
| Simulation | C (POSIX), pthreads, `fork()`, shared memory, named semaphores |
| Networking | TCP/UDP, custom protocol, HTTP |
| Build | Apache Maven (multi-module) |
| Testing | JUnit 5.10, Mockito 5.7, JaCoCo 0.8.12 |
| Diagrams | PlantUML |
| Methodology | Scrum (3 sprints, 4 weeks each) |

---

## My Contributions (Fábio Costa)

| Course Unit | Sprint | What I Built |
|-------------|--------|--------------|
| **SCOMP** | Sprint 2 | US103: Time-step barrier synchronization using two-phase blocking pipes (`send_go()` / `collect()`) across forked child processes |
| **SCOMP** | Sprint 3 | US108: Step simulation synchronization with POSIX named semaphores (`sem_step_start` / `sem_step_done`) + EINTR-safe retries |
| **LPROG** | Sprint 2 | Full ANTLR4 flight plan DSL grammar design, 3-phase validation pipeline, 11 semantic rules, 6 grammar extensions, 36 test files |
| **LPROG** | Sprint 3 | Visitor-based formatting (+30 formatting extensions), JetBrains Mono monospace font integration |
| **EAPLI** | All sprints | Backend domain logic (controllers, services, repositories), JPA entity mapping, GUI screens |

---

## Team

| Member | Main Role |
|--------|----------|
| Jaime Simões | SCOMP — Simulation initialization (US100), Hybrid environment (US105) |
| Cláudio Pinto | SCOMP — Flight physics (US101), Violation detector thread (US106) |
| Dinis Silva | SCOMP — Violation detection (US102), Report notification thread (US107) |
| **Fábio Costa** | SCOMP — Barrier/semaphore synchronization, LPROG — DSL grammar & validation, EAPLI — Backend |
| André Barcelos | SCOMP — Final report generation (US109) |

---

## Setup

```bash
# Prerequisites: Java 21, Maven, PostgreSQL

# Clone
git clone https://github.com/1240826/aisafe-flight-control.git
cd aisafe-flight-control/aisafe.base

# Build
mvn clean install -DskipTests

# Run GUI
./run-gui.sh        # Linux/Mac
run-gui.bat         # Windows

# Run Console
./run-backoffice.sh
run-backoffice.bat

# Run Simulation (C engine)
cd ../scomp/Sprint3/files
make
./simulator
```

---

## License

Academic project developed at ISEP. Repository made public after semester completion with department approval.
