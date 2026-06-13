# AISafe — SCOMP Sprint 3: Simulation (Threads + Processes + Shared Memory)

---

## Component Diagram

```
scenario.json ──► load_plans() ──► init_hybrid_simulation()
                                           │
                  SHARED MEMORY (shm_open + mmap, MAP_SHARED)
                  ┌──────────────────────────────────────────────────────┐
                  │  SharedData {                                        │
                  │    FlightData flights[MAX_FLIGHTS];                  │
                  │    FlightPlan plans[MAX_FLIGHTS];                    │
                  │    Violation violations[MAX_VIOLATIONS];             │
                  │    EnvironmentData env;                              │
                  │    sem_t *sem_step_start, *sem_step_done;            │
                  │    pthread_mutex_t pos_mutex, viol_mutex, ...;       │
                  │    pthread_cond_t viol_cond, detect_cond, ...;       │
                  │    volatile int running, current_step;               │
                  │    int n_flights, n_violations, ...;                 │
                  │  }                                                   │
                  └──────────────────────────────────────────────────────┘
                                           │
            ┌──────────────────────────────┼──────────────────────────────┐
            │         PARENT PROCESS        │                             │
            │  ┌────────────────────────┐   │                             │
            │  │ VIOLATION DETECTOR     │   │                             │
            │  │ (US106 — pthread)      │   │                             │
            │  │ waits on detect_cond   │   │                             │
            │  │ scans all pairs,       │   │                             │
            │  │ signals viol_cond      │   │                             │
            │  └───────────┬────────────┘   │                             │
            │              │ viol_cond       │                            │
            │              ▼                │                             │
            │  ┌────────────────────────┐   │                             │
            │  │ REPORT GENERATOR       │   │                             │
            │  │ (US107 — pthread)      │   │                             │
            │  │ timedwait on viol_cond │   │                             │
            │  │ prints [REPORT],       │   │                             │
            │  │ calls write_report()   │   │                             │
            │  └────────────────────────┘   │                             │
            │                               │                             │
            │  ┌────────────────────────┐   │                             │
            │  │ ENVIRONMENT THREAD     │   │                             │
            │  │ (US110 — pthread)      │   │                             │
            │  │ loads weather from JSON│   │                             │
            │  │ sets wind per flight   │   │                             │
            │  └────────────────────────┘   │                             │
            └──────────────────────────────┘                              │
                                           │
               ┌───────────────────────────┼───────────────────────────┐
            fork()                      fork()                      fork()
               │                          │                           │
          child 0                     child 1                    child N-1
          (TP001)                     (IB002)                    (VY003)
        run_flight_process()     run_flight_process()      run_flight_process()


STEP SYNCHRONIZATION (US108):

  PARENT                              CHILD i
    │                                    │
    │  sem_post(sem_step_start) ──────►  │
    │                                    │  sem_wait(sem_step_start)
    │                                    │  advance() one timestep
    │                                    │  apply wind drift
    │  ◄───────────────────────────── sem_post(sem_step_done)
    │  sem_wait(sem_step_done)           │
    │                                    │
    │  (barrier — all children done)     │
    │                                    │
    │  signal detect_cond                │
    │  signal env_cond                   │
    │  draw_airspace()                   │
    │                                    │
    │  (repeat for next step)            │
    ▼                                    ▼


ENVIRONMENT THREAD FLOW (US110):

  ┌──────────────────────────────────────────────────┐
  │  environment_thread():                           │
  │                                                  │
  │  1. Load weather from JSON file                  │
  │     (CW.json / HP.json / SYNTHETIC.json)         │
  │                                                  │
  │  2. Per simulation step:                         │
  │     for each flight:                             │
  │       pos = fd->pos (from shared memory)         │
  │       lookup zone by lat/lon/alt                 │
  │       fd->wind_speed_kt = zone.speed_kt          │
  │       fd->wind_dir_deg  = zone.dir_deg           │
  │                                                  │
  │  3. Update shm->env.wind for display             │
  │                                                  │
  │  Fallback (no file): sine-wave syntetic wind     │
  │    speed = 15 + 12*sin(t*2π/6) kt                │
  │    dir   = 180 + 40*sin(t*2π/4) deg              │
  └──────────────────────────────────────────────────┘


WIND ON AIRCRAFT (applied in flight process):

  After advance() applies the planned motion:
    wind_rad = D2R(fd->wind_dir_deg);
    wvx = -KT_TO_MS(fd->wind_speed_kt) * sin(wind_rad);
    wvy = -KT_TO_MS(fd->wind_speed_kt) * cos(wind_rad);

    pos.lon += R2D((wvx * dt) / (cos(lat_mid) * EARTH_R));
    pos.lat += R2D((wvy * dt) / EARTH_R);
    vel.vx += wvx;
    vel.vy += wvy;

  This models real wind drift: the air mass moves relative to
  the ground, carrying the aircraft with it. Wind does NOT
  affect airspeed or heading — only ground track and speed
  over ground. This is realistic for ATC simulation.
```

