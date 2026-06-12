# US076 — SOLID & GRASP Principles Applied

Application to the **List Company Pilot Roster** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

Each class in US076 has exactly one responsibility:

#### `Pilot` — Aggregate root: identity, state and certified models

```java
@Entity
@Table(name = "PILOT")
public class Pilot implements AggregateRoot<PilotId> {

    @EmbeddedId
    private PilotId pilotId;

    @Embedded
    private CompanyIATA company;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<AircraftModelCode> certifiedModels = new HashSet<>();

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "CERTIFICATION_DATE", nullable = false)
    private LocalDate certificationDate;
}
```

**Why SRP?** `Pilot` only stores and exposes its own data. It does NOT filter, sort, or format pilot lists — that is the repository's and UI's responsibility.

#### `ListPilotRosterController` — Orchestration only

```java
@UseCaseController
public class ListPilotRosterController {

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }

    public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return pilotRepo.findByCompany(company);
    }

    public Iterable<Pilot> listActiveCompanyPilots(final CompanyIATA company) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return pilotRepo.findActiveByCompany(company);
    }
}
```

**Why SRP?** The controller only **orchestrates** — it authorises and delegates to the repository. It does NOT format output, filter results in memory, or access the database directly.

#### Other classes and their single reasons:

| Class | Single Responsibility |
|-------|----------------------|
| `PilotRepository` | Contract for pilot persistence queries, including company filtering |
| `AirTransportCompanyRepository` | Contract for company queries (`findAll()`) |
| `CompanyIATA` | Value Object: company IATA code identity |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: `ListPilotRosterController` — new listing methods via extension

Adding a new query method (e.g. `listCertifiedForModel(AircraftModelCode)`) requires only adding the method to the controller and a matching method to `PilotRepository`. No existing `listCompanyPilots()` or `listActiveCompanyPilots()` method needs to change.

#### Example 2: `PilotRepository` — new queries extend the interface

```java
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
    boolean hasAssignedFlights(PilotId pilotId);
}
```

A new query (e.g. `findByCertifiedModel(AircraftModelCode)`) can be added to the interface and implemented without modifying any of the existing methods or their callers.

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example: `PilotRepository` — JPA and InMemory are interchangeable

```java
// Interface (abstraction)
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
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

**Why LSP?** `ListPilotRosterController` depends only on `PilotRepository`. Whether the runtime instance is `JpaPilotRepository` or `InMemoryPilotRepository` is irrelevant — both honour the same contract and the controller behaves identically.

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example: `ListPilotRosterController` uses only two focused methods

```java
public Iterable<AirTransportCompany> allCompanies() {
    // uses: companyRepo.findAll()
}

public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
    // uses: pilotRepo.findByCompany(company)
}

public Iterable<Pilot> listActiveCompanyPilots(final CompanyIATA company) {
    // uses: pilotRepo.findActiveByCompany(company)
}
```

`DomainRepository` already provides `save()`, `findAll()`, `ofIdentity()`, etc. `PilotRepository` adds only the company-filtered queries needed by US076 and US077 — no bloated interface.

The controller itself is also segregated: it exposes three focused methods. A UI that only needs the active roster calls only `listActiveCompanyPilots()` and is not forced to deal with `addPilot()` or `deactivatePilot()` (those are in their own controllers).

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### Example: `ListPilotRosterController` depends entirely on abstractions

```java
@UseCaseController
public class ListPilotRosterController {

    // All dependencies are ABSTRACT types:
    private final AuthorizationService authz;              // interface (framework)
    private final PilotRepository pilotRepo;               // interface (our code)
    private final AirTransportCompanyRepository companyRepo; // interface (our code)

