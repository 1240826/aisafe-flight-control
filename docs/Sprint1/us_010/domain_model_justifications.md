# Domain Model Justifications 

## Classification Criteria

**Entity:** has its own business identity and independent lifecycle. 

**Value Object (VO):** characterises another concept. Immutable. Follows the **Information Expert** principle: encapsulates and validates its own data. A concept must be a VO (not a plain attribute) when:
- It has a business rule (format, uniqueness, validation, invariant), OR
- A plain string/number is insufficient — the concept requires multiple cohesive attributes or a specific type

**Plain attribute:** a simple primitive value with no business rules beyond basic type validity. A plain string, integer, or date that a primitive Java type handles adequately.

**Enum:** a fixed set of domain values with no distinct data structure per value. Referenced by association (`-->`), never composed (`*--`). 

---

## Manufacturer

| Concept | Classification | Justification |
|---|---|---|
| **Manufacturer** | Entity (root) | Has its own business identity — registered independently and referenced by other aggregates but not owned by them. |
| `name` | Plain attribute | Section 3.1.2: "only basic information (name and country?)". No uniqueness rule, no format rule. A plain string is sufficient. |
| `country` | Plain attribute | Section 3.1.2: question mark suggests optional. No format or validation rule. Plain string sufficient. |

---

## EngineModel

| Concept | Classification | Justification |
|---|---|---|
| **EngineModel** | Entity (root) | Has its own business identity and independent lifecycle — registered (US056). Tracked by identity, not attributes. |
| **EngineName** | Value Object | US056: "name and manufacturer combination must be unique." Uniqueness rule — plain string cannot enforce a two-attribute uniqueness constraint. |
| `fuelType` | Plain attribute | US056 mentions fuel as information but specifies no format, uniqueness, or validation rule. Plain string sufficient. |
| **Power** | Value Object | Has `value` and `unit` — unit is inseparable from value; a number without a unit has no physical meaning. Business rule: must be positive. Plain number insufficient. |
| **Thrust** | Value Object | Has `value`, `unit`, and `speedReference` — three cohesive physical attributes representing the same concept at a specific speed (static or cruise). Section 3.3. Business rule: must be positive. Plain number cannot represent this. |
| **TSFC** | Value Object | Thrust Specific Fuel Consumption. Has `value` and `unit`. Section 3.3: "can be expressed in other units" — the unit is not fixed. Business rule: must be positive. Plain number insufficient. |
| **MotorizationType** | Enum | Section 3.2: fixed list (turboprop, turbofan, turbojet, ramjet, electricPropeller). |

---

## AircraftModel

| Concept | Classification | Justification |
|---|---|---|
| **AircraftModel** | Entity (root) | Has its own business identity and independent lifecycle — registered, modified, referenced by Aircraft and Pilot. AircraftModel = abstract type; Aircraft = physical instance. Separate because different actors, different invariants, and N Aircraft share 1 AircraftModel. |
| **ModelID** | Value Object | Technical identifier for persistence and cross-aggregate references. Business uniqueness rule: name + manufacturer must be unique (US055).|
| **AircraftType** | Enum | Section 3.2: fixed list (passenger, cargo, mixed). |
| **AircraftWeights** | Value Object | Groups emptyWeight, MTOW, MZFW, maxFuelCapacity (section 3.2). Grouped because: (1) they share the same unit; (2) they have a joint invariant — MTOW > MZFW > emptyWeight, all positive. A VO is the information expert for a multi-attribute invariant. |
| **AircraftPerformance** | Value Object | Groups serviceCeiling, cruiseSpeed, maximumRange (section 3.2, US055). Cohesive performance parameters. Each has its own unit (metres/feet, knots/km/h, nautical miles/km) — a shared `unit` attribute would be ambiguous. In code: sub-VOs or separate value+unit pairs per attribute. |
| **AerodynamicCoefficients** | Value Object | Groups wingArea (A), dragCoefficient (Cd), liftCoefficient (Cl). Section 3.3: always used together in the lift and drag formulas: L = Cl×A×ρv²/2 and D = Cd×A×ρv²/2. No physical meaning in isolation. |
| **AircraftVariant** | Entity (internal) | US057: "combinations of model and engine configuration." Has local identity within the aggregate — each combination is individually identifiable and can be added or removed independently. It can change (engines can be swapped) — cannot be VO (immutable by definition). Holds EngineModelID only, respecting DDD boundary rules. |

---

## AirTransportCompany

