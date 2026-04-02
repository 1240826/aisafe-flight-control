# Entity vs Value Object Justification — AISafe Domain Model

## Criteria Applied

**Entity:** has its own business identity and independent lifecycle. Tracked by identity, not attributes.

**Value Object:** characterises another concept. No identity of its own — two VOs with the same attributes are equal. Immutable. Follows the **Information Expert** principle: encapsulates its own validation rules. A concept should be a VO (not a plain attribute) when:
- It has a business rule (format, uniqueness, validation, invariant), OR
- A plain string/number is not sufficient to represent it — it is more complex than a primitive type

**Plain attribute:** a simple primitive value with no business rules beyond basic type validity.

**Enum:** a fixed set of domain values with no distinct structure — referenced by association (-->), never composed (*--). The enum type in PlantUML does not use <<value object>> stereotype — the `enum` keyword already expresses this.

**No Services or Repositories in the domain model:** The domain model is a conceptual model of business concepts and rules. Services and Repositories are implementation concerns — they appear in code and sequence diagrams, not in the conceptual model. In code, every aggregate root will have a corresponding Repository interface (CO3).

**One use case = one aggregate:** Each use case modifies exactly one aggregate (ACID within aggregate, BASE between aggregates). Cross-aggregate references are read-only — never writing to two aggregates in the same transaction.

---

## Manufacturer Aggregate

**Why an aggregate:** Has its own lifecycle — registered independently by the Backoffice Operator (use case "Create aircraft/engine maker"). Simple aggregate with only the root. Referenced by both EngineModel and AircraftModel but not owned by either.

**Why one Manufacturer for both:** Client: "Yes, it can [be both aircraft and engine manufacturer]." Use case: "Create aircraft/engine maker" — single use case for both roles.

**Cross-aggregate associations:**
- `EngineModel "*" --> "1" Manufacturer : manufacturedBy` — mandatory "1", always has a manufacturer. "*" — many engine models per manufacturer.
- `AircraftModel "*" --> "1" Manufacturer : manufacturedBy` — same reasoning.

**Invariants:** No explicit uniqueness rule in requirements. Only defensible invariant: name must not be empty.

| Concept | Classification | Justification |
|---|---|---|
| **Manufacturer** | Entity (root) | Independent lifecycle. Simple aggregate, no internal entities. Referenced by EngineModel and AircraftModel but not owned by them. |
| `name` | Plain attribute | Section 3.1.2: "only basic information (name and country?)". No uniqueness rule, no format rule. Distinct from CompanyName (has uniqueness rule) and RouteName (has format rule). Plain string sufficient. |
| `country` | Plain attribute | Section 3.1.2: "name and country?" — question mark suggests optional. No format or validation rule. Plain string sufficient. |

---

## EngineModel Aggregate

**Why an aggregate:** Has its own lifecycle — registered independently (US056). Has own invariants: name+manufacturer unique, physical values positive. Referenced by AircraftVariant by ID only.

**Cross-aggregate association:**
- `EngineModel "*" --> "1" Manufacturer : manufacturedBy` — mandatory "1".

| Concept | Classification | Justification |
|---|---|---|
| **EngineModel** | Entity (root) | Independent lifecycle — registered (US056). Invariants: name+manufacturer unique, Power/Thrust/TSFC positive. |
| **EngineName** | Value Object | US056: "name and manufacturer combination must be unique." Uniqueness rule — plain string cannot enforce. VO is information expert. |
| `fuelType` | Plain attribute | US056 mentions "fuel" as important but specifies no format, uniqueness, or validation rule. Plain string sufficient. |
| **Power** | Value Object | US056: "power" listed as important. Has `value` and `unit` — unit is inseparable from value. Business rule: must be positive. Plain number insufficient. |
| **Thrust** | Value Object | Section 3.3: "supplied at two speeds: stopped and at cruise." Has `value`, `unit`, `speedReference` — three cohesive physical attributes. Multiplicity "2" — exactly two Thrust instances per EngineModel (static and cruise). Business rule: must be positive. Plain number cannot represent this. |
| **TSFC** | Value Object | Section 3.3: "fuel consumption (grams/second) per unit of thrust (kN). Can be expressed in other units." Has `value` and `unit` — unit varies (g/s/kN, N/N/S, US units). Must be positive. Plain number insufficient. |
| **MotorizationType** | Value Object (enum) | Section 3.2: fixed list (turboprop, turbofan, turbojet, ramjet, electricPropeller). No distinct data structure per value. Referenced by association (-->), not composed. No <<value object>> stereotype — `enum` keyword is sufficient. |

