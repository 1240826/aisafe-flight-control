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

A **Value Object** is a domain concept that characterises or describes another concept. It has no identity of its own — two VOs with the same attributes are considered equal. VOs are immutable and follow the **Information Expert** principle: they encapsulate and validate their own data. If a concept has a business rule (format, uniqueness, validation, invariant), it should be a Value Object rather than a plain attribute.

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

**Entities** — have business identity and an independent lifecycle:
Manufacturer, EngineModel, AircraftModel, AircraftVariant, Aircraft, AirControlArea, Airport, AirTransportCompany, Collaborator (abstract), ATCCollaborator, Pilot, FlightControlOperator, WeatherPerson, SystemUser (framework boundary), FlightRoute, Flight, FlightPlan, WeatherData, Simulation, SimulationReport, SafetyViolation.

**Value Objects — constitutive (composition \*--)** — characterise an entity, no independent lifecycle:
EngineName, Power, Thrust, TSFC, ModelID, AircraftWeights, AircraftPerformance, AerodynamicCoefficients, RegistrationNumber, CabinConfiguration, AreaCode, Coordinates, Coordinates_ACA, IATACode, ICAOCode, Elevation, CompanyName, CollaboratorName, Position, SecurityClearance, SkillsAssessment, RouteName, FlightDesignator, DepartureSchedule, RegularSchedule, ScheduleEntry, CharterSchedule, FuelQuantity, SimulationTimeRange, SafetyThreshold, VelocityVector, WindCondition.

**Value Objects — enumerations (association -->)** — fixed sets of domain values:
MotorizationType, AircraftType, OperationalStatus, FlightType, FlightPlanStatus, ValidationResult.

### 3.4 Key Design Decisions

**Decision 1 — Enumerations use association (-->) not composition (\*--)**
Enumerations are referenced values, not parts owned by an entity. Applied to: MotorizationType, AircraftType, OperationalStatus, FlightType, FlightPlanStatus, ValidationResult.

**Decision 2 — DepartureSchedule as a VO hierarchy**
Section 3.2: *"Departure day (or days of the week for regular flights and actual date for a charter) and time."*

Client clarification: *"Charter flights will have only a single instance. Regular flights have a recurring schedule — the day of the week and the time for each flight instance. For example: Monday 12:00; Tuesday 12:30; Thursday 11:30."*

The two cases have structurally different data, so a VO hierarchy is used rather than a single VO with optional fields or an enum:
- `CharterSchedule` — one departure date and one departure time.
- `RegularSchedule` — a container for 1..* `ScheduleEntry` instances. Each entry has a `dayOfWeek` and a `departureTime` because each day can have a different time.
- `DepartureSchedule` abstract base has no attributes — it exists solely to allow `Flight` to have one polymorphic composition, so a flight always has exactly one schedule regardless of type.

**Decision 3 — Collaborator is abstract, certifiedFor on root**
Collaborator is abstract because a generic collaborator never exists — always a concrete specialisation (ATCCollaborator, Pilot, FlightControlOperator, WeatherPerson).

US075: *"A pilot is certified to pilot one or more aircraft models."* The certifiedFor association is declared on the Collaborator root because Pilot is an internal entity and DDD rules prevent internal entities from crossing aggregate boundaries. In code, only Pilot implements the certification list — the other specialisations have empty lists. This is a known trade-off when modelling hierarchies in DDD.

**Decision 4 — Pilot is not a separate aggregate**
Although Pilot has specific behaviour (certifications, US077 invariant), the data of Pilot and Collaborator are always manipulated together — creating a Pilot means creating a Collaborator with certifications in one operation. The criterion for a separate aggregate is not met independently of the Collaborator context.

**Decision 5 — Admin and Backoffice Operator not modelled**
Internal actors with no domain-specific business rules beyond authentication. Not collaborators of external customers. The four modelled collaborator types (ATCCollaborator, Pilot, FCO, WeatherPerson) all belong to external customers.

