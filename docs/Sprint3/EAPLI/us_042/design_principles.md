# US042 — SOLID & GRASP & GoF Principles Applied

Application to the **Import Bulk Weather Data (CSV)** use case — how each principle shapes the code.

---

## SOLID Principles

---

### S — Single Responsibility Principle

> *"A class should have one, and only one, reason to change."*

#### `WeatherData` — Aggregate root: stores one weather observation

```java
@Entity
@Table(name = "WEATHER_DATA")
public class WeatherData implements AggregateRoot<Long> {

    private AreaCode areaCode;           // cross-aggregate ref by VO
    private WindCondition windCondition; // embedded VO
    private double temperatureCelsius;
    private String sourceProvider;
    private LocalDateTime recordedDateTime;
}
```

**Why SRP?** `WeatherData` only stores and validates the state of one weather observation. It does NOT parse CSV lines, list areas, or format responses.

#### `WindCondition` — Value Object: wind measurement at a geographic point

```java
@Embeddable
public class WindCondition implements ValueObject {
    private double speedKnots;
    private int directionDegrees;
    private double latitude;
    private double longitude;
    private int altitudeMetres;
}
```

**Why SRP?** `WindCondition` only validates and encapsulates the wind measurement data. It does NOT know about file parsing or persistence.

#### `ImportBulkWeatherDataController` — Orchestration only

```java
@UseCaseController
public class ImportBulkWeatherDataController {

    public ImportResult importFromCsv(final Path csvPath) throws IOException {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON);
        final List<String> lines = Files.readAllLines(csvPath);
        // ... parse headers, parse lines, save each WeatherData
        return new ImportResult(imported, skipped, errors);
    }
}
```

**Why SRP?** The controller only orchestrates the import pipeline (read file → parse headers → parse lines → save). It does NOT format the UI output or validate domain rules beyond parsing.

#### `ImportResult` — Encapsulates import outcome

```java
public static final class ImportResult {
    private final int imported;
    private final int skipped;
    private final List<String> errors;
    // accessors: imported(), skipped(), errors(), hasErrors()
}
```

**Why SRP?** `ImportResult` only carries the summary of an import operation (counts + error messages). No business logic, no persistence.

| Class | Single Responsibility |
|-------|----------------------|
| `WeatherData` | Stores and validates one weather observation |
| `WindCondition` | Validates wind speed, direction, coordinates, altitude |
| `ImportBulkWeatherDataController` | Orchestrates CSV parsing and persistence |
| `ImportResult` | Carries the import outcome (counts + errors) |
| `WeatherDataRepository` | Contract for weather data persistence queries |
| `AirControlAreaRepository` | Contract for area lookup (validates ACA codes in headers) |

---

### O — Open/Closed Principle

> *"Software entities should be open for extension, but closed for modification."*

#### Example 1: New CSV column layouts by extension

The `parseLine()` method checks `EXPECTED_COLUMNS` (currently 12). Adding support for a new CSV format (e.g., 14 columns) would be done by adding a new parsing method or subclassing — not by modifying the existing `parseLine()` logic.

#### Example 2: `ImportResult` — new fields by extension

```java
public static final class ImportResult {
    public int imported() { return imported; }
    public int skipped()  { return skipped;  }
    public List<String> errors() { return errors; }
    public boolean hasErrors()   { return !errors.isEmpty(); }
    // Adding a new summary field (e.g., totalLines) requires only adding the field + accessor
}
```

#### Example 3: `WeatherDataRepository` — new queries by extension

```java
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByAreaCode(AreaCode areaCode);
    // New queries (e.g. findByPeriod()) added without modifying existing methods
}
```

---

### L — Liskov Substitution Principle

> *"Derived types must be substitutable for their base types."*

#### Example: `WeatherDataRepository` — JPA and InMemory are interchangeable

```java
// Interface (abstraction):
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByAreaCode(AreaCode areaCode);
}

// JPA implementation (production):
public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>
        implements WeatherDataRepository { ... }

// InMemory implementation (tests):
public class InMemoryWeatherDataRepository
        extends InMemoryDomainRepository<WeatherData, Long>
        implements WeatherDataRepository { ... }
```

