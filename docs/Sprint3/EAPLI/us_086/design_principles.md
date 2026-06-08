# US086 — SOLID, GRASP & GoF Principles Applied

Application to the **Pilot Remote Access** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

Each class in US086 has exactly one responsibility:

#### `RemotePilotService` — Facade delegation only

```java
public class RemotePilotService {

    private final ListCompanyFleetController fleetController;
    private final ImportFlightPlanController importController;
    private final TestFlightPlanController testController;
    private final GenerateSimulationReportController reportController;
    private final GenerateMonthlyReportController monthlyController;

    public List<AircraftDTO> listFleet() {
        return StreamSupport.stream(fleetController.allActiveAircraft().spliterator(), false)
                .map(AircraftDTO::from)
                .collect(Collectors.toList());
    }

    public TestFlightPlanController.TestResult validateFlightPlan(final String flightPlanId) {
        return testController.testFlightPlan(flightPlanId);
    }
}
```

**Why SRP?** `RemotePilotService` only **delegates** — it does NOT contain fleet listing logic, flight plan creation rules, simulation logic, or report generation. Each operation is forwarded to the controller that owns that responsibility.

#### `PilotClientHandler` — Protocol dispatch only

```java
class PilotClientHandler extends AbstractClientHandler {

    private final RemotePilotService pilotService;

    PilotClientHandler(final Socket clientSocket) {
        super(clientSocket, RemoteProtocol.SVC_PILOT, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        this.pilotService = new RemotePilotService();
    }

    @Override
    protected String handleCommand(final String cmd, final String[] fields) {
        return switch (cmd) {
            case RemoteProtocol.CMD_LIST_FLEET           -> doListFleet();
            case RemoteProtocol.CMD_CREATE_FLIGHT_PLAN   -> doCreateFlightPlan(fields);
            case RemoteProtocol.CMD_IMPORT_FLIGHT_PLAN   -> doImportFlightPlan(fields);
            case RemoteProtocol.CMD_VALIDATE_FLIGHT_PLAN -> doValidateFlightPlan(fields);
            case RemoteProtocol.CMD_GENERATE_REPORT      -> doGenerateReport(fields);
            case RemoteProtocol.CMD_MONTHLY_REPORT       -> doMonthlyReport(fields);
            case RemoteProtocol.CMD_LIST_FLIGHTS         -> doListFlights();
            case RemoteProtocol.CMD_LIST_ROUTES          -> doListRoutes();
            default -> RemoteProtocol.err("Unknown command: " + cmd);
        };
    }
}
```

**Why SRP?** `PilotClientHandler` only **dispatches** TCP commands — it parses the protocol, calls the service, and formats the response. It does NOT perform any business operation.

#### `AircraftDTO` — Data transfer only

```java
public record AircraftDTO(
        String registrationNumber,
        String aircraftModelCode,
        String operationalStatus,
        int totalCapacity
) {
    public static AircraftDTO from(final Aircraft a) {
        return new AircraftDTO(
                a.registrationNumber().number(),
                a.aircraftModelCode().toString(),
                a.operationalStatus().name(),
                a.totalCapacity()
        );
    }
}
```

**Why SRP?** `AircraftDTO` only **transfers data** across the network boundary — it serialises domain aggregate state into a plain record. No business logic, no persistence, no validation beyond the record's canonical constructor.

#### Other service classes and their single reasons:

| Class | Single Responsibility |
|-------|----------------------|
| `AbstractClientHandler.run()` | Auth handshake + read-dispatch loop skeleton |
| `AbstractTcpServer.run()` | TCP accept loop with thread pool management |
| `PilotServerDaemon` | Instantiates `PilotClientHandler` for each TCP connection |
| `PilotClientApp` | Interactive console UI for the remote pilot |
| `TcpClient` | Raw TCP send / receive abstraction |
| `RemoteProtocol` | Protocol constants (commands, responses, ports, separator character) |
| `UdpAccessLogger` | Fire-and-forget UDP event logging for US090 |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: `AbstractClientHandler` — Template Method pattern

```java
public abstract class AbstractClientHandler implements Runnable {

    @Override
    public final void run() {
        // ── Auth loop (closed for modification) ─────────────────
        while ((line = in.readLine()) != null) {
            // AUTH handling, QUIT handling, auth guard...
            // ...
            // ── Dispatch to subclass (open for extension) ────────
            final String response = handleCommand(cmd, fields);
            out.print(response);
            out.flush();
        }
    }

    protected abstract String handleCommand(String cmd, String[] fields);
}
```

