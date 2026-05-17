# US010 — Domain Model Revision (Sprint 2)

## 1. Context

US010 (Domain Model) was first elaborated in Sprint 1 as a living artefact meant to evolve with each sprint. This document records every change made to the domain model during Sprint 2 implementation, the reasoning behind each decision, and the impact on the glossary.

The Sprint 1 domain model is preserved intact in `docs/Sprint1/us_010/`. This document and the accompanying `domain_model_sprint2.puml` represent the **authoritative Sprint 2 state** of the model. Any future sprint revision must follow the same pattern: leave prior sprint docs unchanged and produce a new revision document.

**Sprint 2 EAPLI scope:** US030–US033, US041, US050, US052, US055–US058, US060–US064, US070–US072.

---

## 2. Methodology

Every change listed below was driven by one of three sources:

1. **Client clarification** — official answers from the product owner that add or restrict requirements.
2. **Implementation constraint** — a design decision that was conceptually valid in Sprint 1 but proved impractical or incorrect when confronted with the EAPLI/JPA framework.
3. **Requirements gap** — a field or concept missing from the Sprint 1 model that a Sprint 2 use case explicitly requires.

Each change is tagged with its source and the use case(s) that motivated it.

---

## 3. Changes from Sprint 1 Design to Sprint 2 Implementation

### 3.1 AirControlArea Aggregate

#### Change 1 — `Coordinates_ACA` renamed to `BoundingBox`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US050, US052 |

The Sprint 1 model named the rectangular boundary VO `Coordinates_ACA`. During implementation the team found this name misleading: a coordinate suggests a single geographic point, but this VO holds a rectangle (min/max lat and lon). The team renamed it `BoundingBox` to express the concept accurately.

**Domain methods added to BoundingBox:**
- `containsCoordinates(double lat, double lon)` — used by `CreateAirportController` (US052) to validate that airport coordinates fall within the ACA boundary.
- `overlapsWith(BoundingBox other)` — used by `RegisterAirControlAreaController` (US050) to enforce the no-overlap invariant across ACAs.

These are domain behaviours that belong on the VO itself (Information Expert principle).

#### Change 2 — `AreaName` VO added to AirControlArea

| Attribute | Value |
|-----------|-------|
| Source | Client clarification |
| Motivation | US050.3 |

The Sprint 1 model did not include an operational name for an ACA. During Sprint 2 the client clarified:

> *"Each area must have the name it's usually used in the air control business. It would be a mess if this name wasn't unique."*

`AreaName` was added as a Value Object with a uniqueness invariant enforced at the controller level (cross-aggregate). It validates that the value contains at least one letter and is non-blank.

#### Change 3 — `maxAltitudeMetres` added to AirControlArea

| Attribute | Value |
|-----------|-------|
| Source | Client clarification (official simplification) |
| Motivation | US050.7 |

The Sprint 1 model had no vertical boundary for an ACA. The client confirmed:

> *"The ACA extends from altitude zero to a configurable maximum altitude. Default = 14 000 m."*

The value **must not be hardcoded** — it is read from `Application.settings()` at ACA creation time and stored on the aggregate root. Hardcoding 14 000 m in source code would violate AC 050.7.

---

### 3.2 AircraftModel Aggregate

#### Change 4 — `ModelID` renamed to `AircraftModelCode`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US055 |

The Sprint 1 identity VO was named `ModelID`. During implementation it was renamed `AircraftModelCode` to align with the ICAO aircraft type designator convention (e.g., "B738", "A320") and to be unambiguous when read alongside `EngineModelCode` and `CompanyIATA`.

#### Change 5 — `AircraftVariant` reclassified from Value Object to internal Entity

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US057, US058 |

The Sprint 1 diagram (full model PUML) showed `AircraftVariant` as `<<value object>>`. The glossary and justification document correctly called it an internal entity, but the diagram was inconsistent.

The implementation confirms it is a **local entity with an auto-generated Long ID**: variants are added via US057 and removed via US058 independently of each other. A value object cannot have this add/remove lifecycle — it would need to be replaced whole. The JPA mapping uses `@OneToMany(cascade = ALL, orphanRemoval = true)`.

