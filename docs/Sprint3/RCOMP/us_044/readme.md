# US044 — Weather Person Remote Access (RCOMP)

## 1. Context

This task was assigned in Sprint 3 within the Computer Networks (RCOMP) scope. The objective is to develop a standalone client application that allows a Weather Person to securely interact with the core system remotely over a network, without directly accessing the database.

**Assigned to:** Dinis Silva

### 1.1 List of Issues

- Analysis: #60
- Design: #60
- Implement: #60
- Test: #60

---

## 2. Requirements

**US044** As a Weather Person, I want to remotely access the system in order to upload weather data.

### Acceptance Criteria

- **US044.1** A specific TCP-based network client application must be created to communicate with the server application embedded in the main system.
- **US044.2** The client application's interaction with the system must be strictly limited to the TCP connection. Any direct interaction with the database from the client is unacceptable.
- **US044.3** All Weather Person user stories must be accessible remotely via this client application:
  - **US041** — Register weather data
  - **US042** — Import bulk weather data
  - **US043** — Consult weather data
- **US044.4** Authentication and authorization must be enforced over the remote connection.

### Dependencies/References

- US030 — Authentication and authorization (must be applied to remote logins).
- US041, US042, US043 — The Weather Person functionality that must be exposed remotely.
- US090 — External logging of remote accesses (logs every login/logout/disconnect via UDP).
- NFR08 — Remote RDBMS configuration must be respected.

---

## 3. Analysis

### 3.1 Network Architecture

The system follows a two-tier remote access model:

- A standalone **Weather Person Client App** (Java console) connects via TCP to a dedicated port exposed by the main system
- The **TCP Server** is embedded in the main system and spawns one handler thread per accepted connection
- All business logic (US041, US042, US043) executes on the server side through existing application services
- The client sends text requests and receives text responses — it never touches the database
- The server enforces that the authenticated user holds the `WEATHER_PERSON` role before any operation is dispatched

### 3.2 Application-Level Protocol

A simple text-based request/response protocol is defined for this connection.
Each message is terminated with `\n`. Fields are separated by `|`.

**Client to Server messages:**

| Code | Format | Description |
|------|--------|-------------|
| AUTH | `AUTH|<username>|<password>` | Authenticate the session |
| REGISTER_WEATHER | `REGISTER_WEATHER|<area>|<lat>|<lon>|<alt>|<speed>|<dir>|<temp>|<provider>|<datetime>` | Register weather data (US041) |
| IMPORT_WEATHER | `IMPORT_WEATHER|<area>|<csv_rows>` | Import bulk weather data (US042) |
| CONSULT_WEATHER | `CONSULT_WEATHER|<area>|<date>` | Consult weather data (US043) |
| LIST_AREAS | `LIST_AREAS` | List air control areas |
| QUIT | `QUIT` | Gracefully close the session |

**Server to Client messages:**

| Code | Meaning |
|------|---------|
| `AUTH_OK` | Authentication successful |
| `AUTH_FAIL|<reason>` | Authentication failed |
| `OK|<optional_data>` | Operation succeeded |
| `ERR|<reason>` | Operation failed — reason included |
| `BYE` | Server acknowledges QUIT |

### 3.3 Session Flow

1. Client establishes TCP connection to the server on the Weather-dedicated port (1044)
2. Server accepts and spawns a handler thread for that connection
3. Client sends `AUTH|<username>|<password>`
4. Server validates credentials via EAPLI `AuthzRegistry`
5. Server checks `securityClearanceExpiryDate` (US030.4)
6. Server verifies the authenticated user holds the `WEATHER_PERSON` role
7. If all valid: server responds `AUTH_OK` and session is bound to that Weather Person
8. If invalid: server responds `AUTH_FAIL|<reason>` — connection stays open for retry or QUIT
9. Server emits a UDP log event to the Remote Accesses Logging Server (US090) for both outcomes
10. Any request other than AUTH or QUIT received before authentication is answered with `ERR|NOT_AUTHENTICATED`
11. After QUIT or disconnect, the server closes the socket and cleans up the session

### 3.4 Architecture with DTOs

The system follows the **DTO pattern** to isolate the domain model from the TCP protocol layer.

**DTOs in the Weather remote subdomain:**

| DTO | Fields | Factory |
|-----|--------|---------|
| `AirControlAreaDTO` | areaCode, name, minLat, maxLat, minLon, maxLon, maxAltitudeMetres | `AirControlAreaDTO.from(AirControlArea)` |
| `WeatherDataDTO` | areaCode, latitude, longitude, altitudeMetres, windSpeedKnots, windDirectionDegrees, temperatureCelsius, sourceProvider, recordedDateTime | `WeatherDataDTO.from(WeatherData)` |

