# SCOMP Sprint 2 — Flight Simulation System

## 1. Context

This task was assigned in Sprint 2. This is the first time the SCOMP flight simulation component is being developed. It spans across all user stories: US100, US101, US102, US103, and US109.

### 1.1 List of Issues

- US100: Simulate flights in a given area — 15%
- US101: Capture and process flight movements — 15%
- US102: Detect aircraft safety violations in real time — 20%
- US103: Synchronize flight execution with a time step — 15%
- US109: Generate and store final simulation report — 10%
- Code Quality — 10%
- Validation — 15%

---

## 2. Requirements

**SCOMP Sprint 2** — Flight simulation system in C (POSIX)

*Acceptance Criteria:*

- **US100.1** — Each flight is simulated in a separate child process
- **US100.2** — Parent and child processes communicate via pipes
- **US100.3** — Signal handlers (SIGUSR1, SIGINT) are properly registered

- **US101.1** — Child processes send position updates via pipe at each time step
- **US101.2** — Parent process reads and stores position history
- **US101.3** — Non-blocking pipe reads prevent deadlock

- **US102.1** — Real-time violation detection using pairwise distance checking
- **US102.2** — Minimum separation: 600m horizontal AND vertical
- **US102.3** — SIGUSR1 signals sent to affected aircraft
- **US102.4** — All violations logged with full context

- **US103.1** — Simulation progresses deterministically step-by-step
- **US103.2** — Time barrier ensures all processes ready before advancing
- **US103.3** — All flights and parent synchronized via sleep()

- **US109.1** — Text report with flight summary and violations
- **US109.2** — CSV export of violation details
- **US109.3** — Report includes timestamps, positions, velocity vectors
- **US109.4** — Final validation result (PASSED/FAILED) clearly indicated

*Dependencies/References:*

- Professor's notes on process communication and time synchronization
- Flight physics model from Project Requirements V2a
- POSIX signal handling semantics
- Project structure: `scomp/` folder at repo root

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support code structure and documentation. Below is the main approach used and decisions the team made independently.

---

**Approach:**

- Process architecture: one child per flight, parent orchestrates simulation
- Communication: pipes for position data (low-overhead), signals for events (asynchronous)


**Team decisions:**

- Chose simple linear interpolation over numerical integration (sufficient for requirements)
- Minimal comments in code (professor style) with detailed README documentation
- Single main loop in US103 orchestrating all other components
- Pipe reads non-blocking to prevent deadlock even if buffer fills

### 3.1 What is the Flight Simulation System?

A multi-process simulation where:

- **Parent process** (Simulator) manages N child processes, reads positions, detects violations, generates reports
- **Child processes** (Flights) calculate positions, send updates via pipes, handle signals
- **Pipes** carry position data (FlightMovement struct) from child to parent
- **Signals** notify aircraft of violations (SIGUSR1) or termination (SIGINT)
- **Time barrier** ensures all processes advance in lock-step via synchronized sleep()

### 3.2 Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Linear interpolation** | Simple, correct for short time steps, avoids complexity of numerical integration |
| **Non-blocking pipe reads** | Prevents deadlock if child buffer fills; parent continues processing |
| **Signal-based notifications** | Asynchronous, low overhead, real-time alerts (SIGUSR1 for violations) |
| **Deterministic time stepping** | All processes sleep(time_step) creating implicit synchronization barrier |
| **Single-threaded parent** | Simpler than multi-threaded; sequential violation detection acceptable for simulation |

---

## 4. Design

### 4.1 Architecture

**Process Model:**

```
┌─────────────────────────────────────┐
│      Parent Process (Simulator)      │
│  ┌────────────────────────────────┐  │
│  │ Main Loop (US103):             │  │
│  │ 1. US101: read_pipes()         │  │
│  │ 2. US102: detect_violations()  │  │
│  │ 3. sleep(time_step)            │  │
│  │ 4. advance_time()              │  │
│  └────────────────────────────────┘  │
└─────────────────────────────────────┘
           (pipes + signals)
┌──────────────┬──────────────┬────────────┐
│   Child 1    │   Child 2    │  Child N   │
│  (Flight 1)  │  (Flight 2)  │ (Flight N) │
├──────────────┼──────────────┼────────────┤
│ calc_pos()   │ calc_pos()   │ calc_pos() │
│ write_pipe() │ write_pipe() │ write_pipe()│
│ sleep()      │ sleep()      │ sleep()    │
└──────────────┴──────────────┴────────────┘
```

**Data Flow (per time step):**

```
US101: Capture Movements
  Parent: read(pipe[0]) ← Child writes FlightMovement
  
US102: Detect Violations
  For each pair (i, j):
    distance = sqrt((xi-xj)² + (yi-yj)²)
    if distance < 600m: record violation, send SIGUSR1
    
US103: Synchronization Barrier
  Parent: sleep(time_step)
  All children: sleep(time_step)
  → All wake together, next iteration starts
```

### 4.2 Components

| File | Responsibility | Lines |
|------|---|---|
| `shared.h` | Data structures, constants | ~60 |
| `US100_flight.c` | Process creation, pipes, signals | ~120 |
| `US101_movements.c` | Position calculation, pipe I/O | ~100 |
| `US102_violations.c` | Violation detection, SIGUSR1 | ~130 |
| `US103_sync.c` | Main simulation loop, timing | ~100 |
| `US109_report.c` | Report generation (text + CSV) | ~120 |
| `main.c` | Entry point, sample data | ~80 |
| **Total** | | **~710 lines** |