    // Package-private constructor — accepts abstractions (used by tests)
    ListPilotRosterController(final AuthorizationService authz,
                               final PilotRepository pilotRepo,
                               final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
    }
}
```

**Why DIP?** The controller never imports or instantiates `JpaPilotRepository`, `JpaAirTransportCompanyRepository`, or any concrete authz implementation. It depends only on interfaces — production and test wiring is done at the entry point.

The **default constructor** injects via static factories:

```java
public ListPilotRosterController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().pilots(),
            PersistenceContext.repositories().airTransportCompanies());
}
```

---

## GRASP Principles

---

### Controller (GRASP)

> *Assign the responsibility of handling system events to a non-UI class that represents the overall use case.*

**Applied by:** `ListPilotRosterController`

```java
@UseCaseController
public class ListPilotRosterController {
    public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR); // 1. Authorize
        return pilotRepo.findByCompany(company);                              // 2. Delegate to repo
    }
}
```

The controller is the single point of entry for the "list pilot roster" system operation. The UI never touches repositories directly.

---

### Creator (GRASP)

> *Assign responsibility for creating instances of class A to class B when B contains, aggregates, records, or closely uses A.*

US076 is a pure **query** use case — no domain objects are created during the operation. The `Pilot` objects returned already exist in the repository. The creation of `CompanyIATA` (as the filter argument) is the UI's responsibility, passing it as a parameter to the controller.

---

### Information Expert (GRASP)

> *Assign a responsibility to the class that has the information needed to fulfill it.*

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Filtering pilots by company | `PilotRepository` | The repository holds all pilot records and their company associations — it is the expert on filtering |
| Filtering active pilots | `PilotRepository` | Same — the repository owns the `ACTIVE` column data |
| Providing all companies (for the selection step) | `AirTransportCompanyRepository` | Holds all company records |
| Pilot's own active/inactive state | `Pilot` | `Pilot` owns `active` flag and exposes `isActive()` |
| Authorization check | `AuthorizationService` | Framework service knows authenticated user and roles |

```java
// PilotRepository is the expert on filtering:
Iterable<Pilot> findByCompany(CompanyIATA company);          // ← expert
Iterable<Pilot> findActiveByCompany(CompanyIATA company);    // ← expert
```

---

### High Cohesion (GRASP)

> *Keep responsibilities strongly related and focused within each class.*

```
Low Cohesion (bad):                          High Cohesion (our design):
┌──────────────────────────────┐             ┌──────────────────────────┐
│  ListPilotRosterController   │             │  PilotRepository         │
│  - queryDatabase()           │             │  - findByCompany()        │
│  - filterByStatus()          │             │  - findActiveByCompany()  │
│  - sortByName()              │             └──────────────────────────┘
│  - formatHtmlTable()         │
│  - checkPermissions()        │             ┌──────────────────────────┐
└──────────────────────────────┘             │  ListPilotRosterController│
                                              │  - authorize              │
Each class in our design is cohesive:        │  - delegate to repo       │
                                              └──────────────────────────┘
• Query:       PilotRepository.findByCompany / findActiveByCompany
• Support:     AirTransportCompanyRepository.findAll
• Orchestrate: ListPilotRosterController
```

---

### Low Coupling (GRASP)

> *Keep dependencies between classes as weak as possible.*

```
ListPilotRosterController
    │
    ├──→ PilotRepository               (interface — switchable JPA ↔ InMemory)
    ├──→ AirTransportCompanyRepository (interface — switchable JPA ↔ InMemory)
    └──→ AuthorizationService          (interface — framework abstraction)
```

```java
// The controller never imports JPA or InMemory classes:
import eapli.aisafe.pilot.repositories.PilotRepository;                    // ← interface only
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;    // ← interface only
import eapli.framework.infrastructure.authz.application.AuthorizationService; // ← interface
```

`Pilot` itself has **zero dependencies** on any application or infrastructure class.

---

### Polymorphism (GRASP)

> *Use polymorphic operations to handle variations in behavior based on type.*

#### Repository polymorphism

```java
// Used polymorphically — controller never cares which implementation is active:
PilotRepository repo = isTestEnvironment()
        ? new InMemoryPilotRepository()
        : new JpaPilotRepository("aisafe");
