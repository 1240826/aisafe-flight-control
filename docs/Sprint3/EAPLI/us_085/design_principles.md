# US085 — SOLID & GRASP Principles Applied

Application to the **Test/Validate Flight Plan** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

Each class in US085 has exactly one responsibility:

#### `Flight` — Aggregate root identity and FlightPlan lifecycle

```java
@Entity
@Table(name = "FLIGHT")
public class Flight implements AggregateRoot<FlightDesignator> {

    @EmbeddedId
    private FlightDesignator designator;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightPlan> flightPlans = new ArrayList<>();

    public FlightPlan addFlightPlan(final FlightPlanId flightPlanId, final String dslContent) {
        final var plan = new FlightPlan(this, flightPlanId, dslContent);
        this.flightPlans.add(plan);
        return plan;
    }

    public Optional<FlightPlan> flightPlan(final FlightPlanId flightPlanId) {
        return flightPlans.stream()
                .filter(fp -> fp.identity().equals(flightPlanId))
                .findFirst();
    }
}
```

**Why SRP?** `Flight` only manages its `FlightDesignator` identity and the collection of `FlightPlan` children. It does NOT validate DSL, format JSON, invoke the simulator, or parse reports.

#### `FlightPlan` — Status transitions and test result storage

```java
public void markAsInTest() {
    if (status != FlightPlanStatus.DRAFT) {
        throw new IllegalStateException(
                "Only DRAFT flight plans can be submitted for testing");
    }
    this.status = FlightPlanStatus.IN_TEST;
}

public void recordTestResult(final boolean passed,
                              final String reportFilePath,
                              final String reportContent) {
    if (passed) {
        markAsTestPassed();
    } else {
        markAsTestFailed();
    }
    this.lastTestedAt = LocalDateTime.now();
    this.reportFilePath = reportFilePath;
    this.reportContent = reportContent;
}
```

**Why SRP?** `FlightPlan` knows only about its own state (DRAFT → IN_TEST → TEST_PASSED/TEST_FAILED), DSL content, and test results. It does not know about JSON, C simulators, or repositories.

#### `TestFlightPlanController` — Orchestration only

```java
@UseCaseController
public class TestFlightPlanController {

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final FlightPlanExporter exporter;
    private final SimulationRunner runner;
    private final DslValidator dslValidator;

    public TestResult testFlightPlan(final String flightDesignatorStr,
                                      final String flightPlanIdStr) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final var flightDesignator = FlightDesignator.valueOf(flightDesignatorStr);
        final var flight = flightRepo.ofIdentity(flightDesignator).orElseThrow(...);
        final var flightPlanId = FlightPlanId.valueOf(flightPlanIdStr);
        final var flightPlan = flight.flightPlan(flightPlanId).orElseThrow(...);
        return executeTest(flight, flightPlan);
    }

    private TestResult executeTest(final Flight flight, final FlightPlan flightPlan) {
        // Delegates each step — contains NO business logic
        if (flightPlan.status() != FlightPlanStatus.DRAFT) return failure(...);
        final var dslValidation = dslValidator.validate(flightPlan.dslContent());
        if (!dslValidation.isPassed()) { /* markAsInTest + recordTestResult(false) + save */ }
        flightPlan.markAsInTest(); flightRepo.save(flight);
        final var json = exporter.exportForSimulator(flightPlan);
        final var report = runner.run(json); // throws on failure
        final var parsed = ReportParser.parse(report);
        flightPlan.recordTestResult(parsed.isPassed(), null, report);
        flightRepo.save(flight);
        return new TestResult(parsed.isPassed(), msg, report);
    }
}
```

**Why SRP?** The controller only **orchestrates** — it does NOT validate DSL, generate JSON, run processes, or parse text. Each of those tasks belongs to a dedicated service.

#### Other service classes and their single reasons:

