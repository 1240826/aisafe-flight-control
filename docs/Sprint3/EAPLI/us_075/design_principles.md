# US075 — SOLID & GRASP Principles Applied

Application to the **Add Pilot** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

Each class in US075 has exactly one responsibility:

#### `Pilot` — Aggregate root: identity, company, certifications and active state

```java
@Entity
@Table(name = "PILOT")
public class Pilot implements AggregateRoot<PilotId> {

    @EmbeddedId
    private PilotId pilotId;

    @Embedded
    private CompanyIATA company;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "PILOT_CERTIFIED_MODEL", joinColumns = @JoinColumn(name = "LICENSE_NUMBER"))
    private Set<AircraftModelCode> certifiedModels = new HashSet<>();

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "CERTIFICATION_DATE", nullable = false)
    private LocalDate certificationDate;
}
```

**Why SRP?** `Pilot` only manages its own identity, company reference, certified models and active flag. It does NOT validate license format, list companies, or persist itself.

#### `PilotId` — Value Object: license number format validation

```java
public PilotId(final String licenseNumber) {
    Preconditions.noneNull(licenseNumber);
    final String trimmed = licenseNumber.trim().toUpperCase();
    Invariants.ensure(!trimmed.isBlank(), "Pilot license number must not be blank");
    Invariants.ensure(trimmed.matches("[A-Z][0-9]{4,10}"),
            "Pilot license must be a letter followed by 4-10 digits (e.g. 'P12345')");
    this.licenseNumber = trimmed;
}
```

**Why SRP?** `PilotId` only validates and stores the license number string. It does NOT know about the Pilot entity, the company, or the repository.

#### `AddPilotController` — Orchestration only

```java
@UseCaseController
public class AddPilotController {

    public Pilot addPilot(final String licenseNumber, final CompanyIATA company,
                          final Set<AircraftModelCode> certifiedModels,
                          final LocalDate certificationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        final var pilotId = PilotId.valueOf(licenseNumber);
        final var pilot = new Pilot(pilotId, company, certifiedModels, certificationDate);
        return pilotRepo.save(pilot);
    }
}
```

**Why SRP?** The controller only **orchestrates** — it does NOT validate the license format, check model existence, or format the output. Each responsibility belongs to a dedicated class.

#### Other classes and their single reasons:

| Class | Single Responsibility |
|-------|----------------------|
| `PilotRepository` | Contract for pilot persistence queries |
| `AirTransportCompanyRepository` | Contract for company queries (`findAll()`) |
| `AircraftModelRepository` | Contract for aircraft model queries (`findAll()`) |
| `CompanyIATA` | Value Object: company IATA code format |
| `AircraftModelCode` | Value Object: aircraft model code format |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: `Pilot` — new behaviour via new methods, not modification

Adding a new certification-level field (e.g. `CertificationLevel`) requires only adding the field and a new factory or setter method. No existing `addPilot()`, `deactivate()`, or `identity()` methods need to change.

#### Example 2: `PilotId.valueOf()` — static factory as extension point

```java
public static PilotId valueOf(final String licenseNumber) {
    return new PilotId(licenseNumber);
}
```

New creation strategies (e.g. `fromIcaoFormat(...)`) can be added as additional factory methods without modifying the constructor or existing callers.

#### Example 3: `PilotRepository` — minimal interface, extensible without modification

```java
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
    boolean hasAssignedFlights(PilotId pilotId);
}
```

New queries (e.g. `findByCertifiedModel(AircraftModelCode)`) can be added to the interface and implemented in each repository class without touching any existing method.

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example 1: `PilotRepository` — JPA and InMemory are interchangeable

