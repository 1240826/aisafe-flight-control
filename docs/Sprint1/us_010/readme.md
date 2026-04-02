# US010 — Domain Model

## 1. Context

This task was assigned in Sprint 1 (Sprint A). It is the first time this task is being developed. The objective is to elaborate a Domain Model using Domain-Driven Design (DDD) that captures all the business concepts, rules, and relationships of the AISafe flight control system. The domain model serves as the foundation for all subsequent implementation sprints and must be kept as a living artefact throughout the project.

### 1.1 List of Issues

- Analysis: #19 (Domain Model), #12 (Glossary)
- Design: #19 (Domain Model)
- Implement: N/A — this is a design artefact
- Test: Validated against requirements, use cases, and client clarifications

---

## 2. Requirements

**US010** As Project Manager, I want the team to elaborate a Domain Model using DDD. Because AISafe domain is complex, involving distinct areas and complex business rules. We will adopt DDD as a framework for tackling complexity in our software solution, and ensure the software is a valid implementation of the business needs.

**Acceptance Criteria:**

- US010.1 All relevant business concepts from the requirements document and use cases must be identified and classified as entity or value object.
- US010.2 All associations must have navigability and cardinality indicated. No bidirectional associations are permitted.
- US010.3 Aggregates must be identified and bounded by business invariants. Cross-aggregate references must point only to aggregate roots.
- US010.4 All concepts in the diagram must have a corresponding entry in the glossary, and vice versa.
- US010.5 The domain model must be validated against all use cases — it must be possible to navigate the model to answer every use case.
- US010.6 Value objects must not hold references to entities outside their aggregate boundary.
- US010.7 No concepts may be added beyond what is explicitly stated in the requirements document or client clarifications.

**Dependencies/References:**

- US001 — Technical constraints (DDD methodology, PlantUML for diagrams)
- US002 — Project repository (diagrams stored in docs/ folder)
- US011 — Aggregate justification depends on this domain model
- All functional USs (US030–US114) — the domain model must support navigation of all these use cases

---

## 3. Analysis

### 3.1 Methodology

The domain model was elaborated following the process described in the *Processo de Engenharia de Aplicações* document (Paulo Gandra de Sousa, ISEP):

1. **First**, all business concepts and relationships were identified from the requirements document and use cases.
2. **Then**, each concept was classified as entity or value object.
3. **Only after** both steps were complete were the aggregates identified and bounded.

The model reflects the **EAPLI Java application scope only**. The following are explicitly outside scope: SystemUser internals (EAPLI framework), C simulation execution (SCOMP), DSL grammar and parser (LPROG), network protocols (RCOMP).

### 3.2 Theoretical Foundation

#### 3.2.1 Entity vs Value Object

An **Entity** is a domain concept with its own business identity and an independent lifecycle. It is tracked by its identity, not its attributes — two entities with the same attributes are still distinct objects.

A **Value Object** is a domain concept that characterises or describes another concept. It has no identity of its own — two VOs with the same attributes are considered equal. VOs are immutable and follow the **Information Expert** principle: they encapsulate and validate their own data. A concept should be a VO (not a plain attribute) when it has a business rule (format, uniqueness, validation, invariant) or when a plain string/number is not sufficient to represent it.

A Value Object can have multiple attributes when they form a cohesive concept. A Value Object can also be an enumeration when it represents a fixed set of domain values.

In practical terms in Java: Entity → class with identity and a Repository; Value Object → immutable class with no ID; Value Object enum → Java `enum`.

#### 3.2.2 Aggregate

An aggregate is a cluster of entities and value objects that must be manipulated together to enforce business invariants, with a single root entity that controls all access. The rules applied:

- Nothing outside the aggregate boundary can hold a reference to anything inside — only roots are referenced externally, by ID.
- Only aggregate roots can be obtained directly with database queries (via Repositories as interfaces).
- A delete operation removes everything within the aggregate boundary at once.
- When any change within the aggregate is committed, all invariants of the whole aggregate must be satisfied.
- One use case should only update one aggregate — ACID within the aggregate, BASE between aggregates.

#### 3.2.3 Composition vs Association

- **Composition (`*--`)**: the part cannot exist without the whole. Used for all constitutive VOs and internal entities.
- **Association (`-->`)**: used for enumerations (referenced values, not owned) and for SystemUser (lifecycle managed by the EAPLI framework).
- All associations are unidirectional — no bidirectional associations.

