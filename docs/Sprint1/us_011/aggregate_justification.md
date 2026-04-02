# Aggregate Justification — AISafe Domain Model

This document provides explicit justification for each of the 13 aggregates identified in the domain model, demonstrating the invariant each aggregate enforces and the design decisions behind its boundary.

---

## Aggregate 1 — Manufacturer

*Root:* Manufacturer

*Why an aggregate:* Manufacturer has its own lifecycle — registered independently by the Backoffice Operator (use case "Create aircraft/engine maker"). It is referenced by both EngineModel and AircraftModel but not owned by either. It is a simple aggregate with only the root — no internal entities are needed.

*Why one entity covers both roles:* Client clarification: "Yes, it can [be both aircraft and engine manufacturer]." The use case diagram uses a single "Create aircraft/engine maker" use case — one concept covers both roles.

*Internal members:* None. name and country are plain attributes — no business rule (format, uniqueness) is mentioned for either in the requirements. Section 3.1.2: "only basic information (name and country?) needs to be stored in the system."

*Invariant enforced:* Name must not be empty — a manufacturer without a name has no business meaning.

*Boundary justification:* EngineModel and AircraftModel reference Manufacturer by ID only — they do not own it. Manufacturer has no knowledge of which models reference it (no bidirectional association).

---

## Aggregate 2 — EngineModel

*Root:* EngineModel

*Why an aggregate:* EngineModel has its own lifecycle — registered independently by the Backoffice Operator (US056). It has its own invariants. It is referenced by AircraftModel via AircraftVariant but is not owned by it.

*Internal members:*
- EngineName (VO) — name has a business rule: name + manufacturer combination must be unique. Plain string insufficient.
- Power (VO) — value + unit. Must be positive. Plain number insufficient.
- Thrust (VO) — value + unit + speedReference. Two instances per EngineModel (static and cruise). Section 3.3: "Thrust is a parameter supplied by the manufacturer at two speeds: stopped and at cruise."
- TSFC (VO) — Thrust Specific Fuel Consumption. Value + unit. Must be positive. Section 3.3: "fuel consumption (grams/second) per unit of thrust (kN)."
- MotorizationType (enum) — fixed list: turboprop, turbofan, turbojet, ramjet, electricPropeller. Section 3.2.
- fuelType (plain attribute) — no format or uniqueness rule mentioned. Plain string sufficient.

*Invariants enforced:* Name + manufacturer combination must be unique (US056). Power, Thrust, and TSFC must be positive.

*Boundary justification:* AircraftVariant holds only the EngineModelID — never the EngineModel object itself. This respects the rule that nothing outside the aggregate holds a reference to anything inside.

---

## Aggregate 3 — AircraftModel

*Root:* AircraftModel

*Why an aggregate:* AircraftModel has its own lifecycle — registered (US055), engines added (US057), engines removed (US058). It has multiple invariants. It is referenced by Aircraft and Pilot but not owned by them.

*Why separate from Aircraft:* AircraftModel = the abstract type/design (e.g. "Boeing 737 MAX"). Aircraft = a specific physical instance (e.g. registration "CS-TUA"). They have independent lifecycles, different actors, and different invariants. Many Aircraft share one AircraftModel — bundling them would violate the one use case = one aggregate rule.

*Internal members:*
- ModelID (VO) — technical identifier. Business uniqueness rule: name + manufacturer unique (US055).
- AircraftType (enum) — passenger, cargo, mixed. Section 3.2.
- AircraftWeights (VO) — emptyWeight, MTOW, MZFW, maxFuelCapacity. Grouped because they share the same unit and have a joint invariant: MTOW > MZFW > emptyWeight. Section 3.2.
- AircraftPerformance (VO) — serviceCeiling, cruiseSpeed, maximumRange. Cohesive performance parameters. Each has its own unit — no shared unit attribute.
- AerodynamicCoefficients (VO) — wingArea, Cd, Cl. Always used together in lift/drag formulas (section 3.3): L = Cl×A×ρv²/2 and D = Cd×A×ρv²/2.
- AircraftVariant (entity internal) — US057: "combinations of model and engine configuration." Has local identity within the aggregate — each combination individually identifiable, can be added/removed independently. Holds EngineModelID only.

*Invariants enforced:* Name + manufacturer unique (US055). At least one certified engine model always associated (US055, US058). Same engine cannot be added twice (US057). All certified engines must be of the same motorisation type — US055: "let's assume that all engines are of the same type." MTOW > MZFW > emptyWeight, all positive.

*Boundary justification:* AircraftVariant references EngineModel by ID only. Aircraft references AircraftModel root — never AircraftVariant directly, because internal entities cannot cross aggregate boundaries.

