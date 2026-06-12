# US077 — SOLID & GRASP Principles Applied

Application to the **Remove (Deactivate) a Pilot** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

Each class in US077 has exactly one responsibility:

#### `Pilot` — Aggregate root: owns deactivation and activation state transitions

```java
public void deactivate() {
    Invariants.ensure(active, "Pilot is already inactive");
    this.active = false;
}

public void activate() {
    Invariants.ensure(!active, "Pilot is already active");
    this.active = true;
}
```

**Why SRP?** `Pilot` owns its own active/inactive state and guards the transition rules. It does NOT check whether the pilot has assigned flight plans — that is a cross-aggregate business rule that belongs to the controller.

#### `RemovePilotController` — Orchestration only

```java
@UseCaseController
public class RemovePilotController {

    public Pilot deactivatePilot(final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        final var pilot = pilotRepo.findByLicenseNumber(pilotId)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + pilotId));
        if (flightRepo.existsByPilotLicense(pilotId)) {
            throw new IllegalStateException(
                    "Cannot deactivate pilot " + pilotId + " — flight plans are assigned");
        }
        pilot.deactivate();
        return pilotRepo.save(pilot);
    }
}
```

**Why SRP?** The controller only **orchestrates** — it authorises, loads the pilot, checks the cross-aggregate business rule, delegates state transition to the entity, and persists. It does NOT contain any state-machine logic.

#### Other classes and their single reasons:

| Class | Single Responsibility |
|-------|----------------------|
| `PilotRepository` | Contract for pilot persistence queries |
| `FlightRepository` | Contract for flight/flight-plan queries, including pilot assignment check |
| `PilotId` | Value Object: license number format validation |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: `Pilot` — new state transitions via new methods

Adding a new state (e.g. `suspend()`) requires only adding a new method to `Pilot` with its guard invariant. No existing `deactivate()` or `activate()` method changes.

#### Example 2: `RemovePilotController` — new guard rules via extension

If a new business rule must be checked before deactivation (e.g. check for pending certifications), a new method or query is added to the relevant repository, and the check is inserted in `deactivatePilot()`. Existing `activatePilot()` and `allPilots()` methods are untouched.

#### Example 3: `FlightRepository` — new existence checks via extension

```java
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    // New existence check can be added without modifying existing methods:
    boolean existsByPilotLicense(PilotId pilotId);
}
```

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example 1: `PilotRepository` — JPA and InMemory are interchangeable

```java
// Interface (abstraction)
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
}

// JPA implementation (production)
public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, PilotId, PilotId>
        implements PilotRepository { ... }

// InMemory implementation (tests)
public class InMemoryPilotRepository
        extends InMemoryDomainRepository<Pilot, PilotId>
        implements PilotRepository { ... }
```

**Why LSP?** `RemovePilotController` depends only on `PilotRepository`. Whether the actual instance is `JpaPilotRepository` or `InMemoryPilotRepository`, the deactivation behaviour is identical.

#### Example 2: `FlightRepository` — same substitutability

```java
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    boolean existsByPilotLicense(PilotId pilotId);
}
```

Test code uses `InMemoryFlightRepository`; production uses `JpaFlightRepository`. The controller never knows which.

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example 1: `RemovePilotController` uses only focused methods from each repository

```java
// From PilotRepository:
Optional<Pilot> findByLicenseNumber(PilotId pilotId);  // to load the pilot
Pilot save(Pilot pilot);                               // to persist after state change

// From FlightRepository:
boolean existsByPilotLicense(PilotId pilotId);         // guard check only
```

The controller does NOT call `findAll()`, `findByCompany()`, or any other repository method that is not relevant to this use case. It uses the minimal surface of each interface.

#### Example 2: `RemovePilotController` exposes three focused methods to the UI

```java
public List<Pilot> allPilots() { ... }               // populate the selection list
public Pilot deactivatePilot(final PilotId pilotId) { ... }  // execute deactivation
public Pilot activatePilot(final PilotId pilotId) { ... }    // undo deactivation
```

The UI is not forced to deal with methods from `AddPilotController` or `ListPilotRosterController` — each use case has its own focused controller.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### Example: `RemovePilotController` depends entirely on abstractions

