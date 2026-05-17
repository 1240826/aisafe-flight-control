# Implementation Structure — AISafe EAPLI Sprint 2

This document describes the complete code structure of the EAPLI module of AISafe as implemented in Sprint 2. It covers every layer from domain classes to UI, explaining field names, invariants, patterns, and design decisions as they exist in the code.

---

## 1. Project Layout (Maven Multi-Module)

```
aisafe.base/
├── pom.xml                        ← parent POM (Java 21, JUnit 5, Mockito, JaCoCo)
├── core/                          ← domain + application layer
│   └── src/main/java/eapli/aisafe/
│       ├── aircontrolarea/        ← AirControlArea aggregate
│       ├── aircraft/              ← Aircraft aggregate
│       ├── aircraftmodel/         ← AircraftModel aggregate
│       ├── airport/               ← Airport aggregate
│       ├── collaborator/          ← Collaborator aggregate
│       ├── company/               ← AirTransportCompany aggregate
│       ├── enginemodel/           ← EngineModel aggregate
│       ├── manufacturer/          ← Manufacturer aggregate
│       ├── simulation/            ← Simulation aggregate
│       ├── shared/                ← Shared VOs (Coordinates)
│       ├── usermanagement/        ← Roles, password policy (framework boundary)
│       └── weatherdata/           ← WeatherData aggregate
│
├── persistence/                   ← persistence layer (JPA + in-memory)
│   └── src/main/java/eapli/aisafe/
│       ├── infrastructure/persistence/
│       │   ├── PersistenceContext.java        ← repository registry
│       │   └── RepositoryFactory.java         ← factory interface
│       └── persistence/
│           ├── jpa/               ← JPA repository implementations
│           └── inmemory/          ← in-memory repository implementations
│
└── app/                           ← application entry points + UI
    └── src/main/java/eapli/aisafe/
        ├── Application.java
        ├── ui/                    ← console UI classes per aggregate
        ├── bootstrap/             ← bootstrap classes
        └── app/                   ← menu structure
```

Each aggregate follows the same internal layout:

```
<aggregate>/
├── domain/          ← entities, value objects, enums
├── application/     ← controllers (use case)
└── repositories/    ← repository interface
```

---

## 2. Domain Layer

### 2.1 EAPLI Framework Base Classes

Every domain class extends one of:

| Base class | Usage |
|---|---|
| `AggregateRoot<ID>` | Every aggregate root entity |
| `ValueObject` | Every value object |
| `DomainEntity<ID>` | Internal entities (e.g. AircraftVariant) |

Validation utilities used in every constructor:

| Utility | Purpose |
|---|---|
| `Preconditions.noneNull(a, b, ...)` | Rejects null arguments |
| `Invariants.ensure(condition, msg)` | Rejects invalid business state |

---

### 2.2 Manufacturer Aggregate

**Identity:** `ManufacturerName` (`@EmbeddedId`)

```
Manufacturer
├── ManufacturerName name     @EmbeddedId, case-insensitive equals
└── String country
```

**ManufacturerName** invariants: non-null, contains at least one letter. Equality is case-insensitive (`equalsIgnoreCase`).

---

### 2.3 EngineModel Aggregate

**Identity:** `EngineModelCode` (`@EmbeddedId`)

```
EngineModel
├── EngineModelCode code        @EmbeddedId
├── EngineName engineName       name field; non-blank
├── String manufacturerName     cross-ref to Manufacturer by name
├── String fuelType             plain string (e.g. JET_A1)
├── MotorizationType motorizationType
├── Power power                 value + unit (kW/hp/W), value > 0
├── Thrust staticThrust         value + unit (N/kN/lbf) + speedRef="static"
├── Thrust cruiseThrust         value + unit + speedRef="cruise"
└── TSFC tsfc                   value + unit (g/kN/s / lb/(lbf.h) / mg/N/s)
```

**Key invariants:**
- `EngineName + manufacturerName` must be unique (enforced by controller, not domain).
- `Power.value > 0`, `Thrust.value > 0`, `TSFC.value > 0`.
- `Thrust.speedReference` must be one of `{"static", "cruise"}`.
- Two separate `Thrust` fields (`staticThrust`, `cruiseThrust`) rather than a collection — simpler JPA mapping.

**MotorizationType** enum: `TURBOPROP`, `TURBOFAN`, `TURBOJET`, `RAMJET`, `ELECTRIC_PROPELLER`

---

### 2.4 AircraftModel Aggregate

**Identity:** `AircraftModelCode` (`@EmbeddedId`)