**DTO Flow:**
```
Domain Entity → DTO.from(entity) → DTO record → TCP Handler → format → Client
```

### 3.5 Diagrams

**Sequence Diagram — Remote Weather Person Access:**

![Sequence Diagram](sds/images/sd_us044_weather_remote_access.svg)

*PlantUML source: `sds/uml/sd_us044_weather_remote_access.puml`*

**Component Diagram — Client-Server-Architecture:**

![Component Diagram](sds/images/component_us044_client_server.svg)

*PlantUML source: `sds/uml/component_us044_client_server.puml`*

---

## 4. Design

### 4.1 Realization

**Key classes (RCOMP side):**

| Class | Location | Responsibility |
|-------|----------|---------------|
| `AISafeClientApp` | `rcomp/us044/src/` | Standalone console client with area selection and validation |
| `TcpClient` | `rcomp/us044/src/` | Reusable TCP client with area state persistence |

**Key classes (EAPLI side):**

| Class | Location | Responsibility |
|-------|----------|---------------|
| `WeatherServerDaemon` | `aisafe.base/app/src/.../server/` | Listens on port 1044; accepts connections |
| `WeatherClientHandler` | `aisafe.base/app/src/.../server/` | Per-connection handler: protocol parsing, dispatch, response |
| `RemoteWeatherService` | `aisafe.base/core/src/.../remote/weather/` | Facade wrapping weather controllers |
| `RemoteProtocol` | `aisafe.base/core/src/.../remote/` | Protocol constants and helpers |
| `UdpAccessLogger` | `aisafe.base/core/src/.../remote/` | UDP logging client for US090 |

### 4.2 Acceptance Tests

**AT1 — No Direct Database Access**

Given the client application is running on a machine without database credentials or drivers,
When the user requests to consult weather data (US043),
Then the client successfully retrieves and displays the data by communicating solely over the TCP socket.

**AT2 — Enforced Authentication**

Given an unauthenticated TCP connection,
When the client attempts to send a `REGISTER_WEATHER` protocol message,
Then the server rejects the request with `ERR|NOT_AUTHENTICATED` and does not execute the operation.

**AT3 — Successful Remote Execution**

Given an authenticated connection for a user with the `WEATHER_PERSON` role,
When the client sends a formatted `REGISTER_WEATHER` request,
Then the server processes it using the core domain logic and replies with `OK`, updating the central system.

**AT4 — Wrong Role is Rejected**

Given a user with valid credentials but holding a different role (e.g., `ATC_COLLABORATOR`),
When the client sends `AUTH` on the Weather Person server port,
Then the server responds with `AUTH_FAIL|INSUFFICIENT_ROLE` and the session remains unauthenticated.

**AT5 — Area Selection from List**

Given an authenticated Weather Person,
When the client requests `LIST_AREAS` and chooses an area from the list,
Then subsequent operations use the selected area code, improving user experience and reducing input errors.

---

## 5. Implementation

### 5.1 Files (RCOMP)

| File | Responsibility |
|------|---------------|
| `rcomp/us044/src/AISafeClientApp.java` | Console client with area selection, loop-based validation, and intuitive prompts |
| `rcomp/us044/src/TcpClient.java` | TCP transport layer with selected area state |

### 5.2 UI Improvements

- **Area Selection**: User can choose from a numbered list of areas or enter the code manually
- **Loop-based validation**: Each numeric/date field validates immediately and re-prompts on invalid input
- **Explicit format hints**: Each field shows the expected format
- **Null-safe**: Handles connection errors gracefully without crashing

---

## 6. Integration/Demonstration

1. Start the main application — TCP server binds to the Weather-dedicated port (1044).
2. Start the Remote Accesses Logging Server (US090) to receive UDP event logs.
3. Launch the Weather Person Client App on a different network node.
4. Connect to the main application IP and Weather port.
5. Authenticate as `weather1` / `Password1`.
6. Select an area from the list, then register weather data, import CSV data, and consult data.
7. Check US090 logging server received login/logout events.
8. Verify no direct database calls originated from the client process.

---

## 7. Observations

- The RCOMP component depends on `RemoteWeatherService` from EAPLI — this is the only integration point.
- US090 (UDP logging) must be operational for login/logout events to be recorded.
- The text protocol is identical in structure to US078 and US086 — only the command codes and port differ.
- All input validation happens on the client side with retry loops for robust user experience.
