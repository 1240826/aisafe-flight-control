# US082 — SOLID & GRASP & GoF Principles Applied

Application to the **Add Weather Data to Flight** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

#### `Flight` — Aggregate root: owns the weather assignment and flight plan cascade

```java
public void assignWeatherData(final Long weatherDataId) {
    if (this.weatherDataId != null && this.weatherDataId.equals(weatherDataId)) {
        return;  // idempotent — no-op if already assigned
    }
    this.weatherDataId = weatherDataId;
    // cascade: reset previously-tested flight plans to DRAFT
    for (final FlightPlan fp : flightPlans) {
        if (fp.status() == FlightPlanStatus.TEST_PASSED
                || fp.status() == FlightPlanStatus.TEST_FAILED) {
            fp.resetToDraft();
        }
    }
}
```

**Why SRP?** `Flight` owns its own weather data reference and the business rule "assigning new weather invalidates tested flight plans". It does NOT find weather data, compute midpoints, or query ACA databases — those are the controller's responsibilities.

#### `AddWeatherToFlightController` — Orchestration only

```java
@UseCaseController
public class AddWeatherToFlightController {

    public Flight assignWeather(final String flightDesignator, final Long weatherDataId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        weatherRepo.ofIdentity(weatherDataId).orElseThrow(...);   // guard: weather exists
        final Flight flight = flightByDesignator(flightDesignator);
        flight.assignWeatherData(weatherDataId);                  // business logic in entity
        return flightRepo.save(flight);
    }
}
```

**Why SRP?** The controller only orchestrates — it authorises, validates existence, loads the flight, tells the entity to assign, and persists. The cascade reset logic lives in `Flight`, not here.

#### Other classes and their single reasons:

| Class | Single Responsibility |
|-------|----------------------|
| `Flight.assignWeatherData()` | Stores weather reference; cascades flight plan resets |
| `AddWeatherToFlightController.weatherDataForFlight()` | Computes midpoint and finds matching ACA weather records |
| `AddWeatherToFlightController.computeMidpoint()` | Geometry: maps route name → airport coords → geographic midpoint |
| `WeatherDataRepository` | Contract for weather data queries |
| `FlightRepository` | Contract for flight queries |
| `AirControlAreaRepository` | Contract for ACA geographic queries |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: New airport coordinate mappings by extension

```java
private static final Map<String, double[]> AIRPORT_COORDS = new HashMap<>();
static {
    AIRPORT_COORDS.put("LIS", new double[]{38.774, -9.134});
    AIRPORT_COORDS.put("OPO", new double[]{41.248, -8.681});
    // Adding a new airport: AIRPORT_COORDS.put("MXP", new double[]{45.630, 8.723});
    // ← extension (add entry), no modification of existing code
}
```

#### Example 2: `Flight.assignWeatherData()` — new cascade rules by adding conditions

If a new `FlightPlanStatus` is introduced (e.g. `ON_HOLD`), a new `case` is added to the cascade loop without modifying existing conditions.

#### Example 3: `FlightRepository` — new queries by extension

```java
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    boolean existsByPilotLicense(PilotId pilotId);
    // New queries added without modifying existing method contracts
}
```

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example: All repositories — JPA and InMemory implementations are interchangeable

```java
// FlightRepository interface:
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> { ... }

// JPA production implementation:
public class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightDesignator, FlightDesignator>
        implements FlightRepository { ... }

// InMemory test implementation:
public class InMemoryFlightRepository
        extends InMemoryDomainRepository<Flight, FlightDesignator>
        implements FlightRepository { ... }
```

**Why LSP?** `AddWeatherToFlightController` depends only on the interfaces. Whether the runtime uses JPA or InMemory, the controller behaviour is identical. `Flight.save()` in JPA and in InMemory satisfy the same postcondition (returns the saved entity).

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

`AddWeatherToFlightController` uses focused subsets of each repository interface:

```java
// From FlightRepository:
Optional<Flight> ofIdentity(FlightDesignator id);  // to load the flight
Flight save(Flight flight);                         // to persist after assignment

// From WeatherDataRepository:
Optional<WeatherData> ofIdentity(Long id);          // existence guard only
Iterable<WeatherData> findAll();                    // find matching weather (by area)

// From AirControlAreaRepository:
Iterable<AirControlArea> findAll();                 // to find ACA containing midpoint
```

