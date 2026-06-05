# Sprint 3 — Client Clarifications & Team Decisions

> **Purpose:** Consolidate all client clarifications received during Sprint 3 (Moodle forum)
> and document the team's design decisions based on each clarification, showing how each
> answer influenced our EAPLI, LPROG, RCOMP, and SCOMP design choices.

---

## 1. Client Clarifications and Team Decisions

### C01 — NFR08: Database — PostgreSQL vs H2 in Docker

**Date:** Sprint 3, Moodle forum
**Asked by:** Munaf

**Question:**
> "According to NFR08, we understand that the system must support both 'in-memory' persistence
> and a relational database (RDBMS), and that the final deployment must use a remote persistent
> relational database. Is it mandatory to use an external persistent relational database such as
> PostgreSQL/MySQL in the final deployment? Or would it be sufficient to use H2 in server/file-based
> mode (for example hosted in a Docker container), as long as it remains a persistent relational
> solution?"

**Answer (Ângelo Martins):**
> In the sprint review of sprint 3 you are expected to use a RDBMS, at least in LAPR4.
> Using H2 in server mode in a docker is a possibility.
> On the other hand, using PostgreSQL with PostGIS would be a clever way to simplify queries
> with coordinates.

**Team decisions:**

1. **Decision:** PostgreSQL with PostGIS is our recommended choice for final deployment.
   **Basis:** The client recommended it to simplify coordinate-based queries such as weather data within ACA bounds and route segment lookups.

2. **Decision:** H2 in server mode (Docker) is our accepted fallback.
   **Basis:** The client explicitly permits it as an alternative for simpler setups.

3. **Decision:** In-memory mode (H2 fileless) continues to be used for development and testing.
   **Basis:** This follows the existing Sprint 2 pattern and keeps tests fast without external dependencies.

4. **Decision:** The database choice is configurable via environment properties.
   **Basis:** NFR08 requires "by configuration" switching between persistence modes; this allows changing between in-memory, H2 server, and PostgreSQL without code changes.

---

### C02 — CSV Weather Data: Provided or Self-Created

**Date:** Sprint 3, Moodle forum
**Asked by:** Rodrigo Guerra

**Question:**
> "Regarding the section of this user story where it says 'a simple CSV file format will be available',
> will this CSV file be provided by the client? Or are we (meaning my group) supposed to develop
> our own CSV containing weather data pertaining to our implementation of Weather Data in our project?
> If this is not the case, what would be the best approach if the CSV provided by outside sources
> doesn't have enough information for creating a valid weather data in our system?"

**Answer (Ângelo Martins):**
> I'll give you an example tomorrow. You may have to create others for testing purposes.

**Team decisions:**

1. **Decision:** We rely on the client-provided CSV as our reference format for initial development.
   **Basis:** The client committed to providing at least one example — sufficient to understand the expected structure.

2. **Decision:** Our team creates additional CSV files for testing edge cases, boundary values, and invalid data.
   **Basis:** The client stated each team must create their own for testing.

3. **Decision:** The CSV import module is designed to accept different CSV structures flexibly.
   **Basis:** US042 requires it to be "easy to expand to new weather data sources."

---

### C03 — US080/US081/US085: Workflow Confirmation

**Date:** Sprint 3, Moodle forum
**Asked by:** Monteiro

**Question:**
> "Regarding US080, US081 and US085, we would like to confirm our interpretation of the intended
> workflow. Our understanding is:
> - US080: the Pilot enters the flight plan data manually through the UI, including legs and segments.
>   The system generates the DSL from this data and stores it in the flight plan as a draft.
> - US081: the Pilot provides a DSL file. The system reads it, validates it, and creates a flight plan
>   directly from the file. The DSL is stored in the flight plan.
> - US082: weather data is inserted into the segments of the flight plan, enriching the DSL with wind
>   information before simulation.
> - US085: the system validates the DSL already stored in the flight plan (whether it came from US080
>   or US081) and runs the C simulator.
>
> Is this interpretation correct?"