| Concept | Classification | Justification |
|---|---|---|
| **AirTransportCompany** | Entity (root) | Has its own business identity and independent lifecycle — registered (US060). Referenced by Aircraft, Collaborator, FlightRoute but not owned by them. |
| **CompanyName** | Value Object | US060: company name must be unique. Uniqueness rule — plain string cannot enforce. VO is information expert. |
| **IATACode_ATC** | Value Object | US060: "IATA (2 letters)" — format rule (exactly 2 letters) and uniqueness worldwide. Plain string cannot enforce either. Same class in code as IATACode in Airport — duplicated in diagram for presentation. |
| **ICAOCode_ATC** | Value Object | US060: "ICAO code (2-3 letters)" — format rule and uniqueness worldwide. Same class in code as ICAOCode in Airport. |

---

## AirControlArea

| Concept | Classification | Justification |
|---|---|---|
| **AirControlArea** | Entity (root) | Has its own business identity and independent lifecycle — registered (US050). Referenced by multiple aggregates but not owned by any. |
| **AreaCode** | Value Object | US050: "area code must be unique in the system." Uniqueness rule — plain string cannot enforce. VO is information expert. |
| **Coordinates_ACA** | Value Object | Represents a rectangular geographic boundary. Has four attributes: minLatitude, maxLatitude, minLongitude, maxLongitude. Client: "for sake of simplicity, a rectangle." Business rules: lat ∈ [-90,90], lon ∈ [-180,180], min < max for each. A plain number cannot represent a rectangle. Distinct from Coordinates_Apt and Coordinates_WD (single points). |

---

## Airport

| Concept | Classification | Justification |
|---|---|---|
| **Airport** | Entity (root) | Has its own business identity and independent lifecycle — registered (US052). Referenced by FlightRoute but not owned by it. |
| `name` | Plain attribute | Section 3.2 lists it. No uniqueness rule — two airports can share a name. No format rule. Plain string sufficient. |
| `town` | Plain attribute | Section 3.2 lists it. No business rule. Plain string sufficient. |
| `country` | Plain attribute | Section 3.2 lists it. No format or validation rule. Plain string sufficient. |
| **IATACode** | Value Object | US052: 3-letter format rule and uniqueness worldwide. Plain string cannot enforce either. Same class in code as IATACode_ATC — duplicated in diagram for presentation. |
| **ICAOCode** | Value Object | US052: 4-letter format rule and uniqueness worldwide. Same class in code as ICAOCode_ATC. |
| **Coordinates_Apt** | Value Object | US052: "location coordinates that must be valid." Has latitude and longitude (single point). Business rules: lat ∈ [-90,90], lon ∈ [-180,180]. Same class in code as Coordinates_WD — duplicated in diagram for presentation. |
| **Elevation** | Value Object | US052: "elevation in meters above sea level." Has `value` and `unit` — unit is inseparable from value. Business rule: non-negative. Plain number insufficient. |

---

## Aircraft

| Concept | Classification | Justification |
|---|---|---|
| **Aircraft** | Entity (root) | Has its own business identity and independent lifecycle — added to fleet (US070), decommissioned (US071). AircraftModel = abstract type; Aircraft = physical instance with its own registration, cabin, and status. |
| **RegistrationNumber** | Value Object | US070: "unique worldwide" and "registered in a country (it may not be the company's home country)." Two cohesive attributes: `number` + `registrationCountry` — inseparable. Uniqueness rule. Plain string cannot represent both attributes or enforce uniqueness. |
| **CabinConfiguration** | Value Object | Section 3.2, US070: "total seats cannot exceed model capacity." Container for 1..* SeatClass VOs. Information expert for the sum(seats) ≤ model capacity invariant. |
| **SeatClass** | Value Object | Represents one cabin class. Has `className` and `numberOfSeats` — inseparable: a seat count without a class name has no meaning, and vice versa. Cannot exist without CabinConfiguration. |
| `numberOfFlightCrewMembers` | Plain attribute | Section 3.2: simple count. No format rule, no uniqueness rule. A positive integer is sufficient. |
| **OperationalStatus** | Enum | US070/US071: fixed values (active, decommissioned). No distinct data structure per value. Referenced by association (`-->`). |

---

## Collaborator