Each interface adds only the domain-specific methods needed. `DomainRepository` provides the common `save`, `findAll`, `ofIdentity` baseline.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

```java
@UseCaseController
public class AddWeatherToFlightController {

    // All dependencies are ABSTRACT types:
    private final AuthorizationService authz;          // interface
    private final FlightRepository flightRepo;         // interface
    private final WeatherDataRepository weatherRepo;   // interface
    private final AirControlAreaRepository acaRepo;    // interface

    // Package-private constructor — for test injection:
    AddWeatherToFlightController(final AuthorizationService authz,
                                  final FlightRepository flightRepo,
                                  final WeatherDataRepository weatherRepo,
                                  final AirControlAreaRepository acaRepo) { ... }
}
```

The default constructor injects via factories:

```java
public AddWeatherToFlightController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().flights(),        // ← Factory Method
            PersistenceContext.repositories().weatherData(),    // ← Factory Method
            PersistenceContext.repositories().airControlAreas()); // ← Factory Method
}
```

---

## GRASP Principles

---

### Controller (GRASP)

**Applied by:** `AddWeatherToFlightController`

```java
public Flight assignWeather(final String flightDesignator, final Long weatherDataId) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);           // 1. Authorize
    weatherRepo.ofIdentity(weatherDataId).orElseThrow(...);             // 2. Validate existence
    final Flight flight = flightByDesignator(flightDesignator);         // 3. Load flight
    flight.assignWeatherData(weatherDataId);                            // 4. Business operation
    return flightRepo.save(flight);                                      // 5. Persist
}
```

The controller is the single point of entry. The UI never calls `flight.assignWeatherData()` directly.

---

### Creator (GRASP)

| Creator Class | Creates | Rationale |
|--------------|---------|-----------|
| `Flight` (via `assignWeatherData`) | Updates `weatherDataId` reference | Flight owns the weather assignment |
| `AddWeatherToFlightController` | `RouteMidpoint` record | Controller has the route data and computes the midpoint |
| `FlightDesignator.valueOf()` | `FlightDesignator` | Static factory — class knows its own format |

```java
// Flight creates/updates its own weather reference (owns the assignment):
public void assignWeatherData(final Long weatherDataId) {
    this.weatherDataId = weatherDataId;   // ← Creator (updates own state)
    for (final FlightPlan fp : flightPlans) { fp.resetToDraft(); }
}

// Controller creates the midpoint record:
private RouteMidpoint computeMidpoint(final Flight flight) {
    // ...
    return new RouteMidpoint(midLat, midLon, origin, dest);  // ← Creator
}
```

---

### Information Expert (GRASP)

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Weather assignment + flight plan cascade reset | `Flight` | Flight owns `weatherDataId` and `flightPlans` |
| Route name → airport coordinates mapping | `AddWeatherToFlightController` | Controller owns the static `AIRPORT_COORDS` map |
| Geographic midpoint computation | `AddWeatherToFlightController` | Controller owns the `computeMidpoint()` logic |
| ACA geographic containment check | `AirControlArea` | `AirControlArea` owns its geographic boundary — `containsCoordinates()` |
| Weather records for an ACA | `WeatherDataRepository` | Repository holds all weather records and their area codes |
| Authorization | `AuthorizationService` | Framework service knows the current user and roles |

```java
// Flight is the expert on the cascade rule (owns both fields):
public void assignWeatherData(final Long weatherDataId) {
    this.weatherDataId = weatherDataId;   // ← expert on weatherDataId
    for (final FlightPlan fp : flightPlans) {  // ← expert on flightPlans collection
        if (fp.status() == TEST_PASSED || fp.status() == TEST_FAILED) {
            fp.resetToDraft();  // ← tells FlightPlan to reset (Tell, Don't Ask)
        }
    }
}
```

---

### Tell, Don't Ask

> *"Tell an object what to do, rather than asking it for its data and making decisions on its behalf."*

```java
// GOOD — Controller TELLS Flight to assign weather:
flight.assignWeatherData(weatherDataId);  // tells

// GOOD — Flight TELLS FlightPlan to reset:
fp.resetToDraft();  // tells — does not ask fp.getStatus() and conditionally set it

// BAD (not our design) — controller checking flight plan status:
// for (var fp : flight.flightPlans()) {
//   if (fp.status() == TEST_PASSED) { fp.setStatus(DRAFT); }  // ← asking, then setting
// }
```

