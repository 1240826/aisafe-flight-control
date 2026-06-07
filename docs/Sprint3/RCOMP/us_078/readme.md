# US078 — ATC Collaborator Remote Access (RCOMP)

## 1. Context

This task is assigned in Sprint 3 within the Computer Networks (RCOMP) scope.
The objective is to allow an Air Traffic Control (ATC) Collaborator to remotely access the system through a dedicated TCP-based client application. This makes all ATC Collaborator functionalities available over the network securely, without any direct database connection on the client side.

**Issue:** #61
**Assigned to:** Jaime Simões

### 1.1 List of Issues

- Analysis: #61
- Design: #61
- Implement: #61
- Test: #61

---

## 2. Requirements

**US078** As an ATC Collaborator, I want to remotely access the system in order to manage flight routes and air control areas.

### Acceptance Criteria

- **US078.1** A specific TCP-based client application must be developed to communicate with a concurrent server application embedded in the main system.
- **US078.2** The client application must interact with the system exclusively through the TCP connection — any direct interaction with the database from the client machine is strictly unacceptable.
- **US078.3** All ATC Collaborator user stories must be remotely available via this client application:
    - **US071** — Register Air Control Area (ACA)
    - **US073** — Create Flight Route
    - **US074** — Delete (Deactivate) Flight Route
- **US078.4** Authentication and authorization must be enforced over the connection before any operation is executed. Only users holding the `ATC_COLLABORATOR` role can access this service.

### Dependencies/References

- US071, US073, US074 — ATC Collaborator operations (Sprint 2/3).
- US030 — Authentication and authorization infrastructure (applied to remote logins).
- US090 — External logging of remote accesses (logs login/logout/disconnect via UDP).
- NFR09 — Authentication and authorization enforced on all functionalities.

---

## 3. Analysis

### 3.1 Network Architecture

The system follows a two-tier concurrent remote access model:

- A standalone **ATC Client App** (Java console) connects via TCP to a dedicated port exposed by the main system.
- The **TCP Server** is embedded in the main system and spawns one handler thread (`AtccClientHandler`) per accepted connection to allow concurrent access by multiple ATC Collaborators.
- All business logic (US071, US073, US074) executes strictly on the server side through existing application services.
- The server enforces that the authenticated user holds the `ATC_COLLABORATOR` role before any operation is executed.

---

### 3.2 Application-Level Protocol

A simple, lightweight text-based request/response protocol is defined for this connection.
Each message is terminated with `\n`. Fields inside a message are separated by the pipe character (`|`).

**Client to Server messages:**

| Code | Format | Description |
|------|--------|-------------|
| `AUTH` | `AUTH\|<username>\|<password>` | Authenticate the session |
| `REGISTER_ACA` | `REGISTER_ACA\|<area_code>\|<name>\|<min_lat>\|<max_lat>\|<min_lon>\|<max_lon>` | Register a new ACA (US071) |
| `CREATE_ROUTE` | `CREATE_ROUTE\|<route_name>\|<company_iata>\|<origin_iata>\|<destination_iata>` | Create a new flight route (US073) |
| `DEACTIVATE_ROUTE` | `DEACTIVATE_ROUTE\|<route_name>\|<deactivation_date>` | Deactivate an active route (US074) |
| `LIST_COMPANIES` | `LIST_COMPANIES` | List registered companies for route selection helper |
| `LIST_AIRPORTS` | `LIST_AIRPORTS` | List registered airports for route/ACA helper |
| `QUIT` | `QUIT` | Gracefully close the TCP session |

**Server to Client messages:**

| Code | Meaning |
|------|---------|
| `AUTH_OK` | Authentication successful |
| `AUTH_FAIL\|<reason>` | Authentication failed (invalid credentials, expired clearance, or insufficient roles) |
| `OK\|<optional_payload>` | Operation succeeded — returns data payload if applicable |
| `ERR\|<reason>` | Operation failed — contains detailed error/exception message |

---

### 3.3 Session Flow

1. Client establishes a TCP connection to the server on the ATCC-dedicated port.
2. Server accepts the socket and spawns a thread (`AtccClientHandler`) to handle the client.
3. Client sends `AUTH|<username>|<password>`.
4. Server validates credentials via EAPLI `AuthzRegistry`.
5. Server checks `securityClearanceExpiryDate` (US030.4).
6. Server verifies the user holds the `ATC_COLLABORATOR` role.
7. If valid: Server responds `AUTH_OK`. The session is now authenticated.
8. If invalid: Server responds `AUTH_FAIL|<reason>` (connection remains open for retry or QUIT).
9. Server emits a UDP log event to the Logging Server (US90) for both outcomes.
10. Any request other than `AUTH` or `QUIT` received before successful authentication is answered with `ERR|NOT_AUTHENTICATED`.
11. After `QUIT` or client disconnect, the server closes the sockets, dispatches a UDP log event, and cleans up.

---

## 4. Design

### 4.1 Realization

The system decomposes into clean network-layer classes and reuses EAPLI application controllers:

**Key classes to create (RCOMP network layer):**

| Class | Responsibility |
|-------|---------------|
| `TcpAtccServer` (extends `AbstractTcpServer`) | Listens on the ATC-dedicated TCP port; accepts connections and spawns handlers |
| `AtccClientHandler` (implements `Runnable`) | Handles one socket session: reads requests, parses commands, executes, writes responses |
| `AtccRemoteSessionState` | Tracks authentication state and the authenticated ATC Collaborator |
| `AtccClientApp` | Standalone console application for the remote client |

**Key classes to reuse (EAPLI business layer):**

| Class | Responsibility |
|-------|---------------|
| `RegisterAirControlAreaController` | Core controller for US071 |
| `CreateFlightRouteController` | Core controller for US073 |
| `DeleteFlightRouteController` | Core controller for US074 |

### 4.2 Diagrams

The sequence diagram for the remote operations is defined in `sd_us078_atcc_remote_access.puml` (which shows connection, authentication, and execution of `createRoute(...)` on the server-side controllers).

---

## 5. Implementation

**Key Files:**
- `TcpAtccServer.java` — Server daemon listening on port
- `AtccClientHandler.java` — Concurrent connection thread worker
- `AtccClientApp.java` — Standalone client terminal console UI

---

## 6. Integration/Demonstration

1. Start the main database and backend application (initializing `TcpAtccServer` thread).
2. Start the UDP logging server (US090).
3. Run `AtccClientApp` on a network node.
4. Attempt executing an action without authentication → verifies `ERR|NOT_AUTHENTICATED` is returned.
5. Log in with an invalid role or invalid credentials → verifies `AUTH_FAIL` is returned.
6. Log in with an `ATC_COLLABORATOR` account → verifies `AUTH_OK`.
7. Register an ACA, create a route, and deactivate it remotely → verifies `OK` responses and checks DB updates on the server.
8. Verify UDP server logged all session actions.

---

## 7. Observations

- Reuses existing EAPLI controllers without changing any underlying business logic.
- Network transactions are decoupled from the DB (the client never sees database drivers, passwords, or connection pools).
- Follows the exact protocol pattern as US044 (Weather) and US086 (Pilot), securing a standard approach to socket networking in the project.