**Why OCP?** The auth handshake, UDP logging, QUIT handling, and the main read-loop are **closed** in `AbstractClientHandler`. Adding a new command (e.g. `LIST_REPORTS`) requires only extending `PilotClientHandler.handleCommand()` with a new `case` — the base class is never modified.

#### Example 2: `AbstractTcpServer` — Template Method pattern

```java
public abstract class AbstractTcpServer implements Runnable {

    @Override
    public final void run() {
        try (final ServerSocket server = new ServerSocket(port)) {
            while (running) {
                final Socket client = server.accept();
                pool.submit(createHandler(client));
            }
        }
    }

    protected abstract Runnable createHandler(Socket clientSocket);
}

public class PilotServerDaemon extends AbstractTcpServer {
    @Override
    protected Runnable createHandler(final Socket clientSocket) {
        return new PilotClientHandler(clientSocket);
    }
}
```

**Why OCP?** The accept-loop + thread pool management is **closed** in `AbstractTcpServer`. Supporting a new service (e.g. a future US086 extension, or a different service) means extending `AbstractTcpServer` and overriding `createHandler()` — the TCP infrastructure never changes.

#### Example 3: `RemotePilotService` — new operations via additional delegation methods

```java
public class RemotePilotService {
    // Existing operations — closed
    public List<AircraftDTO> listFleet() { ... }
    public DslValidationResult createFlightPlan(...) { ... }
    public TestResult validateFlightPlan(...) { ... }

    // New operation — open for extension
    public List<?> listFlights() {
        return StreamSupport.stream(testController.allFlights().spliterator(), false)
                .collect(Collectors.toList());
    }
}
```

Adding a new exposed operation means adding a method that delegates to the appropriate controller — no existing methods are modified.

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example 1: `PilotClientHandler` is a valid `AbstractClientHandler`

```java
// Base class defines the contract
public abstract class AbstractClientHandler implements Runnable {
    protected abstract String handleCommand(String cmd, String[] fields);
}

// Subclass satisfies the contract
class PilotClientHandler extends AbstractClientHandler {
    @Override
    protected String handleCommand(final String cmd, final String[] fields) {
        // Always returns a response matching the protocol format
        return switch (cmd) {
            case CMD_LIST_FLEET -> doListFleet();
            // ...
        };
    }
}
```

**Why LSP?** `AbstractClientHandler` can be used anywhere a `Runnable` is expected. `PilotClientHandler` can be substituted for any `AbstractClientHandler` reference. The base class's `run()` method calls `handleCommand()` polymorphically — it never checks the concrete type.

#### Example 2: `PilotServerDaemon` is a valid `AbstractTcpServer`

```java
// Base class
public abstract class AbstractTcpServer implements Runnable {
    protected abstract Runnable createHandler(Socket clientSocket);
}

// Subclass
public class PilotServerDaemon extends AbstractTcpServer {
    @Override
    protected Runnable createHandler(final Socket clientSocket) {
        return new PilotClientHandler(clientSocket);
    }
}
```

**Why LSP?** `PilotServerDaemon` can be used wherever `AbstractTcpServer` is expected. The base class calls `createHandler()` polymorphically and gets a valid `Runnable` that follows the handler contract.

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example 1: `RemotePilotService` — small, focused public surface

```java
public class RemotePilotService {
    public List<AircraftDTO> listFleet();
    public DslValidationResult createFlightPlan(String, String);
    public DslValidationResult importFlightPlan(String, String);
    public TestResult validateFlightPlan(String);
    public String generateReport(String);
    public MonthlyReport monthlyReport(int, int);
    public List<?> listFlights();
    public List<?> listRoutes();
}
```

**Why ISP?** Every method has a single purpose and a distinct return type. Callers (`PilotClientHandler`) depend only on the specific methods they invoke — the service never forces a caller to accept methods it does not use.

#### Example 2: `AircraftDTO` — minimal record (only 3 fields + 1 factory)

```java
public record AircraftDTO(
        String registrationNumber,
        String aircraftModelCode,
        String operationalStatus,
        int totalCapacity
) {
    public static AircraftDTO from(final Aircraft a) { ... }
}
```