```
AircraftModel
├── AircraftModelCode code        @EmbeddedId (ICAO designator, e.g. B738)
├── String name                   human-readable model name
├── ManufacturerName manufacturerName  cross-ref to Manufacturer
├── AircraftType aircraftType     PASSENGER / CARGO / MIXED
├── Integer maxPassengers         maximum cabin capacity
├── AircraftWeights aircraftWeights
├── AircraftPerformance aircraftPerformance
├── AerodynamicCoefficients aerodynamicCoefficients
└── List<AircraftVariant> variants  @OneToMany(cascade=ALL, orphanRemoval=true)
```

**AircraftWeights** invariants: `emptyWeight`, `mtow`, `mzfw`, `maxFuelCapacity` all > 0; `mtow > mzfw > emptyWeight`.

**AircraftPerformance** invariants: `serviceCeiling`, `cruiseSpeed`, `maximumRange` all > 0.

**AerodynamicCoefficients** invariants: `wingArea`, `dragCoefficient`, `liftCoefficient` all > 0.

**AircraftVariant** is a local `@Entity` (not a VO — it has an add/remove lifecycle via US057/US058):
```
AircraftVariant
├── Long id              @GeneratedValue auto-generated identity
├── EngineModelCode engineModelCode   cross-ref to EngineModel
└── MotorizationType motorizationType
```

**Domain methods on AircraftModel:**
- `addVariant(EngineModelCode, MotorizationType)` — enforces: no duplicate engine, all variants must share the same `MotorizationType`.
- `removeVariant(EngineModelCode)` — enforces: at least one variant must remain.

---

### 2.5 AirControlArea Aggregate

**Identity:** `AreaCode` (`@EmbeddedId`)

```
AirControlArea
├── AreaCode code              @EmbeddedId
├── AreaName name              unique operational name
├── BoundingBox boundary       rectangle boundary
└── int maxAltitudeMetres      from Application.settings(); default 14 000
```

**BoundingBox** invariants: `minLat < maxLat`, `minLon < maxLon`, all values in valid geographic ranges (`[-90,90]` lat, `[-180,180]` lon).

**AreaName** invariant: non-blank, contains at least one letter. Uniqueness enforced by controller via `findByName`.

**Domain methods on AirControlArea:**
- `containsCoordinates(double lat, double lon)` — delegates to `BoundingBox.contains()`. Used by `CreateAirportController` to validate airport location.
- `overlapsWith(double minLat, double maxLat, double minLon, double maxLon)` — delegates to `BoundingBox.overlaps()`. Used by `RegisterAirControlAreaController` to reject overlapping areas.

**Controller cross-aggregate invariants (enforced before save):**
1. `AreaCode` unique — `acaRepo.findByAreaCode(code)` must return empty.
2. `AreaName` unique — `acaRepo.findByName(name)` must return empty.
3. No overlap — `acaRepo.findOverlapping(minLat, maxLat, minLon, maxLon)` must return empty.
4. `maxAltitudeMetres` read from `Application.settings().getProperty("aisafe.aca.maxAltitudeMetres", "14000")`.

---

### 2.6 Airport Aggregate

**Identity:** `AirportIATA` (`@EmbeddedId`)

```
Airport
├── AirportIATA iata        @EmbeddedId; exactly 3 uppercase letters
├── AirportICAO icao        exactly 4 uppercase letters; unique
├── String name             non-blank, must contain ≥1 letter
├── String city             non-blank, must contain ≥1 letter
├── String country          from CountryList.ALL (25 countries)
├── Coordinates location    latitude + longitude point
├── Elevation elevation     value > 0; unit ∈ {"m","ft"}
└── AreaCode areaCode       cross-ref to AirControlArea
```

**Coordinates** (shared VO, `eapli.aisafe.shared`): `latitude ∈ [-90,90]`, `longitude ∈ [-180,180]`.

**Controller invariant:** Airport `(latitude, longitude)` must fall within the selected ACA's `BoundingBox`. Checked via `aca.containsCoordinates(lat, lon)` before saving.

---

### 2.7 AirTransportCompany Aggregate

**Identity:** `CompanyIATA` (`@EmbeddedId`)

```
AirTransportCompany
├── CompanyIATA iata     @EmbeddedId; exactly 2 uppercase letters; unique
├── CompanyICAO icao     2-3 uppercase letters; unique (@Column(unique=true))
└── String name          non-blank; unique (@Column(unique=true))
```

Three uniqueness constraints are enforced:
1. `CompanyIATA` — primary key.
2. `CompanyICAO` — `@Column(unique = true)`.
3. `name` — `@Column(unique = true)`.