| Concept | Classification | Justification |
|---|---|---|
| **Collaborator** | Entity (root, abstract) | Has its own business identity and independent lifecycle — registered (US061), disabled (US064). Abstract: a generic collaborator never exists — always a concrete specialisation. |
| **ATCCollaborator** | Entity (internal) | Specialisation of Collaborator for AirTransportCompany. No additional attributes or invariants beyond inherited. No independent lifecycle — created and deleted as part of the Collaborator aggregate. |
| **FlightControlOperator** | Entity (internal) | Specialisation of Collaborator for AirControlArea. No additional attributes. No independent lifecycle. |
| **WeatherPerson** | Entity (internal) | Specialisation of Collaborator for AirControlArea. No additional attributes. No independent lifecycle. |
| **SystemUser** | Entity (framework boundary) | EAPLI framework manages login, email, name, phoneNumber. Referenced by association (`-->`) not composition — has its own lifecycle managed by the framework, not by the Collaborator aggregate. |
| `name` | Plain attribute | Section 3.1.3: mentioned. No uniqueness rule, no format rule. Plain string sufficient. |
| `position` | Plain attribute | Section 3.1.3: mentioned. Represents organisational title — distinct from the collaborator type expressed by the inheritance hierarchy. No business rule. Plain string sufficient. |
| **SecurityClearance** | Value Object | Section 3.1.1: "active security clearance that automatically expires at a given date." Has `expiryDate`. Business rule: must not be expired. VO is information expert for expiry validation. |
| **SkillsAssessment** | Value Object | Section 3.1.1: "periodic (per regulations 5 years) skills assessment." Has `assessmentDate`. Business rule: must be within the last 5 years. VO encapsulates this validation. |

---

## Pilot

| Concept | Classification | Justification |
|---|---|---|
| **Pilot** | Entity (root) | Has its own business identity and independent lifecycle with specific use cases (US075, US076, US077). Separated from Collaborator: (1) own lifecycle; (2) own invariant — US077 cannot be deactivated with assigned flights; (3) directly referenced by Flight — referencing Collaborator would be semantically imprecise. Inherits common attributes from Collaborator via inheritance. No additional own attributes — certifications are the cross-aggregate association `certifiedFor`. |

---

## FlightRoute

| Concept | Classification | Justification |
|---|---|---|
| **FlightRoute** | Entity (root) | Has its own business identity and independent lifecycle — created (US073), deactivated (US074). Referenced by Flight but not owned by it. |
| **RouteName** | Value Object | US073: format "2 letters + up to 4 digits (e.g. TP123)" and uniqueness rule. Plain string cannot enforce either. VO is information expert for format and uniqueness validation. |
| `deactivationDate` | Plain attribute | US074: simple date value. No format rule, no uniqueness rule, no joint invariant requiring grouping. Java LocalDate handles validity. |

---

## Flight

| Concept | Classification | Justification |
|---|---|---|
| **Flight** | Entity (root) | Has its own business identity and independent lifecycle — an instantiation of a route (section 3.4). Referenced by Simulation but not owned by it. |
| **FlightDesignator** | Value Object | Section 3.2: format xxn(n)(n)(n)(a) and uniqueness rule. Has three cohesive attributes: `airlineDesignator`, `flightNumber`, `operationalSuffix`. Plain string cannot enforce format or uniqueness. |
| **FlightType** | Enum | Section 3.2: fixed values (regular, charter). Maintained alongside DepartureSchedule because: (1) explicit business attribute in the requirements; (2) enables a consistency invariant — FlightType must match DepartureSchedule specialisation. Referenced by association (`-->`). |
| **DepartureSchedule** | Value Object (abstract) | Section 3.2: two cases with structurally different data — a VO hierarchy rather than a single VO with optional fields. Abstract base has no attributes — exists solely to allow Flight one polymorphic composition (`private DepartureSchedule schedule`). |
| **RegularSchedule** | Value Object | Specialisation of DepartureSchedule for regular flights. Container for 1..* ScheduleEntry instances. Client: "day of the week and the time for each flight instance." No attributes of its own — role is to hold entries and enforce the 1..* constraint. |
| **ScheduleEntry** | Value Object | One (dayOfWeek, departureTime) pair. Client: "Monday 12:00; Tuesday 12:30; Thursday 11:30." Two inseparable attributes — a departure time without a day has no meaning. |
| **CharterSchedule** | Value Object | Specialisation of DepartureSchedule for charter flights. Has `departureDate` and `departureTime` — date and time together form the concept, inseparable. Client: "charter flights will have only a single instance." |
| `scheduledArrivalTime` | Plain attribute | Section 3.2: simple time value. No invariant requiring grouping with another attribute. Java LocalTime handles validity. |
| **FlightPlan** | Entity (internal) | US080 creates in `draft`; US082 adds weather data to the *existing* FlightPlan; US085 validates it — status changes. Has lifecycle and changes after creation — cannot be a VO (immutable by definition). Client initially suggested VO ("it's final") — formal US behaviour takes precedence. Flight has 1..* FlightPlans (client: "one may have more than a flight plan for a flight"). |
| **FlightPlanStatus** | Enum | US080: fixed values (draft, validated). No distinct data structure per value. Referenced by association (`-->`). |
| **FuelQuantity** | Value Object | US080: "fuel quantity." Has `value` and `unit` — unit is inseparable from value. Business rule: must be strictly positive. Consistent with other single-measurement VOs (Power, Thrust, TSFC, Elevation, SafetyThreshold). |

