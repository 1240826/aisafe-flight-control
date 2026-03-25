# Domain Model Glossary — AISafe Flight Control System

All concepts present in the domain model diagram are listed here, and all concepts listed here are present in the diagram. The glossary is synchronised with the domain model diagram.

---

## Abbreviations

| **Term** | **Description** |
|:---|:---|
| **_ACA_** | Air Control Area |
| **_ATC_** | Air Transport Company |
| **_ATCC_** | Air Transport Company Collaborator |
| **_Cd_** | Drag Coefficient |
| **_Cl_** | Lift Coefficient |
| **_DDD_** | Domain-Driven Design |
| **_DSL_** | Domain Specific Language |
| **_FCO_** | Flight Control Operator |
| **_IATA_** | International Air Transport Association |
| **_ICAO_** | International Civil Aviation Organization |
| **_MTOW_** | Maximum Take-Off Weight |
| **_MZFW_** | Maximum Zero Fuel Weight |
| **_TSFC_** | Thrust Specific Fuel Consumption |
| **_VO_** | Value Object |

---

## Domain Concepts

| **Term** | **Type** | **Description** |
|:---|:---|:---|
| **Aircraft** | `«entity»` `«root»` | An aircraft of a specific model operated by an air transport company. Characterised by a unique registration number, the country of registration, its cabin configuration, and the number of elements of the flight crew. |
| **AircraftModel** | `«entity»` `«root»` | A motorised commercial aircraft model. Characterised by a model ID, maker, type (passenger, cargo, or mixed), motorisation, empty weight, maximum take-off weight (MTOW), maximum zero fuel weight (MZFW), maximum fuel capacity, service ceiling, cruise speed, wing area, drag coefficient (Cd), and lift coefficient (Cl). |
| **AircraftVariant** | `«entity»` | A specific combination of an aircraft model and a certified engine model. An aircraft model may have several variants. |
| **AirControlArea** | `«entity»` `«root»` | A geographic area of airspace used for air traffic control. Has a unique area code and valid geographic boundaries. |
| **AirTransportCompany** | `«entity»` `«root»` | A company that uses the system to register aircraft and flights. Has a unique company name, IATA designator (2 letters), and ICAO code (2–3 letters). |
| **AltitudeSlot** | `«value object»` | An allowed altitude range within a segment, with an associated width for each slot. A segment may have multiple altitude slots to accommodate different traffic layers and aircraft climbing or descending. |
| **ATCCollaborator** | `«entity»` | A collaborator of an air transport company. Can register aircraft, create flight routes, and manage pilots for the company. |
| **CabinConfiguration** | `«value object»` | The number of seats in each class aboard a specific aircraft. The total number of seats cannot exceed the model's capacity. |
| **Collaborator** | `«entity»` `«root»` *(abstract)* | A person with access to the system who works for a customer (an air transport company or an air control area). Has a name, position, email, security clearance, and periodic skills assessment. Associated with a system user for authentication purposes. |
| **Coordinates** | `«value object»` | A geographic position defined by a latitude and longitude pair. Used to express the location of airports, the boundaries of air control areas, the endpoints of segments, the position of nodes, and the location of weather readings. |
| **EngineModel** | `«entity»` `«root»` | An engine model that can be certified for use on aircraft models. Characterised by a name, maker, type, power, fuel type, and efficiency. The combination of name and manufacturer must be unique. |
| **Flight** | `«entity»` `«root»` | A connection between two airports. Characterised by a unique flight designator (format: `xxn(n)(n)(n)(a)`), a type (regular or charter), a departure day or date and time, and a scheduled arrival time. A flight is an instantiation of a route, including flight-specific information such as date/time, aircraft, and load. |
| **FlightControlOperator** | `«entity»` | A collaborator of a flight control entity. Responsible for testing flight plans and simulating air control. Security is a main concern — these collaborators are directly managed by the system administrator. |
| **FlightDesignator** | `«value object»` | The unique identifier of a flight, formed by concatenating the airline designator (2 letters), the numeric flight number (up to 4 digits), and an optional one-letter operational suffix. Full format: `xxn(n)(n)(n)(a)`. |
| **FlightLeg** | `«entity»` | A single non-stop journey segment between two airports within a flight plan, defined by one take-off and one landing. A flight plan may include one or more legs. |
| **FlightPlan** | `«entity»` `«root»` | A document describing how a flight takes place, including the aircraft, pilot, departure date and time, fuel quantity, and the sequence of legs with their segments. Has a status of draft until validated. Can be created manually or imported from a DSL file. |
| **FlightPlanStatus** | `«value object»` *(enum)* | The current status of a flight plan in its lifecycle. One of: `draft`, `validated`. |
| **FlightRoute** | `«entity»` `«root»` | A route between two airports (origin and destination) owned by an air transport company. Has a unique name composed of the company's initials and up to 4 digits (e.g. TP123). Can be deactivated from a given date onwards. |
| **FlightType** | `«value object»` *(enum)* | The type of a flight. One of: `regular` (with specified departure days of the week) or `charter` (with a specific departure date). |
| **FuelLoad** | `«value object»` | The quantity of fuel carried on a specific flight leg, expressed with a unit. Must be strictly positive. |
| **IATACode** | `«value object»` | A code issued by the International Air Transport Association. Used as a 3-letter code for airports and a 2-letter designator for airlines. |
| **ICAOCode** | `«value object»` | A code issued by the International Civil Aviation Organization. Used as a 4-letter code for airports and a 2–3 letter code for companies. |
| **Manufacturer** | `«entity»` `«root»` | A company that makes aircraft models and/or engine models. Characterised by name and country. |
| **MotorizationType** | `«value object»` *(enum)* | The type of motorisation of an engine. One of: `turboprop`, `turbofan`, `turbojet`, `ramjet`, `electricPropeller`. |
| **Node** | `«value object»` | A connection point between two or more segments, enabling an aircraft to move between segments. Defined by its geographic coordinates. Nodes are not airports — they are waypoints within the airspace. Part of the flight plan specification only; not independently stored in the system. |
| **OperationalStatus** | `«value object»` *(enum)* | The operational state of an aircraft. One of: `active`, `decommissioned`. |
| **Pilot** | `«entity»` | A collaborator of an air transport company who is certified to pilot one or more aircraft models. Responsible for registering and validating flight plans. |
| **SafetyViolation** | `«entity»` | An event detected during simulation in which two or more aircraft are in close proximity, potentially violating safety rules. Recorded with a timestamp, the positions of the involved aircraft, and their velocity vectors. |
| **SecurityClearance** | `«value object»` | The security clearance of an AISafe user, which automatically expires at a given date. Can be updated by the Admin or the Backoffice Operator. |
| **Segment** | `«value object»` | A linear path connecting two points within a flight leg. Characterised by the coordinates of the beginning and ending nodes, the allowed altitude slots, the width for each altitude slot, and the wind direction and speed. Part of the flight plan specification only; not independently stored in the system. |
| **SimulationReport** | `«entity»` | A report of the outcomes of a simulation, including the total number of flights, their individual execution statuses, all safety violation events with timestamps and positions, and the overall pass/fail validation result. |
| **Simulation** | `«entity»` `«root»` | A simulation of the flights in a given air control area. Defined by parameters such as time range, geographic area, included flights, weather conditions, safety thresholds, and performance settings. |
| **SkillsAssessment** | `«value object»` | A periodic skills assessment of a collaborator, required by regulations every 5 years. Recorded with a date and result. |
| **SystemUser** | `«entity»` *(EAPLI framework)* | A user of the system identified by a unique valid email, with a name, phone number, and credentials (login and password). Managed by the EAPLI framework. The domain model represents collaborators up to this boundary. |
| **Thrust** | `«value object»` | The force produced by an engine. A manufacturer-supplied parameter given at sea level at two reference speeds (static and cruise). Thrust decreases with altitude. |
| **TSFC** | `«value object»` | Thrust Specific Fuel Consumption. Describes the fuel efficiency of an engine with respect to thrust output — fuel consumption per unit of thrust. |
| **WeatherData** | `«entity»` `«root»` | Meteorological data registered for a specific air control area. Weather conditions are not the same everywhere inside an air control area, so each record may contain readings for different locations and altitudes within the area. Multiple external weather service providers may contribute data for the same area. |
| **WeatherPerson** | `«entity»` | A collaborator responsible for registering and importing weather data into the system. |
| **WindCondition** | `«value object»` | The wind at a given point, expressed as a direction angle relative to North (degrees) and a speed (m/s). In the context of a segment, describes the wind the aircraft is exposed to along that path. In the context of weather data, also includes the coordinates and altitude of the reading within the air control area. |