```java
@UseCaseController
public class RemovePilotController {

    // All dependencies are ABSTRACT types:
    private final AuthorizationService authz;    // interface (framework)
    private final PilotRepository pilotRepo;     // interface (our code)
    private final FlightRepository flightRepo;   // interface (our code)

    // Package-private constructor — accepts abstractions (used by tests)
    RemovePilotController(final AuthorizationService authz,
                           final PilotRepository pilotRepo,
                           final FlightRepository flightRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.flightRepo = flightRepo;
    }
}
```

**Why DIP?**
- `PilotRepository` and `FlightRepository` are **interfaces** — the controller never knows about JPA or InMemory implementations.
- `AuthorizationService` is a **framework interface** — no authz logic is instantiated inside the controller.

The **default constructor** injects via static factories:

```java
public RemovePilotController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().pilots(),
            PersistenceContext.repositories().flights());
}
```

---

## GRASP Principles

---

### Controller (GRASP)

> *Assign the responsibility of handling system events to a non-UI class that represents the overall use case.*

**Applied by:** `RemovePilotController`

```java
@UseCaseController
public class RemovePilotController {
    public Pilot deactivatePilot(final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR); // 1. Authorize
        final var pilot = pilotRepo.findByLicenseNumber(pilotId)             // 2. Load pilot
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + pilotId));
        if (flightRepo.existsByPilotLicense(pilotId)) {                      // 3. Cross-aggregate guard
            throw new IllegalStateException(
                    "Cannot deactivate pilot " + pilotId + " — flight plans are assigned");
        }
        pilot.deactivate();                                                   // 4. State transition
        return pilotRepo.save(pilot);                                         // 5. Persist
    }
}
```

The controller is the single point of entry for the "remove pilot" system operation. The UI never calls `pilot.deactivate()` or repository methods directly.

---

### Creator (GRASP)

> *Assign responsibility for creating instances of class A to class B when B contains, aggregates, records, or closely uses A.*

US077 is a **state-change** use case — no new domain objects are created. The `Pilot` object being deactivated already exists in the repository. The `PilotId` parameter is created by the UI or test caller and passed to the controller.

---

### Information Expert (GRASP)

> *Assign a responsibility to the class that has the information needed to fulfill it.*

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Active/inactive state rule ("already inactive") | `Pilot` | `Pilot` owns the `active` flag and the invariant `"Pilot is already inactive"` |
| Loading a pilot by license number | `PilotRepository` | Repository is the expert on finding pilots by identity |
| Checking if a pilot has assigned flight plans | `FlightRepository` | FlightRepository holds all flight/flight-plan records and knows which pilots are referenced |
| Listing all pilots (for selection) | `PilotRepository` | Repository holds all pilot records |
| Authorization check | `AuthorizationService` | Framework service knows authenticated user and roles |

```java
// Pilot is the expert on its own state transition:
public void deactivate() {
    Invariants.ensure(active, "Pilot is already inactive");  // ← expert
    this.active = false;
}

// FlightRepository is the expert on flight-plan assignment:
if (flightRepo.existsByPilotLicense(pilotId)) {   // ← expert (cross-aggregate guard)
    throw new IllegalStateException(...);
}
```

**Note on cross-aggregate responsibility:** The rule "a pilot with assigned flight plans cannot be deactivated" spans two aggregates (`Pilot` and `Flight`). By DDD convention, `Pilot` cannot hold a reference to `Flight`. The controller is therefore the right place for this cross-aggregate check, using `FlightRepository` as the expert on the Flight side.

---

### High Cohesion (GRASP)

> *Keep responsibilities strongly related and focused within each class.*

```
Low Cohesion (bad):                          High Cohesion (our design):
┌──────────────────────────────┐             ┌──────────────────────────┐
│  RemovePilotController       │             │  Pilot                   │
│  - checkFlightAssignment()   │             │  - deactivate()          │
│  - updateActiveFlag()        │             │  - activate()            │
│  - sendEmail()               │             │  - isActive()            │
│  - logAuditTrail()           │             └──────────────────────────┘
│  - validateLicenseFormat()   │
└──────────────────────────────┘             ┌──────────────────────────┐
                                              │  RemovePilotController   │
Each class in our design is cohesive:        │  - authorize             │
                                              │  - cross-aggregate guard  │
• Domain:      Pilot (state transitions)     │  - delegate deactivate()  │
• Repository:  PilotRepository, FlightRepo   │  - persist               │
• Application: RemovePilotController         └──────────────────────────┘
```

---

### Low Coupling (GRASP)

> *Keep dependencies between classes as weak as possible.*