---

## Files

```
Sprint3/
  files/
    main.c                entry point, menu, simulation loop, scenario JSON parser
    common.h              shared structs (SharedData, FlightData, Violation, ...), constants
    json_parser.h         header-only JSON primitives (jp_skip, jp_close, jp_dbl, jp_str, jp_arr)
    physics.h / physics.c h_dist, in_area, in_area_full, safety_breach, advance, fast_forward_flight
    ui.h / ui.c           draw_airspace(), show_summary(), menu, scenario browser
    Makefile              build rules, targets (run, demo, heavy, ...)

  us105/
    us105_init.h / .c     init_hybrid_simulation() — shm_open, mmap, sem_open, mutex/cond init, pthread_create, fork

  us106/
    us106_threads.h / .c  violation_detector_thread(), detect_initial_violations()

  us107/
    us107_report_notify.h / .c  report_generator_thread() — timedwait on viol_cond, real-time [REPORT], write_report()

  us108/
    us108_sync.h / .c     run_flight_process(), simulation_step() — sem_post/sem_wait barrier

  us109/
    us109_report.h / .c   write_report() — timestamped file, PASS/FAIL verdict

  us110/
    us110_env.h / .c      environment_thread(), env_set_weather_file()
    us110_weather.h / .c  load_weather_from_json(), get_wind_at()

  weather/
    CW.json               Crazy Weather (45 zones, 0–12 000 ft)
    HP.json               Happy Weather (45 zones, 0–12 000 ft)
    SYNTHETIC.json        Full coverage (20 zones, 0–45 000 ft, lat 38–44, lon 10–2W)

  test/
    scenario_transatlantic_arrivals.json   4 flights  PASS
    scenario_mixed_traffic.json            2 flights  PASS
    scenario_conflict_detection.json       3 flights  PASS
    scenario_safe_passage.json             5 flights  PASS
    scenario_heavy_traffic.json            8 flights  PASS
    scenario_demo_comprehensive.json       4 flights  PASS
    scenario_pass_altitudes.json           3 flights  PASS
    scenario_collision_guaranteed.json     2 flights  FAIL  (horizontal, 0.02° lat apart)
    scenario_fail_vertical.json            2 flights  FAIL  (stacked, 200 m apart)
    scenario_fail_crossing.json            2 flights  FAIL  (head-on)
    scenario_fail_overtake.json            2 flights  FAIL  (fast catches slow)
    scenario_area_entry.json               4 flights  PASS  (boundary crossing, all separated)
    scenario_multi_conflict.json           5 flights  FAIL  (horizontal + vertical simultaneous)
```

---

## Build and Run

```bash
cd Sprint3/files
make                          # build simulation

# Command line
./simulation <scenario.json> [start_time] [print_interval]

# Examples
./simulation ../test/scenario_heavy_traffic.json        # 08:30 UTC
./simulation ../test/scenario_heavy_traffic.json 6      # 06:00 UTC
./simulation ../test/scenario_heavy_traffic.json 1 5    # step 1, print every 5 s

# Make targets
make run        # heavy traffic
make demo       # comprehensive demo
make heavy      # heavy traffic
make conflict   # conflict detection
make safe       # safe passage
make transatlantic  # transatlantic arrivals
make mixed          # mixed traffic
make debug          # transatlantic, print every step
```

### Interactive Menu

```
  +---------------- MAIN MENU ----------------+
  |  1. Load Scenario                         |
  |  2. Set Simulation Start Time             |
  |  3. Set Print Interval                    |
  |  4. Start Simulation                      |
  |  5. Display Flight Summary                |
  |  6. Select Weather Provider               |
  |  7. Run Demo (select scenario)            |
  |  8. Exit                                  |
  +-------------------------------------------+
```

### Demo Menu (option 7) — 13 scenarios

