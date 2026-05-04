# US101 – Capture and Process Flight Movements

## 1. Context

As a simulation process, I want to receive movement commands from flight processes so that I can track aircraft positions over time.

## 2. Requirements

- Each flight process must send position updates to the main process via a pipe.
- The main process must track aircraft positions.
- **The system must store past positions to anticipate and detect potential safety violations.**

## 3. Analysis

### Physics model — climb, cruise, descend

The professor's JSON contains three segments: climb, cruise, descend. Each has a different speed and vertical rate. The `advance()` function in `physics.h` handles all three:

| Phase | Speed | Vertical rate |
|-------|-------|---------------|
| CLIMB | Interpolated from climb profile table at current altitude | Positive (from table) |
| CRUISE | Fixed `cruise_kt` | Zero |
| DESCEND | Interpolated from descend profile table at current altitude | Negative (from table) |

The velocity vector has three components:
- `vx` — East (m/s)
- `vy` — North (m/s)
- `vz` — Vertical (m/s), positive = climbing

Position integration per step (1 second):
```
Δlat = (vy × 1) / R_earth
Δlon = (vx × 1) / (R_earth × cos(lat))
Δalt = vz × 1
```

### Position history (US101 AC)

The `FlightState` struct stores up to `MAX_HISTORY = 300` snapshots:
```c
Snapshot history[MAX_HISTORY];
int      hist_count;
```
`store_history()` in `us102_detector.c` is called after every `collect()`. The buffer is circular — when full, the oldest entry is overwritten. This history allows the violation detector to track trajectory trends.

### Why timestep = 1 second?

At 460 knots (237 m/s), two converging aircraft close at ~474 m/s. The horizontal safety cylinder is 14 816 m wide. With a 30s timestep, one step covers 7 100 m — aircraft can enter and exit the cylinder without detection. With 1s, each step is 237 m — the violation is detected step by step.

Professor note: *"se o time step for muito grande não apanha as interseções"*.

### Pipe transmission

The child writes `PosUpdate` after every physics step:
```c
typedef struct {
    int   idx;    /* flight index in parent's array */
    int   step;
    Pos3D pos;    /* lat, lon, alt */
    Vel3D vel;    /* vx, vy, vz (m/s) */
    Phase phase;  /* CLIMB / CRUISE / DESCEND */
    int   done;   /* 1 = all segments finished */
} PosUpdate;
```
`sizeof(PosUpdate)` ≪ `PIPE_BUF` (≥ 512 bytes) → write is atomic.

## 4. Design

```
Child (us101_flight.c)           Parent (us103_sync.c)
  run_flight()                     collect()
    read(gfd, &tok)   ← barrier      read(rfd, &upd)  ← blocks
    advance()                        store_history()
    write(rfd, &upd) ─────────────►  update last_pos/vel
```

## 5. Implementation

**File:** `us101_flight.c` — `run_flight()`  
**Physics:** `physics.h` — `advance()`, `interp_spd()`, `interp_rate()`  
**History:** `us102_detector.c` — `store_history()`

## 6. Integration / Demonstration

```bash
./simulation ../test/scenario0_single.json 5400 1
```

Observe the altitude rising during CLIMB steps, then constant during CRUISE, then falling during DESCEND. Output every 60 steps (1 simulated minute).

## 7. Observations

- `write()` is used in signal handlers instead of `printf()` — required by POSIX async-signal-safety rules (Signals Part II slides).
- `SA_RESTART` ensures SIGUSR1 arriving during `read(gfd,...)` does not cause EINTR — the read restarts automatically.
- The position history enables future extension for predictive collision detection (trajectory extrapolation).