**Invariant enforced by AircraftModel (aggregate root):**
- All `AircraftVariant` entries within the same `AircraftModel` must share the same `MotorizationType`. Enforced in `addVariant()`.
- At least one variant must remain after `removeVariant()`. Enforced in `removeVariant()`.

#### Change 6 — `maxPassengers` added to AircraftModel

| Attribute | Value |
|-----------|-------|
| Source | Requirements gap |
| Motivation | US070 AC: cabin configuration cannot exceed model capacity |

The Sprint 1 model did not represent maximum passenger capacity. US070 requires: *"total seats cannot exceed the model's maximum capacity."* The `maxPassengers` field was added to `AircraftModel` and validated by `AddAircraftController` before accepting a `CabinConfiguration`.

---

### 3.3 EngineModel Aggregate

#### Change 7 — `EngineModelCode` added as primary identity VO

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US056, US057 |

The Sprint 1 model used `EngineName` as the identity. During implementation this was problematic: `EngineName` is not globally unique — only the combination `(name, manufacturer)` is unique. A separate `EngineModelCode` was introduced as a surrogate business key (unique code per engine model). `EngineName` remains a VO for the human-readable name.

#### Change 8 — Two `Thrust` instances modelled as named fields, not a collection

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US056, section 3.3 |

The Sprint 1 model showed `EngineModel *-- "2" Thrust : characterisedBy`. In JPA, a fixed-size collection of two embedded VOs cannot be mapped cleanly without an `@ElementCollection` (which adds complexity). The team instead created two named fields: `staticThrust` and `cruiseThrust`, both of type `Thrust`. This is semantically equivalent (there are always exactly two, they have different meanings) and far simpler to map.

---

### 3.4 Airport Aggregate

#### Change 9 — `town` field renamed to `city`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US052 |

Minor rename for clarity and consistency with standard aviation terminology. `city` is the commonly used term (e.g., ICAO database field name).

#### Change 10 — Shared `Coordinates` VO introduced

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US052, US041 |

The Sprint 1 model showed `Coordinates_Apt` and `Coordinates_WD` as separate diagram classes with a note saying *"same class in code."* In the Sprint 2 implementation, this is made explicit: a single `Coordinates` VO class in the `shared` package holds `latitude` (in [-90, 90]) and `longitude` (in [-180, 180]) with their invariants. `Airport` uses it for its `location` field.

Note: `WindCondition` (WeatherData aggregate) does **not** use the shared `Coordinates` VO — see Change 12 below.

#### Change 11 — `Elevation` invariant clarified: strictly positive

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US052 |

The Sprint 1 glossary stated "must be non-negative." The implementation enforces strictly positive (`value > 0`). This was a team decision driven by the JPA mapping simplicity and the understanding that an airport at exactly sea level (0 m) is represented as "at sea level" which is implicitly positive in aviation databases.

> **Note:** The UI prompt says "airports can be below sea level" — if below-sea-level airports need to be supported in a future sprint, this invariant must be relaxed to allow negative values.

---

### 3.5 AirTransportCompany Aggregate

#### Change 12 — `IATACode_ATC` / `ICAOCode_ATC` renamed to `CompanyIATA` / `CompanyICAO`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US060, US070, US072 |