#### 3.2.4 Enum vs Hierarchy of Value Objects

When a concept has multiple cases with **different data structures**, a hierarchy of VOs is used. When cases are simply **state values with no distinct structure**, an enum is used.

### 3.3 Entity vs Value Object Classification

**Entities (roots):**
Manufacturer, EngineModel, AircraftModel, Aircraft, AirControlArea, Airport, AirTransportCompany, Collaborator (abstract), Pilot, FlightRoute, Flight, WeatherData, Simulation.

**Entities (internal):**
AircraftVariant, ATCCollaborator, FlightControlOperator, WeatherPerson, SystemUser (framework boundary), FlightPlan.

**Value Objects — constitutive (composition `*--`):**
EngineName, Power, Thrust, TSFC, ModelID, AircraftWeights, AircraftPerformance, AerodynamicCoefficients, CompanyName, IATACode, ICAOCode, AreaCode, Coordinates_ACA, Coordinates_Apt, Coordinates_WD, Elevation, RegistrationNumber, CabinConfiguration, SeatClass, SecurityClearance, SkillsAssessment, RouteName, FlightDesignator, DepartureSchedule, RegularSchedule, ScheduleEntry, CharterSchedule, FuelQuantity, WindCondition, SimulationTimeRange, SafetyThreshold, SimulationReport.

**Value Objects — enumerations (association `-->`):**
MotorizationType, AircraftType, OperationalStatus, FlightType, FlightPlanStatus, ValidationResult.

### 3.4 Key Design Decisions

**Decision 1 — Enumerations use association (-->) not composition (\*--)**
Enumerations are referenced values, not parts owned by an entity. Applied to: MotorizationType, AircraftType, OperationalStatus, FlightType, FlightPlanStatus, ValidationResult. In PlantUML the `enum` keyword is used without `<<value object>>` stereotype — the keyword already expresses this.

**Decision 2 — DepartureSchedule as a VO hierarchy**
Section 3.2: *"Departure day (or days of the week for regular flights and actual date for a charter) and time."* Client: *"Charter flights will have only a single instance. Regular flights: the day of the week and the time for each flight instance. For example: Monday 12:00; Tuesday 12:30; Thursday 11:30."* The two cases have structurally different data — a VO hierarchy is used: `CharterSchedule` (one date + one time) and `RegularSchedule` (1..* `ScheduleEntry` instances, each with a day and a time). The abstract `DepartureSchedule` base has no attributes — it exists solely to allow `Flight` to have one polymorphic composition.

**Decision 3 — FlightType maintained alongside DepartureSchedule hierarchy**
Section 3.2 lists FlightType explicitly as a Flight attribute. It is maintained because: (1) it is explicit business information that can be consulted independently; (2) it enables a consistency invariant enforced by the Flight root: if FlightType is REGULAR then DepartureSchedule must be a RegularSchedule, and vice versa.

**Decision 4 — Collaborator is abstract**
A generic collaborator never exists — always a concrete specialisation (ATCCollaborator, FlightControlOperator, WeatherPerson). The abstract class enforces this at the domain level.

**Decision 5 — Pilot as separate aggregate**
Pilot is separated from the Collaborator aggregate because: (1) it has an independent lifecycle with its own use cases (US075, US076, US077); (2) it has a specific invariant — US077: cannot be deactivated with assigned flights; (3) it is directly referenced by Flight — if internal to Collaborator, Flight would have to reference Collaborator (semantically imprecise). Pilot inherits common collaborator attributes via inheritance. The `certifiedFor` association is now semantically precise on the Pilot root: `Pilot "*" --> "1..*" AircraftModel`.

**Decision 6 — employedBy and worksFor as known DDD hierarchy limitation**
The associations `Collaborator --> AirTransportCompany : employedBy` and `Collaborator --> AirControlArea : worksFor` are on the Collaborator root because internal entities cannot cross aggregate boundaries. Semantically: ATCCollaborator always belongs to a company; FCO and WeatherPerson always belong to an area. The 0..1 on both reflects this representation limitation, documented via notes on the diagram.

**Decision 7 — Admin and Backoffice Operator not modelled**
Internal actors with no domain-specific business rules beyond authentication. Not collaborators of external customers.

**Decision 8 — email and phoneNumber not in Collaborator**
These attributes belong to SystemUser (EAPLI framework). Duplicating them in Collaborator would violate the framework boundary.

