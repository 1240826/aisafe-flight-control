# US44 — Weather Person Remote Access (RCOMP)

## 1. Context

This task is assigned in Sprint 3. It is the first time this feature is being developed.
The objective is to allow a Weather Person (WP) to remotely access the system through a
dedicated TCP-based client application, making all Weather Person functionality available
over the network without any direct database interaction from the client side.

**Issue:** #60
**Assigned to:** Dinis Silva

### 1.1 List of Issues

*(to be filled)*

---

## 2. Requirements

**US44** As a Weather Person, I want to remotely access the system in order to upload weather data.

### Acceptance Criteria

- **US44.1** A specific TCP-based client application must be developed to communicate with a server application embedded in the main system.
- **US44.2** The client application must interact with the system exclusively through the TCP connection — any direct interaction with the database is strictly unacceptable.
- **US44.3** All Weather Person user stories must be remotely available via this client application:
  - **US041** — Register weather data
  - **US042** — Import bulk weather data
  - **US043** — Consult weather data
- **US44.4** Authentication and authorization must be enforced before any operation is executed.

### Dependencies/References

- US041, US042, US043 — Weather Person operations implemented in Sprint 2/3
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
- The server validates after authentication that the authenticated user actually holds the `WEATHER_PERSON` role, rejecting any user who authenticates with a different role

**Decisions made by the team:**
- The Weather Client App is a standalone console application, separate from the backoffice app and from the Air Transport Company App
- One TCP connection = one session; the session is tied to a specific Weather Person
- Security clearance expiry check (US030.4) applies to remote login as it does locally
- US041–US043 are already implemented in EAPLI and will be reused server-side without modification

---

### 3.1 Network Architecture

The system follows a two-tier remote access model:

- A standalone **Weather Client App** (Java console) connects via TCP to a dedicated port exposed by the main system
- The **TCP Server** is embedded in the main system and spawns one handler thread per accepted connection
- All business logic (US041–US043) executes on the server side through existing application services
- The client sends text requests and receives text responses — it never touches the database
- The server enforces that the authenticated user holds the `WEATHER_PERSON` role before any operation is dispatched

---

### 3.2 Application-Level Protocol

A simple text-based request/response protocol is defined for this connection.
Each message is terminated with `\n`. Fields are separated by `|`.

**Client to Server messages:**

| Code | Format | Description |
|------|--------|-------------|
| AUTH | `AUTH\|<username>\|<password>` | Authenticate the session |
| REGISTER_WEATHER | `REGISTER_WEATHER\|<area_code>\|<date>\|<temperature>\|<wind_speed>\|<wind_direction>\|<precipitation>` | Register weather data for an air control area (US041) |
| IMPORT_WEATHER | `IMPORT_WEATHER\|<area_code>\|<csv_payload>` | Import bulk weather data (US042) |
| CONSULT_WEATHER | `CONSULT_WEATHER\|<area_code>\|<date>` | Consult weather data for a given area and date (US043) |
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

1. Client establishes TCP connection to the server on the Weather Person-dedicated port
2. Server accepts and spawns a handler thread for that connection
3. Client sends `AUTH|<username>|<password>`
4. Server validates credentials via EAPLI `AuthzRegistry`
5. Server checks `securityClearanceExpiryDate` (US030.4)
6. Server verifies the authenticated user holds the `WEATHER_PERSON` role
7. If all valid: server responds `AUTH_OK` and session is bound to that Weather Person
8. If invalid: server responds `AUTH_FAIL|<reason>` — connection stays open for retry or QUIT
9. Server emits a UDP log event to the Remote Accesses Logging Server (US90) for both outcomes
10. Any request other than AUTH or QUIT received before authentication is answered with `ERR|NOT_AUTHENTICATED`
11. After QUIT or disconnect, the server closes the socket and cleans up the session

---

### 3.4 Identified Domain Concepts

| Concept | Responsibility |
|---------|---------------|
| `TcpWeatherServer` | Listens on the Weather Person-dedicated TCP port; accepts connections and spawns one handler thread per client |
| `WeatherClientHandler` | Manages one client session: reads requests, dispatches to services, writes responses |
| `WeatherRemoteSessionState` | Tracks authentication state and the authenticated Weather Person |
| `WeatherProtocolParser` | Parses raw text messages into structured request objects |
| `RemoteWeatherService` | Delegates to existing US041–US043 application services; no direct DB access |

---

### 3.5 Acceptance Tests

**AT1 — Unauthenticated request is rejected (US44.4)**

Given a client connected to the server but not yet authenticated,
When the client sends a CONSULT_WEATHER request,
Then the server responds with `ERR|NOT_AUTHENTICATED` and does not execute the operation.

**AT2 — Authentication with wrong role is denied (US44.4)**

Given a user with valid credentials and valid security clearance but holding the `ATC_COLLABORATOR` role,
When the client sends AUTH with those credentials on the Weather Person server port,
Then the server responds with `AUTH_FAIL|INSUFFICIENT_ROLE` and the session remains unauthenticated.

**AT3 — Authentication with expired security clearance is denied (US44.4 + US030.4)**

Given a Weather Person whose `securityClearanceExpiryDate` is in the past,
When the client sends AUTH with valid credentials,
Then the server responds with `AUTH_FAIL|SECURITY_CLEARANCE_EXPIRED` and the session remains unauthenticated.

**AT4 — Successful authentication grants access to all Weather Person operations (US44.3)**

Given a Weather Person with valid credentials, valid security clearance and the `WEATHER_PERSON` role,
When the client authenticates successfully,
Then the client can execute US041, US042 and US043 operations, each receiving an `OK` or domain-level `ERR` response.

**AT5 — No direct database access from client (US44.2)**

Given a running authenticated client session,
When any operation is performed,
Then all data access occurs exclusively on the server side through the application services; the client sends and receives only text protocol messages over the TCP connection.

**AT6 — Bulk weather data import succeeds for valid CSV payload (US042)**

Given an authenticated Weather Person,
When the client sends an IMPORT_WEATHER request with a valid CSV payload for an existing air control area,
Then the server responds with `OK` and the weather records are persisted server-side.

**AT7 — Consult weather data returns correct results (US043)**

Given an authenticated Weather Person,
When the client sends a CONSULT_WEATHER request with a valid area code and date for which data exists,
Then the server responds with `OK|<weather_data>` containing the relevant weather records for that area and date.