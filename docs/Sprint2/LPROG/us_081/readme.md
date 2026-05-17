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

  
Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

### LLM Assistance

#### Prompt 1 — Grammar structure for the Flight Plan DSL

> "We are building a flight plan DSL using ANTLR4 in Java. The file format is `.flightplan`. We
> need lexical and syntactic validation only (Phase 1). Invalid files must produce meaningful error
> messages. Suggest a grammar structure and an error listener approach."

**LLM suggestions adopted:**
- Using a custom `FlightPlanErrorListener` extending ANTLR's `BaseErrorListener` to collect all
  errors instead of stopping at the first one
- `FlightPlanRunner.run(path, verbose)` as the single integration point called by the EAPLI layer

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested including semantic validation in the same phase — kept strictly out of scope
  for Sprint 2; semantic rules are Phase 2 (Sprint 3)
- The LLM suggested throwing exceptions on invalid input — replaced with a boolean return and
  error collection, so all errors are reported to the pilot at once


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