```
  --- PASS scenarios (no violations) ---
   1. Transatlantic arrivals (USA -> Iberia)
   2. Mixed traffic (local + charter)
   3. Conflict detection
   4. Safe passage (all clear)
   5. Heavy traffic (8 flights)
   6. Comprehensive demo
   7. Altitude-separated (3 flights, clean)
  --- FAIL scenarios (guaranteed violations) ---
   8. Collision horizontal (side-by-side)
   9. Collision vertical (stacked, 200 m apart)
  10. Collision crossing (head-on)
  11. Collision overtake (fast catches slow)
  12. Area entry/exit (boundary visibility)
  13. Multi-conflict (horizontal + vertical)
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

Timestep:

```
1 second — each step advances the simulation by 1 s.
At 460 kt (237 m/s) a 1 s step moves the aircraft ~237 m,
well within the 9260 m safety cylinder.
```

These values are defined as constants in `common.h` so they can be changed without touching any other file.

---

## Design Decisions

**Shared memory before threads and forks.** `shm_open` + `ftruncate` + `mmap` (`MAP_SHARED`) is called before any `pthread_create` or `fork`. All processes and threads inherit the mapped memory. The shared memory region contains the entire `SharedData` struct: flight data, violation queue, semaphores, mutexes, condition variables. Cleanup calls `shm_unlink` + `munmap`.

**Named semaphores for step sync, not unnamed.** Although the initial design (US105 analysis) considered unnamed `sem_init(pshared=1)`, named semaphores (`sem_open`) were chosen because they are reliably process-shared across forks on all POSIX systems. A pair of counting semaphores (`sem_step_start` / `sem_step_done`) handles N flights with a single pair: parent posts N times, waits N times.

**Process-shared mutexes and condition variables.** All mutexes (`pos_mutex`, `viol_mutex`, `detect_mutex`, `env_mutex`) are initialised with `PTHREAD_PROCESS_SHARED` attribute so they work across process boundaries. Same for condition variables (`viol_cond`, `detect_cond`, `env_cond`) with `PTHREAD_PROCESS_SHARED`.

**Four mutexes, not one global.** Separating concerns:
- `pos_mutex` — protects flight positions (read by detector, written by children)
- `viol_mutex` — protects the violation queue (written by detector, read by report thread)
- `detect_mutex` — protects `step_ready` flag (main writes, detector reads)
- `env_mutex` — protects `env_ready` flag (main writes, environment thread reads)

This avoids contention: the detector can scan positions while the report thread processes violations.

**Three threads, meeting US106 (2 required) + US110 (environment).**
- `violation_detector_thread` — scans all pairs each step, signals `viol_cond`
- `report_generator_thread` — `pthread_cond_timedwait` on `viol_cond` (1 s timeout), prints `[REPORT]` in real-time, calls `write_report()` at end
- `environment_thread` — loads weather from JSON, assigns wind per flight each step

**US107 separated from US106 into its own file.** Detector (US106) only signals; report thread (US107) consumes. Split into `us106/us106_threads.c` and `us107/us107_report_notify.c` to match US numbering and architectural separation.

**`pthread_cond_timedwait` (1 s) in report thread.** Instead of blocking indefinitely on `pthread_cond_wait`, a 1-second timeout lets the thread detect `shm->running == 0` within 1 s of simulation end, avoiding deadlock on shutdown.

**Wind drift applied in flight process, not just display.** Wind is read from `fd->wind_speed_kt` / `fd->wind_dir_deg` (written by environment thread) and applied as a perturbation to the aircraft's position and velocity after `advance()`. This is realistic: the aircraft flies through an air mass that moves relative to the ground, changing ground track and ground speed. Wind does not affect indicated airspeed or the aircraft's heading relative to the air mass. This means wind can affect collision detection results — a strong crosswind might push aircraft closer together or further apart — which is physically accurate for ATC simulation.

**Map grid visibility gated by `in_area()`.** Flights outside the monitored rectangle (`AREAs_LAT_MIN..MAX`, `LON_MIN..MAX`) are invisible on the map grid, even if active. They are always shown in the flight list (status `OUT`). Trail dots (`.`) also only draw for positions where `in_area()` is true. This matches real ATC displays that only show traffic within the sector.

**Flight-in-violation marker `*` (red bold).** When a flight is involved in a violation at the current step, its marker changes from `●` (dot) to `*` (asterisk) rendered in `ANSI_RED ANSI_BOLD`. The flight list appends `** VIOLATION **` for the same flights.

**Fast-forward for flights departing before sim start.** If a flight's departure time is before `sim_start_sec`, `fast_forward_flight()` advances the flight through its segments to compute its position at the simulation start, so it appears already mid-route. Flights departing after sim start remain inactive (`WAIT` status) until their departure time.

**Three weather providers.** Crazy Weather (`CW.json`, 45 zones, 0–12 000 ft), Happy Weather (`HP.json`, 45 zones, 0–12 000 ft), and Synthetic (`SYNTHETIC.json`, 20 zones, 0–45 000 ft, full coverage). The real providers only cover low altitude; the synthetic dataset fills the gap with jet-stream patterns at FL350–FL450. When no file is selected, a simple sine-wave formula generates wind.

**Safety cap.** `SAFETY_CAP_S = 10 800 s` (3 hours, 10 800 steps) limits the maximum iteration count. Exists only to prevent infinite loops if a JSON segment is unreachable. Normal termination is always `all flights completed`.

**`MAX_VIOLATIONS = 10800`** — equal to the maximum possible number of steps, so no violation is silently dropped.

---

## US Descriptions

### US105 — Hybrid Simulation Environment (`us105/us105_init.c`)

Creates the shared memory segment (`shm_open` + `ftruncate` + `mmap`), initialises process-shared mutexes and condition variables, opens named semaphores, copies flight plans into shared memory with fast-forwarded positions, creates all three threads, then forks one child per flight:

```c
/* Shared memory allocation */
shm_unlink(SHM_NAME);
int fd = shm_open(SHM_NAME, O_CREAT | O_RDWR, 0666);
ftruncate(fd, sizeof(SharedData));
SharedData *shm = mmap(NULL, sizeof(SharedData),
                       PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);