**Note on value+unit pattern:** VOs representing a single physical measurement (Power, Thrust, TSFC, FuelQuantity, Elevation, SafetyThreshold) carry explicit `value` and `unit` — one value, one unit, unambiguous. AircraftPerformance groups three measurements each with a different unit — a shared `unit` attribute would be ambiguous; each attribute implicitly carries its own unit defined at implementation time.

**Note on cross-aggregate references rising to the Flight root:** FlightPlan is an internal entity and cannot hold cross-aggregate references (DDD rule). Therefore the references to Aircraft (US080), Pilot (US080), and WeatherData (US082) all rise to the Flight root. WeatherData is `0..1` — optional at creation in draft, added later via US082.

---

## WeatherData

| Concept | Classification | Justification |
|---|---|---|
| **WeatherData** | Entity (root) | Has its own business identity and independent lifecycle — registered (US041), imported (US042), consulted (US043). Referenced by Simulation and Flight but not owned by either. |
| `recordedDateTime` | Plain attribute | US043: needed for filtering by day. Simple timestamp. No business rule beyond being a valid date/time — Java LocalDateTime handles validity. |
| `sourceProvider` | Plain attribute | US042: "multiple external weather service providers." Identifies the external source. No format or uniqueness rule. Plain string sufficient. |
| **WindCondition** | Value Object | Has `directionAngle` and `speed` — cohesive physical concept (section 3.2: "wind direction (angle) relative to North and speed (m/s)"). Immutable snapshot — a wind reading at a point in time. Two inseparable attributes. |
| **Coordinates_WD** | Value Object | Represents the geographic point where the wind reading was taken. Composed inside WindCondition — each reading has its own location. Client: "weather conditions are not the same everywhere inside an air control area." Business rules: lat ∈ [-90,90], lon ∈ [-180,180]. Same class in code as Coordinates_Apt — duplicated in diagram for presentation. |

---

## Simulation

| Concept | Classification | Justification                                                                                                                                                                                                                    |
|---|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Simulation** | Entity (root) | Has its own business identity and independent lifecycle — created by FlightControlOperator (US100), sends parameters to C module, receives report.                                                                               |
| **SimulationTimeRange** | Value Object | US100: "time range" parameter. Has `startDateTime` and `endDateTime` — two attributes with a joint invariant: start < end. The VO is the information expert for this two-attribute constraint.                                   |
| **SafetyThreshold** | Value Object | US100: "safety thresholds" parameter. Has `value` and `unit` — unit inseparable from value. Business rule: must be positive. Plain number insufficient.                                                                          |
| **SimulationReport** | Value Object | US109/US111: final document received from the C module. Immutable — no US adds data to it after it is received. Unlike FlightPlan, it does not change after creation. Contains `totalFlights` and references `ValidationResult`. |
| **ValidationResult** | Enum | US109/US111: fixed values (passed, failed). No distinct data structure per value. Referenced by association (`-->`) from SimulationReport V0.                                                                                    |

---

## Summary Table