| Class | Single Responsibility |
|-------|----------------------|
| `ImportFlightPlanController.importFlightPlan()` | ANTLR 3-phase pipeline at import time; creates FlightPlan with DRAFT status |
| `FlightPlanToScenarioConverter.convert()` | Parses DSL with ANTLR; outputs structured scenario JSON (legs, coordinates, flight profile) |
| `DslValidator.validate()` | Checks DSL content rules (altitude, speed, waypoint, engine, wake, passengers, non-ASCII) |
| `FlightPlanExporter.exportForSimulator()` | Serializes FlightPlan → JSON; uses `FlightPlanToScenarioConverter` with legacy fallback |
| `SimulationRunner.run()` / `run(json, weatherFile)` | Interface — invokes C simulator (local or remote) |
| `SocketSimulationRunner.run()` | TCP client — sends JSON to sim_server, receives report |
| `ProcessBuilderSimulationRunner.run()` | Local subprocess — invokes C simulator via `ProcessBuilder` with temp files |
| `ReportParser.parse()` | Parses C simulator text output (PASS/FAIL + violation count + report type) |
| `ValidationResult` | Encapsulates pass/fail outcome with reasons |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: `FlightPlanStatus` enum — new statuses via extension

```java
public enum FlightPlanStatus {
    DRAFT,
    IN_TEST,
    TEST_PASSED,
    TEST_FAILED
}
```

Adding a new status (e.g. `ON_HOLD`) requires only adding a constant to the enum and adding a new guard clause in `FlightPlan` — no existing code is modified.

#### Example 2: `ValidationResult` — static factory methods

```java
public static ValidationResult passed() {
    return new ValidationResult(true, List.of());
}

public static ValidationResult failed(final String reason) {
    return new ValidationResult(false, List.of(reason));
}
```

New creation patterns (e.g. `passedWithWarnings(...)`) can be added as new factory methods without changing callers.

#### Example 3: `FlightPlan` transition methods — guarded by enum comparison

```java
public void markAsTestPassed() {
    if (status != FlightPlanStatus.IN_TEST) {  // ← only reference to enum constant
        throw new IllegalStateException("Only IN_TEST flight plans can be marked as passed");
    }
    this.status = FlightPlanStatus.TEST_PASSED;
}
```

If a new status is inserted between DRAFT and IN_TEST, only the guard condition in `markAsInTest()` changes — all other transition methods remain untouched.

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example 1: `FlightRepository` — JPA and InMemory are interchangeable

```java
// Interface (abstraction)
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    Optional<Flight> findByFlightPlanId(FlightPlanId flightPlanId);
}

// JPA implementation
public class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightDesignator, FlightDesignator>
        implements FlightRepository {
    @Override
    public Optional<Flight> findByFlightPlanId(final FlightPlanId flightPlanId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("id", flightPlanId.toString());
        return matchOne("SELECT f FROM Flight f JOIN f.flightPlans fp WHERE fp.flightPlanId.id = :id", params);
    }
}

// InMemory implementation
public class InMemoryFlightRepository
        extends InMemoryDomainRepository<Flight, FlightDesignator>
        implements FlightRepository {
    @Override
    public Optional<Flight> findByFlightPlanId(final FlightPlanId flightPlanId) {
        return StreamSupport.stream(match(f -> f.flightPlans().stream()
                .anyMatch(fp -> fp.identity().equals(flightPlanId)))
                .spliterator(), false).findFirst();
    }
}
```

**Why LSP?** The controller (`TestFlightPlanController`) depends on `FlightRepository` (the interface). Whether the actual instance is `JpaFlightRepository` or `InMemoryFlightRepository` is irrelevant — both satisfy the contract. The controller never needs to know.

#### Example 2: `FlightDesignator` implements `ValueObject` + `Comparable`

```java
@Embeddable
public class FlightDesignator implements ValueObject, Comparable<FlightDesignator> {
    // ...
    @Override
    public int compareTo(final FlightDesignator other) {
        return this.designator.compareTo(other.designator);
    }
}
```