The Sprint 1 model used `IATACode_ATC` and `ICAOCode_ATC` as diagram class names (the `_ATC` suffix was a workaround to avoid PlantUML naming conflicts with Airport's `IATACode`). The implementation uses distinct Java classes `CompanyIATA` and `CompanyICAO` to make cross-aggregate references unambiguous in code.

---

### 3.6 WeatherData Aggregate

#### Change 13 — `WeatherData *-- "1..*" WindCondition` reduced to single `WindCondition`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint + team decision |
| Motivation | US041 |

The Sprint 1 model allowed multiple `WindCondition` instances per `WeatherData` record (client: *"Weather conditions are not the same everywhere inside an air control area"*). During implementation the team decided that **each weather data record represents a single observation at a single point in space and time**. If multiple points are needed, multiple `WeatherData` records are registered. This simplified the JPA mapping (no `@ElementCollection`) and the UI (one form = one observation).

The client requirement about spatial variation is still met: multiple `WeatherData` records can be registered for the same ACA, each with different coordinates.

#### Change 14 — `Coordinates_WD` VO removed; fields embedded directly in `WindCondition`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US041 |

The Sprint 1 model had a separate `Coordinates_WD` VO nested inside `WindCondition`. JPA `@Embeddable` classes cannot nest other `@Embeddable` classes in all JPA providers without explicit column name overrides (which increase complexity). The team embedded the coordinate fields (`latitude`, `longitude`) directly in `WindCondition`, alongside `altitudeMetres`. This has identical semantics without the nesting complexity.

#### Change 15 — `altitudeMetres` added to `WindCondition`

| Attribute | Value |
|-----------|-------|
| Source | Requirements gap |
| Motivation | US041 AC 041.3 |

The Sprint 1 model had no altitude for wind observations. US041 acceptance criterion 041.3 explicitly requires `altitudeMetres ≥ 0`. Wind behaviour is altitude-dependent (section 3.3), so the observation altitude is necessary for simulation input.

#### Change 16 — `temperatureCelsius` added to `WeatherData`

| Attribute | Value |
|-----------|-------|
| Source | Requirements gap |
| Motivation | US041 AC 041.5 |

Temperature was not present in the Sprint 1 model. AC 041.5 requires it: *"temperatureCelsius must be provided (no range restriction — can be negative)."* It was added as a plain `double` field on the aggregate root.

---

### 3.7 Simulation Aggregate

#### Change 17 — `SimulationReport` changed from `{totalFlights}` to `{filePath, content}`

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US109, US111 |

The Sprint 1 model modelled `SimulationReport` with a `totalFlights` count. During implementation the team realised the simulation module (C component) returns a **raw file** (not a structured count). The report is stored as a file path and its full text content (a JPA `@Lob` / CLOB column). The number of flights can be derived from the content when needed. The VO remains immutable — once set, it cannot be changed.

#### Change 18 — `ValidationResult` enum: `PENDING` state added

| Attribute | Value |
|-----------|-------|
| Source | Implementation constraint |
| Motivation | US100, US109 |

The Sprint 1 model had only `passed` and `failed`. A simulation is created before results arrive from the C module. The `PENDING` state captures this intermediate condition. `Simulation.validationResult` defaults to `PENDING` on creation and transitions to `PASSED` or `FAILED` via `recordValidationResult()`.

---

### 3.8 Aircraft Aggregate

#### Change 19 — `registrationDate` added to `Aircraft`

| Attribute | Value |
|-----------|-------|
| Source | Requirements gap |
| Motivation | US072d |

The Sprint 1 model did not record the date an aircraft was registered. US072d requires filtering the fleet by aircraft age, computed as `Period.between(registrationDate, LocalDate.now()).getYears()`. The invariant is: `registrationDate` must not be in the future. Initial operational status is always `ACTIVE`.

---

### 3.9 Collaborator Aggregate

#### Change 20 — `SecurityClearance` and `SkillsAssessment` reclassified as Value Objects

| Attribute | Value |
|-----------|-------|
| Source | Implementation decision (correcting Sprint 1 diagram) |
| Motivation | US061–US064 |

The Sprint 1 full-model PUML incorrectly showed `SecurityClearance` and `SkillsAssessment` as entities (with assessmentScore as a structural field). The glossary and justification document correctly identified them as **Value Objects** — they are immutable once created; a "renewal" replaces the whole VO. The implementation follows the glossary. When a clearance is renewed, `Collaborator.renewSecurityClearance(LocalDate newExpiry)` replaces the VO instance.

**Domain rules:**
- `SecurityClearance.expiryDate` must not be before today (creation-time check).
- `SkillsAssessment.assessmentDate` must not be in the future.
- `SkillsAssessment.REGULATORY_PERIOD_YEARS = 5` (section 3.1.1).

#### Change 21 — `phone` field added directly to `Collaborator`

| Attribute | Value |
|-----------|-------|
| Source | Requirements gap |
| Motivation | US061, section 3.1.1 |

Section 3.1.1 states: *"A user also has a name and phone number."* The phone number is stored as a plain optional String on `Collaborator` and can be updated via `updatePhone()`.

---

### 3.10 Not Yet Implemented (Sprint 3 Scope)

The following aggregates were **designed in Sprint 1** and remain in the domain model, but have not been implemented in Sprint 2:

| Aggregate | Planned Sprint | Design reference |
|-----------|----------------|------------------|
| `FlightRoute` | Sprint 3 | US073–US074 |
| `Flight` + `FlightPlan` | Sprint 3 | US080–US085 |
| `Pilot` | Sprint 3 | US075–US077 |

These are shown in the Sprint 2 domain model diagram with a *"Sprint 3 — Not yet implemented"* note to make the implementation boundary clear.

---

## 4. Updated Glossary Entries

The following entries are **new or changed** relative to the Sprint 1 glossary (`docs/Sprint1/us_010/glossary.md`). All other entries remain valid.

| **Term** | **Type** | **Sprint 2 Change** |
|:---|:---|:---|
| **AreaName** | Value Object | **NEW.** The unique operational name of an AirControlArea. Client clarification: "it would be a mess if this name wasn't unique." Validates non-blank and contains at least one letter. Uniqueness enforced by controller. US050.3. |
| **BoundingBox** | Value Object | **RENAMED** from `Coordinates_ACA`. Rectangular geographic boundary of an AirControlArea. Added domain methods: `containsCoordinates()` and `overlapsWith()`. Invariants unchanged: minLat < maxLat, minLon < maxLon, all values within valid geographic ranges. US050.4–5. |
| **AircraftModelCode** | Value Object | **RENAMED** from `ModelID`. The ICAO designator-style identity of an AircraftModel (e.g., "B738", "A320"). Non-empty string. Uniqueness within system enforced by controller. US055. |
| **AircraftVariant** | Entity (internal) | **RECLASSIFIED** (diagram was inconsistent with glossary). Confirmed as internal entity with auto-generated Long ID. Added/removed independently via US057/US058. All variants within one AircraftModel must share the same MotorizationType. |
| **CompanyIATA** | Value Object | **RENAMED** from `IATACode_ATC`. Exactly 2 uppercase letters. Primary identity of AirTransportCompany. US060. |
| **CompanyICAO** | Value Object | **RENAMED** from `ICAOCode_ATC`. 2–3 uppercase letters. Unique. US060. |
| **CompanyName** | Value Object | **UNCHANGED.** Noted here: uniqueness enforced at DB level via `@Column(unique = true)`. |
| **Coordinates** | Value Object | **RENAMED/CLARIFIED** from `Coordinates_Apt`. Now a shared VO in the `shared` package used by Airport's location field. Invariants: latitude ∈ [-90, 90], longitude ∈ [-180, 180]. |
| **EngineModelCode** | Value Object | **NEW.** Unique surrogate identity of an EngineModel. Separate from `EngineName` — the name is only unique when combined with the manufacturer. |
| **SimulationReport** | Value Object | **CHANGED.** Fields changed from `totalFlights` to `filePath` (non-blank String) + `content` (CLOB text). The report is the raw file received from the C simulation module. Immutable. US109, US111. |
| **ValidationResult** | Enum | **CHANGED.** Added `PENDING` state (initial state before C module returns results). Was: `passed`, `failed`. Now: `PASSED`, `FAILED`, `PENDING`. |
| **WindCondition** | Value Object | **CHANGED.** Removed nested `Coordinates_WD` VO. Fields `latitude` and `longitude` are now direct attributes. Added `altitudeMetres` (≥ 0). `directionDegrees` renamed from `directionAngle`. Speed unit changed from m/s to knots (`speedKnots`). Invariants: speedKnots > 0, directionDegrees ∈ [0, 360), latitude ∈ [-90, 90], longitude ∈ [-180, 180], altitudeMetres ≥ 0. US041. |
| **WeatherData** | Entity (root) | **CHANGED.** Now holds a single `WindCondition` (was `1..*`). Added `temperatureCelsius` (double, no range restriction — can be negative). `sourceProvider` field identifies the observation source (e.g. "IPMA", "METAR LPPC"). US041. |
| **RegistrationDate** | Plain attribute on Aircraft | **NEW.** `registrationDate` (LocalDate) records when the aircraft was registered. Invariant: must not be in the future. Used by US072d to compute `ageInYears()`. |
| **SecurityClearance** | Value Object | **CORRECTED** (Sprint 1 full-model diagram showed it as entity). Confirmed as immutable VO. `expiryDate` must not be before today on creation. Renewal replaces the VO instance. |
| **SkillsAssessment** | Value Object | **CORRECTED** (Sprint 1 full-model diagram showed it as entity). Confirmed as immutable VO. `assessmentDate` must not be in the future. Regulatory period: 5 years (section 3.1.1). |

---

## 5. Domain Model Diagram

The updated PlantUML diagram is at [`domain_model_sprint2.puml`](domain_model_sprint2.puml). It supersedes the Sprint 1 full model for Sprint 2.

**Key conventions in the Sprint 2 diagram:**
- Aggregates not yet implemented (FlightRoute, Flight, Pilot) are marked with a *"Sprint 3"* note.
- Cross-aggregate references are shown as directed associations labelled with the identity VO used (e.g., `AreaCode`, `CompanyIATA`).
- `AircraftVariant` is correctly labelled `<<entity>>` (not `<<value object>>`).
- `BoundingBox` replaces `Coordinates_ACA` everywhere.
- `Coordinates` appears as a shared VO referenced by `Airport`.

---

## 6. Implementation Architecture Notes

### 6.1 EAPLI Framework Integration

All aggregate roots implement `AggregateRoot<ID>` from the EAPLI framework. All value objects implement `ValueObject`. The framework provides:
- `Preconditions.noneNull(...)` — null checks in constructors.
- `Invariants.ensure(condition, message)` — business rule enforcement in constructors.

The pattern is consistent across all aggregates: VO constructors validate their own invariants (Information Expert), and controllers perform cross-aggregate checks before delegating to the domain.

### 6.2 JPA Mapping Decisions

| Pattern | Usage |
|---------|-------|
| `@EmbeddedId` | AirportIATA, CompanyIATA (primary keys that are VOs) |
| `@Embedded` | BoundingBox, WindCondition, AircraftWeights, etc. (VOs as columns) |
| `@OneToMany(cascade = ALL, orphanRemoval = true)` | AircraftVariant list within AircraftModel |
| `@ElementCollection` | SeatClass list within CabinConfiguration |
| `@OneToOne(cascade = NONE)` | SystemUser reference in Collaborator (lifecycle managed by framework) |
| `@Enumerated(EnumType.STRING)` | `Collaborator.collaboratorType` — stored as `"ATC"` / `"FCO"` / `"WEATHER"` (single table, no JPA inheritance) |
| `@Lob` | SimulationReport.content (CLOB for large text) |

### 6.3 Controller Pattern (All Sprint 2 USs)

Every controller follows the same structure:
```
1. authz.ensureAuthenticatedUserHasAnyOf(ROLE)
2. [Cross-aggregate uniqueness / existence checks via repositories]
3. Create domain objects (VOs first, then aggregate root)
4. repository.save(root)
5. return saved root
```

Each controller has two constructors: a **production constructor** (uses `AuthzRegistry` and `PersistenceContext`) and a **package-private testing constructor** (accepts mock dependencies) — enabling unit testing without a running database.

---

## 7. Observations

The Sprint 2 implementation confirmed the core DDD design from Sprint 1 was sound. The changes were minor refinements, not structural redesigns. The most impactful decision was simplifying `WeatherData *-- "1..*" WindCondition` to a single VO — this trades expressive power (one record = many readings) for implementation simplicity (no collection table, simpler UI). The business requirement (spatial variation of weather) remains achievable by registering multiple records.

The `AircraftVariant` entity/VO confusion in the Sprint 1 diagram is now resolved definitively: it is an internal entity. The glossary was always correct on this point; the diagram was the source of the inconsistency.

All 352 tests pass (0 failures) at Sprint 2 completion.
