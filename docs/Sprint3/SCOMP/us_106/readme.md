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
- Two dedicated thread functions: `violation_detector_thread()` and `report_generator_thread()`
  created with `pthread_create()` in `main()` after shared memory is initialised
- A `pthread_mutex_t` protecting the violation event queue written by the detector and read by
  the report thread
- A `pthread_cond_t` used by the detector to signal the report thread when a new violation is
  detected — the report thread blocks on `pthread_cond_wait()` between events

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested a single global mutex for all shared memory — split into two: `pos_mutex`
  for flight positions and `viol_mutex` for the violation queue, to reduce contention
- The LLM proposed busy-waiting in the detector thread — replaced with `pthread_cond_timedwait`
  to avoid wasting CPU cycles between simulation steps

---

### 3.1 Thread Model

| Thread | Function | Responsibility |
|--------|----------|---------------|
| Violation Detector | `violation_detector_thread()` | Scans shared memory for flight conflicts; signals report thread via condition variable |
| Report Generator | `report_generator_thread()` | Waits on condition variable; compiles results and logs violation events |

Both threads share access to the flight state in shared memory and the violation event queue,
protected by mutexes.

### 3.2 Synchronisation Model

```c
/* violation_detector_thread() */
pthread_mutex_lock(&pos_mutex);
/* read flight positions from shared memory */
pthread_mutex_unlock(&pos_mutex);
if (conflict_detected) {
    pthread_mutex_lock(&viol_mutex);
    enqueue_violation(event);
    pthread_cond_signal(&viol_cond);
    pthread_mutex_unlock(&viol_mutex);
}

/* report_generator_thread() */
pthread_mutex_lock(&viol_mutex);
pthread_cond_wait(&viol_cond, &viol_mutex);
/* process violation event */
pthread_mutex_unlock(&viol_mutex);
```

---

## 4. Design

The parent process creates both threads after shared memory is initialised (US105). The
violation detector scans shared memory on each simulation step, locking `pos_mutex` for reads.
When a conflict is found, it enqueues the event under `viol_mutex` and signals `viol_cond`.
The report thread blocks on `pthread_cond_wait()` and wakes immediately upon signal, processes
the event, and blocks again — ensuring violations are logged in real time rather than at the
end of the simulation.
main():
init_shared_memory()          ← US105
pthread_create(violation_detector_thread)
pthread_create(report_generator_thread)
run simulation (child processes)
pthread_join() both threads
cleanup mutexes and cond

### 4.1 Acceptance Tests

**AT1 — Both threads are created and run concurrently (US106.1, US106.2)**

Given the simulation starts with at least two flight processes,
When `main()` initialises shared memory and calls `pthread_create()` for both threads,
Then both `violation_detector_thread()` and `report_generator_thread()` are running
concurrently before any flight process executes its first step.

**AT2 — Violation logged immediately, not at end of simulation (US106.2)**

Given a simulation with flights that produce a conflict at step t,
When the violation detector signals the report thread,
Then the report thread logs the event at step t — not after all steps complete.

**AT3 — Detector uses mutex to protect shared memory reads (US106.4)**

Given the detector thread reads flight positions from shared memory,
When it accesses the shared memory segment,
Then it always acquires `pos_mutex` before reading and releases it immediately after,
preventing data races with the child flight processes writing positions.

**AT4 — Both threads joined cleanly on termination (US106.4)**

Given the simulation has completed all steps,
When `main()` signals both threads to stop and calls `pthread_join()`,
Then both threads exit without deadlock and all mutexes and condition variables are destroyed
before the process exits.

---

## 5. Implementation

| File | Responsibility |
|------|---------------|
| `us106_threads.c` | `violation_detector_thread()`, `report_generator_thread()`, thread creation |
| `us106_threads.h` | Thread function declarations, mutex and condition variable declarations |
| `main.c` | Calls thread initialisation after shared memory initialisation (US105) |

**Compile:**
```bash
gcc -Wall -Wextra -D_GNU_SOURCE \
    main.c us105_shm.c us106_threads.c us107_notify.c us108_sync.c \
    -o simulation -lpthread -lm
```

---

## 6. Integration/Demonstration

```bash
./simulation ../test/scenario3_violations.json
```

Expected — violation logged immediately by the report thread, not at end of simulation:
[DETECTOR] step=2338  conflict detected: TP201 <-> IB202
[REPORT]   step=2338  violation logged and report updated

Confirm both threads joined cleanly — no output after simulation end other than final report.

---

## 7. Observations

- All threads must be joined with `pthread_join()` before `main()` exits to avoid resource leaks.
- Mutexes and condition variables must be destroyed after joining with `pthread_mutex_destroy()`
  and `pthread_cond_destroy()`.
- The violation detector reads flight positions from shared memory written by child flight
  processes — `pos_mutex` must be acquired for both reads and writes to the same memory region.