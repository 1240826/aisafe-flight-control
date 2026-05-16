# Client Clarifications — Sprint 2

**Source:** Q&A forum with Product Owner (Angelo Martins)
**Captured:** 2026-05-06

---

## Table of Contents

1. [Repository Factory Wiring](#1-repository-factory-wiring)
2. [US031 — Valid Email Domains](#2-us031--valid-email-domains)
3. [US031 — Security Clearance and Skills Assessment Scope](#3-us031--security-clearance-and-skills-assessment-scope)
4. [US061 — Meaning of "Position"](#4-us061--meaning-of-position)
5. [US050 — Area Code Format and Name Uniqueness](#5-us050--area-code-format-and-name-uniqueness)
6. [US050 — Geographic Boundaries and No-Overlap Rule](#6-us050--geographic-boundaries-and-no-overlap-rule)
7. [US052 — Airport Coordinates Outside ACA](#7-us052--airport-coordinates-outside-aca)
8. [US060 — Uniqueness of Company Name, IATA, ICAO](#8-us060--uniqueness-of-company-name-iata-icao)
9. [US032 — Disabling a User: No Cascade to Collaborator](#9-us032--disabling-a-user-no-cascade-to-collaborator)
10. [US041 — Weather Data Time Period](#10-us041--weather-data-time-period)
11. [US055/056 — Aircraft Model ID Format and Manufacturer](#11-us055056--aircraft-model-id-format-and-manufacturer)
12. [US055/056 — Manufacturer: Dynamic List, Case-Insensitive](#12-us055056--manufacturer-dynamic-list-case-insensitive)
13. [Security Clearance Effect on Login](#13-security-clearance-effect-on-login)
14. [US056 — Fuel Type: Bootstrapped List](#14-us056--fuel-type-bootstrapped-list)
15. [US041 — Overlapping Weather Data (MVP Simplification)](#15-us041--overlapping-weather-data-mvp-simplification)
16. [US061 — FCO/WeatherPerson: ACA vs Flight Control Entity](#16-us061--fcoweatherperson-aca-vs-flight-control-entity)
17. [US052 — Town and Country Data](#17-us052--town-and-country-data)
18. [US072 — Aircraft Age and Capacity Definitions](#18-us072--aircraft-age-and-capacity-definitions)
19. [Flight Business Identifier](#19-flight-business-identifier)
20. [Official Simplifications Announcement (Portuguese)](#20-official-simplifications-announcement)

---

## 1. Repository Factory Wiring

**Q:** When adding a new aggregate's repository we created a JPA implementation, an in-memory implementation, and manually added a method to `RepositoryFactory` and both factory implementations. Is this correct?

**A (PO):** That approach is correct. See EAPLI TP6 "Sample Project: eCafeteria" for details and examples.

**Team decision:** Confirmed — for every new aggregate create `JpaXxxRepository`, `InMemoryXxxRepository`, and wire via `RepositoryFactory`.

---

## 2. US031 — Valid Email Domains

**Q:** US031 says users need an email from a list of valid domains. Should this list be bootstrapped, managed via a use case, or other?

**A (PO):** There is a list of valid AISafe domains for internal emails — load it in the bootstrap. ATCC's and pilots must use a valid company email. ATC collaborators (US061) may have emails outside the ATC domain. You cannot verify the domain of other users.

**Team decisions:**
- A predefined set of valid AISafe internal domains (e.g., `@aisafe.com`) is loaded during bootstrap.
- For ADMIN and BACKOFFICE_OPERATOR roles the controller validates the email against that bootstrapped list.
- For operational roles (ATCC, Pilot) the company email is enforced when the collaborator is linked (US061). ATC collaborators have no domain restriction.

---

## 3. US031 — Security Clearance and Skills Assessment Scope

**Q:** Do security clearance and skills assessment apply to ALL users (including Admins / Backoffice Operators), or only operational roles?

**A (PO):** Security clearance requirements apply to ALL users. Administrators and Backoffice Users can update them (in real life a user cannot update their own — simplified here).

**Follow-up Q:** Should expired clearance auto-deactivate the account?

**A (PO):** Security clearance has a validity date. **After that date the user cannot log in** — it does not mean the account is deactivated. Skills assessment has **no effect** on login ability.

**Team decisions:**
- US031 collects a `securityClearanceExpiryDate` (LocalDate) for **every** registered user.
- A companion entity `UserSecurityProfile` (linked by username, own repository) stores this date outside the framework's `SystemUser`.
- The login flow (US030) checks `securityClearanceExpiryDate >= today`; if expired → login rejected (not deactivated).
- Skills assessment date is stored but never blocks login.

---

## 4. US061 — Meaning of "Position"

**Q:** Does "position" in Air Transport Company context mean geographic location or professional role/authority?

**A (PO):** Position = Role.

**Team decision:** The `position` attribute on `Collaborator` represents the collaborator's professional role (e.g., "Senior ATC Officer", "Team Lead").

---

## 5. US050 — Area Code Format and Name Uniqueness

**Q:** Should the area code follow a specific format (uppercase, numeric, etc.)?

**A (PO):** The area code is internal — no specific format requirements beyond uniqueness. Furthermore, each area must have the **name** commonly used in air control operations. This name must also be unique.

**Team decisions:**
- `AreaCode` VO: non-null, non-empty string; no format restriction.
- `AirControlArea` gains a `name` attribute (plain String, non-empty).
- **Both `AreaCode` and `name` must be unique** — two separate controller uniqueness checks.

---

## 6. US050 — Geographic Boundaries and No-Overlap Rule

**Q:** Should coordinate validation include "reasonable" ranges? Any other boundary constraints?

**A (PO):**
- Standard lat/lon ranges are valid (no explicit tighter bound).
- **Air control areas CANNOT overlap** — must be enforced at creation time.
- **Coordinates must not be equal (zero-area rectangle)**.
- **Airport coordinates must lie inside the ACA they belong to** (see also US052).

**Official simplification (last page of PDF):**
- ACAs are always rectangular, from **altitude zero up to a configurable maximum**.
- Teams may assume default max altitude = **14 000 m** but **must not hardcode this** — it must be a configuration parameter or constructor argument.
- No overlapping ACAs allowed.

**Team decisions:**
- `AirControlArea` root gains `maxAltitudeMetres` (double/int), read from `Application.settings()` at creation; default 14 000 m.
- Controller calls `AirControlAreaRepository.findOverlapping(minLat, maxLat, minLon, maxLon)` before saving.
- Zero-area enforced by `Coordinates_ACA` invariant: `minLat < maxLat` AND `minLon < maxLon` (strict inequality, already in original docs).

---

## 7. US052 — Airport Coordinates Outside ACA

**Q:** If airport coordinates don't fall within the selected ACA should the system reject or redirect?

**A (PO):** Strict validation — violation of business rules. You cannot create the airport.

**Team decision:** Controller rejects creation if coordinates are not contained within the ACA rectangle. No guided workflow.

---

## 8. US060 — Uniqueness of Company Name, IATA, ICAO

**Q (1):** Is it the combination of name + IATA + ICAO that must be unique, or each field independently?

**A (PO):** Any of them must be unique — strange to have two companies with the same name.

**Q (2):** Is it the combination of IATA and ICAO that must be unique, or each independently?

**A (PO):** Any of them must be unique. No two airlines can have the same IATA or ICAO codes.

**Team decision:** `CompanyName`, `IATACode`, and `ICAOCode` are each individually unique — three separate controller checks.

---

## 9. US032 — Disabling a User: No Cascade to Collaborator

**Q:** Should disabling a SystemUser automatically remove or disable the linked Collaborator?

**A (PO):** "You are being 'too creative' and not thinking about the side effects. 'remove' was definitely the wrong expression."

**Team decision:** Disabling a `SystemUser` (US032) has **no effect** on the `Collaborator` aggregate. Disabling a collaborator (US064) is a separate, independent operation.

---

## 10. US041 — Weather Data Time Period

**Q:** Should weather data cover a whole day, an hourly slot, or a user-defined period?

**A (PO):** The user can define start and end time. Talking about predefined time slots doesn't make any sense.

**Team decisions:**
- `recordedDateTime` (single timestamp) is **replaced** by a `validFrom` / `validTo` pair of `LocalDateTime`.
- Both values are entered by the user. Invariant: `validTo > validFrom`.
- No constraint on minimum/maximum duration.

---

## 11. US055/056 — Aircraft Model ID Format and Manufacturer

**Q:** What format is the `ModelID`? Is Manufacturer a fixed list or a full entity?

**A (PO):** Model name examples: A320, A330-neo, B737 Max 8, B747, B777-ER. Manufacturers: Airbus, Boeing, Embraer, Bombardier, Cessna, Antonov, etc. **"Obviously, a manufacturer cannot be a VO."**

**Team decisions:**
- `Manufacturer` is a **full aggregate** (aggregate root, own repository, own persistence table).
- `ModelID` VO holds `modelName` (String) + `manufacturerId` reference only. No strict character format.
- `AircraftModel` and `EngineModel` reference `Manufacturer` by ID only (cross-aggregate boundary).

---

## 12. US055/056 — Manufacturer: Dynamic List, Case-Insensitive

**Q:** Should the system support adding new manufacturers? Should "Airbus" and "AIRBUS" be the same?

**A (PO):** The list cannot be static — new ones may be added. Bootstrap a predefined list for testing. And yes, "Airbus" and "AIRBUS" must be considered the same manufacturer.

**Team decisions:**
- `ManufacturerName` VO stores name; uniqueness check is **case-insensitive** (`findByNameIgnoreCase`).
- Bootstrap loads: Airbus, Boeing, Embraer, Bombardier, Cessna, Antonov.
- A "Register Manufacturer" use case is implicitly needed — treated as Sprint 2 infrastructure / bootstrapped for now. A dedicated US may be added if sprint scope permits.

---

## 13. Security Clearance Effect on Login

**Q:** Should users with expired security clearance have access automatically deactivated?

**A (PO):** Security clearance has a validity date. After that date the user is **not able to log into the system** — it does NOT mean the account is deactivated. Skills assessment expiry has **no effect** on login.

*(See also §3 — same clarification thread.)*

---

## 14. US056 — Fuel Type: Bootstrapped List

**Q:** Should fuel types (name, density, specific energy) be bootstrapped for selection, or entered manually per engine?

**A (PO):** Pre-loading available fuel types is a good solution for an MVP.

**Team decisions:**
- A `FuelType` concept (bootstrapped enum-like set: JET A-1, AVGAS 100LL, SAF, etc.) is loaded at startup.
- The UI presents the pre-loaded list for selection — no free-text entry.
- `fuelType` on `EngineModel` stores a `String` name corresponding to the selected bootstrapped type.

---

## 15. US041 — Overlapping Weather Data (MVP Simplification)

**Q:** Should overlapping weather data be allowed for the same ACA and time period?

**A (PO):** Very complex in real life (gaps, section overlaps, update nightmares). Use a simplified MVP model.

**Official simplification (PDF last page):**
- Weather data is given for **rectangular sub-areas of ACAs**; sub-areas must stay within the parent ACA boundary.
- Different altitude layers use the **same horizontal sub-areas** (parallelepiped slices); **no overlap between altitude layers**.
- During simulation, weather data switches when the aircraft enters a new sub-area/altitude layer.
- **Recommendation:** sub-areas should cover the full ACA (no gaps) to simplify simulation.

**Team decisions:**
- `WeatherData` gains a `WeatherSubArea` VO (minLat, maxLat, minLon, maxLon, minAlt, maxAlt).
- Controller checks the `WeatherSubArea` lies within the parent ACA boundary.
- Overlapping sub-areas for the same time/area are not explicitly blocked at MVP, but documented as a known simplification.

---

## 16. US061 — FCO/WeatherPerson: ACA vs Flight Control Entity

**Q:** Should FCO and WeatherPerson be associated with "air control area" or "flight control entity"?

**A (PO):** A Flight Control Operator is responsible for managing air traffic in **just one Air Control Area** — that's why US061 uses Air Control Area. Optionally, an FCO can manage more than one ACA.

**Team decision:** `FlightControlOperator` and `WeatherPerson` are associated with `AirControlArea` by `AreaCode` reference (one ACA per FCO/WeatherPerson for Sprint 2).

---

## 17. US052 — Town and Country Data

**Q:** Are Town and Country pre-loaded via bootstrap, or freely entered by the operator?

**A (PO):** Country data must be loaded via bootstrap. The city can be inserted when creating an airport.

**Team decisions:**
- `country` on `Airport` is selected from a bootstrapped list of countries.
- `town`/`city` on `Airport` is entered as free text by the operator.

---

## 18. US072 — Aircraft Age and Capacity Definitions

**Q:** What does "age" mean in US072d? What is "capacity" in US072c?

**A (PO):**
- US072d — "age" = age of the aircraft (derived from registration/commissioning date).
- US072c — "capacity" = number of passengers the plane can carry (total seats from cabin configuration).

**Team decision:** Aircraft age = `today − registrationDate`. Capacity = sum of `numberOfSeats` across all `SeatClass` VOs in `CabinConfiguration`.

---

## 19. Flight Business Identifier

**Q:** Is the flight identifier strictly numerical?

**A (PO):** The internal identifier does not matter. A flight is business-identified by **route name + day of flight**. A flight cannot happen twice on the same day (local time).

**Team decision:** Domain business key for `Flight` = `routeName` + `flightDate`. JPA surrogate key is implementation detail.

---

## 20. Official Simplifications Announcement

*(Message in Portuguese from Angelo Martins — translated and summarised below.)*

**Flight Simulation** *(DSL not affected — only simulation test cases)*
- Simulated flights take off and land at airports within the **same air control area**.
- A flight has exactly: one climb phase, one cruise phase, one descent phase.

**Weather Information**
- Given for **rectangular sub-areas** of ACAs; must remain within the ACA boundary.
- Different altitude layers use the same horizontal sub-areas (parallelepiped slices); **no overlap between layers**.
- During simulation, weather data changes when the aircraft position enters a new sub-area/layer.
- Sub-areas should cover the full ACA (no gaps recommended).

**Air Control Areas**
- Always rectangular, from **altitude zero to a configurable maximum**.
- Default max altitude = **14 000 m** — **must NOT be hardcoded**; use configuration / constructor parameter.
- **No overlapping ACAs** allowed.

---

---

## 21. US050/US052 — ACA Does Not Require Airports

**Q:** Must an Air Control Area have at least one airport to be valid?

**A (PO):** No — an ACA can exist without any airports. The relationship is from Airport → ACA (airport belongs to an ACA), not the other way around.

**Team decision:** `AirControlArea` has no back-reference to airports. Creating an ACA with no airports is perfectly valid.

---

## 22. US056 — Real Engine Names and Fuel Compatibility

**Q:** Are there real engine naming conventions we should follow? Can an engine model work with limited fuel types?

**A (PO):** Real engine names: Trent XWB, Trent 1000, Trent 7000, Trent 900, BR710, RB199 (Rolls-Royce family). Fuel types are extensible (not a fixed enum) — an engine model declares which fuel types it accepts, and the bootstrapped list can be extended. Engine models can work with a subset of available fuel types.

**Team decisions:**
- Engine names follow manufacturer conventions; examples bootstrapped: GE90-94B, Trent 970, CFM56-5B4, LEAP-1B.
- `fuelType` stored as free text String (selected from bootstrapped list in the UI); currently `EngineModel` stores a single `fuelType`.
- Fuel list for MVP: "Jet-A1", "AvGas 100LL", "SAF" (Sustainable Aviation Fuel).

---

## 23. US055/056 — Manufacturer Concept Shared by Aircraft and Engines

**Q:** Is the `Manufacturer` concept the same for aircraft models and engine models?

**A (PO):** Yes — the manufacturer concept is the same. A manufacturer can produce both airframes and engines (e.g., GE Aviation makes engines; Boeing makes airframes).

**Team decision:** A single `Manufacturer` aggregate is used as the cross-aggregate reference for both `AircraftModel` and `EngineModel`. Both store `manufacturerName` as a String reference.

---

## 24. US061 — Propeller Engines: Power + Propeller = Thrust

**Q:** For turboprop and propeller engines, is "power" the only relevant output metric?

**A (PO):** For propeller-driven engines, thrust comes from power + propeller configuration — not from power alone.

**Team decision:** The `Power` VO (kW or shaft HP) is retained for turboprop/electric engines. The `Thrust` VOs (static and cruise) represent derived thrust at the propeller shaft. No separate propeller configuration VO in Sprint 2 — noted as future enhancement.

---

## 25. US061 — Collaborator Works in One ACA; ACA is the Customer

**Q:** Can a collaborator (FCO or WeatherPerson) work in multiple ACAs at the same time?

**A (PO):** A collaborator works in one ACA at a time. The ACA is the "customer" in the system. A company may own or manage several ACAs.

**Team decisions:**
- `FlightControlOperator` and `WeatherPerson` reference exactly one `AreaCode` (as implemented).
- Moving a collaborator to a different ACA would require editing the collaborator (US063).
- `AirTransportCompany` is distinct from ACA — a company may be associated with multiple ACAs (future US).

---

## 26. US072 — Aircraft Decommission: Only Validated Flight Plans Count

**Q:** When decommissioning an aircraft, should the check consider draft flight plans or only validated ones?

**A (PO):** Only consider validated flight plans — drafts should not block decommissioning.

**Team decision:** The decommission controller fetches flight plans linked to the aircraft and filters for status = VALIDATED. Only if there are validated future flights is the decommission rejected. Draft plans are ignored.

---

## 27. Bootstrap Test Data Responsibility

**Q:** Who is responsible for defining ACA test data for Sprint 2 demos?

**A (PO):** That is the team's responsibility.

**Team decision:** The team bootstraps 23 real-world-inspired Air Control Areas (FIR/ARTCC boundaries), 50 airports, 8 manufacturers, 6 engine models, 5 aircraft models, 10 air transport companies, and 6 collaborators. All seeded via `AISafeDemoDataBootstrapper` (run with `-bootstrap:demo`).

---

## 28. Database Technology — H2 in Docker Acceptable

**Q:** Can we use H2 in a Docker container? Would PostgreSQL + PostGIS be better for coordinates?

**A (PO):** H2 in Docker is acceptable for Sprint 2. PostgreSQL + PostGIS would be a clever choice for handling geographic coordinate queries natively (e.g., finding airports within an ACA boundary using spatial indexes).

**Team decision:** Sprint 2 uses H2 file-based (`jdbc:h2:~/aisafe`). Migrating to PostgreSQL + PostGIS is noted as a future improvement, especially relevant for US050 overlap checks and US052 coordinate validation.

---

## Summary of Documentation Changes per US

| US | Change Required | Status |
|----|----------------|--------|
| US031 | Add SecurityClearance expiry for ALL users; email domain bootstrap | Updated |
| US041 | Replace `recordedDateTime` → `validFrom`/`validTo`; add `WeatherSubArea` VO | Updated |
| US050 | Add `name` (unique); add `maxAltitudeMetres` (configurable); overlap check in SD | Updated |
| US052 | Country bootstrapped; city free text | Updated |
| US055 | Manufacturer = full aggregate, case-insensitive unique name | Updated |
| US056 | `fuelType` selected from bootstrapped list | Updated |
| US060 | Already correct — 3 independent uniqueness checks | No change |
| US032 | Already correct — disable user ≠ disable collaborator | No change |
| US061 | Already correct — FCO/WeatherPerson linked to AirControlArea | No change |
| US064 | Already correct — Collaborator disable separate from SystemUser | No change |
