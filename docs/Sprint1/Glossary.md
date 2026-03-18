| **Term** (EN) | **Description** (EN) |
|:---|:---|
| **Admin** | A system user responsible for managing system-wide users and security clearances. Can update security clearance and skills assessment information for AISafe users. |
| **Air Control Area** | A geographic region defined with boundaries and characterized by a unique area code. Used to organize airspace for flight control operations. |
| **Aircraft** | An aircraft of a specific model, characterized by registration (unique ID), company, cabin configuration (number of seats in each class), and number of flight crew elements. Also has an operational status. |
| **Aircraft Engine Model** | An engine model to be used by aircrafts. Characterized by name and manufacturer combination (must be unique), type, power, fuel, and efficiency. |
| **Aircraft Manufacturer** | An organization that manufactures aircraft. Characterized only by name and country. |
| **Aircraft Model** | A motorized commercial aircraft type characterized by Model ID, Maker, Type (passenger, cargo, mixed), Motorization, Empty weight, Maximum take-off weight (MTOW), Maximum Zero Fuel Weight (MZFW), Maximum fuel capacity, Service ceiling, Cruise speed, Wing area, Drag coefficient (Cd), and Lift coefficient (Cl). The combination of model name and manufacturer must be unique. |
| **Airport** | A location characterized by Name, Town, Country, IATA airport code (3 letters), ICAO airport code (4 letters), Coordinates, and Altitude. Must be associated with exactly one Air Control Area. Airport codes must be unique worldwide. |
| **Air Transport Company** | An airline or aircraft operator registered in the system. Characterized by company name, IATA code (2 letters), and ICAO code (2-3 letters), both codes must be unique. |
| **Air Transport Company Collaborator (ATCC)** | A system user representing an air transport company. Can register aircrafts, flight routes, and manage pilots for the company. Characterized by email (valid company email), name, and position. Can be enabled or disabled. |
| **Altitude Slot** | Allowed altitude range with width for each altitude slot in a flight segment. |
| **Arrival Information** | Details about when and where a flight leg arrives. |
| **Backoffice Operator** | A system user responsible for basic configuration tasks including registering aircraft models, engine models, manufacturers, air transport companies, airports, and air traffic areas. |
| **Coordinates** | Geographic location defined by latitude and longitude. |
| **Credentials** | User authentication information including email (valid and unique from allowed domains), name, and phone number. |
| **Departure Information** | Details about when and from where a flight leg departs. |
| **Engine Manufacturer** | An organization that manufactures aircraft engines. Characterized only by name and country. |
| **Flight** | A connection between two airports representing an instantiation of a route with specific operational information. Characterized by unique flight designator (airline designator + numeric flight number + optional operational suffix), type (regular or charter), departure day/time, scheduled arrival time, aircraft, pilot, fuel load, and flight plan. Flight status is initially "draft" until validated. |
| **Flight Control Operator (FCO)** | A system user from flight control entities (public agencies like FAA or Eurocontrol, or private companies) with access to test flight plans and simulate air control. Directly managed by system administrator following standard user rules. |
| **Flight Designator** | The unique identifier of a flight. Formatted as airline designator (2 letters) + numeric flight number (1-4 digits) + optional operational suffix (1 letter). Format: xxn(n)(n)(n)(a). |
| **Flight Leg** | A single, non-stop journey segment between two airports. Part of a flight plan. Each leg contains departure information, arrival information, route, segments, and fuel information. |
| **Flight Plan** | A complete flight operation description that may include multiple take-offs and landings. Composed of one or more flight legs. Describes how the flight takes place including fuel load. Must conform to the Core Flight DSL and be validated before use. |
| **Flight Route** | A named path between two airports (start and end) defined by an Air Transport Company. Route name contains 2 letters (company initials) and up to 4 numbers (e.g. TP123). Name must be unique. Can be deactivated from a given date onwards. |
| **Flight Segment** | A linear path connecting two points in a flight route. Characterized by coordinates of beginning node, coordinates of ending node, allowed altitude slots, width for each altitude slot, wind direction (angle relative to North), and wind speed (m/s). |
| **Manufacturer Code** | Unique identification for a manufacturer with name and country. |
| **Node/Junction** | A connection point between two or more segments allowing the aircraft to move between segments. |
| **Pilot** | A system user certified to pilot one or more aircraft models. Employed by an Air Transport Company. Can create flight plans for routes of their company. Can be made inactive but cannot be deactivated if they have assigned flight plans. |
| **Route Designator** | The unique identifier of a flight route. Formatted as 2-letter company initials + up to 4 numbers (e.g. TP123). Must be unique. |
| **Security Clearance** | Active security status for an AISafe user with automatic expiration date. Updated by Admin or Backoffice Operator. |
| **Skills Assessment** | Periodic (every 5 years per regulation) evaluation of technical skills for AISafe users. Updated by Admin or Backoffice Operator. |
| **System User** | A person with access to the system. Identified by unique valid email from allowed domains. Also has name and phone number. Must authenticate to access the system. |
| **Weather Data** | Information about weather conditions for a specific air control area and day. Can be registered individually or imported in bulk from multiple external weather service providers. Consulted by Weather Persons, Pilots, and Flight Control Operators. |
| **Weather Person** | A system user responsible for registering weather data, importing bulk weather data, and ensuring weather information is available for flights in specific air control areas. |