It can be used anywhere the eapli framework expects a `ValueObject` or a `Comparable` (sorted collections, `DomainRepository` identity matching).

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example 1: `FlightRepository` — minimal addition to framework interface

```java
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    // Only ONE custom method added
    Optional<Flight> findByFlightPlanId(FlightPlanId flightPlanId);
}
```

`DomainRepository` already provides `findAll()`, `ofIdentity()`, `save()`, etc. `FlightRepository` adds exactly one method needed by the controller — no more.

#### Example 2: `DslValidator` — single method

```java
public class DslValidator {
    public ValidationResult validate(final String dslContent) {
        // basic semantic check: empty, starts with "departure", contains ";"
    }
}
```

A single, focused method with a single return type. No configuration, no lifecycle methods, no callbacks.

#### Example 3: `ReportParser` — static single method + record

```java
public class ReportParser {
    public static ReportParseResult parse(final String reportContent) {
        // parses "RESULT: PASS|FAIL" and "Total violations detected: N"
    }

    public record ReportParseResult(boolean isPassed, int violationCount, String rawOutput) {}
}
```

The caller only sees one method and one record type. No factory, no builder, no configuration dependencies.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### Example: `TestFlightPlanController` depends entirely on abstractions

```java
@UseCaseController
public class TestFlightPlanController {

    // All dependencies are ABSTRACT types (interfaces or services):
    private final AuthorizationService authz;         // interface (framework)
    private final FlightRepository flightRepo;         // interface (our code)
    private final FlightPlanExporter exporter;          // concrete service (no I/O)
    private final SimulationRunner runner;              // interface (two implementations)

    // Package-private constructor — accepts abstractions
    TestFlightPlanController(final AuthorizationService authz,
                              final FlightRepository flightRepo,
                              final FlightPlanExporter exporter,
                              final SimulationRunner runner,
                              final DslValidator dslValidator) {
        this.authz = authz;
        this.flightRepo = flightRepo;
        this.exporter = exporter;
        this.runner = runner;
        this.dslValidator = dslValidator;
    }
}
```

**Why DIP?**
- `FlightRepository` is an **interface** — the controller never knows about `JpaFlightRepository` or `InMemoryFlightRepository`
- `AuthorizationService` is a **framework interface** — the controller doesn't instantiate authz logic
- `SimulationRunner` is an **interface** (not concrete) — the controller never knows about `SocketSimulationRunner` or `ProcessBuilderSimulationRunner`
- Even though `FlightPlanExporter` and `DslValidator` are concrete classes (not interfaces), they are **stateless services with no I/O dependencies** — the principle of abstraction is maintained because their implementations are decoupled from the controller's orchestration logic

The **default constructor** uses dependency injection via static factories:
```java
public TestFlightPlanController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().flights(),  // ← returns FlightRepository
            new FlightPlanExporter(),
            createRunner(),
            new DslValidator());
}
```

---

## GRASP Principles

---

### Controller (GRASP)

> *Assign the responsibility of handling system events to a non-UI class that represents the overall use case.*

**Applied by:** `ImportFlightPlanController` (import use case), `TestFlightPlanController` (test use case)

```java
@UseCaseController
public class TestFlightPlanController {
    // Orchestrates the entire validation pipeline:
    public TestResult testFlightPlan(final String flightDesignator, final String flightPlanId) {
        authz.ensureAuthenticatedUserHasAnyOf(...);           // 1. Authorize
        final var flight = flightRepo.ofIdentity(...).get();  // 2. Load flight
        final var plan = flight.flightPlan(...).get();        // 3. Load plan
        if (plan.status() != DRAFT) return failure(...);      // 4. Check status
        if (!dslValidator.validate(...).isPassed()) { ... }   // 5. Validate DSL
        flightPlan.markAsInTest(); flightRepo.save(flight);   // 6. Mark in test
        final var json = exporter.exportForSimulator(plan);   // 7. Export JSON
        final var report = runner.run(json);                  // 8. Invoke C sim
        final var parsed = ReportParser.parse(report);        // 9. Parse result
        flightPlan.recordTestResult(parsed.isPassed(), ...);  // 10. Record
        flightRepo.save(flight);                              // 11. Persist
        return new TestResult(parsed.isPassed(), msg, report);// 12. Return
    }
}
```

