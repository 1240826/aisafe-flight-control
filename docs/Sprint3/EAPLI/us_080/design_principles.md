# US080 — SOLID & GRASP Principles Applied

Application to the **Create Flight Plan** use case.

---

## SOLID Principles

---

### S — Single Responsibility Principle

#### `FlightPlan` — Aggregate root

```java
@Entity
public class FlightPlan implements AggregateRoot<FlightPlanId> {
    @EmbeddedId private FlightPlanId flightPlanId;
    @Enumerated private FlightPlanStatus status;
    @Lob private String dslContent;
}
```

**Why SRP?** `FlightPlan` only manages its identity, status, and DSL content. It does not validate pilot-company alignment, look up routes, or check aircraft availability.

#### `FlightPlanStatus` — Enum with lifecycle states

```java
public enum FlightPlanStatus {
    DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED
}
```

#### `Flight` — Entity linking route, aircraft, pilot, and flight plan

```java
@Entity
public class Flight implements AggregateRoot<FlightDesignator> {
    @EmbeddedId private FlightDesignator designator;
    private LocalDateTime departureTime;
    @Embedded private FlightRouteName routeName;
    private String aircraftRegistration;
    @Embedded private PilotId pilotId;
    @Enumerated private FlightType flightType;
    @OneToMany(cascade = ALL) private List<FlightPlan> flightPlans;
}
```

| Class | Single Responsibility |
|-------|----------------------|
| `FlightRoute` | Route definition (origin, destination, company) |
| `Pilot` | Pilot identity, certifications, active state |
| `Aircraft` | Aircraft registration and operational status |
| `FlightDesignator` | Value Object: flight designator format |
| `FlightPlanId` | Value Object: flight plan ID format |

---

### O — Open/Closed Principle

New flight plan validation rules (e.g. crew rest time, aircraft range checks) can be added as new repository queries or domain guards without modifying existing `Flight` or `FlightPlan` code.

### L — Liskov Substitution Principle

`FlightRepository`, `PilotRepository`, `AircraftRepository` — JPA and InMemory implementations are interchangeable.

### I — Interface Segregation Principle

Each repository exposes only the methods needed by US080: route lookup, pilot lookup by license, aircraft lookup.

### D — Dependency Inversion Principle

Controllers depend on repository interfaces and the `AuthorizationService` interface, never on concrete implementations.

---

## GRASP Principles

### Controller (GRASP)

The create flight plan controller orchestrates pilot resolution, route validation, company alignment check, and persistence.

### Creator (GRASP)

The controller creates `FlightDesignator`, `FlightPlanId`, `FlightPlan`, and `Flight` instances.

### Information Expert (GRASP)

| Responsibility | Expert |
|---------------|--------|
| Pilot-route company alignment | Controller (compares `Pilot.company()` vs `FlightRoute.company()`) |
| Draft status initialization | `FlightPlan` constructor sets `DRAFT` |

### Protected Variations (GRASP)

Repository interfaces protect against persistence technology changes.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── flightplan/
│   ├── domain/
│   │   ├── FlightPlan.java
│   │   ├── FlightPlanId.java
│   │   └── FlightPlanStatus.java
│   ├── repositories/
│   │   └── FlightPlanRepository.java
│   └── application/
│       └── ImportFlightPlanController.java
├── flight/
│   ├── domain/
│   │   ├── Flight.java
│   │   ├── FlightDesignator.java
│   │   └── FlightType.java
│   └── repositories/
│       └── FlightRepository.java
├── pilot/
│   └── domain/
│       ├── Pilot.java
│       └── PilotId.java
└── aircraft/
    └── domain/
        ├── Aircraft.java
        └── RegistrationNumber.java
```

---

## GoF Design Patterns

### Factory Method

`FlightDesignator.valueOf()`, `FlightPlanId.valueOf()` — static factory methods.

### Adapter

JPA repository implementations adapt the EAPLI framework to domain repository interfaces.

### Strategy

Repository interfaces allow swapping persistence strategies (JPA/InMemory).

### Facade

The controller provides a unified interface over multiple repositories, authorization, and domain objects.
