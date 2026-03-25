# Domain Model — AISafe Flight Control System

## US010 — Domain Model using DDD

---

## 1. Methodology

This domain model was elaborated following the process described in the Processo de Engenharia de Aplicações document (Paulo Gandra de Sousa, ISEP):

1. *First*, all business concepts and relationships were identified from the requirements document and use cases.
2. *Then*, each concept was classified as «entity» or «value object».
3. *Only after* both previous steps were complete were the aggregates identified and bounded.

The domain model reflects the *EAPLI Java application scope only*. The simulation execution mechanics (C processes, pipes, threads — SCOMP) are outside this model. The Java application stores simulation parameters, execution data, and results.

The domain model is a *living artefact* and will be updated as understanding of the domain deepens throughout the sprints.

---

## 2. Scope and Bounded Context

The EAPLI Java application is responsible for:

- *Basic configuration* — aircraft models, engine models, manufacturers, airports, air control areas, air transport companies
- *Flight management* — flight routes, flight plans (including DSL import and validation), flights
- *Weather service* — weather data registration, import and consultation
- *Simulation management* — storing simulation parameters, recording execution results and safety violations

The following are *outside* the EAPLI domain model scope:
- Internal details of SystemUser (login, password, email validation) — handled by the EAPLI framework
- C simulation execution mechanics (processes, pipes, shared memory, signals) — SCOMP responsibility
- DSL grammar and parser internals — LPROG responsibility
- Network communication protocols — RCOMP responsibility

---

## 3. Client Clarifications and Their Impact on the Model

The following decisions were revised after direct clarification with the client (product owner):

### 3.1 Manufacturer — single concept for aircraft and engine makers
*Question asked:* Can a single entity be both an aircraft manufacturer and an engine manufacturer?
*Client answer:* "Yes, it can."
*Decision:* A single Manufacturer concept covers both roles. AircraftModel and EngineModel both reference the same Manufacturer root. No need for separate concepts.

### 3.2 FlightRoute — two endpoints only
*Question asked:* Is a FlightRoute composed of multiple legs, or does it only have two endpoints?
*Client answer:* "A route has two endpoints. A flight plan for that route may include several legs."
*Decision:* FlightRoute has exactly one origin airport and one destination airport. Multiple legs belong to the FlightPlan, not the route. The route is a simple connection; the plan is the document describing how to fly it.

### 3.3 AltitudeSlots — multiple per segment
*Question asked:* Can a single segment have multiple altitude slots simultaneously?
*Client answer:* "Why not multiple slots. Just make sure that supports aircraft climbing/descending."
*Decision:* Segment has 1..* AltitudeSlot value objects. This supports layered altitude ranges for different traffic types and climb/descent phases.

### 3.4 Nodes and Segments — part of the FlightPlan document, not independently stored
*Question asked:* Are nodes always associated with airports, or can they be independent waypoints?
*Client answer:* "Of course a flight plan will have nodes that are not airports."

*Follow-up question:* Are segments and nodes preloaded? Can they be managed by any system user?
*Client answer:* "I believe we don't have any user story about managing nodes/junctions. There is actually the need to have them stored in the system? Can they be only part of a flight plan specification? Storing and managing all segments would be a lot of pain... can we avoid that?"

*Decision:* Segment and Node are *Value Objects* within the FlightPlan aggregate. They are part of the flight plan document/specification (defined via DSL). There is no independent repository for segments or nodes — they exist only as part of a FlightPlan. This is a direct consequence of the client's guidance and the absence of any user story managing segments or nodes independently.

This decision changes the earlier classification of Segment and Node from entity to value object.

### 3.5 WeatherData — varies within an air control area
*Question asked:* When weather data is registered for an area, can it have different values depending on location or altitude within the area?
*Client answer:* "Weather conditions are not the same everywhere inside an air control area. It could happen, but most likely it won't."
*Decision:* WindCondition within WeatherData is enriched with coordinates and altitude attributes, allowing weather readings to be associated with specific locations and altitudes within an area. The 1..* multiplicity on WindCondition per WeatherData record was already correct and is now further justified.

### 3.6 Route ownership confirmed
*Client answer:* "A route is owned by an ATC. Its ID includes the company ID (e.g. TP123)."
*Decision:* FlightRoute references AirTransportCompany root. Route name includes company initials. Already correct — confirmed by client.

---

## 4. Entity vs Value Object — Decision Criteria

Following the DDD methodology described in the process document:

