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
- Semaphores must be process-shared (stored in shared memory).
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
  1. For each active child i:
       sem_post(&shm->sem_step_start)     // unblock child i

  2. For each active child i:
       sem_wait(&shm->sem_step_done)      // wait for child i

  3. Signal detector thread to scan for violations
  4. Check termination conditions

CHILD (flight i, infinite loop):
  1. sem_wait(&shm->sem_step_start)       // block until parent starts step
  2. if shared->running == 0: exit
  3. Lock pos_mutex
  4. Read alt_adjust / hold_position from shared memory
  5. If !hold_position: advance() physics
  6. Write new position to shared memory
  7. history[n++] = snapshot
  8. Unlock pos_mutex
  9. sem_post(&shm->sem_step_done)        // signal parent we're done
```

This guarantees that all children complete step `t` before the parent starts violation detection for step `t`.

---

## 4. Design

### Semaphore Initialization (in Shared Memory)

```c
sem_init(&shm->sem_step_start, 1, 0);   // pshared=1, initial 0
sem_init(&shm->sem_step_done,  1, 0);   // pshared=1, initial 0
```

The `pshared=1` flag makes both semaphores accessible to the parent process and all forked children because they reside in the shared memory segment.

### Parent Step Loop Pseudocode

```c
for (step = 0; step < max_steps && !stop; step++) {
    // Release children
    for (i = 0; i < n; i++)
        if (shared->flights[i].active)
            sem_post(&shared->sem_step_start);

    // Collect children
    for (i = 0; i < n; i++)
        if (shared->flights[i].active)
            sem_wait(&shared->sem_step_done);

    // Signal detection thread to scan
    pthread_mutex_lock(&shared->detect_mutex);
    shared->step_ready = 1;
    pthread_cond_signal(&shared->detect_cond);
    pthread_mutex_unlock(&shared->detect_mutex);
}
```

### Child Step Loop Pseudocode

```c
while (shared->running) {
    sem_wait(&shared->sem_step_start);
    if (!shared->running) break;

    pthread_mutex_lock(&shared->pos_mutex);

    if (shared->flights[idx].hold_position) {
        // hold, don't advance
    } else {
        if (shared->flights[idx].alt_adjust != 0) {
            pos.alt += shared->flights[idx].alt_adjust;
            shared->flights[idx].alt_adjust = 0;
        }
        done = advance(plan, &seg[cur], &pos, &vel, timestep);
        shared->flights[idx].pos = pos;
        shared->flights[idx].vel = vel;
        if (done) shared->flights[idx].completed = 1;
    }

    pthread_mutex_unlock(&shared->pos_mutex);
    sem_post(&shared->sem_step_done);
}
```

---

## 5. Implementation

| File | Responsibility |
|------|---------------|
| `us108_sync.c` | `step_sync()`, `flight_wait_start()`, `flight_signal_done()`, barrier logic |
| `us108_sync.h` | Function declarations |
| `common.h` | Semaphore fields in `SharedData` |

Semaphores are unnamed POSIX semaphores initialized with `pshared=1` so they work across fork boundaries. Cleanup: `sem_destroy()` after child processes have exited.

---

## 6. Integration/Demonstration

```bash
make
 ../test/scenario3_violations.json 09:30
```

Expected output: all flights advance in lockstep. No flight runs ahead of others. Violations detected at the correct simulated time.

---

## 7. Observations

- `sem_wait()` can return `EINTR` if a signal is received. The child and parent must handle this by retrying the `sem_wait()` call.
- `sem_post()` is async-signal-safe and can be called from signal handlers if needed.
- Counting semaphores eliminate the need for a per-flight semaphore pair — a single pair suffices for N flights.