**Answer (Ângelo Martins):**
> In US080 you don't need to read the flight plan line by line. You can, if you want, but you can
> also read it as a string or even from a text file. You don't validate it.
>
> In US082 it would be strange and burdensome to make the pilot to manually load data for the flight.
> That information it's already in the system and pilots are expensive (hourly cost). So, I believe it
> makes more sense to allow the pilot to select the weather information he/she wants to use in the
> flight plan. This is aligned with US043.
>
> Please take in account that US042 hints at the possibility that there may be multiple weather data
> providers, thus there may be multiple weather data information for the same area at a given time.

**Team decisions:**

1. **Decision:** US080 accepts DSL as a raw string, from any source (text input, file upload, or UI fields), without validation.
   **Basis:** The client explicitly said "read it as a string or even from a text file. You don't validate it."

2. **Decision:** US080 is not a form-based UI that breaks down legs and segments — it accepts the DSL text directly.
   **Basis:** The original question assumed form-based entry; the client corrected this interpretation.

3. **Decision:** US081 requires full DSL validation (lexical, syntactic, semantic) before creating the plan.
   **Basis:** This is the key difference from US080 — US081 validates, US080 does not.

4. **Decision:** US082 presents existing weather data for the pilot to select, with no manual data entry.
   **Basis:** The client emphasised that "pilots are expensive" and argued against manual data entry, aligning with US043 (consult weather data).

5. **Decision:** US085 performs two-phase validation: re-run DSL validation on the stored DSL, then invoke the C simulator.
   **Basis:** Two-phase validation confirmed by the client — fail fast on DSL errors before running the simulator.

6. **Decision:** The system supports multiple weather data sources for the same area and time; the pilot chooses which to use.
   **Basis:** US042 supports multiple providers and the client confirmed this possibility.

---

### C04 — Weather Data Sources: Files vs API

**Date:** Sprint 3, Moodle forum
**Asked by:** Vieira

**Question:**
> "What do you understand by 'weather data sources'? (Are they simply different file formats or the
> name of enterprises/entities that we have collected the data from?) How would the import work?
> Would this bulk import consist of only files or would it be an API call to another system?"

**Answer (Ângelo Martins):**
> The bulk import may use files or API calls to another systems. The support for the provided files
> will be mandatory.

**Team decisions:**

1. **Decision:** CSV file import is mandatory for Sprint 3.
   **Basis:** The client explicitly requires support for the provided files.

2. **Decision:** API-based import is optional, but our design anticipates it.
   **Basis:** The client said import "may use files or API calls" — we design for extensibility.

3. **Decision:** The weather data import module uses a strategy/adapter pattern.
   **Basis:** This allows swapping between file-based and API-based sources without changing the core import logic.

---

### C05 — US075: Pilot vs Collaborator (Duplication with US061)

**Date:** Sprint 3, Moodle forum
**Asked by:** Silva

**Question:**
> "US75 asks to add a pilot to the ATCC company; however, the project requirements considers a pilot
> to be an collaborator. Therefore, in US61, if the collaborator to be added is a pilot, both US61
> and US75 fulfill the same requirement in a similar manner. My question concerns the distribution
> of responsibilities for a 'Backoffice Operator': should this user be able to add pilots to a specific
> company?"

**Answer (Ângelo Martins):**
> I believe you are not understanding the concept of "role". US 075 is cristal clear.

**Team decisions:**

1. **Decision:** US075 is a distinct user story — we do not merge it with US061.
   **Basis:** The client was emphatic: "crystal clear" — Pilot is a separate role from generic Collaborator.

2. **Decision:** The Pilot role is separate from the generic Collaborator aggregate.
   **Basis:** US075 specifically addresses adding a Pilot (not a generic collaborator), and the client distinguished them.

3. **Decision:** A Backoffice Operator may add collaborators via US061; adding pilots may require different authorization.
   **Basis:** Role segregation must be respected — the two user stories serve different actors.

---

### C06 — RCOMP: DEI Infrastructure vs External Cloud

**Date:** Sprint 3, Moodle forum
**Asked by:** Mariana Gabriel

**Question:**
> "Relativamente às user stories de RCOMP, o hosting dos serviços remotos e da base de dados deve
> obrigatoriamente utilizar infraestrutura disponibilizada pelo DEI, ou podemos utilizar uma solução
> cloud externa à nossa escolha desde que cumpra os requisitos do projeto?"