---

## AircraftModel Aggregate

**Why a separate aggregate from Aircraft:** AircraftModel = abstract type/design shared by many aircraft. Aircraft = specific physical instance. Separate because: (1) independent lifecycles — AircraftModel created by Backoffice Operator, Aircraft by ATCCollaborator; (2) different invariants; (3) different actors; (4) N Aircraft share 1 AircraftModel — bundling would violate one use case = one aggregate.

**Why an aggregate:** Independent lifecycle — registered (US055), engines added (US057), removed (US058). Multiple invariants enforced by root.

**Invariants enforced by root:**
- name + manufacturer unique (US055)
- at least one engine certified always (US055, US058)
- same engine cannot be added twice (US057)
- all certified engines must be same motorization type — US055: "let's assume that all engines are of the same type"
- MTOW > MZFW > emptyWeight, all positive (AircraftWeights VO, section 3.2)

**Cross-aggregate associations:**
- `AircraftModel "*" --> "1" Manufacturer : manufacturedBy` — mandatory "1".
- `AircraftModel "*" --> "1..*" EngineModel : certifies` — verb from requirements: "certified engine model." At least one required (US055). Managed via AircraftVariant which holds EngineModelID only.

| Concept | Classification | Justification |
|---|---|---|
| **AircraftModel** | Entity (root) | Independent lifecycle. Multiple invariants. Referenced by Aircraft and Pilot but not owned by them. |
| **ModelID** | Value Object | Technical identifier for persistence and cross-aggregate references. Business identity is name+manufacturer (US055 uniqueness rule) — verified by controller via repository. Follows EAPLI pattern of identity VO per root. |
| **AircraftType** | Value Object (enum) | Section 3.2: "Type (passenger, cargo, mixed)" — fixed list. No distinct structure per value. Referenced by association (-->). |
| **AircraftWeights** | Value Object | Section 3.2: emptyWeight, MTOW, MZFW, maxFuelCapacity. Grouped because: (1) share same unit; (2) joint invariant MTOW > MZFW > emptyWeight, all positive. VO is information expert for this multi-attribute invariant. |
| **AircraftPerformance** | Value Object | Section 3.2: serviceCeiling, cruiseSpeed. US055: "maximum range." Grouped as cohesive performance parameters. Each has its own unit (metres/feet, knots/km/h, nautical miles/km) — a shared `unit` attribute would be ambiguous. Note in diagram clarifies units are defined at implementation time. In code: sub-VOs or separate value+unit pairs per attribute. |
| **AerodynamicCoefficients** | Value Object | Section 3.3: wingArea (A), dragCoefficient (Cd), liftCoefficient (Cl). Always used together in L = Cl×A×ρv²/2 and D = Cd×A×ρv²/2. Grouping reflects physical cohesion — no meaning in isolation. |
| **AircraftVariant** | Entity (internal) | US057: "combinations of model and engine configuration." Has local identity — each combination individually identifiable, can be added/removed independently. Not a VO because it can change. Holds EngineModelID only — cross-aggregate by ID, respecting DDD boundary rules. |

---

## AirTransportCompany Aggregate

**Why an aggregate:** Independent lifecycle — registered (US060). Referenced by Aircraft, Collaborator, FlightRoute but not owned by any.

**Cross-aggregate associations (incoming):**
- `Aircraft "*" --> "1" AirTransportCompany : ownedBy` — aircraft always owned by a company.
- `Collaborator "*" --> "0..1" AirTransportCompany : employedBy` — only ATCCollaborator. Known DDD hierarchy limitation.
- `FlightRoute "*" --> "1" AirTransportCompany : ownedBy` — client: "A route is owned by an ATC."

| Concept | Classification | Justification |
|---|---|---|
| **AirTransportCompany** | Entity (root) | Independent lifecycle. Referenced by multiple aggregates but not owned by any. |
| **CompanyName** | Value Object | US060: company name must be unique. Uniqueness business rule — plain string cannot enforce. VO is information expert. May also have format constraints (non-empty). |
| **IATACode_ATC** | Value Object | US060: "IATA (2 letters)" — format rule (2 letters) and uniqueness. Same class as IATACode in Airport — duplicated in diagram for presentation. |
| **ICAOCode_ATC** | Value Object | US060: "ICAO code (2-3 letters)" — format rule (2-3 letters) and uniqueness. Same class as ICAOCode in Airport. |

