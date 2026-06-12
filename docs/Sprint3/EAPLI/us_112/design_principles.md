# US112 — SOLID & GRASP & GoF Principles Applied

Application to the **Generate Monthly Report** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

#### `GenerateMonthlyReportController` — Orchestration only

```java
@UseCaseController
public class GenerateMonthlyReportController {

    public MonthlyReport generateForMonth(final YearMonth period) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final AreaCode areaCode = areaCodeOfCurrentUser();  // resolves current user → area
        return provider.generateForMonth(period, areaCode); // delegates to data provider
    }
}
```

**Why SRP?** The controller only authorises, resolves the user's area code, and delegates. It does NOT query flights, count flight plans, filter weather records, or format text.

#### `DatabaseMonthlyReportDataProvider` — Data aggregation only

```java
public class DatabaseMonthlyReportDataProvider implements MonthlyReportDataProvider {

    @Override
    public MonthlyReport generateForMonth(final YearMonth period, final AreaCode areaCode) {
        // Iterates flights, counts flight plans by status, counts weather records,
        // counts active pilots, counts aircraft, computes flights-per-week string
        return new MonthlyReport(period, totalFlights, totalFlightPlans,
                draft, inTest, passed, failed, weatherInPeriod,
                activePilots, totalAircraft, flightsPerWeek);
    }
}
```

**Why SRP?** `DatabaseMonthlyReportDataProvider` only aggregates raw data from multiple repositories. It does NOT authorise, format the output for the UI, or persist anything.

#### `MonthlyReport` — Presentation of report data only

```java
public class MonthlyReport implements ValueObject {
    // holds all report fields; formats itself as a text block
    @Override
    public String toString() {
        // builds the formatted report string with separators, percentages, tree structure
    }
}
```

**Why SRP?** `MonthlyReport` only stores and formats the report content. It does NOT query repositories, validate business rules, or communicate over the network.

#### `MonthlyReportDataProvider` — Contract for data generation only

```java
public interface MonthlyReportDataProvider {
    MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode);
}
```

**Why SRP?** Defines a single method contract. No lifecycle, no configuration, no secondary responsibilities.

| Class | Single Responsibility |
|-------|----------------------|
| `GenerateMonthlyReportController` | Authorise + resolve user area + delegate to provider |
| `DatabaseMonthlyReportDataProvider` | Query four repositories and aggregate counts for the period |
| `MonthlyReport` | Store and format the monthly statistics |
| `MonthlyReportDataProvider` | Contract: given a period and area, produce a `MonthlyReport` |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: New data providers by extension

```java
// Interface is the stable target:
public interface MonthlyReportDataProvider {
    MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode);
}

// New provider (e.g., from a REST API or cache) extends the interface, not modifies it:
public class CachedMonthlyReportDataProvider implements MonthlyReportDataProvider {
    @Override
    public MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode) {
        // check cache first, fall back to database
    }
}
```

Adding `CachedMonthlyReportDataProvider` requires no change to `GenerateMonthlyReportController` or `DatabaseMonthlyReportDataProvider`.

#### Example 2: New `MonthlyReport` fields by extension

Adding a new statistic (e.g. `totalCancelledFlights`) requires:
1. Adding the field to `MonthlyReport`
2. Computing it in `DatabaseMonthlyReportDataProvider`
3. Including it in `toString()`

No existing field or method is modified.

#### Example 3: New flight plan statuses by extension

```java
switch (fp.status()) {
    case DRAFT      -> draft++;
    case IN_TEST    -> inTest++;
    case TEST_PASSED -> passed++;
    case TEST_FAILED -> failed++;
    // Adding ON_HOLD: case ON_HOLD -> onHold++; ← extension only
}
```

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example 1: `MonthlyReportDataProvider` — implementations are interchangeable

```java
// Interface:
public interface MonthlyReportDataProvider {
    MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode);
}

// DatabaseMonthlyReportDataProvider (production):
public class DatabaseMonthlyReportDataProvider implements MonthlyReportDataProvider { ... }

// Hypothetical InMemoryMonthlyReportDataProvider (tests):
public class InMemoryMonthlyReportDataProvider implements MonthlyReportDataProvider {
    @Override
    public MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode) {
        // uses in-memory data, same contract
    }
}
```

**Why LSP?** `GenerateMonthlyReportController` depends only on `MonthlyReportDataProvider`. Whether the actual instance is `DatabaseMonthlyReportDataProvider` or a test stub, the controller's behaviour is identical.

#### Example 2: All repositories — JPA and InMemory are interchangeable

`DatabaseMonthlyReportDataProvider` depends on `FlightRepository`, `WeatherDataRepository`, `PilotRepository`, `AircraftRepository` — all interfaces. Both JPA and InMemory implementations satisfy the same contracts.

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example: `MonthlyReportDataProvider` — single-method interface