The controller is the single point of entry for the "test flight plan" system operation. It delegates each step to the appropriate domain/service class.

---

### Creator (GRASP)

> *Assign responsibility for creating instances of class A to class B when B contains, aggregates, records, or closely uses A.*

| Creator Class | Creates | Rationale |
|--------------|---------|-----------|
| `Flight` | `FlightPlan` | `Flight` aggregates `FlightPlan` (`@OneToMany`) — the aggregate root is the natural factory for its children |
| `ImportFlightPlanController` | `FlightPlan` (via Flight) | Controller orchestrates ANTLR validation and then calls `Flight.addFlightPlan()` to create the plan |
| `FlightPlanToScenarioConverter` | JSON `String` | Converter is the expert on ANTLR parsing and structured JSON format for the C simulator |
| `ValidationResult.passed()` / `.failed()` | `ValidationResult` | Static factory methods: the class creates itself with the correct initial state |
| `FlightPlanExporter` | JSON `String` | Exporter delegates to converter or creates legacy format |
| `ReportParser.parse()` | `ReportParseResult` | Parser creates the result record from raw text |

```java
// Flight creates and adds FlightPlan to its own collection
public FlightPlan addFlightPlan(final FlightPlanId flightPlanId, final String dslContent) {
    final var plan = new FlightPlan(this, flightPlanId, dslContent);  // ← Creator
    this.flightPlans.add(plan);
    return plan;
}

// ValidationResult creates itself via factory
var success = ValidationResult.passed();    // ← Creator (self)
var failure = ValidationResult.failed("invalid syntax");  // ← Creator (self)
```

---

### Information Expert (GRASP)

> *Assign a responsibility to the class that has the information needed to fulfill it.*

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Status transitions (DRAFT → IN_TEST → ...) | `FlightPlan` | FlightPlan owns its `status` field |
| DSL content storage and retrieval | `FlightPlan` | FlightPlan stores `dslContent` |
| ID format validation | `FlightPlanId` | FlightPlanId knows its own format rules |
| Pass/fail semantics with reasons | `ValidationResult` | ValidationResult encapsulates `passed` boolean and reasons list |
| ANTLR 3-phase validation and FlightDesignator extraction | `ImportFlightPlanController` | Controller orchestrates ANTLR lexer, parser, semantic validation and extracts flight designator from parse tree |
| Structured scenario JSON for C simulator | `FlightPlanToScenarioConverter` | Converter knows the exact ANTLR parse tree structure and the C simulator's expected JSON schema |
| Legacy JSON serialization format | `FlightPlanExporter` | Exporter knows both converter and fallback format |
| C simulator report format | `ReportParser` | Parser knows the regex patterns for "RESULT: PASS/FAIL" and "Total violations detected: N" |
| Flight identity | `FlightDesignator` | FlightDesignator holds the `xxn(n)(n)(n)(a` format rules |

```java
// FlightPlan is the expert on its own status
public void recordTestResult(final boolean passed, ...) {
    if (passed) markAsTestPassed(); else markAsTestFailed();  // ← expert decision
    this.lastTestedAt = LocalDateTime.now();
    this.reportContent = reportContent;
}
```

---

### High Cohesion (GRASP)

> *Keep responsibilities strongly related and focused within each class.*

