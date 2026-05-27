# AISafe – SCOMP Sprint 2: Flight Simulation System

---

## Component Diagram

```
plans.json ──► load_plans() ──► init_simulation()
                                       │
                   ┌───────────────────┼───────────────────┐
                   │  ALL pipes created BEFORE any fork()  │
                   │  pipe(report_pipe[0])                 │
                   │  pipe(go_pipe[0])                     │
                   │  pipe(report_pipe[1])                 │
                   │  pipe(go_pipe[1])                     │
                   │  ... one pair per flight              │
                   └───────────────────┬───────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
           fork()                   fork()                   fork()
              │                       │                         │
         child 0                  child 1                 child N-1
         (TP001)                  (IB002)                  (VY003)
       run_flight()             run_flight()             run_flight()


COMMUNICATION — 2 pipes per flight, both directions:

  go_pipe[i]       parent ─────────────────────► child i
                   GoToken { step, safe, alt_adjust }
                     safe=1  → advance position
                     safe=0  → hold position (violation nearby)
                     alt_adjust>0 → climb order (route change)

  report_pipe[i]   child i ─────────────────────► parent
                   PosUpdate { pos, vel, phase, done }


SIGNALS:

  SIGUSR1   parent ──► child    violation detected nearby
  SIGTERM   parent ──► child    threshold exceeded OR flight left area


PARENT MAIN LOOP:

  ┌─────────────────────────────────────────────────────────────┐
  │  for step = 0, 1, 2, ...                                    │
  │                                                             │
  │    1. send_go()     write GoToken to each active child      │
  │                     children unblock from read(gfd,...)     │
  │                                                             │
  │    2. collect()     read PosUpdate from each active child   │
  │                     each read() BLOCKS until child writes   │
  │                     loop exits only when ALL have reported  │
  │                     ─────── BARRIER GUARANTEE ──────        │
  │                                                             │
  │    3. detect_violations()                                   │
  │                     check all pairs: H < 9260m, V < 305m    │
  │                     predict 30s ahead using velocity        │
  │                     send SIGUSR1 to both children           │
  │                     issue alt_adjust via GoToken            │
  │                                                             │
  │    4. if n_viol >= VIOL_THRESHOLD:                          │
  │            terminate_all() → SIGTERM to all active          │
  │                                                             │
  │    5. if active == 0: stop                                  │
  └─────────────────────────────────────────────────────────────┘

  waitpid() all children
  write_report() ──► simulation_report.txt


CHILD LOOP — run_flight():

  ┌─────────────────────────────────────────────────────────────┐
  │  while (!do_terminate):                                     │
  │                                                             │
  │    read(gfd, &tok)        BLOCKS — US103 barrier point      │
  │                                                             │
  │    if tok.alt_adjust != 0:                                  │
  │        pos.alt += tok.alt_adjust    apply altitude order    │
  │                                                             │
  │    if !tok.safe:                                            │
  │        write(rfd, &upd)             hold, report, continue  │
  │                                                             │
  │    advance()              compute next position             │
  │    write(rfd, &upd)       send PosUpdate to parent          │
  └─────────────────────────────────────────────────────────────┘
```

---

## Files

```
simulation/
  common.h           shared constants, structs (GoToken, PosUpdate, FlightState, ...)
  json_parser.h      minimal JSON reader, no external dependencies
  physics.h          function prototypes
  physics.c          h_dist, in_area, safety_breach, advance, interp_spd, ...
  main.c             main loop, argument parsing, waitpid, report call
  us100_init.c       US100: init_simulation() — pipes + forks
  us101_flight.c     US101: run_flight() — physics loop inside each child
  us102_detector.c   US102: detect_violations(), terminate_all(), store_history()
  us103_sync.c       US103: send_go(), collect()
  us109_report.c     US109: write_report()
  Makefile

test/
  scenario0_single.json         1 flight  — original professor JSON
  scenario1_area_cases.json     3 flights — enters area, leaves area, never enters
  scenario2_normal.json         3 flights — normal flow, PASS report
  scenario3_violations.json     2 flights — overtaking, prediction, resolution
  scenario4_large_normal.json   8 flights — large fleet, no violations
  scenario5_termination.json   10 flights — 5 pairs violate → SIGTERM all
```