---

### 2.8 Aircraft Aggregate

**Identity:** `RegistrationNumber` (`@EmbeddedId`)

```
Aircraft
├── RegistrationNumber registrationNumber  @EmbeddedId
│   ├── String number                      format [A-Z][A-Z0-9\-]{0,7}
│   └── String registrationCountry
├── AircraftModelCode aircraftModelCode    cross-ref to AircraftModel
├── CompanyIATA companyId                  cross-ref to AirTransportCompany
├── int numberOfFlightCrewMembers          ≥ 1
├── CabinConfiguration cabinConfiguration
├── OperationalStatus operationalStatus    always ACTIVE on creation
└── LocalDate registrationDate             must not be in the future
```

**CabinConfiguration** contains `List<SeatClass>` via `@ElementCollection`. `totalCapacity()` sums all `SeatClass.numberOfSeats`. `SeatClass.numberOfSeats > 0`.

**Controller invariant:** `cabinConfiguration.totalCapacity() ≤ model.maxPassengers()`.

**Domain methods:**
- `decommission()` — sets `operationalStatus = DECOMMISSIONED`; throws if already decommissioned. Irreversible.
- `totalCapacity()` — delegates to `cabinConfiguration.totalCapacity()`.
- `ageInYears()` — `Period.between(registrationDate, LocalDate.now()).getYears()`. Used by US072d.

---

### 2.9 Collaborator Aggregate

**Identity:** `Long id` (auto-generated)

The aggregate uses a **single concrete class** with a `CollaboratorType` enum field — **no class inheritance**. All three role variants (ATC, FCO, WEATHER) live in one `COLLABORATOR` table. Type-specific preconditions are enforced by static factory methods.

```
Collaborator
├── Long id                          @GeneratedValue
├── CollaboratorType collaboratorType  @Enumerated(STRING) — ATC / FCO / WEATHER
├── SystemUser systemUser            @OneToOne(cascade=NONE)
├── String name
├── String position
├── SecurityClearance securityClearance    @Embedded VO
├── SkillsAssessment skillsAssessment      @Embedded VO
├── boolean active                   true on creation
├── String phone                     optional, nullable
├── CompanyIATA companyId            @Embedded; non-null only for ATC type
└── AreaCode areaCode                @Embedded; non-null only for FCO/WEATHER types
```

**Factory methods enforce type-specific preconditions at construction time:**

| Factory method | Type stored | `companyId` | `areaCode` |
|---|---|---|---|
| `Collaborator.ofATC(...)` | `ATC` | required (non-null) | null |
| `Collaborator.ofFlightControlOperator(...)` | `FCO` | null | required (non-null) |
| `Collaborator.ofWeatherPerson(...)` | `WEATHER` | null | required (non-null) |

The private constructor is shared by all three factory methods. `CollaboratorType` is stored as `@Enumerated(EnumType.STRING)` — no `@Inheritance` or `@DiscriminatorColumn` involved.

**SecurityClearance** (VO, `@Embeddable`): `expiryDate` must not be before `LocalDate.now()` on creation. `isValid()` re-checks at runtime.

**SkillsAssessment** (VO, `@Embeddable`): `assessmentDate` must not be in the future. `REGULATORY_PERIOD_YEARS = 5`. `isExpiredByRegulations()` returns true if `assessmentDate` is more than 5 years ago. `nextDueDate()` returns `assessmentDate.plusYears(5)`.

**Domain methods on Collaborator:**
- `disable()` — sets `active = false`; throws if already disabled. Irreversible.
- `updateName(String)`, `updatePosition(String)`, `updatePhone(String)` — mutable fields.
- `renewSecurityClearance(SecurityClearance)` — replaces VO instance.
- `updateSkillsAssessment(SkillsAssessment)` — replaces VO instance.

**SystemUser** lifecycle is managed entirely by the EAPLI framework (`eapli.framework.infrastructure.authz`). The domain never creates or modifies `SystemUser` directly — the controller uses `UserManagementService` from the framework.

---

### 2.10 WeatherData Aggregate

**Identity:** `Long id` (auto-generated)

```
WeatherData
├── Long id                     @GeneratedValue
├── AreaCode areaCode           cross-ref to AirControlArea
├── WindCondition windCondition @Embedded VO
├── double temperatureCelsius   no range restriction (can be negative)
├── String sourceProvider       non-blank (e.g. "IPMA", "METAR LPPC")
└── LocalDateTime recordedDateTime
```

**WindCondition** (`@Embeddable` VO):