```java
public interface MonthlyReportDataProvider {
    MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode);
}
```

`GenerateMonthlyReportController` depends on this single-method interface. It is not forced to depend on methods for caching, configuration, lifecycle management, or separate counting operations.

#### Example: `DatabaseMonthlyReportDataProvider` uses minimal surface of each repository

```java
// From FlightRepository:    findAll()
// From WeatherDataRepository: findByAreaCode(areaCode)
// From PilotRepository:     findAll()
// From AircraftRepository:  findAll()
```

The provider uses only the `findAll()` and `findByAreaCode()` methods — `DomainRepository` provides the rest. No custom methods added solely for US112.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### `GenerateMonthlyReportController` depends on the `MonthlyReportDataProvider` abstraction

```java
@UseCaseController
public class GenerateMonthlyReportController {

    // Dependencies are ABSTRACT types:
    private final AuthorizationService authz;         // interface
    private final CollaboratorRepository collaboratorRepo; // interface
    private final MonthlyReportDataProvider provider; // interface ← key abstraction

    // Package-private constructor — for test injection:
    GenerateMonthlyReportController(final AuthorizationService authz,
                                     final CollaboratorRepository collaboratorRepo,
                                     final MonthlyReportDataProvider provider) {
        this.authz = authz;
        this.collaboratorRepo = collaboratorRepo;
        this.provider = provider;
    }
}
```

**Why DIP?** The controller never knows whether the provider is `DatabaseMonthlyReportDataProvider`, a cached version, or a test stub. This is the key inversion: the high-level controller (`GenerateMonthlyReportController`) and the low-level implementation (`DatabaseMonthlyReportDataProvider`) both depend on the `MonthlyReportDataProvider` abstraction.

The default constructor injects the concrete implementation:

```java
public GenerateMonthlyReportController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().collaborators(),
            new DatabaseMonthlyReportDataProvider(          // ← concrete implementation
                    PersistenceContext.repositories().flights(),
                    PersistenceContext.repositories().weatherData(),
                    PersistenceContext.repositories().pilots(),
                    PersistenceContext.repositories().aircraft()));
}
```

---

## GRASP Principles

---

### Controller (GRASP)

**Applied by:** `GenerateMonthlyReportController`

```java
public MonthlyReport generateForMonth(final YearMonth period) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR); // 1. Authorize
    final AreaCode areaCode = areaCodeOfCurrentUser();                           // 2. Resolve area
    return provider.generateForMonth(period, areaCode);                          // 3. Delegate
}
```

The controller is the single point of entry. The UI never calls `DatabaseMonthlyReportDataProvider` directly.

---

### Creator (GRASP)

| Creator Class | Creates | Rationale |
|--------------|---------|-----------|
| `DatabaseMonthlyReportDataProvider` | `MonthlyReport` | Has all the aggregated count data needed for construction |
| `MonthlyReport` constructor | Computes `testedFlightPlans`, `passRatePercent` | Report computes derived fields from its own constructor arguments |

```java
// DatabaseMonthlyReportDataProvider creates MonthlyReport with all counts:
return new MonthlyReport(period, totalFlights, totalFlightPlans,
        draft, inTest, passed, failed, weatherInPeriod,
        activePilots, totalAircraft, flightsPerWeek);  // ← Creator

// MonthlyReport computes derived fields in its own constructor:
this.testedFlightPlans = flightPlansPassed + flightPlansFailed;  // ← Creator (self)
this.passRatePercent = testedFlightPlans > 0
        ? (double) flightPlansPassed / testedFlightPlans * 100.0
        : 0.0;  // ← Creator (self)
```

---

### Information Expert (GRASP)

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Counting flights, flight plans, and their statuses per period | `DatabaseMonthlyReportDataProvider` | Provider has access to `FlightRepository` and iterates flight plans |
| Counting weather records per area and period | `DatabaseMonthlyReportDataProvider` | Provider has `WeatherDataRepository` and `areaCode` |
| Counting active pilots | `DatabaseMonthlyReportDataProvider` | Provider has `PilotRepository`; `Pilot.isActive()` is the expert on active state |
| Counting total aircraft | `DatabaseMonthlyReportDataProvider` | Provider has `AircraftRepository` |
| Computing pass rate percentage | `MonthlyReport` | Report owns `flightPlansPassed` and `testedFlightPlans` |
| Formatting the report text | `MonthlyReport` | Report owns all fields — it is the expert on how to present them |
| Resolving user's area code | `GenerateMonthlyReportController` | Controller has access to `AuthzRegistry` + `CollaboratorRepository` |

```java
// MonthlyReport is the expert on its own derived fields and text format:
this.passRatePercent = testedFlightPlans > 0
        ? (double) flightPlansPassed / testedFlightPlans * 100.0 : 0.0;  // ← expert

@Override
public String toString() {
    sb.append(String.format("  Test Pass Rate : %.1f%%  (%d/%d tested)%n",
            passRatePercent, flightPlansPassed, testedFlightPlans));   // ← expert
}
```