**Why LSP?** `ImportBulkWeatherDataController` depends only on `WeatherDataRepository`. Both implementations satisfy the same contract — `save()` behaves as expected in both cases.

---

### I — Interface Segregation Principle

> *"Clients should not be forced to depend on methods they do not use."*

`ImportBulkWeatherDataController` uses only two focused methods from repositories:

```java
repo.save(wd);                                        // from WeatherDataRepository
acaRepo.ofIdentity(AreaCode.valueOf(areaCodeStr));    // from AirControlAreaRepository (header validation)
```

`DomainRepository` already provides `save()`, `findAll()`, `ofIdentity()`, etc. The controller is not forced to deal with listing, deleting, or searching — it uses only what the import operation needs.

---

### D — Dependency Inversion Principle

> *"Depend on abstractions, not on concretions."*

```java
@UseCaseController
public class ImportBulkWeatherDataController {

    // All dependencies are ABSTRACT types:
    private final AuthorizationService authz;       // interface (framework)
    private final WeatherDataRepository repo;       // interface (our code)
    private final AirControlAreaRepository acaRepo; // interface (our code)

    // Package-private constructor — for test injection:
    ImportBulkWeatherDataController(final AuthorizationService authz,
                                     final WeatherDataRepository repo,
                                     final AirControlAreaRepository acaRepo) {
        this.authz = authz;
        this.repo = repo;
        this.acaRepo = acaRepo;
    }
}
```

The default constructor injects via static factories:

```java
public ImportBulkWeatherDataController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().weatherData(),     // ← returns interface
            PersistenceContext.repositories().airControlAreas()); // ← returns interface
}
```

The controller never imports `JpaWeatherDataRepository` or `JpaAirControlAreaRepository`.

---

## GRASP Principles

---

### Controller (GRASP)

**Applied by:** `ImportBulkWeatherDataController`

```java
public ImportResult importFromCsv(final Path csvPath) throws IOException {
    authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON); // 1. Authorize
    final List<String> lines = Files.readAllLines(csvPath);             // 2. Read file
    // 3. Parse headers (ACA mappings)
    // 4. For each data line: parse → create WeatherData → save
    return new ImportResult(imported, skipped, errors);                 // 5. Return summary
}
```

The controller is the single point of entry for the "import bulk weather data" operation.

---

### Creator (GRASP)

| Creator Class | Creates | Rationale |
|--------------|---------|-----------|
| `ImportBulkWeatherDataController.parseLine()` | `WeatherData` | Controller has all parsed field values needed for construction |
| `ImportBulkWeatherDataController.parseLine()` | `WindCondition` | Controller computes all wind and coordinate values |
| `ImportBulkWeatherDataController.importFromCsv()` | `ImportResult` | Controller has the final counts and error list |
| `AreaCode.valueOf()` | `AreaCode` | Static factory: the class creates itself from a string |

```java
// Controller creates both WindCondition and WeatherData:
return new WeatherData(
        areaCode,
        new WindCondition(windSpeed, direction, centerLat, centerLon, midAltMetres), // ← Creator
        0.0,
        "WEATHER_FORECAST",
        dateTime);  // ← Creator
```

---

### Information Expert (GRASP)

| Responsibility | Expert Class | Why |
|---------------|-------------|-----|
| Wind measurement validation (speed > 0, dir 0–360°, lat/lon, altitude ≥ 0) | `WindCondition` | WindCondition owns all wind and coordinate fields |
| Weather observation validation (provider not blank, fields not null) | `WeatherData` | WeatherData owns the observation fields |
| ACA code format | `AreaCode` | AreaCode knows its own format rules |
| ACA existence check | `AirControlAreaRepository` | Repository holds all registered ACA records |
| Parsing European decimals (comma → period) | `ImportBulkWeatherDataController` | Controller owns the CSV parsing logic |
| Authorization | `AuthorizationService` | Framework service knows the current user's roles |