```java
// Interface (abstraction)
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
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

**Why LSP?** `AddPilotController` depends only on `PilotRepository`. Whether the runtime instance is `JpaPilotRepository` or `InMemoryPilotRepository` is irrelevant — both honour the same contract.

#### Example 2: `PilotId` implements `ValueObject` + `Comparable`

```java
@Embeddable
public class PilotId implements ValueObject, Comparable<PilotId> {
    @Override
    public int compareTo(final PilotId other) {
        return this.licenseNumber.compareTo(other.licenseNumber);
    }
}
```

`PilotId` can be used anywhere the eapli framework expects a `ValueObject` or a `Comparable` (sorted collections, `DomainRepository` identity matching).

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example 1: `PilotRepository` — only the methods each US actually needs

```java
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    // US075/077: look up by license
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
    // US076: list by company
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
    // US077: guard check
    boolean hasAssignedFlights(PilotId pilotId);
}
```

`DomainRepository` already provides `save()`, `findAll()`, `ofIdentity()`, etc. `PilotRepository` adds exactly the methods needed by the three use cases — no more.

#### Example 2: `AddPilotController` — one public operation per use case step

```java
public Iterable<AirTransportCompany> allCompanies() { ... }   // populate combo
public Iterable<AircraftModel> allAircraftModels() { ... }    // populate multi-select
public Pilot addPilot(...) { ... }                             // execute use case
```

The UI only calls the methods relevant to each step. There is no method that forces the UI to deal with activation, listing, or removal.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### Example: `AddPilotController` depends entirely on abstractions

```java
@UseCaseController
public class AddPilotController {

    // All dependencies are ABSTRACT types:
    private final AuthorizationService authz;              // interface (framework)
    private final PilotRepository pilotRepo;               // interface (our code)
    private final AirTransportCompanyRepository companyRepo; // interface (our code)
    private final AircraftModelRepository modelRepo;       // interface (our code)

    // Package-private constructor — accepts abstractions (used by tests)
    AddPilotController(final AuthorizationService authz,
                        final PilotRepository pilotRepo,
                        final AirTransportCompanyRepository companyRepo,
                        final AircraftModelRepository modelRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
        this.modelRepo = modelRepo;
    }
}
```

**Why DIP?**
- `PilotRepository`, `AirTransportCompanyRepository`, `AircraftModelRepository` are all **interfaces** — the controller never knows about JPA or InMemory implementations.
- `AuthorizationService` is a **framework interface** — no authz logic is instantiated inside the controller.

The **default constructor** injects concrete implementations via static factories:

```java
public AddPilotController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().pilots(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().aircraftModels());
}
```

---

## GRASP Principles

---

### Controller (GRASP)

> *Assign the responsibility of handling system events to a non-UI class that represents the overall use case.*

**Applied by:** `AddPilotController`

```java
@UseCaseController
public class AddPilotController {
    public Pilot addPilot(final String licenseNumber, final CompanyIATA company,
                          final Set<AircraftModelCode> certifiedModels,
                          final LocalDate certificationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR); // 1. Authorize
        final var pilotId = PilotId.valueOf(licenseNumber);                   // 2. Create VO
        final var pilot = new Pilot(pilotId, company, certifiedModels, certificationDate); // 3. Create entity
        return pilotRepo.save(pilot);                                          // 4. Persist
    }
}
```

The controller is the single point of entry for the "add pilot" system operation. The UI never touches domain classes or repositories directly.

---

### Creator (GRASP)

> *Assign responsibility for creating instances of class A to class B when B contains, aggregates, records, or closely uses A.*

| Creator Class | Creates | Rationale |
|--------------|---------|-----------|
| `AddPilotController` | `PilotId` | Controller is the first use point of the license string — it calls `PilotId.valueOf()` |
| `AddPilotController` | `Pilot` | Controller has all the data needed for construction (pilotId, company, models, date) |
| `PilotId.valueOf()` | `PilotId` | Static factory: the class creates itself with the correct validated state |

```java
// Controller creates both VO and entity:
final var pilotId = PilotId.valueOf(licenseNumber);                            // ← Creator
final var pilot   = new Pilot(pilotId, company, certifiedModels, certificationDate); // ← Creator
```

---

### Information Expert (GRASP)

> *Assign a responsibility to the class that has the information needed to fulfill it.*

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| License number format validation | `PilotId` | `PilotId` owns the regex `[A-Z][0-9]{4,10}` and the trim/uppercase logic |
| Certified model set (not empty) invariant | `Pilot` | `Pilot` owns `certifiedModels` |
| Certification date (not future) invariant | `Pilot` | `Pilot` owns `certificationDate` and applies `Invariants.ensure(...)` |
| Active flag default | `Pilot` | `Pilot` initialises `active = true` in its constructor |
| Company lookup | `AirTransportCompanyRepository` | Repository is the expert on finding all companies |
| Aircraft model lookup | `AircraftModelRepository` | Repository is the expert on finding all models |
| Authorization check | `AuthorizationService` | Framework service knows authenticated user and roles |

```java
// Pilot is the expert on its own invariants:
public Pilot(...) {
    Invariants.ensure(!certifiedModels.isEmpty(),
            "Pilot must be certified for at least one aircraft model");      // ← expert
    Invariants.ensure(!certificationDate.isAfter(LocalDate.now()),
            "Certification date must not be in the future");                 // ← expert
}
```

---

### High Cohesion (GRASP)

> *Keep responsibilities strongly related and focused within each class.*

```
Low Cohesion (bad):                          High Cohesion (our design):
┌──────────────────────────────┐             ┌──────────────────────────┐
│  MegaPilotController         │             │  PilotId                 │
│  - validateLicenseFormat()   │             │  - validate format        │
│  - checkCompanyExists()      │             │  - trim + uppercase       │
│  - checkModelExists()        │             └──────────────────────────┘
│  - savePilot()               │
│  - sendEmailConfirmation()   │             ┌──────────────────────────┐
└──────────────────────────────┘             │  Pilot                   │
                                              │  - enforce invariants     │
