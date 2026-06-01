# Sprint 3 — Client Clarifications & Impact Analysis

> **Purpose:** Consolidate all client clarifications received during Sprint 3 (Moodle forum)
> and document their impact on design decisions, ensuring EAPLI, LPROG, RCOMP, and SCOMP
> components integrate coherently.

---

## 1. Client Clarifications

### C01 — NFR08: Database — PostgreSQL vs H2 in Docker

**Date:** Sprint 3, Moodle forum
**Asked by:** Munaf

**Question:** "According to NFR08, we understand that the system must support both 'in-memory' persistence and a relational database (RDBMS), and that the final deployment must use a remote persistent relational database. Is it mandatory to use an external persistent relational database such as PostgreSQL/MySQL in the final deployment? Or would it be sufficient to use H2 in server/file-based mode (for example hosted in a Docker container), as long as it remains a persistent relational solution?"

**Answer (Ângelo Martins):**
> In the sprint review of sprint 3 you are expected to use a RDBMS, at least in LAPR4. Using H2 in server mode in a docker is a possibility.
> On the other hand, using PostgreSQL with PostGIS would be a clever way to simplify queries with coordinates.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| PostgreSQL with PostGIS is the **recommended** choice for final deployment | Simplifies coordinate-based queries (weather data within ACA bounds, route segments) |
| H2 in server mode (Docker) is **acceptable** as a fallback for simpler setups | Client explicitly permits it |
| In-memory mode (H2 fileless) continues to be used for development/testing | Existing pattern from Sprint 2 |
| Database choice should be configurable via properties (NFR08 "by configuration") | Allows switching between in-memory, H2 server, and PostgreSQL |

---

### C02 — CSV Weather Data: Provided or Self-Created

**Date:** Sprint 3, Moodle forum
**Asked by:** Rodrigo Guerra

**Question:** "Regarding the section of this user story where it says 'a simple CSV file format will be available', will this CSV file be provided by the client? Or are we (meaning my group) supposed to develop our own CSV containing weather data pertaining to our implementation of Weather Data in our project? If this is not the case, what would be the best approach if the CSV provided by outside sources doesn't have enough information for creating a valid weather data in our system?"

**Answer (Ângelo Martins):**
> I'll give you an example tomorrow. You may have to create others for testing purposes.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| The client will provide at least one example CSV | Sufficient for initial development and understanding of the format |
| Each team must create additional CSVs for testing | Coverage of edge cases, boundary values, and invalid data |
| The CSV import module must be flexible enough to accept different CSV structures | US042 says "easy to expand to new weather data sources" |

---

### C03 — US080/US081/US085: Workflow Confirmation

**Date:** Sprint 3, Moodle forum
**Asked by:** Monteiro

**Question:** "Regarding US080, US081 and US085, we would like to confirm our interpretation of the intended workflow. Our understanding is:
- US080: the Pilot enters the flight plan data manually through the UI, including legs and segments. The system generates the DSL from this data and stores it in the flight plan as a draft.
- US081: the Pilot provides a DSL file. The system reads it, validates it, and creates a flight plan directly from the file. The DSL is stored in the flight plan.
- US082: weather data is inserted into the segments of the flight plan, enriching the DSL with wind information before simulation.
- US085: the system validates the DSL already stored in the flight plan (whether it came from US080 or US081) and runs the C simulator.

Is this interpretation correct?"

**Answer (Ângelo Martins):**
> In US080 you don't need to read the flight plan line by line. You can, if you want, but you can also read it as a string or even from a text file. You don't validate it.
>
> In US082 it would be strange and burdensome to make the pilot to manually load data for the flight. That information it's already in the system and pilots are expensive (hourly cost). So, I believe it makes more sense to allow the pilot to select the weather information he/she wants to use in the flight plan. This is aligned with US043.
>
> Please take in account that US042 hints at the possibility that there may be multiple weather data providers, thus there may be multiple weather data information for the same area at a given time.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| US080 accepts DSL as a **raw string** (from text input, file, or UI fields) — no validation | Client explicitly says "read it as a string or even from a text file. You don't validate it" |
| US080 is NOT a form-based UI for legs/segments — it accepts the DSL text directly | The original question assumed form-based entry; client corrected this |
| US081 requires **full DSL validation** pipeline before creating the plan | Different from US080 — validation IS required |
| US082 presents **existing weather data** for selection; no manual entry by pilot | "Pilots are expensive" — aligns with US043 (consult weather data) |
| US085 **re-runs DSL validation** on stored DSL AND invokes C simulator | Two-phase validation confirmed |
| Multiple weather data may exist for same area/time — pilot chooses | US042 supports multiple providers |

