# US108 – Semaphore-Based Step-by-Step Synchronization

## 1. Context

As a Flight Control Operator, I want the simulation steps to be synchronized using semaphores, so that all flight processes advance in lockstep and the parent can detect violations after all positions are updated.

**Assigned to:** Fábio Costa

### List of Issues

- Analysis: #78
- Design: #78
- Implement: #78
- Test: #78

---

## 2. Requirements

- The parent process and child flight processes must synchronize each simulation step using POSIX semaphores.
- Each step: parent signals children to start → children compute next position → children signal completion → parent detects violations.
- Semaphores must be process-shared (POSIX named semaphores, inherently shared via kernel namespace).
- The synchronization must ensure no child runs ahead of others (lockstep barrier).

## 3. Analysis

### Approach

The previous Sprint 2 approach used blocking `read()` on pipes as a barrier:
```
parent: write(go_pipe[i])  →  child: read() unblocks → compute → write(report_pipe[i]) → parent: read() unblocks
```

In Sprint 3, the pipe-based barrier is replaced by a counting semaphore pair stored in shared memory:

- `sem_step_start`: Counting semaphore, initially 0.  
  Parent posts N times (one per active child). Each child waits once.

- `sem_step_done`: Counting semaphore, initially 0.  
  Each child posts once after writing its updated position to shared memory.  
  Parent waits N times (one per active child) before proceeding to violation detection.

### Synchronization Flow

```
PARENT (step = 0..N):
   1. simulation_step(shm):
        a. Count active children (not completed)
        b. For each active child:
             sem_post(sem_step_start)     // unblock child
        c. For each active child:
             sem_wait(sem_step_done)      // wait for child (retry on EINTR)

   2. step_ready = 1; signal detect_cond    // detector thread scans for violations
   3. env_ready = 1; signal env_cond        // environment thread updates wind
   4. Check termination (all completed / Ctrl+C)

CHILD (flight i):
   1. sem_wait(sem_step_start)             // block until parent starts (retry on EINTR)
   2. if !shm->running: exit loop
   3. Advance physics: advance(segment, pos, vel, timestep)
   4. Apply wind displacement (if any)
   5. Lock pos_mutex
   6. Write new pos, vel, phase to shared memory
   7. history[hist_count++] = snapshot
   8. Unlock pos_mutex
   9. sem_post(sem_step_done)              // signal parent we're done
```

This guarantees that all children complete step `t` before the parent starts violation detection for step `t`.

---

## 4. Design

### Semaphore Initialization

```c
/* POSIX named semaphores — inherently process-shared via kernel namespace */
sem_unlink("/aisafe_start");
sem_unlink("/aisafe_done");
shm->sem_step_start = sem_open("/aisafe_start", O_CREAT, 0666, 0);
shm->sem_step_done  = sem_open("/aisafe_done",  O_CREAT, 0666, 0);
```

Named semaphores (`sem_open`) are used instead of unnamed (`sem_init`). This avoids the need to store the `sem_t` structure in shared memory — the kernel manages them by name, accessible to any process that calls `sem_open` with the same name. Both parent and children access the same semaphores via the `sem_t*` pointers copied into the shared memory segment.

### Parent Step Loop (actual code — `simulation_step()` in `us108_sync.c`)

```c
int simulation_step(SharedData *shm)
{
    int count = 0;
    for (int i = 0; i < shm->n_flights; i++)
        if (!shm->flights[i].completed) count++;

    if (count == 0) return 0;

    /* Release children — post for each active flight */
    for (int i = 0; i < count; i++)
        sem_post(shm->sem_step_start);

    /* Collect children — wait for all to complete this step */
    for (int i = 0; i < count; i++) {
        int ret;
        do { ret = sem_wait(shm->sem_step_done); }
        while (ret != 0 && errno == EINTR);  /* EINTR safety */
    }

    /* Recalculate remaining after step */
    int remaining = 0;
    for (int i = 0; i < shm->n_flights; i++)
        if (!shm->flights[i].completed) remaining++;

    return remaining;
}
```