**Why ISP?** The DTO exposes only the fields needed by the remote client (`totalCapacity` is computed for UI display). The full `Aircraft` aggregate (with `CabinConfiguration`, `OperationalStatus` enum, `RegistrationNumber` VO, `AircraftModelCode` VO) is never exposed — the DTO acts as a segregated interface.

#### Example 3: `RemoteProtocol` — constants grouped by purpose

```java
public class RemoteProtocol {
    // Command constants
    public static final String CMD_LIST_FLEET       = "LIST_FLEET";
    public static final String CMD_CREATE_FLIGHT_PLAN = "CREATE_FLIGHT_PLAN";
    // ...
    // Response constants
    public static final String RESP_AUTH_OK   = "AUTH_OK";
    public static final String RESP_AUTH_FAIL = "AUTH_FAIL";
    // ...
}
```

**Why ISP?** `PilotClientHandler` uses both command and response constants. `PilotClientApp` (client side) uses only command constants. `RemoteProtocol` is not segregated further because the cost of additional abstraction outweighs the benefit for constants.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### Example: `RemotePilotService` depends on controller abstractions

```java
public class RemotePilotService {

    // All dependencies are CONTROLLERS (application layer abstractions):
    private final ListCompanyFleetController fleetController;
    private final ImportFlightPlanController importController;
    private final TestFlightPlanController testController;
    private final GenerateSimulationReportController reportController;
    private final GenerateMonthlyReportController monthlyController;

    // Default constructor — framework-managed instantiation
    public RemotePilotService() {
        this.fleetController = new ListCompanyFleetController();
        this.importController = new ImportFlightPlanController();
        this.testController = new TestFlightPlanController();
        this.reportController = new GenerateSimulationReportController();
        this.monthlyController = new GenerateMonthlyReportController();
    }

    // Package-private constructor — testability via dependency injection
    RemotePilotService(final ListCompanyFleetController fleetController,
                        final ImportFlightPlanController importController,
                        final TestFlightPlanController testController,
                        final GenerateSimulationReportController reportController,
                        final GenerateMonthlyReportController monthlyController) {
        this.fleetController = fleetController;
        this.importController = importController;
        this.testController = testController;
        this.reportController = reportController;
        this.monthlyController = monthlyController;
    }
}
```

**Why DIP?**
- `RemotePilotService` depends on **controllers** (application layer), never on domain entities, repositories, or infrastructure
- Each controller is a framework-managed `@UseCaseController` that handles its own authorisation and persistence
- The package-private constructor allows **test doubles** to be injected — every test in `RemotePilotServiceTest` uses mocked controllers
- `RemotePilotService` does NOT depend on `AuthzRegistry`, `PersistenceContext`, or any concrete repository

#### Example: `PilotServerDaemon` depends on `AbstractTcpServer` abstraction

```java
// Concretion depends on abstraction
public class PilotServerDaemon extends AbstractTcpServer {
    // Only implements createHandler() — the base class handles all TCP logic
}
```

**Why DIP?** `PilotServerDaemon` (high-level server) depends on `AbstractTcpServer` (abstract base). The TCP socket infrastructure (`ServerSocket`, `ExecutorService`) is encapsulated in the base class — the subclass never imports `java.net.ServerSocket`.

---

## GRASP Principles

---

### Controller (GRASP)

> *Assign the responsibility of handling system events to a non-UI class that represents the overall use case.*

**Applied by:** `RemotePilotService` (remote access facade), `PilotClientHandler` (TCP command dispatcher)

```java
// RemotePilotService handles ALL remote access system events:
public class RemotePilotService {
    public List<AircraftDTO> listFleet()         { /* delegates to fleet controller */ }
    public DslValidationResult createFlightPlan(...) { /* delegates to import controller */ }
    public TestResult validateFlightPlan(...)     { /* delegates to test controller */ }
    public String generateReport(...)             { /* delegates to report controller */ }
    public MonthlyReport monthlyReport(...)       { /* delegates to monthly controller */ }
    public List<?> listFlights()                  { /* delegates to test controller */ }
    public List<?> listRoutes()                   { /* delegates to persistence */ }
}
```

`RemotePilotService` is the single point of entry for every remote operation. `PilotClientHandler` receives TCP commands and invokes the service as a classic client → controller flow.

---

### Creator (GRASP)