---

### C04 — Weather Data Sources: Files vs API

**Date:** Sprint 3, Moodle forum
**Asked by:** Vieira

**Question:** "What do you understand by 'weather data sources'? (Are they simply different file formats or the name of enterprises/entities that we have collected the data from?) How would the import work? Would this bulk import consist of only files or would it be an API call to another system?"

**Answer (Ângelo Martins):**
> The bulk import may use files or API calls to another systems. The support for the provided files will be mandatory.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| CSV file import is **mandatory** for Sprint 3 | Client explicitly requires support for provided files |
| API-based import is **optional** but the design should anticipate it | "May use files or API calls" — design for extensibility |
| Weather data import module should use a strategy/adapter pattern | Allows swapping between file-based and API-based sources |

---

### C05 — US075: Pilot vs Collaborator (Duplication with US061)

**Date:** Sprint 3, Moodle forum
**Asked by:** Silva

**Question:** "US75 asks to add a pilot to the ATCC company; however, the project requirements considers a pilot to be an collaborator. Therefore, in US61, if the collaborator to be added is a pilot, both US61 and US75 fulfill the same requirement in a similar manner. My question concerns the distribution of responsibilities for a 'Backoffice Operator': should this user be able to add pilots to a specific company?"

**Answer (Ângelo Martins):**
> I believe you are not understanding the concept of "role". US 075 is cristal clear.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| US075 is a distinct US — do not merge with US061 | Client was emphatic: "crystal clear" |
| The Pilot role is separate from generic Collaborator | US075 specifically addresses adding a Pilot (not a generic collaborator) |
| A Backoffice Operator may add collaborators (US061); adding pilots may require different authorization | Must respect role segregation |

---

### C06 — RCOMP: DEI Infrastructure vs External Cloud

**Date:** Sprint 3, Moodle forum
**Asked by:** Mariana Gabriel

**Question:** "Relativamente às user stories de RCOMP, o hosting dos serviços remotos e da base de dados deve obrigatoriamente utilizar infraestrutura disponibilizada pelo DEI, ou podemos utilizar uma solução cloud externa à nossa escolha desde que cumpra os requisitos do projeto?"

**Answer (Ângelo Martins):**
> You can use external infrastructure, but you will have to cover those expenses.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Teams may choose DEI infrastructure or external cloud (e.g., AWS, Azure, GCP) | Client permits external infrastructure |
| External cloud costs are the team's responsibility | Budget consideration |
| DEI infrastructure is the default recommended path | No additional costs |

---

### C07 — Pilot Licenses / Certifications

**Date:** Sprint 3, Moodle forum
**Asked by:** Cardoso

**Question:** "In real-world aviation, pilots have licenses that include categories, classes, flight hours, and aircraft-specific ratings and certifications. For our implementation, should we model these distinctions, or can we simplify the system by assuming that all pilots are equally qualified to fly any aircraft?"

**Answer (Ângelo Martins):**
> US075 says "A pilot is certified to pilot one or more aircraft models". That means that a pilot has a list of flight models that he/she can fly.
> Saying that a pilot can fly any plane is way out of the requirements.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Pilot must have a **list of AircraftModel** they are certified to fly | US075 explicitly states this |
| A pilot CANNOT fly any aircraft — certification is per model | Client considered the opposite "way out of requirements" |
| US080 and US121 must validate that the pilot is certified for the assigned aircraft | Cross-reference validation is required by this clarification |
| The `Pilot` aggregate must include `Set<AircraftModelCode> certifications` | New field needed |

---

### C08 — Multiple Routes with Same Origin-Destination

**Date:** Sprint 3, Moodle forum
**Asked by:** Henri Fontes

**Question:** "Should an air transport company have flight routes that have the same pair starting-ending airports (with different names, of course) (e.g. a company that has two flight routes from Lisbon to Porto)? Does it makes sense in the context of the project?"