| Field | Type | Invariant |
|---|---|---|
| `speedKnots` | double | > 0 |
| `directionDegrees` | int | ∈ [0, 360) |
| `latitude` | double | ∈ [-90, 90] |
| `longitude` | double | ∈ [-180, 180] |
| `altitudeMetres` | int | ≥ 0 |

One `WindCondition` per `WeatherData` record. Multiple records for the same ACA represent different observation points or times.

---

### 2.11 Simulation Aggregate

**Identity:** `Long id` (auto-generated)

```
Simulation
├── Long id                           @GeneratedValue
├── AreaCode areaCode                 cross-ref to AirControlArea
├── SimulationTimeRange timeRange     @Embedded VO
├── SafetyThreshold safetyThreshold  @Embedded VO
├── SimulationReport report           @Embedded VO; nullable until results arrive
└── ValidationResult validationResult  defaults to PENDING on creation
```

**SimulationTimeRange** invariant: `endDateTime` strictly after `startDateTime`.

**SafetyThreshold** invariant: `value > 0`.

**SimulationReport** (`@Embeddable`): `filePath` (non-blank) + `content` (JPA `@Lob` CLOB — raw text of the file returned by the C module). Immutable once set.

**ValidationResult** enum: `PASSED`, `FAILED`, `PENDING` (default). Transitions via `recordValidationResult(ValidationResult)`.

---

## 3. Application Layer — Controllers

### 3.1 Controller Pattern

Every controller follows this structure without exception:

```java
@UseCaseController
public class XxxController {

    private final AuthorizationService authz;
    private final XxxRepository repo;
    // + other repos as needed

    /** Production constructor — uses framework registries */
    public XxxController() {
        this(AuthzRegistry.authorizationService(),
             PersistenceContext.repositories().xxx());
    }

    /** Testing constructor — accepts mocks (package-private) */
    XxxController(AuthorizationService authz, XxxRepository repo, ...) {
        this.authz = authz;
        this.repo = repo;
    }

    public Xxx doSomething(...) {
        // 1. Auth check
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ROLE_A, AISafeRoles.ROLE_B);
        // 2. Cross-aggregate checks (uniqueness, existence)
        // 3. Create VOs + domain object
        // 4. Save via repository
        return repo.save(entity);
    }
}
```

The **package-private testing constructor** accepts mock dependencies — this allows every controller to be unit-tested without a running database or auth system.

### 3.2 Sprint 2 Controllers

| Controller | Role(s) | Key US | Cross-aggregate checks |
|---|---|---|---|
| `RegisterAirControlAreaController` | ADMIN, BACKOFFICE_OPERATOR | US050 | AreaCode unique, AreaName unique, no overlap |
| `CreateAirportController` | ADMIN, BACKOFFICE_OPERATOR | US052 | ACA exists, coordinates inside ACA boundary |
| `CreateAircraftModelController` | ADMIN | US055 | Manufacturer exists, model code unique |
| `CreateEngineModelController` | ADMIN | US056 | Manufacturer exists, name+manufacturer unique |
| `AddEngineVariantController` | ADMIN | US057 | Model exists, engine exists, MotorizationType consistent |
| `RemoveEngineVariantController` | ADMIN | US058 | At least one variant remains |
| `RegisterAirTransportCompanyController` | ADMIN | US060 | IATA unique (PK), ICAO unique, name unique |
| `AddCollaboratorController` | ADMIN, BACKOFFICE_OPERATOR | US061 | SystemUser creation, ATC→company, FCO/WP→ACA |
| `ListCollaboratorsController` | ADMIN, BACKOFFICE_OPERATOR | US062 | — |
| `EditCollaboratorController` | ADMIN, BACKOFFICE_OPERATOR | US063 | Collaborator exists |
| `DisableCollaboratorController` | ADMIN, BACKOFFICE_OPERATOR | US064 | Collaborator exists, not already disabled |
| `AddAircraftController` | ATC_COLLABORATOR | US070 | Model exists, company exists, capacity ≤ maxPassengers |
| `DecommissionAircraftController` | ATC_COLLABORATOR | US071 | Aircraft exists and is ACTIVE |
| `ListCompanyFleetController` | ATC_COLLABORATOR, FLIGHT_CONTROL_OPERATOR | US072 | — |
| `RegisterWeatherDataController` | WEATHER_PERSON | US041 | ACA exists |

---

## 4. Persistence Layer

### 4.1 Repository Factory

`PersistenceContext.repositories()` returns a `RepositoryFactory` instance. In production it returns JPA repositories; in tests it returns in-memory implementations.