```java
// WindCondition is the expert on its own invariants:
Invariants.ensure(speedKnots > 0, "Wind speed must be strictly positive");
Invariants.ensure(directionDegrees >= 0 && directionDegrees < 360, "...");
Invariants.ensure(latitude >= -90 && latitude <= 90, "...");
```

---

### High Cohesion (GRASP)

```
ImportBulkWeatherDataController:     WindCondition:          WeatherData:
  - read CSV lines                     - validate speed        - validate provider
  - parse headers (ACA map)            - validate direction    - validate not-null fields
  - parse data lines                   - validate lat/lon      - store observation
  - save WeatherData records           - validate altitude
  - return ImportResult
```

Each class has strongly related responsibilities. No class crosses into another's domain.

---

### Low Coupling (GRASP)

```
ImportBulkWeatherDataController
    │
    ├──→ WeatherDataRepository        (interface — switchable JPA ↔ InMemory)
    ├──→ AirControlAreaRepository     (interface — switchable JPA ↔ InMemory)
    └──→ AuthorizationService         (interface — framework abstraction)
```

`WeatherData` and `WindCondition` have **zero dependencies** on application or infrastructure. `AreaCode` (cross-aggregate ref) is held as a VO, not as an object reference to `AirControlArea`.

---

### Protected Variations (GRASP)

| Variation Point | Protected By | What Changes |
|----------------|-------------|--------------|
| CSV column layout | `EXPECTED_COLUMNS` constant + `parseLine()` | Changing the file format affects only parsing logic inside the controller |
| European decimal format | `parseEuropeanDouble()` | Changes to locale-specific parsing affect only that private method |
| Persistence technology | `WeatherDataRepository` interface | Switching JPA ↔ file ↔ NoSQL means swapping the implementation |
| ACA code format rules | `AreaCode.valueOf()` | Format changes affect only `AreaCode` |

---

### Pure Fabrication (GRASP)

| Class | Why It's a Fabrication |
|-------|----------------------|
| `ImportBulkWeatherDataController` | The aviation domain has no "import controller" — it's a use-case artifact |
| `ImportResult` | A domain expert would not recognise this as a real-world aviation concept — it's a technical summary object |
| `WeatherDataRepository` | Repository is a persistence abstraction, not a real-world concept |

---

## GoF Design Patterns

---

### Factory Method (Creational)

> *"Define an interface for creating an object, but let subclasses or factory methods decide which class to instantiate."*

**Applied by:** `AreaCode.valueOf()`, `PersistenceContext.repositories().*()`, `ImportResult` constructor.

```java
// AreaCode uses a static factory method:
acaMapping.put(numericId, AreaCode.valueOf(areaCodeStr));   // ← Factory Method

// PersistenceContext.repositories() is a factory for all repository instances:
public ImportBulkWeatherDataController() {
    this(AuthzRegistry.authorizationService(),
            PersistenceContext.repositories().weatherData(),     // ← Factory Method
            PersistenceContext.repositories().airControlAreas()); // ← Factory Method
}
```

**Why Factory Method?** `AreaCode.valueOf()` hides the constructor and applies validation. `PersistenceContext.repositories()` centralises the decision of which concrete repository implementation to use (JPA, InMemory, or another), without the controller knowing which.

---

### Adapter (Structural)

> *"Convert the interface of a class into another interface expected by the client."*

**Applied by:** `JpaWeatherDataRepository`, `JpaAirControlAreaRepository`

```java
// Target — our domain interface:
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByAreaCode(AreaCode areaCode);
}

// Adapter — translates domain methods to EAPLI JPA framework calls:
public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>  // ← Adaptee (framework)
        implements WeatherDataRepository {                     // ← Target (domain interface)

    @Override
    public Iterable<WeatherData> findByAreaCode(final AreaCode areaCode) {
        return match("SELECT w FROM WeatherData w WHERE w.areaCode = :code",
                Map.of("code", areaCode));  // ← adapts domain method to framework's match()
    }
}
```