---

## AirControlArea Aggregate

**Why an aggregate:** Independent lifecycle — registered (US050). Has own invariants: AreaCode unique, boundaries valid, areas cannot overlap (client clarification).

**Cross-aggregate associations (incoming):**
- `Airport "*" --> "1" AirControlArea : locatedIn` — US052: "exactly one air control area." Mandatory "1". Client: "Can have multiple airports" — "*" on Airport side.
- `Collaborator "*" --> "0..1" AirControlArea : worksFor` — FCO and WeatherPerson only. Known DDD hierarchy limitation — association on root, in code only FCO/WeatherPerson populate it.
- `WeatherData "*" --> "1" AirControlArea : registeredFor` — US041: "for a specific air control area." Mandatory "1".
- `Simulation "*" --> "1" AirControlArea : covers` — US100: "geographic area" parameter. Mandatory "1".

**Known DDD hierarchy limitation — employedBy and worksFor:**
The associations `Collaborator --> AirTransportCompany : employedBy` and `Collaborator --> AirControlArea : worksFor` are on the root because internal entities cannot cross aggregate boundaries. Semantically: ATCCollaborator always belongs to a company (never an area); FCO/WeatherPerson always belong to an area (never a company). The 0..1 on both reflects the representation limitation, documented via notes on the diagram. `note on link` conflicts with association labels in PlantUML — documented via `note right of Collaborator` instead.

| Concept | Classification | Justification |
|---|---|---|
| **AirControlArea** | Entity (root) | Independent lifecycle. Invariants: AreaCode unique, boundaries valid, areas cannot overlap. Referenced by multiple aggregates. |
| **AreaCode** | Value Object | US050: "area code must be unique in the system." Uniqueness rule — plain string cannot enforce. VO is information expert. |
| **Coordinates_ACA** | Value Object | US050: "geographic boundaries must be valid." Client: "for sake of simplicity, a rectangle." Contains minLatitude, maxLatitude, minLongitude, maxLongitude. Business rules: lat [-90,90], lon [-180,180], min < max for each. Distinct from Coordinates_Apt (single point). |

---

## Airport Aggregate

**Why an aggregate:** Independent lifecycle — registered (US052). Own invariants: IATA/ICAO unique worldwide, belongs to exactly one AirControlArea.

**Cross-aggregate associations:**
- `Airport "*" --> "1" AirControlArea : locatedIn` — US052: "exactly one air control area." Mandatory "1".
- Incoming: `FlightRoute "*" --> "1" Airport : hasOrigin` and `hasDestination` — client: "A route has two endpoints."

| Concept | Classification | Justification |
|---|---|---|
| **Airport** | Entity (root) | Independent lifecycle. Invariants: IATA/ICAO unique worldwide, coordinates valid, belongs to exactly one AirControlArea. |
| `name` | Plain attribute | Section 3.2 lists it. No uniqueness rule — two airports can share a name. No format rule. Plain string sufficient. |
| `town` | Plain attribute | Section 3.2 lists it. No business rule. Plain string sufficient. |
| `country` | Plain attribute | Section 3.2 lists it. No format or validation rule. Plain string sufficient. |
| **IATACode** | Value Object | US052: "unique worldwide." Format rule (3 letters) and uniqueness. Plain string cannot enforce either. Same class as IATACode_ATC — duplicated in diagram for presentation. |
| **ICAOCode** | Value Object | US052: "unique worldwide." Format rule (4 letters for airports) and uniqueness. Same class as ICAOCode_ATC. |
| **Coordinates_Apt** | Value Object | US052: "location coordinates that must be valid." Has latitude and longitude. Business rules: lat [-90,90], lon [-180,180]. Represents a single geographic point — distinct from Coordinates_ACA (rectangle). |
| **Elevation** | Value Object | US052: "elevation in meters above sea level." Has `value` and `unit` — unit is inseparable from value. Business rule: non-negative. Plain number insufficient. |

---

## Aircraft Aggregate

**Why a separate aggregate from AircraftModel:** AircraftModel = abstract type. Aircraft = specific physical instance with own registration number. Separate because: (1) independent lifecycles; (2) different invariants; (3) different actors; (4) N Aircraft share 1 AircraftModel — bundling violates one use case = one aggregate.

**Why an aggregate:** Independent lifecycle — added to fleet (US070), decommissioned (US071). Own invariants: registration number unique, total seats ≤ model capacity, decommissioned aircraft cannot have flights.