### Child Step Loop (actual code — `run_flight_process()` in `us108_sync.c`)

```c
while (shm->running && !flight_completed) {
    /* Wait for parent signal (retry on EINTR) */
    int ret;
    do { ret = sem_wait(shm->sem_step_start); }
    while (ret != 0 && errno == EINTR);
    if (ret != 0) break;
    if (!shm->running) break;

    /* Compute physics for this timestep */
    if (started && cur_seg < plan->n_seg) {
        int done = advance(plan, &plan->seg[cur_seg], &pos, &vel, shm->timestep);
        /* Apply wind displacement */
        if (fd->wind_speed_kt > 0.5) {
            double wvx = ...; double wvy = ...;
            pos.lon += R2D((wvx * dt) / (cos(lat_mid) * EARTH_R));
            pos.lat += R2D((wvy * dt) / EARTH_R);
        }
        if (done) { cur_seg++; /* advance or complete */ }
    }

    /* Write results to shared memory under mutex */
    pthread_mutex_lock(&shm->pos_mutex);
    fd->pos = pos; fd->vel = vel; fd->phase = phase;
    fd->completed = flight_completed;
    fd->active = started && !flight_completed;
    /* store history snapshot */
    pthread_mutex_unlock(&shm->pos_mutex);

    /* Signal parent step done */
    sem_post(shm->sem_step_done);
}
```

---

## 5. Implementation

| File | Responsibility |
|------|---------------|
| `us108_sync.c` | `run_flight_process()` (child step loop), `simulation_step()` (parent barrier), `sem_wait_retry()` (EINTR-safe wrapper) |
| `us108_sync.h` | Function declarations |
| `common.h` | `sem_t *sem_step_start`, `sem_t *sem_step_done` pointers in `SharedData` |

Semaphores are POSIX **named** semaphores (`sem_open`) — inherently process-shared via the kernel namespace. No `sem_init` with `pshared` flag is needed. Cleanup is done with `sem_close()` (release reference) followed by `sem_unlink()` (mark for removal after all processes close it).

---

## 6. Integration/Demonstration

```bash
make simulation
./simulation ../test/scenario_conflict_detection.json 1
```

Or run any of the predefined Makefile targets:
```bash
make heavy       # ../test/scenario_heavy_traffic.json
make conflict    # ../test/scenario_conflict_detection.json
make safe        # ../test/scenario_safe_passage.json
make demo        # ../test/scenario_demo_comprehensive.json
```

Expected output: all flights advance in lockstep. No flight runs ahead of others. Violations detected at the correct simulated time.

---

## 7. Observations

- `sem_wait()` can return `EINTR` if a signal (e.g., SIGINT from Ctrl+C) is delivered to the process. Both the parent and child must **retry** the `sem_wait()` call on `EINTR`, otherwise the semaphore is not decremented and the synchronization count becomes mismatched — leading to a **deadlock**. The helper `sem_wait_retry()` wraps this pattern:
  ```c
  int ret;
  do { ret = sem_wait(sem); } while (ret != 0 && errno == EINTR);
  ```
- `sem_post()` is async-signal-safe and can be called from signal handlers if needed.
- POSIX **named** semaphores (`sem_open`) are used instead of unnamed (`sem_init`). Named semaphores are inherently process-shared via the kernel namespace — no `pshared` flag is needed, and the `sem_t` structure does not need to reside in shared memory.
- **Cleanup**: `sem_close()` releases the process's reference; `sem_unlink()` marks the semaphore for removal after all processes close it. This two-step cleanup matches the POSIX named semaphore lifecycle (as shown in the professor's examples).
- Counting semaphores eliminate the need for a per-flight semaphore pair — a single pair (`sem_step_start` + `sem_step_done`) suffices for N flights.