```
RemovePilotController
    │
    ├──→ PilotRepository     (interface — switchable JPA ↔ InMemory)
    ├──→ FlightRepository    (interface — switchable JPA ↔ InMemory)
    └──→ AuthorizationService (interface — framework abstraction)
```

```java
// The controller never imports JPA or InMemory classes:
import eapli.aisafe.pilot.repositories.PilotRepository;              // ← interface only
import eapli.aisafe.flight.repositories.FlightRepository;            // ← interface only
import eapli.framework.infrastructure.authz.application.AuthorizationService; // ← interface
```

`Pilot` has **zero dependencies** on `FlightRepository` or any other aggregate — cross-aggregate checks are done exclusively in the controller. This is the DDD rule for cross-aggregate low coupling.

---

### Polymorphism (GRASP)

> *Use polymorphic operations to handle variations in behavior based on type.*

#### Repository polymorphism

```java
// Used polymorphically — controller never cares which implementation is active:
PilotRepository  pilotRepo  = isTestEnvironment() ? new InMemoryPilotRepository()  : new JpaPilotRepository("aisafe");
FlightRepository flightRepo = isTestEnvironment() ? new InMemoryFlightRepository() : new JpaFlightRepository("aisafe");
```

#### `Pilot.deactivate()` / `Pilot.activate()` — behaviour encoded in state

```java
public void deactivate() {
    Invariants.ensure(active, "Pilot is already inactive");  // ← varies by active flag
    this.active = false;
}
```

The method's behaviour changes depending on the current `active` state — the caller does not need a conditional: it simply calls `deactivate()` and the entity enforces its own rules.

---

### Protected Variations (GRASP)

> *Protect the system from variations in external components by wrapping them behind stable interfaces.*

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| Active-flag storage (column, type) | `Pilot.deactivate()` | Changes to persistence mapping affect only `@Column` annotations on `Pilot` |
| Flight-plan assignment query strategy | `FlightRepository.existsByPilotLicense()` | Changes to the JPQL or query approach affect only the repository implementation |
| Persistence technology | Repository interfaces | Switching JPA ↔ file ↔ NoSQL means swapping implementations — controller is untouched |
| Guard rule (new reasons to block deactivation) | `RemovePilotController.deactivatePilot()` | New guard checks are added in the controller method — `Pilot.deactivate()` does not change |
| Authentication mechanism | `AuthorizationService` | Framework-level change; controller code stays identical |

```java
// The controller is protected from all persistence and guard variations:
public Pilot deactivatePilot(final PilotId pilotId) {
    // If flight-query strategy changes → only FlightRepository implementation changes
    if (flightRepo.existsByPilotLicense(pilotId)) { throw ...; }
    // If state-machine logic changes → only Pilot.deactivate() changes
    pilot.deactivate();
    // If persistence changes → only JpaPilotRepository changes
    return pilotRepo.save(pilot);
    // → Controller orchestration code stays identical
}
```

---

### Pure Fabrication (GRASP)

> *Create artificial classes that do not represent domain concepts to achieve low coupling and high cohesion.*

| Class | Why It's a Fabrication |
|-------|----------------------|
| `RemovePilotController` | The aviation domain has no "remove pilot controller" — it is a use-case artifact that orchestrates authorization, cross-aggregate guard checks, entity state change and persistence |
| `PilotRepository` | Repositories do not exist in the real world — they are technical abstractions for persistence |
| `FlightRepository` | Same — used here purely as a query service to check cross-aggregate constraints |

```java
// Without Pure Fabrication, Pilot would need to query flights:
public class Pilot {
    public void deactivate(FlightRepository repo) {  // ← BAD! domain entity knows about another repo
        if (repo.existsByPilotLicense(this.pilotId)) throw ...;
        this.active = false;
    }
}

// With Pure Fabrication:
// The cross-aggregate check stays in the CONTROLLER (pure fabrication):
public class RemovePilotController {
    public Pilot deactivatePilot(final PilotId pilotId) {
        if (flightRepo.existsByPilotLicense(pilotId)) { throw ...; }  // ← controller handles this
        pilot.deactivate();  // ← Pilot only knows about itself
        return pilotRepo.save(pilot);
    }
}
```

---

### Indirection (GRASP)

> *Assign the responsibility of mediating between two components to an intermediate object.*

| Mediator | Mediates Between | Why |
|----------|-----------------|-----|
| `PilotRepository` | Controller ↔ Pilot persistence | Controller never touches JPA code |
| `FlightRepository` | Controller ↔ Flight persistence (read-only guard) | Controller never queries flight tables directly |
| `RemovePilotController` | UI ↔ Domain + Repositories | UI never calls `pilot.deactivate()` or any repository method directly |

