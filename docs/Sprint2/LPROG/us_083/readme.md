# US083 – Flight DSL Specification and Validation

## Context

As a Project Manager, I want the team to specify and implement the Flight Description DSL, so that flight plans can be formally defined and validated.

This user story is the responsibility of the LPROG course unit and covers **Phase 1 (Sprint 2, 17th May 2026)**: lexical and syntactic specification, ANTLR4 grammar, and error reporting. Semantic validation is deferred to Phase 2 (Sprint 3, 14th June 2026).

### List of Issues

- Analysis: #41
- Design: #41
- Implement: #41
- Test: N/A

---

## Requirements

**Acceptance criteria — Phase 1:**

- The informal lexical and syntactic specification of the DSL is documented.
- A formal grammar is defined using ANTLR4.
- The system performs lexical and syntactic validation.
- Invalid inputs generate clear and informative error messages (error type, line, column).

---

## Analysis

### Conceptual Model

The structure follows the informal diagram from section 3.4.1 of the project specification:

```
flight
├── ID
├── Type
├── Route              ← at flight level (shared by all legs)
├── Leg (1)
│   ├── Departure      { airport, date, time }
│   ├── Arrival        { airport, time }
│   ├── Fuel           { quantity }
│   └── Segments (1..n){ from, to, altitudes, wind }
├── Leg (2)
│   └── ...
└── Leg (n)
```

A flight plan represents a complete flight operation. It may include multiple legs, where each leg is a non-stop journey segment between two airports. The route (origin and destination of the overall flight) is defined once at flight level and is shared across all legs.

### Informal Lexical Specification

| Category | Examples | Notes |
|---|---|---|
| Keywords | `flight`, `leg`, `departure`, `arrival`, `route`, `segment`, `fuel`, `regular`, `charter` | Case-insensitive |
| Identifiers | `TP1234`, `L1`, `SEG1` | Case-sensitive; starts with letter |
| ICAO codes | `EDDF`, `LPPT` | Exactly 4 uppercase letters |
| IATA codes | `LIS`, `LHR` | Exactly 3 uppercase letters |
| Numbers | `38.7813`, `-9.1359`, `15000` | Integer or float, optionally signed |
| Date literals | `2026-05-10` | Format `YYYY-MM-DD` |
| Time literals | `08:30`, `10:45:00` | Format `HH:MM` or `HH:MM:SS` |
| Units | `kg`, `l`, `m`, `m/s`, `ft`, `km`, `km/h`, `kt` | Qualify numeric values |
| Symbols | `{ } ( ) [ ] , : ;` | Block delimiters and separators |
| Comments | `// ...` and `/* ... */` | Extension — ignored by the lexer |
| Whitespace | spaces, tabs, newlines | Ignored — free formatting |

Keywords are **case-insensitive**. Identifiers and literals are **case-sensitive**.

### Informal Syntax Specification

```
flightFile    ::= flightDecl+ EOF

flightDecl    ::= FLIGHT id COLON flightType LBRACE
                      routeDecl
                      legDecl+
                  RBRACE

flightType    ::= REGULAR | CHARTER

routeDecl     ::= ROUTE LBRACE
                      ORIGIN      COLON airportCode SEMI
                      DESTINATION COLON airportCode SEMI
                  RBRACE

legDecl       ::= LEG id LBRACE
                      departureDecl
                      arrivalDecl
                      fuelDecl
                      segmentDecl+
                  RBRACE

departureDecl ::= DEPARTURE LBRACE
                      AIRPORT COLON airportCode  SEMI
                      DATE    COLON DATE_LITERAL SEMI
                      TIME    COLON TIME_LITERAL SEMI
                  RBRACE

arrivalDecl   ::= ARRIVAL LBRACE
                      AIRPORT COLON airportCode  SEMI
                      TIME    COLON TIME_LITERAL SEMI
                  RBRACE

fuelDecl      ::= FUEL LBRACE
                      QUANTITY COLON numericValue SEMI
                  RBRACE

segmentDecl   ::= SEGMENT id LBRACE
                      FROM      COLON coordinatePair   SEMI
                      TO        COLON coordinatePair   SEMI
                      ALTITUDES COLON altitudeSlotList SEMI
                      WIND      COLON windDecl         SEMI
                  RBRACE

coordinatePair    ::= LPAREN numericValue COMMA numericValue RPAREN
altitudeSlotList  ::= LBRACKET altitudeSlot (COMMA altitudeSlot)* RBRACKET
altitudeSlot      ::= numericValue (WIDTH numericValue)?
windDecl          ::= LPAREN numericValue COMMA numericValue RPAREN
numericValue      ::= NUMBER unit?
airportCode       ::= IATA_CODE | ICAO_CODE
```

---

### LLM Assistance

Generative AI (Claude, Anthropic) was used to support the analysis and design of this user story.
Below are the main prompts used, the suggestions adopted, and the decisions the team made
independently or where we deviated from the AI output.

---

#### Prompt 1 — ANTLR4 grammar structure for the Flight Plan DSL

> "We are specifying a DSL for flight plans using ANTLR4. A flight plan has an ID, type (regular
> or charter), a route, and one or more legs. Each leg has departure, arrival, fuel, and segments.
> Keywords must be case-insensitive. Suggest a grammar structure and the main lexer design decisions."