```
Low Cohesion (bad):                          High Cohesion (our design):
┌──────────────────────────────┐             ┌──────────────────────┐
│  MegaController              │             │  FlightPlan          │
│  - validateDSL()             │             │  - markAsInTest()    │
│  - exportJSON()              │             │  - recordTestResult()│
│  - runSimulator()            │             │  - status()          │
│  - parseReport()             │             └──────────────────────┘
│  - saveToDatabase()          │
│  - sendEmail()               │             ┌──────────────────────┐
└──────────────────────────────┘             │  FlightPlanExporter  │
                                              │  - exportForSim()    │
Each class in our design is cohesive:        │                       │
                                              └──────────────────────┘
• Domain: FlightPlan, FlightPlanId, etc.     ┌──────────────────────┐
• Export: FlightPlanExporter                 │  ReportParser        │
• Process: SimulationRunner implementations │  - parse()           │
• Parse: ReportParser                        └──────────────────────┘
• Orchestrate: TestFlightPlanController
```

---

### Low Coupling (GRASP)

> *Keep dependencies between classes as weak as possible.*

```
TestFlightPlanController
    │
    ├──→ FlightRepository        (interface — switchable JPA ↔ InMemory)
    ├──→ AuthorizationService    (interface — framework abstraction)
    ├──→ FlightPlanExporter      (stateless service, no side effects)
    ├──→ SimulationRunner (interface — SocketSimulationRunner or ProcessBuilderSimulationRunner)
    └──→ DslValidator            (stateless, pure function)
```

```java
// The controller never imports JPA or InMemory classes:
import eapli.aisafe.flight.repositories.FlightRepository;  // ← interface only
import eapli.framework.infrastructure.authz.application.AuthorizationService;  // ← interface
```

Domain classes (`Flight`, `FlightPlan`, `FlightPlanId`, `ValidationResult`) have **zero dependencies** on infrastructure, application, or other domain aggregates. They depend only on the eapli framework (`@Entity`, `AggregateRoot`, `ValueObject`, `Preconditions`).

---

### Polymorphism (GRASP)

> *Use polymorphic operations to handle variations in behavior based on type.*

#### Enum-based state machine

```java
public enum FlightPlanStatus {
    DRAFT,
    IN_TEST,
    TEST_PASSED,
    TEST_FAILED
}

// FlightPlan uses polymorphic enum comparison for state transitions:
public void markAsInTest() {
    if (status != FlightPlanStatus.DRAFT) {  // ← polymorphic: behavior depends on current status
        throw new IllegalStateException("Only DRAFT flight plans can be submitted for testing");
    }
    this.status = FlightPlanStatus.IN_TEST;
}
```

#### Repository polymorphism

```java
// Both implementations share the same interface — the controller exercises them polymorphically:
FlightRepository repo = isTestEnvironment()
        ? new InMemoryFlightRepository()   // ← tests use this
        : new JpaFlightRepository("aisafe");   // ← production uses this
```

---

### Protected Variations (GRASP)

> *Protect the system from variations in external components by wrapping them behind stable interfaces.*

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| C simulator output format | `ReportParser` | Changes to report delimiters, keywords, or format affect only `ReportParser.parse()` |
| Structured JSON format for C simulator | `FlightPlanToScenarioConverter` | Changes to the structured scenario JSON schema affect only `convert()` |
| Legacy fallback JSON format | `FlightPlanExporter` | Changes to the simple `{ID, FlightPlanDSL}` format affect only `exportForSimulator()` |
| Subprocess execution | `ProcessBuilderSimulationRunner` / `SocketSimulationRunner` | Changes to local process or TCP protocol affect only each implementation's `run()` |
| DSL validation rules | `DslValidator` + `ImportFlightPlanController` | Grammar changes, new semantic checks affect only the ANTLR pipeline |
| ANTLR grammar evolution | `FlightPlan.g4` + `ImportFlightPlanController` | Grammar changes are isolated in the `aisafe.dsl` module; controller only consumes parse results |
| Persistence technology | `FlightRepository` interface | Switching JPA ↔ file ↔ NoSQL means swapping the implementation class, not touching the controller |

```java
// The controller is protected from ALL these variations:
private TestResult executeTest(...) {
    // If the C simulator changes its JSON schema → only FlightPlanExporter changes
    final var json = exporter.exportForSimulator(flightPlan);
    // If the simulator mode changes (local → remote TCP) → only the runner implementation changes
    final var report = runner.run(json);
    // If the report format changes → only ReportParser changes
    final var parsed = ReportParser.parse(report);
    // → Controller code stays identical
}
```