### 4.3 Sequence Diagram Example: US102 (Safety Violation Detection)

```
Participant Parent, Flight_i, Flight_j

Parent -> Parent: elapsed += time_step
loop for each pair (i, j)
  Parent -> Parent: h_dist = distance_h(pos_i, pos_j)
  Parent -> Parent: v_dist = distance_v(pos_i, pos_j)
  alt h_dist < 600 OR v_dist < 600
    Parent -> Parent: record_violation(i, j, h_dist, v_dist)
    Parent -> Flight_i: kill(SIGUSR1)
    Parent -> Flight_j: kill(SIGUSR1)
    Flight_i <- Flight_i: handle_violation_signal()
    Flight_j <- Flight_j: handle_violation_signal()
  end
end
```

### 4.4 Invariants Enforced

| Component | Invariant | Enforced By |
|-----------|-----------|---|
| **FlightMovement** | Timestamp > 0; position valid | Child process before write() |
| **SafetyViolation** | distance_h, distance_v >= 0 | Parent (distance calculation) |
| **SimulationConfig** | num_flights ∈ [1, MAX]; time_step > 0 | main() validation |
| **Simulation Loop** | All children wrote before parent advances | sleep() barrier |
| **Report** | total_violations = count(violations array) | us109_generate_report() |

---

## 5. Implementation

### 5.1 File Structure

```
scomp/
├── shared.h                    # Data structures + constants
├── US100_flight.c             # Process creation + signals
├── US101_movements.c          # Position tracking via pipes
├── US102_violations.c         # Safety violation detection
├── US103_sync.c               # Main loop + synchronization
├── US109_report.c             # Report generation
├── main.c                     # Entry point + sample data
└── README.md                  # This file
```

### 5.2 Compilation

```bash
gcc -Wall -Wextra -std=c99 -lm *.c -o simulator
```

**Flags:**
- `-Wall -Wextra` — All warnings enabled
- `-std=c99` — C99 standard (POSIX C)
- `-lm` — Math library (for sqrt, fabs, etc)

### 5.3 Execution

```bash
./simulator
```

**Output:**
- Console logs of simulation progress
- `simulation_report.txt` — Human-readable summary
- `simulation_violations.csv` — Structured violation data (if violations > 0)

### 5.4 Sample Data

Three default flights created in `main.c`:

1. **TAP100**: OPO → LIS, altitude 1000m → 2000m
2. **TAP101**: OPO → LIS, altitude 3000m → 4000m (same route, different altitude)
3. **TAP200**: LIS → OPO (reverse direction)

Flights depart 10 seconds apart, each takes 900 seconds.

**Expected result:** 0 violations (no intersections)

---

## 6. Integration / Demonstration

### 6.1 Testing

**Test 1 — Compile without warnings:**
```bash
gcc -Wall -Wextra -std=c99 -lm *.c -o simulator
# Expected: 0 warnings, 0 errors
```

**Test 2 — Run with default configuration:**
```bash
./simulator
# Expected: 3 flights complete in ~60 seconds, 0 violations
```

**Test 3 — Verify output files:**
```bash
cat simulation_report.txt
# Expected: PASSED status, 0 violations
```

### 6.2 Demonstration Checklist

- [ ] Code compiles with `gcc -Wall -Wextra`
- [ ] Simulator runs without crashes
- [ ] `simulation_report.txt` is generated correctly
- [ ] Process creation works (check `ps` during run)
- [ ] Pipe communication works (positions are tracked)
- [ ] Time synchronization works (simulation takes ~total_time)
- [ ] Signal handling works (SIGUSR1 sent when applicable)
- [ ] Report format is correct (text + CSV)

---

## 7. Observations

### 7.1 Code Quality

- **No over-documentation** — Comments explain logic only, not obvious code
- **Clear function names** — `calc_position()`, `detect_violations()`, etc
- **Error handling** — All system calls checked; `perror()` for failures
- **Modular design** — Each US in separate file; clear responsibilities

### 7.2 Architecture Patterns

- **Information Expert** — Each component knows only what it needs (position calculations in US101, violation detection in US102)
- **Separation of Concerns** — Parent manages simulation, children execute flights
- **POSIX Compliance** — Uses standard pipes, signals, fork/wait
- **Non-blocking I/O** — Prevents deadlock in pipe communication

### 7.3 Limitations (Intentional)

- **Linear trajectories** — Sufficient for Sprint 2; enhanced physics in future sprints
- **O(N²) violation checking** — Acceptable for N ≤ 50 flights
- **Single-threaded parent** — Simplicity preferred; can be enhanced with threading (US106/107)
- **Fixed separation threshold** — 600m threshold; made configurable in shared.h

### 7.4 Future Enhancements

- **US105** — Shared memory + semaphores (better synchronization)
- **US106/107** — Multi-threaded parent (concurrent violation detection + reporting)
- **US110** — Environmental effects (wind, weather)
- **US113** — UDP logging server (remote monitoring)
- **US114** — Web-based visualization

---

## 8. References

### Physics & Domain
- Classical Mechanics — Lift, drag, thrust calculations
- Project Requirements V2a — Flight specifications, safety rules