> *Assign responsibility for creating instances of class A to class B when B contains, aggregates, records, or closely uses A.*

| Creator Class | Creates | Rationale |
|--------------|---------|-----------|
| `AircraftDTO.from(Aircraft)` | `AircraftDTO` | Static factory — the DTO class knows its own mapping from `Aircraft` |
| `RemoteProtocol.ok()` / `err()` | Response `String` | Protocol class knows the exact format for success/error responses |
| `PilotClientHandler(PilotClientHandler)` | `RemotePilotService` | Handler uses the service — it is the natural creator |
| `PilotServerDaemon` | `PilotClientHandler` | Server creates one handler per accepted TCP connection |
| `ImportFlightPlanController` | `DslValidationResult` | Controller creates the result from ANTLR pipeline (delegated from RemotePilotService) |

```java
// AircraftDTO creates itself from a domain Aircraft
public static AircraftDTO from(final Aircraft a) {
    return new AircraftDTO(
            a.registrationNumber().number(),
            a.aircraftModelCode().toString(),
            a.operationalStatus().name(),
            a.totalCapacity()
    );
}

// RemoteProtocol creates formatted response strings
public static String ok(final String data) {
    return RESP_OK + SEP + data + "\n";
}

public static String err(final String msg) {
    return RESP_ERR + SEP + msg + "\n";
}
```

---

### Information Expert (GRASP)

> *Assign a responsibility to the class that has the information needed to fulfill it.*

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Fleet listing data | `ListCompanyFleetController` | Controller owns access to `AircraftRepository` |
| Flight plan creation | `ImportFlightPlanController` | Controller owns the ANTLR validation pipeline |
| Flight plan validation | `TestFlightPlanController` | Controller owns the DSL → C simulator pipeline |
| Simulation report generation | `GenerateSimulationReportController` | Controller owns simulation results |
| Monthly report generation | `GenerateMonthlyReportController` | Controller owns monthly aggregation |
| DTO mapping (Aircraft → AircraftDTO) | `AircraftDTO` | DTO knows its own fields and conversion from `Aircraft` |
| TCP protocol format | `RemoteProtocol` | Protocol class owns all constants and helpers |
| Auth handshake | `AbstractClientHandler` | Handler knows the AUTH command flow, UDP logging, security clearance check |
| TCP accept loop | `AbstractTcpServer` | Server knows bind, accept, thread pool lifecycle |

```java
// AircraftDTO is the expert on its own structure
public static AircraftDTO from(final Aircraft a) {
    // knows exactly which Aircraft fields map to which DTO fields
    return new AircraftDTO(
            a.registrationNumber().number(),    // expert: extracts just the string
            a.aircraftModelCode().toString(),    // expert: converts VO to string
            a.operationalStatus().name(),        // expert: enum to string
            a.totalCapacity()                     // expert: computed field
    );
}
```

---

### Tell, Don't Ask

> *Tell an object what to do, rather than asking it for its data and making decisions on its behalf.*

```java
// GOOD — RemotePilotService TELLS the controller to operate
public TestFlightPlanController.TestResult validateFlightPlan(final String flightPlanId) {
    return testController.testFlightPlan(flightPlanId);  // tells, does not ask
}

// GOOD — AbstractClientHandler tells the logger to record events
logger.loginOk(authenticatedUsername, clientIp, clientPort, serviceId);

// GOOD — PilotClientHandler tells the service to list the fleet
final var aircraft = pilotService.listFleet();
```

**Applied by:** `RemotePilotService` never checks authorisation, never queries the database, never inspects domain state — it tells controllers to act, and the controllers decide internally. `PilotClientHandler` never inspects `RemotePilotService` internals — it tells the service what to do and processes the result.

---

### High Cohesion (GRASP)

> *Keep responsibilities strongly related and focused within each class.*

```
Low Cohesion (bad):                          High Cohesion (our design):
┌──────────────────────────────┐             ┌──────────────────────┐
│  MegaNetworkHandler          │             │  RemotePilotService  │
│  - auth()                    │             │  - listFleet()       │
│  - listFleet()               │             │  - createFlightPlan()│
│  - validateFlightPlan()      │             │  - validateFlight()  │
│  - parseProtocol()           │             │  - generateReport()  │
│  - manageThreadPool()        │             └──────────────────────┘
│  - formatReport()            │
│  - connectToDatabase()       │             ┌──────────────────────┐
└──────────────────────────────┘             │  PilotClientHandler  │
                                             │  - handleCommand()   │
Each class in our design is cohesive:        │  - format response   │
                                             └──────────────────────┘
• Facade: RemotePilotService                 ┌──────────────────────┐
• Handler: PilotClientHandler                │ AbstractClientHandler│
• Server: AbstractTcpServer                  │  - auth handshake    │
• Protocol: RemoteProtocol                   │  - read-dispatch loop│
• Transport: AircraftDTO                     └──────────────────────┘
```