---

## Build and run

```bash
make

make scenario3            # violations + resolution
make scenario4            # 8 aircraft, no violations
make scenario5            # SIGTERM termination
make debug                # print every step

# P = print_every (default 60s simulated)
make scenario4 P=300      # print every 5 min
make scenario4 P=1        # print every step

# directly:
./simulation <plans.json> [timestep_s] [print_every]
```

---

## Simulation Area and Parameters

The simulation covers the **Iberian Peninsula air control region**:

```
Latitude:   [38°N, 44°N]
Longitude:  [10°W,  2°W]
Altitude:   [0, 14 000 m]
```

Safety separation (ICAO Doc 4444, Chapter 8 — en-route radar):
```
Horizontal: 5 NM  = 9 260 m
Vertical:   1000 ft = 305 m   (RVSM standard)
```

These values are defined as constants in `common.h` so they can be changed without touching any other file.

---

## Design Decisions

**Timestep = 1 second.**
At 460 kt (237 m/s) a 30-second step covers 7 100 m per step. An aircraft can enter and exit the 9 260 m safety cylinder in a single step without being detected. With 1 s each step is 237 m, making detection reliable. The physics and violation detection always run every step — `print_every` only controls terminal output.

**Pipes before forks.**
A child inherits only the file descriptors that were open at the moment `fork()` was called. All N pipe pairs are created before any fork so every child inherits the full set. After forking, each child closes every end it does not own — if it kept an open write end of another flight's `report_pipe`, the parent's `read()` on that pipe would never return EOF and the simulation would deadlock.

**Blocking pipe read as barrier.**
The barrier in `collect()` uses a plain `read()` per child. This blocks until that child writes. No semaphores, no shared memory, no busy-wait. The guarantee: all step-t positions are in before `detect_violations()` runs. Without this, a fast child could be 50 steps ahead of a slow one — comparing their positions would be physically meaningless.

**GoToken carries two controller decisions.**
`safe=0` tells the child to hold position without advancing — the controller prevents movement when a violation is near. `alt_adjust` carries a climb order: when a violation is detected, the lower aircraft is ordered to climb 400 m. Since 400 m > 305 m (the vertical separation minimum), separation is restored in a single step. The order travels through the existing barrier pipe.

**SA_RESTART on SIGUSR1.**
The child is blocked in `read(gfd, &tok)` when a SIGUSR1 arrives. Without `SA_RESTART`, the read returns -1 with `EINTR` and the barrier breaks. With it, the read restarts transparently.

**Area exit terminates the child.**
When a flight's position moves outside the area, the controller sends SIGTERM and stops tracking it. The controller is only responsible for flights within its airspace — continuing to simulate flights outside serves no purpose.

**Safety cap.**
`SAFETY_CAP_S = 10 800 s` (3 hours) limits the maximum iteration count. It exists only to prevent infinite loops if a JSON segment is unreachable. Normal termination is always `active == 0`.

---

## US Descriptions

### US100 – Simulation initialisation (`us100_init.c`)

Creates all pipe pairs first, then forks one child per flight plan:

```c
/* Phase 1: all pipes */
for (i = 0; i < n; i++) {
    pipe(report_pipe[i]);
    pipe(go_pipe[i]);
}
 
/* Phase 2: forks */
for (i = 0; i < n; i++) {
    pid = fork();
    if (pid == 0) {
        for (j = 0; j < n; j++) {
            if (j == i) {
                close(report_pipe[i][0]);   /* parent reads here */
                close(go_pipe[i][1]);       /* parent writes here */
            } else {
                close(report_pipe[j][0]); close(report_pipe[j][1]);
                close(go_pipe[j][0]);     close(go_pipe[j][1]);
            }
        }
        run_flight(&plans[i], i, report_pipe[i][1], go_pipe[i][0], timestep);
    }
    /* parent closes child-side ends */
    close(report_pipe[i][1]);
    close(go_pipe[i][0]);
}
```