- *Entity*: has a business identity of its own; can change over time while remaining the same concept; has an independent lifecycle; must be tracked.
- *Value Object*: describes or characterises something; has no identity of its own; is immutable; equality is based on attribute values, not identity; can and should be duplicated across aggregates for presentation purposes (as confirmed by the course coordinator — in code it is the same class).

---

## 5. Entity Decisions

### Manufacturer
*Entity.* Has its own business identity (name + country). Is referenced independently by both AircraftModel and EngineModel. A manufacturer can produce both aircraft and engine models (client confirmed). Lifecycle: created, potentially updated.

### EngineModel
*Entity.* Has a unique business identity (name + manufacturer combination — US056). Has its own lifecycle. Referenced by AircraftVariant.

### AircraftModel
*Entity.* Has a unique business identity (name + manufacturer combination — US055). Has its own lifecycle. Contains certified engine combinations. Referenced by Aircraft and Pilot.

### AircraftVariant
*Entity* (internal to AircraftModel aggregate). Explicitly mentioned in US057: "An aircraft model might have several aircraft variants (combinations of model and engine configuration)". Has identity within the context of its AircraftModel. Cannot exist without it.

### AirControlArea
*Entity.* Has a unique area code (US050). Has geographic boundaries. Has its own lifecycle. Referenced by Airport, WeatherData, Simulation.

### Airport
*Entity.* Has unique IATA and ICAO codes worldwide (US052). Has its own lifecycle and geographic location. Referenced by FlightRoute and FlightLeg.

### AirTransportCompany
*Entity.* Has unique IATA (2 letters) and ICAO (2–3 letters) codes (US060). Manages its own fleet and collaborators. Has its own lifecycle.

### Collaborator (abstract root)
*Entity.* Has a business identity tied to a SystemUser. Has an active/inactive lifecycle (US064). Skills and security clearance are managed over time. The domain model represents collaborators up to the SystemUser boundary — internal SystemUser details (login, password, email validation) are managed by the EAPLI framework, as confirmed by the course coordinator.

### ATCCollaborator
*Entity* (specialisation of Collaborator). Belongs to an AirTransportCompany. Manages aircraft, routes and pilots (US021–US023). Shares the same business nature as other collaborators — hence part of the same aggregate hierarchy.

### Pilot
*Entity* (specialisation of Collaborator). Belongs to an AirTransportCompany. Adds aircraft model certifications (US075). Creates and validates flight plans (US080–US085).

### FlightControlOperator
*Entity* (specialisation of Collaborator). Belongs to an AirControlArea. Tests flight plans and simulates air control (US042–US043). Managed directly by the Admin (US031).

### WeatherPerson
*Entity* (specialisation of Collaborator). Belongs to an AirControlArea. Registers and imports weather data (US041–US043).

### Aircraft
*Entity.* Has a unique registration number (US070). Has its own operational lifecycle (active → decommissioned — US071). Belongs to an AirTransportCompany.

### FlightRoute
*Entity.* Has a unique name (company initials + up to 4 digits — US073). Has its own lifecycle (can be deactivated — US074). Connects exactly two airports (client confirmed: "a route has two endpoints").

### FlightPlan
*Entity.* Described in section 3.4. Created before a flight exists (US080, US081). Has its own status lifecycle: draft → validated. Can be imported from a DSL file, validated independently (US085). It is the formal document describing how a flight takes place.

### FlightLeg
*Entity* (internal to FlightPlan aggregate). A single non-stop journey segment within a flight plan (section 3.4.1). Has a sequence position. Cannot exist without its FlightPlan. Client confirmed: "a flight plan for that route may include several legs".

### Flight
*Entity.* Has a unique flight designator (format xxn(n)(n)(n)(a) — section 3.2). Is an instantiation of a FlightRoute on a specific date/time with a specific aircraft (section 3.4: "a flight is an instantiation of a route"). Has its own lifecycle.

### WeatherData
*Entity.* Identified by area + date/time + source provider. Has its own lifecycle (registered, imported, consulted — US041–US043). Multiple providers can contribute data for the same area.

### Simulation
*Entity.* Has its own identity, parameters, and lifecycle (created → executed → reported). The Java application stores simulation parameters and records results. Execution mechanics are in C (SCOMP).

### SimulationReport
*Entity* (internal to Simulation aggregate). Records the outcome of a simulation. Cannot exist without a Simulation.

### SafetyViolation
*Entity* (internal to Simulation aggregate). Records a specific detected violation event. Cannot exist without a SimulationReport.