Each aggregate has:
- A **repository interface** in `<aggregate>/repositories/` (part of `core`).
- A **JPA implementation** in `persistence/jpa/`.
- An **in-memory implementation** in `persistence/inmemory/`.

### 4.2 JPA Mapping Decisions

| Pattern | Applied to |
|---|---|
| `@EmbeddedId` | `AirControlArea` (AreaCode), `Aircraft` (RegistrationNumber), `AircraftModel` (AircraftModelCode), `Airport` (AirportIATA), `AirTransportCompany` (CompanyIATA), `EngineModel` (EngineModelCode), `Manufacturer` (ManufacturerName) |
| `@Embedded` | All other value objects inside aggregate roots (BoundingBox, WindCondition, etc.) |
| `@OneToMany(cascade=ALL, orphanRemoval=true)` | `AircraftModel.variants` (AircraftVariant list) |
| `@ElementCollection` | `CabinConfiguration.seatClasses` (SeatClass list) |
| `@OneToOne(cascade=NONE)` | `Collaborator.systemUser` (EAPLI manages lifecycle) |
| `@Enumerated(EnumType.STRING)` | `Collaborator.collaboratorType` — stored as `"ATC"` / `"FCO"` / `"WEATHER"` (no JPA inheritance) |
| `@Lob` | `SimulationReport.content` (CLOB for report text) |
| `@Column(unique=true)` | `AirTransportCompany.icao`, `AirTransportCompany.name`, `AirportICAO`, `AreaName` |

### 4.3 Database

- **Engine:** H2 file-based database.
- **File location:** `~/aisafe.mv.db` (user home directory).
- **Schema management:** `hbm2ddl.auto = update` — tables are created/updated automatically on startup.
- **Reset:** Delete `~/aisafe.mv.db` to start fresh.

---

## 5. UI Layer — Console Presentation

### 5.1 Pattern

Each use case has a `*UI` class in `app/src/main/java/eapli/aisafe/ui/<domain>/`. All extend `AbstractUI` from the EAPLI framework and implement `doShow()`.

Standard flow:
1. Instantiate the controller (no-arg production constructor).
2. Collect inputs from the user via `Console.readLine()` / `Console.readDouble()` / `Console.readInteger()`.
3. Validate format locally in a `do/while` loop (e.g., IATA code must match `[A-Z]{3}`).
4. Call the controller method.
5. Print success or catch `IllegalArgumentException` / `IntegrityViolationException` / `ConcurrencyException`.

### 5.2 UI Classes per Sprint 2 US

| UI Class | US |
|---|---|
| `RegisterAirControlAreaUI` | US050 |
| `CreateAirportUI` | US052 |
| `CreateAircraftModelUI` | US055 |
| `CreateEngineModelUI` | US056 |
| `AddEngineVariantUI` | US057 |
| `RemoveEngineVariantUI` | US058 |
| `RegisterAirTransportCompanyUI` | US060 |
| `AddCollaboratorUI` | US061 |
| `ListCollaboratorsUI` | US062 |
| `EditCollaboratorUI` | US063 |
| `DisableCollaboratorUI` | US064 |
| `AddAircraftUI` | US070 |
| `DecommissionAircraftUI` | US071 |
| `ListCompanyFleetUI` | US072 |
| `RegisterWeatherDataUI` | US041 |

### 5.3 Menu Structure

**Backoffice menu (ADMIN / BACKOFFICE_OPERATOR):**
- Air Control Areas → Register ACA
- Airports → Create Airport
- Aircraft Models → Create, Add Variant, Remove Variant
- Engine Models → Create
- Companies → Register
- Collaborators → Add, List, Edit, Disable
- Users → Register, Disable/Enable, List

**Operations menu (ATC_COLLABORATOR / FLIGHT_CONTROL_OPERATOR / WEATHER_PERSON):**
- Aircraft → Add Aircraft, Decommission, List Fleet
- Weather Data → Register

---

## 6. System Roles

| Role constant | Role string | Assigned to |
|---|---|---|
| `AISafeRoles.ADMIN` | `"ADMIN"` | System administrator |
| `AISafeRoles.BACKOFFICE_OPERATOR` | `"BACKOFFICE_OPERATOR"` | Backoffice staff |
| `AISafeRoles.ATC_COLLABORATOR` | `"ATC_COLLABORATOR"` | ATC (Air Transport Company) collaborator |
| `AISafeRoles.FLIGHT_CONTROL_OPERATOR` | `"FCO"` | Flight Control Operator |
| `AISafeRoles.WEATHER_PERSON` | `"WEATHER_PERSON"` | Weather Person |
| `AISafeRoles.POWER_USER` | `"POWER_USER"` | Bootstrap only — not assigned to any collaborator |