**Decision 6 — email and phoneNumber not in Collaborator**
These attributes belong to SystemUser (EAPLI framework). Section 3.1.1 and 3.1.3 mention them in the context of system users, not domain collaborators. Duplicating them in Collaborator would violate the framework boundary — they are referenced in the SystemUser note in the diagram.

**Decision 7 — AircraftVariant as internal entity**
US057: *"An aircraft model might have several aircraft variants (combinations of model and engine configuration)."* AircraftVariant is an entity (not a VO) because it has local identity within the aggregate — each combination is individually identifiable and can be added or removed independently. It holds a reference to EngineModel by ID only, respecting aggregate boundaries.

**Decision 8 — FlightLeg, Segment, Node, AltitudeSlot absent**
Client: *"I sincerely doubt we will need Flight Legs in the DDD Domain Model."* and *"Storing and managing all segments would be a lot of pain... can we avoid that?"* and *"I believe we don't have any user story about managing nodes/junctions."* These belong to the DSL specification (LPROG) and are not managed by the Java application.

**Decision 9 — DepartureSchedule in Flight, not FlightPlan**
Section 3.2 lists departure day/time as an attribute of Flight. A flight may have multiple FlightPlans (client: *"one can have multiple Flight Plans, albeit just one approved/validated"*) — if the departure schedule were in FlightPlan, different plans for the same flight could have different schedules, which makes no sense.

**Decision 10 — AircraftWeights and AerodynamicCoefficients as grouped VOs**
`AircraftWeights` groups emptyWeight, MTOW, MZFW, and maxFuelCapacity because they share the same unit and have a joint invariant: MTOW > MZFW > emptyWeight (section 3.2). The VO is the information expert for this invariant.

`AerodynamicCoefficients` groups wingArea, Cd, and Cl because they are always used together in the lift and drag formulas (section 3.3): L = Cl × A × ρv²/2 and D = Cd × A × ρv²/2.

**Decision 11 — Thrust has speedReference**
Section 3.3: *"Thrust is a parameter supplied by the manufacturer at two speeds: stopped and at cruise."* An EngineModel has two Thrust values. `speedReference` identifies which of the two each instance represents.

**Decision 12 — AirControlArea boundary as rectangle**
Client clarification: *"In real life, an air control area is a polygon. For sake of simplicity, in the project it may be a rectangle."* Therefore `Coordinates_ACA` represents a rectangle (minLatitude, maxLatitude, minLongitude, maxLongitude), distinct from `Coordinates` used elsewhere which represents a single point. Additional invariants confirmed: boundaries cannot overlap; airport coordinates must fall within its area's boundary.

**Decision 13 — SimulationReport and SafetyViolation as internal entities**
Both have identity within the simulation context. SimulationReport accumulates SafetyViolation events over time and is not replaceable as a whole. Neither has a lifecycle outside the Simulation aggregate — internal entities by composition.

**Decision 14 — WeatherData Coordinates represent a point**
Client: *"Weather conditions are not the same everywhere inside an air control area."* Each WindCondition reading is localised to a specific point (latitude + longitude) within the area where it was measured, not to a sub-area.

**Decision 15 — No Services or Repositories in the domain model**
The domain model is a conceptual model of business concepts and rules. Services and Repositories are implementation concerns — they appear in code and sequence diagrams, not in the conceptual model. In code, every aggregate root will have a corresponding Repository interface (CO3).

### 3.5 Client Clarifications Applied