/* Process-shared mutexes */
pthread_mutexattr_t mattr;
pthread_mutexattr_init(&mattr);
pthread_mutexattr_setpshared(&mattr, PTHREAD_PROCESS_SHARED);
pthread_mutex_init(&shm->pos_mutex, &mattr);
pthread_mutex_init(&shm->viol_mutex, &mattr);
pthread_mutex_init(&shm->detect_mutex, &mattr);
pthread_mutex_init(&shm->env_mutex, &mattr);

/* Process-shared condition variables */
pthread_condattr_t cattr;
pthread_condattr_init(&cattr);
pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
pthread_cond_init(&shm->viol_cond, &cattr);
pthread_cond_init(&shm->detect_cond, &cattr);
pthread_cond_init(&shm->env_cond, &cattr);

/* Named semaphores */
shm->sem_step_start = sem_open(SEM_START_NAME, O_CREAT, 0666, 0);
shm->sem_step_done  = sem_open(SEM_DONE_NAME, O_CREAT, 0666, 0);

/* Threads */
pthread_create(&detect_thr, NULL, violation_detector_thread, shm);
pthread_create(&report_thr, NULL, report_generator_thread, shm);
pthread_create(&env_thr,    NULL, environment_thread, shm);

/* Forks */
for (int i = 0; i < n; i++) {
    pid_t pid = fork();
    if (pid == 0) {
        run_flight_process(i, shm);
        exit(0);
    }
    shm->flights[i].pid = pid;
}
```

Flights that departed before `sim_start_sec` are fast-forwarded to their current position using `fast_forward_flight()`, which calls `advance()` repeatedly to simulate elapsed time.

### US106 — Violation Detector Thread (`us106/us106_threads.c`)

A single thread that wakes on `detect_cond` each step, scans all flight pairs in shared memory under `pos_mutex`, calls `safety_breach()` (h < 9260 m && v < 305 m), and records violations:

```c
while (shm->running) {
    pthread_mutex_lock(&shm->detect_mutex);
    while (!shm->step_ready && shm->running)
        pthread_cond_wait(&shm->detect_cond, &shm->detect_mutex);
    if (!shm->running) { pthread_mutex_unlock(&shm->detect_mutex); break; }
    shm->step_ready = 0;
    pthread_mutex_unlock(&shm->detect_mutex);

    pthread_mutex_lock(&shm->pos_mutex);
    for (int i = 0; i < shm->n_flights; i++) {
        for (int j = i + 1; j < shm->n_flights; j++) {
            double h_m, v_m;
            if (safety_breach(fa->pos, fb->pos, &h_m, &v_m)) {
                pthread_mutex_lock(&shm->viol_mutex);
                Violation *v = &shm->violations[shm->n_violations++];
                v->step = step; v->fa = i; v->fb = j;
                v->pa = fa->pos; v->pb = fb->pos;
                v->h_m = h_m; v->v_m = v_m;
                shm->flights[i].n_viol++;
                shm->flights[j].n_viol++;
                pthread_cond_signal(&shm->viol_cond);
                pthread_mutex_unlock(&shm->viol_mutex);
            }
        }
    }
    pthread_mutex_unlock(&shm->pos_mutex);
}
```

`detect_initial_violations()` is called once at startup, before the main loop, to catch any violations that exist from the fast-forwarded initial positions. It directly writes to the violation queue and signals `viol_cond`.

### US107 — Report Notification Thread (`us107/us107_report_notify.c`)

A POSIX thread that waits on `viol_cond` with a 1-second timeout. On signal, iterates newly recorded violations and writes `[REPORT]` lines to stderr in real time. At simulation end, calls `write_report()`:

```c
while (shm->running) {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_sec += 1;  /* 1-second timeout */

    pthread_mutex_lock(&shm->viol_mutex);
    int rc = 0;
    while (shm->n_violations <= last_printed + 1 && shm->running && rc == 0)
        rc = pthread_cond_timedwait(&shm->viol_cond, &shm->viol_mutex, &ts);

    if (!shm->running) { pthread_mutex_unlock(&shm->viol_mutex); break; }

    while (last_printed + 1 < shm->n_violations) {
        last_printed++;
        Violation *v = &shm->violations[last_printed];
        snprintf(buf, sizeof(buf),
            "[REPORT] Step %d: %s <-> %s  h_dist=%.0fm  v_dist=%.0fm\n",
            v->step, shm->flights[v->fa].id, shm->flights[v->fb].id,
            v->h_m, v->v_m);
        write(STDERR_FILENO, buf, strlen(buf));
    }
    pthread_mutex_unlock(&shm->viol_mutex);
}