**LLM suggestions adopted:**
- Case-insensitive keywords via ANTLR fragment rules — consistent with the LPROG slides pattern
- Placing ICAO before IATA in the lexer so the longer match wins
- Alternative labels (`#Name`) on parser rule alternatives to generate specific visitor methods

**Decisions made by the team / deviations from LLM output:**
- The LLM placed `route` inside `legDecl` — moved to `flightDecl` to match section 3.4.1 of the
  specification (route is shared by all legs)
- Unit tokens renamed to `UNIT_L`, `UNIT_M` etc. to avoid conflict with fragment letters already
  defined for keyword matching


## Design

### Grammar (`FlightPlan.g4`)

Located at `src/main/antlr4/FlightPlan.g4`.

**Key design decisions:**

**Route at flight level** — the `route` block is placed inside `flightDecl`, not inside `legDecl`. This matches the informal structure diagram in section 3.4.1 of the specification. The route defines the overall origin and destination of the flight and is shared by all legs.

**Keywords case-insensitive** — implemented using ANTLR fragment rules, following the pattern from the LPROG slides:
```antlr
FLIGHT : F L I G H T ;
fragment F : [fF];  fragment L : [lL];  // etc.
```

**Alternative labels (`#Name`)** — every parser rule alternative is labelled so ANTLR generates specific `visitXxx` / `enterXxx` / `exitXxx` methods, following the pattern from the LPROG slides (`#opExprMulDiv`, `#atomExpr`):
```antlr
flightDecl : FLIGHT flightId COLON flightType LBRACE routeDecl legDecl+ RBRACE  #flightBlock ;
```

**ICAO before IATA in the lexer** — ANTLR uses longest-match. The 4-letter ICAO rule is placed before the 3-letter IATA rule so `EDDF` is tokenised as ICAO and not as IATA `EDD` + identifier `F`.

**Units use `UNIT_` prefix** — the fragment letters `L` and `M` are already defined for case-insensitive keyword matching. Unit tokens are therefore named `UNIT_L`, `UNIT_M`, etc. to avoid redefinition errors. Longer unit tokens (`UNIT_MS`, `UNIT_KMH`) are placed before shorter ones (`UNIT_M`, `UNIT_KM`) so the longer match wins.

**Extensions over Core DSL (justified and formally specified):**
- `WIDTH` keyword in altitude slots — adds expressiveness for segment width specification; compatible with Core DSL.
- Inline comments (`//` and `/* */`) — improve readability of `.flightplan` files; no semantic meaning.

### Error reporting

`FlightPlanErrorListener` extends ANTLR's `BaseErrorListener` and overrides `syntaxError`. Errors include phase, line, and column — following the pattern from the LPROG slides:

```
[LEXER]  line 17:16 - token recognition error at: '@'
[PARSER] line 13:8  - missing ';' at '}'
```

### Processing pipeline

Follows the exact pattern from the LPROG slides (`Expressions.java` example):

```java
CharStream input = CharStreams.fromPath(filePath);

FlightPlanLexer lexer = new FlightPlanLexer(input);
lexer.removeErrorListeners();
lexer.addErrorListener(new FlightPlanErrorListener("LEXER"));

CommonTokenStream tokens = new CommonTokenStream(lexer);

FlightPlanParser parser = new FlightPlanParser(tokens);
parser.removeErrorListeners();
parser.addErrorListener(new FlightPlanErrorListener("PARSER"));

ParseTree tree = parser.flightFile();
System.out.println(tree.toStringTree(parser));
```

---

## Implementation

### Files

| File | Location |
|---|---|
| `FlightPlan.g4` | `src/main/antlr4/` |
| `FlightPlanErrorListener.java` | `src/main/java/aisafe/lprog/errors/` |
| `FlightPlanRunner.java` | `src/main/java/aisafe/lprog/dsl/` |
| `valid_direct_flight.flightplan` | `src/main/resources/examples/` |
| `valid_multi_leg.flightplan` | `src/main/resources/examples/` |
| `invalid_flight.flightplan` | `src/main/resources/examples/` |

### Files generated by ANTLR (in `target/` — not committed)

`FlightPlanLexer.java`, `FlightPlanParser.java`, `FlightPlanBaseVisitor.java`, `FlightPlanVisitor.java`, `FlightPlanBaseListener.java`, `FlightPlanListener.java`

Generated automatically during `mvn compile`. These are the base classes that will be extended in Sprint 3 for the visitor and listener implementations.

---

## Integration / Demonstration

**Valid file output:**
```
Validation OK: valid_direct_flight.flightplan
(flightFile (flightDecl flight (flightId TP1234) : (flightType regular) {
  (routeDecl route { origin : (airportCode LIS) ; destination : (airportCode LHR) ; })
  (legDecl leg L1 { ... }) }) <EOF>)
```

**Invalid file output:**
```
Validation FAILED: invalid_flight.flightplan
[LEXER]  line 17:16 - token recognition error at: '@'
[PARSER] line 13:8  - missing ';' at '}'
```

---

## Observations

- Phase 2 (Sprint 3) will add: semantic validation (section 3.4.5), a visitor that builds the internal representation of the flight plan, and a listener for parse tree tracing — following the `EvalVisitor` / `EvalListener` patterns from the LPROG slides.
- Weather data, aircraft, load (people/cargo) are explicitly out of scope for the DSL (section 3.4.6).
- The `pom.xml` already has `<visitor>true</visitor>` and `<listener>true</listener>` in the ANTLR4 plugin configuration, so the base classes are generated and ready for Sprint 3.