---

## Aggregate 4 — Aircraft

*Root:* Aircraft

*Why an aggregate:* Aircraft has its own lifecycle — added to fleet by ATCCollaborator (US070), decommissioned (US071). It has its own invariants. It is referenced by Flight but not owned by it.

*Why separate from AircraftModel:* A specific aircraft instance has its own registration, its own cabin configuration, and its own operational status. Its lifecycle is managed independently of the model definition.

*Internal members:*
- RegistrationNumber (VO) — number + registrationCountry. Two cohesive attributes. Uniqueness rule: unique worldwide. US070: "registered in a country (it may not be the company's home country)."
- CabinConfiguration (VO) — container for 1..* SeatClass entries. Information expert for capacity invariant.
- SeatClass (VO) — className + numberOfSeats. Section 3.2: "number of seats in each class."
- OperationalStatus (enum) — active, decommissioned. US070/US071.
- numberOfFlightCrewMembers (plain attribute) — simple count, no business rule beyond being positive.

*Invariants enforced:* Registration number unique worldwide (US070). Total seats across all SeatClass entries cannot exceed the model's capacity (US070). A decommissioned aircraft cannot have new flights created for it (US071).

*Boundary justification:* Aircraft references AircraftModel root (ofModel) and AirTransportCompany root (ownedBy). Flight references Aircraft root — never its internal members.

---

## Aggregate 5 — AirControlArea

*Root:* AirControlArea

*Why an aggregate:* AirControlArea has its own lifecycle — registered by Backoffice Operator (US050). It is referenced by Airport, WeatherData, Simulation, and Collaborator but not owned by any of them.

*Internal members:*
- AreaCode (VO) — uniqueness rule: must be unique in the system. US050.
- Coordinates_ACA (VO) — rectangle defined by minLatitude, maxLatitude, minLongitude, maxLongitude. Client: "For sake of simplicity, in the project it may be a rectangle." Business rules: latitude [-90,90], longitude [-180,180], min < max. Distinct from Coordinates_Apt (single point).

*Invariants enforced:* AreaCode unique in the system (US050). Boundaries must be valid. Boundaries of different areas cannot overlap (client clarification). Airport coordinates must fall within the area's boundary (client clarification).

*Boundary justification:* All external aggregates reference AirControlArea by root. No external aggregate holds a reference to AreaCode or Coordinates_ACA directly.

---

## Aggregate 6 — Airport

*Root:* Airport

*Why an aggregate:* Airport has its own lifecycle — registered by Backoffice Operator (US052). It has its own invariants. It is referenced by FlightRoute (origin and destination) but not owned by it.

*Internal members:*
- IATACode (VO) — 3-letter code. Format rule and uniqueness worldwide. US052.
- ICAOCode (VO) — 4-letter code. Format rule and uniqueness worldwide. US052.
- Coordinates_Apt (VO) — latitude + longitude (single point). Validation rules: lat [-90,90], lon [-180,180]. US052: "location coordinates that must be valid."
- Elevation (VO) — value + unit. Must be non-negative. US052: "elevation in meters above sea level."
- name, town, country (plain attributes) — no format or uniqueness rules mentioned. Plain strings sufficient.

*Invariants enforced:* IATA code unique worldwide (US052). ICAO code unique worldwide (US052). Coordinates valid. Elevation valid.

*Boundary justification:* FlightRoute references Airport root (hasOrigin, hasDestination). No internal member of Airport is accessed from outside the aggregate.

---

## Aggregate 7 — AirTransportCompany

*Root:* AirTransportCompany

*Why an aggregate:* AirTransportCompany has its own lifecycle — registered by Backoffice Operator (US060). It is referenced by Aircraft, Collaborator, and FlightRoute but does not own them — they have independent lifecycles.

*Internal members:*
- CompanyName (VO) — uniqueness rule. US060.
- IATACode_ATC (VO) — 2-letter format rule and uniqueness. Same class in code as IATACode in Airport. US060.
- ICAOCode_ATC (VO) — 2-3 letter format rule and uniqueness. Same class in code as ICAOCode in Airport. US060.

*Invariants enforced:* Company name unique (US060). IATA code unique (US060). ICAO code unique (US060).

*Boundary justification:* Collaborator, Aircraft, and FlightRoute reference AirTransportCompany root. The company does not own these aggregates — they have independent lifecycles and repositories.

---

## Aggregate 8 — Collaborator

*Root:* Collaborator (abstract)

*Why an aggregate:* Collaborator has its own lifecycle — registered by Backoffice Operator (US061), disabled (US064). The three specialisations (ATCCollaborator, FlightControlOperator, WeatherPerson) share common attributes and are always manipulated together with those attributes.

