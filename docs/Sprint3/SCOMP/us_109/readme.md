# US109 — Generate and Store Final Simulation Report

## 1. Context

This task is assigned in Sprint 3 as part of the SCOMP-related work. It is the first time
this feature is being developed. The objective is to provide the Flight Control Operator
with a comprehensive report that details the simulation outcomes — including flight execution
statuses, safety violation events (with timestamps, positions and velocity vectors), and
overall validation results — so that they can assess the safety and performance of the flights
post-simulation.

**Assigned to:** (to be filled by the team)

### 1.1 List of Issues

- Analysis: #91
- Design: #91
- Implement: #91
- Test: #91

---

## 2. Requirements

**US109** As a Flight Control Operator, I want a comprehensive report that details the
simulation outcomes — including flight execution statuses, safety violation events (with
timestamps, positions and velocity vectors), and overall validation results —, so that I
can assess the safety and performance of the flights post-simulation.

### Acceptance Criteria

- **US109.1** The report generation thread aggregates data once the simulation concludes.
- **US109.2** The report includes the total number of flights, individual execution statuses,
  and detailed safety violation events.
- **US109.3** The final validation result (pass/fail) is clearly indicated.
- **US109.4** The complete report is saved to a file for future reference.

### Dependencies/References

- **US105** — Initialize hybrid simulation environment with shared memory (the shared memory
  segment that the report thread reads from).
- **US106** — Implement function-specific threads in the parent process (the report generation
  thread is one of the dedicated threads created here).
- **US107** — Notify report thread via condition variables upon safety violation detection
  (the mechanism by which violation events reach the report thread in real time).
- **US108** — Enforce step-by-step simulation synchronization (semaphores that control
  simulation progression the report thread must respect).
- **US111** — Generate a simulation report (EAPLI-side trigger and presentation of the
  report produced by this US).
- **US030** — Authentication and authorization infrastructure.
- **NFR09** — Authentication and authorization must be enforced.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user
story. Below are the main prompts used, the suggestions adopted, and the decisions the team
made independently or where we deviated from the AI output.

---

#### Prompt 1 — Report generation thread design

> "How should a report generation thread in C be designed to aggregate simulation data from
> shared memory once the simulation concludes, while also receiving real-time safety violation
> notifications via condition variables?"

**LLM suggestions adopted:**
- The report generation thread waits on a condition variable, signalled by the safety
  violation detection thread (US107) whenever a violation is detected — allowing real-time
  logging of violations during the simulation
- Once the simulation concludes (all flight processes have terminated), the thread performs
  a final aggregation pass over the shared memory to collect flight execution statuses and
  produce the overall pass/fail result
- The report is written to a plain text file with a structured format for easy reading

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested writing to JSON — replaced with plain text for simplicity and alignment
  with the EAPLI layer (US111) which reads and presents the report
- The LLM proposed a separate aggregation thread — kept as a single report generation
  thread that handles both real-time violation logging and final aggregation, as specified
  in US106

---

#### Prompt 2 — Shared memory data structures for report aggregation

> "What shared memory data structures are needed to support report generation in a
> multi-process C simulation, including flight execution statuses and safety violation
> events with timestamps, positions and velocity vectors?"

**LLM suggestions adopted:**
- `FlightRecord` struct in shared memory: flight id, execution status (RUNNING, COMPLETED,
  ABORTED, VIOLATION), current position, current velocity vector
- `ViolationEvent` struct: timestamp, positions and velocity vectors of both involved
  aircraft, involved flight ids
- A fixed-size array of `ViolationEvent` in shared memory, written by the violation
  detection thread and read by the report thread

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested dynamic memory allocation for violation events — replaced with a
  fixed-size array in shared memory to avoid complexity in inter-process communication
- Maximum number of violation events is a configurable constant defined at compile time

---

### 3.1 Thread Interaction Model

The report generation thread interacts with three other components during the simulation:

- **Safety violation detection thread (US107):** signals the report thread via a condition
  variable whenever a violation is written to shared memory. The report thread wakes up,
  reads the new violation event, and appends it to its internal violation log.
- **Shared memory segment (US105):** contains the `FlightRecord` array (one entry per
  flight process) and the `ViolationEvent` array. The report thread reads these once the
  simulation concludes for final aggregation.
- **Simulation end signal:** when all flight processes have terminated, the parent process
  signals the report thread to perform the final aggregation and write the report file.

```
┌─────────────────────────────────────────────────────┐
│                   Parent Process                    │
│                                                     │
│  ┌──────────────────────┐   condition variable      │
│  │ Safety Violation     │ ───────────────────────►  │
│  │ Detection Thread     │                           │
│  │ (US107)              │        ┌────────────────┐ │
│  └──────────────────────┘        │ Report         │ │
│                                  │ Generation     │ │
│  ┌──────────────────────┐        │ Thread         │ │
│  │ Shared Memory        │ ──────►│ (US109)        │ │
│  │ - FlightRecord[]     │ read   │                │ │
│  │ - ViolationEvent[]   │        └───────┬────────┘ │
│  └──────────────────────┘                │          │
│                                          │ write    │
└──────────────────────────────────────────┼──────────┘
                                           ▼
                                     report file
```

---

### 3.2 Report File Format

The report is saved as a plain text file with the following structure:

```
=== AISafe Simulation Report ===
Generated: 2026-06-10T15:00:00

Total Flights: 5

--- Flight Execution Statuses ---
Flight TP101: COMPLETED
Flight TP102: COMPLETED
Flight TP103: ABORTED
Flight TP104: VIOLATION
Flight TP105: COMPLETED

--- Safety Violations ---
[2026-06-10T14:45:12] VIOLATION DETECTED
  Flight A: TP103 | Position: (x=1200.5, y=3400.2, z=10000.0) | Velocity: (vx=250.0, vy=0.0, vz=0.0)
  Flight B: TP104 | Position: (x=1205.1, y=3398.7, z=10000.0) | Velocity: (vx=-250.0, vy=0.0, vz=0.0)

--- Validation Result ---
FAIL — 1 safety violation(s) detected.
```

---

### 3.3 Key Structures and Functions (C)

```c
typedef enum {
    RUNNING, COMPLETED, ABORTED, VIOLATION
} FlightStatus;

typedef struct {
    int    flightId;
    FlightStatus status;
    double posX, posY, posZ;
    double velX, velY, velZ;
} FlightRecord;

typedef struct {
    time_t timestamp;
    int    flightIdA;
    double posXA, posYA, posZA;
    double velXA, velYA, velZA;
    int    flightIdB;
    double posXB, posYB, posZB;
    double velXB, velYB, velZB;
} ViolationEvent;
```

Key functions:

| Function | Responsibility |
|----------|---------------|
| `report_thread_func(void*)` | Main thread loop; waits on condition variable; handles violation notifications and final aggregation |
| `aggregate_final_report()` | Reads shared memory after simulation ends; collects flight statuses and violation count |
| `write_report_to_file()` | Writes the structured report to a plain text file |
| `append_violation_event()` | Called on each condition variable signal; appends violation to internal log |

---

### 3.4 Acceptance Tests

**AT1 — Report aggregated after simulation concludes (US109.1)**

Given a simulation that has concluded with all flight processes terminated,
When the report generation thread performs final aggregation,
Then the report contains the correct total number of flights and their individual
execution statuses as recorded in shared memory.

**AT2 — Safety violations included with full detail (US109.2)**

Given a simulation where 2 safety violations were detected and signalled via condition
variables during execution,
When the report is generated,
Then both violations are listed with their timestamps, positions and velocity vectors
for both involved aircraft.

**AT3 — Validation result clearly indicated (US109.3)**

Given a simulation where at least one safety violation occurred,
When the report is generated,
Then the final validation result is `FAIL` with the number of violations indicated.
Given a simulation with no violations,
Then the result is `PASS`.

**AT4 — Report saved to file (US109.4)**

Given a successfully generated report,
When the report generation thread calls `write_report_to_file()`,
Then a plain text file is created at the configured output path and contains the
complete report content.

---

## 4. Design

### 4.1 Realization

**Classes/structures involved:**

| Name | Type | Module | Responsibility |
|------|------|--------|---------------|
| `FlightRecord` | C struct | SCOMP simulation | Holds per-flight execution status, position and velocity in shared memory |
| `ViolationEvent` | C struct | SCOMP simulation | Holds a single safety violation event with timestamps, positions and velocity vectors |
| `report_thread_func` | C function | SCOMP simulation | Report generation thread entry point; waits on condition variable and performs final aggregation |
| `aggregate_final_report` | C function | SCOMP simulation | Reads shared memory after simulation ends and computes overall result |
| `write_report_to_file` | C function | SCOMP simulation | Writes structured plain text report to file |
| `GenerateSimulationReportController` | Java class | `aisafe.core.application` | EAPLI controller (US111); reads the file produced by this US and presents it to the FCO |

**Sequence Diagram — Report Generation Thread Lifecycle:**

![Sequence Diagram — Report Generation Thread](sd_us109_generate_store_final_report.svg)

### 4.2 Acceptance Tests

See section 3.4.

---

## 5. Implementation

**Key new files:**

- `report_thread.c` / `report_thread.h` — report generation thread implementation
- `simulation_structs.h` — `FlightRecord` and `ViolationEvent` struct definitions (shared
  with US105, US106, US107)

*Major commits: (to be filled after implementation)*

---

## 6. Integration/Demonstration

1. Start the simulation (US105) with at least 3 flight processes.
2. Let the simulation run — safety violations (if any) are signalled to the report thread
   in real time via condition variables (US107).
3. Once all flight processes terminate, the report thread performs final aggregation.
4. Verify the report file is created at the configured output path.
5. Open the file and verify: total flights, individual statuses, violation events with full
   detail, and overall PASS/FAIL result.
6. Trigger the EAPLI report view (US111) and confirm the same data is presented in the UI.

---

## 7. Observations

- This US is the SCOMP-side implementation of the report; US111 is the EAPLI-side trigger
  and presentation layer. The integration point is the plain text report file written by
  this US and read by the US111 controller.
- The `FlightRecord` and `ViolationEvent` structs defined here are shared with US105
  (shared memory initialization), US106 (thread creation), and US107 (condition variable
  signalling) — all struct definitions must be kept in a single shared header file
  (`simulation_structs.h`) to avoid inconsistencies.
- Mutex locking must be applied when reading shared memory during final aggregation to
  avoid race conditions with any remaining active threads.