**Answer (Ângelo Martins):**
> I believe I've explained before what is a route: [link to previous post]
> You should read the forum before asking new questions. If you have any question related to a previous post, ask questions on that conversation.

> *(from earlier post, referenced here)* One of the routes Paris (ORY)-Rome (FCO) by Transavia is TO3910. Some flights on this route:
> Tuesday, 24th March 2026 - 06:45 - 08:50
> Wednesday, 25th March 2026 - 09:15 - 11:25
> ...
> Another route Paris (ORY)-Rome (FCO) by Transavia is TO3914.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Route identity = **route name** (flight number), NOT origin-destination pair | TO3910 and TO3914 are both ORY-FCO but different routes |
| Multiple routes with same O/D pair are valid | Already implemented in `FlightRoute` — name is the identity |

---

### C09 — Route Modeling: Regular vs Charter (Reuse Routes)

**Date:** Sprint 3, Moodle forum
**Asked by:** Tavares

**Question:** "Currently we consider that a route represents a fixed path and flights are instances associated with that route. When a route Porto→Lisbon already exists with a regular flight plan, and a charter flight is needed for the same route and schedule, should we: (1) create a new route + flight plan for the charter, or (2) reuse the same route and associate the type to the flight plan?"

**Answer (Ângelo Martins):**
> One of the routes Paris (ORY)-Rome (FCO) by Transavia is TO3910. Some flights on this route:
> Tuesday, 24th March 2026 - 06:45 - 08:50
> Wednesday, 25th March 2026 - 09:15 - 11:25
> Thursday, 26th March 2026 - 09:55 - 12:00
> Friday, 26th March 2026 - 06:15 - 08:20
> Another route Paris (ORY)-Rome (FCO) by Transavia is TO3914.
>
> So recycling is very good for the environment, but it doesn't apply to this situation.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Each flight plan creates or references its own route (route name = flight number) | Different flights = different routes even if same O/D |
| A route is NOT reused across different flights | "Recycling doesn't apply" |
| FlightPlan references a FlightRoute by identity (not inline) | Existing pattern — route is a separate aggregate |

---

### C10 — US080 vs US081: DSL Difference

**Date:** Sprint 3, Moodle forum
**Asked by:** Sousa

**Question:** "Should the Flight Description DSL include the pilot (and other mandatory attributes) to be imported in US081 to create a flight plan? If the US080 lets the pilot input the DSL as string, what would the difference to US081?"

**Answer (Ângelo Martins):**
> Please read section 3.4.
> If you read the two user stories, you can see that US081 and US080 are quite different. For example, US081 requires the validation of the flight plan description. The same it's not required in US080.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| US080 accepts DSL input without any validation | Status: DRAFT directly |
| US081 validates DSL (lexical → syntactic → semantic) before creating flight plan | Status: DRAFT only after passing validation |
| Both may accept a DSL file or string — the difference is the VALIDATION STEP | Reinforces C03 |
| US080 and US081 share the same FlightPlan aggregate | One domain model, two input paths |

---

### C11 — Route Deactivation: Delete vs Soft-Delete

**Date:** Sprint 3, Moodle forum
**Asked by:** António Alves

**Question:** "Regarding the deactivation of a route. Should the information of a deactivated route be kept in the database with a deactivation parameter, like a deactivation date, or should we delete the route completely? If the option is the complete deletion of the route, this raises a question in the case in which there are planned flights for a given route that is to be deleted after the current date."

**Answer (Ângelo Martins):**
> The simple fact that it came to your mind that you could actually delete (erase) a route from the DB makes me sad. I was responsible for BDDAD and how do you think I feel when a former students suggests that?

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Routes are **never physically deleted** (no DELETE SQL) | Client was emphatic: deleting from DB "makes me sad" |
| Use **soft-delete / deactivation** with a boolean flag or deactivation date | Standard practice in aviation systems |
| Existing flight plans that reference a deactivated route remain valid | Referential integrity must be preserved |
| New flight plans cannot be created on a deactivated route | Application-level validation |

---

### C12 — Weather Data Insertion: Manual Selection, Missing Data, Re-entry

**Date:** Sprint 3, Moodle forum
**Asked by:** Silva

