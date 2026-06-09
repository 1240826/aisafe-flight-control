# US107 — Notify Report Thread via Condition Variables upon Safety Violation Detection

## 1. Context

This task was assigned in Sprint 3 within the Computer Systems (SCOMP) scope. The objective is to implement the inter-thread notification mechanism between the safety violation detection thread and the report generation thread, using POSIX condition variables and mutexes, so that safety violations are logged in real time as the simulation progresses.

**Assigned to:** Dinis Silva

### 1.1 List of Issues

- Analysis: #81
- Design: #81
- Implement: #81
- Test: #81

---

## 2. Requirements

**US107** As a PO, I want the simulation system safety violation detection thread to notify the report generation thread through condition variables when a safety violation occurs, so that the report is updated in real time with accurate information.

### Acceptance Criteria

- **US107.1** The safety violation detection thread continuously monitors shared memory for flight conflicts.
- **US107.2** Upon detecting a safety violation, the detection thread signals the report generation thread using a POSIX condition variable.
- **US107.3** The report generation thread waits on the condition variable and, upon being signalled, immediately processes and logs the safety violation event.
- **US107.4** Proper mutex locking must be used around all accesses to the shared violation data to ensure thread safety.

### Dependencies/References

- US105 — Initialize hybrid simulation environment with shared memory (shared memory and thread setup)
- US106 — Implement function-specific threads in the parent process (creates the detection and report threads that this US connects)
- US108 — Enforce step-by-step simulation synchronization (semaphores controlling simulation progression)
- US109 — Generate and store final simulation report (report thread writes the final output)

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "In a POSIX C multi-threaded simulation, what is the correct pattern for a producer-consumer notification between a violation detection thread and a report generation thread using pthread condition variables and mutexes? What are the common pitfalls?"

**LLM suggestions adopted:**
- The standard producer-consumer pattern: the detection thread locks the mutex, writes violation data to a shared structure, signals the condition variable, and unlocks the mutex
- The report thread loops on `pthread_cond_timedwait` inside a `while` check on a predicate to guard against spurious wakeups

**Decisions made by the team:**
- A dedicated `pthread_mutex_t` (`viol_mutex`) and `pthread_cond_t` (`viol_cond`) pair is used exclusively for the detection-to-report notification channel, keeping synchronization concerns separated from the simulation step semaphores (US108)
- The report thread does not block the detection thread — after signalling, the detection thread immediately releases the mutex and resumes monitoring
- The violation data is stored in a flat fixed-size array (`violations[MAX_VIOLATIONS]`) inside shared memory, with a counter (`n_violations`) — simpler than a circular buffer and avoids dynamic memory allocation in inter-process context
- All sync primitives (`viol_mutex`, `viol_cond`) are initialized with `PTHREAD_PROCESS_SHARED` so they work across process boundaries, even though detector and report threads belong to the same parent process (required because child flight processes may also read violation data)

### 3.1 Concurrency Model

Two threads in the parent process interact through this mechanism:

- **Safety Violation Detection Thread** (US106 — producer) — scans shared memory at each simulation step, detects proximity violations, appends to `violations[]` array, increments `n_violations`, and signals `viol_cond`
- **Report Generation Thread** (US107 — consumer) — waits on `viol_cond` via `pthread_cond_timedwait` with a 1-second timeout, wakes up on signal, drains new violations by comparing `n_violations` against its `last_printed` index, and logs each event to stderr

The shared violation data structure and its associated mutex/condition variable are allocated in the shared memory segment (`SharedData` struct), so both threads within the same process can access them.

### 3.2 Shared Data Structures

```c
/* Violation event stored in shared memory */
typedef struct {
    int step;
    time_t ts;
    int fa, fb;               /* flight indices */
    Pos3D pa, pb;             /* positions */
    Vel3D va, vb;             /* velocities */
    double h_m, v_m;          /* horizontal / vertical separation */
} Violation;

/* Inside SharedData (shared memory) */
Violation violations[MAX_VIOLATIONS];
int n_violations;
pthread_mutex_t viol_mutex;
pthread_cond_t  viol_cond;
```