---

### High Cohesion (GRASP)

```
GenerateMonthlyReportController:    DatabaseMonthlyReportDataProvider:    MonthlyReport:
  - authorise                          - count flights                        - store stats
  - resolve user area code             - count flight plans by status         - compute pass rate
  - delegate to provider               - count weather records                - format toString()
                                       - count active pilots
                                       - count aircraft
                                       - compute flights-per-week
```

Each class handles one tight cluster of related responsibilities.

---

### Low Coupling (GRASP)

```
GenerateMonthlyReportController
    │
    ├──→ MonthlyReportDataProvider      (interface — key abstraction)
    ├──→ CollaboratorRepository         (interface)
    └──→ AuthorizationService           (interface)

DatabaseMonthlyReportDataProvider
    │
    ├──→ FlightRepository               (interface)
    ├──→ WeatherDataRepository          (interface)
    ├──→ PilotRepository                (interface)
    └──→ AircraftRepository             (interface)

MonthlyReport ── no dependencies on any repository or controller
```

The `MonthlyReport` value object has **zero dependencies** on any repository, controller, or infrastructure class. It is a pure data-and-format object.

---

### Protected Variations (GRASP)

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| Data source (database vs. cache vs. REST) | `MonthlyReportDataProvider` interface | Swapping providers requires only changing the constructor injection in the default constructor |
| Flight plan status values | `switch` in `DatabaseMonthlyReportDataProvider` | New statuses add a `case` — existing counts unchanged |
| Report text format | `MonthlyReport.toString()` | Changing formatting affects only `toString()` — controller and provider are untouched |
| Week-of-year computation | `IsoFields.WEEK_OF_WEEK_BASED_YEAR` | Changing the calendar standard affects only the provider |
| Persistence technology | Repository interfaces | Switching JPA ↔ file ↔ NoSQL changes only repository implementations |

---

### Pure Fabrication (GRASP)

| Class | Why It's a Fabrication |
|-------|----------------------|
| `GenerateMonthlyReportController` | A "report controller" has no real-world aviation counterpart — it is a use-case artifact |
| `MonthlyReportDataProvider` | An interface for data provision is a technical contract, not a domain concept |
| `DatabaseMonthlyReportDataProvider` | A class that aggregates counts from four repositories is a technical service, not a domain entity |
| Repository interfaces | Persistence abstractions with no real-world counterpart |

---

## GoF Design Patterns

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable."*

**Applied by:** `MonthlyReportDataProvider` — the most prominent GoF pattern in US112.

```java
// Strategy interface:
public interface MonthlyReportDataProvider {
    MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode);
}

// Concrete Strategy A — production (queries four JPA repositories):
public class DatabaseMonthlyReportDataProvider implements MonthlyReportDataProvider {
    @Override
    public MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode) {
        // queries FlightRepository, WeatherDataRepository, PilotRepository, AircraftRepository
    }
}

// Concrete Strategy B — tests (in-memory data, fast, no database):
// e.g. a stub passed in the package-private constructor:
MonthlyReportDataProvider testProvider = (period, areaCode) ->
        new MonthlyReport(period, 5, 10, 2, 1, 5, 2, 20, 3, 8, "W24:3 | W25:2");

// Context (controller) — identical regardless of which strategy is active:
public MonthlyReport generateForMonth(final YearMonth period) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
    final AreaCode areaCode = areaCodeOfCurrentUser();
    return provider.generateForMonth(period, areaCode);  // ← same call for both strategies
}
```

**Why Strategy?** The data generation algorithm (database queries vs. cache vs. test stub) is completely decoupled from the controller's orchestration logic. Switching from a full database scan to a pre-computed cache requires only passing a different `MonthlyReportDataProvider` to the constructor — no change to `GenerateMonthlyReportController`.

---

### Factory Method (Creational)

> *"Define an interface for creating an object, but let subclasses or factory methods decide which class to instantiate."*

**Applied by:** `PersistenceContext.repositories().*()`, `MonthlyReport` constructor as a builder-like factory.

```java
// PersistenceContext is a factory for all repository instances:
public GenerateMonthlyReportController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().collaborators(),  // ← Factory Method
            new DatabaseMonthlyReportDataProvider(
                    PersistenceContext.repositories().flights(),      // ← Factory Method
                    PersistenceContext.repositories().weatherData(),  // ← Factory Method
                    PersistenceContext.repositories().pilots(),       // ← Factory Method
                    PersistenceContext.repositories().aircraft()));   // ← Factory Method
}
```

**Why Factory Method?** `PersistenceContext.repositories()` centralises the creation of all repository instances. The controller and provider never decide which concrete implementation to instantiate — the factory decides.

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client."*

