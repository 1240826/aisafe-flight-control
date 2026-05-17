# US102 – Detect Aircraft Safety Violations in Real Time

## 1. Context

As a simulation system, I want to continuously monitor aircraft positions for overlaps so that I can identify and report safety violations.

**Assigned to:** Dinis Silva

### List of Issues

- Analysis: #49
- Design: #49
- Implement: #49
- Test: #49

---

## 2. Requirements

- Detect when two or more aircraft **may eventually** violate safety rules.
- Log the event and notify both aircraft via signals.
- Each flight process handles SIGUSR1 and blocks other signals while doing so.
- Early termination when violations exceed threshold (SIGTERM).
- Flight processes handle termination and perform cleanup.

## 3. Analysis

### Safety cylinder — ICAO official values

| Dimension | Value | Source |
|-----------|-------|--------|
| Horizontal | **5 NM = 9 260 m** | ICAO Doc 4444 (PANS-ATM), Chapter 8 |
| Vertical | **305 m (≈ 1 000 ft)** | ICAO RVSM airspace standard |

These are the ICAO standard values for radar en-route separation, as confirmed by the professor ("usar os oficiais"). Defined as `#define` in `common.h` so they can be changed without modifying any other file.

### Real-time detection

After every time step, `detect_violations()` checks all pairs of active, in-area aircraft:

```
for each pair (i, j):
    h = horizontal_distance(pos_i, pos_j)
    v = |alt_i - alt_j|
    if h < 9260 AND v < 305: BREACH
```

### Predictive detection — "may eventually violate"

US102 acceptance criterion: *"The system must detect when two or more aircraft **may eventually** violate safety rules."*

`predict_collision()` extrapolates both aircraft positions `LOOKAHEAD_S = 30` seconds ahead using current velocity vectors:

```c
pa_future.lat = last_pos.lat + R2D(vel.vy * 30 / EARTH_R);
pa_future.lon = last_pos.lon + R2D(vel.vx * 30 / (EARTH_R * cos(lat)));
pa_future.alt = last_pos.alt + vel.vz * 30;
```

If the predicted positions breach the cylinder, a `[PRED]` event is logged and corrective action is taken **before** the actual collision — giving the controller 30 seconds to react.

### Route adjustment — "mudar a rota"

When a breach or prediction is detected, the controller orders the **lower aircraft** to climb `ALT_ADJUST_M = 400 m`. This restores vertical separation well above `SAFETY_V_M = 305 m`.

The adjustment is sent via `GoToken.alt_adjust` to the specific child. The child applies it immediately before the next physics step.

### Position history (US101)

`store_history()` maintains a circular buffer of 600 snapshots per flight. This enables trajectory analysis for predictive detection and is shown in the final report.

### SIGUSR1 handler

Follows the safe pattern from Signals Part II lectures:
- `sigfillset(&act.sa_mask)` — blocks ALL signals during handler
- Only sets `volatile sig_atomic_t got_violation = 1`
- Uses `write()` not `printf()`

### LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — Safety cylinder and predictive detection

> "We are implementing real-time collision detection in C for a flight simulation. Aircraft positions
> are updated every second. We need to detect current breaches and predict future ones. What are the
> standard ICAO separation values and how should predictive detection work?"

**LLM suggestions adopted:**
- ICAO standard values: 5 NM horizontal (9 260 m) and 305 m vertical, defined as `#define` in
  `common.h`
- Linear extrapolation `LOOKAHEAD_S = 30` seconds ahead using current velocity vectors for
  predictive detection

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested notifying all active aircraft on any violation — narrowed to only the two
  involved aircraft (`kill(pid_i, SIGUSR1)`, `kill(pid_j, SIGUSR1)`)
- Route adjustment applies only to the lower aircraft (`ALT_ADJUST_M = 400 m`), not both — the
  LLM initially suggested adjusting both.

  
## 4. Design

```
detect_violations()
  reset safe[]=1, alt_adj[]=0
  for each in-area active pair (i,j):
    current_breach = safety_breach(pos_i, pos_j)
    predicted_breach = predict_collision(i, j, +30s)
    if either:
      log Violation (with resolution info)
      kill(pid_i, SIGUSR1)
      kill(pid_j, SIGUSR1)
      lower = min(alt_i, alt_j)
      alt_adj[lower] = +400m
      safe[i] = safe[j] = 0

terminate_all()
  for each active flight: kill(pid, SIGTERM), killed=1
```

## 5. Implementation

**File:** `us102_detector.c` — `detect_violations()`, `terminate_all()`, `store_history()`, `predict_collision()`  
**Handlers:** `us101_flight.c` — `handle_usr1()`, `handle_term()`, `setup_signals()`

## 6. Integration / Demonstration

```bash
./simulation ../test/scenario3_violations.json
```

Expected output:
```
[PRED]   step=2338  PREDICTED  TP201 <-> IB202  H_pred=9256m  V_pred=0m  (in 30s)
[CTRL]   Ordering TP201 to climb 400m to restore separation
[FLIGHT] SIGUSR1 - violation detected
[FLIGHT TP201] step=2340  VIOLATION at (41.5000, -3.7068, 9600m)
```

## 7. Observations

- Standard signals are not queued: if two SIGUSR1 arrive during the handler, only one is delivered after it returns. Acceptable — the violation is already logged by the parent.
- The altitude adjustment is applied once (`alt_adjust` is reset to 0 after `send_go()`). Further adjustments require new violations to be detected.
- `LOOKAHEAD_S = 30` is the same as TIMESTEP_S × 30 — the prediction window can be adjusted via this define.