### 3.3 Realization

**Files to create/modify:**

| File | Responsibility |
|------|---------------|
| `files/common.h` | Defines `Violation` struct and declares `viol_mutex`, `viol_cond`, `violations[]`, `n_violations` in `SharedData` |
| `us105/us105_init.c` | Initializes `viol_mutex` and `viol_cond` with `PTHREAD_PROCESS_SHARED` attribute |
| `us106/us106_threads.c` | Detection thread loop; locks `viol_mutex`, writes to `violations[]`, signals `viol_cond` |
| `us107/us107_report_notify.c` | Report thread loop; calls `pthread_cond_timedwait` on `viol_cond`, drains new violations, logs to stderr |

### 3.4 Detection Thread Logic (producer)

```c
void *violation_detector_thread(void *arg) {
    SharedData *shm = (SharedData *)arg;

    while (shm->running) {
        pthread_mutex_lock(&shm->detect_mutex);
        while (!shm->step_ready && shm->running)
            pthread_cond_wait(&shm->detect_cond, &shm->detect_mutex);
        shm->step_ready = 0;
        pthread_mutex_unlock(&shm->detect_mutex);

        pthread_mutex_lock(&shm->pos_mutex);
        for each flight pair (i, j):
            if safety_breach():
                pthread_mutex_lock(&shm->viol_mutex);
                if (shm->n_violations < MAX_VIOLATIONS):
                    v = &shm->violations[shm->n_violations++]
                    fill violation data (step, ts, fa, fb, pa, pb, va, vb, h_m, v_m)
                    pthread_cond_signal(&shm->viol_cond)
                pthread_mutex_unlock(&shm->viol_mutex)
        pthread_mutex_unlock(&shm->pos_mutex);
    }
    pthread_exit(NULL);
}
```

### 3.5 Report Thread Logic (consumer)

```c
void *report_generator_thread(void *arg) {
    SharedData *shm = (SharedData *)arg;
    int last_printed = -1;

    while (shm->running) {
        struct timespec ts;
        clock_gettime(CLOCK_REALTIME, &ts);
        ts.tv_sec += 1;

        pthread_mutex_lock(&shm->viol_mutex);
        int rc = 0;
        /* Wait for new violations or timeout */
        while (shm->n_violations <= last_printed + 1 && shm->running && rc == 0)
            rc = pthread_cond_timedwait(&shm->viol_cond, &shm->viol_mutex, &ts);

        /* Drain all new violations */
        while (last_printed + 1 < shm->n_violations) {
            last_printed++;
            Violation *v = &shm->violations[last_printed];
            log_violation(v);
        }
        pthread_mutex_unlock(&shm->viol_mutex);
    }

    /* Simulation ended — write final report */
    if (shm->generate_report)
        write_report(shm, shm->report_total_steps);

    pthread_exit(NULL);
}
```

### 3.6 Acceptance Tests

**AT1 — Violation is logged in real time (US107.1, US107.2, US107.3)**

Given a running simulation where two aircraft enter a proximity violation at step N,
When the detection thread detects the conflict, locks `viol_mutex`, appends to `violations[]`, and signals `viol_cond`,
Then the report thread wakes up, drains the new violation, and logs it to stderr before step N+1 begins.

**AT2 — Spurious wakeup does not cause incorrect behaviour (US107.3)**

Given the report thread waiting on `pthread_cond_timedwait` for `viol_cond`,
When a spurious wakeup occurs with no new violations (`n_violations <= last_printed + 1`),
Then the report thread returns to waiting without logging any event.

**AT3 — Multiple violations in rapid succession are not lost (US107.1)**

Given two violations detected within the same simulation step,
When both are appended to `violations[]` before the report thread drains them,
Then the report thread logs both events correctly, in FIFO order.

**AT4 — Mutex ensures thread-safe access to the array (US107.4)**

Given the detection thread and report thread accessing the violation array concurrently,
When both attempt to access it simultaneously,
Then the mutex guarantees that only one thread accesses it at a time, with no data corruption.