---

### Pure Fabrication (GRASP)

> *Create artificial classes that do not represent domain concepts to achieve low coupling and high cohesion.*

These classes have no counterpart in the domain model — they are pure fabrications created to keep domain classes clean:

| Class | Why It's a Fabrication |
|-------|----------------------|
| `ImportFlightPlanController` | The domain has no "import controller" — it's a use-case artifact orchestrating ANTLR validation and flight creation |
| `TestFlightPlanController` | The domain has no "controller" — it's a use-case artifact to orchestrate the pipeline |
| `FlightPlanToScenarioConverter` | JSON conversion is an infrastructure concern (C simulator format), not a domain concept |
| `FlightPlanExporter` | Serialization is an infrastructure concern, not a domain concept |
| `ProcessBuilderSimulationRunner` | OS-level process management is pure infrastructure |
| `SocketSimulationRunner` | TCP socket communication is pure infrastructure |
| `ReportParser` | Text parsing is a technical detail, not flight operations |
| `DslValidator` | DSL validation is a LPROG concern, not a Flight domain concept |

```java
// Without Pure Fabrication, FlightPlan would need to know about JSON:
public class FlightPlan {
    public String toJson() { ... }  // ← BAD! Domain entity knows about serialization format
}

// With Pure Fabrication:
public class FlightPlanExporter {  // ← PURE FABRICATION
    public String exportForSimulator(final FlightPlan flightPlan) {
        // JSON logic lives here, not in FlightPlan
    }
}
```

---

### Indirection (GRASP)

> *Assign the responsibility of mediating between two components to an intermediate object.*

| Mediator | Mediates Between | Why |
|----------|-----------------|-----|
| `FlightRepository` | Controller ↔ Persistence (JPA / InMemory) | Controller never touches JPA code |
| `ImportFlightPlanController` | Import UI ↔ ANTLR FlightPlanRunner + FlightRepository | UI never knows about ANTLR or repository persistence |
| `FlightPlanToScenarioConverter` | FlightPlan ↔ structured JSON for C simulator | FlightPlan never knows about C simulator's JSON schema |
| `FlightPlanExporter` | Controller ↔ JSON string (converter or legacy) | FlightPlan never formats itself |
| `SimulationRunner` (interface) | Controller ↔ C simulator (local or remote) | Controller never calls `ProcessBuilder` or `Socket` directly |
| `ProcessBuilderSimulationRunner` | Controller ↔ OS Process | Concrete subprocess implementation |
| `SocketSimulationRunner` | Controller ↔ TCP sim_server | Concrete socket implementation |
| `DslValidator` | Controller ↔ ANTLR grammar | Controller never imports ANTLR directly |