**Answer (Ângelo Martins):**
> You can use external infrastructure, but you will have to cover those expenses.

**Team decisions:**

1. **Decision:** We may choose DEI infrastructure or external cloud (e.g., AWS, Azure, GCP).
   **Basis:** The client permits external infrastructure.

2. **Decision:** External cloud costs are the team's responsibility.
   **Basis:** Budget consideration — the client warned of expenses.

3. **Decision:** DEI infrastructure is our default path.
   **Basis:** No additional costs and sufficient for our deployment needs.

---

### C07 — Pilot Licenses / Certifications

**Date:** Sprint 3, Moodle forum
**Asked by:** Cardoso

**Question:**
> "In real-world aviation, pilots have licenses that include categories, classes, flight hours, and
> aircraft-specific ratings and certifications. For our implementation, should we model these
> distinctions, or can we simplify the system by assuming that all pilots are equally qualified
> to fly any aircraft?"

**Answer (Ângelo Martins):**
> US075 says "A pilot is certified to pilot one or more aircraft models". That means that a pilot
> has a list of flight models that he/she can fly.
> Saying that a pilot can fly any plane is way out of the requirements.

**Team decisions:**

1. **Decision:** The Pilot aggregate includes `Set<AircraftModelCode> certifications`.
   **Basis:** US075 explicitly states a pilot is certified for specific aircraft models. The client considered the opposite "way out of requirements."

2. **Decision:** A pilot CANNOT fly any aircraft — certification is per model.
   **Basis:** Direct client clarification — "Saying that a pilot can fly any plane is way out of the requirements."

3. **Decision:** US080 and US121 validate that the assigned pilot is certified for the chosen aircraft.
   **Basis:** Cross-reference validation required by this clarification to prevent assigning uncertified pilots.

---

### C08 — Multiple Routes with Same Origin-Destination

**Date:** Sprint 3, Moodle forum
**Asked by:** Henri Fontes

**Question:**
> "Should an air transport company have flight routes that have the same pair starting-ending airports
> (with different names, of course) (e.g. a company that has two flight routes from Lisbon to Porto)?
> Does it makes sense in the context of the project?"

**Answer (Ângelo Martins):**
> I believe I've explained before what is a route: [link to previous post]
> You should read the forum before asking new questions. If you have any question related to a
> previous post, ask questions on that conversation.
>
> *(from earlier post, referenced here)* One of the routes Paris (ORY)-Rome (FCO) by Transavia
> is TO3910. Some flights on this route:
> Tuesday, 24th March 2026 - 06:45 - 08:50
> Wednesday, 25th March 2026 - 09:15 - 11:25
> ...
> Another route Paris (ORY)-Rome (FCO) by Transavia is TO3914.

**Team decisions:**

1. **Decision:** Route identity is the route name (flight number), not the origin-destination pair.
   **Basis:** The example shows TO3910 and TO3914 are both ORY-FCO but are different routes with different schedules.

2. **Decision:** Multiple routes with the same origin/destination pair are valid and distinct.
   **Basis:** Already implemented in `FlightRoute` — the route name (e.g., "TO3910") is the identity.

---

### C09 — Route Modeling: Regular vs Charter (Reuse Routes)

**Date:** Sprint 3, Moodle forum
**Asked by:** Tavares

**Question:**
> "Currently we consider that a route represents a fixed path and flights are instances associated
> with that route. When a route Porto→Lisbon already exists with a regular flight plan, and a charter
> flight is needed for the same route and schedule, should we: (1) create a new route + flight plan
> for the charter, or (2) reuse the same route and associate the type to the flight plan?"

**Answer (Ângelo Martins):**
> One of the routes Paris (ORY)-Rome (FCO) by Transavia is TO3910. Some flights on this route:
> Tuesday, 24th March 2026 - 06:45 - 08:50
> Wednesday, 25th March 2026 - 09:15 - 11:25
> Thursday, 26th March 2026 - 09:55 - 12:00
> Friday, 26th March 2026 - 06:15 - 08:20
> Another route Paris (ORY)-Rome (FCO) by Transavia is TO3914.
>
> So recycling is very good for the environment, but it doesn't apply to this situation.

