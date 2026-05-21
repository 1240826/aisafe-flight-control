# US91 — Remote Accesses Logging Visualization (RCOMP)

## 1. Context

This task is assigned in Sprint 3. It is the first time this feature is being developed.
The objective is to provide the Administrator with a web-based visualization of all remote
access events stored by the Remote Accesses Logging Server, keeping the displayed data
continuously updated without requiring the user to reload the page.

**Assigned to:** Cláudio Pinto

### 1.1 List of Issues

*(to be filled)*

---

## 2. Requirements

**US91** As Administrator, I want to view the logs stored at the Remote Accesses Logging Server.

### Acceptance Criteria

- **US91.1** The Remote Accesses Logging Server application must include an embedded HTTP server that serves status pages to clients running a standard web browser.
- **US91.2** At least two pages must be available:
    - A page presenting the list of the last recorded remote access events.
    - A page presenting the list of remote users currently active (connected).
- **US91.3** Both pages must be kept updated to reflect the current state of the system without requiring the user to reload the page manually.
- **US91.4** AJAX must be used to update the presented data dynamically.

### Dependencies/References

- US90 — External logging of remote accesses; provides the event data that this US visualizes
- US44, US78, US86 — The three remote services whose events are logged and displayed

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis of this user story.

**Prompt 1:** "What is the simplest way to embed an HTTP server in a Java application without external frameworks, capable of serving static HTML and responding to AJAX requests with JSON data?"

**LLM suggestions adopted:**
- Use `com.sun.net.httpserver.HttpServer` (built into the JDK) to avoid introducing external dependencies — sufficient for the scope of this use case
- Serve two static HTML pages and expose two JSON endpoints that the pages poll periodically via AJAX (`setInterval`)
- The JSON endpoints read directly from the in-memory data structures maintained by US90's UDP receiver, ensuring no additional persistence layer is needed for the visualization

**Decisions made by the team:**
- AJAX polling interval set to 5 seconds — simple to implement, sufficient for near-real-time updates without the complexity of WebSockets
- The HTTP server runs on the same process as the UDP logging server (US90), sharing the same in-memory event store
- Active users page derives its data from the event log: a user is considered active if their last recorded event is a successful login with no subsequent logout or disconnect

---

### 3.1 System Architecture

The Remote Accesses Logging Server is a standalone application that combines three responsibilities:

- **UDP Receiver** (US90): listens for UDP datagrams from the TCP servers and stores events in memory
- **HTTP Server** (US91): serves HTML pages and JSON data endpoints to any web browser
- **In-Memory Event Store**: shared data structure between the UDP receiver and the HTTP server

Both the UDP receiver and the HTTP server run as concurrent threads within the same process, sharing access to the event store through proper synchronization.

---

### 3.2 HTTP Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Redirects to `/events` |
| GET | `/events` | Serves the Last Events HTML page |
| GET | `/active` | Serves the Active Users HTML page |
| GET | `/api/events` | Returns the last recorded events as JSON (consumed by AJAX) |
| GET | `/api/active` | Returns the currently active users as JSON (consumed by AJAX) |

---

### 3.3 JSON Response Format

**GET /api/events** — last recorded events:

```json
[
  {
    "timestamp": "2026-06-10T14:32:01",
    "username": "weather.person@aisafe.com",
    "clientIp": "192.168.1.45",
    "clientPort": 54321,
    "service": "US44",
    "eventType": "LOGIN_SUCCESS"
  }
]
```

**GET /api/active** — currently active users:

```json
[
  {
    "username": "atcc.user@airline.com",
    "clientIp": "192.168.1.72",
    "clientPort": 61234,
    "service": "US78",
    "connectedSince": "2026-06-10T14:20:00"
  }
]
```

---

### 3.4 Page Descriptions

**Last Events Page (`/events`)**

Displays a table with the most recent remote access events, including timestamp, username,
client IP and port, service (US44 / US78 / US86) and event type (LOGIN_SUCCESS,
LOGIN_FAIL, LOGOUT, DISCONNECT). The table is refreshed automatically every 5 seconds
via an AJAX call to `/api/events` — only the table body is replaced, the page is never reloaded.

**Active Users Page (`/active`)**

Displays a table of users currently connected to any of the remote services, showing
username, client IP and port, service and connection start time. A user is considered
active if their last logged event is LOGIN_SUCCESS with no subsequent LOGOUT or DISCONNECT.
The table is refreshed automatically every 5 seconds via an AJAX call to `/api/active`.

---

### 3.5 AJAX Update Mechanism

Both pages use the same client-side pattern:

1. On page load, immediately fetch data from the corresponding JSON endpoint and render the table
2. Use `setInterval` to repeat the fetch every 5 seconds
3. On each interval, replace only the table body (`innerHTML`) with the newly received data
4. Display a last-updated timestamp on the page so the administrator can confirm data is live

No external JavaScript libraries are required — the implementation uses native `fetch` API and vanilla DOM manipulation.

---

### 3.6 Identified Domain Concepts

| Concept | Responsibility |
|---------|---------------|
| `HttpVisualizationServer` | Embedded HTTP server; registers handlers for all endpoints and starts listening |
| `EventsPageHandler` | Serves the Last Events HTML page |
| `ActiveUsersPageHandler` | Serves the Active Users HTML page |
| `EventsApiHandler` | Serializes the event log to JSON and returns it |
| `ActiveUsersApiHandler` | Derives the active user list from the event log and returns it as JSON |
| `RemoteAccessEventStore` | Thread-safe in-memory store shared between the UDP receiver (US90) and the HTTP handlers |

---

### 3.7 Acceptance Tests

**AT1 — Last events page is accessible via browser (US91.1)**

Given the Remote Accesses Logging Server is running,
When an administrator navigates to `/events` in a standard web browser,
Then an HTML page is rendered showing a table of the most recently recorded remote access events.

**AT2 — Active users page is accessible via browser (US91.2)**

Given the Remote Accesses Logging Server is running and at least one remote user is connected,
When an administrator navigates to `/active` in a standard web browser,
Then an HTML page is rendered showing a table of currently active remote users.

**AT3 — Pages update without reload (US91.3 + US91.4)**

Given an administrator has the events page open in a browser,
When a new remote access event is logged by the UDP receiver (US90),
Then within 5 seconds the new event appears in the table without the administrator reloading the page.

**AT4 — Active user disappears after logout (US91.2 + US91.3)**

Given an administrator has the active users page open and a remote user is listed as active,
When that user disconnects or logs out and the event is received by the UDP server,
Then within 5 seconds the user is removed from the active users table without a page reload.