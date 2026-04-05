# Aggregate Justification — AISafe Domain Model

This document justifies each of the 13 aggregates: why the cluster exists as an aggregate, what invariants the root enforces, which concepts belong inside the boundary, and how the boundary relates to other aggregates.

---

## Aggregate 1 — Manufacturer

**Root:** Manufacturer

**Why an aggregate:** Has an independent lifecycle — registered by the Backoffice Operator independently of any engine or aircraft model. Referenced by EngineModel and AircraftModel but owned by neither. A simple aggregate with only the root — no internal entities needed.

**Why one entity covers both roles:** Client clarification: *"Yes, it can [be both aircraft and engine manufacturer]."* The use case diagram has a single "Create aircraft/engine maker" use case.

**Invariant enforced:** Name must not be empty — a manufacturer without a name has no business meaning.

**Boundary justification:** EngineModel and AircraftModel reference Manufacturer by ID only — they never hold the Manufacturer object. No bidirectional association.

---

## Aggregate 2 — EngineModel

**Root:** EngineModel

**Why an aggregate:** Has an independent lifecycle — registered by the Backoffice Operator (US056) independently of any aircraft model. Has its own invariants distinct from AircraftModel. Referenced by AircraftModel via AircraftVariant (by ID only) but not owned by it.

**Internal members:** EngineName, Power, Thrust, TSFC, MotorizationType, fuelType. All are constitutive characteristics of this engine — they have no meaning outside this context.

**Invariants enforced:** Name + manufacturer combination must be unique (US056). Power, Thrust, and TSFC must be positive.

**Boundary justification:** AircraftVariant holds only the EngineModelID — never the EngineModel object. This respects the rule that nothing outside the aggregate holds a reference to anything inside it.

---

## Aggregate 3 — AircraftModel

**Root:** AircraftModel

**Why an aggregate:** Has an independent lifecycle — registered (US055), engines added (US057), engines removed (US058). Has multiple invariants that must be enforced atomically. Referenced by Aircraft and Pilot but owned by neither.

**Why separate from Aircraft:** AircraftModel = the abstract type/design. Aircraft = a specific physical instance. They have different actors, different invariants, and different lifecycles. N Aircraft share 1 AircraftModel — bundling them would mean one use case updates two aggregates (violation of ACID-per-aggregate rule).

**Internal members:** ModelID, AircraftType, AircraftWeights, AircraftPerformance, AerodynamicCoefficients, AircraftVariant. AircraftVariant is an internal entity because it has local identity — each (model, engine) combination is individually identifiable and can be added or removed independently. It holds only EngineModelID, not the EngineModel object.

**Invariants enforced:**
- Name + manufacturer unique (US055)
- At least one certified engine model always associated (US055, US058 — cannot remove the last one)
- Same engine model cannot be added twice (US057)
- All certified engines must be of the same motorisation type — US055: *"let's assume that all engines are of the same type"*
- MTOW > MZFW > emptyWeight, all positive (section 3.2)

**Boundary justification:** Aircraft references AircraftModel root — never AircraftVariant, because internal entities cannot cross aggregate boundaries.

---

## Aggregate 4 — Aircraft

**Root:** Aircraft

**Why an aggregate:** Has an independent lifecycle — added to fleet (US070), decommissioned (US071). Has its own invariants distinct from AircraftModel. Referenced by Flight but not owned by it.

**Why separate from AircraftModel:** A specific aircraft instance has its own unique registration, its own cabin configuration, and its own operational status. Its lifecycle is independent of the model definition — an aircraft can be decommissioned while the model remains active.

**Internal members:** RegistrationNumber, CabinConfiguration, SeatClass (within CabinConfiguration), OperationalStatus, numberOfFlightCrewMembers.

**Invariants enforced:**
- Registration number unique worldwide (US070)
- Total seats across all SeatClass entries cannot exceed the model's capacity (US070)
- A decommissioned aircraft cannot have new flights created for it (US071)

**Boundary justification:** Flight references Aircraft root. Aircraft references AircraftModel root (ofModel) and AirTransportCompany root (ownedBy) — never their internal members.

---

## Aggregate 5 — AirControlArea

**Root:** AirControlArea

**Why an aggregate:** Has an independent lifecycle — registered by the Backoffice Operator (US050). Referenced by Airport, WeatherData, Simulation, and Collaborator but owned by none of them. 

**Internal members:** AreaCode, Coordinates_ACA.

**Invariants enforced:**
- AreaCode unique in the system (US050)
- Boundaries must be valid (latitude ∈ [-90,90], longitude ∈ [-180,180], min < max)
- Boundaries of different areas cannot overlap (client clarification)
- Airport coordinates must fall within the area's boundary (client clarification)