**Team decisions:**

1. **Decision:** Each flight plan creates or references its own route (route name = flight number).
   **Basis:** Different flights mean different routes even if they share the same origin/destination. The client said "recycling doesn't apply."

2. **Decision:** A route is NOT reused across different flights.
   **Basis:** "Recycling is very good for the environment, but it doesn't apply to this situation."

3. **Decision:** FlightPlan references a FlightRoute by identity, not inline.
   **Basis:** Existing pattern — route is a separate aggregate referenced by its identity.

---

### C10 — US080 vs US081: DSL Difference

**Date:** Sprint 3, Moodle forum
**Asked by:** Sousa

**Question:**
> "Should the Flight Description DSL include the pilot (and other mandatory attributes) to be imported
> in US081 to create a flight plan? If the US080 lets the pilot input the DSL as string, what would
> the difference to US081?"

**Answer (Ângelo Martins):**
> Please read section 3.4.
> If you read the two user stories, you can see that US081 and US080 are quite different.
> For example, US081 requires the validation of the flight plan description. The same it's not
> required in US080.

**Team decisions:**

1. **Decision:** US080 accepts DSL input without any validation — status goes directly to DRAFT.
   **Basis:** The client confirmed US080 does not require validation.

2. **Decision:** US081 validates the DSL (lexical, syntactic, semantic) before creating the flight plan — status is DRAFT only after passing validation.
   **Basis:** Validation is the defining difference between US080 and US081, as stated by the client.

3. **Decision:** Both US080 and US081 share the same FlightPlan aggregate.
   **Basis:** One domain model with two input paths — the aggregate does not differentiate between sources.

---

### C11 — Route Deactivation: Delete vs Soft-Delete

**Date:** Sprint 3, Moodle forum
**Asked by:** António Alves

**Question:**
> "Regarding the deactivation of a route. Should the information of a deactivated route be kept in
> the database with a deactivation parameter, like a deactivation date, or should we delete the route
> completely? If the option is the complete deletion of the route, this raises a question in the case
> in which there are planned flights for a given route that is to be deleted after the current date."

**Answer (Ângelo Martins):**
> The simple fact that it came to your mind that you could actually delete (erase) a route from the
> DB makes me sad. I was responsible for BDDAD and how do you think I feel when a former students
> suggests that?

**Team decisions:**

1. **Decision:** Routes are never physically deleted from the database — no DELETE SQL is ever issued.
   **Basis:** The client was emphatic that deleting from DB is unacceptable coming from a student of his.

2. **Decision:** We use soft-delete / deactivation with a boolean flag or deactivation date.
   **Basis:** Standard practice in aviation systems — keeps historical data intact.

3. **Decision:** Existing flight plans that reference a deactivated route remain valid.
   **Basis:** Referential integrity must be preserved for historical records.

4. **Decision:** New flight plans cannot be created on a deactivated route.
   **Basis:** Application-level validation enforced at creation time.

---

### C12 — Weather Data Insertion: Manual Selection, Missing Data, Re-entry

**Date:** Sprint 3, Moodle forum
**Asked by:** Silva

**Question:**
> "Regarding the insertion of weather data onto a flight plan by a Pilot:
> 1. Is the data entry manual or automatic?
> 2. What if weather data is not yet available for the flight's area/date?
> 3. If the pilot re-enters the exact same weather report, does the test become void?"

**Answer (Ângelo Martins):**
> I believe I've said something about this before. No pilot would accept to have to insert a ton of
> weather data manually into the system. He only has to choose one (or more according to the flight
> schedule and air control areas involved) of the available weather forecasts.
>
> If there is no available weather forecast then the pilot can't choose one, isn't it?
>
> There is a file with weather forecasts in Moodle.

**Team decisions:**

1. **Decision:** The pilot selects from available weather forecasts — no manual data entry.
   **Basis:** The client stated "no pilot would accept to have to insert a ton of weather data manually." Consistent with C03 and US043.

2. **Decision:** If no forecast exists for the area/time, the pilot cannot select one.
   **Basis:** Simple logic — if no data, nothing to select. The system makes this visible to the pilot.