```java
// Without Indirection: UI touches domain and repositories directly
public class RemovePilotUI {
    public void doIt() {
        var pilot = new JpaPilotRepository("aisafe")      // ← BAD! tight coupling to JPA
                        .findByLicenseNumber(pilotId).get();
        var hasFlights = new JpaFlightRepository("aisafe") // ← BAD!
                        .existsByPilotLicense(pilotId);
        if (!hasFlights) pilot.deactivate();
    }
}

// With Indirection:
public class RemovePilotController {   // ← INDIRECTION
    public Pilot deactivatePilot(final PilotId pilotId) {
        // UI calls one method — all details are hidden
        ...
        pilot.deactivate();
        return pilotRepo.save(pilot);
    }
}
```

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── pilot/
│   ├── domain/
│   │   ├── Pilot.java              (Entity, AggregateRoot<PilotId>)
│   │   └── PilotId.java            (ValueObject, @Embeddable)
│   ├── repositories/
│   │   └── PilotRepository.java    (Interface)
│   └── application/
│       └── RemovePilotController.java (@UseCaseController)
└── flight/
    ├── domain/
    │   └── Flight.java             (Entity — referenced read-only for guard check)
    └── repositories/
        └── FlightRepository.java   (Interface — used as cross-aggregate guard)
```

### Layer Responsibilities

| Layer | Contains | Dependencies |
|-------|----------|-------------|
| **Domain** | Entities, VOs | eapli framework only |
| **Application** | Controllers | Domain + infrastructure abstractions |
| **Infrastructure** | Persistence (JPA, InMemory) | All of the above |

### Layer Dependency Rules

```
Controller → Repository (interface, application layer boundary)
Controller → Domain
Repository implementation (infrastructure) → Repository interface
```

No domain class (`Pilot`, `PilotId`) imports any class from `flight` or from any application/infrastructure layer.
The cross-aggregate constraint ("pilot with flights cannot be deactivated") is enforced exclusively in the controller, keeping both aggregates independent.

---

## GoF Design Patterns

---

### Factory Method (Creational)

> *"Define an interface for creating an object, but let subclasses or factory methods decide which class to instantiate."*

**Applied by:** `PilotId.valueOf()`

```java
// PilotId — static factory method used when loading the pilot to deactivate:
public static PilotId valueOf(final String licenseNumber) {
    return new PilotId(licenseNumber);   // ← Factory Method
}

// Used in the UI / test caller before passing to the controller:
PilotId pilotId = PilotId.valueOf("P12345");
controller.deactivatePilot(pilotId);
```

**Why Factory Method?** `PilotId.valueOf()` hides construction detail (trim, uppercase, regex validation) behind a named factory. If validation rules change, callers are unaffected. The same static-factory idiom is also used by the default constructor via `PersistenceContext`:

```java
public RemovePilotController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().pilots(),   // ← Factory Method
            PersistenceContext.repositories().flights()); // ← Factory Method
}
```

---

### State (Behavioural)

> *"Allow an object to alter its behaviour when its internal state changes. The object will appear to change its class."*

**Applied by:** `Pilot.deactivate()` / `Pilot.activate()`

```java
// Pilot has two states: ACTIVE and INACTIVE.
// Each state determines whether a transition is allowed.

public void deactivate() {
    Invariants.ensure(active, "Pilot is already inactive");  // ← guard: only valid in ACTIVE state
    this.active = false;                                      // ← transition to INACTIVE
}

public void activate() {
    Invariants.ensure(!active, "Pilot is already active");   // ← guard: only valid in INACTIVE state
    this.active = true;                                       // ← transition to ACTIVE
}
```

```
        deactivate()              activate()
ACTIVE ──────────────→ INACTIVE ──────────────→ ACTIVE
         (guard: active)          (guard: !active)
```

**Why State?** `Pilot` encodes its own state machine. Each transition method guards against illegal calls from the wrong state and transitions to the correct state. The caller never writes `if (pilot.isActive()) pilot.deactivate()` — the entity enforces the rule itself. This is the State pattern applied at the entity level: the object's behaviour (allowed or throws) changes depending on its current state.

In a more complex scenario this would use dedicated `PilotState` objects, but for a two-state machine the boolean flag with guarded methods is the simplest correct application of the same principle.

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client. Adapter lets classes work together that otherwise could not because of incompatible interfaces."*

**Applied by:** `JpaPilotRepository`, `JpaFlightRepository`

```java
// Target — our domain interface:
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
}