---

### Low Coupling (GRASP)

> *Keep dependencies between classes as weak as possible.*

```
PilotClientHandler
    │
    ├──→ RemotePilotService     (facade — single class dependency)
    ├──→ AbstractClientHandler  (base class — extension, not composition)
    ├──→ RemoteProtocol         (constants only)
    └──→ AISafeRoles            (role constant)

RemotePilotService
    │
    ├──→ ListCompanyFleetController        (interface-like controller)
    ├──→ ImportFlightPlanController        (interface-like controller)
    ├──→ TestFlightPlanController          (interface-like controller)
    ├──→ GenerateSimulationReportController (interface-like controller)
    └──→ GenerateMonthlyReportController   (interface-like controller)
```

```java
// PilotClientHandler depends on only 2 concrete classes + 1 abstract + 1 constants class:
import eapli.aisafe.remote.RemoteProtocol;          // constants
import eapli.aisafe.remote.pilot.RemotePilotService;// facade
import eapli.aisafe.usermanagement.domain.AISafeRoles; // role constant

// RemotePilotService depends on 5 controllers (all @UseCaseController):
import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
```

---

### Polymorphism (GRASP)

> *Use polymorphic operations to handle variations in behavior based on type.*

#### Template Method polymorphism (abstract base → concrete handler)

```java
// AbstractClientHandler defines the skeleton
public abstract class AbstractClientHandler implements Runnable {
    @Override
    public final void run() {
        // auth loop, read-dispatch — SAME for all services
        // ...
        final String response = handleCommand(cmd, fields);  // ← POLYMORPHIC
        out.print(response);
    }
    protected abstract String handleCommand(String cmd, String[] fields);
}

// PilotClientHandler provides the pilot-specific behaviour
class PilotClientHandler extends AbstractClientHandler {
    @Override
    protected String handleCommand(final String cmd, final String[] fields) {
        return switch (cmd) { /* pilot commands */ };
    }
}
```

#### AbstractTcpServer → PilotServerDaemon polymorphism

```java
public abstract class AbstractTcpServer implements Runnable {
    @Override
    public final void run() { /* accept loop */ }
    protected abstract Runnable createHandler(Socket s);  // ← POLYMORPHIC
}

public class PilotServerDaemon extends AbstractTcpServer {
    @Override
    protected Runnable createHandler(final Socket s) {
        return new PilotClientHandler(s);  // ← each service creates its handler
    }
}
```

---

### Protected Variations (GRASP)

> *Protect the system from variations in external components by wrapping them behind stable interfaces.*

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| TCP protocol format (pipe-separated commands) | `RemoteProtocol` constants + `parse()` | Changing the separator, command names, or response format affects only `RemoteProtocol` |
| TCP transport (blocking NIO) | `AbstractTcpServer` accept loop | Switching to NIO, SSL, or UNIX sockets affects only the server class |
| Authentication mechanism | `AbstractClientHandler` + `AuthzRegistry` | Changing LDAP, OAuth, or local auth affects only the AUTH block in `AbstractClientHandler` |
| Security clearance check (US030.4) | `AbstractClientHandler.isClearanceValid()` | Changing clearance policy (expiry rule, database schema) affects only that method |
| UDP access logging (US090) | `UdpAccessLogger` | Changing log format, destination, or protocol affects only the logger class |
| Underlying controller implementations | `RemotePilotService` facade | Changing any controller's constructor or public API affects only the service method that delegates to it |
| Client UI technology | `PilotClientApp` standalone | Moving to a GUI, web, or mobile client requires replacing only the client application |

