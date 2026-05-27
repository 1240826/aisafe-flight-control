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
- The standard producer-consumer pattern is used: the detection thread locks the mutex, writes the violation data to a shared queue, signals the condition variable, and unlocks the mutex; the report thread loops on `pthread_cond_wait` inside a `while` check on a predicate to guard against spurious wakeups
- A dedicated violation queue (circular buffer or linked list) is used instead of a single shared variable, so that multiple violations detected in quick succession are not lost

**Decisions made by the team:**
- A single `pthread_mutex_t` and `pthread_cond_t` pair is used exclusively for the detection-to-report notification channel, keeping synchronization concerns separated from the simulation step semaphores (US108)
- The report thread does not block the detection thread — after signalling, the detection thread immediately releases the mutex and resumes monitoring
- The violation event stored in the shared structure includes: timestamp, aircraft identifiers, positions, and velocity vectors, as required by US109

### 3.1 Concurrency Model

Two threads in the parent process interact through this mechanism:

- **Safety Violation Detection Thread** (producer) — scans shared memory at each simulation step, detects proximity violations, appends to the violation queue, and signals the condition variable
- **Report Generation Thread** (consumer) — waits on the condition variable, wakes up on signal, drains the violation queue, and logs each event to the report structure

The shared violation queue and its associated mutex/condition variable are allocated in the parent process heap (not in shared memory), since both threads belong to the same process.

---

## 4. Design

### 4.1 Shared Data Structures

```c
/* Violation event recorded by the detection thread */
typedef struct {
    time_t    timestamp;
    char      flight_a[16];
    char      flight_b[16];
    double    pos_a[3];      /* x, y, altitude */
    double    pos_b[3];
    double    vel_a[3];
    double    vel_b[3];
} ViolationEvent;

/* Shared notification channel between detection and report threads */
typedef struct {
    ViolationEvent  queue[MAX_VIOLATIONS];
    int             head;
    int             tail;
    int             count;
    pthread_mutex_t mutex;
    pthread_cond_t  cond;
} ViolationChannel;
```

### 4.2 Realization

**Files to create/modify:**

| File | Responsibility |
|------|---------------|
| `violation_channel.h` | Declares `ViolationChannel`, `ViolationEvent`, and the channel API |
| `violation_channel.c` | Implements `vc_init`, `vc_push` (detection side), `vc_wait_and_drain` (report side), `vc_destroy` |
| `detection_thread.c` | Detection thread loop; calls `vc_push` on each detected violation |
| `report_thread.c` | Report thread loop; calls `vc_wait_and_drain` and logs each event |
| `simulation.c` | Initializes `ViolationChannel` and passes it to both threads at startup |

**Sequence Diagram — Condition Variable Notification:**

![Sequence Diagram — US107](diagrams/SD_US107_CondVarNotification.png)

### 4.3 Detection Thread Logic (producer)

```c
void vc_push(ViolationChannel *vc, ViolationEvent *event) {
    pthread_mutex_lock(&vc->mutex);

    if (vc->count < MAX_VIOLATIONS) {
        vc->queue[vc->tail] = *event;
        vc->tail = (vc->tail + 1) % MAX_VIOLATIONS;
        vc->count++;
    }

    pthread_cond_signal(&vc->cond);
    pthread_mutex_unlock(&vc->mutex);
}
```

### 4.4 Report Thread Logic (consumer)

```c
void vc_wait_and_drain(ViolationChannel *vc, ViolationEvent *out, int *n) {
    pthread_mutex_lock(&vc->mutex);

    /* Guard against spurious wakeups */
    while (vc->count == 0) {
        pthread_cond_wait(&vc->cond, &vc->mutex);
    }

    *n = 0;
    while (vc->count > 0) {
        out[(*n)++] = vc->queue[vc->head];
        vc->head = (vc->head + 1) % MAX_VIOLATIONS;
        vc->count--;
    }

    pthread_mutex_unlock(&vc->mutex);
}
```

### 4.5 Acceptance Tests

**AT1 — Violation is logged in real time**

Given a running simulation where two aircraft enter a proximity violation at step N,
When the detection thread detects the conflict and calls `vc_push`,
Then the report thread wakes up, drains the event, and logs it before step N+1 begins.

**AT2 — Spurious wakeup does not cause incorrect behaviour**

Given the report thread waiting on the condition variable,
When a spurious wakeup occurs with no violation in the queue (`count == 0`),
Then the report thread returns to waiting without logging any event.

**AT3 — Multiple violations in rapid succession are not lost**

Given two violations detected by the detection thread within the same simulation step,
When both are pushed to the queue before the report thread drains it,
Then the report thread logs both events correctly in FIFO order.

**AT4 — Mutex ensures thread-safe access to the queue**

Given the detection thread and report thread accessing the violation queue concurrently,
When both attempt to access the queue simultaneously,
Then the mutex guarantees that only one thread accesses the queue at a time, with no data corruption.

**AT5 — Channel is correctly cleaned up after simulation ends**

Given a simulation that has completed,
When `vc_destroy` is called on the `ViolationChannel`,
Then `pthread_mutex_destroy` and `pthread_cond_destroy` are called without errors, and no resources are leaked.

---

## 5. Implementation

**Key new/modified files:**

- `[TBD]`

*Major commits: [TBD]*

---

## 6. Integration/Demonstration

1. Start the simulation with at least two flights whose paths are known to produce a proximity violation.
2. Observe the report output file being updated in real time as violations are detected.
3. Confirm that the timestamp, aircraft identifiers, and position data in the report match the simulation step at which the violation occurred.
4. Run the simulation with no violations and confirm the report thread remains idle without logging any false events.

---

## 7. Observations

[TBD]