*Why abstract:* A generic collaborator never exists — there is always a concrete specialisation. The requirements always identify collaborators by their specific role.

*Why Admin and Backoffice Operator not modelled:* Internal actors managed by the EAPLI framework with no domain-specific business rules beyond authentication.

*Why email and phoneNumber not in Collaborator:* They belong to SystemUser (EAPLI framework boundary). Duplicating them would violate the framework boundary.

*Internal members:*
- name (plain attribute) — no uniqueness or format rule. Plain string sufficient.
- position (plain attribute) — organisational title, distinct from collaborator type expressed by inheritance. No business rule. Plain string sufficient.
- SecurityClearance (VO) — expiryDate. Business rule: must be active (not expired). Section 3.1.1.
- SkillsAssessment (VO) — assessmentDate. Business rule: must be within the last 5 years. Section 3.1.1.
- ATCCollaborator (entity internal) — no attributes or invariants beyond inherited. No independent lifecycle.
- FlightControlOperator (entity internal) — idem.
- WeatherPerson (entity internal) — idem.
- SystemUser (entity, framework boundary) — associated by reference (-->) not composed. Has its own lifecycle managed by the framework.

*Known DDD hierarchy limitation:* The associations employedBy AirTransportCompany and worksFor AirControlArea are on the root because internal entities cannot cross aggregate boundaries. In code, ATCCollaborator populates employedBy; FCO and WeatherPerson populate worksFor. Documented via notes on the diagram.

*Invariants enforced:* SecurityClearance must not be expired. SkillsAssessment must be within 5 years.

*Boundary justification:* SystemUser has its own lifecycle — associated, not composed. External aggregates reference Collaborator root only.

---

## Aggregate 9 — Pilot

*Root:* Pilot

*Why a separate aggregate from Collaborator:* Pilot is separated because: (1) has an independent lifecycle with its own use cases — US075 creates, US076 lists, US077 deactivates; (2) has a specific invariant — US077: "cannot be removed if they have flights assigned"; (3) is directly referenced by Flight — if internal to Collaborator, Flight would reference Collaborator (semantically imprecise); (4) inherits common collaborator attributes (name, position, SecurityClearance, SkillsAssessment, SystemUser) from Collaborator via inheritance — no duplication.

*certifiedFor on Pilot root:* Now that Pilot is a separate root, the certifiedFor association is semantically precise: Pilot "*" --> "1..*" AircraftModel. US075: "A pilot is certified to pilot one or more aircraft models." Multiplicity 1..* — at least one model required.

*Invariants enforced:* Cannot be deactivated if assigned flights exist (US077). Must be certified for at least one AircraftModel (US075: "one or more").

*Boundary justification:* Flight references Pilot root (assignedTo). Pilot inherits framework linkage to SystemUser from Collaborator.

---

## Aggregate 10 — FlightRoute

*Root:* FlightRoute

*Why an aggregate:* FlightRoute has its own lifecycle — created by ATCCollaborator (US073), deactivated (US074). It has its own invariants. It is referenced by Flight but not owned by it.

*Internal members:*
- RouteName (VO) — format: 2 letters + up to 4 digits (e.g. TP123). Format rule and uniqueness rule. Client: "A route is owned by an ATC. Its ID includes the company ID." The 2-letter prefix must match the company IATA code — verified by controller on creation.
- deactivationDate (plain attribute) — simple date value. No format or uniqueness rule.

*On route ownership and operations:* ATCCollaborator acts on behalf of their company (US073, US074). Operations modify only FlightRoute — one use case, one aggregate. The controller verifies the ATCCollaborator belongs to the company that owns the route — cross-aggregate validation at application layer.

*Invariants enforced:* RouteName unique and follows format 2 letters + up to 4 digits (US073). Route cannot be deactivated if there are planned flights after the deactivation date (US074).

*Boundary justification:* Flight references FlightRoute root (instantiates). FlightRoute references Airport root (hasOrigin, hasDestination) and AirTransportCompany root (ownedBy) — never internal members of those aggregates.

---

## Aggregate 11 — Flight

*Root:* Flight

*Why an aggregate:* Flight has its own lifecycle — an instantiation of a route. Section 3.4: "a flight is an instantiation of a route." It has its own invariants. It is referenced by Simulation but not owned by it.

*On FlightPlan — internal entity, not VO:* Angelo initially suggested: "A flight plan is a description written using a DSL. It's final. So, likely it is a value object." However the full US flow contradicts immutability: US080 creates the FlightPlan in draft; US082 adds weather data to the existing FlightPlan; US085 validates it — status changes from draft to validated. A VO is immutable by definition. Therefore FlightPlan is an internal entity of Flight, following formal US behaviour. Angelo also confirmed: "one may have more than a flight plan for a flight" — if rejected, a new one is submitted.