The cascade reset is handled entirely by `Flight` — the controller never inspects `FlightPlan` status values.

---

### High Cohesion (GRASP)

```
AddWeatherToFlightController:        Flight:                 AirControlArea:
  - allFlights()                       - assignWeatherData()   - containsCoordinates()
  - flightByDesignator()               - cascade reset plans   - areaCode()
  - weatherDataForFlight()
  - findAcaForMidpoint()
  - assignWeather()
  - computeMidpoint()  (private)
```

Each class has tightly related responsibilities in a single domain: the controller coordinates, `Flight` owns business logic, `AirControlArea` knows geography.

---

### Low Coupling (GRASP)

```
AddWeatherToFlightController
    │
    ├──→ FlightRepository            (interface)
    ├──→ WeatherDataRepository       (interface)
    ├──→ AirControlAreaRepository    (interface)
    └──→ AuthorizationService        (interface)

Flight ──→ FlightPlan    (owns, cascade via @OneToMany)
Flight ──→ WeatherDataId (cross-aggregate ref: just a Long, not WeatherData object)
```

`Flight` never imports `WeatherData`, `WeatherDataRepository`, or `AddWeatherToFlightController`. The weather assignment is done via a plain `Long` ID — the DDD rule for cross-aggregate low coupling.

---

### Protected Variations (GRASP)

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| Airport coordinate database | `AIRPORT_COORDS` map | New airports added as map entries; computation logic unchanged |
| ACA geographic algorithm | `AirControlArea.containsCoordinates()` | Changing the bounding-box algorithm affects only that method |
| Weather filtering algorithm | `weatherDataForFlight()` | Changing from brute-force scan to spatial index affects only that method |
| Cascade reset rule (which statuses to reset) | `Flight.assignWeatherData()` | New statuses to reset are added in the `if` condition — controller is untouched |
| Persistence technology | Repository interfaces | Switching JPA ↔ NoSQL ↔ REST affects only implementations |

---

### Pure Fabrication (GRASP)

| Class | Why It's a Fabrication |
|-------|----------------------|
| `AddWeatherToFlightController` | No aviation domain expert would recognise a "weather assignment controller" — it's a use-case artifact |
| `RouteMidpoint` (private record) | A technical computation helper; not a real-world aviation concept |
| Repository interfaces | Technical persistence abstractions |

---

## GoF Design Patterns

---

### Factory Method (Creational)

> *"Define an interface for creating an object, but let subclasses or factory methods decide which class to instantiate."*

**Applied by:** `FlightDesignator.valueOf()`, `PersistenceContext.repositories().*()`, `RouteMidpoint` private record.

```java
// FlightDesignator — static factory method hides constructor and format validation:
final Flight flight = flightByDesignator(flightDesignator);
// internally: FlightDesignator.valueOf(designator) ← Factory Method

// PersistenceContext — factory for all repository instances:
public AddWeatherToFlightController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().flights(),          // ← Factory Method
            PersistenceContext.repositories().weatherData(),      // ← Factory Method
            PersistenceContext.repositories().airControlAreas()); // ← Factory Method
}

// RouteMidpoint private record — created by a private factory-style method:
private RouteMidpoint computeMidpoint(final Flight flight) {
    // ...
    return new RouteMidpoint(midLat, midLon, origin, dest);  // ← factory-style creation
}
```

**Why Factory Method?** `FlightDesignator.valueOf()` centralises format validation. `PersistenceContext.repositories()` is a factory that selects the correct concrete implementation (JPA, InMemory) without the controller knowing which.

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client."*

**Applied by:** `JpaFlightRepository`, `JpaWeatherDataRepository`, `JpaAirControlAreaRepository`

```java
// Target:
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> { ... }

// Adapter — translates domain methods to EAPLI JPA framework calls:
public class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightDesignator, FlightDesignator>  // ← Adaptee
        implements FlightRepository {                                              // ← Target

    @Override
    public Optional<Flight> ofIdentity(final FlightDesignator id) {
        // adapts the framework's find method to our domain method signature
    }
}
```

**Why Adapter?** `AddWeatherToFlightController` depends only on `FlightRepository` (target). The JPA framework's `JpaAutoTxRepository` speaks a different language. Each repository implementation **adapts** the framework API to the domain's expected interface.

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable."*

**Applied by:** `FlightRepository`, `WeatherDataRepository`, `AirControlAreaRepository`

