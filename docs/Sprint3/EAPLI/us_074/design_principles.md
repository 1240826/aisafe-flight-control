# US074 — SOLID & GRASP Principles Applied

Application to the **Delete (Deactivate) Flight Route** use case.

---

## SOLID Principles

---

### S — Single Responsibility Principle

#### `FlightRoute` — Domain: deactivation state management

```java
public void deactivate(final LocalDate date) {
    Preconditions.noneNull(date);
    Invariants.ensure(isActive(), "Route is already deactivated");
    this.deactivationDate = date;
}
```

**Why SRP?** `FlightRoute` only manages its own deactivation state (`deactivationDate`). It does NOT check flight schedules, handle authorization, or manage persistence.

#### `DeleteFlightRouteController` — Orchestration only

```java
public FlightRoute deactivateRoute(final String routeName, final LocalDate deactivationDate) {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    final FlightRoute route = repo.ofIdentity(name).orElseThrow(...);
    if (flightRepo.existsByRouteNameAndDepartureTimeAfter(name, deactivationDate.atStartOfDay()))
        throw new IllegalStateException(...);
    route.deactivate(deactivationDate);
    return repo.save(route);
}
```

#### `FlightRouteRepository` — Route persistence queries

```java
public interface FlightRouteRepository extends DomainRepository<FlightRouteName, FlightRoute> {
    Iterable<FlightRoute> findAllActive();
}
```

#### `FlightRepository` — Cross-aggregate query for flight schedules

```java
public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {
    boolean existsByRouteNameAndDepartureTimeAfter(FlightRouteName name, LocalDateTime dateTime);
}
```

---

### O — Open/Closed Principle

New deactivation rules (e.g. require manager approval) can be added without modifying `FlightRoute`. The business rule check (planned flights) is in the controller and can be extended.

### L — Liskov Substitution Principle

`FlightRouteRepository` and `FlightRepository` — JPA and InMemory implementations are fully interchangeable.

### I — Interface Segregation Principle

Each repository exposes only the methods needed by US074: `findAllActive()`, `ofIdentity()`, `save()` on `FlightRouteRepository`; `existsByRouteNameAndDepartureTimeAfter()` on `FlightRepository`.

### D — Dependency Inversion Principle

`DeleteFlightRouteController` depends solely on interfaces (`FlightRouteRepository`, `FlightRepository`, `AuthorizationService`).

---

## GRASP Principles

### Controller (GRASP)

`DeleteFlightRouteController` handles the system event "deactivate flight route".

### Information Expert (GRASP)

| Responsibility | Expert | Why |
|---------------|--------|-----|
| Deactivation state | `FlightRoute` | Owns `deactivationDate` |
| Planned flight check | `FlightRepository` | Knows all flight schedules |
| Route existence lookup | `FlightRouteRepository` | Knows all routes |

### Creator (GRASP)

`DeleteFlightRouteController` creates `FlightRouteName` from user input.

### Protected Variations (GRASP)

The `FlightRouteRepository` and `FlightRepository` interfaces protect the controller from persistence technology changes.

### Indirection (GRASP)

Both repositories mediate between controller and persistence layer.

### Pure Fabrication (GRASP)

`DeleteFlightRouteController`, `FlightRouteRepository`, `FlightRepository` — pure fabrications.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── flightroute/
│   ├── domain/
│   │   ├── FlightRoute.java
│   │   └── FlightRouteName.java
│   ├── repositories/
│   │   └── FlightRouteRepository.java
│   └── application/
│       └── DeleteFlightRouteController.java
└── flight/
    ├── repositories/
        └── FlightRepository.java
```

---

## GoF Design Patterns

### Factory Method

`FlightRouteName.valueOf()` — static factory method.

### Adapter

`JpaFlightRouteRepository` adapts JPA framework to domain repository interface.

### Strategy

Repository interfaces enable interchangeable JPA/InMemory strategies.

### Facade

`DeleteFlightRouteController` provides a single entry point over `FlightRouteRepository`, `FlightRepository`, and `AuthorizationService`.