**Cross-aggregate associations:**
- `Aircraft "*" --> "1" AircraftModel : ofModel` — section 3.2: "aircraft of a specific model." Mandatory "1".
- `Aircraft "*" --> "1" AirTransportCompany : ownedBy` — section 3.2: "Company" listed as attribute. Mandatory "1".
- Incoming: `Flight "*" --> "1" Aircraft : uses` — US080.

| Concept | Classification | Justification |
|---|---|---|
| **Aircraft** | Entity (root) | Independent lifecycle. Invariants: registration unique, seats ≤ model capacity, decommissioned cannot have flights. |
| **RegistrationNumber** | Value Object | US070: "unique worldwide" and "registered in a country (may not be company's home country)." Two attributes: `number` + `registrationCountry` — cohesive, inseparable. Uniqueness rule. Plain string cannot represent both or enforce uniqueness. |
| **CabinConfiguration** | Value Object | Section 3.2: "number of seats in each class." US070: "total seats cannot exceed model capacity." Container for 1..* SeatClass VOs. No attributes of its own — role is to group seat classes and enforce capacity invariant. Information expert for sum(seats) ≤ model capacity. |
| **SeatClass** | Value Object | Represents one cabin class. Has `className` and `numberOfSeats` — inseparable: a seat count without a class name has no meaning. Cannot exist without CabinConfiguration. Client: "The requirements document mentions cabin configuration" — class names are generic, filled at runtime. |
| `numberOfFlightCrewMembers` | Plain attribute | Section 3.2: "Number of elements of the flight crew." Simple count. No format rule, no uniqueness rule. Positive integer sufficient. |
| **OperationalStatus** | Value Object (enum) | US070/US071: fixed values (active, decommissioned). No distinct structure per value. Referenced by association (-->), not composed — Aircraft does not create or destroy the enum. |

---

## Collaborator Aggregate

**Why an aggregate:** Independent lifecycle — registered (US061), disabled (US064). Invariants: SecurityClearance must be active, SkillsAssessment within 5 years. Four specialisations share common attributes — always manipulated together.

**Why abstract:** A generic Collaborator never exists — always a concrete specialisation. Requirements always identify collaborators by specific role. Abstract class enforces this — cannot be instantiated directly.

**Why Admin and Backoffice Operator not modelled:** Internal actors managed by EAPLI framework. No domain-specific business rules beyond authentication/authorisation — framework responsibility.

**Why email and phoneNumber not in Collaborator:** Section 3.1.1 mentions them in context of SystemUser. Duplicating in Collaborator would violate framework boundary. Referenced in SystemUser note.

**Cross-aggregate associations:**
- `Collaborator "*" --> "0..1" AirTransportCompany : employedBy` — ATCCollaborator only. 0..1 — known DDD hierarchy limitation. In code only ATCCollaborator populates this.
- `Collaborator "*" --> "0..1" AirControlArea : worksFor` — FCO and WeatherPerson only. Same limitation. Semantically: every collaborator belongs to either a company or an area — never both, never neither. The 0..1 on both is a representation limitation documented via notes.

| Concept | Classification | Justification |
|---|---|---|
| **Collaborator** | Entity (root, abstract) | Independent lifecycle. Abstract — never instantiated directly. Invariants: SecurityClearance active, SkillsAssessment within 5 years. |
| **ATCCollaborator** | Entity (internal) | Specialisation for air transport companies. No attributes or invariants beyond inherited. No independent lifecycle. |
| **FlightControlOperator** | Entity (internal) | Specialisation for air control areas. No additional attributes. No independent lifecycle. |
| **WeatherPerson** | Entity (internal) | Specialisation for air control areas. No additional attributes. No independent lifecycle. |
| **SystemUser** | Entity (framework boundary) | EAPLI framework manages login, password, email, phoneNumber. Referenced by association (-->) not composition — has own lifecycle managed by framework. |
| `name` | Plain attribute | Section 3.1.3: "name" mentioned. No uniqueness rule, no format rule. Plain string sufficient — no business rule makes it inadequate. |
| `position` | Plain attribute | Section 3.1.3: "position" mentioned. Represents organisational title (e.g. "Senior Operations Manager") — distinct from collaborator type expressed by inheritance hierarchy. No format or uniqueness rule. Both coexist: inheritance expresses what kind of collaborator; position describes their title within the organisation. |
| **SecurityClearance** | Value Object | Section 3.1.1: "active security clearance that automatically expires at a given date." Has `expiryDate`. Business rule: must have valid future expiry date; system checks if expired. VO is information expert for expiry validation. |
| **SkillsAssessment** | Value Object | Section 3.1.1: "periodic (per regulations 5 years) skills assessment." Has `assessmentDate`. Business rule: must be within last 5 years. VO encapsulates this validation — information expert. |

