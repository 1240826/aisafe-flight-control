# US100 – Simulate Flights in a Given Area

## 1. Context

As a Flight Control Operator, I want to simulate flights in a given area. Simulations have parameters such as time range, geographic area, included flights, safety thresholds, and performance settings. All required parameters should be validated.

## 2. Requirements

- Implemented in C using processes, pipes, and signals.
- Fork one child process per flight plan.
- Each child executes its designated flight plan.
- Pipes facilitate communication between the main process and each flight process.
- Main process tracks aircraft positions over time using an appropriate data structure.

## 3. Analysis

### Input

The JSON file provided by the professor describes one flight with one leg: segments for climb, cruise, and descent with speed/rate profiles per altitude. The SCOMP simulation reads this file directly — the LPROG DSL is processed separately and produces equivalent JSON input.

Professor simplification (Angelo Martins): *"Flights depart and land in airports of the same air control area. A flight has exactly one climb phase, one cruise phase, and one descent phase."*

### Simulation area

Rectangular bounding box with configurable altitude ceiling:

| Boundary | Value |
|---|---|
| Latitude | [38°N, 44°N] |
| Longitude | [10°W, 2°W] |
| Altitude | [0, `AREA_MAX_ALT_M`] |

`AREA_MAX_ALT_M = 14000.0 m` — defined in `common.h` as a `#define`, not hardcoded. Professor clarification: *"do not put 14000 m directly in the code, use configuration"*.

### Four area scenarios

| Scenario | Detected by |
|---|---|
| Starts inside, stays inside | `in_area=1` at init, no transitions |
| Starts inside, leaves | `LEAVING` message in `collect()` |
| Starts outside, enters | `ENTERING` message in `collect()` |
| Never in the area | `in_area=0` always, no violation checks |

### Pipe layout (created before any `fork()`)

```
report_pipe[i]   child i → parent   PosUpdate (every step)
go_pipe[i]       parent  → child i  GoToken   (every step)
```

All pipes are created **before** the first `fork()` so every child inherits all descriptors. After `fork()`, each child closes every pipe end it does not own.

## 4. Design

```
init_simulation()
  for i in 0..n:
    pipe(report_pipe[i])
    pipe(go_pipe[i])
  for i in 0..n:
    flights[i].in_area = in_area(plans[i].seg[0].start)
    fork()
    if child:  close others, run_flight()
    if parent: record pid/fds, close child ends
```

## 5. Implementation

**File:** `us100_init.c` — `init_simulation()`

**Compile:**
```bash
gcc -Wall -Wextra -D_GNU_SOURCE \
    main.c us100_init.c us101_flight.c us102_detector.c \
    us103_sync.c us109_report.c -o simulation -lm
```

## 6. Integration / Demonstration

```bash
# Area transitions: TP101 leaves, IB202 enters, LH303 never appears
./simulation ../test/scenario1_area_cases.json
```

Expected:
```
[CTRL]   IB202 starts OUTSIDE the area
[CTRL]   LH303 starts OUTSIDE the area
...
[AREA]   IB202 ENTERING area at step 2009
[AREA]   TP101 LEAVING area at step 2353
```

## 7. Observations

- `fflush(stdout)` before each `fork()` prevents buffered output from appearing in children.
- LH303 (Frankfurt→Rome) is entirely north and east of the area — it never triggers `ENTERING`.
- The simulation has no mandatory `max_steps`. It runs until all flights complete segments or leave the area. A safety cap (`SAFETY_CAP_S = 10800`) prevents infinite loops.