3. **Decision:** The weather forecast file available on Moodle must be imported via US042.
   **Basis:** This is the reference data for testing and demonstration.

4. **Decision:** If weather data is changed (even to the same data), the test result is voided.
   **Basis:** US082 acceptance criteria stand unchanged — any change to weather invalidates previous test results.

---

### C13 — Weather Data vs Air Control Areas: Altitude Range

**Date:** Sprint 3, Moodle forum
**Asked by:** Rodrigo Guerra

**Question:**
> "I was studying the Datasets for the weather information for the Air Control Area '121' and I can't
> help but notice that the information regarding the air control area has only the longitudinal and
> latitudinal boundaries. It was my understanding that air control areas would have geographic areas
> that are also bound by Altitude ranges, ranging from 0 to up to 14000m as per the information from
> the client on the 5th of May. Are we to assume that area 121 encompasses this entire range and is
> that also true for every single air control area?"

**Answer (Ângelo Martins):**
> The weather information it is not used to create Air Control Areas. The first sheet is provided
> just to help you understand the Air Control Area's boundaries.
> In your system, you don't read the first sheet. You have an Air Control Area (ACA) created in the
> system and you load the weather information, making sure that it matches the ACA's boundaries.

**Team decisions:**

1. **Decision:** Weather data is imported independently of ACA creation.
   **Basis:** Weather data is loaded separately and matched to existing ACAs — it does not create them.

2. **Decision:** The first sheet (ACA metadata) in the weather CSV is for human reference only — the system does NOT read it.
   **Basis:** The client explicitly said "you don't read the first sheet."

3. **Decision:** Weather data is validated against ACA geographic boundaries (latitude/longitude).
   **Basis:** The client confirmed the matching requirement.

4. **Decision:** All ACAs span 0 to maxAltitude (default 14,000 m) — altitude is not part of ACA boundary validation.
   **Basis:** Consistent with US050 and the client's earlier clarification.

---

### C14 — Simulation Report: Java-C Integration and Persistence (US111)

**Date:** Sprint 3, Moodle forum
**Asked by:** Silva

**Question:**
> "Regarding the generation of a simulation report:
> 1. Should the Java system (US111) simply read a file previously generated by the C program running
>    independently, or should the Flight Control Operator be able to trigger the C simulation directly
>    from the Java UI?
> 2. Does the generated summary report need to be saved in the Relational Database (NFR08) for future
>    historical consultation, or is it purely a transient file?"

**Answer (Ângelo Martins):**
> 1. Don't ask the client how to implement the software. Of course the client would be happy with
>    a seamless experience.
> 2. The user story says that a file should be stored in the file system. I wouldn't call it transient.

**Team decisions:**

1. **Decision:** The Java UI allows the operator to trigger the C simulation and read its output programmatically.
   **Basis:** The client implied a "seamless experience" — the operator should not manually copy files between systems.

2. **Decision:** The report file is stored permanently in the filesystem.
   **Basis:** The client explicitly expects persistence: "I wouldn't call it transient."

3. **Decision:** The Simulation aggregate stores both `filePath` and `content` as a CLOB in the database.
   **Basis:** NFR08 requires historical consultation — having both the file path and the content in the DB provides redundancy and easy querying.

4. **Decision:** File-based handoff is the integration boundary: Java writes JSON → C writes report.
   **Basis:** This existing pattern from Sprint 2 avoids tight coupling (no JNI or sockets) and matches how the C module operates.

---

### C15 — US080: Fuel Per Leg, Departure Times, and Leg Structure

**Date:** Sprint 3, Moodle forum (late May)
**Asked by:** Carlos Fernandes

**Question:**
> "US080 lists 'fuel quantity' as a single input field and does not mention legs. How should fuel
> quantity per leg (DSL semantic rules 3.4.5) reconcile with the single fuel field in US080? Should
> US080 model multiple legs (fuel per leg) or stay flat?"