if (shm->generate_report)
    write_report(shm, shm->report_total_steps);
```

The timeout ensures the thread can detect `shm->running == 0` within 1 second of simulation end, preventing deadlock on shutdown. The thread prints violations as they happen (real-time, not batched at end).

### US108 — Step Synchronisation (`us108/us108_sync.c`)

**`simulation_step()`** — counting semaphore barrier:

```c
int simulation_step(SharedData *shm)
{
    /* Count active flights */
    int count = 0;
    for (int i = 0; i < shm->n_flights; i++)
        if (!shm->flights[i].completed) count++;
    if (count == 0) return 0;

    /* Release all active children */
    for (int i = 0; i < count; i++)
        sem_post(shm->sem_step_start);

    /* Wait for all active children (retry on EINTR) */
    for (int i = 0; i < count; i++) {
        int ret;
        do { ret = sem_wait(shm->sem_step_done); }
        while (ret != 0 && errno == EINTR);
    }

    /* Count remaining */
    int remaining = 0;
    for (int i = 0; i < shm->n_flights; i++)
        if (!shm->flights[i].completed) remaining++;
    return remaining;
}
```

**`run_flight_process()`** — each child's main loop (runs in a forked process):

```c
while (shm->running && !flight_completed) {
    /* barrier — wait for parent (retry on EINTR) */
    {   int ret;
        do { ret = sem_wait(shm->sem_step_start); }
        while (ret != 0 && errno == EINTR);
        if (ret != 0) break;
    }
    if (!shm->running) break;

    /* Activate at departure time */
    if (!started && current_time >= plan->departure_sec) {
        started = 1;
        pos = plan->seg[0].start;
        phase = plan->seg[0].phase;
    }

    if (started && cur_seg < plan->n_seg) {
        int done = advance(plan, &seg[cur_seg], &pos, &vel, timestep);

        /* Apply wind drift */
        if (wind_speed > 0.5) {
            wvx = -KT_TO_MS(wind_speed) * sin(wind_rad);
            wvy = -KT_TO_MS(wind_speed) * cos(wind_rad);
            pos.lon += R2D((wvx * dt) / (cos(lat_mid) * EARTH_R));
            pos.lat += R2D((wvy * dt) / EARTH_R);
            vel.vx += wvx;
            vel.vy += wvy;
        }

        if (done) {
            cur_seg++;
            if (cur_seg >= plan->n_seg) flight_completed = 1;
        }
    }

    /* Write to shared memory under pos_mutex */
    pthread_mutex_lock(&shm->pos_mutex);
    fd->pos = pos; fd->vel = vel; fd->phase = phase;
    fd->cur_seg = cur_seg; fd->active = started && !flight_completed;
    fd->completed = flight_completed;
    fd->in_area = in_area_full(pos);
    if (fd->in_area) fd->ever_in_area = 1;
    /* history */
    pthread_mutex_unlock(&shm->pos_mutex);

    sem_post(shm->sem_step_done);           /* signal parent we're done */
}
```

**EINTR safety**: both `simulation_step()` and `run_flight_process()` wrap `sem_wait()` in a `do...while` loop that retries on `EINTR`. This prevents deadlock if a signal (e.g., SIGINT from Ctrl+C) interrupts a `sem_wait()` call — without the retry, the interrupted semaphore is not decremented, causing the parent/child count to fall out of sync and the simulation to hang.

The guarantee: all children complete step t before the parent starts step t+1's detection.

### US109 — Report Generation (`us109/us109_report.c`)

`write_report()` generates a timestamped file `report_YYYYMMDD_HHMMSS.txt`:

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
  ...

============================================
  RESULT: PASS
============================================
```