// Adapter — adapts the EAPLI JPA framework (Adaptee) to our interface (Target):
public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, PilotId, PilotId>  // ← Adaptee
        implements PilotRepository {                            // ← Target

    @Override
    public Optional<Pilot> findByLicenseNumber(final PilotId pilotId) {
        return matchOne("SELECT p FROM Pilot p WHERE p.pilotId = :id",
                Map.of("id", pilotId));  // ← translates our method to framework method
    }
}

// FlightRepository adapter — exposes cross-aggregate guard as a domain method:
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    boolean existsByPilotLicense(PilotId pilotId);   // ← target method
}

public class JpaFlightRepository implements FlightRepository {
    public boolean existsByPilotLicense(final PilotId pilotId) {
        // adapts framework's count() to our boolean method
        return count("SELECT COUNT(f) FROM Flight f WHERE f.pilotLicense = :id",
                Map.of("id", pilotId)) > 0;
    }
}
```

**Why Adapter?** `RemovePilotController` depends on `PilotRepository` and `FlightRepository` (targets). The JPA framework's `JpaAutoTxRepository` speaks `matchOne`, `count`, `match`. Each repository adapter **translates** the domain's expected method names and return types into the framework's internal API — without changing either the controller or the framework.

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable."*

**Applied by:** `PilotRepository` and `FlightRepository`

```java
// Strategy interface A:
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
}

// Concrete Strategy A1 — production:
public class JpaPilotRepository implements PilotRepository { ... }

// Concrete Strategy A2 — tests:
public class InMemoryPilotRepository implements PilotRepository { ... }

// Strategy interface B:
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    boolean existsByPilotLicense(PilotId pilotId);
}

// Concrete Strategy B1 — production:
public class JpaFlightRepository implements FlightRepository { ... }

// Concrete Strategy B2 — tests:
public class InMemoryFlightRepository implements FlightRepository { ... }

// Context (controller) — the deactivation logic never changes regardless of strategy:
public Pilot deactivatePilot(final PilotId pilotId) {
    final var pilot = pilotRepo.findByLicenseNumber(pilotId).orElseThrow(...);
    if (flightRepo.existsByPilotLicense(pilotId)) { throw ...; }
    pilot.deactivate();
    return pilotRepo.save(pilot);   // ← same logic for JPA or InMemory
}
```

**Why Strategy?** Both the pilot-loading algorithm and the flight-existence check are swapped between production and test environments without any change to the controller's orchestration logic.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `allPilots()`

```java
// allPilots() returns a List<Pilot> built from an Iterable (Iterator pattern):
public List<Pilot> allPilots() {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    final List<Pilot> result = new ArrayList<>();
    pilotRepo.findAll().forEach(result::add);   // ← consumes an Iterator internally
    return result;
}

// UI iterates to display the selection list:
for (Pilot pilot : controller.allPilots()) {
    // show pilot in list for user to select
}
```

**Why Iterator?** `pilotRepo.findAll()` returns an `Iterable<Pilot>` — the standard Java implementation of the Iterator pattern. The controller never knows whether the underlying collection is a JPA cursor, an in-memory `ArrayList`, or a lazy stream. The `forEach` / enhanced-for loop works identically in all cases.

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem."*

**Applied by:** `RemovePilotController`

```java
// Without the controller, the UI would need to coordinate all of these:
//   PilotId (VO), PilotRepository, FlightRepository, AuthorizationService, Pilot.deactivate()
//
// The controller (Facade) hides all of this behind three methods:

RemovePilotController controller = new RemovePilotController();

// Step 1: load all pilots to display selection list
List<Pilot> pilots = controller.allPilots();

// Step 2: user picks one — deactivation with full cross-aggregate guard
Pilot deactivated = controller.deactivatePilot(selectedPilotId);

// Step 3 (optional undo): re-activate
Pilot reactivated = controller.activatePilot(selectedPilotId);
```

**Why Facade?** The UI calls three methods. Behind the scenes the controller authorises the user, queries two repositories (`PilotRepository` and `FlightRepository`), enforces the cross-aggregate business rule, calls `Pilot.deactivate()`, and persists the result. All of this complexity is invisible to the UI layer — the classic purpose of the Facade pattern.