**Answer (Ângelo Martins):**
> "Really? Have you read section 3.4.3? You have fuel quantity and the departure time for each leg."
>
> "You are not suggesting that we should store the fuel quantity both in the Flight domain entity and
> in the FlightPlan description (DSL). The fuel quantity it's used only for the simulation."
>
> "Regarding departure time for the flight (1st leg), I'm torn between doing it right and doing the
> right thing. Choosing the latter, in spite of having the information in the FlightPlan description
> (DSL), I believe departure time of the first leg is a property of a flight and one that will be used
> often. Thus, I would propose a duplication here, albeit with a verification in US085, so that
> incongruent information is detected."
>
> "Regarding sections 3.2 and 3.4, is it the first time you are reading the document? Didn't you need
> this for the second sprint?"

**Team decisions:**

1. **Decision:** US080 accepts departure time AND fuel quantity per leg, as specified in section 3.4.3.
   **Basis:** The client confirmed that section 3.4.3 defines these as per-leg fields — the UI must allow each leg to have its own fuel quantity and departure time.

2. **Decision:** Fuel quantity is stored exclusively in `FlightPlan.dslContent` (the DSL string). The `Flight` entity has no fuel field.
   **Basis:** The client explicitly rejected storing fuel in both places — fuel is used only for simulation, not for domain queries against the Flight entity.

3. **Decision:** The `Flight` entity stores `departureTime` (LocalDateTime) representing the first leg's departure time.
   **Basis:** The client proposed this intentional duplication "for doing the right thing" — departure time of the first leg is a frequently-queried flight property that justifies a direct field on Flight.

4. **Decision:** US085 cross-verifies `Flight.departureTime` against the first-leg departure time in the DSL.
   **Basis:** The client mandated this verification to detect incongruent information between the duplicated fields, ensuring data consistency.

5. **Decision:** Multi-leg support in US080 is optional for Sprint 3 — the UI may accept a single leg (direct flight) with one fuel value and one departure time.
   **Basis:** Section 3.2 ("only direct flights") was Sprint 2 material that the team is expected to know. The team may simplify to single-leg input without violating requirements, since the system already supports legs via the DSL structure.

---

## 2. Summary of Design Impacts

### Domain Model Changes (from Sprint 2 → Sprint 3)

| Change | Source | US |
|--------|--------|----|
| Pilot must have `Set<AircraftModelCode> certifications` | C07 | US075, US080, US085, US121 |
| FlightPlan stores `dslContent` as raw String | C03, C10 | US080, US081, US085 |
| FlightPlanStatus lifecycle: DRAFT → IN_TEST → TEST_PASSED / TEST_FAILED | C03, C14 | US085 |
| Route is NEVER physically deleted (soft-delete only) | C11 | US074 |
| Weather data is selected (not manually entered) by Pilot | C03, C12 | US082 |
| Multiple weather data sources supported via adapter/strategy | C04 | US042 |
| SimulationReport stores filePath + content (CLOB) | C14 | US111 |
| FlightPlanExporter converts domain → JSON for C simulator | C14 | US085 |
| SimulationRunner interface abstracts C invocation | C14 | US085 |
| `Flight` aggregate extended with `departureTime` (LocalDateTime) | C15 | US080, US085 |
| Fuel quantity stored in `FlightPlan.dslContent` **only** — `Flight` entity has no fuel field | C15 | US080 |
| US085 cross-verifies `Flight.departureTime` against first-leg departure in DSL | C15 | US085 |