> **Note on ATC_COLLABORATOR naming:** The abbreviation ATC in this role stands for *Air Transport Company*, not *Air Traffic Control*. This matches the user story wording ("As an Air Transport Company Collaborator…"). The role was kept as-is to avoid breaking changes.

Roles are assigned by ADMIN when creating a user/collaborator. The EAPLI framework enforces that only users with matching roles can access each use case (enforced in every controller via `authz.ensureAuthenticatedUserHasAnyOf(...)`).

---

## 7. Bootstrap

The application has two bootstrap modes. The main bootstrap seeds system users; the demo bootstrap additionally seeds all reference domain data.

### 7.1 Main Bootstrap — `AISafeBootstrapper`

Run with: `bootstrap` (default)

Creates the `poweruser` system user (password: `poweruserA1`) directly in the persistence layer, then runs `AISafeMasterUsersBootstrapper`.

**`AISafeMasterUsersBootstrapper`** creates these SystemUsers (all with password `Password1`):

| Username | Roles | Purpose |
|---|---|---|
| `demo` | ALL roles | Full-access superuser for demos |
| `admin1` | ADMIN | User management and configuration |
| `backoffice1` | BACKOFFICE_OPERATOR | Backoffice configuration |
| `atc1` | ATC_COLLABORATOR | Aircraft / fleet management |
| `fco1` | FLIGHT_CONTROL_OPERATOR | Flight simulation / monitoring |
| `weather1` | WEATHER_PERSON | Weather data registration |

All `registerUser` calls are idempotent — if the user already exists, the call is skipped silently.

### 7.2 Demo Bootstrap — `AISafeDemoDataBootstrapper`

Run with: `bootstrap -bootstrap:demo`

Seeds all reference domain data needed to demonstrate the full application. The bootstrapper is also idempotent — each entity is checked by identity before creation.

#### Manufacturers (8)

| Name | Country |
|---|---|
| Boeing | United States |
| Airbus | France |
| Embraer | Brazil |
| GE Aviation | United States |
| Rolls-Royce | United Kingdom |
| CFM International | United States/France |
| Pratt & Whitney | United States |
| Engine Alliance | United States |

#### Engine Models (6)

| Code | Name | Manufacturer | Type |
|---|---|---|---|
| `GE90B` | GE90-94B | GE Aviation | TURBOFAN |
| `TRENT970` | Trent 970 | Rolls-Royce | TURBOFAN |
| `CFM565B4` | CFM56-5B4 | CFM International | TURBOFAN |
| `LEAP1B` | CFM LEAP-1B | CFM International | TURBOFAN |
| `GE90115B` | GE90-115B | GE Aviation | TURBOFAN |
| `TRENTXWB84` | Trent XWB-84 | Rolls-Royce | TURBOFAN |

#### Aircraft Models (5 models, each with one engine variant)

| Code | Name | Manufacturer | Max pax | Engine variant |
|---|---|---|---|---|
| `B77W` | Boeing 777-200ER | Boeing | 396 | GE90B (TURBOFAN) |
| `A388` | Airbus A380-800 | Airbus | 555 | TRENT970 (TURBOFAN) |
| `B738` | Boeing 737-800 | Boeing | 189 | LEAP1B (TURBOFAN) |
| `A320` | Airbus A320-200 | Airbus | 180 | CFM565B4 (TURBOFAN) |
| `B773ER` | Boeing 777-300ER | Boeing | 396 | GE90115B (TURBOFAN) |

#### Air Control Areas (23)

Real-world FIR/ARTCC boundaries (slightly simplified bounding boxes). Sample entries:

| Code | Name | Region |
|---|---|---|
| `LPPC` | Lisboa FIR | Portugal / Atlantic |
| `LECM` | Madrid FIR | Spain |
| `WEFIR` | West Europe FIR | Central Europe |
| `EGTT` | London FIR | UK/Ireland |
| `KZNE` | US Northeast ARTCC | USA East |
| `KZLA` | US West Coast ARTCC | USA West |
| `RJTT` | Tokyo FIR | Japan |
| `ZBPE` | Beijing FIR | China |
| … | … (23 total) | … |

All ACAs are seeded with `maxAltitudeMetres = 13 700`.

#### Airports (50)

50 airports across Europe, North America, South America, Asia, Africa, and Oceania. Each is assigned to one of the 23 ACAs by bounding box containment. Samples:

| IATA | Name | City | ACA |
|---|---|---|---|
| OPO | Francisco de Sá Carneiro | Porto | LPPC |
| LIS | Humberto Delgado | Lisbon | LPPC |
| LHR | Heathrow | London | EGTT |
| JFK | John F. Kennedy Intl | New York | KZNE |
| HND | Tokyo Haneda | Tokyo | RJTT |
| SYD | Sydney Kingsford Smith | Sydney | YBBB |

> Note: Amsterdam (AMS) has real elevation −11 m; the spec rejects non-positive values (US052 AT), so 1 m is used with a comment in the bootstrapper.

#### Air Transport Companies (10)

| IATA | ICAO | Name |
|---|---|---|
| TP | TAP | TAP Air Portugal |
| FR | RYR | Ryanair |
| BA | BAW | British Airways |
| LH | DLH | Lufthansa |
| AF | AFR | Air France |
| IB | IBE | Iberia |
| AA | AAL | American Airlines |
| DL | DAL | Delta Air Lines |
| UA | UAL | United Airlines |
| KL | KLM | KLM Royal Dutch Airlines |

#### Demo Collaborators (6)

| Username | Password | CollaboratorType | Name | Company / ACA |
|---|---|---|---|---|
| `atc1` | Password1 | ATC | Carlos Ferreira | TAP (TP) |
| `atc2` | Password1 | ATC | Hans Mueller | Lufthansa (LH) |
| `fco1` | Password1 | FCO | Ana Santos | Lisboa FIR (LPPC) |
| `fco2` | Password1 | FCO | John Smith | US Northeast (KZNE) |
| `met1` | Password1 | WEATHER | Maria Costa | Lisboa FIR (LPPC) |
| `met2` | Password1 | WEATHER | Klaus Weber | West Europe FIR (WEFIR) |

All demo collaborators are created with `SecurityClearance` expiring 5 years from now and `SkillsAssessment` dated 6 months ago.

#### Demo Aircraft (8)

| Registration | Country | Model | Company | Cabin | Registration date |
|---|---|---|---|---|---|
| CS-TUI | Portugal | A320 | TAP | 12 Business + 162 Economy = **174** | 2018-03-15 |
| CS-TNL | Portugal | B77W | TAP | 8 First + 48 Business + 276 Economy = **332** | 2015-06-10 |
| EI-RKI | Ireland | B738 | Ryanair | 189 Economy = **189** | 2019-09-01 |
| EI-FDA | Ireland | B738 | Ryanair | 189 Economy = **189** | 2012-04-20 |
| G-BHNA | United Kingdom | A388 | British Airways | 14 First + 56 Business + 303 Economy = **373** | 2014-07-22 |
| G-STBD | United Kingdom | B773ER | British Airways | 48 Business + 228 Economy = **276** | 2010-11-05 |
| D-AIPB | Germany | A320 | Lufthansa | 20 Business + 138 Economy = **158** | 2020-02-14 |
| D-AIMA | Germany | A388 | Lufthansa | 8 First + 76 Business + 364 Economy = **448** | 2011-05-30 |

All demo aircraft are seeded with `numberOfFlightCrewMembers = 2` and `OperationalStatus = ACTIVE`.

---

## 8. Testing

### 8.1 Approach

All Sprint 2 use case controllers are covered by unit tests using **JUnit 5** and **Mockito**. Tests use the package-private testing constructor of each controller to inject mocks — no running database or auth system is needed.

**Sprint 2 totals: 33 test classes, 352 tests, 0 failures.**

### 8.2 Controller Test Pattern

```java
class RegisterAirControlAreaControllerTest {

    private AuthorizationService authz;
    private AirControlAreaRepository repo;
    private RegisterAirControlAreaController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AirControlAreaRepository.class);
        // Package-private testing constructor — injects mocks
        controller = new RegisterAirControlAreaController(authz, repo);
    }

    @Test
    void ensureRegisterAirControlAreaSavesArea() {
        // Arrange
        final AirControlArea expected = new AirControlArea(
                AreaCode.valueOf("LPPC"), new AreaName("Lisboa Oceânico"),
                36.0, 44.0, -25.0, -6.0, 14000);
        when(repo.save(any(AirControlArea.class))).thenReturn(expected);

        // Act
        final AirControlArea result = controller.registerAirControlArea(
                "LPPC", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, 14000);

        // Assert
        verify(repo).save(any(AirControlArea.class));
        assertNotNull(result);
    }

    @Test
    void ensureRegisterAirControlAreaChecksAuthorization() {
        when(repo.save(any())).thenReturn(mock(AirControlArea.class));
        controller.registerAirControlArea(
                "LPPC", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, 14000);
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureRegisterAirControlAreaWithBlankCodeThrows() {
        assertThrows(Exception.class,
                () -> controller.registerAirControlArea(
                        "", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, 14000));
    }
}
```