---

## Pilot Aggregate

**Why a separate aggregate:** Separated from Collaborator because: (1) independent lifecycle — US075 creates, US076 lists, US077 deactivates; (2) specific invariant — US077: "cannot be removed if they have flights assigned"; (3) directly referenced by Flight — if internal to Collaborator, Flight would reference Collaborator (semantically imprecise); (4) inherits common attributes from Collaborator via inheritance — no duplication. In Java/JPA: subclass that is root of its own aggregate.

**certifiedFor now on Pilot root:** Previously on Collaborator root with 0..* as known DDD limitation. Now that Pilot is a separate root: `Pilot "*" --> "1..*" AircraftModel : certifiedFor` — semantically precise. Multiplicity 1..* — US075: "one or more aircraft models."

**Cross-aggregate associations:**
- `Pilot "*" --> "1..*" AircraftModel : certifiedFor` — US075: "one or more." Mandatory 1..* — a pilot must be certified for at least one model.
- `Flight "*" --> "1" Pilot : assignedTo` — US080. References Pilot root directly — semantically correct now that Pilot is a separate aggregate.
- Inherits `employedBy AirTransportCompany` from Collaborator — client: "a pilot only works for an ATC at a time." 0..1.

| Concept | Classification | Justification |
|---|---|---|
| **Pilot** | Entity (root) | Separate aggregate: independent lifecycle (US075-077), specific invariant (US077), directly referenced by Flight. Inherits common attributes from Collaborator. No additional attributes — certifications are the cross-aggregate association. |

---

## FlightRoute Aggregate

**Why an aggregate:** Independent lifecycle — created (US073), deactivated (US074). Own invariants: RouteName unique and specific format, cannot be deactivated with planned flights after date. Referenced by Flight but not owned by it.

**Cross-aggregate associations:**
- `FlightRoute "*" --> "1" Airport : hasOrigin` — client: "A route has two endpoints." Mandatory "1".
- `FlightRoute "*" --> "1" Airport : hasDestination` — same. Two separate associations to same Airport aggregate.
- `FlightRoute "*" --> "1" AirTransportCompany : ownedBy` — client: "A route is owned by an ATC. Its ID includes the company ID (e.g. TP123)." Mandatory "1".
- Incoming: `Flight "*" --> "1" FlightRoute : instantiates` — section 3.4.

**Route ownership and operations:** ATCCollaborator acts on behalf of their company (US073, US074). Operations modify only FlightRoute — one use case, one aggregate. Controller verifies ATCCollaborator belongs to the company that owns the route — cross-aggregate validation at application layer, not by root. RouteName prefix (2 letters) must match company IATA code — verified by controller on creation.

| Concept | Classification | Justification |
|---|---|---|
| **FlightRoute** | Entity (root) | Independent lifecycle. Invariants: RouteName unique with format, cannot deactivate with planned flights. |
| **RouteName** | Value Object | US073: "2 letters (company initials) + up to 4 numbers (e.g. TP123). Must be unique." Format rule and uniqueness rule — plain string cannot enforce either. VO is information expert. |
| `deactivationDate` | Plain attribute | US074: "deactivate from a given date onwards." Simple date value. No format rule, no uniqueness, no joint invariant. Plain attribute sufficient. |

---

## Flight Aggregate

**Why an aggregate:** Independent lifecycle — instantiation of a route (section 3.4). Own invariants: FlightDesignator unique, aircraft and pilot must belong to route's company. Referenced by Simulation but not owned by it.

**On FlightPlan — entity not VO:** Angelo initially suggested "likely a value object." However the full US flow contradicts immutability: US080 creates FlightPlan in `draft`; US082 adds weather data to the *existing* FlightPlan; US085 validates it — status changes draft→validated. A VO is immutable by definition — cannot change state or receive additional data after creation. Therefore FlightPlan is an **internal entity** of the Flight aggregate, following formal US behaviour over Angelo's informal suggestion. Angelo confirmed: "one may have more than a flight plan for a flight" — if rejected, a new one is submitted. Flight has 1..* FlightPlans.

