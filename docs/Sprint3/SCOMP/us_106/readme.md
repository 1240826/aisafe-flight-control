# US106 — Implement Function-Specific Threads in the Parent Process

## 1. Context

This task is assigned in Sprint 3. The objective is to introduce dedicated threads in the
simulation parent process so that safety violation detection and report generation operate
concurrently and independently. This US builds on US105 (hybrid simulation environment with
shared memory) and is a prerequisite for US107 (condition variable notification between threads).

**Assigned to:** Cláudio Pinto

### 1.1 List of Issues

- Analysis: #76 (Function-Specific Threads in Parent Process)
- Design: #76 (Function-Specific Threads in Parent Process)
- Implement: #76 (Function-Specific Threads in Parent Process)
- Test: #76 (Function-Specific Threads in Parent Process)

---

## 2. Requirements

**US106** As a PO, I want the simulation controller parent process to have at least two dedicated
threads — one for safety violation detection and one for report generation — so that each
functionality operates concurrently and independently.

### Acceptance Criteria

- **US106.1** The parent process creates a safety violation detection thread responsible for
  scanning the shared memory for aircraft flight conflicts.
- **US106.2** A report generation thread is created to compile simulation results and respond to
  safety violation events.
- **US106.3** Any additional thread deemed appropriate for the required functionalities may be
  added.
- **US106.4** Threads are managed using mutexes and condition variables for internal
  synchronization.

### Dependencies/References

- US105 — Shared memory must exist and be initialised before threads are created.
- US107 — Condition variable notification between violation detector and report thread.
- US108 — Step-by-step synchronisation via semaphores (flight processes).
- US110 — Environment thread (weather data loading).
- US100–US103 — Sprint 2 simulation logic (processes, pipes, signals) that this Sprint 3
  architecture replaces and extends.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — Thread structure and synchronisation model

> "We are implementing a C simulation where the parent process must run at least two concurrent
> threads: one for safety violation detection (scanning shared memory) and one for report
> generation (responding to violations). Threads must synchronise using mutexes and condition
> variables. Suggest a thread structure and synchronisation approach."

**LLM suggestions adopted:**
- Dedicated thread functions: `violation_detector_thread()` and `report_generator_thread()`
  created with `pthread_create()` after shared memory is initialised
- A `pthread_mutex_t` protecting shared data structures (split into specialized mutexes)
- A `pthread_cond_t` used by the detector to signal the report thread when a new violation is
  detected — the report thread blocks on `pthread_cond_timedwait()` between events

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested a single global mutex for all shared memory — split into four specialized
  mutexes: `pos_mutex` (flight positions), `viol_mutex` (violation queue), `detect_mutex` (step
  readiness), and `env_mutex` (environment/weather data), to reduce contention
- The LLM proposed busy-waiting in the detector thread — replaced with `pthread_cond_wait` on
  `detect_cond`, blocking until `step_ready` is set by the main loop
- The LLM suggested `pthread_cond_timedwait` in the detector — the report thread uses
  `pthread_cond_timedwait` with a 1-second timeout, while the detector uses blocking wait
- An additional `environment_thread` (US110) was added to load weather data concurrently

---

### 3.1 Thread Model

Three threads are created in `init_hybrid_simulation()` (US105) after shared memory is set up:

| Thread | Function | File | Responsibility |
|--------|----------|------|---------------|
| Violation Detector | `violation_detector_thread()` | `us106_threads.c` | Scans shared memory for flight conflicts; signals report thread via condition variable |
| Report Generator | `report_generator_thread()` | `us107_report_notify.c` | Waits on `viol_cond`; logs violations in real-time; writes final report at end |
| Environment | `environment_thread()` | `us110_env.c` | Loads weather/environment data concurrently |

### 3.2 Synchronisation Model

```c
/* Violation Detector — step-driven */
pthread_mutex_lock(&detect_mutex);
pthread_cond_wait(&detect_cond, &detect_mutex); // waits for step_ready
/* step_ready consumed, now scan */
pthread_mutex_lock(&pos_mutex);
for each flight pair:
    if safety_breach():
        pthread_mutex_lock(&viol_mutex);
        enqueue_violation();
        pthread_cond_signal(&viol_cond);
        pthread_mutex_unlock(&viol_mutex);
pthread_mutex_unlock(&pos_mutex);
pthread_mutex_unlock(&detect_mutex);

/* Report Generator — event-driven */
pthread_mutex_lock(&viol_mutex);
pthread_cond_timedwait(&viol_cond, &viol_mutex, 1s timeout);
while unprocessed violations:
    log_violation();
pthread_mutex_unlock(&viol_mutex);
/* end of simulation: call write_report() */
```

---

### 3.3 Key Design Decisions