**Boundary justification:** All external aggregates (Airport, WeatherData, Simulation, Collaborator) reference AirControlArea by root. No external aggregate accesses AreaCode or Coordinates_ACA directly.

---

## Aggregate 6 — Airport

**Root:** Airport

**Why an aggregate:** Has an independent lifecycle — registered by the Backoffice Operator (US052). Referenced by FlightRoute (as origin and destination) but not owned by it.

**Internal members:** IATACode, ICAOCode, Coordinates_Apt, Elevation, plus the plain attributes name, town, country.

**Invariants enforced:**
- IATA code unique worldwide (US052)
- ICAO code unique worldwide (US052)
- Coordinates must be valid
- Elevation must be non-negative

**Boundary justification:** FlightRoute references Airport root twice (hasOrigin, hasDestination). No internal member of Airport is accessed from outside the aggregate.

---

## Aggregate 7 — AirTransportCompany

**Root:** AirTransportCompany

**Why an aggregate:** Has an independent lifecycle — registered by the Backoffice Operator (US060). Referenced by Aircraft, Collaborator, and FlightRoute but does not own any of them — each has its own independent lifecycle.

**Internal members:** CompanyName, IATACode_ATC, ICAOCode_ATC.

**Invariants enforced:**
- Company name unique (US060)
- IATA code unique (US060)
- ICAO code unique (US060)

**Boundary justification:** Collaborator, Aircraft, and FlightRoute reference AirTransportCompany root. The company does not own these aggregates — they have independent lifecycles and separate repositories.

---

## Aggregate 8 — Collaborator

**Root:** Collaborator (abstract)

**Why an aggregate:** Has an independent lifecycle — registered by the Backoffice Operator (US061), disabled (US064). The three specialisations (ATCCollaborator, FlightControlOperator, WeatherPerson) share common attributes and invariants that are always enforced together.

**Why abstract:** A generic collaborator never exists — always a concrete specialisation. The requirements identify collaborators always by their specific role.

**Known DDD hierarchy limitation:** The associations `employedBy AirTransportCompany` (ATCCollaborator only) and `worksFor AirControlArea` (FCO and WeatherPerson only) are placed on the root because internal entities cannot cross aggregate boundaries. The 0..1 on both reflects this representation constraint — in code, each specialisation populates only its relevant association. Documented via notes on the diagram.

**Internal members:** ATCCollaborator, FlightControlOperator, WeatherPerson (internal entities — no independent lifecycle), SecurityClearance, SkillsAssessment, name, position, plus the SystemUser reference. SystemUser is associated by reference (`-->`) not composed — it has its own lifecycle managed by the EAPLI framework.

**Invariants enforced:**
- SecurityClearance must not be expired (section 3.1.1)
- SkillsAssessment must be within the last 5 years (section 3.1.1)

**Boundary justification:** External aggregates reference Collaborator root. SystemUser has its own lifecycle — associated, not composed.

---

## Aggregate 9 — Pilot

**Root:** Pilot

**Why a separate aggregate from Collaborator:** Four reasons justify the separation:
1. **Independent lifecycle** — US075 creates, US076 lists, US077 deactivates — three dedicated use cases.
2. **Specific invariant** — US077: *"cannot be removed if they have flights assigned."* This invariant does not apply to any other Collaborator specialisation.
3. **Direct reference by Flight** — Flight must reference Pilot (US080). If Pilot were internal to Collaborator, Flight would reference Collaborator — semantically imprecise, and cross-aggregate references must point to roots.
4. **No duplication** — Pilot inherits all common Collaborator attributes (name, position, SecurityClearance, SkillsAssessment, SystemUser) via inheritance. Nothing is repeated.

**certifiedFor on Pilot root:** With Pilot as a separate root, the `certifiedFor` association is semantically precise: `Pilot "*" --> "1..*" AircraftModel`. US075: *"one or more aircraft models"* — multiplicity 1..*, a pilot must always be certified for at least one model.

**Invariants enforced:**
- Cannot be deactivated with assigned flights (US077)
- Must be certified for at least one AircraftModel (US075)

**Boundary justification:** Flight references Pilot root (assignedTo). Pilot references AircraftModel root (certifiedFor) — never AircraftVariant or any internal member.

---

## Aggregate 10 — FlightRoute

**Root:** FlightRoute

**Why an aggregate:** Has an independent lifecycle — created by ATCCollaborator (US073), deactivated (US074). Referenced by Flight but not owned by it. Client: *"A route is owned by an ATC. Its ID includes the company ID."*

**Internal members:** RouteName, deactivationDate.

