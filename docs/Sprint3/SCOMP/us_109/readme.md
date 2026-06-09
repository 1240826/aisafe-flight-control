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
- **Shared memory segment (US105):** contains the `FlightData` array (one entry per
  flight process) and the `Violation` array. The report thread reads these once the
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
│  │ - FlightData[]       │ read   │                │ │
│  │ - Violation[]        │        └───────┬────────┘ │
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
============================================
  AISafe Simulation Report
  Generated: Wed May 27 12:34:56 2026
  Total steps: 3732  (3732 seconds simulated)
  Flights: 4
  Total violations detected: 0
============================================

FLIGHT SUMMARY:
  ARRIV1: n_viol=0  ever_in_area=yes  completed=yes
  ARRIV2: n_viol=0  ever_in_area=yes  completed=yes
  ...

VIOLATION LOG:
  #1 step=128  HORIZ1 <-> HORIZ2  h_dist=2226m  v_dist=0m
    pos_a=(41.0000, -8.5000, 9200)  pos_b=(41.0200, -8.5000, 9200)

============================================
  RESULT: PASS
============================================
```

---

### 3.3 Key Structures and Functions (C)

```c
/* Flight status tracked via FlightData fields in shared memory */
typedef struct {
    char id[ID_LEN];
    pid_t pid;
    Pos3D pos;          /* current position (lat, lon, alt) */
    Vel3D vel;          /* current velocity (vx, vy, vz) */
    Phase phase;
    int active;         /* 1 if flight has departed and not completed */
    int in_area;        /* 1 if inside monitored airspace */
    int ever_in_area;
    int n_viol;         /* number of violations this flight was involved in */
    int completed;      /* 1 if flight has reached final segment end */
    int cur_seg;
    double wind_speed_kt;
    double wind_dir_deg;
    Snapshot history[MAX_HISTORY];
    int hist_count;
} FlightData;

/* Violation event stored in shared memory */
typedef struct {
    int step;
    time_t ts;
    int fa, fb;         /* flight indices */
    Pos3D pa, pb;       /* positions of both aircraft */
    Vel3D va, vb;       /* velocities of both aircraft */
    double h_m, v_m;    /* horizontal / vertical separation */
} Violation;
```

Key functions:

| Function | File | Responsibility |
|----------|------|---------------|
| `report_generator_thread()` | `us107/us107_report_notify.c` | Main thread loop; `pthread_cond_timedwait` on `viol_cond`; logs `[REPORT]` in real time; calls `write_report()` at end |
| `write_report()` | `us109/us109_report.c` | Writes the structured report to a timestamped plain text file with PASS/FAIL verdict |
| `set_report_output_path()` | `us109/us109_report.c` | Configures a custom output path for the report file |

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
| `FlightData` | C struct | `files/common.h` | Holds per-flight execution status, position, velocity, wind in shared memory |
| `Violation` | C struct | `files/common.h` | Holds a single safety violation event with step, timestamps, positions and velocity vectors |
| `SharedData` | C struct | `files/common.h` | Top-level shared memory struct containing all flight data, violations, mutexes, condvars, semaphores |
| `report_generator_thread` | C function | `us107/us107_report_notify.c` | Report generation thread entry point; timedwait on viol_cond, real-time [REPORT] logging, calls write_report() at end |
| `write_report` | C function | `us109/us109_report.c` | Writes structured plain text report to timestamped file |

**Sequence Diagram — Report Generation Thread Lifecycle:**

![Sequence Diagram — Report Generation Thread](sd_us109_generate_store_final_report.svg)

### 4.2 Acceptance Tests

See section 3.4.

---

## 5. Implementation

**Key files:**

| File | Responsibility |
|------|---------------|
| `files/common.h` | `SharedData`, `FlightData`, `Violation` struct definitions (shared with US105–US110) |
| `us107/us107_report_notify.c` | `report_generator_thread()` — real-time violation logging and final report trigger |
| `us109/us109_report.c` | `write_report()` — writes structured plain text report to file |

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
- The `Violation` and `SharedData` structs are defined in `files/common.h` and shared with
  US105–US110. This single header avoids inconsistencies across all user stories.
- Mutex locking must be applied when reading shared memory during final aggregation to
  avoid race conditions with any remaining active threads.