The final verdict is `PASS` when `shm->n_violations == 0`, `FAIL` otherwise. Written by the report thread when simulation ends and `shm->generate_report` is set.

### US110 — Environment / Weather (`us110/us110_env.c`, `us110_weather.c`)

The environment thread loads wind data from JSON at startup, then for each simulation step:

```c
pthread_mutex_lock(&shm->pos_mutex);
for (int i = 0; i < shm->n_flights; i++) {
    FlightData *fd = &shm->flights[i];
    if (!fd->active && !fd->in_area) continue;
    double speed = 15.0, dir = 180.0;
    if (g_weather_loaded) {
        if (!get_wind_at(&g_weather_data, fd->pos.lat, fd->pos.lon,
                         fd->pos.alt, &speed, &dir)) {
            speed = 15.0; dir = 180.0;
        }
    } else {
        /* Synthetic sine wave */
        speed = 15.0 + 12.0 * sin(t * 2π / 6);
        dir = 180.0 + 40.0 * sin(t * 2π / 4);
    }
    fd->wind_speed_kt = speed;
    fd->wind_dir_deg = dir;
}
pthread_mutex_unlock(&shm->pos_mutex);
```

The `get_wind_at()` function performs zone lookup by latitude, longitude, and altitude (converted to feet). A zone matches when the position falls within its rectangular volume (lat_south..lat_north, lon_west..lon_east, alt_ft_lo..alt_ft_hi).

The weather JSON format:
```json
{
    "provider": "Crazy Weather Data",
    "duration_hours": 8.25,
    "zones": [
        {
            "lat_north": 43.84, "lat_south": 40.23,
            "lon_west": -9.80,  "lon_east": -7.95,
            "alt_ft_lo": 0, "alt_ft_hi": 1000,
            "dir_deg": 90, "speed_kt": 28.75
        }
    ]
}
```

The fallback synthetic formula generates periodic wind: speed varies between 3 and 27 kt over a 6-hour cycle, direction varies between 140° and 220° over a 4-hour cycle.

---

## UI Display

The map grid (70×20 characters) shows the Iberian airspace with lat/lon axes:

```
  IBERIAN AIRSPACE MONITOR  09:35 (UTC+1)  step=300  [LAT 38-44N  LON 10-2W]
  Wind: 15kt from 180°

  +--------------------------------------------------------------------+
  |                                                                    |
  |          ●                                                         |
  |          .                                                         |
  |          .                                                         |
  |          .●                                                        |
  |                                                                    |
  |                                    *                               |
  |                                                                    |
  |                          ●                                         |
  |                                                                    |
  |            ●                                                       |
  |                                                                    |
  |                                                                    |
  +--------------------------------------------------------------------+
  ------------------------------------------------------------------
  ● ARRIV1    alt=  9200m  spd= 400kt  IN
  ● ARRIV2    alt= 10600m  spd= 400kt  OUT
  ● EXIT1     alt= 10000m  spd= 400kt  IN
  ● LOCAL     alt= 11000m  spd= 430kt  IN
  >>> COLLISION at step 300: HORIZ1, HORIZ2
```

**Grid elements:**
- `●` (coloured dot) — flight inside area, not violating
- `*` (red bold) — flight inside area, currently in violation
- `.` (dim cyan) — trail dot, only drawn for history positions inside area
- `OUT` status — flight is active but outside monitored rectangle (invisible on grid)
- `WAIT` status — flight has not yet reached its departure time
- Violating flights show `** VIOLATION **` in the flight list
- `>>> COLLISION at step N: ID1, ID2` banner appears when violations are active

---

## Expected Output

### PASS — Safe Passage (0 violations)