**Why Adapter?** The controller calls `repo.save(wd)` — a domain interface method. The adapter translates this into whatever the JPA framework requires internally. Switching the adapter (e.g., to a REST API adapter) requires no change in the controller.

---

### Strategy (Behavioural)

> *"Define a family of algorithms, encapsulate each one, and make them interchangeable."*

**Applied by:** `WeatherDataRepository` and `AirControlAreaRepository`

```java
// Concrete Strategy A — production (JPQL queries):
public class JpaWeatherDataRepository implements WeatherDataRepository { ... }

// Concrete Strategy B — tests (in-memory filter):
public class InMemoryWeatherDataRepository implements WeatherDataRepository { ... }

// Context (controller) — same import logic regardless of strategy:
public ImportResult importFromCsv(final Path csvPath) throws IOException {
    // ...
    repo.save(wd);        // ← same call for JPA or InMemory
    // ...
}
```

**Why Strategy?** Persistence algorithm is swapped via constructor injection. Unit tests inject `InMemoryWeatherDataRepository` and `InMemoryAirControlAreaRepository` — the import logic is tested identically.

---

### Iterator (Behavioural)

> *"Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation."*

**Applied by:** `Files.readAllLines()` + the `for` loop over CSV lines.

```java
// Files.readAllLines() returns a List<String> — List implements Iterable<String> (Iterator pattern):
final List<String> lines = Files.readAllLines(csvPath);

// The controller iterates without knowing whether it's a BufferedReader, MappedByteBuffer, etc.:
for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
    final String line = lines.get(lineNum).trim();
    // parse and save
}
```

Also in the ACA validation step:

```java
// Iterates all registered ACAs without knowing the underlying data structure:
for (final var aca : acaRepo.findAll()) {   // ← Iterator over Iterable<AirControlArea>
    // validate header code exists
}
```

**Why Iterator?** `Files.readAllLines()` returns a `List<String>` — the iterator is Java's standard collection iterator. `acaRepo.findAll()` returns `Iterable<AirControlArea>`, decoupling the traversal from the storage representation (JPA cursor vs. in-memory list).

---

### Facade (Structural)

> *"Provide a unified interface to a set of interfaces in a subsystem."*

**Applied by:** `ImportBulkWeatherDataController`

```java
// The subsystem has:
//   Files (I/O), AreaCode.valueOf(), WindCondition constructor,
//   WeatherData constructor, WeatherDataRepository, AirControlAreaRepository,
//   AuthorizationService, ImportResult builder
//
// The UI only needs to know ONE class:

ImportBulkWeatherDataController controller = new ImportBulkWeatherDataController();
ImportResult result = controller.importFromCsv(Path.of("weather_data.csv"));

System.out.println("Imported: " + result.imported());
if (result.hasErrors()) result.errors().forEach(System.err::println);
```

**Why Facade?** Without the controller, the UI would need to orchestrate file reading, header parsing, ACA code validation, coordinate conversion (feet → metres, bounding box → centroid), VO construction, and repository persistence. The controller hides all of this behind a single method.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── weatherdata/
│   ├── domain/
│   │   ├── WeatherData.java       (Entity, AggregateRoot<Long>)
│   │   └── WindCondition.java     (ValueObject, @Embeddable)
│   ├── repositories/
│   │   └── WeatherDataRepository.java (Interface)
│   └── application/
│       └── ImportBulkWeatherDataController.java (@UseCaseController)
└── aircontrolarea/
    ├── domain/
    │   ├── AirControlArea.java    (Entity)
    │   └── AreaCode.java          (ValueObject — used as cross-aggregate ref)
    └── repositories/
        └── AirControlAreaRepository.java (Interface)
```

### Layer Dependency Rules

```
Controller → Repository (interface boundary)
Controller → Domain (creates VOs and entities)
Repository implementation (infrastructure) → Repository interface
```

No domain class imports any application or infrastructure class.
Cross-aggregate references are held as identity VOs (`AreaCode`), never as object references to `AirControlArea`.
