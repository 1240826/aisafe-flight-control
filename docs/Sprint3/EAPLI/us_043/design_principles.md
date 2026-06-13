# US043 — SOLID & GRASP Principles Applied

Application to the **Consult Weather Data** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

Each class in US043 has exactly one responsibility:

#### `ConsultWeatherDataController` — Orchestration only

```java
@UseCaseController
public class ConsultWeatherDataController {
    public Iterable<WeatherData> consultWeatherData(final String areaCode, final LocalDate date) {
        authz.ensureAuthenticatedUserHasAnyOf(WEATHER_PERSON, PILOT, FLIGHT_CONTROL_OPERATOR);
        return repo.findByAreaCodeAndDate(AreaCode.valueOf(areaCode), date);
    }
}
```

**Why SRP?** The controller only **orchestrates** — it authorizes the user, converts string parameters to domain value objects, and delegates the query to the repository. It does NOT validate dates, format output, or manage transactions.

#### `WeatherData` — Aggregate root: weather record data

```java
@Entity
public class WeatherData implements AggregateRoot<Long> {
    @Embedded private AreaCode areaCode;
    @Embedded private WindCondition windCondition;
    private double temperature;
    private String sourceProvider;
    private LocalDateTime recordedDateTime;
}
```

**Why SRP?** `WeatherData` only manages its own weather record attributes. It does NOT know about the repository, the controller, or how queries are executed.

#### `WeatherDataRepository` — Contract for persistence queries

```java
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByAreaCodeAndDate(AreaCode areaCode, LocalDate date);
}
```

**Why SRP?** The repository is responsible only for declaring data access methods. It does not contain business logic or formatting.

| Class | Single Responsibility |
|-------|----------------------|
| `AreaCode` | Value Object: ACA code format validation |
| `ConsultWeatherDataUI` | Prompts user for area and date, displays results |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example: New roles can query weather data without modifying the controller

Adding a new authorized role (e.g. `ADMIN`) requires only adding it to the `ensureAuthenticatedUserHasAnyOf()` call. The controller logic remains unchanged.

#### Example: New query methods on the repository

New queries (e.g. `findByAreaCodeAndDateRange`, `findByProvider`) can be added to `WeatherDataRepository` without modifying existing methods or the controller.

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example: `WeatherDataRepository` — JPA and InMemory are interchangeable

```java
// Interface
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByAreaCodeAndDate(AreaCode areaCode, LocalDate date);
}

// JPA implementation (production)
public class JpaWeatherDataRepository extends JpaAutoTxRepository<...> implements WeatherDataRepository {}

// InMemory implementation (tests)
public class InMemoryWeatherDataRepository extends InMemoryDomainRepository<...> implements WeatherDataRepository {}
```

`ConsultWeatherDataController` depends only on `WeatherDataRepository`. Whether the runtime instance is JPA or InMemory is irrelevant.

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

#### Example: `WeatherDataRepository` — only the method US043 needs

```java
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByAreaCodeAndDate(AreaCode areaCode, LocalDate date);
}
```

`DomainRepository` already provides `save()`, `findAll()`, `ofIdentity()`. `WeatherDataRepository` adds exactly one method — `findByAreaCodeAndDate`. No method that forces the UI to deal with unrelated data operations.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

#### Example: `ConsultWeatherDataController` depends entirely on abstractions

```java
@UseCaseController
public class ConsultWeatherDataController {
    private final AuthorizationService authz;       // framework interface
    private final WeatherDataRepository repo;       // domain interface

    // Testing constructor accepts abstractions
    ConsultWeatherDataController(final AuthorizationService authz, final WeatherDataRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }
}
```

- `WeatherDataRepository` is an **interface** — the controller never knows about JPA or InMemory.
- `AuthorizationService` is a **framework interface**.

---

## GRASP Principles

---

### Controller (GRASP)

**Applied by:** `ConsultWeatherDataController`