Each child keeps only its two ends (`report_pipe[i][1]` to write to parent, `go_pipe[i][0]` to read from parent). `fflush(stdout)` before `fork()` prevents buffered output from being duplicated in both processes.

### US101 – Flight physics and pipe transmission (`us101_flight.c`, `physics.c`)

Each child blocks on a GoToken, applies any controller orders, advances one physics step, and sends a PosUpdate:

```c
while (!do_terminate) {
    n = read(gfd, &tok, sizeof(GoToken));   /* blocks — barrier point */
    if (n <= 0) break;
 
    if (tok.alt_adjust != 0.0)
        pos.alt += tok.alt_adjust;          /* altitude order from US102 */
 
    if (!tok.safe) {
        write(rfd, &upd, sizeof(PosUpdate)); /* hold position */
        continue;
    }
 
    done = advance(plan, &plan->seg[cur], &pos, &vel, (double)timestep);
    write(rfd, &upd, sizeof(PosUpdate));    /* send to parent */
}
```

Signal handlers use only `write()` (async-signal-safe). `sigfillset` blocks all signals during the SIGUSR1 handler as required. `SA_RESTART` prevents `EINTR` on the blocking `read()`:

```c
static volatile sig_atomic_t got_violation = 0;
static volatile sig_atomic_t do_terminate  = 0;
 
static void handle_usr1(int sig) {
    write(STDOUT_FILENO, msg, sizeof(msg) - 1);
    got_violation = 1;
}
 
sigfillset(&act.sa_mask);   /* block all signals during handler */
act.sa_flags = SA_RESTART;  /* read() restarts on EINTR         */
```

Physics (`physics.c`): CLIMB and DESCEND interpolate speed and vertical rate from the profile table at the current altitude. CRUISE uses constant speed with `vz=0`:

```c
if (seg->phase == CRUISE) {
    spd_ms = KT_TO_MS(plan->cruise_kt);
    vz     = 0.0;
} else if (seg->phase == CLIMB) {
    spd_ms = KT_TO_MS(interp_spd(plan->climb, plan->n_climb, pos->alt, plan->cruise_kt));
    vz     = interp_rate(plan->climb, plan->n_climb, pos->alt, 8.0);
} else {
    spd_ms = KT_TO_MS(interp_spd(plan->desc, plan->n_desc, pos->alt, plan->cruise_kt));
    vz     = interp_rate(plan->desc, plan->n_desc, pos->alt, -8.0);
}
```

Position history: circular buffer, 600 snapshots per flight, oldest overwritten when full:

```c
int idx = f->hist_count % MAX_HISTORY;
f->history[idx].step = step;
f->history[idx].pos  = f->last_pos;
f->history[idx].vel  = f->last_vel;
f->hist_count++;
```

### US102 – Violation detection (`us102_detector.c`)

Checks all active in-area pairs every step against the ICAO cylinder. Also extrapolates positions 30 seconds ahead using current velocity to detect future breaches before they happen:

```c
fa.lat = a->last_pos.lat + R2D(a->last_vel.vy * dt / EARTH_R);
fa.lon = a->last_pos.lon + R2D(a->last_vel.vx * dt
         / (EARTH_R * cos(D2R(a->last_pos.lat))));
fa.alt = a->last_pos.alt + a->last_vel.vz * dt;
/* same for fb, then: */
return safety_breach(fa, fb, h_out, v_out);
```

On any event (real or predicted): logs positions and velocities, sends SIGUSR1 to both children, orders the lower aircraft to climb via `GoToken.alt_adjust`, and sets `safe=0` for both so neither advances this step:

```c
int lower = (flights[i].last_pos.alt <= flights[j].last_pos.alt) ? i : j;
alt_adj[lower] = ALT_ADJUST_M;   /* 400 m > 305 m — separation restored */
safe[i] = 0;
safe[j] = 0;
kill(flights[i].pid, SIGUSR1);
kill(flights[j].pid, SIGUSR1);
```

When total violations reach `VIOL_THRESHOLD`, `terminate_all()` sends SIGTERM to every active child.