**On FlightType and DepartureSchedule coexistence:** FlightType is listed explicitly as a Flight attribute in section 3.2. It is maintained alongside the DepartureSchedule hierarchy because: (1) explicit business information — can be consulted independently of the schedule; (2) enables a consistency invariant: if FlightType = REGULAR then DepartureSchedule must be RegularSchedule, and vice versa. The Flight root enforces this at construction time. In code: `private FlightType flightType` and `private DepartureSchedule schedule` coexist, with root enforcing consistency.

**Cross-aggregate associations:**
- `Flight "*" --> "1" FlightRoute : instantiates` — section 3.4: "a flight is an instantiation of a route." Mandatory "1".
- `Flight "*" --> "1" Aircraft : uses` — US080: "I must add the aircraft." Rises to root because FlightPlan is internal entity — internal entities cannot cross aggregate boundaries.
- `Flight "*" --> "1" Pilot : assignedTo` — US080: "I must add... pilot." Same reasoning. References Pilot root directly because Pilot is a separate aggregate.
- `Flight "*" --> "0..1" WeatherData : usesWeather` — US082: weather added to FlightPlan. Since FlightPlan is internal entity, reference rises to Flight root. 0..1 — flight may not have weather yet when created in draft. "*" on Flight side — many flights can reference same WeatherData.

| Concept | Classification | Justification |
|---|---|---|
| **Flight** | Entity (root) | Independent lifecycle. Invariants: FlightDesignator unique, aircraft and pilot of route's company. Referenced by Simulation. |
| **FlightDesignator** | Value Object | Section 3.2: format xxn(n)(n)(n)(a), unique. Format rule and uniqueness rule. Has `airlineDesignator`, `flightNumber`, `operationalSuffix`. Plain string cannot enforce either rule. |
| **FlightType** | Value Object (enum) | Section 3.2: fixed values (regular, charter). Maintained alongside DepartureSchedule hierarchy — explicit business attribute enabling consistency invariant with schedule type. Referenced by association (-->). |
| **DepartureSchedule** | Value Object (abstract) | Section 3.2: "Departure day (or days of the week for regular flights and actual date for a charter) and time." Two cases with structurally different data — VO hierarchy. Abstract base has no attributes — exists solely to allow Flight one polymorphic composition. In code: `private DepartureSchedule schedule` holds RegularSchedule or CharterSchedule at runtime. |
| **RegularSchedule** | Value Object | Client: "Regular flights have a recurring schedule — day of week and time for each instance." Contains 1..* ScheduleEntry. No attributes of its own — role is to contain entries and enforce 1..* constraint. In code: `private final List<ScheduleEntry> entries`. |
| **ScheduleEntry** | Value Object | Client: "Monday 12:00; Tuesday 12:30; Thursday 11:30." One entry = one (dayOfWeek, departureTime) pair — inseparable. In code: `private final DayOfWeek dayOfWeek; private final LocalTime departureTime` — immutable. Plain string cannot represent two cohesive attributes. |
| **CharterSchedule** | Value Object | Client: "Charter flights will have only a single instance." Has `departureDate` and `departureTime` — date and time together form the concept, inseparable. Plain date insufficient. |
| `scheduledArrivalTime` | Plain attribute | Section 3.2: "Scheduled arrival (local time)." Simple time value. No invariant requiring grouping. Java LocalTime handles validity. Plain attribute sufficient. |
| **FlightPlan** | Entity (internal) | US080 creates in draft; US082 adds weather; US085 validates — status changes. Has lifecycle and changes after creation — cannot be VO (immutable by definition). Angelo's "likely a VO" was informal — formal US behaviour takes precedence. Flight has 1..* FlightPlans — if rejected, new one submitted. |
| **FlightPlanStatus** | Value Object (enum) | US080: "status is set to 'draft' until validated." Fixed values (draft, validated). Referenced by association (-->). |
| **FuelQuantity** | Value Object | US080: "fuel quantity." Has `value` and `unit` — one value, one unit, inseparable. Business rule: must be strictly positive. Consistent with other single-measurement VOs (Power, Thrust, TSFC, Elevation, SafetyThreshold) — all follow value+unit pattern. Plain number insufficient. |