```java
// Without Indirection: controller calls ProcessBuilder or Socket directly
public class TestFlightPlanController {
    public TestResult testFlightPlan(...) {
        var pb = new ProcessBuilder("aisafe-simulator", input.toString());  // ← BAD! tight coupling
        var process = pb.start();
        // or worse: new Socket("vm-host", 9999) ...
    }
}

// With Indirection:
public interface SimulationRunner {               // ← INDIRECTION (interface)
    String run(String jsonInput);
    default String run(String jsonInput, String weatherPath) { return run(jsonInput); }
}

public class ProcessBuilderSimulationRunner implements SimulationRunner {  // ← one impl
    public String run(final String jsonInput) { ... }
}

public class SocketSimulationRunner implements SimulationRunner {  // ← another impl
    public String run(final String jsonInput) { ... }
}

public class TestFlightPlanController {
    public TestResult testFlightPlan(...) {
        var report = runner.run(json);  // ← controller never knows about ProcessBuilder or Socket
    }
}
```

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── flight/
│   ├── domain/
│   │   ├── Flight.java              (Entity, AggregateRoot)
│   │   └── FlightDesignator.java    (ValueObject, @Embeddable)
│   └── repositories/
│       └── FlightRepository.java    (Interface)
├── flightplan/
│   ├── domain/
│   │   ├── FlightPlan.java          (Entity, inside Flight aggregate)
│   │   ├── FlightPlanId.java        (ValueObject, @Embeddable)
│   │   ├── FlightPlanStatus.java    (Enum)
│   │   └── ValidationResult.java    (ValueObject)
│   └── application/
│       ├── ImportFlightPlanController.java     (@UseCaseController)
│       ├── TestFlightPlanController.java       (@UseCaseController)
│       ├── FlightPlanExporter.java             (Service)
│       ├── FlightPlanToScenarioConverter.java  (Service)
│       ├── SimulationRunner.java                    (Interface)
│       ├── SocketSimulationRunner.java             (Service)
│       ├── ProcessBuilderSimulationRunner.java      (Service)
│       ├── ReportParser.java                   (Service)
│       └── DslValidator.java                   (Service)
```

### Layer Responsibilities

| Layer | Contains | Dependencies |
|-------|----------|-------------|
| **Domain** | Entities, VOs, Enums | eapli framework only |
| **Application** | Controllers, Services | Domain + infrastructure abstractions |
| **Infrastructure** | Persistence (JPA, InMemory) | All of the above |

### Layer Dependency Rules

```
Controller → Service (application) → Domain (domain)
Controller → Repository (interface, application layer boundary)
Repository implementation (infrastructure) → Repository interface
```

No domain class imports any application or infrastructure class.
No service class imports any infrastructure class (except through interfaces).

---

## GoF Design Patterns

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable."*

**Applied by:** `SimulationRunner` — the most prominent GoF pattern in US085.

```java
// Strategy interface:
public interface SimulationRunner {
    String run(String jsonInput);
    default String run(String jsonInput, String weatherPath) { return run(jsonInput); }
}

// Concrete Strategy A — local subprocess (default):
public class ProcessBuilderSimulationRunner implements SimulationRunner {
    @Override
    public String run(final String jsonInput) {
        // invokes C simulator via ProcessBuilder + temp files
    }
}

// Concrete Strategy B — remote TCP connection:
public class SocketSimulationRunner implements SimulationRunner {
    @Override
    public String run(final String jsonInput) {
        // sends JSON to sim_server over TCP, reads report
    }
}

// Context (controller) — completely unaware of which runner is in use:
public TestResult testFlightPlan(...) {
    final var json   = exporter.exportForSimulator(flightPlan);
    final var report = runner.run(json);   // ← same call for both strategies
    final var parsed = ReportParser.parse(report);
    ...
}
```

**Why Strategy?** The simulation algorithm (local subprocess vs. remote TCP) is selected at construction time and swapped without any change to `TestFlightPlanController`. The production default (`createRunner()`) chooses based on configuration; tests can inject a mock or stub `SimulationRunner`. Neither strategy knows about the other, and the controller never knows which one is active.

---

### Factory Method (Creational)

> *"Define an interface for creating an object, but let subclasses or factory methods decide which class to instantiate."*

**Applied by:** `ValidationResult`, `FlightPlanId`, `FlightDesignator`

```java
// ValidationResult — static factory methods hide constructor and set correct initial state:
public static ValidationResult passed() {
    return new ValidationResult(true, List.of());       // ← Factory Method
}

public static ValidationResult failed(final String reason) {
    return new ValidationResult(false, List.of(reason)); // ← Factory Method
}

// Callers use intention-revealing names, not `new ValidationResult(true, ...)`:
var ok  = ValidationResult.passed();
var nok = ValidationResult.failed("DSL is empty");

