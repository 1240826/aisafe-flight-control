# US073 — SOLID & GRASP Principles Applied

Application to the **Create Flight Route** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

Each class in US073 has exactly one responsibility:

#### `FlightRoute` — Aggregate root for a flight route

```java
@Entity
public class FlightRoute implements AggregateRoot<FlightRouteName> {
    @EmbeddedId private FlightRouteName name;
    @Embedded private CompanyIATA companyIATA;
    @Embedded private AirportIATA origin;
    @Embedded private AirportIATA destination;
    private LocalDate deactivationDate;
}
```

**Why SRP?** `FlightRoute` only manages identity, company, airports, and active/deactivated state. It does NOT validate route name format, look up airports, or persist itself.

#### `FlightRouteName` — Value Object: route name format validation

```java
public FlightRouteName(final String name) {
    Invariants.ensure(trimmed.matches("[A-Z]{2}\\d{1,4}"),
            "Flight route name must be 2 letters followed by 1-4 digits (e.g. 'TP123')");
}
```

**Why SRP?** `FlightRouteName` only validates and stores the route name string. It does NOT know about the FlightRoute entity, airports, or company.

#### `CreateFlightRouteController` — Orchestration only

```java
public FlightRoute createFlightRoute(final String routeName, final String companyIata,
                                      final String originCode, final String destinationCode) {
    authz.ensureAuthenticatedUserHasAnyOf(ATC_COLLABORATOR);
    final var name = FlightRouteName.valueOf(routeName);
    final var existing = routeRepo.ofIdentity(name);
    if (existing.isPresent()) throw new IllegalArgumentException("Route already exists");
    final var route = new FlightRoute(name, company, origin, destination);
    return routeRepo.save(route);
}
```

| Class | Single Responsibility |
|-------|----------------------|
| `FlightRouteRepository` | Contract for flight route persistence |
| `AirportRepository` | Contract for airport lookup |
| `AirTransportCompanyRepository` | Contract for company lookup |
| `AirportIATA` | Value Object: airport IATA code format |

---

### O — Open/Closed Principle

New route validation rules (e.g. no overlapping routes between the same airports) can be added as new repository queries without modifying existing methods. New query methods can be added to `FlightRouteRepository` without changing `CreateFlightRouteController`.

### L — Liskov Substitution Principle

`FlightRouteRepository` — both `JpaFlightRouteRepository` and `InMemoryFlightRouteRepository` implement the same interface and are interchangeable in the controller.

### I — Interface Segregation Principle

`FlightRouteRepository` only exposes exactly the methods needed: `ofIdentity()`, `findAllActive()`, `save()`. No method forces the UI to deal with unrelated operations.

### D — Dependency Inversion Principle

`CreateFlightRouteController` depends on abstractions (`FlightRouteRepository`, `AirportRepository`, `AuthorizationService`) — never on concrete JPA or InMemory implementations.

---

## GRASP Principles

### Controller (GRASP)

`CreateFlightRouteController` is the single entry point for the "create flight route" system operation.

### Creator (GRASP)

| Creator | Creates | Rationale |
|---------|---------|-----------|
| `CreateFlightRouteController` | `FlightRouteName`, `CompanyIATA`, `AirportIATA` | Controller has all the input data |
| `CreateFlightRouteController` | `FlightRoute` | Controller has all needed data |

### Information Expert (GRASP)

| Responsibility | Expert | Why |
|---------------|--------|-----|
| Route name format validation | `FlightRouteName` | Owns the regex |
| Origin/dest different invariant | `FlightRoute` | Owns both airports |
| Route uniqueness check | `FlightRouteRepository` | Repository knows all routes |
| Authorization | `AuthorizationService` | Framework service |

### Pure Fabrication (GRASP)

`CreateFlightRouteController`, `FlightRouteRepository` — pure fabrications with no real-world domain counterpart.

### Indirection (GRASP)

`FlightRouteRepository` mediates between controller and persistence. `CreateFlightRouteController` mediates between UI and domain.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── flightroute/
│   ├── domain/
│   │   ├── FlightRoute.java         (Entity, AggregateRoot)
│   │   └── FlightRouteName.java     (ValueObject)
│   ├── repositories/
│   │   └── FlightRouteRepository.java
│   └── application/
│       └── CreateFlightRouteController.java
├── airport/
│   ├── domain/
│   │   └── AirportIATA.java         (ValueObject)
│   └── repositories/
│       └── AirportRepository.java
└── company/
    ├── domain/
    │   └── CompanyIATA.java         (ValueObject)
    └── repositories/
        └── AirTransportCompanyRepository.java
```

---

## GoF Design Patterns

### Factory Method

`FlightRouteName.valueOf()` — static factory hiding the constructor with validation.

### Adapter

`JpaFlightRouteRepository` adapts EAPLI JPA framework to `FlightRouteRepository` interface.

### Strategy

`FlightRouteRepository` — JPA vs InMemory strategies swapped via injection.

### Facade

`CreateFlightRouteController` provides a unified interface over `FlightRouteRepository`, `AirportRepository`, `CompanyRepository`, and `AuthorizationService`.
