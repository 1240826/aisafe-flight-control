# US086 — Pilot Remote Access (EAPLI side)

## 1. Context

This task is assigned in Sprint 3. It is the first time this feature is being developed.
The objective is to expose all Pilot/Flight Control Operator (FCO) user stories through a
TCP-based remote access layer, allowing a Pilot to interact with the system from a remote
client application. This document covers the EAPLI side: exposing existing controllers via
a `RemotePilotService` that the RCOMP TCP handler invokes.

**Issue:** #62
**Assigned to:** Fábio Costa (both EAPLI and RCOMP sides)

### 1.1 List of Issues

- Analysis: #62
- Design: #62
- Implement: #62
- Test: #62

---

## 2. Requirements

**US086** As a Pilot (Flight Control Operator), I want to remotely access the system in order to manage flight plans and view reports.

### Acceptance Criteria

- **US086.1** A `RemotePilotService` must wrap existing FCO controllers so they can be invoked from a TCP handler without UI dependency.
- **US086.2** Access to every operation must require prior authentication and authorization (via EAPLI `AuthzRegistry`).
- **US086.3** The following FCO operations must be remotely accessible:
  - US072 — List company fleet
  - US080 — Create a flight plan
  - US085 — Test/validate a flight plan
  - US111 — Generate a simulation report
  - US112 — Monthly report generation
  - US121 — Create a valid flight plan from a file
- **US086.4** No client code may interact directly with the database — all access must go through the application layer.

### Dependencies/References

- US072, US080, US085, US111, US112, US121 — existing FCO controllers
- US030 — Authentication and authorization infrastructure
- NFR09 — Authentication and authorization enforced on all functionalities
- RCOMP US86 — TCP server and client implementation

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis of this user story.

**Prompt 1:** "In a Java DDD application with existing controllers, how do I create a service layer that wraps controllers so a TCP handler can invoke them without depending on the UI?"

**LLM suggestions adopted:**
- `RemotePilotService` is a facade that depends only on existing application service interfaces (not controllers directly)
- Each remote operation maps to exactly one method in the facade
- Authorization is checked at the facade level before delegating to the underlying service

**Decisions made by the team:**
- The `RemotePilotService` does not duplicate authorization — it calls the same controller methods that already enforce authorization
- Session state (authenticated user) is managed by the RCOMP `PilotClientHandler`, not by the EAPLI layer
- Output is returned as plain strings (serialized by the caller) — the service layer returns domain objects or simple DTOs

---

### 3.1 Architecture

```
PilotClientHandler (RCOMP)
    ↓ calls
RemotePilotService (EAPLI facade)
    ↓ delegates to
Existing Application Services / Controllers
    ↓
Domain / Persistence
```

- `RemotePilotService` is instantiated per-session (one instance per TCP connection)
- It receives the authenticated `SystemUser` on construction so that all delegated calls can be traced back to the authenticated user
- All methods throw typed exceptions (`UnauthorizedException`, `BusinessRuleException`) that the RCOMP handler maps to `ERR|<reason>` responses

---

### 3.2 RemotePilotService Interface

```java
public class RemotePilotService {

    public RemotePilotService(SystemUser authenticatedUser);

    public List<AircraftDTO> listFleet();                                  // US072
    public FlightPlan createFlightPlan(FlightId flightId, String dsl);     // US080
    public ValidationResult validateFlightPlan(FlightPlanId id);           // US085
    public SimulationReport generateReport(FlightId flightId);             // US111
    public MonthlyReport monthlyReport(int year, int month);               // US112
    public FlightPlan importFlightPlan(String filePath);                   // US121
}
```

---

### 3.3 Identified Domain Concepts

| Concept | Responsibility |
|---------|---------------|
| `RemotePilotService` | Facade exposing FCO operations for remote invocation |
| `AircraftDTO` | Lightweight DTO for fleet listing (avoids serializing full aggregates) |
| `ValidationResult` | Result object containing pass/fail and reason |

---

### 3.4 Acceptance Tests

**AT1 — List fleet requires authenticated Pilot (US086.2)**

Given a non-authenticated invocation of `RemotePilotService.listFleet()`,
When the method is called,
Then an `UnauthorizedException` is thrown.

**AT2 — Valid operations return correct results (US086.3)**

Given an authenticated Pilot and an existing flight with a draft flight plan,
When `validateFlightPlan` is called with the flight plan ID,
Then a `ValidationResult` with the correct pass/fail status is returned.

**AT3 — No direct DB access from client code (US086.4)**

Given the `RemotePilotService` implements any operation,
When the operation executes,
Then all persistence access occurs only through the application/repository layer — never directly in the service.

---

## 4. Design

### 4.1 Realization

The sequence diagram for the full remote access flow is in the RCOMP US86 documentation:
`docs/Sprint3/RCOMP/us_086/sd_us086_pilot_remote_access.puml`

---

## 5. Implementation

**Key new files:**

| File | Responsibility |
|------|---------------|
| `eapli.aisafe.app.remote.pilot.RemotePilotService` | Facade wrapping existing FCO controllers |
| `eapli.aisafe.app.remote.pilot.AircraftDTO` | DTO for fleet listing |

*No existing controller is modified — only a new facade is created.*

---

## 6. Integration/Demonstration

1. Start the main application (TCP server embedded).
2. Launch the Pilot Client App (standalone Java console).
3. Connect to the server IP and Pilot-dedicated port.
4. Authenticate as `fco1` / `Password1`.
5. Execute LIST_FLEET, CREATE_FLIGHT_PLAN, VALIDATE_FLIGHT_PLAN operations.
6. All responses are received over TCP — the client never touches the database.

---

## 7. Observations

- `RemotePilotService` depends only on existing application service interfaces — no UI or infrastructure coupling.
- The RCOMP handler is responsible for session management (authentication state, socket lifecycle).
- DTOs are kept minimal to reduce serialization overhead over the TCP connection.
