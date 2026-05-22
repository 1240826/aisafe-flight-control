# US078 — Air Transport Company Collaborator Remote Access

## 1. Context

This task was assigned in Sprint 3 within the Computer Networks (RCOMP) scope. The objective is to develop a standalone client application that allows an Air Transport Company Collaborator to securely interact with the core system remotely over a network, without directly accessing the database.

**Assigned to:** Jaime Simões

### 1.1 List of Issues

- Analysis: #61
- Design: #61
- Implement: #61
- Test: #61

---

## 2. Requirements

**US078** As an Air Transport Company Collaborator, I want to remotely access the system using the Air Transport Company App.

### Acceptance Criteria

- **US078.1** A specific TCP-based network client application must be created to communicate with the server application embedded in the main system.
- **US078.2** The client application's interaction with the system must be strictly limited to the TCP connection. Any direct interaction with the database from the client is unacceptable.
- **US078.3** All Air Transport Company Collaborator user stories (e.g., US070, US073, US076) must be accessible remotely via this client application.
- **US078.4** Authentication and authorization must be enforced over the remote connection.

### Dependencies/References

- US030 — Authentication and authorization (must be applied to remote logins).
- All ATC Collaborator User Stories (US070-US077) — The functionality that must be exposed.
- NFR08 — Remote RDBMS configuration must be respected.

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "[Insert LLM Prompt used for defining the custom TCP application protocol or handling concurrent client connections]"

**LLM suggestions adopted:**
- [Insert adopted suggestion, e.g., using a specific Message Format with fields for Version, Code, Data Length, and Payload]

**Decisions made by the team:**
- [Insert specific team decisions, e.g., how the server delegates requests to the existing EAPLI controllers (e.g., `CreateFlightRouteController`) rather than rewriting business logic]

### 3.1 Network Architecture & Protocol

The system requires a **Client-Server Architecture**.
1.  **Server:** A concurrent TCP server running within the main AISafe application. It listens on a specific port, accepts incoming connections, and spawns a thread (or uses asynchronous I/O) for each client.
2.  **Client:** A lightweight console/UI application that connects to the server IP and port.
3.  **Protocol:** A custom application-layer protocol must be defined to serialize requests (e.g., "Login", "Create Route") and deserialize responses.

---

## 4. Design

### 4.1 Realization

**Classes to create/modify:**

| Class | Module | Responsibility |
|-------|--------|----------------|
| `ATCClientApp` | `aisafe.app.atc.client` | Entry point for the client application; manages UI |
| `TcpNetworkClient` | `aisafe.app.atc.client` | Handles TCP socket creation and stream I/O on the client |
| `ATCServerDaemon` | `aisafe.app.server` | Background service in the main app listening for TCP connections |
| `ATCClientHandler` | `aisafe.app.server` | Thread/Runnable that processes requests for a specific connected client |
| `NetworkMessage` | `aisafe.network.common` | Represents the structured protocol message (Request/Response) |
| `MessageParser` | `aisafe.network.common` | Serializes and deserializes objects to/from byte streams |

**Sequence Diagram — Remote Login and Request Execution:**

![Sequence Diagram — Remote ATCC Access]([Insert Sequence Diagram File Name])

### 4.2 Acceptance Tests

**AT1 — No Direct Database Access**
Given the client application is running on a machine without database credentials or drivers,
When the user requests to list the company fleet (US072),
Then the client successfully retrieves and displays the fleet by communicating solely over the TCP socket.

**AT2 — Enforced Authentication**
Given an unauthenticated TCP connection,
When the client attempts to send a "Create Flight Route" protocol message,
Then the server rejects the request with an unauthorized error code and drops or maintains the connection awaiting login.

**AT3 — Successful Remote Execution**
Given an authenticated connection for user "ATC_COLLAB",
When the client sends a formatted request to add an aircraft (US070),
Then the server processes it using the core domain logic and replies with a success message, updating the central system.

---

## 5. Implementation

**Key new/modified files:**

- `[List relevant files created or altered]`

*Major commits: [Insert links or hashes]*

---

## 6. Integration/Demonstration

1. Start the main AISafe server application (which initializes the remote database connection and starts the TCP listening thread).
2. On a separate terminal or machine, launch the `ATCClientApp`.
3. Provide the server's IP address and port.
4. Authenticate using valid Air Transport Company Collaborator credentials.
5. Execute a domain action (e.g., List Pilots) and verify the data matches the central system.
6. Verify via network monitoring (e.g., Wireshark) that communication is restricted strictly to the defined TCP port and protocol.

---

## 7. Observations

[Insert any technical debt, difficulties encountered, or architectural notes here, such as handling socket timeouts or thread pooling limits]