```
Starting simulation...

SIMULATION RUNNING — Ctrl+C to stop

[ENV] Loaded Synthetic: 20 zones, 8.25 hours

  All flights completed at step 3732

  Simulation ended: 3732 steps, 3732s simulated.

  +------------ SIMULATION SUMMARY ------------+
  | Steps: 3732  Flights: 5  Violations: 0    |
  | RESULT: PASS                               |
  +--------------------------------------------+

  Flights:
  ● TP001     DONE  violations=0
  ● IB002     DONE  violations=0
  ● VY003     DONE  violations=0
  ● LH304     DONE  violations=0
  ● FR555     DONE  violations=0

  Report written to report_20260527_123456.txt  RESULT: PASS
```

### FAIL — Horizontal Collision (guaranteed)

```
[REPORT] Step 0: ALPHA1 <-> BRAVO2  h_dist=2226m  v_dist=0m
[REPORT] Step 1: ALPHA1 <-> BRAVO2  h_dist=2226m  v_dist=0m
...

  +------------ SIMULATION SUMMARY ------------+
  | Steps: 10800  Flights: 2  Violations: 600 |
  | RESULT: FAIL                               |
  +--------------------------------------------+

  ● ALPHA1    DONE  violations=300
  ● BRAVO2    DONE  violations=300

  Violations:
  #1  step=0     ALPHA1 <-> BRAVO2  h=2226m  v=0m
  ... and 599 more violations
```

### FAIL — Vertical Collision (stacked)

```
[REPORT] Step 0: VERT1 <-> VERT2  h_dist=0m  v_dist=200m
[REPORT] Step 1: VERT1 <-> VERT2  h_dist=0m  v_dist=200m
...

RESULT: FAIL
```

### FAIL — Crossing Collision (head-on)

```
[REPORT] Step <N>: CROSS1 <-> CROSS2  h_dist=<h>m  v_dist=0m

RESULT: FAIL
```

### FAIL — Overtake Collision

```
[REPORT] Step <N>: FAST1 <-> SLOW2  h_dist=...m  v_dist=0m

RESULT: FAIL
```

### FAIL — Multi-conflict (horizontal + vertical simultaneous)

```
[REPORT] Step 0: HORIZ1 <-> HORIZ2  h_dist=2226m  v_dist=0m
[REPORT] Step 0: VERT1 <-> VERT2  h_dist=0m  v_dist=200m

RESULT: FAIL
```

---

## Test Scenarios

| # | File | Flights | Expected | What it tests |
|---|------|---------|----------|---------------|
| 1 | `scenario_transatlantic_arrivals.json` | 4 | PASS | Flights entering from different directions, well separated |
| 2 | `scenario_mixed_traffic.json` | 2 | PASS | Local flights within area |
| 3 | `scenario_conflict_detection.json` | 3 | PASS | No intersection, clean |
| 4 | `scenario_safe_passage.json` | 5 | PASS | Well-separated corridors |
| 5 | `scenario_heavy_traffic.json` | 8 | PASS | High traffic volume, still safe |
| 6 | `scenario_demo_comprehensive.json` | 4 | PASS | Mixed demonstration |
| 7 | `scenario_pass_altitudes.json` | 3 | PASS | 9200 / 10600 / 11000 m — vertical separation guaranteed |
| 8 | `scenario_collision_guaranteed.json` | 2 | FAIL | ALPHA1 (41.00N) + BRAVO2 (41.02N) both at 9200 m, h~2226 m < 9260 m |
| 9 | `scenario_fail_vertical.json` | 2 | FAIL | VERT1 at 9000 m + VERT2 at 9200 m, same lat/lon, v=200 m < 305 m |
| 10 | `scenario_fail_crossing.json` | 2 | FAIL | CROSS1 N→S + CROSS2 S→N head-on along -7W at 10 000 m |
| 11 | `scenario_fail_overtake.json` | 2 | FAIL | FAST1 (500 kt) behind SLOW2 (250 kt) eastbound at 10 000 m, catches up |
| 12 | `scenario_area_entry.json` | 4 | PASS | ARRIV1 enters S (9200 m), ARRIV2 enters W (10 600 m), EXIT1 leaves S (10 000 m), LOCAL stays (11 000 m) — all vertically separated ≥600 m |
| 13 | `scenario_multi_conflict.json` | 5 | FAIL | HORIZ1/HORIZ2 horizontal (0.02° lat at 9200 m) + VERT1/VERT2 vertical (200 m diff at 39.50N) + SAFE1 isolated |

