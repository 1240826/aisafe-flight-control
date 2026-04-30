# US081 – Create a Flight Plan from a File

## Context

As a Pilot, I want to create a flight plan from a file. There may be multiple file formats, and I want to validate the file format before using it.

This user story depends on US083 (Flight DSL specification). The LPROG component provides the validation pipeline. The EAPLI component provides the controller, application service, and persistence. This README covers the LPROG responsibility only.

### List of Issues

- Analysis: #32
- Design: #32
- Implement: #32
- Test: N/A

---

## Requirements

**Acceptance criteria:**

- The flight plan file must conform to the Core Flight DSL.
- The file must be validated through lexical and syntactic analysis (Phase 1 — Sprint 2).
- Invalid files must produce meaningful error messages.
- Only valid flight plans may be imported and used by the system.

---

## Analysis

A pilot submits a `.flightplan` file. The system must:

1. Detect the file format.
2. Run lexical and syntactic validation (LPROG — `FlightPlanRunner`).
3. If valid: pass the result to the EAPLI layer.
4. If invalid: report all errors to the pilot and reject the file.

**What is NOT in scope for LPROG:**
- Weather data — handled by US082 (EAPLI).
- Aircraft assignment — handled by US080 (EAPLI).
- Load (passengers/cargo) — handled by EAPLI domain.
- Semantic validation — Phase 2, Sprint 3.

---

## Design

### Interaction with EAPLI

```
Pilot submits file
        │
        ▼
  FileFormatDetector       detects ".flightplan"
        │
        ▼
  FlightPlanRunner.run()   ← LPROG responsibility
        │
        ├── valid   → EAPLI creates FlightPlan domain object
        └── invalid → errors shown to Pilot
```

### File format detection

`FileFormatDetector` selects the correct runner by file extension. Adding a new format in a future sprint only requires adding a new branch — existing code is not modified (Open/Closed Principle).

---

## Implementation

| File | Location |
|---|---|
| `FlightPlanRunner.java` | `src/main/java/aisafe/lprog/dsl/` |
| `FlightPlanErrorListener.java` | `src/main/java/aisafe/lprog/errors/` |
| `FlightPlan.g4` | `src/main/antlr4/` |

### Integration point

```java
// In the EAPLI controller (Sprint 2 integration)
boolean valid = FlightPlanRunner.run(path, false);
if (!valid) {
    // retrieve and display errors to the pilot
}
```

---

## Observations

- Phase 2 (Sprint 3): after successful syntactic validation, semantic rules from section 3.4.5 will be applied. The visitor built in Sprint 3 will also produce an internal representation of the flight plan that the EAPLI layer can use directly to create the domain object.