**Decision 9 — AircraftVariant as internal entity**
US057: *"An aircraft model might have several aircraft variants (combinations of model and engine configuration)."* AircraftVariant has local identity within the aggregate — each combination is individually identifiable and can be added or removed independently. It holds EngineModelID only, respecting aggregate boundaries.

**Decision 10 — All engines of same motorisation type**
US055: *"let's assume that all engines are of the same type."* This is a fourth invariant enforced by the AircraftModel root when adding a new engine: all AircraftVariants within a model must share the same MotorizationType.

**Decision 11 — FlightLeg, Segment, Node, AltitudeSlot absent**
Client: *"I sincerely doubt we will need Flight Legs in the DDD Domain Model."* and *"Storing and managing all segments would be a lot of pain... can we avoid that?"* These belong to the DSL specification (LPROG) and are not managed by the Java application.

**Decision 12 — DepartureSchedule in Flight, not FlightPlan**
Section 3.2 lists departure day/time as an attribute of Flight. A flight may have multiple FlightPlans — if the departure schedule were in FlightPlan, different plans for the same flight could have different schedules, which makes no sense.

**Decision 13 — FlightPlan as internal entity of Flight**
Angelo initially suggested FlightPlan could be a VO: *"It's final. So, likely it is a value object."* However the full US flow contradicts immutability: US080 creates the FlightPlan in `draft`; US082 adds weather data to the existing FlightPlan; US085 validates it — status changes. A VO is immutable by definition and cannot change state or receive data after creation. Therefore FlightPlan is an internal entity of Flight, following the formal US behaviour.

**Decision 14 — Flight cross-aggregate references rise to root**
FlightPlan is an internal entity and cannot hold cross-aggregate references. The references to Aircraft (US080: "I must add the aircraft") and Pilot (US080: "I must add... pilot") rise to the Flight root. The reference to WeatherData (US082: weather added to flight plan) also rises to the Flight root with 0..1 multiplicity — optional because it is added after the flight plan is created in draft.

**Decision 15 — AircraftWeights and AerodynamicCoefficients as grouped VOs**
`AircraftWeights` groups four weight attributes (section 3.2) because they share the same unit and have a joint invariant: MTOW > MZFW > emptyWeight. `AerodynamicCoefficients` groups wingArea, Cd, and Cl because they are always used together in the lift and drag physics formulas (section 3.3).

**Decision 16 — AircraftPerformance without shared unit attribute**
`AircraftPerformance` groups serviceCeiling, cruiseSpeed, and maximumRange. Each has a different unit (metres/feet, knots/km/h, nautical miles/km) — a shared `unit` attribute would be ambiguous. Units are defined at implementation time. This is consistent with the approach: VOs with a single measurement (Power, Thrust, TSFC, FuelQuantity, Elevation, SafetyThreshold) carry explicit `value` and `unit`; AircraftPerformance does not because its three values have distinct units.

**Decision 17 — AirControlArea boundary as rectangle**
Client: *"In real life, an air control area is a polygon. For sake of simplicity, in the project it may be a rectangle."* Therefore `Coordinates_ACA` represents a rectangle (minLatitude, maxLatitude, minLongitude, maxLongitude). Additional invariants: boundaries cannot overlap; airport coordinates must fall within the area's boundary.

**Decision 18 — SimulationReport as VO**
SimulationReport is an immutable final document received from the C module. Unlike FlightPlan, it does not change after being received — no US adds data to it after creation. Therefore it is a VO. It contains `totalFlights` and references `ValidationResult` by association — a VO referencing an enum is valid because both are immutable.

**Decision 19 — WeatherData does not link to Collaborator**
No US requires tracking which WeatherPerson registered each record. The `sourceProvider` attribute covers the external data source origin.

**Decision 20 — No Services or Repositories in the domain model**
The domain model is a conceptual model of business concepts and rules. Services and Repositories are implementation concerns — in code, every aggregate root will have a corresponding Repository interface (CO3).

### 3.5 Client Clarifications Applied