**detect_initial_violations()** — Before the simulation loop begins, a separate function scans
all active flights for conflicts at step 0. Any initial violations are recorded in shared memory
and signalled to the report thread the same way as step-driven violations.

**Step-driven detector** — The detector thread does not poll. It blocks on `detect_cond` and is
woken by the main loop when `step_ready` is set (via `pthread_cond_broadcast`). This avoids
busy-waiting and ensures the detector always runs after flight processes have updated positions.

**Process-shared synchronisation** — Mutexes and condition variables are initialised with
`PTHREAD_PROCESS_SHARED` attribute so they synchronise both threads (parent process) and child
flight processes writing to shared memory.

---

## 4. Design

### 4.1 Initialisation Flow

```
main()
  → init_hybrid_simulation()          ← US105
      → shm_open / mmap / memset
      → init mutexes (PROCESS_SHARED)
      → init cond vars (PROCESS_SHARED)
      → copy flight plans to shm
      → fast_forward_flights() to sim_start
      → pthread_create(violation_detector_thread)   ← US106
      → pthread_create(report_generator_thread)      ← US107
      → pthread_create(environment_thread)           ← US110
      → fork() child flight processes               ← US108
  → detect_initial_violations()                     ← US106
  → simulation loop (sem_step_start / sem_step_done)
  → set shm->running = 0
  → pthread_join(all threads)
  → cleanup_shared_memory()
```

### 4.2 Acceptance Tests

**AT1 — Violation detector thread is created and runs (US106.1, US106.4)**

Given the simulation starts with at least two flight processes,
When `init_hybrid_simulation()` calls `pthread_create()` for `violation_detector_thread`,
Then the thread is running, blocked on `detect_cond`, and wakes when `step_ready` is set.

**AT2 — Detector uses mutexes to protect shared memory reads (US106.4)**

Given the detector thread reads flight positions from shared memory,
When it accesses the shared memory segment,
Then it always acquires `pos_mutex` before reading and releases it after,
preventing data races with child flight processes writing positions.

**AT3 — Initial violations are detected at step 0**

Given active flights that are in safety breach at simulation start,
When `detect_initial_violations()` runs before the main loop,
Then violations are recorded in shared memory and signalled via `viol_cond`.

**AT4 — Threads are joined cleanly on termination (US106.4)**

Given the simulation has completed all steps,
When `main()` sets `shm->running = 0` and calls `pthread_join()`,
Then both threads exit without deadlock and all mutexes/condvars are destroyed.

---

## 5. Implementation

| File | Responsibility |
|------|---------------|
| `us106_threads.c` | `violation_detector_thread()`, `detect_initial_violations()` |
| `us106_threads.h` | Declarations for the above functions |
| `us105_init.c` | `init_hybrid_simulation()` — creates shared memory, mutexes, condvars, threads, forks children |
| `us107_report_notify.c` | `report_generator_thread()` — logs violations, writes final report |
| `files/common.h` | `SharedData` struct with all mutexes, condvars, and shared state |

**Compile:**
```bash
gcc -Wall -Wextra -D_GNU_SOURCE \
    files/main.c us105_init.c us106_threads.c us107_report_notify.c \
    us108_sync.c us109_report.c us110_env.c us110_weather.c \
    files/physics.c files/ui.c \
    -o simulation -lpthread -lrt -lm
```

---

## 6. Integration/Demonstration

1. Compile the simulation with all US modules.
2. Run with a scenario that produces conflicts:
   ```bash
   ./simulation ../test/scenario3_violations.json
   ```
3. Expected output (real-time):
   ```
   [DETECTOR] Step 0: TP201 <-> IB202  h=2340m v=89m
   [REPORT]   Step 0: TP201 <-> IB202  h_dist=2340m  v_dist=89m
   ...
   [DETECTOR] Step 42: TP201 <-> IB202  h=1120m v=45m
   [REPORT]   Step 42: TP201 <-> IB202  h_dist=1120m  v_dist=45m
   ```
4. Confirm both threads join cleanly — no orphaned threads or mutex errors.

---

## 7. Observations

- `violation_detector_thread()` is step-driven: it blocks on `detect_cond` and runs once per step.
- `report_generator_thread()` is event-driven: it uses `pthread_cond_timedwait` with a 1-second
  timeout to periodically check for new violations while staying responsive to shutdown.
- `detect_initial_violations()` is NOT a thread — it runs in the parent before the main loop to
  capture any existing conflicts at step 0.
- All mutexes and condvars use `PTHREAD_PROCESS_SHARED` because child flight processes access the
  same shared memory regions.
- Mutexes are split into four (`pos_mutex`, `viol_mutex`, `detect_mutex`, `env_mutex`) to
  minimise lock contention between threads.
