# US103 – Synchronize Flight Execution with a Time Step

## 1. Context

As a simulation engine, I want to synchronize aircraft movements based on time steps so that I can accurately simulate real-world execution.

**Assigned to:** Fábio Costa

### List of Issues

- Analysis: #50
- Design: #50
- Implement: #50
- Test: #50

---

## 2. Requirements

- The simulation must progress step by step.
- Each flight process must send position updates at defined intervals.
- **The main process must ensure all updates for a given time step are processed before advancing to the next step.**

## 3. Analysis

### The problem

Without synchronisation a fast child process could compute steps 0, 1, 2, 3 while a slow child is still on step 0. Comparing positions from different time points in the violation detector would be meaningless — like comparing a flight's position now with another's position 3 seconds ago.

### The solution — two-phase pipe barrier

Phase 1 and Phase 2 together form a complete barrier using only blocking pipe I/O. No semaphores or mutexes needed.

**Phase 1 — `send_go(step, safe[], alt_adj[])`:**  
Parent writes `GoToken{step, safe, alt_adjust}` to each active child's go pipe. Children are blocked in `read(gfd, &tok, sizeof(GoToken))` and unblock when the token arrives.

The `GoToken` carries:
- `step` — which step the child is allowed to execute
- `safe` — 1 = compute next position; 0 = hold (violation detected)
- `alt_adjust` — altitude correction ordered by controller (US102)

**Phase 2 — `collect(step)`:**  
Parent calls `read(rfd, &upd, sizeof(PosUpdate))` for each active child — **blocking** until each child writes its update. The parent **cannot advance to step+1** until the loop completes and all children have reported. This is the barrier guarantee.

### Complete protocol per step t

```
Parent                                  Child i
─────────────────────────────────────────────────────────────
send_go(t, safe, alt_adj):
  write(go_pipe[i][1], &tok{t, safe[i], alt_adj[i]})
                              ───────────► read(gfd, &tok)  ← unblocks
                                            pos.alt += tok.alt_adjust
                                            if tok.safe: advance()
                                            write(rfd, &upd)
collect(t):
  read(report_pipe[i][0], &upd)  ◄───────────────────────────
  [BLOCKS until upd arrives]
  store_history()
  detect area transition (ENTERING/LEAVING)
  update last_pos, last_vel, in_area
detect_violations(t)  →  update safe[], alt_adj[]
step = t + 1  →  repeat
```

### Professor requirement: "SE não houver colisão"

Professor notes: *"pai diz aos filhos para calcular a próxima posição se não houver colisão"*.

When `detect_violations()` finds a breach, it sets `safe[i] = 0`. The child receives `GoToken{safe=0}` and skips the physics step — sending back its current position unchanged (holding).

### Atomicity

```
sizeof(GoToken)   =  20 bytes
sizeof(PosUpdate) =  ~80 bytes
PIPE_BUF          >= 512 bytes  (POSIX minimum)
```

Both writes are below `PIPE_BUF` — each `write()` is atomic.

### Area transitions

Detected in `collect()` because this is where the parent first reads the new position:

```c
was_in = flights[i].in_area;
now_in = in_area_full(upd.pos);  /* checks lat, lon, AND altitude */
if (!was_in && now_in) printf("ENTERING...");
if ( was_in && !now_in) printf("LEAVING...");
```

`in_area_full()` adds the altitude check (`0 ≤ alt ≤ AREA_MAX_ALT_M`) missing from the original `in_area()`.

### Termination handshake

When the simulation ends, `main()` closes `go_pipe[i][1]`. The child's `read(gfd, ...)` returns 0 (EOF):

```c
n = read(gfd, &tok, sizeof(GoToken));
if (n <= 0) break;   /* pipe closed = simulation over */
```

### LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — Barrier synchronisation using pipes in C

> "We are implementing a step-by-step flight simulation in C with one child process per flight.
> The parent must ensure all children report their position for step t before advancing to step t+1.
> How can we implement a barrier using only blocking pipe I/O, without semaphores or mutexes?"

**LLM suggestions adopted:**
- Two-phase barrier: parent writes a `GoToken` to each child (phase 1), then blocks on `read()`
  from each child before advancing (phase 2)
- Termination via `close(go_pipe[i][1])` — child's `read()` returns 0 (EOF) and exits cleanly

**Decisions made by the team / deviations from LLM output:**
- The LLM initially suggested a shared memory counter as the barrier — replaced with the pipe
  barrier since pipes are already in use and no additional IPC mechanism is needed
- `GoToken` extended to carry `safe` and `alt_adjust` alongside `step`, combining synchronisation
  and control in a single atomic write (below `PIPE_BUF`)


## 4. Implementation

**File:** `us103_sync.c` — `send_go()`, `collect()`  
**Child barrier point:** `us101_flight.c` — `read(gfd, &tok, sizeof(GoToken))`

## 5. Integration / Demonstration

Verify barrier: all flights report step 0 before any reports step 60:

```bash
./simulation ../test/scenario2_normal.json 2>&1 | grep "step= *0\|step= *60" | head -8
```

Expected:
```
[CTRL]   step=    0  TP001  ...  CLIMB  IN
[CTRL]   step=    0  IB002  ...  CLIMB  IN
[CTRL]   step=    0  VY003  ...  CLIMB  IN
[CTRL]   step=   60  TP001  ...  CLIMB  IN
```

## 6. Observations

- One go pipe per flight allows the parent to selectively hold individual flights with `safe=0` without affecting others.
- `SA_RESTART` on the child's SIGUSR1 handler ensures a signal arriving during `read(gfd,...)` does not fail with EINTR — the read restarts automatically after the handler.
- The `alt_adjust` in `GoToken` is reset to 0 by `main()` immediately after `send_go()` — it is a one-shot order.