**On value+unit consistency:** VOs with a single measurement (FuelQuantity, Power, Thrust, TSFC, Elevation, SafetyThreshold) explicitly carry `value` and `unit` — one value, one unit, unambiguous. AircraftPerformance groups three measurements each with a different unit — a shared `unit` would be ambiguous. Uses plain attributes with a note; in code implemented as sub-VOs or separate value+unit pairs.

---

## WeatherData Aggregate

**Why an aggregate:** Independent lifecycle — registered (US041), imported (US042), consulted (US043). Referenced by Simulation but not owned by it.

**WeatherData does not link to Collaborator:** No US states the need to track which WeatherPerson registered each record. sourceProvider covers the external data source. WeatherPerson is the actor — application layer concern, not domain model.

**Cross-aggregate associations:**
- `WeatherData "*" --> "1" AirControlArea : registeredFor` — US041: "for a specific air control area." Mandatory "1".
- Incoming: `Flight "*" --> "0..1" WeatherData : usesWeather` — US082. 0..1 — flight may not have weather yet.
- Incoming: `Simulation "*" --> "*" WeatherData : uses` — US100: "weather conditions" parameter.

**Why Coordinates_WD is inside WindCondition:** Client: "Weather conditions are not the same everywhere inside an air control area." Each WindCondition reading is localised to a specific point. If coordinates were in WeatherData, only one location per record — contradicts client. Each reading has its own location.

| Concept | Classification | Justification |
|---|---|---|
| **WeatherData** | Entity (root) | Independent lifecycle. Invariants: belongs to one area, recordedDateTime valid. |
| `recordedDateTime` | Plain attribute | US043: "consult in a given day" — needed for day filtering. Timestamp (date + time). No business rule beyond being valid — Java LocalDateTime handles it. |
| `sourceProvider` | Plain attribute | US042: "multiple external weather service providers." Identifies external source. No format or uniqueness rule. Plain string sufficient. |
| **WindCondition** | Value Object | US041: "register weather data." Client: "conditions not the same everywhere in area." Section 3.2 (in segment context): "wind direction (angle) relative to North and speed (m/s)." Has `directionAngle` and `speed` — cohesive physical concept. Immutable — a wind reading is a snapshot. Plain number cannot represent both. |
| **Coordinates_WD** | Value Object | Represents the geographic point where the wind reading was taken. Composed inside WindCondition — each reading has its own location (client: conditions vary within area). Business rules: lat [-90,90], lon [-180,180]. Distinct from Coordinates_ACA (rectangle) and Coordinates_Apt (airport point). |

---

## Simulation Aggregate

**Why an aggregate:** Independent lifecycle — created with parameters, sends to C module, receives report. Client: "You have to send information to run the simulation and receive feedback/results. This information must be in the system."

**SimulationReport as VO:** Received from C module — immutable, final document. Same reasoning as originally suggested for FlightPlan by Angelo: "it's final." Unlike FlightPlan, SimulationReport does not change after being received — no US adds data to it after creation. Therefore it is a VO. Contains `totalFlights` and references `ValidationResult`.

**ValidationResult in SimulationReport VO:** US109/US111: "overall validation result (pass/fail)" — explicitly required. Although SimulationReport is a VO, it can reference an enum by association (-->): enums are referenced values, not owned parts. A VO referencing an enum is valid — both are immutable.

**Cross-aggregate associations:**
- `Simulation "*" --> "1" AirControlArea : covers` — US100: "geographic area." Mandatory "1".
- `Simulation "*" --> "1..*" Flight : includes` — US100: "included flights." At least one required — 1..*.
- `Simulation "*" --> "*" WeatherData : uses` — US100: "weather conditions." Optional — *.

| Concept | Classification | Justification |
|---|---|---|
| **Simulation** | Entity (root) | Independent lifecycle. Parameters sent to C module; report received back. |
| **SimulationTimeRange** | Value Object | US100: "time range" parameter. Has `startDateTime` and `endDateTime`. Business invariant: start < end — two-attribute invariant justifies VO. VO is information expert for this constraint. |
| **SafetyThreshold** | Value Object | US100: "safety thresholds" parameter. Has `value` and `unit` — unit inseparable from value. Business rule: must be positive. Plain number insufficient. |
| **SimulationReport** | Value Object | US109/US111: total flights, execution statuses, pass/fail result. Received from C module — immutable, final document. Does not change after being received (unlike FlightPlan). Contains `totalFlights` and references ValidationResult. |
| **ValidationResult** | Value Object (enum) | US109/US111: "overall validation result (pass/fail)." Fixed values (passed, failed). Referenced by association (-->) from SimulationReport VO — valid: both are immutable. |