---

## 4. Design

### 4.1 Initialisation Flow

```
main()
  → init_hybrid_simulation()                  ← US105
      → shm_open / mmap / memset
      → sem_open (named semaphores)
      → init mutexes (PROCESS_SHARED)         ← viol_mutex included
      → init cond vars (PROCESS_SHARED)       ← viol_cond included
      → copy flight plans to shm
      → pthread_create(violation_detector_thread)   ← US106
      → pthread_create(report_generator_thread)      ← US107
      → pthread_create(environment_thread)           ← US110
      → fork() child flight processes               ← US108
  → detect_initial_violations()
  → simulation loop
  → set shm->running = 0
  → pthread_join(all threads)
  → cleanup_shared_memory()                  ← sem_close + sem_unlink
```

### 4.2 Sequence — Violation Notification

```
Detector Thread                 Shared Memory                 Report Thread
     │                               │                             │
     │  lock(viol_mutex)             │                             │
     │  write violations[n_viols]    │                             │
     │  n_violations++               │                             │
     │  pthread_cond_signal(viol_cond)──► (wakes up)              │
     │  unlock(viol_mutex)           │                             │
     │                               │                             │  lock(viol_mutex)
     │                               │                             │  read violations[last_printed+1..n_violations-1]
     │                               │                             │  unlock(viol_mutex)
     │                               │                             │  log to stderr
```

---

## 5. Implementation

| File | Responsibility |
|------|---------------|
| `files/common.h` | `SharedData` struct — includes `Violation violations[MAX_VIOLATIONS]`, `int n_violations`, `pthread_mutex_t viol_mutex`, `pthread_cond_t viol_cond` |
| `us105/us105_init.c` | Initializes `viol_mutex` and `viol_cond` with `PTHREAD_PROCESS_SHARED` |
| `us106/us106_threads.c` | `violation_detector_thread()` — detects violations, appends to `violations[]`, signals `viol_cond` |
| `us107/us107_report_notify.c` | `report_generator_thread()` — `pthread_cond_timedwait` on `viol_cond`, real-time `[REPORT]` logging to stderr, calls `write_report()` at end |
| `us109/us109_report.c` | `write_report()` — final report file with PASS/FAIL |

**Compile:**
```bash
cd Sprint3/files && make
```

---

## 6. Integration/Demonstration

1. Compile the simulation with all US modules.
2. Run with a scenario that produces conflicts:
   ```bash
   ./simulation ../test/scenario_collision_guaranteed.json
   ```
3. Expected output (real-time):
   ```
   [DETECTOR] Step 0: ALPHA1 <-> BRAVO2  h=2226m v=0m
   [REPORT]   Step 0: ALPHA1 <-> BRAVO2  h_dist=2226m  v_dist=0m
   [DETECTOR] Step 1: ALPHA1 <-> BRAVO2  h=2226m v=0m
   [REPORT]   Step 1: ALPHA1 <-> BRAVO2  h_dist=2226m  v_dist=0m
   ```
4. Confirm both threads join cleanly — no orphaned threads or mutex errors.

---

## 7. Observations

- `violation_detector_thread()` is step-driven: it blocks on `detect_cond` and runs once per step.
- `report_generator_thread()` is event-driven: it uses `pthread_cond_timedwait` with a 1-second timeout to periodically check for new violations while staying responsive to shutdown.
- The flat `violations[]` array in shared memory avoids dynamic allocation and is safe for inter-process access. The `viol_mutex` protects both reads (report thread) and writes (detection thread) to the array and counter.
- `detect_initial_violations()` is NOT a thread — it runs in the parent before the main loop to capture any existing conflicts at step 0. It also signals `viol_cond` so the report thread processes initial violations.
- The `pthread_cond_timedwait` with 1 s timeout in the report thread ensures it detects `shm->running == 0` within 1 second of simulation end, preventing deadlock on shutdown.
- All mutexes and condvars use `PTHREAD_PROCESS_SHARED` even though the detector and report threads are in the same process, for consistency and because child flight processes also access shared memory.