| Clarification | Impact |
|---|---|
| *"A pilot only works for an ATC at a time."* | Collaborator --> AirTransportCompany multiplicity 0..1 |
| *"You cannot simulate an aircraft behaviour without the engines."* | AircraftVariant internal entity linking model to engine configuration |
| *"Route - 1:N - Flight - 1:N - Flight Plan."* | Flight --> FlightRoute; FlightPlan internal to Flight |
| *"A route is owned by an ATC. Its ID includes the company ID."* | FlightRoute --> AirTransportCompany; RouteName VO with format rule |
| *"A route has two endpoints."* | FlightRoute hasOrigin and hasDestination to Airport |
| *"Weather conditions are not the same everywhere inside an air control area."* | WindCondition carries Coordinates_WD; multiple readings per WeatherData record |
| *"I sincerely doubt we will need Flight Legs in the DDD Domain Model."* | FlightLeg absent |
| *"Storing and managing all segments would be a lot of pain."* | Segment, Node, AltitudeSlot absent |
| *"You have to send information to run the simulation and receive feedback."* | Simulation aggregate with SimulationReport VO |
| *"You don't need to detail performance settings in sprint 1."* | Generic SafetyThreshold and SimulationTimeRange only |
| *"Yes, it [Manufacturer] can [be both aircraft and engine maker]."* | Single Manufacturer entity covers both roles |
| *"Charter flights will have only a single instance. Regular flights: day of the week and time for each instance."* | DepartureSchedule hierarchy: CharterSchedule and RegularSchedule with ScheduleEntry |
| *"For sake of simplicity, [boundary] may be a rectangle. Boundaries cannot overlap. Airport coordinates must fall within its area."* | Coordinates_ACA as rectangle (minLat, maxLat, minLon, maxLon) |
| *"The requirements document mentions 'cabin configuration'."* | CabinConfiguration with SeatClass entries — no type-specific attributes beyond seating |
| *"US080 -> Create a flight, including its flight plan. US082 -> Add weather data to an existing flight plan."* | FlightPlan is internal entity of Flight (not VO); weather reference rises to Flight root |
| *"let's assume that all engines are of the same type"* | AircraftModel invariant: all AircraftVariants must share same MotorizationType |

---

## 4. Design

### 4.1. Realization

The domain model diagram was produced using PlantUML. Two representations are provided:

- **Full model** — all aggregates, cross-aggregate associations, and internal structure:
    - `docs/Sprint1/us_010/domain_model_full/puml/domain_model_full.puml`
    - `docs/Sprint1/us_010/domain_model_full/images/png/domain_model_full.png`
    - `docs/Sprint1/us_010/domain_model_full/images/svg/domain_model_full.svg`

- **Per-aggregate diagrams** — one diagram per aggregate showing internal entities and VOs (professor recommendation):
    - `docs/Sprint1/us_010/puml_aggregates/<name>_aggregate.puml`
    - `docs/Sprint1/us_010/images_aggregates/png/<name>_aggregate.png`
    - `docs/Sprint1/us_010/images_aggregates/svg/<name>_aggregate.svg`

Generate all diagrams:
```sh
sh generate-plantuml-diagrams.sh
```

**Notation conventions:**
- Package `X_Aggregate` = aggregate boundary. The `_Aggregate` suffix is required — without it, PlantUML renders cross-aggregate arrows from the package border rather than from the root class.
- `*--` = composition (part cannot exist without the whole)
- `-->` = association (enum or framework boundary)
- `<|--` = inheritance (specialisation)
- `>` on cross-aggregate labels = reading direction indicator

**Cross-aggregate associations:**

| From | Mult. | To | Mult. | Relation |
|---|---|---|---|---|
| EngineModel | \* | Manufacturer | 1 | manufacturedBy |
| AircraftModel | \* | Manufacturer | 1 | manufacturedBy |
| AircraftModel | \* | EngineModel | 1..* | certifies |
| Aircraft | \* | AircraftModel | 1 | ofModel |
| Aircraft | \* | AirTransportCompany | 1 | ownedBy |
| Airport | \* | AirControlArea | 1 | locatedIn |
| Collaborator | \* | AirTransportCompany | 0..1 | employedBy |
| Collaborator | \* | AirControlArea | 0..1 | worksFor |
| Pilot | \* | AircraftModel | 1..* | certifiedFor |
| FlightRoute | \* | Airport | 1 | hasOrigin |
| FlightRoute | \* | Airport | 1 | hasDestination |
| FlightRoute | \* | AirTransportCompany | 1 | ownedBy |
| Flight | \* | FlightRoute | 1 | instantiates |
| Flight | \* | Aircraft | 1 | uses |
| Flight | \* | Pilot | 1 | assignedTo |
| Flight | \* | WeatherData | 0..1 | usesWeather |
| WeatherData | \* | AirControlArea | 1 | registeredFor |
| Simulation | \* | AirControlArea | 1 | covers |
| Simulation | \* | Flight | 1..* | includes |
| Simulation | \* | WeatherData | \* | uses |

