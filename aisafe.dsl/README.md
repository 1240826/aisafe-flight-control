# Flight Plan DSL

**Module:** `aisafe.dsl` | **USs:** US081, US083, US120, US121  
**Grammar:** `src/main/antlr4/FlightPlan.g4`

---

## 1. What this module does

Implements the **Core Flight DSL** so that flight plan files (`.flightplan`) can be formally
defined, loaded, and validated by the AISafe system (US081 + US083).

Every file is processed through **three phases**:

| Phase | Tool | Catches |
|-------|------|---------|
| 1 — Lexical | `FlightPlanLexer` + `FlightPlanErrorListener` | unknown characters, malformed tokens |
| 2 — Syntactic | `FlightPlanParser` + `FlightPlanErrorListener` | missing blocks, wrong order, bad keywords |
| 3 — Semantic | `SemanticValidationListener` via `ParseTreeWalker` | duplicate IDs, invalid routes, impossible dates |

A file only passes to the next phase if the previous one produced no errors. Only files that
clear all three phases are accepted for import (US081: *"only valid flight plans may be
imported and used by the system"*).

Error format (all phases):
```
[LEXER]    line 19:22 - token recognition error at: '#'
[PARSER]   line 22:8  - missing ';' at '}'
[SEMANTIC] line 6     - [R5] route origin 'OPO' must match first leg departure airport 'LIS'
```

**Multiple file formats (US081):** US081 acknowledges that multiple formats may exist.
The current implementation covers the Core Flight DSL format as required by US083. Other
formats can be added in future sprints by plugging in additional lexer/parser pairs — the
`FlightPlanRunner.run(Path, boolean)` call site is format-agnostic. Any non-conformant file
fails at Phase 1 or 2 with located errors before any data reaches the system.

---

## 2. Flight plan structure

The informal structure of every flight plan file:

```
─ Flight ID
─ Type (regular | charter)
─ Route
─ Leg (1)
    ─ Departure  (airport + schedule + time)
    ─ Arrival    (airport + time)
    ─ Fuel
    ─ Segments (1..n)
─ Leg (2)
─ ...
─ Leg (n)
```

**Charter flight** — specific date per leg, for one-off trips:

```
// Porto → Warsaw via Frankfurt  (multi-leg charter, file: valid_multi_leg.flightplan)

flight TP5678 : charter {

    route {
        origin      : OPO;
        destination : WAW;
    }

    aircraft : CS-TUB;
    pilot    : P12345;

    leg {
        departure { airport: OPO; datetime: 2026-06-15T06:00+01:00; }
        arrival   { airport: EDDF; datetime: 2026-06-15T09:30+02:00; }
        fuel      { quantity: 18500 kg; }
        segment {
            from      : (41.2481, -8.6814);
            to        : (50.0333,  8.5706);
            altitudes : [11000 m WIDTH 80 m];
        }
    }

    leg {
        departure { airport: EDDF; datetime: 2026-06-15T11:00+02:00; }
        arrival   { airport: WAW;  datetime: 2026-06-15T13:15+02:00; }
        fuel      { quantity: 12000 kg; }
        segment {
            from      : (50.0333,  8.5706);
            to        : (52.1657, 20.9671);
            altitudes : [10000 m WIDTH 60 m];
        }
    }
}
```

**Regular flight** — day of week per leg, for repeating weekly schedules:

```
// Lisbon → Madrid → Paris  (regular weekly, file: valid_regular_multi_leg.flightplan)
// Multiple schedules: Monday, Wednesday, Friday at 07:00

flight TP3000 : regular {

    route {
        origin      : LIS;
        destination : CDG;
    }

    aircraft : CS-TUB;
    pilot    : P12345;

    leg {
        departure { airport: LIS; day: Monday;   datetime: 2026-05-18T07:00+01:00;
                                   day: Wednesday; datetime: 2026-05-20T07:00+01:00;
                                   day: Friday;    datetime: 2026-05-22T07:00+01:00; }
        arrival   { airport: MAD; datetime: 2026-05-18T09:30+02:00; }
        fuel      { quantity: 8000 kg; }
        segment {
            from      : (38.7813, -9.1359);
            to        : (40.4983, -3.5676);
            altitudes : [10000 m WIDTH 60 m];
        }
    }

    leg {
        departure { airport: MAD; day: Monday;   datetime: 2026-05-18T11:00+02:00;
                                   day: Wednesday; datetime: 2026-05-20T11:00+02:00;
                                   day: Friday;    datetime: 2026-05-22T11:00+02:00; }
        arrival   { airport: CDG; datetime: 2026-05-18T13:30+02:00; }
        fuel      { quantity: 9000 kg; }
        segment {
            from      : (40.4983, -3.5676);
            to        : (49.0097, 2.5479);
            altitudes : [11000 m WIDTH 60 m];
        }
    }
}
```

---

## 3. Grammar — rule by rule

One **combined ANTLR4 grammar** (`grammar FlightPlan;`) — lexer and parser in a single file. This generates `FlightPlanLexer`, `FlightPlanParser`, `FlightPlanBaseListener` and `FlightPlanBaseVisitor` all under the `FlightPlan` prefix without needing separate grammar files.

### 3.1 Keywords — case-insensitive via inline character classes

```antlr
FLIGHT : [fF][lL][iI][gG][hH][tT] ;
LEG    : [lL][eE][gG] ;
// 20 keywords total
```

The specification requires keywords to be case-insensitive. Each letter position uses a character class matching both cases (`[fF]`), so `flight`, `FLIGHT`, `Flight` all produce the same token. Keywords: `FLIGHT`, `LEG`, `ROUTE`, `DEPARTURE`, `ARRIVAL`, `FUEL`, `SEGMENT`, `AIRPORT`, `DATETIME`, `DAY`, `ORIGIN`, `DESTINATION`, `FROM`, `TO`, `ALTITUDES`, `WIND`, `QUANTITY`, `WIDTH`, `REGULAR`, `CHARTER`, `AIRCRAFT`, `PILOT`.

### 3.2 Identifiers

```antlr
IDENTIFIER : [a-zA-Z][a-zA-Z0-9_\-]* ;
```

Case-sensitive, starts with a letter, allows `_` and `-` inside (e.g., `TP1234`, `SEG-1`, `L_1`). Keywords are declared before `IDENTIFIER` — on a tie, ANTLR takes the first matching rule, so `flight` matches `FLIGHT`, not `IDENTIFIER`. A longer word like `flight1` (7 chars) beats `FLIGHT` (6 chars), so it becomes an identifier.

### 3.3 Airport codes — ICAO before IATA

```antlr
ICAO_CODE : [A-Z][A-Z][A-Z][A-Z] ;   // 4 uppercase letters
IATA_CODE : [A-Z][A-Z][A-Z] ;        // 3 uppercase letters
```

`ICAO_CODE` must be declared first. For `EDDF` (4 chars), maximal munch picks the 4-char ICAO match; if IATA came first, `EDDF` would tokenise as `EDD` (IATA) + `F` (identifier) — wrong. Lowercase codes fall through to `IDENTIFIER` and the parser rejects them.

### 3.4 Numbers

```antlr
NUMBER : '-'?[0-9]+('.'[0-9]+)? ;
```

The optional leading `-` is required for negative coordinates (Lisbon longitude: −9.1359). Without it, negative coordinates would require a subtraction expression, which is out of scope.

### 3.5 Timestamps and Day-of-week

```antlr
TIMESTAMP
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
      'T'
      [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])?
      ( 'Z' | ('+' | '-') [0-9][0-9] ':' [0-9][0-9] )
    ;

DAY_OF_WEEK
    : [mM][oO][nN][dD][aA][yY]
    | [tT][uU][eE][sS][dD][aA][yY]
    | [wW][eE][dD][nN][eE][sS][dD][aA][yY]
    | [tT][hH][uU][rR][sS][dD][aA][yY]
    | [fF][rR][iI][dD][aA][yY]
    | [sS][aA][tT][uU][rR][dD][aA][yY]
    | [sS][uU][nN][dD][aA][yY]
    ;
```

`TIMESTAMP` locks in ISO 8601 (`YYYY-MM-DDTHH:MM+HH:MM`) with mandatory timezone offset. Any other format (e.g., `10-05-2026T08:00`) tokenises as plain `NUMBER` tokens and the parser rejects it. The optional `:SS` seconds component allows both `HH:MM` and `HH:MM:SS` precision. Calendar validity (Feb 30, month > 12, hour > 23, etc.) is a semantic check — rule R10. `DAY_OF_WEEK` matches all seven English day names case-insensitively and must be declared before `IDENTIFIER` so that `Monday` is not lexed as an identifier.

### 3.6 Units — `UNIT_` prefix and compound-before-simple ordering

```antlr
UNIT_MS  : 'm/s' ;   // before UNIT_M
UNIT_M   : 'm' ;
UNIT_KMH : 'km/h' ;  // before UNIT_KM
UNIT_KM  : 'km' ;
```

Units are lowercase-only — the specification lists them as a separate lexical element (not keywords), and SI/aviation notation always uses lowercase. `UNIT_` prefix avoids naming conflicts with other tokens. Compound units (`UNIT_MS`, `UNIT_KMH`) must come before their shorter prefixes so maximal munch picks the full token: `m/s` → `UNIT_MS`, not `UNIT_M` + rest.

### 3.7 Whitespace and Comments

```antlr
WS            : [ \t\r\n]+    -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
```

`-> skip` discards the token — the parser never sees whitespace or comments. `LINE_COMMENT` and `BLOCK_COMMENT` are **extensions** (see 4).

### 3.8 File and Flight

```antlr
flightFile : flightDecl EOF;

flightDecl
    : FLIGHT flightId COLON flightType LBRACE
          routeDecl
          AIRCRAFT COLON IDENTIFIER SEMI
          PILOT    COLON IDENTIFIER SEMI
          legDecl+
      RBRACE
    ;

flightId   : IDENTIFIER ;
flightType : REGULAR | CHARTER ;
```

`flightDecl EOF` enforces exactly one flight per file — an empty file or a file with multiple flights fails at parse time. `route` is at flight level, not inside each leg: semantic rules R5/R6 reference the *first* and *last* leg airports, proving that `route` is a shared concept across all legs. `REGULAR | CHARTER` are the only two valid types — anything else produces `mismatched input 'cargo' expecting {REGULAR, CHARTER}`.

### 3.9 Route

```antlr
routeDecl
    : ROUTE LBRACE
          ORIGIN      COLON airportCode SEMI
          DESTINATION COLON airportCode SEMI
      RBRACE
    ;

airportCode : IATA_CODE | ICAO_CODE ;
```

`airportCode` has two alternatives without labels — the visitor resolves them with `ctx.IATA_CODE() != null`. Semantic rules R5/R6 check that origin and destination match the actual first and last leg airports.

### 3.10 Leg

```antlr
legDecl
    : LEG LBRACE
          departureDecl
          arrivalDecl
          fuelDecl
          segmentDecl+
      RBRACE
    ;
```

The four sub-blocks are mandatory and ordered — departure then arrival then fuel then segment(s). `segmentDecl+` catches a missing segment at parse time (`mismatched input '}' expecting SEGMENT`) before any semantic check runs. Legs are identified by their sequential position in the file (first `leg {}` = leg 1, second = leg 2, etc.).

### 3.11 Departure and Arrival

```antlr
departureDecl
    : DEPARTURE LBRACE
          AIRPORT COLON airportCode  SEMI
          departureSchedule
      RBRACE
    ;

departureSchedule
    : DATETIME COLON TIMESTAMP SEMI                                    // charter: exact datetime
    | daySchedule+                                                     // regular: one or more day + datetime pairs
    ;

daySchedule
    : DAY COLON DAY_OF_WEEK SEMI DATETIME COLON TIMESTAMP SEMI
    ;

arrivalDecl
    : ARRIVAL LBRACE
          AIRPORT COLON airportCode SEMI
          arrivalSchedule
      RBRACE
    ;

arrivalSchedule
    : DATETIME COLON TIMESTAMP SEMI
    ;
```

`departureSchedule` supports two formats depending on flight type:
- **Charter**: `datetime: YYYY-MM-DDTHH:MM+HH:MM;` — a specific date and time with timezone offset.
- **Regular**: one or more `day: DayOfWeek; datetime: YYYY-MM-DDTHH:MM+HH:MM;` pairs — each pair describes a weekly schedule instance (e.g., Monday at 07:00, Wednesday at 07:00). Each `datetime` carries its own timezone for correct cross-timezone flight duration.

Semantic rule R11 enforces that charter flights use `datetime:` only, and regular flights use `day: + datetime:`. Arrival always uses `datetime:` only — the destination timezone is required.

### 3.12 Fuel

```antlr
fuelDecl
    : FUEL LBRACE
          QUANTITY COLON numericValue SEMI
      RBRACE
    ;
```

Single quantity with optional unit. Semantic rule R2 checks it is strictly positive (grammar alone allows zero and negative values via `NUMBER`).

### 3.13 Segment, Coordinates, Altitudes

```antlr
segmentDecl
    : SEGMENT LBRACE
          FROM      COLON coordinatePair   SEMI
          TO        COLON coordinatePair   SEMI
          ALTITUDES COLON altitudeSlotList SEMI
      RBRACE
    ;

coordinatePair   : LPAREN numericValue COMMA numericValue RPAREN ;
altitudeSlotList : LBRACKET altitudeSlot (COMMA altitudeSlot)* RBRACKET ;
altitudeSlot     : numericValue WIDTH numericValue ;
numericValue     : NUMBER unit? ;
unit : UNIT_KG | UNIT_L | UNIT_MS | UNIT_M | UNIT_KMH | UNIT_KM | UNIT_FT | UNIT_KT ;
```

`altitudeSlot` requires a mandatory `WIDTH` for every altitude (each altitude band must specify its corridor width). Semantic rule R9 checks altitude > 0 and width > 0.

**Wind data is not included in the DSL.** Weather information (wind direction and speed) is imported separately via weather forecast files (US042) and associated with flight plans by the pilot (US082). This separation avoids duplication between the DSL and external weather data sources, and keeps the DSL focused on the flight's structural data (route, legs, segments, altitudes, fuel).

---

## 4. Grammar Extensions

The following features go beyond the Core DSL defined in the specification. Each is justified by a concrete aviation or usability reason and is fully specified in the grammar.

### 4.1 Comments

```antlr
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
```

`.flightplan` files can contain both single-line and multi-line comments. Comments are skipped entirely — they never reach the parser. This is a pure usability extension: it allows operators and tools to annotate flight plan files (e.g., mission notes, approval stamps, revision history) without affecting validation.

### 4.2 Altitude corridor width (`WIDTH`)

```antlr
altitudeSlot : numericValue WIDTH numericValue ;
```

Each altitude slot carries a lateral corridor width. In real ATM (Air Traffic Management), an altitude band is paired with a horizontal separation corridor — a plane cleared for FL350 is also assigned a lateral track width. Without the `WIDTH` extension, the DSL could only express an altitude level, not the complete airspace allocation.

### 4.3 Multiple altitude slots per segment

```antlr
altitudeSlotList : LBRACKET altitudeSlot (COMMA altitudeSlot)* RBRACKET ;
```

A segment can be assigned a list of altitude bands (`[10000 m, 11000 m, 12000 m]`). A single segment may span an altitude transition — for example, a climb phase goes from cruise FL290 to FL350 mid-segment. Allowing a list makes it possible to model stepped altitude profiles without splitting the segment.

### 4.4 Extra aviation units (`ft`, `km`, `km/h`, `kt`)

```antlr
UNIT_FT  : 'ft' ;
UNIT_KM  : 'km' ;
UNIT_KMH : 'km/h' ;
UNIT_KT  : 'kt' ;
```

The Core DSL units (`kg`, `l`, `m`, `m/s`) cover basic quantities. The extension adds:

| Unit | Symbol | Used for |
|------|--------|----------|
| feet | `ft` | Altitude — ATC clears aircraft in flight levels based on feet (e.g., FL350 = 35 000 ft) |
| knots | `kt` | Airspeed — ICAO standard; METAR/TAF wind reports are in knots |
| kilometres | `km` | Horizontal distance — radar range and route distance in metric contexts |
| km/h | `km/h` | Ground speed — used in some European ATM contexts alongside knots |

A DSL that only accepts `m` for altitude and `m/s` for speed cannot express standard aviation quantities without manual conversion.

### 4.5 Timestamp with timezone

```antlr
TIMESTAMP
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
      'T'
      [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])?
      ( 'Z' | ('+' | '-') [0-9][0-9] ':' [0-9][0-9] )
    ;
```

The timestamp literal combines date, time, and timezone offset into a single token matching ISO 8601. Example: `2026-06-15T06:00+01:00` or `2026-06-15T08:30Z`. This design was chosen over separate `DATE_LITERAL` + `TIME_LITERAL` tokens because:
- Timezone offsets are inseparable from the datetime — a departure at `08:30` in Lisbon means `+01:00` in summer.
- Having a single token avoids three-way tokenisation (`DATE TIME TIMEZONE`) and makes the grammar simpler.
- The optional seconds component (`:SS`) allows minute-level precision for scheduling and second-level precision for time-critical operations.

Semantic rule R10 validates calendar correctness (e.g., rejects Feb 30, hour > 23).

### 4.6 Schedule type tied to flight type (`DAY_OF_WEEK`, `departureSchedule`, R11)

```antlr
departureSchedule
    : DATETIME COLON TIMESTAMP SEMI
    | daySchedule+
    ;

daySchedule
    : DAY COLON DAY_OF_WEEK SEMI DATETIME COLON TIMESTAMP SEMI
    ;

DAY         : [dD][aA][yY] ;
DAY_OF_WEEK : [mM][oO][nN][dD][aA][yY]
            | [tT][uU][eE][sS][dD][aA][yY]
            | [wW][eE][dD][nN][eE][sS][dD][aA][yY]
            | [tT][hH][uU][rR][sS][dD][aA][yY]
            | [fF][rR][iI][dD][aA][yY]
            | [sS][aA][tT][uU][rR][dD][aA][yY]
            | [sS][uU][nN][dD][aA][yY]
            ;
```

**Motivation:** regular and charter flights have fundamentally different scheduling semantics. A regular flight (e.g., TP001) runs every Monday and Thursday — its schedule is expressed as a day of week. A charter flight is booked for a specific date — its schedule is a full calendar date. Conflating the two would make the DSL imprecise about when a flight actually operates.

**Design decisions:**
- `departureSchedule` replaces the hard-coded `DATETIME COLON TIMESTAMP` in `departureDecl`, so both schedule forms are syntactically valid. The grammar does not enforce which form is used — that responsibility belongs to semantic rule R11.
- Regular flights can have **multiple** `day + datetime` pairs per leg, allowing a weekly schedule with different days and times (e.g., Monday 07:00, Wednesday 07:00, Friday 07:00).
- `DAY_OF_WEEK` is declared before `IDENTIFIER` so that day names (`Monday`, `TUESDAY`, etc.) are not tokenised as identifiers.
- `DAY` is declared separately from `DATETIME` to prevent the prefix `day` from partially matching the `datetime` rule under maximal munch.
- R11 is enforced in `exitDepartureDecl`, where the current flight type (captured in `enterFlightDecl`) is compared against the `departureSchedule` alternative present in the parse tree.
- All timestamps (both regular and charter) carry full ISO 8601 datetime with timezone, enabling consistent UTC-aware comparison for R4.

---

## 5. Processing Pipeline

```
.flightplan file
  └─► CharStream
        └─► FlightPlanLexer ──[FlightPlanErrorListener]──► [LEXER] errors
              └─► CommonTokenStream
                    └─► FlightPlanParser ──[FlightPlanErrorListener]──► [PARSER] errors
                          └─► ParseTree (AST)
                                ├─► ParseTreeWalker + SemanticValidationListener ──► [SEMANTIC] errors
                                └─► FlightPlanPrinterVisitor ──► formatted summary (verbose mode)
```

`FlightPlanRunner.run(path, verbose)` orchestrates the three phases. Phases 1 and 2 both
use `FlightPlanErrorListener` (extends `BaseErrorListener`) which replaces ANTLR's default
`ConsoleErrorListener` and stores errors as `[PHASE] line L:C - message`. If either fails,
Phase 3 is **skipped** to avoid false semantic errors on a malformed tree.

**The ParseTree produced in Phase 2 is the internal representation (AST)** required by
US083 ("an internal representation (AST or domain objects) is produced"). Each node is a
typed Java context object (`FlightDeclContext`, `LegDeclContext`, etc.) retaining the
matched tokens and child rules. The Visitor and Listener both operate on this tree.

---

## 6. Listener — Semantic Validation

`SemanticValidationListener` (extends `FlightPlanBaseListener`) is invoked by
`ParseTreeWalker.DEFAULT.walk(listener, tree)`. The walker calls `enterXxx`/`exitXxx`
automatically, depth-first — **no explicit child visits needed**.

**R1 enforcement:** The grammar rule `flightFile : flightDecl EOF` ensures exactly one flight declaration per file — a parser error is raised if a second `flight` keyword appears. The flight type is captured in `enterFlightDecl` for R11:

**State across events (R3–R11):** For rules that span multiple blocks, the listener accumulates state across events:

```
enterFlightDecl  → clear per-flight state; capture flight type (R11)
  exitRouteDecl      → save routeOrigin, routeDestination
  exitDepartureDecl  → save dep airport/schedule/time; R10: validate date; R11: type vs schedule
  exitArrivalDecl    → save arr airport/time; R10: validate time
  exitFuelDecl       → R2: fuel > 0
  exitSegmentDecl    → R8: from ≠ to
  exitAltitudeSlot   → R9: altitude > 0, width > 0
  exitLegDecl        → append [depAirport, dateOrDay, depTime, arrAirport, arrTime] to legs list
exitFlightDecl   → R3 R4 R5 R6 R7 using the accumulated legs list
```

### Semantic Rules

| Rule | Constraint | Checked at |
|------|-----------|------------|
| **R1** | Flight identifier unique within file | grammar: `flightDecl EOF` |
| **R2** | Fuel quantity > 0 | `exitFuelDecl` |
| **R3** | Arrival airport of leg N = departure airport of leg N+1 | `exitFlightDecl` |
| **R4** | Arrival time of leg N < departure time of leg N+1 | `exitFlightDecl` |
| **R5** | Route origin = first leg departure airport | `exitFlightDecl` |
| **R6** | Route destination = last leg arrival airport | `exitFlightDecl` |
| **R7** | No airport visited more than once in the same flight | `exitFlightDecl` |
| **R8** | Segment from-coordinate ≠ to-coordinate | `exitSegmentDecl` |
| **R9** | Altitudes and corridor widths > 0 | `exitAltitudeSlot` |
| **R10** | Dates are valid calendar dates; times are valid HH:MM[:SS] values | `exitDepartureDecl`, `exitArrivalDecl` |
| **R11** _(extension)_ | Regular flights must use `day:`, charter flights must use `date:` | `exitDepartureDecl` |

**R4 detail:** Arrival blocks have no date or day field. The listener uses the leg's departure schedule (ISO date for charter, day name for regular) as a proxy for the arrival day. For charter flights, datetimes are compared as `YYYY-MM-DDThh:mm:ss` strings — ISO 8601 lexicographic order equals chronological order. For regular flights, comparison uses a day-ordinal score (Monday = 1, …, Sunday = 7) multiplied by 1440 plus minutes since midnight.

**R7 detail:** Visited stops = {departure of leg 1} ∪ {arrival of each leg}. For a round trip LIS→CDG→LHR→LIS, LIS appears at leg-1 departure and leg-3 arrival — duplicate detected.

---

## 7. Visitor — FlightPlanPrinterVisitor

`FlightPlanPrinterVisitor` (extends `FlightPlanBaseVisitor<String>`) formats the parsed
flight plan as a human-readable text summary. Unlike the Listener, children must be visited
**explicitly** and every method **returns a value**:

```java
@Override
public String visitFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
    String id    = visit(ctx.flightId());
    String type  = visit(ctx.flightType());
    String route = visit(ctx.routeDecl());
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Flight %-10s [%s]\n", id, type));
    sb.append(route);
    for (var leg : ctx.legDecl()) sb.append(visit(leg));
    return sb.toString();
}
```

Running `FlightPlanRunner.run(path, true)` on `valid_regular_multi_leg.flightplan` prints:
```
=== Flight Plan Summary ===
Flight TP3000      [REGULAR]
  Aircraft : CS-TUB
  Pilot    : P12345
  Route    : LIS -> CDG
  Leg 1
    Departure : LIS  Monday  2026-05-18T07:00+01:00  Wednesday  2026-05-20T07:00+01:00  Friday  2026-05-22T07:00+01:00
    Arrival   : MAD  2026-05-18T09:30+02:00
    Fuel      : 8000kg
    Segment 1 from:(38.7813,-9.1359) to:(40.4983,-3.5676) alt:[10000m width 60m]
  Leg 2
    Departure : MAD  Monday  2026-05-18T11:00+02:00  Wednesday  2026-05-20T11:00+02:00  Friday  2026-05-22T11:00+02:00
    Arrival   : CDG  2026-05-18T13:30+02:00
    Fuel      : 9000kg
    Segment 1 from:(40.4983,-3.5676) to:(49.0097,2.5479) alt:[11000m width 60m]
```

`visitDepartureSchedule` returns either the ISO datetime or the day+datetime combination — the same `visitDepartureDecl` format string handles both.

#### The visitor runs only after all three validation phases pass.

## 8. Example Files

All files are in `src/main/resources/examples/`.

| File | Type | Covers |
|------|------|--------|
| `valid_direct_flight.flightplan` | valid | regular flight, LIS→LHR, day-of-week schedule, WIDTH altitude |
| `valid_multi_leg.flightplan` | valid | charter, OPO→EDDF→WAW, 2 legs, ICAO+IATA mix |
| `valid_regular_multi_leg.flightplan` | valid | regular, LIS→MAD→CDG, 2 legs, 3 schedules each (Mon/Wed/Fri), (R11 extension) |
| `valid_demo_regular_usa_lis.flightplan` | valid | regular, JFK→LIS, 3 schedules (Tue/Thu/Sat), 3 segments crossing Atlantic |
| `valid_demo_charter_lis_mad.flightplan` | valid | charter, LIS→MAD, 2 segments |
| `valid_long_haul_charter.flightplan` | valid | charter, DXB→JFK, 3 segments, multiple altitude slots |
| `valid_icao_codes.flightplan` | valid | regular, ICAO 4-letter codes LPPT→EDDM |
| `valid_different_units.flightplan` | valid | charter, altitudes in ft (alternative aviation unit) |
| `valid_short_hop.flightplan` | valid | charter, OPO→LIS, 2 segments approach |
| `invalid_bad_flight_type.flightplan` | syntactic | `cargo` is not a valid flight type |
| `invalid_missing_route.flightplan` | syntactic | route block absent |
| `invalid_missing_departure.flightplan` | syntactic | departure block absent from leg |
| `invalid_missing_arrival.flightplan` | syntactic | arrival block absent from leg |
| `invalid_missing_fuel.flightplan` | syntactic | fuel block absent from leg |
| `invalid_missing_segment.flightplan` | syntactic | no segment in leg |
| `invalid_unclosed_brace.flightplan` | syntactic | missing `}` for flight block |
| `invalid_bad_date_format.flightplan` | syntactic | date in `DD-MM-YYYY` instead of `YYYY-MM-DD` |
| `invalid_bad_time_format.flightplan` | syntactic | time `8:30` instead of `08:30` |
| `invalid_bad_airport_code.flightplan` | syntactic | 2-letter code `LI` |
| `invalid_unknown_token.flightplan` | syntactic | illegal character `#` |
| `invalid_missing_semicolon.flightplan` | syntactic | missing `;` after field value |
| `invalid_flight.flightplan` | syntactic | combined: missing `;` + illegal `@` |
| `invalid_sem_duplicate_id.flightplan` | semantic R1 | flight `TP100` declared twice |
| `invalid_sem_zero_fuel.flightplan` | semantic R2 | fuel = 0 |
| `invalid_sem_negative_fuel.flightplan` | semantic R2 | fuel = -500 |
| `invalid_sem_leg_airport_gap.flightplan` | semantic R3 | leg 1 arrives EDDF, leg 2 departs LHR |
| `invalid_sem_leg_time_order.flightplan` | semantic R4 | leg 1 arrives 13:00, leg 2 departs 11:00 |
| `invalid_sem_route_origin_mismatch.flightplan` | semantic R5 | route origin OPO ≠ first dep LIS |
| `invalid_sem_route_dest_mismatch.flightplan` | semantic R6 | route dest CDG ≠ last arr LHR |
| `invalid_sem_airport_revisited.flightplan` | semantic R7 | LIS→CDG→LHR→LIS revisits LIS |
| `invalid_sem_same_coords.flightplan` | semantic R8 | from = to = (38.7813, -9.1359) |
| `invalid_sem_zero_altitude.flightplan` | semantic R9 | altitude = 0 m |
| `invalid_sem_r11_regular_uses_date.flightplan` | semantic R11 | regular flight using `date:` instead of `day:` |
| `invalid_sem_r11_charter_uses_day.flightplan` | semantic R11 | charter flight using `day:` instead of `date:` |

---

## 9. Build and Run

```bash
# Build and run all tests (FlightPlanRunnerTest + SemanticValidationTest)
mvn test

# Run only syntactic tests
mvn test -Dtest=FlightPlanRunnerTest

# Run only semantic tests
mvn test -Dtest=SemanticValidationTest

# Validate a file from the command line (verbose: prints formatted summary)
java -cp target/aisafe.dsl-1.4.0-SNAPSHOT.jar \
     aisafe.lprog.dsl.FlightPlanRunner \
     src/main/resources/examples/valid_direct_flight.flightplan
```

Maven generates the ANTLR sources automatically on the first `mvn compile` or `mvn test`
(via `antlr4-maven-plugin 4.13.1`). Generated files appear in
`target/generated-sources/antlr4/aisafe/lprog/`:
`FlightPlanLexer.java`, `FlightPlanParser.java`,
`FlightPlanBaseListener.java`, `FlightPlanBaseVisitor.java`.
