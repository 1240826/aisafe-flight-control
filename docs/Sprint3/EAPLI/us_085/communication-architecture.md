# Communication Architecture: Java ↔ C Simulator

## Pipeline Overview

The system allows a flight plan described in DSL (Domain-Specific Language) to be imported, validated, executed in the C simulator and the report processed. This document describes the communication flow between all components.

## Full Flow: Import → Validate → Simulate → Result

```
User / RemotePilotService
    │
    │  ① DSL text (e.g., "departure LIS 10:00; arrival OPO 11:00; ...")
    ▼
┌──────────────────────────────────────────────┐
│          ImportFlightPlanController          │
│                                              │
│  ┌─────────┐   ┌──────────┐   ┌──────────┐   │
│  │ ANTLR   │ → │ ANTLR    │ → │ Semantic │   │
│  │ Lexer   │   │ Parser   │   │ Validator│   │
│  └─────────┘   └──────────┘   └──────────┘   │
│       ↓              ↓              ↓        │
│   lexical        syntactic       semantic    │
│   errors         errors         errors       │
│                                              │
│  If all phases pass:                         │
│  → Creates FlightPlan (status DRAFT)         │
│  → Persists to database                      │
│  → Returns DslValidationResult               │
└──────────────────────────────────────────────┘
    │
    │  Validated DSL + persisted FlightPlan
    ▼
┌──────────────────────────────────────────────┐
│          TestFlightPlanController            │
│                                              │
│  1. Checks status = DRAFT                    │
│  2. DslValidator.validate() — domain rules   │
│  3. Marks FlightPlan as IN_TEST              │
│  4. FlightPlanExporter.exportForSimulator()  │
│     → Converts DSL to structured JSON        │
│  5. WeatherDataToSimulatorExporter (optional)│
│     → Converts weather data to JSON          │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │      SimulationRunner (interface)      │  │
│  │  TCP Socket  │  ProcessBuilder         │  │
│  └──────┬───────┴─────────────────────────┘  │
└─────────┼────────────────────────────────────┘
          │
          │  Scenario JSON + Weather JSON (TCP)
          ▼
┌──────────────────────────────────────────────┐
│           C++ Simulator                      │
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐   │
│  │json_parse│→ │Simulation│→ │us109_repor│   │
│  │r.h       │  │Threads   │  │t.c        │   │
│  │          │  │+ Sync    │  │           │   │
│  │          │  │+ Weather │  │           │   │
│  └──────────┘  └──────────┘  └───────────┘   │
│                                              │
│  Returns report (text) via TCP               │
└──────────────────────────────────────────────┘
    │
    │  Report (text)
    ▼
┌──────────────────────────────────────────────┐
│          TestFlightPlanController            │
│                                              │
│  6. ReportParser.parse(report)               │
│     → Extracts PASS/FAIL, violations, etc.   │
│  7. flightPlan.recordTestResult()            │
│     → Status → TEST_PASSED or TEST_FAILED    │
│  8. Returns TestResult(passed, msg, report)  │
└──────────────────────────────────────────────┘
    │
    ▼
User / RemotePilotService
```

---

## 1. DSL Import (Flight Plan Script)

The DSL text is received by **ImportFlightPlanController** which submits it to three validation phases using ANTLR:

1. **Lexical Analysis** — `FlightPlanLexer` tokenizes the text. Errors here are "unknown words"
2. **Syntactic Analysis** — `FlightPlanParser` builds a parse tree. Errors here are "malformed grammar"
3. **Semantic Analysis** — `SemanticValidationListener` walks the parse tree and validates business rules (existing airports, realistic altitudes, etc.)

If all phases pass, the controller creates a **FlightPlan** entity with status **DRAFT** and persists it to the database. The result is returned as a **DslValidationResult** (record with `lexicalPassed`, `syntacticPassed`, `semanticPassed`, `allPassed`, `summary`, `flightPlan`).

If any phase fails, the flight plan is **not** persisted and errors are returned to the user.

```
Input:   DSL text (e.g.: "departure LIS 10:00; arrival OPO 11:00; ...")
Output:  DslValidationResult + FlightPlan (DRAFT) persisted
Communication: Local method call (Java → Java)
```

---

## 2. Validation and Preparation for Simulation

**TestFlightPlanController.testFlightPlan()** orchestrates the validation and execution pipeline:

### 2.1 Pre-validation
- Checks the FlightPlan is in **DRAFT** status (cannot re-test without reset)
- **DslValidator** runs domain validations over the DSL (business rules, data integrity)

### 2.2 Status update
- FlightPlan transitions to **IN_TEST** in the database (prevents concurrent execution)

### 2.3 Export to JSON (Java ↔ C contract)
**FlightPlanExporter** converts DSL (text) to structured JSON — this JSON is the **data contract** between Java and C++:

- Tries **FlightPlanToScenarioConverter** (ANTLR-based) for semantic DSL → JSON conversion
- Falls back to regex-based `exportFromDsl()` if ANTLR conversion fails

