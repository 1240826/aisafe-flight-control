# US78 — Air Transport Company Collaborator Remote Access (RCOMP)

## 1. Context

This task is assigned in Sprint 3. It is the first time this feature is being developed.
The objective is to allow an Air Transport Company Collaborator (ATCC) to remotely access
the system through a dedicated TCP-based client application, making all ATCC functionality
available over the network without any direct database interaction from the client side.

**Assigned to:** Fábio Costa

### 1.1 List of Issues

*(to be filled)*

---

## 2. Requirements

**US78** As an Air Transport Company Collaborator, I want to remotely access the system using the Air Transport Company App.

### Acceptance Criteria

- **US78.1** A specific TCP-based client application must be developed to communicate with a server application embedded in the main system.
- **US78.2** The client application must interact with the system exclusively through the TCP connection — any direct interaction with the database is strictly unacceptable.
- **US78.3** All Air Transport Company Collaborator user stories must be remotely available via this client application:
    - **US070** — Add aircraft to air transport company
    - **US071** — Decommission aircraft
    - **US072** — List company fleet
    - **US073** — Create a flight route
    - **US074** — Delete (deactivate) a flight route
    - **US075** — Add a pilot
    - **US076** — List pilot roster
    - **US077** — Remove a pilot
- **US78.4** Authentication and authorization must be enforced before any operation is executed.

### Dependencies/References

- US070, US071, US072 — ATCC operations implemented in Sprint 2
- US073, US074, US075, US076, US077 — ATCC operations implemented in Sprint 3
- US030 — Authentication and authorization infrastructure (Sprint 2)
- US90 — External logging of remote accesses (logs every login/logout/disconnect via UDP)
- NFR09 — Authentication and authorization enforced on all functionalities

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis of this user story.

**Prompt 1:** "In a Java TCP client-server system, if multiple remote client types exist (Weather Person, Air Transport Collaborator, Pilot), should each have its own TCP server on a different port, or should a single server handle all client types and distinguish them after authentication?"

**LLM suggestions adopted:**
- Each client type gets its own dedicated TCP server running on a distinct port — this keeps handler logic simple, role enforcement straightforward, and avoids a single server becoming a routing bottleneck
- Authentication still reuses the shared EAPLI `AuthzRegistry` on the server side regardless of which server the client connects to
- The server validates after authentication that the authenticated user actually holds the `ATC_COLLABORATOR` role, rejecting any user who authenticates with a different role

**Decisions made by the team:**
- The Air Transport Company App is a standalone console application, separate from the backoffice app and from the Weather Client App
- One TCP connection = one session; the session is tied to a specific company collaborator — operations are scoped to that collaborator's company
- Security clearance expiry check (US030.4) applies to remote login as it does locally
- US070–US072 are already implemented in EAPLI (Sprint 2) and will be reused server-side without modification

---

### 3.1 Network Architecture

The system follows a two-tier remote access model:

- A standalone **Air Transport Company App** (Java console) connects via TCP to a dedicated port exposed by the main system
- The **TCP Server** is embedded in the main system and spawns one handler thread per accepted connection
- All business logic (US070–US077) executes on the server side through existing application services
- The client sends text requests and receives text responses — it never touches the database
- The server enforces that the authenticated user holds the `ATC_COLLABORATOR` role before any operation is dispatched

---

### 3.2 Application-Level Protocol

A simple text-based request/response protocol is defined for this connection.
Each message is terminated with `\n`. Fields are separated by `|`.

**Client to Server messages:**