### 8.3 Collaborator Controller Test Example

`AddCollaboratorController` takes 6 constructor arguments (auth + 5 repos). The test injects all mocks:

```java
class AddCollaboratorControllerTest {

    private AddCollaboratorController controller;

    @BeforeEach
    void setUp() {
        authz             = mock(AuthorizationService.class);
        userSvc           = mock(UserManagementService.class);
        collaboratorRepo  = mock(CollaboratorRepository.class);
        companyRepo       = mock(AirTransportCompanyRepository.class);
        acaRepo           = mock(AirControlAreaRepository.class);
        profileRepo       = mock(UserSecurityProfileRepository.class);
        controller = new AddCollaboratorController(
                authz, userSvc, collaboratorRepo, companyRepo, acaRepo, profileRepo);
        when(profileRepo.save(any())).thenReturn(mock(UserSecurityProfile.class));
    }

    @Test
    void ensureAddATCCollaboratorSavesCollaborator() {
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(),
                anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        when(collaboratorRepo.save(any())).thenReturn(mock(Collaborator.class));

        final Collaborator result = controller.addATCCollaborator(
                "jdoe", "Pass1234!", "Jane", "Doe", "jdoe@aisafe.pt",
                "Jane Doe", "ATC Officer",
                LocalDate.now().plusYears(1),
                LocalDate.now().minusDays(1),
                "TP");                      // company IATA

        verify(collaboratorRepo).save(any(Collaborator.class));
        assertNotNull(result);
    }

    @Test
    void ensureAddATCCollaboratorCreatesSecurityProfile() {
        // AC 031.7: UserSecurityProfile must be persisted alongside the SystemUser
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(),
                anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        when(collaboratorRepo.save(any())).thenReturn(mock(Collaborator.class));

        controller.addATCCollaborator("jdoe", "Pass1234!", "Jane", "Doe",
                "jdoe@aisafe.pt", "Jane Doe", "ATC Officer",
                LocalDate.now().plusYears(1), LocalDate.now().minusDays(1), "TP");

        verify(profileRepo).save(any(UserSecurityProfile.class));
    }
}
```

### 8.4 What is Tested

For every controller:

| Test category | What is verified |
|---|---|
| **Happy path** | Valid inputs produce a saved entity; `verify(repo).save(...)` passes |
| **Authorization** | `verify(authz).ensureAuthenticatedUserHasAnyOf(...)` is always called |
| **Domain invariants** | Invalid VO inputs (e.g., BoundingBox `minLat ≥ maxLat`) throw `IllegalArgumentException` |
| **Cross-aggregate checks** | Duplicate code/name causes rejection before reaching the repository |
| **Support queries** | `allXxx()` methods delegate to repo; `verify(repo).findAll()` passes |

Domain VOs are also tested independently:
- `BoundingBoxTest`, `AirControlAreaTest`, `AircraftModelTest`, `EngineModelTest`
- `AircraftTest` (includes `ageInYears()`, `totalCapacity()`, `decommission()`)
- `CollaboratorTest`, `WeatherDataTest`, `SimulationTest`

---

## 9. Cross-Aggregate Reference Pattern

All cross-aggregate references are by **identity value object only** — never by object reference. This is the fundamental DDD rule that keeps aggregates independently loadable.

| From | Refers to | Via |
|---|---|---|
| `Airport` | `AirControlArea` | `AreaCode` |
| `Aircraft` | `AircraftModel` | `AircraftModelCode` |
| `Aircraft` | `AirTransportCompany` | `CompanyIATA` |
| `AircraftVariant` | `EngineModel` | `EngineModelCode` |
| `AircraftModel` | `Manufacturer` | `ManufacturerName` (String-wrapped VO) |
| `EngineModel` | `Manufacturer` | `String manufacturerName` |
| `Collaborator` (ATC variant) | `AirTransportCompany` | `CompanyIATA` |
| `Collaborator` (FCO/WEATHER variants) | `AirControlArea` | `AreaCode` |
| `WeatherData` | `AirControlArea` | `AreaCode` |
| `Simulation` | `AirControlArea` | `AreaCode` |

When a controller needs data from both sides (e.g., display aircraft with model name), it queries both repositories separately and combines in the application layer — never via a JPA join.