```java
// If the protocol changes command names:
// → ONLY RemoteProtocol constants change
private static final String CMD_LIST_FLEET = "LIST_FLEET";  // before
private static final String CMD_LIST_FLEET = "FLEET";       // after — one edit

// If the auth mechanism changes:
// → ONLY AbstractClientHandler.AUTH block changes
final Authenticator auth = AuthzRegistry.authenticationService();
final var session = auth.authenticate(username, password, requiredRole);

// If a controller signature changes:
// → ONLY the corresponding RemotePilotService method changes
public List<AircraftDTO> listFleet() {
    return StreamSupport.stream(fleetController.allActiveAircraft().spliterator(), false)
            .map(AircraftDTO::from)
            .collect(Collectors.toList());
}
```

---

### Pure Fabrication (GRASP)

> *Create artificial classes that do not represent domain concepts to achieve low coupling and high cohesion.*

These classes have no counterpart in the domain model — they are pure fabrications created to keep domain classes clean:

| Class | Why It's a Fabrication |
|-------|----------------------|
| `RemotePilotService` | The domain has no "remote pilot service" — it's a network-facing facade to existing controllers |
| `PilotClientHandler` | TCP command dispatching is an infrastructure concern, not a domain concept |
| `AbstractClientHandler` | Auth handshake + read loop is a reusable networking pattern, not domain logic |
| `AbstractTcpServer` | TCP accept loop + thread pool is pure infrastructure |
| `PilotServerDaemon` | Service-specific wiring of server + handler is infrastructure |
| `RemoteProtocol` | Protocol constants are a wire-format concern, not part of the domain |
| `UdpAccessLogger` | UDP logging is an operational concern (US090), not domain behaviour |
| `AircraftDTO` | Data Transfer Object is a serialisation concern, not a domain entity |
| `PilotClientApp` | Console UI is a presentation concern |
| `TcpClient` | Raw socket send/receive is infrastructure |

```java
// Without Pure Fabrication, the Pilot domain entity would need to know about TCP:
public class Pilot {                        // ← BAD! Domain entity knows about networking
    public String listFleetOverTcp() { ... }
    public String createFlightPlanOverTcp() { ... }
}

// With Pure Fabrication:
public class RemotePilotService {           // ← PURE FABRICATION (facade)
    public List<AircraftDTO> listFleet() { /* delegates to controller */ }
}

public class PilotClientHandler {           // ← PURE FABRICATION (protocol handler)
    public String handleCommand(...) { /* formats TCP response */ }
}
```

---

### Indirection (GRASP)

> *Assign the responsibility of mediating between two components to an intermediate object.*

| Mediator | Mediates Between | Why |
|----------|-----------------|-----|
| `RemotePilotService` | Network layer (`PilotClientHandler`) ↔ Application controllers | The handler never imports any controller directly |
| `PilotClientHandler` | TCP socket (byte stream) ↔ `RemotePilotService` (Java objects) | The service never sees raw bytes or protocol parsing |
| `AbstractClientHandler` | `Socket` (I/O streams) ↔ subclass `handleCommand()` | Subclasses never deal with `BufferedReader` or `PrintWriter` |
| `AbstractTcpServer` | `ServerSocket` ↔ `AbstractClientHandler` | Handlers never create or manage the server socket |
| `RemoteProtocol` | Handler code ↔ wire format | Protocol format changes affect only RemoteProtocol, not handlers |
| `AircraftDTO` | `Aircraft` (domain) ↔ remote client | The remote client never receives a JPA-managed `Aircraft` entity |

```java
// Without Indirection: PilotClientHandler would import and call every controller
class PilotClientHandler {
    public String doListFleet() {
        var ctrl = new ListCompanyFleetController();  // ← BAD! tight coupling
        var fleet = ctrl.allActiveAircraft();
        // format ...
    }
}

// With Indirection — RemotePilotService mediates:
class PilotClientHandler {
    public String doListFleet() {
        var aircraft = pilotService.listFleet();  // ← INDIRECTION via facade
        // format only, no controller imports
    }
}

// Without Indirection — handler parses raw bytes
class PilotClientHandler {
    // BAD: handler sees BufferedReader, InputStreamReader, etc.
}

// With Indirection — AbstractClientHandler mediates:
// Base class handles all I/O, subclass only implements handleCommand(String, String[])
```

---

## GoF Patterns

---

### Facade Pattern

> *Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use.*

**Where:** `RemotePilotService`