```

#### `Pilot.isActive()` — behaviour varies by state

```java
public boolean isActive() {
    return active;
}
```

Any consumer (UI, controller, or test) calls the same method regardless of whether the pilot was ever deactivated — the boolean state encapsulates the variation.

---

### Protected Variations (GRASP)

> *Protect the system from variations in external components by wrapping them behind stable interfaces.*

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| Persistence technology | `PilotRepository` interface | Switching JPA ↔ file ↔ NoSQL means swapping the implementation class, not touching the controller |
| Company filter SQL / query strategy | `PilotRepository.findByCompany()` | Changes to the JPQL query affect only the repository implementation |
| Active-filter strategy | `PilotRepository.findActiveByCompany()` | Controller is never aware of how "active" is stored |
| Authentication mechanism | `AuthorizationService` | Framework-level change; controller code stays identical |

```java
// The controller is protected from persistence and filter variations:
public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    // If the query strategy changes → only PilotRepository implementation changes
    return pilotRepo.findByCompany(company);
    // → Controller code stays identical
}
```

---

### Pure Fabrication (GRASP)

> *Create artificial classes that do not represent domain concepts to achieve low coupling and high cohesion.*

| Class | Why It's a Fabrication |
|-------|----------------------|
| `ListPilotRosterController` | The aviation domain has no "list roster controller" — it is a use-case artifact that orchestrates authorization and repository delegation |
| `PilotRepository` | Repositories do not exist in the real world — they are technical abstractions for persistence |
| `AirTransportCompanyRepository` | Same |

```java
// Without Pure Fabrication, Pilot would need to know about persistence:
public class Pilot {
    public static List<Pilot> findAllByCompany(CompanyIATA c) { /* SQL here? */ }  // ← BAD!
}

// With Pure Fabrication:
public interface PilotRepository {           // ← PURE FABRICATION
    Iterable<Pilot> findByCompany(CompanyIATA company);
}
```

---

### Indirection (GRASP)

> *Assign the responsibility of mediating between two components to an intermediate object.*

| Mediator | Mediates Between | Why |
|----------|-----------------|-----|
| `PilotRepository` | Controller ↔ Persistence (JPA / InMemory) | Controller never touches JPA code |
| `AirTransportCompanyRepository` | Controller ↔ Company persistence | Controller never queries company tables directly |
| `ListPilotRosterController` | UI ↔ Domain + Repositories | UI never calls repositories directly |

```java
// Without Indirection: UI queries database directly
public class ListPilotRosterUI {
    public void doIt() {
        var repo = new JpaPilotRepository("aisafe");  // ← BAD! tight coupling
        repo.findByCompany(selectedCompany).forEach(this::display);
    }
}