*Internal members:*
- FlightDesignator (VO) — format xxn(n)(n)(n)(a), unique. Section 3.2.
- FlightType (enum) — regular, charter. Maintained alongside DepartureSchedule because: (1) explicit business attribute from section 3.2; (2) enables consistency invariant: FlightType must match DepartureSchedule specialisation.
- DepartureSchedule (abstract VO) — no attributes. Exists to allow Flight one polymorphic composition.
- RegularSchedule (VO) — container for 1..* ScheduleEntry. Client: "Monday 12:00; Tuesday 12:30; Thursday 11:30."
- ScheduleEntry (VO) — dayOfWeek + departureTime pair. Client clarification confirmed each day can have a different time.
- CharterSchedule (VO) — departureDate + departureTime. Client: "Charter flights will have only a single instance."
- scheduledArrivalTime (plain attribute) — simple time value. No invariant.
- FlightPlan (entity internal) — lifecycle: draft → validated. Has FlightPlanStatus and FuelQuantity.
- FlightPlanStatus (enum) — draft, validated. US080.
- FuelQuantity (VO) — value + unit. Must be strictly positive. US080.

*On cross-aggregate references rising to root:* FlightPlan is an internal entity and cannot hold cross-aggregate references. The references to Aircraft (US080), Pilot (US080), and WeatherData (US082) all rise to the Flight root. WeatherData multiplicity is 0..1 — optional at creation (draft), added later via US082.

*Invariants enforced:* FlightDesignator unique and follows format xxn(n)(n)(n)(a). FlightType must match DepartureSchedule specialisation. Only one FlightPlan validated at a time. FuelQuantity strictly positive.

*Boundary justification:* Simulation references Flight root (includes). Flight references FlightRoute, Aircraft, Pilot, and WeatherData roots — never their internal members.

---

## Aggregate 12 — WeatherData

*Root:* WeatherData

*Why an aggregate:* WeatherData has its own lifecycle — registered (US041), imported in bulk (US042), consulted (US043). It is referenced by Simulation and Flight but not owned by either.

*WeatherData does not link to Collaborator:* No US requires tracking which WeatherPerson registered each record. sourceProvider covers the external data source origin. The WeatherPerson is the actor — application layer concern, not domain model.

*Internal members:*
- recordedDateTime (plain attribute) — timestamp needed for US043: "consult weather data in a given day." No business rule beyond being valid. Java LocalDateTime handles validity.
- sourceProvider (plain attribute) — identifies the external provider (e.g. "IPMA"). No format or uniqueness rule.
- WindCondition (VO) — directionAngle + speed. Section 3.2: "Wind direction (angle) relative to North and speed (m/s)." Immutable snapshot.
- Coordinates_WD (VO) — latitude + longitude (single point) where the reading was taken. Composed inside WindCondition because each reading has its own location. Client: "Weather conditions are not the same everywhere inside an air control area."

*Invariants enforced:* recordedDateTime must be valid. WindCondition coordinates must be valid.

*Boundary justification:* Simulation references WeatherData root (uses). Flight references WeatherData root (usesWeather). WeatherData references AirControlArea root (registeredFor) — never internal members.

---

## Aggregate 13 — Simulation

*Root:* Simulation

*Why an aggregate:* Simulation has its own lifecycle — created by FlightControlOperator (US100). It groups all parameters sent to the C simulation module together with the results received back. Client: "You have to send information to run the simulation and you receive feedback/results from the simulation module. This information must be in the system."

*SimulationReport as VO:* SimulationReport is an immutable final document received from the C module. Unlike FlightPlan, it does not change after being received — no US adds data to it after creation. Therefore it is a VO. Contains totalFlights and references ValidationResult by association — a VO referencing an enum is valid because both are immutable.

*Internal members:*
- SimulationTimeRange (VO) — startDateTime + endDateTime. Business invariant: start < end. Information expert for this two-attribute constraint. US100: "time range" parameter.
- SafetyThreshold (VO) — value + unit. Must be positive. US100: "safety thresholds" parameter.
- SimulationReport (VO) — totalFlights + ValidationResult reference. Final immutable document. US109/US111.
- ValidationResult (enum) — passed, failed. US109/US111.

*Invariants enforced:* At least one flight must be included (Simulation --> Flight multiplicity 1..*). SimulationTimeRange start must be before end. SafetyThreshold must be positive.

*Boundary justification:* Simulation references AirControlArea, Flight, and WeatherData roots — never their internal members.