| Concept | Classification | Key Reason |
|---|---|---|
| Manufacturer | Entity (root) | Own identity, independent lifecycle |
| `name`, `country` | Plain attribute | No business rule; plain string sufficient |
| EngineModel | Entity (root) | Own identity, independent lifecycle |
| EngineName | VO | Uniqueness rule (name + manufacturer) |
| `fuelType` | Plain attribute | No format or uniqueness rule |
| Power | VO | value + unit; must be positive |
| Thrust | VO | value + unit + speedReference; 2 instances; must be positive |
| TSFC | VO | value + unit; must be positive |
| MotorizationType | Enum | Fixed domain values |
| AircraftModel | Entity (root) | Own identity, independent lifecycle, separate from Aircraft |
| ModelID | VO | Technical identity VO; business uniqueness via name+manufacturer |
| AircraftType | Enum | Fixed domain values |
| AircraftWeights | VO | 4 attributes; joint invariant MTOW > MZFW > emptyWeight |
| AircraftPerformance | VO | 3 cohesive parameters; distinct units per attribute |
| AerodynamicCoefficients | VO | 3 attributes always used together in physics formulas |
| AircraftVariant | Entity (internal) | Local identity; individually added/removed; mutable |
| AirTransportCompany | Entity (root) | Own identity, independent lifecycle |
| CompanyName | VO | Uniqueness rule |
| IATACode_ATC | VO | Format rule (2 letters) + uniqueness |
| ICAOCode_ATC | VO | Format rule (2-3 letters) + uniqueness |
| AirControlArea | Entity (root) | Own identity, independent lifecycle |
| AreaCode | VO | Uniqueness rule |
| Coordinates_ACA | VO | 4 attributes (rectangle); validation rules; distinct from point VOs |
| Airport | Entity (root) | Own identity, independent lifecycle |
| `name`, `town`, `country` | Plain attribute | No business rule; plain string sufficient |
| IATACode | VO | Format rule (3 letters) + uniqueness |
| ICAOCode | VO | Format rule (4 letters) + uniqueness |
| Coordinates_Apt | VO | 2 attributes (point); validation rules |
| Elevation | VO | value + unit; non-negative rule |
| Aircraft | Entity (root) | Own identity, independent lifecycle; distinct from AircraftModel |
| RegistrationNumber | VO | 2 attributes (number + country); uniqueness rule |
| CabinConfiguration | VO | Container for SeatClass; capacity invariant |
| SeatClass | VO | className + numberOfSeats inseparable |
| `numberOfFlightCrewMembers` | Plain attribute | Simple count; no business rule |
| OperationalStatus | Enum | Fixed domain values |
| Collaborator | Entity (root, abstract) | Own identity, independent lifecycle; never instantiated directly |
| ATCCollaborator | Entity (internal) | Specialisation; no independent lifecycle |
| FlightControlOperator | Entity (internal) | Specialisation; no independent lifecycle |
| WeatherPerson | Entity (internal) | Specialisation; no independent lifecycle |
| SystemUser | Entity (framework boundary) | Own lifecycle managed by EAPLI framework |
| `name`, `position` | Plain attribute | No business rule; plain string sufficient |
| SecurityClearance | VO | expiryDate; must not be expired |
| SkillsAssessment | VO | assessmentDate; must be within 5 years |
| Pilot | Entity (root) | Own identity, independent lifecycle (US075-077); own invariant (US077) |
| FlightRoute | Entity (root) | Own identity, independent lifecycle |
| RouteName | VO | Format rule (2 letters + up to 4 digits) + uniqueness |
| `deactivationDate` | Plain attribute | Simple date; no joint invariant |
| Flight | Entity (root) | Own identity, independent lifecycle; instantiation of a route |
| FlightDesignator | VO | Format rule xxn(n)(n)(n)(a) + uniqueness |
| FlightType | Enum | Fixed domain values; enables consistency invariant with DepartureSchedule |
| DepartureSchedule | VO (abstract) | Polymorphic composition; structurally different cases |
| RegularSchedule | VO | Container for 1..* ScheduleEntry |
| ScheduleEntry | VO | dayOfWeek + departureTime inseparable |
| CharterSchedule | VO | departureDate + departureTime inseparable |
| `scheduledArrivalTime` | Plain attribute | Simple time value; no joint invariant |
| FlightPlan | Entity (internal) | Mutable lifecycle (draft→validated); receives weather after creation |
| FlightPlanStatus | Enum | Fixed domain values |
| FuelQuantity | VO | value + unit; must be strictly positive |
| WeatherData | Entity (root) | Own identity, independent lifecycle |
| `recordedDateTime`, `sourceProvider` | Plain attribute | No business rule beyond basic validity |
| WindCondition | VO | directionAngle + speed; immutable snapshot |
| Coordinates_WD | VO | 2 attributes (point); validation rules; inside WindCondition |
| Simulation | Entity (root) | Own identity, independent lifecycle |
| SimulationTimeRange | VO | startDateTime + endDateTime; invariant start < end |
| SafetyThreshold | VO | value + unit; must be positive |
| SimulationReport | VO | Immutable final document; no US changes it after creation |
| ValidationResult | Enum | Fixed domain values |