```java
public class RemotePilotService {

    // Five different controllers hidden behind a single class
    private final ListCompanyFleetController fleetController;
    private final ImportFlightPlanController importController;
    private final TestFlightPlanController testController;
    private final GenerateSimulationReportController reportController;
    private final GenerateMonthlyReportController monthlyController;

    // Seven operations exposed through a clean, unified interface
    public List<AircraftDTO> listFleet();
    public DslValidationResult createFlightPlan(String, String);
    public DslValidationResult importFlightPlan(String, String);
    public TestResult validateFlightPlan(String);
    public String generateReport(String);
    public MonthlyReport monthlyReport(int, int);
    public List<?> listFlights();
    public List<?> listRoutes();
}
```

**Why Facade?**
- `PilotClientHandler` calls a **single service** instead of five different controllers
- The subsystem (FCO controllers) is decoupled from the TCP layer
- Each controller's complexity (auth, validation, persistence) is hidden behind the facade

---

### Template Method Pattern

> *Define the skeleton of an algorithm in an operation, deferring some steps to subclasses.*

#### Template 1: `AbstractClientHandler.run()`

```java
public abstract class AbstractClientHandler implements Runnable {

    @Override
    public final void run() {                    // ← TEMPLATE METHOD (final)
        // ── Common steps (invariant) ──
        // 1. Auth handshake (AUTH / AUTH_OK / AUTH_FAIL)
        // 2. QUIT handling with UDP logging
        // 3. Auth guard on all commands
        // 4. Read-dispatch loop

        // ── Deferred step (variant) ──
        final String response = handleCommand(cmd, fields);  // ← HOOK
        out.print(response);
    }

    protected abstract String handleCommand(String cmd, String[] fields);  // ← ABSTRACT
}
```

#### Template 2: `AbstractTcpServer.run()`

```java
public abstract class AbstractTcpServer implements Runnable {

    @Override
    public final void run() {                    // ← TEMPLATE METHOD (final)
        // ── Common steps (invariant) ──
        try (ServerSocket server = new ServerSocket(port)) {
            while (running) {
                Socket client = server.accept();
                pool.submit(createHandler(client));   // ← HOOK
            }
        }
    }

    protected abstract Runnable createHandler(Socket clientSocket);  // ← ABSTRACT
}
```

**Why Template Method?**
- Avoids code duplication — the accept-loop / auth-loop is written once
- Enforces invariant structure — subclasses cannot accidentally reorder or skip steps
- Makes extension trivial — adding a new handler or server requires only implementing the abstract method

---

### Data Transfer Object (DTO) Pattern

> *An object that carries data between processes to reduce the number of method calls.*

**Where:** `AircraftDTO`

```java
public record AircraftDTO(
        String registrationNumber,
        String aircraftModelCode,
        String operationalStatus,
        int totalCapacity
) {
    public static AircraftDTO from(final Aircraft a) {
        return new AircraftDTO(
                a.registrationNumber().number(),
                a.aircraftModelCode().toString(),
                a.operationalStatus().name(),
                a.totalCapacity()
        );
    }
}
```

**Why DTO?**
- The remote client receives **flat, serialisable data** — no JPA proxies, no lazy-loading, no circular references
- The DTO contains **only what the client needs** (4 fields), not the full `Aircraft` aggregate (which has `CabinConfiguration`, `OperationalStatus` enum, `RegistrationNumber` VO, seat lists, etc.)
- The `record` keyword gives **immutability, equals/hashCode, and toString** for free
- DTOs are serialisable across the TCP connection (as pipe-separated text)

---

### Builder Pattern (Framework-integrated)

> *Separate the construction of a complex object from its representation so that the same construction process can create different representations.*

**Where:** Remote protocol response assembly in `RemoteProtocol`

```java
public class RemoteProtocol {
    public static final String SEP = "|";
    public static final String RESP_OK  = "OK";
    public static final String RESP_ERR = "ERR";

    public static String ok(final String data) {
        return RESP_OK + SEP + data + "\n";
    }

    public static String err(final String msg) {
        return RESP_ERR + SEP + msg + "\n";
    }
}
```

Each `PilotClientHandler.do*()` method builds its response by composing protocol tokens:

```java
private String doListFleet() {
    final StringBuilder sb = new StringBuilder();
    int count = 0;
    for (final AircraftDTO a : aircraft) {
        if (count > 0) sb.append(";");
        sb.append(a.registrationNumber()).append(",")
                .append(a.aircraftModelCode()).append(",")
                .append(a.operationalStatus()).append(",")
                .append(a.totalCapacity());
        count++;
    }
    return RemoteProtocol.ok(count + " aircraft: " + sb);
}
```