| Clarification | Impact |
|---|---|
| *"A pilot only works for an ATC at a time."* | Collaborator --> AirTransportCompany multiplicity 0..1 |
| *"You cannot simulate an aircraft behaviour without the engines."* | AircraftVariant internal entity linking model to engine |
| *"Route - 1:N - Flight - 1:N - Flight Plan."* | FlightPlan --> Flight --> FlightRoute direction confirmed |
| *"A route is owned by an ATC. Its ID includes the company ID."* | FlightRoute --> AirTransportCompany; RouteName VO with format rule |
| *"A route has two endpoints."* | FlightRoute hasOrigin and hasDestination to Airport |
| *"Weather conditions are not the same everywhere inside an air control area."* | WindCondition carries Coordinates and altitude |
| *"I sincerely doubt we will need Flight Legs in the DDD Domain Model."* | FlightLeg absent |
| *"Storing and managing all segments would be a lot of pain."* | Segment, Node, AltitudeSlot absent |
| *"You have to send information to run the simulation and receive feedback."* | Simulation aggregate with SimulationReport and SafetyViolation |
| *"You don't need to detail performance settings in sprint 1."* | Generic SafetyThreshold and SimulationTimeRange only |
| *"Yes, it [Manufacturer] can [be both aircraft and engine maker]."* | Single Manufacturer entity covers both roles |
| *"Charter flights will have only a single instance. Regular flights: day of the week and time for each instance."* | DepartureSchedule hierarchy: CharterSchedule and RegularSchedule with ScheduleEntry |
| *"For sake of simplicity, [boundary] may be a rectangle. Boundaries cannot overlap. Airport coordinates must fall within its area."* | Coordinates_ACA as rectangle (minLat, maxLat, minLon, maxLon) |
| *"The requirements document mentions 'cabin configuration'."* | CabinConfiguration with seatsPerClass only — no type-specific attributes |

---

## 4. Design

### 4.1. Realization

The domain model diagram was produced using PlantUML and is available at:

![Domain Model](domain-model.svg)

Source: `docs/Sprint1/us_010/domain_model_ddd.puml`

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
| Collaborator | \* | AircraftModel | \* | certifiedFor |
| FlightRoute | \* | Airport | 1 | hasOrigin |
| FlightRoute | \* | Airport | 1 | hasDestination |
| FlightRoute | \* | AirTransportCompany | 1 | ownedBy |
| Flight | \* | FlightRoute | 1 | instantiates |
| FlightPlan | \* | Flight | 1 | plannedFor |
| FlightPlan | \* | Aircraft | 1 | uses |
| FlightPlan | \* | Collaborator | 1 | assignedTo |
| WeatherData | \* | AirControlArea | 1 | registeredFor |
| Simulation | \* | AirControlArea | 1 | covers |
| Simulation | \* | Flight | 1..* | includes |
| Simulation | \* | WeatherData | \* | uses |

### 4.2. Acceptance Tests

**Test 1 — All use cases navigable through the domain model**

- US050 Register air control area → AirControlArea, AreaCode, Coordinates_ACA
- US052 Create airport → Airport, IATACode, ICAOCode, Coordinates, Elevation, AirControlArea
- US055 Create aircraft model → AircraftModel, ModelID, AircraftWeights, AircraftPerformance, AerodynamicCoefficients
- US056 Create engine model → EngineModel, EngineName, Power, Thrust, TSFC, MotorizationType
- US057 Add engine to aircraft model → AircraftVariant within AircraftModel, certifies
- US060 Register air transport company → AirTransportCompany, CompanyName, IATACode, ICAOCode
- US070 Add aircraft to fleet → Aircraft, RegistrationNumber, CabinConfiguration, OperationalStatus
- US073 Create flight route → FlightRoute, RouteName, Airport, AirTransportCompany
- US075 Add pilot → Pilot within Collaborator, certifiedFor AircraftModel
- US080 Create flight plan → FlightPlan, FuelQuantity, FlightPlanStatus, Flight, Aircraft, Collaborator
- US100 Simulate flights → Simulation, SimulationTimeRange, SafetyThreshold, AirControlArea, Flight, WeatherData
- US109 Generate simulation report → SimulationReport, SafetyViolation, VelocityVector, Coordinates, ValidationResult

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

- `docs/Sprint1/us_010/domain_model_ddd.puml` — PlantUML source
- `docs/Sprint1/us_010/domain-model.svg` — Vector diagram (generated via `generate-plantuml-diagrams.sh`)
- `docs/Sprint1/us_010/glossary.md` — Glossary of all domain concepts
- `docs/Sprint1/us_010/readme.md` — This file

Generate the diagram:
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

**On VOs duplicated in the diagram:** Coordinates, IATACode, and ICAOCode appear in multiple aggregates for presentation clarity. In code there is a single class for each.