### US103 – Time-step barrier (`us103_sync.c`)

Phase 1 — `send_go()` writes one GoToken to each active child, unblocking their `read()`:

```c
tok.step = step;
tok.safe = safe[i];
tok.alt_adjust = alt_adj[i];
write(go_pipe[i][1], &tok, sizeof(GoToken));
```

Phase 2 — `collect()` reads one PosUpdate from each active child. Each `read()` blocks until that child writes. The loop only exits when all have reported — this is the barrier guarantee. `detect_violations()` runs after `collect()` returns:

```c
n = read(report_pipe[i][0], &upd, sizeof(PosUpdate)); /* blocks */
```

Area exit is detected here because this is where the parent reads each new position. When `was_in=1 → now_in=0`, SIGTERM is sent and the flight is deactivated immediately.

### US109 – Final report (`us109_report.c`)

Written by the parent after `waitpid()` collects all children. Includes per-flight status (COMPLETED / TERMINATED / RUNNING), per-violation details (step, timestamp, positions of both aircraft, velocity vectors, measured H and V separation, resolution applied), and overall PASS / FAIL. Saved to `simulation_report.txt`.
 
---

## Expected Output

The following shows the key output from each scenario, confirming correct behaviour.

### S0 — Single flight (climb → cruise → descend)

```
[CTRL]   Loaded 1 plan(s) from scenario0_single.json
[CTRL]   Forked 123456    PID=580  in_area=1
[CTRL]   step=    0  123456    (41.2637, -8.6845,   81m)  CLIMB   IN
[CTRL]   step=  600  123456    (41.8188, -8.1760, 6194m)  CLIMB   IN
[CTRL]   step= 1200  123456    (41.8437, -7.4301, 9249m)  CRUISE  IN
[CTRL]   step= 1800  123456    (41.4124, -5.8300, 9249m)  CRUISE  IN
[CTRL]   step= 2400  123456    (40.9775, -4.2676, 9009m)  DESCEND IN
[CTRL]   step= 3000  123456    (40.4902, -3.5654, 3288m)  DESCEND IN
[CTRL]   123456 completed at step 3377
[CTRL]   All flights done at step 3378 (3378s simulated)
```

All three phases visible. Flight completes naturally — no SIGTERM, no violations.

### S1 — Area entry, area exit, never enters

```
[CTRL]   IB202 starts OUTSIDE the area
[CTRL]   LH303 starts OUTSIDE the area
[AREA]   IB202 ENTERING area at step 2009
[AREA]   TP101 LEAVING area at step 2353
[FLIGHT] SIGTERM - shutdown              ← TP101 immediately terminated
[CTRL]   IB202 completed at step 6173
[CTRL]   LH303 completed at step 6386
[CTRL]   All flights done at step 6387 (6387s simulated)
```

Three distinct area cases in one run: entry mid-flight, exit mid-flight (with SIGTERM), never enters (LH303 flies entirely outside and still completes).

### S2 — Normal flow, PASS report

```
[AREA]   IB002 LEAVING area at step 1750
[CTRL]   TP001 completed at step 3410
[AREA]   VY003 LEAVING area at step 3731
[CTRL]   All flights done at step 3732 (3732s simulated)
[CTRL]   Report saved: simulation_report.txt
```

Zero violations. Report result: **PASS**.

### S3 — Predictive detection, altitude resolution

Two aircraft on converging paths at the same altitude. The detector flags a future breach 30 seconds before it would happen, issues a climb order, and both complete normally:

```
[PRED]   step=2338  TP201 <-> IB202  H_pred=9256m  V_pred=0m  (in 30s)
[CTRL]   TP201 ordered to climb 400m
[FLIGHT] SIGUSR1 - violation            ← both children notified
[FLIGHT] SIGUSR1 - violation
[FLIGHT TP201] step=2339  altitude +400m -> 9600m
[CTRL]   IB202 completed at step 2380
[CTRL]   TP201 completed at step 2413   ← TP201 at 9600m, IB202 at 9200m
[CTRL]   All flights done at step 2414 (2414s simulated)
```