The generated JSON includes: flight ID, type, route, departure time, flight profiles (climb, cruise, descent), segments with coordinates and altitudes, fuel consumption.

Optional weather data is exported by **WeatherDataToSimulatorExporter** as a second JSON.

```
Input:   FlightPlan (DRAFT) + DSL
Output:  Scenario JSON + Weather JSON (optional)
Communication: Local method call (Java → Java)
```

---

## 3. Sending to the C++ Simulator

Communication with C++ is abstracted by the **SimulationRunner** interface, which has three operation modes.

### 3.1 Binary Protocol (TCP)

All modes exchange data in a length-prefixed binary format (network byte order / big-endian):

```
Java → C++:
   int32  json_length        (4 bytes, big-endian)
   byte[] json_data          (exactly json_length bytes)
   int32  weather_length     (4 bytes, big-endian; 0 = no weather)
   byte[] weather_data       (exactly weather_length bytes, omitted if 0)

C++ → Java:
   int32  report_length      (4 bytes, big-endian; 0 = error)
   byte[] report_data        (exactly report_length bytes, omitted if 0)
```

### 3.2 Execution Modes

#### Mode A — TCP Socket (production)

```
TestFlightPlanController
    ↓
SocketSimulationRunner.run(json, weatherFile?)
    ↓
TCP Socket → C++ simulator (localhost:9999 by default)
    ↓  [4-byte len][JSON][4-byte weather len][weather]
    ↓  ← [4-byte len][report]
```

**SocketSimulationRunner** connects via TCP to the C++ simulator, which may be on a different machine. The C++ server has two implementations:

- **main.c in server mode** — accepts connections, processes the scenario in-process, sends the report back
- **sim_server.c** (standalone) — per connection, `fork()` + `execlp("./simulation", ...)` to isolate scenarios in child processes

**Activated when:** system property `aisafe.simulator.host` is set.

**Configuration:**
| Property | Default | Description |
|----------|---------|-------------|
| `aisafe.simulator.host` | *(empty)* | sim_server IP address |
| `aisafe.simulator.port` | `9999` | TCP port |
| `aisafe.simulator.timeout` | `120` | Timeout in seconds |

#### Mode B — Local ProcessBuilder

```
TestFlightPlanController
    ↓
ProcessBuilderSimulationRunner.run(json, weatherFile?)
    ↓
ProcessBuilder("./simulation", input.json, output.txt, [weather.json])
    ↓
simulation binary (local process)
    ↓
report written to output.txt
```

**Activated when:** `aisafe.simulator.host` is **not** set.

**Configuration:**
| Property | Default | Description |
|----------|---------|-------------|
| `aisafe.simulator.executable` | `aisafe-simulator` | Path to the C binary |
| `aisafe.simulator.timeout` | `120` | Timeout in seconds |

#### Mode C — WSL Bridge (Windows → WSL)

```
TestFlightPlanController
    ↓
ProcessBuilderSimulationRunner
    ↓
aisafe-simulator.cmd → scripts/simulator-bridge.ps1
    ↓
wsl.exe bash -c "./simulation <linux-path> ..."
    ↓
WSL Linux environment → simulation binary
    ↓
report copied back to Windows file system
```

**Activated when:** the configured executable is the `.cmd` bridge and the OS is Windows.

Uses a shared folder (`C:\ARQCP\partilha\SCOMP\SPRINT3`) for file exchange between Windows and WSL.

```
Input:   Scenario JSON + Weather JSON (optional)
Output:  Simulation report (text)
Communication: TCP socket OR local subprocess OR WSL bridge
```

---

## 4. Result Processing

When the report arrives (via TCP or file), **TestFlightPlanController**:

1. **ReportParser.parse(reportContent)** — parses the report text and extracts:
   - Global result (PASS/FAIL)
   - Violation counts (critical, major, minor)
   - Per-flight results
   - Unresolved conflicts

2. **flightPlan.recordTestResult(passed, null, reportContent)** — persists the result:
   - If PASSED → status becomes **TEST_PASSED**
   - If FAILED → status becomes **TEST_FAILED**

3. Returns a **TestResult** (record with `passed`, `message`, `reportContent`) to the caller

```
Input:   Report (plain text)
Output:  TestResult + persisted FlightPlan with updated status
Communication: Local method call (Java → Java)
```

---

## 5. RemotePilotService Integration

**RemotePilotService** is a facade that exposes this pipeline to remote pilots via TCP:

```
PilotClientApp (rcomp)
  │
  │── TCP connect (port 1086)
  │── AUTH|user|pass
  │◀── AUTH_OK
  │
  │── IMPORT_FLIGHT_PLAN|flightId|dslText
  │   └── RemotePilotService.createFlightPlan()
  │       └── ImportFlightPlanController.importFlightPlan()
  │           └── ANTLR (lex → syn → sem) → FlightPlan (DRAFT)
  │◀── OK|result
  │
  │── VALIDATE_FLIGHT_PLAN|flightPlanId
  │   └── RemotePilotService.validateFlightPlan()
  │       └── TestFlightPlanController.testFlightPlan()
  │           ├── FlightPlanExporter → JSON
  │           ├── SocketSimulationRunner → TCP → C++ simulator
  │           ├── ReportParser.parse(report)
  │           └── flightPlan.recordTestResult()
  │◀── OK|PASSED/FAILED
```