---

## 6. Value Object Decisions

### Coordinates
*Value Object.* A latitude/longitude pair. Immutable. Appears in AirControlArea, Airport, Segment, Node, and WindCondition (in WeatherData). Duplicated in diagram for presentation; same class in code (course coordinator confirmed).

### IATACode
*Value Object.* A code with specific format rules (3 letters for airports, 2 letters for companies). Validates its own format. Immutable.

### ICAOCode
*Value Object.* A code with specific format rules (4 letters for airports, 2–3 letters for companies). Validates its own format. Immutable.

### SecurityClearance
*Value Object.* Describes clearance status with an automatic expiry date (section 3.1.1). Immutable — when updated, a new value replaces the old.

### SkillsAssessment
*Value Object.* Describes a periodic skills assessment result (every 5 years — section 3.1.1). Immutable. No identity of its own.

### Thrust
*Value Object.* Numeric value + unit describing engine thrust. Immutable. Characterises an EngineModel.

### TSFC
*Value Object.* Numeric value + unit describing fuel efficiency per unit of thrust (section 3.3). Immutable. Characterises an EngineModel.

### MotorizationType
*Value Object* (enumeration). One of: turboprop, turbofan, turbojet, ramjet, electric propeller.

### CabinConfiguration
*Value Object.* Number of seats per class. Immutable per configuration. Characterises an Aircraft.

### OperationalStatus
*Value Object* (enumeration). One of: active, decommissioned.

### FlightDesignator
*Value Object.* String with validated format xxn(n)(n)(n)(a). Immutable. Validates its own format.

### FlightType
*Value Object* (enumeration). One of: regular, charter.

### FlightPlanStatus
*Value Object* (enumeration). One of: draft, validated.

### AltitudeSlot
*Value Object.* An allowed altitude range + width for a segment. Immutable. Client confirmed multiple slots per segment to support climbing/descending.

### WindCondition
*Value Object.* Wind direction (angle relative to North) + speed (m/s). In WeatherData, also carries coordinates and altitude to specify location within the area (client confirmed: "weather conditions are not the same everywhere inside an air control area"). Immutable. Same class used in Segment and WeatherData.

### FuelLoad
*Value Object.* Fuel quantity + unit per leg. Immutable. Characterises a FlightLeg.

### Segment (revised from entity — client clarification)
*Value Object* (internal to FlightPlan aggregate). A linear path within a leg defined by start and end coordinates, altitude slots, and wind condition. *Revised to value object* following client clarification: "Can they be only part of a flight plan specification? Storing and managing all segments would be a lot of pain... can we avoid that?" There is no user story managing segments independently.

### Node (revised from entity — client clarification)
*Value Object* (internal to FlightPlan aggregate). A junction point connecting two or more segments, identified by its coordinates. *Revised to value object* following client clarification: "I believe we don't have any user story about managing nodes/junctions." Nodes exist only as part of the flight plan specification.

---

## 7. Aggregate Justification

(See dedicated file: aggregate-justification.md)

---

## 8. Checklist Compliance

Following the checklist from the Processo de Engenharia de Aplicações document (section 4.4):

1. ✅ No "verb" concepts — all concepts are nouns
2. ✅ All associations have navigability and cardinality indicated in the diagram
3. ✅ No bidirectional associations
4. ✅ Cross-aggregate references point only to aggregate roots, never to internal entities
5. ✅ All entities have a business identity
6. ✅ All entities, value objects, and aggregates have a description in the glossary
7. ✅ All names present in the requirements document — none invented
8. ✅ No technical/implementation names
9. ✅ No generic names ("data", "information")
10. ✅ No external system concepts modelled internally (SystemUser boundary respected)
11. ✅ All use cases can be answered by navigating the domain model

---

## 9. Notes on SystemUser

Per the course coordinator's guidance: collaborators must appear in the domain model and have a SystemUser which belongs to the EAPLI framework. The SystemUser has login and password. The domain model represents collaborators *up to* SystemUser — the internal details of SystemUser are not part of our domain model and are managed by the framework.

## 10. Notes on Simulation

Per the course coordinator's guidance: the EAPLI Java application and the C simulations are separate concerns. The Java application stores execution data and results. The domain model for EAPLI reflects what the Java application manages — simulation parameters, results, and safety violation records. The execution mechanics (processes, pipes, shared memory) are SCOMP's responsibility and do not appear in this domain model.