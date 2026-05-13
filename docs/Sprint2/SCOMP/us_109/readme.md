# US109 – Generate and Store Final Simulation Report

## 1. Context

As a Flight Control Operator, I want a comprehensive report that details the simulation outcomes — including flight execution statuses, safety violation events (with timestamps, positions and velocity vectors), and overall validation results.

## 2. Requirements

- The report generation **process** aggregates data once the simulation concludes.
  *(Professor Luís Nogueira clarification: "In Sprint 2, 'thread' should be read as 'process'. Threads will be used in Sprint 3.")*
- Includes total flights, individual statuses, and detailed safety events.
- Final validation result (pass/fail) clearly indicated.
- Complete report saved to a file.

## 3. Analysis

`write_report()` is called by the parent process after all children have been reaped with `waitpid()`. No concurrent access — no synchronisation needed.

### Report sections

1. **Simulation area** — lat/lon/altitude bounds with `AREA_MAX_ALT_M`.
2. **Simulation parameters** — timestep, steps run, safety cylinder dimensions with ICAO reference.
3. **Flights table** — ID, PID, status (`COMPLETED`/`TERMINATED`/`RUNNING`), violations, route (phases).
4. **Flight details** — fuel, cruise speed, last position, history snapshot count.
5. **Safety events** — per event: step, timestamp, both positions and velocity vectors, measured separation, resolution applied.
6. **Overall result** — PASS / FAIL, with count of resolved vs unresolved events.

### Flight status logic

```c
if      (flights[i].active)   status = "RUNNING";      /* still active */
else if (flights[i].killed)   status = "TERMINATED";   /* received SIGTERM */
else                          status = "COMPLETED";     /* exited normally */
```

`killed` is set by `terminate_all()` in `us102_detector.c` when SIGTERM is sent.

### Resolution tracking

Each `Violation` struct records:
```c
int    resolved;       /* 1 = altitude adjustment was applied */
double adj_m;          /* metres of adjustment                */
int    adjusted_flt;   /* which flight received the order     */
```

The report shows whether each violation was resolved by the controller or remained unresolved (threshold exceeded).

## 4. Implementation

**File:** `us109_report.c` — `write_report()`  
Output file: `simulation_report.txt` (current directory)

## 5. Integration / Demonstration

```bash
# Normal flow — should produce PASS
./simulation ../test/scenario2_normal.json && cat simulation_report.txt

# Violations — should produce FAIL with resolution details
./simulation ../test/scenario3_violations.json && cat simulation_report.txt
```

## 6. Observations

- The report now receives `FlightPlan *plans` in addition to `FlightState *flights` so it can print the segment phases (CLIMB → CRUISE → DESCEND) for each flight.
- Position history counts confirm US101 AC ("store past positions") is fulfilled.
- Velocity vectors in each violation event allow post-simulation analysis of collision geometry and approach angles.
- Sprint 3 will replace this process-based implementation with a dedicated thread.