---

## What was implemented

- POSIX shared memory (`shm_open` + `ftruncate` + `mmap`, `MAP_SHARED`) for all inter-process data (US105)
- Process-shared mutexes (`PTHREAD_PROCESS_SHARED`) — 4 mutexes for fine-grained locking (US105)
- Process-shared condition variables — `detect_cond`, `viol_cond`, `env_cond` (US105)
- Named semaphores (`sem_open`) for step-sync barrier (US108)
- Violation detector thread — scans all pairs each step, records violations, signals `viol_cond` (US106)
- Report generator thread — `pthread_cond_timedwait` (1 s), real-time `[REPORT]` to stderr, calls `write_report()` at end (US107)
- Environment thread — loads JSON weather, assigns wind per flight per step (US110)
- Wind drift applied to aircraft position/velocity in flight process (realistic ground track perturbation)
- `detect_initial_violations()` at startup for fast-forwarded flights (US106)
- 3 weather providers: Crazy Weather, Happy Weather, Synthetic (full coverage)
- Synthetic sine-wave fallback when no file selected
- 13 demo scenarios (7 PASS + 6 FAIL) with one-key selection
- Map grid showing `●` (flight), `*` (violation), `.` (trail) inside area only
- Flight list with WAIT / OUT / IN / DONE status and `** VIOLATION **` marker
- Timestamped report file with flight summaries, violation log, PASS/FAIL verdict (US109)
- Fast-forward for flights departing before sim start
- SIGINT handler to cleanly stop simulation (sets `shm->running = 0`)
- Clean shutdown sequence: wait children, signal threads, `pthread_join`, cleanup shared memory
- `cleanup_shared_memory()` destroys mutexes, conditions, semaphores, unmaps and unlinks shm
- Zero `-Wall -Wextra` warnings at compile time
- Step-by-step semaphore barrier guaranteeing all children finish step t before detection runs
- `in_area()` / `in_area_full()` for area membership with altitude component
- `MAX_VIOLATIONS = 10800` (no dropped violations)
- `MAX_HISTORY = 600` snapshots per flight for trail rendering
- `last_viol_step[][]` matrix for per-step violation tracking in UI
- Adjustable `print_interval` with speed-up when flights are inside area
- All scenario files tested and verified

## Known limitations

- Wind drift affects ground track but does not affect climb/descent rate (vertical component not wind-perturbed)
- The report thread's 1-second timeout means shutdown may take up to 1 extra second
- Flight altitude orders (alt_adjust from Sprint 2) are not implemented in Sprint 3 — violations are detected but not resolved
- Named semaphores leave entries in `/dev/shm` until `sem_unlink` on cleanup — not a problem under normal shutdown but may leak if process is killed with SIGKILL
- No TCP remote access integration (out of scope for SCOMP)
- Weather file must be in JSON format — no direct xlsx parsing (done offline via `convert-weather.ps1`)
- `SYNTHETIC.json` is generated, not from a real provider — wind patterns are simplified

---

## Simulation Area and Constants

```
SAFETY_H_M  = 5.0 * 1852.0 = 9260.0 m   (ICAO Doc 4444, en-route radar)
SAFETY_V_M  = 305.0 m                     (ICAO RVSM, 1000 ft)
AREA_LAT_MIN =  38.0                      (Iberian airspace south)
AREA_LAT_MAX =  44.0                      (Iberian airspace north)
AREA_LON_MIN = -10.0                      (Iberian airspace west)
AREA_LON_MAX =  -2.0                      (Iberian airspace east)
AREA_MAX_ALT_M = 14000.0                  (maximum monitored altitude)
TIMESTEP_S     = 1                        (1 second per step)
MAX_VIOLATIONS = 10800                    (max possible steps)
MAX_HISTORY    = 600                      (trail snapshots per flight)
```

---

## Self-assessment of contribution (Sprint 3)

| US | Description | SCOMP Lead | Contribution |
|----|-------------|------------|-------------|
| US105 | Initialize hybrid simulation environment with shared memory | Jaime Simões | 100% |
| US106 | Implement function-specific threads in the parent process | Cláudio Pinto | 100% |
| US107 | Notify report thread via condition variables upon safety violation | Dinis Silva | 100% |
| US108 | Enforce step-by-step simulation synchronisation with semaphores | Fábio Costa | 100% |
| US109 | Generate and store final simulation report | André Barcelos | 100% |
| US110 | Integrate environmental influences (wind) into simulation | All team | 100% (shared) |