```java
// Three Strategy interfaces, each with two implementations:
FlightRepository      → JpaFlightRepository      / InMemoryFlightRepository
WeatherDataRepository → JpaWeatherDataRepository  / InMemoryWeatherDataRepository
AirControlAreaRepository → JpaAirControlAreaRepository / InMemoryAirControlAreaRepository

// Context (controller) — same weather assignment logic regardless of strategy:
public Flight assignWeather(final String flightDesignator, final Long weatherDataId) {
    weatherRepo.ofIdentity(weatherDataId).orElseThrow(...);   // ← same call for JPA or InMemory
    final Flight flight = flightByDesignator(flightDesignator);
    flight.assignWeatherData(weatherDataId);
    return flightRepo.save(flight);                            // ← same call for JPA or InMemory
}
```

**Why Strategy?** Three different persistence strategies are swapped simultaneously via constructor injection. Unit tests inject InMemory implementations; production uses JPA.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `allFlights()`, `weatherDataForFlight()`, `findAcaForMidpoint()`

```java
// allFlights() — returns Iterable<Flight>:
public Iterable<Flight> allFlights() {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
    return flightRepo.findAll();   // ← Iterable (Iterator pattern)
}

// weatherDataForFlight() — iterates all weather records:
for (final var wd : weatherRepo.findAll()) {     // ← Iterator over Iterable<WeatherData>
    if (wd.areaCode().equals(acaCode)) {
        result.add(wd);
    }
}

// findAcaForMidpoint() — iterates all ACAs:
for (final var aca : acaRepo.findAll()) {         // ← Iterator over Iterable<AirControlArea>
    if (aca.containsCoordinates(lat, lon)) {
        return aca;
    }
}
```

**Why Iterator?** None of the traversals know or care whether the underlying collection is a JPA `PersistentBag`, an in-memory `ArrayList`, or a lazy query result. The `Iterable<T>` interface decouples traversal from storage.

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem."*

**Applied by:** `AddWeatherToFlightController`

```java
// Subsystem hidden behind the controller:
//   FlightDesignator.valueOf(), FlightRepository, WeatherDataRepository,
//   AirControlAreaRepository, AirControlArea.containsCoordinates(),
//   AIRPORT_COORDS map, midpoint geometry, Flight.assignWeatherData(),
//   AuthorizationService

// The UI only needs:
AddWeatherToFlightController ctrl = new AddWeatherToFlightController();

// Step 1: select a flight
Iterable<Flight> flights = ctrl.allFlights();

// Step 2: find relevant weather for that flight (hides midpoint + ACA computation)
List<WeatherData> weather = ctrl.weatherDataForFlight(selectedFlight);

// Step 3: assign selected weather (hides existence guard + cascade reset + persistence)
Flight updated = ctrl.assignWeather(selectedFlight.identity().toString(), selectedWeatherId);
```

**Why Facade?** The full weather assignment pipeline involves five repositories, a static airport database lookup, geographic midpoint calculation, and a cascade reset on `FlightPlan` entities. The controller hides all of this behind three focused methods.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── flight/
│   ├── domain/
│   │   ├── Flight.java              (Entity, AggregateRoot<FlightDesignator>)
│   │   └── FlightDesignator.java    (ValueObject, @EmbeddedId)
│   ├── repositories/
│   │   └── FlightRepository.java    (Interface)
│   └── application/
│       └── AddWeatherToFlightController.java (@UseCaseController)
├── weatherdata/
│   ├── domain/
│   │   ├── WeatherData.java         (Entity — weather cross-aggregate ref by Long id)
│   │   └── WindCondition.java       (ValueObject)
│   └── repositories/
│       └── WeatherDataRepository.java (Interface)
└── aircontrolarea/
    ├── domain/
    │   └── AirControlArea.java      (Entity — geographic boundary check)
    └── repositories/
        └── AirControlAreaRepository.java (Interface)
```

### Layer Dependency Rules

```
Controller → Repository interfaces (application boundary)
Controller → Domain (reads Flight, WeatherData state)
Flight → FlightPlan (child aggregate — cascade reset)
Flight → WeatherData (cross-aggregate by Long id only — no object reference)
```

No domain class imports any application or infrastructure class.
`Flight` holds `weatherDataId` as a plain `Long` — it never imports or references the `WeatherData` class directly.