### Integration Architecture — US085 Detailed Workflow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         US085 — Test/Validate Flight Plan                │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐     ┌──────────────────────┐     ┌───────────────────┐    │
│  │  US080/  │     │  EAPLI Domain        │     │  C Simulator      │    │
│  │  US081   │────>│  (Java)              │────>│  (SCOMP)          │    │
│  │          │     │                      │     │                   │    │
│  │ DRAFT FP │     │  Phase 1 — DSL Re-   │     │  1. Read JSON     │    │
│  │ (DSL +   │     │  validation (LPROG)  │     │  2. Physics sim   │    │
│  │  data)   │     │  Phase 2 — JSON      │     │  3. Detect viol   │    │
│  └──────────┘     │  Export → C Sim      │     │  4. Write report  │    │
│                   │  Phase 3 — Parse      │     └────────┬──────────┘    │
│                   │  Report → Record      │              │               │
│                   │  Result → Update      │<─────────────┘               │
│                   │  Status               │                              │
│                   └──────────────────────┘                               │
│                                                                          │
│  LPROG: Phase 1 calls FlightPlanRunner.run(dslContent)                   │
│  SCOMP: Phase 2 writes report_<flightId>_<timestamp>.txt                │
│  EAPLI: Phase 3 parses report, stores in Simulation aggregate + DB       │
└──────────────────────────────────────────────────────────────────────────┘
```

### Key Integration Points

| Step | Component | Description | Clarification |
|------|-----------|-------------|---------------|
| 1 | EAPLI | Load FlightPlan + verify Pilot authorization | C03 |
| 2 | LPROG | `FlightPlanRunner.run(dslContent)` — lexical + syntactic + semantic | C03, C10 |
| 3 | EAPLI | If DSL fails → set status TEST_FAILED, return DSL errors | C03 |
| 4 | EAPLI | `FlightPlanExporter.toJson(fp)` — convert domain to C JSON format | C14 |
| 5 | EAPLI → SCOMP | `ProcessBuilder` invokes C simulator with JSON + start time | C14 |
| 6 | SCOMP | Writes report to filesystem (not transient) | C14 |
| 7 | EAPLI | Reads report file, parses, stores in DB (filePath + content) | C14 |
| 8 | EAPLI | Updates FlightPlan.status to TEST_PASSED / TEST_FAILED | C03 |

---

## 3. Design Decisions (Final)

| # | Question | Decision | Rationale |
|---|----------|----------|-----------|
| 1 | FlightPlan standalone or embedded? | **Embedded inside Flight aggregate** | Professor confirmed: FlightPlan is a document of Flight, not an aggregate root |
| 2 | Package naming | `eapli.aisafe.flightplan.domain` | Not `flight` to avoid confusion with Flight aggregate |
| 3 | Pilot certifications — how do they reach the service? | Via `PilotRepository` — `Set<AircraftModelCode>` on Pilot | Professor suggested attribute on Pilot; no duplication in FlightPlan |
| 4 | Re-validate DSL always or cache? | **Always re-validate** | Grammar may change; weather data (US082) alters DSL; ANTLR is fast |
| 5 | Report persistence | **filePath + content (TEXT)** | C14 requires persistence; backup in DB + filesystem |
| 6 | Final database | **PostgreSQL** (already configured in persistence.xml) | Client recommended it; already in use |
| 7 | C invocation | **ProcessBuilder** (file-based handoff) | Decided since Sprint 2; no JNI/sockets |
| 8 | US086 RemotePilotService | Session-scoped (SystemUser in constructor) | One instance per TCP session |
| 9 | Report naming | `report_<flightId>_<timestamp>.txt` | Avoids collisions; identifies origin |
| 10 | US080 fuel storage | **Only in `FlightPlan.dslContent`** — `Flight` entity has no fuel field | C15: fuel is for simulation only; client rejected storing it in both places |
| 11 | `Flight.departureTime` | First-leg departure time IS stored in `Flight` (intentional duplication with DSL) | C15: client proposed this for frequently-queried flight property; US085 verifies consistency |
| 12 | US080 leg input | US080 UI accepts fuel + departure time **per leg** per section 3.4.3; single-leg (direct flight) sufficient for Sprint 3 | C15: client confirmed 3.4.3 applies; Sprint 2 "only direct flights" allows simplification |

---

## 4. References

| Document | Location |
|----------|----------|
| Sprint Planning | `docs/Sprint3/SCRUM/SprintPlanning.md` |
| Non-Functional Requirements | `docs/NonFunctionalRequirements.md` |
| Domain Model (Sprint 3) | `docs/Sprint3/EAPLI/us_010/domain_model_sprint3.puml` |
| C Simulator Code | `scomp/Sprint3/files/` |
| DSL Grammar | `aisafe.dsl/src/main/antlr4/aisafe/lprog/FlightPlan.g4` |
| Sprint 2 Clarifications | `docs/Sprint2/client_clarifications.md` |
| Project Specification | `docs/ProjectSpecification.md` (sections 3.2, 3.4) |