---

## Summary Table

| Concept | Classification | Key Reason |
|---|---|---|
| Manufacturer | Entity (root) | Independent lifecycle |
| EngineModel | Entity (root) | Independent lifecycle, invariants |
| EngineName | VO | Uniqueness rule (name + manufacturer) |
| Power | VO | value + unit + positivity rule |
| Thrust | VO | value + unit + speedReference, 2 instances |
| TSFC | VO | value + unit + positivity rule |
| MotorizationType | VO (enum) | Fixed domain values |
| AircraftModel | Entity (root) | Independent lifecycle, multiple invariants |
| ModelID | VO | Technical identity VO, business uniqueness via name+manufacturer |
| AircraftType | VO (enum) | Fixed domain values |
| AircraftWeights | VO | 4 attributes, joint invariant MTOW > MZFW > emptyWeight |
| AircraftPerformance | VO | Cohesive performance parameters, distinct units per attribute |
| AerodynamicCoefficients | VO | 3 attributes always used together in physics formulas |
| AircraftVariant | Entity (internal) | Local identity, individually added/removed |
| Aircraft | Entity (root) | Independent lifecycle, distinct from AircraftModel |
| RegistrationNumber | VO | Uniqueness rule + registrationCountry |
| CabinConfiguration | VO | Container for SeatClass, capacity invariant |
| SeatClass | VO | className + numberOfSeats inseparable |
| OperationalStatus | VO (enum) | Fixed domain values |
| AirControlArea | Entity (root) | Independent lifecycle, invariants |
| AreaCode | VO | Uniqueness rule |
| Coordinates_ACA | VO | 4 values (rectangle) + validation rules |
| Airport | Entity (root) | Independent lifecycle, invariants |
| IATACode | VO | Format rule + uniqueness rule |
| ICAOCode | VO | Format rule + uniqueness rule |
| Coordinates_Apt | VO | 2 values (point) + validation rules |
| Elevation | VO | value + unit + positivity rule |
| AirTransportCompany | Entity (root) | Independent lifecycle |
| CompanyName | VO | Uniqueness rule + format constraints |
| IATACode_ATC | VO | Format rule (2 letters) + uniqueness |
| ICAOCode_ATC | VO | Format rule (2-3 letters) + uniqueness |
| Collaborator | Entity (root, abstract) | Independent lifecycle, never instantiated directly |
| ATCCollaborator | Entity (internal) | Specialisation, no independent lifecycle |
| FlightControlOperator | Entity (internal) | Specialisation, no independent lifecycle |
| WeatherPerson | Entity (internal) | Specialisation, no independent lifecycle |
| SecurityClearance | VO | expiryDate + expiry business rule |
| SkillsAssessment | VO | assessmentDate + 5-year periodic rule |
| Pilot | Entity (root) | Independent lifecycle (US075-077), own invariant (US077), referenced by Flight |
| FlightRoute | Entity (root) | Independent lifecycle, format+uniqueness invariant |
| RouteName | VO | Format rule (2 letters + 4 digits) + uniqueness |
| Flight | Entity (root) | Independent lifecycle, instantiation of route |
| FlightDesignator | VO | Format rule xxn(n)(n)(n)(a) + uniqueness |
| FlightType | VO (enum) | Fixed domain values + consistency invariant with DepartureSchedule |
| DepartureSchedule | VO (abstract) | Polymorphic composition in Flight |
| RegularSchedule | VO | Container for 1..* ScheduleEntry |
| ScheduleEntry | VO | dayOfWeek + departureTime inseparable |
| CharterSchedule | VO | departureDate + departureTime inseparable |
| FlightPlan | Entity (internal) | Lifecycle: draft→validated, receives weather (US082) |
| FlightPlanStatus | VO (enum) | Fixed domain values |
| FuelQuantity | VO | value + unit + positivity rule |
| WeatherData | Entity (root) | Independent lifecycle |
| WindCondition | VO | directionAngle + speed, immutable snapshot |
| Coordinates_WD | VO | 2 values (point) + validation rules, inside WindCondition |
| Simulation | Entity (root) | Independent lifecycle |
| SimulationTimeRange | VO | startDateTime + endDateTime + invariant start < end |
| SafetyThreshold | VO | value + unit + positivity rule |
| SimulationReport | VO | Final immutable document from C module |
| ValidationResult | VO (enum) | Fixed domain values, referenced by SimulationReport VO |