Each class in our design is cohesive:        │  - deactivate / activate  │
                                              └──────────────────────────┘
• Domain:      Pilot, PilotId
• Repository:  PilotRepository               ┌──────────────────────────┐
• Application: AddPilotController            │  AddPilotController      │
                                              │  - orchestrate only       │
                                              └──────────────────────────┘
```

---

### Low Coupling (GRASP)

> *Keep dependencies between classes as weak as possible.*

```
AddPilotController
    │
    ├──→ PilotRepository               (interface — switchable JPA ↔ InMemory)
    ├──→ AirTransportCompanyRepository (interface — switchable JPA ↔ InMemory)
    ├──→ AircraftModelRepository       (interface — switchable JPA ↔ InMemory)
    └──→ AuthorizationService          (interface — framework abstraction)
```

```java
// The controller never imports JPA or InMemory classes:
import eapli.aisafe.pilot.repositories.PilotRepository;                    // ← interface only
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;    // ← interface only
import eapli.framework.infrastructure.authz.application.AuthorizationService; // ← interface
```

Domain classes (`Pilot`, `PilotId`, `CompanyIATA`, `AircraftModelCode`) have **zero dependencies** on application or infrastructure layers. They depend only on the eapli framework (`@Entity`, `AggregateRoot`, `ValueObject`, `Preconditions`, `Invariants`).

Cross-aggregate references are kept by identity VO (`CompanyIATA`, `AircraftModelCode`) — never by object reference — which is the DDD rule for cross-aggregate low coupling.

---

### Polymorphism (GRASP)

> *Use polymorphic operations to handle variations in behavior based on type.*

#### Repository polymorphism

```java
// Both implementations share the same interface — used polymorphically:
PilotRepository repo = isTestEnvironment()
        ? new InMemoryPilotRepository()   // ← tests use this
        : new JpaPilotRepository("aisafe");   // ← production uses this
```

#### `PilotId.compareTo()` — natural ordering

```java
@Override
public int compareTo(final PilotId other) {
    return this.licenseNumber.compareTo(other.licenseNumber);
}
```

Any sorted collection or framework utility that operates on `Comparable<PilotId>` works correctly without knowing the internal string representation.

---

### Protected Variations (GRASP)

> *Protect the system from variations in external components by wrapping them behind stable interfaces.*

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| License number format rules | `PilotId` | Changes to the format regex affect only `PilotId`'s constructor — `AddPilotController` is untouched |
| Persistence technology | `PilotRepository` interface | Switching JPA ↔ file ↔ NoSQL means swapping the implementation class, not touching the controller |
| Company lookup strategy | `AirTransportCompanyRepository` interface | Data source changes affect only the repository implementation |
| Aircraft model lookup strategy | `AircraftModelRepository` interface | Same as above |
| Authentication mechanism | `AuthorizationService` interface | Framework-level change; controller code stays identical |

```java
// The controller is protected from ALL format variations:
public Pilot addPilot(final String licenseNumber, ...) {
    // If the license format changes → only PilotId changes
    final var pilotId = PilotId.valueOf(licenseNumber);
    // If persistence changes → only JpaPilotRepository changes
    return pilotRepo.save(pilot);
    // → Controller code stays identical
}
```

---

### Pure Fabrication (GRASP)

> *Create artificial classes that do not represent domain concepts to achieve low coupling and high cohesion.*

These classes have no counterpart in the real-world domain model — they are pure fabrications:

| Class | Why It's a Fabrication |
|-------|----------------------|
| `AddPilotController` | The aviation domain has no "add pilot controller" — it is a use-case artifact orchestrating authorization, VO creation, entity instantiation and persistence |
| `PilotRepository` | Repositories do not exist in the real world — they are technical abstractions for persistence |
| `AirTransportCompanyRepository` | Same — a persistence abstraction, not a real-world concept |
| `AircraftModelRepository` | Same |

```java
// Without Pure Fabrication, Pilot would need to know about persistence:
public class Pilot {
    public void save() { /* SQL here? */ }  // ← BAD! domain entity knows about database
}