The generated report confirms the event:

```
SAFETY EVENTS  (1 total)
 
  Event #1  step=2338  time=15:08:52
    TP201: pos=(41.5000, -3.7096, 9200m)  vel=(236.6, 0.0, 0.0) m/s
    IB202: pos=(41.5000, -3.5503, 9200m)  vel=(102.9, 0.0, 0.0) m/s
    Separation: H=9256m (limit 9260m)  V=0m (limit 305m)
    Resolution: TP201 ordered to climb 400m (separation restored)
 
  RESULT: FAIL — 1 safety event(s) detected
    Resolved by altitude adjustment: 1
    Unresolved (threshold exceeded): 0
```

### S5 — Threshold exceeded, SIGTERM all

Ten aircraft in five pairs, each pair in violation at step 0. Five violations are detected immediately, the threshold is reached, and all ten children are terminated:

```
[VIOL]   step=0  FA500 <-> SL500  H=6372m(lim 9260m)  V=0m(lim 305m)
[VIOL]   step=0  FA501 <-> SL501  H=6498m(lim 9260m)  V=0m(lim 305m)
[VIOL]   step=0  FA502 <-> SL502  H=6620m(lim 9260m)  V=0m(lim 305m)
[VIOL]   step=0  FA503 <-> SL503  H=6740m(lim 9260m)  V=0m(lim 305m)
[VIOL]   step=0  FA504 <-> SL504  H=6857m(lim 9260m)  V=0m(lim 305m)
[CTRL]   Threshold (5) reached — terminating
[FLIGHT] SIGTERM - shutdown              ← all 10 children receive SIGTERM
...
[CTRL]   Simulation ended: 1 step(s), 1s simulated.
```
 
---

## Test Scenarios

| File | Flights | Covers |
|---|---|---|
| `scenario0_single.json` | 1 | Original professor JSON — climb / cruise / descend |
| `scenario1_area_cases.json` | 3 | Enters area mid-flight; leaves area; never enters |
| `scenario2_normal.json` | 3 | Normal flow, exits and completes — PASS |
| `scenario3_violations.json` | 2 | Overtaking → `[PRED]` → SIGUSR1 → +400 m → both complete |
| `scenario4_large_normal.json` | **8** | Large fleet, separated corridors, zero violations |
| `scenario5_termination.json` | **10** | 5 pairs violate at step 0 → threshold → SIGTERM all |
 
---

## What was implemented

- Pipes before forks; correct descriptor cleanup per child (US100)
- Climb/cruise/descend physics with altitude-interpolated profile (US101)
- `volatile sig_atomic_t`; `write()` in handlers; `SA_RESTART` (US101)
- Circular history buffer, 600 snapshots per flight (US101)
- Runtime `timestep` and `print_every` via arguments and `make P=` (US101/US103)
- ICAO cylinder check every step (US102)
- Predictive detection 30 s ahead by velocity extrapolation (US102)
- SIGUSR1 + altitude route change via GoToken (US102)
- SIGTERM on threshold; `killed` flag tracked for report (US102)
- Two-phase blocking-pipe barrier with `safe` and `alt_adjust` (US103)
- Area exit detection with immediate SIGTERM and deactivation (US103)
- Natural termination when `active == 0` (US103)
- Full report: timestamps, positions, velocity vectors, PASS/FAIL (US109)
## Known limitations

- `alt_adjust` is permanent — aircraft stays at the adjusted altitude and does not return to original cruise altitude
- `VIOL_THRESHOLD = 5` to make the SIGTERM scenario reachable within `MAX_FLIGHTS = 10`
- Weather not simulated (out of scope)
---

## Self-assessment of contribution (Sprint 2)

| Member | US | Contribution |
|---|---|---|
| Jaime Simões | US100 – Simulation initialisation | 100% |
| Cláudio Pinto | US101 – Flight physics | 100% |
| Dinis Silva | US102 – Violation detection | 100% |
| Fábio Costa | US103 – Timestep barrier synchronisation | 100% |
| André Barcelos | US109 – Final report | 100% |
