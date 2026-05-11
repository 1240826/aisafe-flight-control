# US101 – Capture and Process Flight Movements

## 1. Context

As a simulation process, I want to receive movement commands from flight processes so that I can track aircraft positions over time.

## 2. Requirements

- Each flight process must send position updates to the main process via a pipe.
- The main process must track aircraft positions.
- **The system must store past positions to anticipate and detect potential safety violations.**

## 3. Analysis

### Flight phases — climb, cruise, descend

The professor's JSON has exactly three segments (professor simplification confirmed). The `advance()` function in `physics.h` handles each phase:

| Phase | Speed | Vertical rate `vz` |
|-------|-------|-------------------|
| CLIMB | Interpolated from climb profile at current altitude | Positive (from profile table) |
| CRUISE | Fixed `cruise_kt` | Zero |
| DESCEND | Interpolated from descend profile at current altitude | Negative (from profile table) |

The velocity vector is 3-dimensional:
- `vx` — East (m/s)
- `vy` — North (m/s)
- `vz` — Vertical (m/s), positive = climbing

Position integration per 1-second step:
```
Δlat = (vy × 1) / R_earth
Δlon = (vx × 1) / (R_earth × cos(lat))
Δalt = vz × 1
```

### Timestep = 1 second

Professor note: *"se o time step for muito grande não apanha as interseções"*.

At 460 kt cruise (237 m/s), two converging aircraft close at ~474 m/s. The horizontal safety limit is 9 260 m (5 NM). With `dt = 30s`: each step covers 7 100 m — aircraft can enter and leave the cylinder in a single step without detection. With `dt = 1s`: each step covers 237 m — the violation builds up gradually over ~39 consecutive steps.

### Position history (US101 acceptance criterion)

`FlightState` stores up to `MAX_HISTORY = 600` position snapshots (10 minutes at 1s/step):

```c
Snapshot history[MAX_HISTORY];
int      hist_count;
```

`store_history()` (in `us102_detector.c`) is called from `collect()` after every step. The buffer is circular — oldest entry is overwritten when full. This history is used by the predictive collision detection in US102.

### Route adjustment via GoToken

When US102 detects a violation, it orders an altitude change. The `GoToken` struct carries this order:

```c
typedef struct {
    int    step;
    int    safe;          /* 1=proceed, 0=hold */
    double alt_adjust;    /* metres to add to altitude */
} GoToken;
```

The child applies `alt_adjust` before computing the next position step.

## 4. Design

```
Child (us101_flight.c)                Parent (us103_sync.c)
  run_flight()                          collect()
    read(gfd, &tok)  ← BLOCKS            read(rfd, &upd)  ← BLOCKS
    pos.alt += tok.alt_adjust            store_history()
    if tok.safe: advance()               update last_pos/vel/in_area
    write(rfd, &upd) ─────────────────►
```

## 5. Implementation

**File:** `us101_flight.c` — `run_flight()`  
**Physics:** `physics.h` — `advance()`, `interp_spd()`, `interp_rate()`  
**History storage:** `us102_detector.c` — `store_history()`

## 6. Integration / Demonstration

```bash
./simulation ../test/scenario0_single.json
```

Output every 60 steps shows altitude rising during CLIMB, constant during CRUISE, falling during DESCEND.

## 7. Observations

- `write()` in signal handlers, not `printf()` — required by POSIX async-signal-safety.
- `SA_RESTART` ensures SIGUSR1 arriving during `read(gfd,...)` does not cause EINTR — the read restarts automatically.
- `volatile sig_atomic_t` prevents compiler caching of signal flags in registers.
- `PRINT_EVERY = 60`: position printed every 60 steps (1 simulated minute). Violations always printed immediately.
