# US111 — SOLID & GRASP Principles Applied

Application to the **Generate Simulation Report** use case.

---

## SOLID Principles

---

### S — Single Responsibility Principle

#### `GenerateSimulationReportController` — Orchestration

```java
public String generateReport(final String areaCode) {
    if (areaCode == null) throw new IllegalArgumentException("Area code must not be null");
    authz.ensureAuthenticatedUserHasAnyOf(ADMIN, FLIGHT_CONTROL_OPERATOR);
    final Simulation simulation = repo.findByAreaCode(code).findFirst().orElseThrow(...);
    final String outputPath = "reports/simulation_report_" + code + ".txt";
    return writer.writeToFile(simulation, outputPath);
}
```

#### `Simulation` — Aggregate root

```java
@Entity
public class Simulation implements AggregateRoot<Long> {
    @Embedded private AreaCode areaCode;
    @Embedded private SimulationTimeRange timeRange;
    @Embedded private SafetyThreshold safetyThreshold;
    @Embedded private SimulationReport report;
    @Enumerated private ValidationResult validationResult;
}
```

#### `SimulationReport` — Value Object embedded in Simulation

```java
@Embeddable
public class SimulationReport implements ValueObject {
    private String filePath;
    @Lob private String content;
}
```

#### `SimulationReportFileWriter` — File writing service

```java
public class SimulationReportFileWriter {
    public String writeToFile(final Simulation simulation, final String outputPath) {
        Files.writeString(Path.of(outputPath), simulation.report().content());
        return outputPath;
    }
}
```

| Class | Single Responsibility |
|-------|----------------------|
| `SafetyThreshold` | Value Object: threshold value and unit |
| `SimulationTimeRange` | Value Object: time window with end-after-start invariant |
| `ValidationResult` | Enum: PASSED, FAILED, PENDING |
| `SimulationRepository` | Contract for simulation persistence |

---

### O — Open/Closed Principle

New output formats (e.g. JSON, PDF) can be added by extending `SimulationReportFileWriter` without modifying the controller.

### L — Liskov Substitution Principle

`SimulationRepository` — `JpaSimulationRepository` and `InMemorySimulationRepository` are interchangeable.

### I — Interface Segregation Principle

`SimulationRepository` exposes only `findAll()`, `findByAreaCode()`, `findByValidationResult()`, `save()` — exactly what the controller needs.

### D — Dependency Inversion Principle

Controller depends on `SimulationRepository` (interface) and `SimulationReportFileWriter` (service), not on concrete persistence or file I/O implementations.

---

## GRASP Principles

### Controller (GRASP)

`GenerateSimulationReportController` handles the "generate simulation report" system event.

### Information Expert (GRASP)

| Responsibility | Expert |
|---------------|--------|
| Report content | `SimulationReport` (embedded in `Simulation`) |
| Simulation lookup by area | `SimulationRepository` |
| File writing | `SimulationReportFileWriter` |
| Authorization | `AuthorizationService` |

### Pure Fabrication (GRASP)

`GenerateSimulationReportController`, `SimulationRepository`, `SimulationReportFileWriter` — pure fabrications.

### Indirection (GRASP)

`SimulationRepository` mediates between controller and database. `SimulationReportFileWriter` mediates between controller and file system.

---

## Package & Layer Architecture

```
core/src/main/java/eapli/aisafe/
├── simulation/
│   ├── domain/
│   │   ├── Simulation.java
│   │   ├── SimulationReport.java
│   │   ├── SimulationTimeRange.java
│   │   ├── SafetyThreshold.java
│   │   └── ValidationResult.java
│   ├── repositories/
│   │   └── SimulationRepository.java
│   └── application/
│       ├── GenerateSimulationReportController.java
│       └── SimulationReportFileWriter.java
└── aircontrolarea/
    └── domain/
        └── AreaCode.java
```

---

## GoF Design Patterns

### Factory Method

`AreaCode.valueOf()` — static factory method.

### Adapter

`JpaSimulationRepository` adapts EAPLI JPA to `SimulationRepository` interface.

### Strategy

`SimulationRepository` enables interchangeable JPA/InMemory persistence strategies.

### Facade

The controller provides a unified interface over `SimulationRepository`, `SimulationReportFileWriter`, and `AuthorizationService`.