**On route ownership validation:** The controller verifies that the ATCCollaborator belongs to the company that owns the route — cross-aggregate validation at the application layer, not by the root. The RouteName prefix (2 letters) must match the company IATA code — also verified by the controller on creation.

**Invariants enforced:**
- RouteName unique and follows format: 2 letters + up to 4 digits (US073)
- Cannot be deactivated if planned flights exist after the deactivation date (US074)

**Boundary justification:** Flight references FlightRoute root (instantiates). FlightRoute references Airport root (hasOrigin, hasDestination) and AirTransportCompany root (ownedBy) — never their internal members.

---

## Aggregate 11 — Flight

**Root:** Flight

**Why an aggregate:** Has an independent lifecycle — an instantiation of a route (section 3.4). Has its own invariants. Referenced by Simulation but not owned by it. Client: *"Route - 1:N - Flight - 1:N - Flight Plan."*

**Why FlightPlan is an internal entity (not VO):** Client initially suggested: *"A flight plan is a description written using a DSL. It's final. So, likely it is a value object."* However the full US flow contradicts immutability: US080 creates FlightPlan in `draft`; US082 adds weather data to the *existing* FlightPlan; US085 validates it — status changes. A VO is immutable by definition. Therefore FlightPlan is an internal entity of Flight. Client also confirmed: *"one may have more than a flight plan for a flight"* — if one is rejected, a new one is submitted.

**Why cross-aggregate references rise to Flight root:** FlightPlan is an internal entity and cannot hold cross-aggregate references. The references to Aircraft (US080), Pilot (US080), and WeatherData (US082) all rise to the Flight root. WeatherData is `0..1` — optional at creation in draft, added later via US082.

**Why DepartureSchedule is in Flight, not FlightPlan:** Section 3.2 lists departure day/time as an attribute of Flight. A flight may have multiple FlightPlans — different plans for the same flight must not have different departure schedules.

**Internal members:** FlightDesignator, FlightType, DepartureSchedule (abstract VO with RegularSchedule and CharterSchedule specialisations), ScheduleEntry (within RegularSchedule), scheduledArrivalTime, FlightPlan (internal entity containing FlightPlanStatus and FuelQuantity).

**Invariants enforced:**
- FlightDesignator unique and follows format xxn(n)(n)(n)(a)
- FlightType must match DepartureSchedule specialisation (regular ↔ RegularSchedule; charter ↔ CharterSchedule)
- Only one FlightPlan validated at a time
- FuelQuantity strictly positive

**Boundary justification:** Simulation references Flight root. Flight references FlightRoute, Aircraft, Pilot, and WeatherData roots — never their internal members.

---

## Aggregate 12 — WeatherData

**Root:** WeatherData

**Why an aggregate:** Has an independent lifecycle — registered (US041), imported in bulk (US042), consulted (US043). Referenced by Simulation and Flight but owned by neither. US042: *"data might be from multiple external weather service providers."*

**Why WeatherData does not link to Collaborator:** No US requires tracking which WeatherPerson registered each record. `sourceProvider` covers the external data source origin. The WeatherPerson is the actor — an application-layer concern, not a domain model concern.

**Internal members:** recordedDateTime, sourceProvider, WindCondition (×1..*), Coordinates_WD (within each WindCondition). WindCondition carries Coordinates_WD because client: *"weather conditions are not the same everywhere inside an air control area"* — each reading is localised to a specific point.

**Invariants enforced:**
- recordedDateTime must be valid
- Each WindCondition must have valid coordinates

**Boundary justification:** Simulation references WeatherData root (uses). Flight references WeatherData root (usesWeather, 0..1). WeatherData references AirControlArea root (registeredFor) — never internal members of AirControlArea.

---

## Aggregate 13 — Simulation

**Root:** Simulation

**Why an aggregate:** Has an independent lifecycle — created by FlightControlOperator (US100). Groups all parameters sent to the C simulation module together with the results received back. Client: *"You have to send information to run the simulation and you receive feedback/results from the simulation module. This information must be in the system."*

**Why SimulationReport is a VO (not internal entity):** SimulationReport is an immutable final document received from the C module. No US adds data to it after it is received — unlike FlightPlan, which changes status and receives weather data after creation. Therefore SimulationReport is a VO. It references ValidationResult by association (`-->`) — a VO referencing an enum is valid because both are immutable.

**Internal members:** SimulationTimeRange, SafetyThreshold, SimulationReport (VO), ValidationResult (enum within SimulationReport).

**Invariants enforced:**
- At least one flight must be included — `Simulation "*" --> "1..*" Flight` (US100)
- SimulationTimeRange: start must be before end
- SafetyThreshold must be positive

**Boundary justification:** Simulation references AirControlArea, Flight, and WeatherData roots — never their internal members.