**Question:** "Regarding the insertion of weather data onto a flight plan by a Pilot:
1. Is the data entry manual or automatic?
2. What if weather data is not yet available for the flight's area/date?
3. If the pilot re-enters the exact same weather report, does the test become void?"

**Answer (Ângelo Martins):**
> I believe I've said something about this before. No pilot would accept to have to insert a ton of weather data manually into the system. He only has to choose one (or more according to the flight schedule and air control areas involved) of the available weather forecasts.
>
> If there is no available weather forecast then the pilot can't choose one, isn't it?
>
> There is a file with weather forecasts in Moodle.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Pilot **selects** from available weather forecasts (no manual data entry) | Consistent with C03 and US043 |
| If no forecast exists for the area/time, the pilot **cannot select** one | System must make this visible |
| There is a weather forecast file available on Moodle — must be imported via US042 | Reference data for testing/demo |
| If weather data is changed (even to same data), test is voided | US082 acceptance criteria stand — unchanged |

---

### C13 — Weather Data vs Air Control Areas: Altitude Range

**Date:** Sprint 3, Moodle forum
**Asked by:** Rodrigo Guerra

**Question:** "I was studying the Datasets for the weather information for the Air Control Area '121' and I can't help but notice that the information regarding the air control area has only the longitudinal and latitudinal boundaries. It was my understanding that air control areas would have geographic areas that are also bound by Altitude ranges, ranging from 0 to up to 14000m as per the information from the client on the 5th of May. Are we to assume that area 121 encompasses this entire range and is that also true for every single air control area?"

**Answer (Ângelo Martins):**
> The weather information it is not used to create Air Control Areas. The first sheet is provided just to help you understand the Air Control Area's boundaries.
> In your system, you don't read the first sheet. You have an Air Control Area (ACA) created in the system and you load the weather information, making sure that it matches the ACA's boundaries.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| Weather data is **imported independently** of ACA creation | Weather data is loaded separately and matched to existing ACAs |
| The first sheet (ACA metadata) in the weather CSV is for **human reference only** — system does NOT read it | Client explicitly says "you don't read the first sheet" |
| Weather data must be validated against ACA geographic boundaries (lat/lon) | Client confirms the matching requirement |
| ACAs span 0 to maxAltitude (default 14,000 m) — altitude is not part of ACA boundary validation | Consistent with US050 |

---

### C14 — Simulation Report: Java-C Integration and Persistence (US111)

**Date:** Sprint 3, Moodle forum
**Asked by:** Silva

**Question:** "Regarding the generation of a simulation report:
1. Should the Java system (US111) simply read a file previously generated by the C program running independently, or should the Flight Control Operator be able to trigger the C simulation directly from the Java UI?
2. Does the generated summary report need to be saved in the Relational Database (NFR08) for future historical consultation, or is it purely a transient file?"

**Answer (Ângelo Martins):**
> 1. Don't ask the client how to implement the software. Of course the client would be happy with a seamless experience.
> 2. The user story says that a file should be stored in the file system. I wouldn't call it transient.

**Impact:**

| Decision | Rationale |
|----------|-----------|
| The Java UI should allow the operator to **trigger the C simulation** and read its output | "Seamless experience" — the operator shouldn't manually copy files |
| The report file is stored **permanently** in the filesystem | Client explicitly expects persistence (not transient) |
| The Simulation aggregate stores `filePath` AND `content` as a CLOB in the DB | Historical consultation (NFR08) + filesystem backup |
| File-based handoff is the integration boundary (Java writes JSON → C writes report) | Existing pattern from Sprint 2 |

---

## 2. Summary of Design Impacts

### Domain Model Changes (from Sprint 2 → Sprint 3)

Based on the clarifications, the following changes to the domain model are needed:

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

Decisions taken after team discussion:

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

---

## 4. References

| Document | Location |
|----------|----------|
| Sprint Planning | `docs/Sprint3/SCRUM/SprintPlanning.md` |
| Non-Functional Requirements | `docs/NonFunctionalRequirements.md` |
| Domain Model (Sprint 3) | `docs/Sprint3/EAPLI/us_010/domain_model_sprint3.puml` |
| C Simulator Code | `scomp/Sprint3/files/` |
| DSL Grammar | `aisafe.dsl/src/main/antlr4/aisafe/lprog/FlightPlan.g4` |