The response assembly is separated from the protocol framing — `RemoteProtocol.ok()` wraps data in the wire format, while `doListFleet()` builds only the data portion.

---

### Singleton / Registry Pattern (EAPLI Framework)

> *Ensure a class has only one instance and provide a global point of access to it.*

**Where:** `AuthzRegistry` (EAPLI framework) — used in `AbstractClientHandler`

```java
// Singleton access to authentication service
final Authenticator auth = AuthzRegistry.authenticationService();
final var session = auth.authenticate(username, password, requiredRole);
```

**Why Singleton/Registry?**
- `AuthzRegistry` is a well-known single point of access for the authentication subsystem
- The registry decouples handler code from concrete authentication implementation (local DB, LDAP, etc.)
- Used consistently across all EAPLI controllers — the TCP handlers reuse the same pattern

---

### Layers Pattern

> *Organise code into layers where each layer has a specific responsibility and depends only on lower layers.*

```
TCP Client (PilotClientApp)          ─── Presentation Layer (remote)
    │
TCP Server (PilotServerDaemon)       ─── Infrastructure / Network
    │
AbstractClientHandler / PilotClientHandler  ─── Infrastructure / Protocol
    │
RemotePilotService                   ─── Application (Facade)
    │
Controllers                          ─── Application (Use Case)
    │
Domain Entities / VOs                ─── Domain
    │
Persistence (JPA / InMemory)         ─── Infrastructure / Persistence
```

```
TCP Client (outside app)
    │ TCP (pipe-separated protocol)
PilotServerDaemon + PilotClientHandler  (RCOMP component, embedded in app)
    │ Java API
RemotePilotService                       (EAPLI facade)
    │ delegation
Flight / FlightPlan / Aircraft           (Domain — untouched by remote code)
```

**Layer Responsibilities:**

| Layer | Contains | Dependencies |
|-------|----------|-------------|
| **Client (RCOMP)** | `PilotClientApp`, `TcpClient` | TCP socket only |
| **Server (RCOMP)** | `PilotServerDaemon`, `AbstractTcpServer`, `AbstractClientHandler`, `PilotClientHandler` | TCP socket + EAPLI facade |
| **Application (EAPLI)** | `RemotePilotService`, `AircraftDTO`, `RemoteProtocol`, `UdpAccessLogger` | Controllers, Domain |
| **Domain (EAPLI)** | `Flight`, `FlightPlan`, `Aircraft`, `Pilot`, etc. | EAPLI framework only |
| **Infrastructure (EAPLI)** | `Jpa*Repository`, `InMemory*Repository` | Domain + Persistence framework |

---

## Package & Layer Architecture

```
aisafe.base/
├── core/src/main/java/eapli/aisafe/
│   ├── remote/
│   │   ├── pilot/
│   │   │   ├── RemotePilotService.java    (Facade, Application)
│   │   │   └── AircraftDTO.java          (DTO, Application)
│   │   ├── RemoteProtocol.java           (Protocol constants, Infrastructure)
│   │   └── UdpAccessLogger.java          (Logging, Infrastructure)
│   └── ... (existing domain/application packages)
│
├── app/src/main/java/eapli/aisafe/
│   └── server/
│       ├── AbstractTcpServer.java         (Template, Infrastructure)
│       ├── AbstractClientHandler.java     (Template, Infrastructure)
│       ├── PilotServerDaemon.java         (Concrete server, Infrastructure)
│       └── PilotClientHandler.java       (Concrete handler, Infrastructure)
│
rcomp/
└── us086/src/rcomp/
    └── client/
        ├── PilotClientApp.java           (Client UI, Presentation)
        └── TcpClient.java                (Raw TCP, Infrastructure)
```

### Layer Dependency Rules

```
PilotClientApp → TcpClient                  (client → socket)
PilotClientHandler → RemotePilotService     (handler → facade)
RemotePilotService → Controllers             (facade → use case)
Controllers → Domain + Repository interfaces (use case → domain)
Repository implementations → Repository interfaces  (infra → interface)
```

No domain class imports any remote, server, or client class.
No controller class imports any `RemotePilotService` or `RemoteProtocol` class.
No server class imports any controller directly (all access goes through `RemotePilotService`).