`PilotClientHandler` (TCP server in `aisafe.base.app`) receives commands, invokes `RemotePilotService`, and sends responses back to the remote client.

```
Input:   TCP commands (IMPORT_FLIGHT_PLAN, VALIDATE_FLIGHT_PLAN, ...)
Output:  TCP responses (OK|result)
Communication: TCP socket (remote client → Java server)
```

---

## 6. Layered Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  PRESENTATION                                                    │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐      │
│  │ PilotClientApp│  │ImportFlightPlan│  │ TestFlightPlanUI │     │
│  │ (TCP rcomp)   │  │ UI (console)   │  │ (console)        │     │
│  └──────┬───────┘  └───────┬────────┘  └────────┬─────────┘      │
└─────────┼──────────────────┼────────────────────┼────────────────┘
          │                  │                    │
┌─────────┼──────────────────┼────────────────────┼────────────────┐
│  CONTROLLER / SERVICE     │                    │                 │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐      │
│  │PilotClientHan│  │ImportFlightPlan│  │ TestFlightPlan   │      │
│  │dler (TCP)    │  │ Controller     │  │ Controller       │      │
│  └──────┬───────┘  └───────┬────────┘  └────────┬─────────┘      │
│         │                  │                     │               │
│         ▼                  ▼                     ▼               │
│  ┌──────────────────────────────────────────────────────┐        │
│  │              RemotePilotService (Facade)             │        │
│  └──────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────┘
          │
┌─────────┼──────────────────────────────────────────────────────┐
│  DOMAIN / INFRASTRUCTURE                                       │
│  ▼                                                             │
│  DslValidator → FlightPlanExporter → FlightPlanToScenarioConverter
│                              ↓                                 │
│                   SimulationRunner (interface)                 │
│                    ┌──────────┴──────────┐                     │
│                    ▼                      ▼                    │
│         SocketSimulationRunner    ProcessBuilderSimRunner      │
└─────────────┬──────────────────────────────────────────────────┘
              │
              │  TCP (length-prefixed binary)
              ▼
┌──────────────────────────────────────────────────────────────┐
│  C++ Simulator (scomp/Sprint3)                               │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐   │
│  │sim_server│  │main.c     │  │us109_repor│  │physics.c  │   │
│  │.c (TCP)  │  │(server)   │  │t.c        │  │           │   │
│  └──────────┘  └───────────┘  └───────────┘  └───────────┘   │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐                  │
│  │json_parse│  │us106_threa│  │us107_sync.│                  │
│  │r.h       │  │ds.c       │  │c          │                  │
│  └──────────┘  └───────────┘  └───────────┘                  │
└──────────────────────────────────────────────────────────────┘
```

**Key Principles:**
- **TestFlightPlanController** does not know about transport — it uses the `SimulationRunner` interface
- The **data contract** between Java and C++ is JSON produced by `FlightPlanExporter`
- The **transport protocol** is length-prefixed binary (big-endian)
- **Mode selection** is done via configuration (system properties), not code

## 7. Communication Contracts Summary

| Step | From → To | Data | Protocol |
|------|-----------|------|----------|
| 1. DSL Import | User → ImportFlightPlanController | DSL text | Local method call |
| 2. ANTLR Validate | ImportFlightPlanController → ANTLR | DSL text | Local method call |
| 3. Export JSON | TestFlightPlanController → FlightPlanExporter | FlightPlan → JSON | Local method call |
| 4. Simulate | TestFlightPlanController → C++ Simulator | Binary JSON | TCP length-prefixed (or subprocess) |
| 5. Result | C++ Simulator → TestFlightPlanController | Report text | TCP length-prefixed (or file) |
| 6. Parse report | TestFlightPlanController → ReportParser | Text → ReportParseResult | Local method call |
| 7. Persist | TestFlightPlanController → Database | Updated FlightPlan | JPA |

---

## Runner Selection Logic

```java
private static SimulationRunner createRunner() {
    final var hostProp = System.getProperty("aisafe.simulator.host");
    final int timeout = Integer.getInteger("aisafe.simulator.timeout", 120);
    if (hostProp != null && !hostProp.isEmpty()) {
        final int port = Integer.getInteger("aisafe.simulator.port", 9999);
        return new SocketSimulationRunner(hostProp, port, timeout);
    }
    final String executable = System.getProperty("aisafe.simulator.executable", "aisafe-simulator");
    return new ProcessBuilderSimulationRunner(executable, timeout);
}
```

**Resolution order:**
1. If `-Daisafe.simulator.host=<ip>` is set → use TCP socket mode (Mode A)
2. Otherwise → use local `ProcessBuilder` with the configured executable (Mode B or C)

The WSL bridge (Mode C) is triggered when `aisafe-simulator.cmd` is found at the configured executable path and the underlying OS is Windows.