### 4.2. Acceptance Tests

**Test 1 — All use cases navigable through the domain model**

- US050 Register air control area → AirControlArea, AreaCode, Coordinates_ACA
- US052 Create airport → Airport, IATACode, ICAOCode, Coordinates_Apt, Elevation, AirControlArea
- US055 Create aircraft model → AircraftModel, ModelID, AircraftWeights, AircraftPerformance, AerodynamicCoefficients
- US056 Create engine model → EngineModel, EngineName, Power, Thrust, TSFC, MotorizationType
- US057 Add engine to aircraft model → AircraftVariant within AircraftModel, certifies
- US060 Register air transport company → AirTransportCompany, CompanyName, IATACode, ICAOCode
- US070 Add aircraft to fleet → Aircraft, RegistrationNumber, CabinConfiguration, SeatClass, OperationalStatus
- US073 Create flight route → FlightRoute, RouteName, Airport, AirTransportCompany
- US075 Add pilot → Pilot aggregate, certifiedFor AircraftModel
- US080 Create flight including flight plan → Flight, FlightPlan, FuelQuantity, FlightPlanStatus, Aircraft, Pilot
- US082 Add weather data to flight plan → Flight --> WeatherData
- US100 Simulate flights → Simulation, SimulationTimeRange, SafetyThreshold, AirControlArea, Flight, WeatherData
- US109/US111 Generate simulation report → SimulationReport, ValidationResult

**Test 2 — Checklist from Processo de Engenharia de Aplicações (section 4.4)**

1. No verb concepts — all concepts are nouns
2. All cross-aggregate associations have navigability and cardinality
3. No bidirectional associations
4. Cross-aggregate references point only to aggregate roots
5. All entities have a business identity
6. All concepts described in the glossary, synchronised with diagram
7. All names from requirements document — none invented
8. No technical/implementation names
9. No generic names
10. SystemUser boundary respected
11. All use cases navigable
12. Value objects do not hold cross-aggregate entity references
13. Enumerations use association not composition
14. No concepts added beyond requirements or client clarifications

---

## 5. Implementation

This user story produces design artefacts only. Deliverables:

- `docs/Sprint1/us_010/domain_model_full/puml/domain_model_full.puml` — Full model PlantUML source
- `docs/Sprint1/us_010/domain_model_full/images/` — Full model PNG and SVG
- `docs/Sprint1/us_010/puml_aggregates/` — Per-aggregate PlantUML sources
- `docs/Sprint1/us_010/images_aggregates/` — Per-aggregate PNG and SVG
- `docs/Sprint1/us_010/glossary.md` — Glossary of all domain concepts
- `docs/Sprint1/us_010/readme.md` — This file

Generate diagrams:
```sh
sh generate-plantuml-diagrams.sh
```

Major commits:
- 8fda0356f42c54443813577c61b99c1d1c4ba86a
- c94d92a0e8b75a9da5ce8fec9a100667bf059306
- 82a6e21bcd11f3ec7433f999619b0713d2dcebb3

---

## 6. Integration/Demonstration

The domain model is the foundation for all subsequent sprints. Before implementing any use case the team must:

1. Verify the domain model supports the use case by navigating it.
2. If new concepts are identified during implementation, update the domain model, glossary, and aggregate justification before writing code.
3. Java domain classes must remain synchronised with the domain model at all times (CO3 assessment criterion).

---

## 7. Observations

The domain model covers the EAPLI Java application scope only.

**On Repositories and Services:** In code, every aggregate root will have a corresponding Repository declared as a Java interface (CO3). Neither Repositories nor Services appear in the conceptual domain model.

**On the one-aggregate-per-use-case rule:** Each use case modifies exactly one aggregate. Cross-aggregate references are read-only — never for writing to two aggregates in the same transaction.

**On VOs duplicated in the diagram:** IATACode, ICAOCode, Coordinates_Apt, and Coordinates_WD appear in multiple aggregates for presentation clarity. In code there is a single class for each.

**On the _Aggregate suffix:** The `_Aggregate` suffix in package names is required in PlantUML. Without it, when a package name equals the root class name, PlantUML renders cross-aggregate arrows from the package border instead of from the root class.