# Glossary — AISafe Flight Control System (Sprint 3)

This glossary defines all domain concepts present in the Sprint 3 domain model. Every class in the diagram has exactly one entry here. Each entry identifies whether the concept is an Entity, Value Object, or Enum.

| **Term** | **Type** | **Description** |
|:---|:---|:---|
| **AerodynamicCoefficients** | Value Object | Groups wing area (m²), drag coefficient (Cd), and lift coefficient (Cl) of an aircraft model. Always used together in the lift and drag formulas: L = Cl × A × ρv²/2 and D = Cd × A × ρv²/2 (section 3.3). Constitutive part of AircraftModel. |
| **AirControlArea** | Entity (root) | A geographic area of airspace used for air traffic control. Identified by a unique area code and defined by valid rectangular geographic boundaries. US050. |
| **Aircraft** | Entity (root) | A specific physical aircraft of a given model operated by an air transport company. Identified by a unique registration number and country of registration. Has a cabin configuration and crew count. Section 3.2, US070. |
| **AircraftModel** | Entity (root) | A motorised commercial aircraft model. Characterised by a model code, manufacturer, type, motorisation, weights, performance parameters, and aerodynamic coefficients. Name and manufacturer combination must be unique. Section 3.2, US055. |
| **AircraftModelCode** | Value Object | The business identifier of an AircraftModel. Uniqueness rule: name + manufacturer must be unique. US055. Constitutive part of AircraftModel. |
| **AircraftPerformance** | Value Object | Groups service ceiling, cruise speed, and maximum range of an aircraft model. Each value has its own unit. Section 3.2, US055. Constitutive part of AircraftModel. |
| **AircraftType** | Enum | The commercial purpose of an aircraft model. Values: `PASSENGER`, `CARGO`, `MIXED`. Section 3.2. |
| **AircraftVariant** | Entity (internal) | A specific combination of an aircraft model and a certified engine model. Exists only within AircraftModel — no independent lifecycle. References EngineModel by ID only. US057. |
| **AircraftWeights** | Value Object | Groups empty weight, MTOW, MZFW, and max fuel capacity of an aircraft model. Invariant: MTOW > MZFW > emptyWeight, all positive. Section 3.2. Constitutive part of AircraftModel. |
| **Airport** | Entity (root) | An airport used as origin or destination of a flight route. Identified by IATA (3-letter) and ICAO (4-letter) codes. Has location coordinates and elevation. US052. |
| **AirportIATA** | Value Object | 3-letter IATA code identifying an airport. Business rule: unique worldwide. US052. Constitutive part of Airport. |
| **AirportICAO** | Value Object | 4-letter ICAO code identifying an airport. Business rule: unique worldwide. US052. Constitutive part of Airport. |
| **AirTransportCompany** | Entity (root) | A company that uses the system to register aircraft, manage pilots, and operate flights. Has a unique IATA and ICAO code. US060. |
| **AreaCode** | Value Object | The unique identifier of an AirControlArea. Business rule: unique in the system. US050. Constitutive part of AirControlArea. |
| **AreaName** | Value Object | The name of an AirControlArea. US050. Constitutive part of AirControlArea. |
| **BoundingBox** | Value Object | The rectangular geographic boundary of an AirControlArea, defined by min/max latitude and longitude. Invariant: min < max for both axes. US050. Constitutive part of AirControlArea. |
| **CabinConfiguration** | Value Object | The seating arrangement of an aircraft, composed of one or more SeatClass entries. Business rule: total seat count ≤ aircraft model capacity. Section 3.2, US070. Constitutive part of Aircraft. |
| **Collaborator** | Entity (root) | A person who works for an AirTransportCompany (ATC) or AirControlArea (FCO, Weather). Has a name, position, security clearance, and periodic skills assessment. Linked to a SystemUser for authentication. Specialised by `CollaboratorType`. Section 3.1. |
| **CollaboratorType** | Enum | The role of a Collaborator. Values: `ATC` (employed by AirTransportCompany), `FCO` (Flight Control Operator, works for AirControlArea), `WEATHER` (Weather Person, works for AirControlArea). |
| **CompanyIATA** | Value Object | 2-letter IATA code identifying an AirTransportCompany. Business rule: unique worldwide. US060. Constitutive part of AirTransportCompany. |
| **CompanyICAO** | Value Object | 2–3 letter ICAO code identifying an AirTransportCompany. Business rule: unique worldwide. US060. Constitutive part of AirTransportCompany. |
| **Coordinates** | Value Object | Geographic location defined by latitude and longitude (single point). Business rules: latitude ∈ [-90, 90], longitude ∈ [-180, 180]. Used by Airport and WindCondition (same class in code). Constitutive part of Airport / WindCondition. |
| **Elevation** | Value Object | The altitude of an airport in metres above sea level. Has value and unit. Business rule: non-negative. US052. Constitutive part of Airport. |
| **EngineModel** | Entity (root) | An engine model certified for use on aircraft models. Characterised by a code, name, manufacturer, motorisation type, power, thrust (static and cruise), TSFC, and fuel type. Name + manufacturer combination must be unique. US056. |
| **EngineModelCode** | Value Object | The business code identifying an EngineModel. Constitutive part of EngineModel. |
| **EngineName** | Value Object | The name of an EngineModel. Uniqueness rule: name + manufacturer must be unique. US056. Constitutive part of EngineModel. |
| **Flight** | Entity (root) | An instantiation of a FlightRoute on a given departure time. Identified by a unique flight designator. Has a type (regular or charter), assigned aircraft, assigned pilot, departure time, and one or more flight plans. Section 3.4, US080. |
| **FlightDesignator** | Value Object | The unique identifier of a flight. Single string field — format: `xxn(n)(n)(n)(a)` (airline designator + number + optional suffix). Business rule: unique. Section 3.2. Constitutive part of Flight. |
| **FlightPlan** | Entity (internal) | A document describing how a specific flight takes place. Contains DSL content, creation and test timestamps, report file path and content, and a validation result. Created in DRAFT status, moves through IN_TEST to TEST_PASSED or TEST_FAILED. A flight may have multiple plans. Internal to Flight aggregate — no lifecycle outside it. US080, US082, US085. |
| **FlightPlanId** | Value Object | Auto-generated numeric identifier of a FlightPlan. Constitutive part of FlightPlan. |
| **FlightPlanStatus** | Enum | The lifecycle state of a flight plan. Values: `DRAFT` (created/weather-added), `IN_TEST` (validation in progress), `TEST_PASSED` (all phases passed), `TEST_FAILED` (at least one phase failed). US080, US082, US085. |
| **FlightRoute** | Entity (root) | A route between two airports (origin and destination) owned by an AirTransportCompany. Has a unique route name and optional deactivation date. US073, US074. |
| **FlightType** | Enum | The type of a flight. Values: `REGULAR` (recurring schedule defined at route level), `CHARTER` (single occurrence). Section 3.2. |
| **fuelType** (EngineModel) | Attribute (String) | The fuel type consumed by an EngineModel. Validated against `FuelType.ALL` constants class. Valid values: `Jet-A1` (kerosene-based, sec. 3.3 — density 0.804 kg/l, specific energy 42.80 MJ/kg), `AvGas 100LL` (aviation gasoline), `SAF` (Sustainable Aviation Fuel). |
| **Manufacturer** | Entity (root) | A company that manufactures aircraft models and/or engine models. A single manufacturer can cover both roles. Section 3.1.2. |
| **ManufacturerName** | Value Object | The name of a Manufacturer. Constitutive part of Manufacturer. |
| **MotorizationType** | Enum | The type of motorisation of an engine model. Values: `TURBOPROP`, `TURBOFAN`, `TURBOJET`, `RAMJET`, `ELECTRIC_PROPELLER`. Section 3.2. |
| **OperationalStatus** | Enum | The operational state of an aircraft. Values: `ACTIVE`, `DECOMMISSIONED`. US071. |
| **Pilot** | Entity (root) | A professional pilot belonging to an AirTransportCompany, certified to fly one or more aircraft models. Identified by a professional license number. Has certification date and active status. Responsible for creating and validating flight plans. Cannot be deactivated with assigned flights. Standalone aggregate — does not extend Collaborator. US075–US077. |
| **PilotId** | Value Object | The unique identifier of a Pilot. Wraps the professional `licenseNumber` (not an email). C01, C05. Constitutive part of Pilot. |
| **Power** | Value Object | The power output of an engine model. Has value and unit. Business rule: positive. US056. Constitutive part of EngineModel. |
| **RegistrationNumber** | Value Object | The unique registration of an aircraft, composed of registration number and country of registration. Business rule: unique worldwide. US070. Constitutive part of Aircraft. |
| **FlightRouteName** | Value Object | The name of a FlightRoute. Format: 2 letters (company initials) + up to 4 digits (e.g., TP123). Business rule: unique. US073. Constitutive part of FlightRoute. |
| **SafetyThreshold** | Value Object | The safety threshold parameter of a simulation. Has value and unit. Business rule: positive. US100. Constitutive part of Simulation. |
| **SeatClass** | Value Object | One class of seating within a CabinConfiguration. Has a class name and number of seats. Constitutive part of CabinConfiguration. Section 3.2. |
| **SecurityClearance** | Value Object | The security clearance of a collaborator. Contains an expiry date. Business rule: must not be expired. Section 3.1.1. Constitutive part of Collaborator. |
| **Simulation** | Entity (root) | A simulation of flights in a given AirControlArea. Defined by time range, safety thresholds, included flights, and optional weather data. Produces a simulation report. US100. |
| **SimulationReport** | Value Object | The outcomes of a simulation. Contains file path and content (TEXT/CLOB in database). Final immutable document from the C module. US109, US111. Constitutive part of Simulation. |
| **SimulationTimeRange** | Value Object | The time range of a simulation. Contains start and end date/time. Invariant: start < end. US100. Constitutive part of Simulation. |
| **SkillsAssessment** | Value Object | The periodic skills assessment of a collaborator. Contains the assessment date. Business rule: must be within the last 5 years. Section 3.1.1. Constitutive part of Collaborator. |
| **SystemUser** | Entity (framework boundary) | A user managed by the EAPLI framework. Stores email (unique), name, and phone. Section 3.1.1. Referenced by Collaborator — lifecycle managed by the framework, not the domain. |
| **Thrust** | Value Object | The thrust produced by an engine at a given speed reference (static or cruise). Has value, unit, and speed reference. Section 3.3. An EngineModel always has exactly two Thrust instances. Constitutive part of EngineModel. |
| **TSFC** | Value Object | Thrust Specific Fuel Consumption — fuel efficiency of an engine design. Has value and unit. Section 3.3. Constitutive part of EngineModel. |
| **ValidationResult** (Simulation) | Enum | The overall outcome of a simulation. Values: `PASSED`, `FAILED`, `PENDING`. US109, US111. |
| **ValidationResult** (FlightPlan) | Value Object | The outcome of a flight plan validation. Contains `passed` (boolean) and `reasons` (list of strings). Used as a return type by the validation service — not persisted in FlightPlan. |
| **WeatherData** | Entity (root) | Meteorological data for a specific AirControlArea at a given date/time. Contains wind condition readings. May come from multiple external providers. US041, US042. |
| **WindCondition** | Value Object | A wind reading at a specific geographic point. Has speed (knots), direction (degrees), coordinates, and altitude (metres). Constitutive part of WeatherData. |