// FlightPlanId and FlightDesignator follow the same idiom:
final var flightPlanId     = FlightPlanId.valueOf(flightPlanIdStr);      // ← Factory Method
final var flightDesignator = FlightDesignator.valueOf(designatorStr);    // ← Factory Method
```

The `TestFlightPlanController` default constructor also uses factory methods:

```java
public TestFlightPlanController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().flights(),  // ← Factory Method
            new FlightPlanExporter(),
            createRunner(),                               // ← Factory Method (chooses strategy)
            new DslValidator());
}
```

**Why Factory Method?** Callers are decoupled from the concrete construction details of VOs and services. `createRunner()` centralises the decision of which `SimulationRunner` to instantiate, keeping that variation in one place.

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client."*

**Applied by:** `JpaFlightRepository` adapts the EAPLI JPA framework to `FlightRepository`.

```java
// Target — our domain interface:
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    Optional<Flight> findByFlightPlanId(FlightPlanId flightPlanId);
    boolean existsByPilotLicense(PilotId pilotId);
}

// Adapter — translates domain method calls into EAPLI JPA framework calls:
public class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightDesignator, FlightDesignator>  // ← Adaptee
        implements FlightRepository {                                              // ← Target

    @Override
    public Optional<Flight> findByFlightPlanId(final FlightPlanId id) {
        return matchOne(
            "SELECT f FROM Flight f JOIN f.flightPlans fp WHERE fp.flightPlanId.id = :id",
            Map.of("id", id.toString()));  // ← adapts domain method to framework matchOne()
    }
}
```

`TestFlightPlanController` depends only on `FlightRepository` (target). The JPA framework API (`matchOne`, `match`) is hidden inside the adapter.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `flight.flightPlans()` — iterating the `FlightPlan` collection inside `Flight`.

```java
// Flight returns an unmodifiable view of its FlightPlan children:
public List<FlightPlan> flightPlans() {
    return Collections.unmodifiableList(flightPlans);  // ← Iterator-backed List
}

// TestFlightPlanController traverses a flight's plans without knowing the storage structure:
for (final var fp : flight.flightPlans()) {    // ← Iterator pattern
    switch (fp.status()) {
        case DRAFT      -> draft++;
        case TEST_PASSED -> passed++;
        // ...
    }
}
```

**Why Iterator?** `TestFlightPlanController` never depends on whether `flightPlans` is an `ArrayList`, a `LinkedList`, or a JPA-managed `PersistentBag`. The `List<FlightPlan>` interface (which extends `Iterable`) provides the Iterator abstraction.

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem."*

**Applied by:** `TestFlightPlanController`

```java
// Subsystem classes hidden behind the controller (Facade):
//   FlightRepository, DslValidator, FlightPlanExporter,
//   SimulationRunner (2 implementations), ReportParser, FlightPlan status machine

// The UI calls ONE method and gets a self-contained result:
TestFlightPlanController controller = new TestFlightPlanController();
TestResult result = controller.testFlightPlan(designatorStr, flightPlanIdStr);

// Behind the scenes (12 steps, 6 collaborators):
// 1. authz.ensureAuthenticatedUserHasAnyOf(...)
// 2. FlightDesignator.valueOf(designatorStr)
// 3. flightRepo.ofIdentity(flightDesignator).orElseThrow(...)
// 4. FlightPlanId.valueOf(flightPlanIdStr)
// 5. flight.flightPlan(flightPlanId).orElseThrow(...)
// 6. if status != DRAFT → return failure(...)
// 7. dslValidator.validate(flightPlan.dslContent())
// 8. flightPlan.markAsInTest(); flightRepo.save(flight)
// 9. exporter.exportForSimulator(flightPlan) → JSON string
// 10. runner.run(json)               → C simulator report
// 11. ReportParser.parse(report)
// 12. flightPlan.recordTestResult(...); flightRepo.save(flight)
```

**Why Facade?** Without the controller, the UI would need to import and orchestrate `FlightRepository`, `DslValidator`, `FlightPlanExporter`, `SimulationRunner`, and `ReportParser` — five subsystem classes across three packages. The controller Facade reduces this to a single method call with a single return type.