The controller is the single point of entry for the "consult weather data" system operation. The UI never touches domain classes or repositories directly.

```java
public Iterable<WeatherData> consultWeatherData(final String areaCode, final LocalDate date) {
    authz.ensureAuthenticatedUserHasAnyOf(WEATHER_PERSON, PILOT, FLIGHT_CONTROL_OPERATOR);
    return repo.findByAreaCodeAndDate(AreaCode.valueOf(areaCode), date);
}
```

---

### Information Expert (GRASP)

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Area code format validation | `AreaCode` | `AreaCode` owns the format regex |
| Weather data persistence | `WeatherDataRepository` | Repository is the expert on data access |
| Authorization check | `AuthorizationService` | Framework service knows authenticated user and roles |
| Query orchestration | `ConsultWeatherDataController` | Controller has all the parameters needed |

---

### High Cohesion (GRASP)

```
Low Cohesion (bad):                     High Cohesion (our design):
┌─────────────────────────────┐         ┌──────────────────────────┐
│  MegaWeatherController      │         │  ConsultWeatherDataCtrl  │
│  - importWeather()          │         │  - consultWeatherData()  │
│  - consultWeather()         │         └──────────────────────────┘
│  - registerWeather()        │
│  - deleteWeather()          │         ┌──────────────────────────┐
└─────────────────────────────┘         │  WeatherDataRepository   │
                                         │  - findByAreaCodeAndDate │
Each class is cohesive:                  └──────────────────────────┘
- Domain:      WeatherData, AreaCode
- Repository:  WeatherDataRepository
- Application: ConsultWeatherDataController
```

---

### Low Coupling (GRASP)

```
ConsultWeatherDataController
    │
    ├──→ WeatherDataRepository   (interface — switchable JPA ↔ InMemory)
    └──→ AuthorizationService    (interface — framework abstraction)
```

The controller never imports JPA or InMemory classes. Domain classes have zero dependencies on application or infrastructure layers.

---

### Pure Fabrication (GRASP)

| Class | Why It's a Fabrication |
|-------|----------------------|
| `ConsultWeatherDataController` | The weather domain has no "consult weather data controller" — it is a use-case artifact |
| `WeatherDataRepository` | Repositories do not exist in the real world — they are technical abstractions |

---

### Indirection (GRASP)

| Mediator | Mediates Between | Why |
|----------|-----------------|-----|
| `WeatherDataRepository` | Controller ↔ Persistence (JPA / InMemory) | Controller never touches JPA code |
| `ConsultWeatherDataController` | UI ↔ Domain + Repository | UI never calls repository directly |

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── weatherdata/
│   ├── domain/
│   │   ├── WeatherData.java           (Entity, AggregateRoot)
│   │   └── WindCondition.java         (ValueObject)
│   ├── repositories/
│   │   └── WeatherDataRepository.java (Interface)
│   └── application/
│       └── ConsultWeatherDataController.java (@UseCaseController)
└── aircontrolarea/
    └── domain/
        └── AreaCode.java              (ValueObject)
```

### Layer Responsibilities

| Layer | Contains | Dependencies |
|-------|----------|-------------|
| **Domain** | Entities, VOs | eapli framework only |
| **Application** | Controllers | Domain + infrastructure abstractions |
| **Infrastructure** | Persistence (JPA, InMemory) | All of the above |

---

## GoF Design Patterns

---

### Factory Method (Creational)

**Applied by:** `AreaCode.valueOf()`

```java
public static AreaCode valueOf(final String code) {
    return new AreaCode(code);
}
```

Static factory method instead of exposing the constructor directly.

### Adapter (Structural)

**Applied by:** `JpaWeatherDataRepository`

Adapts the EAPLI JPA framework interface to the `WeatherDataRepository` domain interface. The controller depends only on the domain repository interface.

### Strategy (Behavioural)

**Applied by:** `WeatherDataRepository`

The persistence algorithm (JPA vs. in-memory) is swapped without changing the controller. The strategy is injected via the constructor.