| Code | Format | Description |
|------|--------|-------------|
| AUTH | `AUTH\|<username>\|<password>` | Authenticate the session |
| ADD_AIRCRAFT | `ADD_AIRCRAFT\|<registration>\|<model_id>\|<seats_config>` | Add aircraft to fleet (US070) |
| DECOMMISSION | `DECOMMISSION\|<registration>` | Decommission an aircraft (US071) |
| LIST_FLEET | `LIST_FLEET` | List company fleet (US072) |
| CREATE_ROUTE | `CREATE_ROUTE\|<name>\|<origin_code>\|<destination_code>` | Create a flight route (US073) |
| DELETE_ROUTE | `DELETE_ROUTE\|<route_name>\|<date_from>` | Deactivate a flight route (US074) |
| ADD_PILOT | `ADD_PILOT\|<username>\|<certified_models>` | Add a pilot to the company (US075) |
| LIST_PILOTS | `LIST_PILOTS` | List pilot roster (US076) |
| REMOVE_PILOT | `REMOVE_PILOT\|<username>` | Make a pilot inactive (US077) |
| QUIT | `QUIT` | Gracefully close the session |

**Server to Client messages:**

| Code | Meaning |
|------|---------|
| `OK\|<optional_data>` | Operation succeeded |
| `ERR\|<reason>` | Operation failed — reason included |
| `AUTH_OK` | Authentication successful |
| `AUTH_FAIL\|<reason>` | Authentication failed (wrong credentials, expired clearance, or wrong role) |

---

### 3.3 Session Flow

1. Client establishes TCP connection to the server on the ATCC-dedicated port
2. Server accepts and spawns a handler thread for that connection
3. Client sends `AUTH|<username>|<password>`
4. Server validates credentials via EAPLI `AuthzRegistry`
5. Server checks `securityClearanceExpiryDate` (US030.4)
6. Server verifies the authenticated user holds the `ATC_COLLABORATOR` role
7. If all valid: server responds `AUTH_OK` and session is bound to that collaborator's company
8. If invalid: server responds `AUTH_FAIL|<reason>` — connection stays open for retry or QUIT
9. Server emits a UDP log event to the Remote Accesses Logging Server (US90) for both outcomes
10. Any request other than AUTH or QUIT received before authentication is answered with `ERR|NOT_AUTHENTICATED`
11. After QUIT or disconnect, the server closes the socket and cleans up the session

---

### 3.4 Identified Domain Concepts

| Concept | Responsibility |
|---------|---------------|
| `TcpAtccServer` | Listens on the ATCC-dedicated TCP port; accepts connections and spawns one handler thread per client |
| `AtccClientHandler` | Manages one client session: reads requests, dispatches to services, writes responses |
| `AtccRemoteSessionState` | Tracks authentication state, authenticated user and their associated company |
| `AtccProtocolParser` | Parses raw text messages into structured request objects |
| `RemoteAtccService` | Delegates to existing US070–US077 application services; no direct DB access |

---

### 3.5 Acceptance Tests

**AT1 — Unauthenticated request is rejected (US78.4)**

Given a client connected to the server but not yet authenticated,
When the client sends a LIST_FLEET request,
Then the server responds with `ERR|NOT_AUTHENTICATED` and does not execute the operation.

**AT2 — Authentication with wrong role is denied (US78.4)**

Given a user with valid credentials and valid security clearance but holding the WEATHER_PERSON role,
When the client sends AUTH with those credentials on the ATCC server port,
Then the server responds with `AUTH_FAIL|INSUFFICIENT_ROLE` and the session remains unauthenticated.

**AT3 — Authentication with expired security clearance is denied (US78.4 + US030.4)**

Given an ATCC whose securityClearanceExpiryDate is in the past,
When the client sends AUTH with valid credentials,
Then the server responds with `AUTH_FAIL|SECURITY_CLEARANCE_EXPIRED` and the session remains unauthenticated.

**AT4 — Successful authentication grants access to all ATCC operations (US78.3)**

Given an ATCC with valid credentials, valid security clearance and the ATC_COLLABORATOR role,
When the client authenticates successfully,
Then the client can execute US070, US071, US072, US073, US074, US075, US076 and US077 operations, each receiving an OK or domain-level ERR response.

**AT5 — No direct database access from client (US78.2)**

Given a running authenticated client session,
When any operation is performed,
Then all data access occurs exclusively on the server side through the application services; the client sends and receives only text protocol messages over the TCP connection.