// With Indirection:
public class ListPilotRosterController {   // ← INDIRECTION
    public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return pilotRepo.findByCompany(company);  // ← UI never knows about JPA
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
│       └── ListPilotRosterController.java (@UseCaseController)
└── company/
    ├── domain/
    │   ├── AirTransportCompany.java (Entity)
    │   └── CompanyIATA.java         (ValueObject — filter parameter)
    └── repositories/
        └── AirTransportCompanyRepository.java (Interface)
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
The controller returns `Iterable<Pilot>` directly — DTOs are not required for this read-only listing use case.

---

## GoF Design Patterns

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client. Adapter lets classes work together that otherwise could not because of incompatible interfaces."*

**Applied by:** `JpaPilotRepository`, `JpaAirTransportCompanyRepository`

```java
// Our domain expects this interface (Target):
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
}

// JpaPilotRepository ADAPTS the EAPLI JPA framework (Adaptee) to our interface (Target):
public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, PilotId, PilotId>  // ← Adaptee (framework)
        implements PilotRepository {                            // ← Target (our interface)

    @Override
    public Iterable<Pilot> findByCompany(final CompanyIATA company) {
        // adapts the framework's match() to our domain method
        return match("SELECT p FROM Pilot p WHERE p.company = :c",
                Map.of("c", company));
    }

    @Override
    public Iterable<Pilot> findActiveByCompany(final CompanyIATA company) {
        return match("SELECT p FROM Pilot p WHERE p.company = :c AND p.active = true",
                Map.of("c", company));
    }
}
```

**Why Adapter?** `ListPilotRosterController` depends only on `PilotRepository` (the target interface). The JPA framework speaks a different language (`match`, `matchOne`). The repository implementation **adapts** the framework to the controller's expected interface — without changing either.

The `InMemoryPilotRepository` is a second adapter that converts `InMemoryDomainRepository` to `PilotRepository` for use in unit tests.

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from the clients that use it."*

**Applied by:** `PilotRepository` and `AirTransportCompanyRepository`

```java
// Strategy interface:
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Iterable<Pilot> findByCompany(CompanyIATA company);
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);
}

// Concrete Strategy A — production (JPQL query):
public class JpaPilotRepository implements PilotRepository {
    public Iterable<Pilot> findActiveByCompany(CompanyIATA company) {
        return match("SELECT p FROM Pilot p WHERE p.company = :c AND p.active = true", ...);
    }
}

// Concrete Strategy B — tests (in-memory stream filter):
public class InMemoryPilotRepository implements PilotRepository {
    public Iterable<Pilot> findActiveByCompany(CompanyIATA company) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .filter(p -> p.company().equals(company) && p.isActive())
                .collect(Collectors.toList());
    }
}

// Context (controller) — unaware of which strategy is active:
public Iterable<Pilot> listActiveCompanyPilots(final CompanyIATA company) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    return pilotRepo.findActiveByCompany(company);  // ← same call regardless of strategy
}
```

**Why Strategy?** The filtering algorithm (SQL vs. in-memory) is completely swapped without any change to the controller. The strategy is injected via the constructor.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `listCompanyPilots()`, `listActiveCompanyPilots()`, `allCompanies()`

```java
// All query methods return Iterable — Java's built-in Iterator pattern:
public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    return pilotRepo.findByCompany(company);   // ← Iterable<Pilot>
}

public Iterable<AirTransportCompany> allCompanies() {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    return companyRepo.findAll();              // ← Iterable<AirTransportCompany>
}

// UI iterates without knowing the underlying data structure:
for (Pilot pilot : controller.listCompanyPilots(selectedCompany)) {
    // display pilot row
}
```

**Why Iterator?** The UI never knows whether the collection is a `List`, a `Set`, a JPA `ScrollableResults`, or a lazy in-memory stream. The `Iterable<T>` / `Iterator<T>` interfaces decouple the traversal from the storage representation — the fundamental purpose of this pattern.

This is especially important for the pilot roster: a company may have hundreds of pilots, and a lazy JPA cursor (which is still `Iterable`) avoids loading all of them into memory at once.

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use."*

**Applied by:** `ListPilotRosterController`

```java
// The DOMAIN subsystem has multiple classes:
//   Pilot (entity), PilotId (VO), PilotRepository,
//   AirTransportCompany (entity), AirTransportCompanyRepository,
//   AuthorizationService
//
// The UI only needs to know ONE class — the controller (Facade):

ListPilotRosterController controller = new ListPilotRosterController();

// Step 1: populate company selection (hides companyRepo, authz check)
Iterable<AirTransportCompany> companies = controller.allCompanies();

// Step 2: list pilots for selected company (hides pilotRepo, authz, filtering)
Iterable<Pilot> pilots = controller.listCompanyPilots(selectedCompany);

// OR: list only active pilots (hides the active filter strategy)
Iterable<Pilot> activePilots = controller.listActiveCompanyPilots(selectedCompany);
```

**Why Facade?** Without the controller, the UI would need to import and use `PilotRepository`, `AirTransportCompanyRepository`, and `AuthorizationService` directly. The controller provides a single simplified entry point that encapsulates authz checking, repository delegation and the choice between all-pilots vs. active-only listing.