// With Pure Fabrication:
public interface PilotRepository {          // ← PURE FABRICATION
    Pilot save(Pilot pilot);
}
```

---

### Indirection (GRASP)

> *Assign the responsibility of mediating between two components to an intermediate object.*

| Mediator | Mediates Between | Why |
|----------|-----------------|-----|
| `PilotRepository` | Controller ↔ Persistence (JPA / InMemory) | Controller never touches JPA code |
| `AirTransportCompanyRepository` | Controller ↔ Company persistence | Controller never queries company tables directly |
| `AircraftModelRepository` | Controller ↔ Model persistence | Controller never queries model tables directly |
| `AddPilotController` | UI ↔ Domain + Repositories | UI never instantiates `Pilot`, `PilotId`, or touches repositories |

```java
// Without Indirection: UI calls repository and domain directly
public class AddPilotUI {
    public void doIt() {
        var repo = new JpaPilotRepository("aisafe");  // ← BAD! tight coupling to JPA
        var pilot = new Pilot(new PilotId(license), company, models, date);
        repo.save(pilot);
    }
}

// With Indirection:
public class AddPilotController {   // ← INDIRECTION
    public Pilot addPilot(...) {
        final var pilotId = PilotId.valueOf(licenseNumber);
        final var pilot = new Pilot(pilotId, company, certifiedModels, certificationDate);
        return pilotRepo.save(pilot);   // ← UI never knows about JPA
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
│   │   └── PilotId.java            (ValueObject, @Embeddable, Comparable)
│   ├── repositories/
│   │   └── PilotRepository.java    (Interface)
│   └── application/
│       └── AddPilotController.java (@UseCaseController)
├── company/
│   ├── domain/
│   │   ├── AirTransportCompany.java (Entity)
│   │   └── CompanyIATA.java         (ValueObject — cross-aggregate ref)
│   └── repositories/
│       └── AirTransportCompanyRepository.java (Interface)
└── aircraftmodel/
    ├── domain/
    │   ├── AircraftModel.java       (Entity)
    │   └── AircraftModelCode.java   (ValueObject — cross-aggregate ref)
    └── repositories/
        └── AircraftModelRepository.java (Interface)
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

No domain class imports any application or infrastructure class.
Cross-aggregate references are held as identity VOs (`CompanyIATA`, `AircraftModelCode`), never as object references.

---

## GoF Design Patterns

---

### Factory Method (Creational)

> *"Define an interface for creating an object, but let subclasses or factory methods decide which class to instantiate."*

**Applied by:** `PilotId.valueOf()`

```java
// PilotId — static factory method instead of exposing the constructor directly
public static PilotId valueOf(final String licenseNumber) {
    return new PilotId(licenseNumber);   // ← Factory Method
}

// Used in AddPilotController:
final var pilotId = PilotId.valueOf(licenseNumber);
```

**Why Factory Method?** `PilotId.valueOf()` hides the constructor, applies trim/uppercase normalisation before creating the instance, and gives callers a clear intention-revealing name (`valueOf`) instead of `new PilotId(...)`. If the creation logic needs to change (e.g., cache instances, return a subtype), callers are not affected.

The default constructor of `AddPilotController` also uses factory methods via `PersistenceContext`:

```java
public AddPilotController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().pilots(),           // ← Factory Method
            PersistenceContext.repositories().airTransportCompanies(),  // ← Factory Method
            PersistenceContext.repositories().aircraftModels());        // ← Factory Method
}
```

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client. Adapter lets classes work together that otherwise could not because of incompatible interfaces."*

**Applied by:** `JpaPilotRepository`, `JpaAirTransportCompanyRepository`, `JpaAircraftModelRepository`

```java
// Our domain expects this interface:
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
}

// The JPA infrastructure class (adaptee) provides a different interface.
// JpaPilotRepository ADAPTS the EAPLI JPA framework to our domain interface:
public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, PilotId, PilotId>  // ← Adaptee (framework)
        implements PilotRepository {                            // ← Target (our interface)

    @Override
    public Optional<Pilot> findByLicenseNumber(final PilotId pilotId) {
        // adapts the framework's matchOne() to our domain method name
        return matchOne("SELECT p FROM Pilot p WHERE p.pilotId = :id",
                Map.of("id", pilotId));
    }
}
```

**Why Adapter?** `AddPilotController` depends only on `PilotRepository` (the target interface). The JPA framework (`JpaAutoTxRepository`) speaks a different language (`matchOne`, `match`, `count`). The repository implementation **adapts** the framework's interface to the domain's expected interface — without changing either the controller or the framework.

The same applies to `InMemoryPilotRepository`, which adapts `InMemoryDomainRepository` to `PilotRepository` for use in tests.

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from the clients that use it."*

**Applied by:** `PilotRepository` (and `AirTransportCompanyRepository`, `AircraftModelRepository`)

```java
// Strategy interface:
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Iterable<Pilot> findByCompany(CompanyIATA company);
}

// Concrete Strategy A — production:
public class JpaPilotRepository implements PilotRepository {
    public Iterable<Pilot> findByCompany(CompanyIATA company) {
        // JPQL query against real database
    }
}

// Concrete Strategy B — tests:
public class InMemoryPilotRepository implements PilotRepository {
    public Iterable<Pilot> findByCompany(CompanyIATA company) {
        // in-memory stream filter
    }
}

// Context (controller) — unaware of which strategy is active:
public Pilot addPilot(...) {
    return pilotRepo.save(pilot);   // ← same call regardless of strategy
}
```

**Why Strategy?** The persistence algorithm (JPA vs. in-memory) is swapped without changing the controller. The strategy is injected via the constructor — production code uses `JpaPilotRepository`, tests inject `InMemoryPilotRepository`.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `allCompanies()`, `allAircraftModels()`

```java
// Controller returns Iterable — the Iterator pattern via Java's standard interface:
public Iterable<AirTransportCompany> allCompanies() {
    return companyRepo.findAll();   // ← returns an Iterator-backed Iterable
}

public Iterable<AircraftModel> allAircraftModels() {
    return modelRepo.findAll();     // ← same
}

// UI uses it without knowing whether it's a List, Set, JPA ScrollableResults, etc.:
for (AirTransportCompany company : controller.allCompanies()) {
    // display company
}
```

**Why Iterator?** The UI iterates over companies and aircraft models without ever knowing the underlying data structure (JPA result list, in-memory collection, or lazy cursor). The `Iterable<T>` interface is Java's built-in implementation of the Iterator pattern.

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use."*

**Applied by:** `AddPilotController`

```java
// The DOMAIN subsystem has multiple classes:
//   PilotId (VO), Pilot (entity), PilotRepository, AirTransportCompanyRepository,
//   AircraftModelRepository, AuthorizationService
//
// The UI only needs to know ONE class — the controller (Facade):

AddPilotController controller = new AddPilotController();

// Step 1: populate companies combo (hides companyRepo internals)
Iterable<AirTransportCompany> companies = controller.allCompanies();

// Step 2: populate model multi-select (hides modelRepo internals)
Iterable<AircraftModel> models = controller.allAircraftModels();

// Step 3: create pilot (hides PilotId creation, Pilot constructor, authz, pilotRepo.save())
Pilot pilot = controller.addPilot(licenseNumber, company, certifiedModels, certificationDate);
```

**Why Facade?** Without the controller, the UI would have to import and instantiate `PilotId`, `Pilot`, `PilotRepository`, `AirTransportCompanyRepository`, `AircraftModelRepository`, and `AuthorizationService` — six subsystem classes. The controller provides a single simplified entry point that hides all this complexity.