**Applied by:** `JpaFlightRepository`, `JpaWeatherDataRepository`, `JpaPilotRepository`, `JpaAircraftRepository`

```java
// Target (our interface):
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> { ... }

// Adapter — translates domain method calls to EAPLI JPA framework:
public class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightDesignator, FlightDesignator>  // ← Adaptee
        implements FlightRepository {                                              // ← Target

    @Override
    public Iterable<Flight> findAll() {
        // adapts the framework's generic findAll to the domain interface
    }
}
```

**Why Adapter?** `DatabaseMonthlyReportDataProvider` depends on four repository interfaces (targets). The JPA framework (`JpaAutoTxRepository`) has a different internal API. The four JPA repository classes adapt the framework to the domain's expected interfaces — without the provider knowing anything about JPA.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `flightRepo.findAll()`, `pilotRepo.findAll()`, `aircraftRepo.findAll()`, `weatherRepo.findByAreaCode()`

```java
// Iterates all flights — Iterator pattern via Iterable<Flight>:
final Iterable<Flight> allFlights = flightRepo.findAll();
for (final Flight flight : allFlights) {                     // ← Iterator
    if (isInPeriod(flight, period)) {
        totalFlights++;
        for (final var fp : flight.flightPlans()) {           // ← Iterator (nested)
            switch (fp.status()) { ... }
        }
    }
}

// Filters weather records using stream (Iterator-backed):
final long weatherInPeriod = StreamSupport.stream(
        weatherRepo.findByAreaCode(areaCode).spliterator(), false)
        .filter(w -> YearMonth.from(w.recordedDateTime()).equals(period))
        .count();   // ← Iterator consumed by stream API

// Counts active pilots using stream:
final long activePilots = StreamSupport.stream(allPilots.spliterator(), false)
        .filter(Pilot::isActive)
        .count();   // ← Iterator consumed by stream API
```

**Why Iterator?** `DatabaseMonthlyReportDataProvider` traverses four different collections without knowing whether each is a JPA lazy cursor, an in-memory `ArrayList`, or a `TreeMap` entry set. The `Iterable<T>` / `Stream<T>` pattern is Java's built-in Iterator implementation — it enables seamless iteration over any data source.

Also, `flight.flightPlans()` returns `Collections.unmodifiableList(flightPlans)` — the provider iterates it via the Iterator pattern without ever modifying the flight's internal list.

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem."*

**Applied by:** `GenerateMonthlyReportController`

```java
// Subsystem classes hidden behind the controller:
//   CollaboratorRepository, AuthzRegistry, MonthlyReportDataProvider,
//   DatabaseMonthlyReportDataProvider, FlightRepository, WeatherDataRepository,
//   PilotRepository, AircraftRepository, IsoFields calendar logic

// The UI calls ONE method and gets a fully formatted report:
GenerateMonthlyReportController controller = new GenerateMonthlyReportController();
MonthlyReport report = controller.generateForMonth(YearMonth.of(2026, 5));
System.out.println(report);  // ← formatted multi-line report, ready to display
```

**Why Facade?** Without the controller, the UI would need to resolve the user's area code (querying `CollaboratorRepository` + `AuthzRegistry`), instantiate `DatabaseMonthlyReportDataProvider` with four repositories, call `generateForMonth()`, and handle any `IllegalStateException` for missing collaborator profiles. The controller Facade reduces all of this to a single method call.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── report/
│   ├── domain/
│   │   └── MonthlyReport.java                   (ValueObject — stores + formats report)
│   └── application/
│       ├── GenerateMonthlyReportController.java  (@UseCaseController)
│       ├── MonthlyReportDataProvider.java        (Strategy interface)
│       └── DatabaseMonthlyReportDataProvider.java (Concrete Strategy)
├── flight/
│   └── repositories/
│       └── FlightRepository.java                 (Interface — used by provider)
├── weatherdata/
│   └── repositories/
│       └── WeatherDataRepository.java            (Interface — used by provider)
├── pilot/
│   └── repositories/
│       └── PilotRepository.java                  (Interface — used by provider)
└── aircraft/
    └── repositories/
        └── AircraftRepository.java               (Interface — used by provider)
```

### Layer Dependency Rules

```
Controller → MonthlyReportDataProvider (interface — Strategy abstraction)
Controller → CollaboratorRepository (interface boundary)
DatabaseMonthlyReportDataProvider → Repository interfaces only (no controllers)
MonthlyReport → no dependencies (pure ValueObject)
```

`MonthlyReport` is a pure value object with no external dependencies.
`GenerateMonthlyReportController` never imports `DatabaseMonthlyReportDataProvider` directly — it only knows the `MonthlyReportDataProvider` interface.
`DatabaseMonthlyReportDataProvider` never imports any controller — it only knows repository interfaces.
