# US120 – Flight DSL Specification and Validation

## 1. Context

This user story extends the DSL work developed in previous sprints.  
The initial version already supported lexical and syntactic validation using ANTLR4.  
In this sprint, the DSL was refined and expanded with new grammar features, semantic validation, listeners/visitors, and construction of an internal representation of the flight plan.

The grammar evolved to support:

- regular and charter flight scheduling,
- ISO-8601 timestamps with timezone offsets,
- aircraft and pilot declarations,
- semantic validation rules,
- AST/domain model construction,
- and improved error reporting.

This task belongs to the LPROG course unit.

---

### 1.1 List of Issues

Analysis: #85

Design: #85

Implement: #85

Test: #85

---

# 2. Requirements

**US120** – As a Project Manager, I want the team to specify and implement the Flight Description DSL, so that flight plans can be formally defined and validated.

---

## Acceptance Criteria

- **US120.1** The informal lexical and syntactic specification of the DSL must be documented.

- **US120.2** A formal grammar must be implemented using ANTLR4.

- **US120.3** The system must perform lexical analysis.

- **US120.4** The system must perform syntactic analysis.

- **US120.5** The system must perform semantic validation.

- **US120.6** The implementation must use ANTLR listeners and/or visitors.

- **US120.7** The system must produce an internal representation (AST/domain model).

- **US120.8** Invalid inputs must generate clear and informative error messages.

---

## Dependencies / References

This user story extends the previous DSL implementation developed in earlier sprints.

The implementation follows the ANTLR4 approach presented in the LPROG lectures, particularly:

- lexer/parser generation,
- visitors and listeners,
- parse tree traversal,
- semantic validation,
- and custom error listeners.

---

# 3. Analysis

## Conceptual Model

The DSL represents a complete flight plan composed of one or more legs.

```text
flight
├── ID
├── Type (regular | charter)
├── Route
│   ├── Origin
│   └── Destination
├── Aircraft
├── Pilot
├── Leg (1)
│   ├── Departure
│   │   ├── Airport
│   │   └── Schedule
│   ├── Arrival
│   │   ├── Airport
│   │   └── Datetime
│   ├── Fuel
│   │   └── Quantity
│   └── Segment (1..n)
│       ├── From coordinates
│       ├── To coordinates
│       ├── Altitudes
│       └── Wind
├── Leg (2)
│   └── ...
└── Leg (n)
```

The DSL now distinguishes between:

- **Regular flights**
    - include a weekly schedule (`day`)
    - include timezone-aware datetimes

- **Charter flights**
    - use only a datetime schedule

---

## Informal Lexical Specification

| Category | Examples | Notes |
|---|---|---|
| Keywords | `flight`, `route`, `leg`, `departure`, `arrival` | Case-insensitive |
| Scheduling keywords | `datetime`, `day` | Used for schedules |
| Operational keywords | `aircraft`, `pilot`, `width` | Added in current version |
| Identifiers | `TP5678`, `SEG1`, `CS-TUB` | Case-sensitive |
| ICAO codes | `EDDF`, `LPPT` | 4 uppercase letters |
| IATA codes | `LIS`, `MAD`, `CDG` | 3 uppercase letters |
| Numbers | `11000`, `-8.6814` | Integer or decimal |
| Timestamp literals | `2026-06-15T06:00+01:00` | ISO-8601 |
| Day literals | `Monday`, `Friday` | Case-insensitive |
| Units | `kg`, `m`, `m/s`, `ft`, `kt` | Attached to numbers |
| Degree marker | `degrees`, `°` | Optional in wind direction |
| Symbols | `{ } ( ) [ ] , : ;` | Delimiters |
| Comments | `//`, `/* */` | Ignored |
| Whitespace | spaces, tabs, newlines | Ignored |

---

## Informal Syntax Specification

```text
flightFile ::= flightDecl

flightDecl ::= FLIGHT flightId COLON flightType LBRACE
                   routeDecl
                   AIRCRAFT COLON IDENTIFIER SEMI
                   PILOT COLON IDENTIFIER SEMI
                   legDecl+
               RBRACE

flightType ::= REGULAR | CHARTER

routeDecl ::= ROUTE LBRACE
                  ORIGIN COLON airportCode SEMI
                  DESTINATION COLON airportCode SEMI
              RBRACE

legDecl ::= LEG IDENTIFIER LBRACE
                departureDecl
                arrivalDecl
                fuelDecl
                segmentDecl+
            RBRACE

departureDecl ::= DEPARTURE LBRACE
                      AIRPORT COLON airportCode SEMI
                      scheduleField
                  RBRACE

scheduleField ::= DATETIME COLON TIMESTAMP_LITERAL SEMI
                | DAY COLON DAY_LITERAL SEMI
                  DATETIME COLON TIMESTAMP_LITERAL SEMI

arrivalDecl ::= ARRIVAL LBRACE
                    AIRPORT COLON airportCode SEMI
                    DATETIME COLON TIMESTAMP_LITERAL SEMI
                RBRACE
```

---

## Semantic Validation Rules

The semantic analysis validates constraints that cannot be enforced directly by grammar rules.

Examples:

- A flight must contain at least one leg.
- Each leg must contain at least one segment.
- Fuel quantity must be positive.
- Wind direction must be between `0` and `360`.
- Coordinates must be within valid latitude/longitude ranges.
- Charter flights cannot define a `day`.
- Regular flights must define a `day`.
- Departure and arrival airports in the same leg cannot be equal.
- Route origin must match the first departure airport.
- Route destination must match the final arrival airport.

---

# 4. Design

## 4.1 Realization

The implementation follows the ANTLR4 processing pipeline presented in the LPROG lectures.

```text
Input File
    ↓
Lexer
    ↓
Parser
    ↓
Parse Tree
    ↓
Visitor / Listener
    ↓
Semantic Validation
    ↓
Domain Object Model
```

---

## Main Design Decisions

### Route at flight level

The `route` block remains inside `flightDecl` because it represents the global route shared by all legs.

---

### Scheduling model

Two scheduling formats are supported:

#### Charter flight

```text
datetime : 2026-06-15T06:00+01:00;
```

#### Regular flight

```text
day      : Monday;
datetime : 2026-05-18T07:00+01:00;
```

---

### ISO-8601 timestamps

The lexer supports timestamps with timezone offsets:

```text
2026-06-15T06:00+01:00
2026-06-15T08:30Z
```

---

### Wind direction extension

The grammar accepts:

```text
(90 degrees, 20 m/s)
(90°, 20 m/s)
(90, 20 m/s)
```

---

### Visitor and Listener usage

ANTLR generates:

- `FlightPlanVisitor`
- `FlightPlanBaseVisitor`
- `FlightPlanListener`
- `FlightPlanBaseListener`

Visitors are used for:

- semantic validation,
- internal model construction,
- parse tree traversal.

---

### Internal Representation

The visitor constructs domain objects representing the parsed flight plan.

```text
FlightPlan
├── id
├── type
├── route
├── aircraft
├── pilot
└── legs
```

---

## 4.2 Acceptance Tests

### Acceptance Test 1 – Informal DSL specification documentation

**Objective:**  
Validate that the lexical and syntactic specification of the DSL is properly documented.

**Procedure:**
1. Open the technical documentation of the user story.
2. Verify that the documentation contains:
    - lexical specification,
    - syntactic specification,
    - supported literals,
    - token categories,
    - grammar structure examples.

**Expected Result:**  
The documentation clearly describes the DSL structure, supported tokens, literals, keywords, and syntax rules.

**Refers to Acceptance Criteria:** US120.1

---

### Acceptance Test 2 – ANTLR grammar generation

**Objective:**  
Validate that the DSL grammar is formally specified using ANTLR4.

**Procedure:**
1. Execute:
   ```bash
   mvn compile
   ```
2. Verify that ANTLR generates:
    - `FlightPlanLexer.java`
    - `FlightPlanParser.java`
    - `FlightPlanVisitor.java`
    - `FlightPlanListener.java`

**Expected Result:**  
ANTLR successfully generates the lexer, parser, listener, and visitor classes without grammar errors.

**Refers to Acceptance Criteria:** US120.2

---

### Acceptance Test 3 – Lexical validation

**Objective:**  
Validate that the lexer correctly recognizes valid DSL tokens and detects invalid characters.

**Procedure:**
1. Execute the parser using a valid `.flightplan` file.
2. Execute the parser using an invalid file containing illegal characters (example: `@`).

**Expected Result:**
- Valid files are tokenized successfully.
- Invalid files generate lexer error messages containing:
    - error type,
    - line,
    - column,
    - invalid token.

Example:
```text
[LEXER] line 17:16 - token recognition error at: '@'
```

**Refers to Acceptance Criteria:** US120.3, US120.8

---

### Acceptance Test 4 – Syntactic validation

**Objective:**  
Validate that the parser detects invalid DSL structures.

**Procedure:**
1. Execute the parser with a valid flight plan.
2. Execute the parser with malformed syntax:
    - missing semicolons,
    - invalid block structure,
    - incomplete declarations.

**Expected Result:**
- Valid files are parsed successfully.
- Invalid files generate parser error messages containing:
    - line,
    - column,
    - description of the syntax error.

Example:
```text
[PARSER] line 13:8 - missing ';' at '}'
```

**Refers to Acceptance Criteria:** US120.4, US120.8

---

### Acceptance Test 5 – Semantic validation

**Objective:**  
Validate that semantic constraints are enforced after parsing.

**Procedure:**
1. Execute the semantic visitor using files with:
    - negative fuel values,
    - invalid wind directions,
    - inconsistent route airports,
    - invalid regular/charter scheduling rules.

**Expected Result:**  
Semantic violations generate descriptive semantic error messages.

Example:
```text
[SEMANTIC] line 24:15 - fuel quantity must be positive
```

**Refers to Acceptance Criteria:** US120.5, US120.8

---

### Acceptance Test 6 – Visitor and listener traversal

**Objective:**  
Validate that ANTLR visitors/listeners are used during processing.

**Procedure:**
1. Execute the DSL processing pipeline.
2. Verify that:
    - visitors traverse the parse tree,
    - listeners are generated by ANTLR,
    - semantic validation is executed through visitor traversal.

**Expected Result:**  
The system successfully processes the parse tree using generated visitors/listeners.

**Refers to Acceptance Criteria:** US120.6

---

### Acceptance Test 7 – Internal representation generation

**Objective:**  
Validate that the system constructs an internal representation of the parsed flight plan.

**Procedure:**
1. Execute the parser with a valid multi-leg flight plan.
2. Inspect the generated internal model/domain objects.

**Expected Result:**  
The system produces a complete internal representation containing:
- flight information,
- route,
- aircraft,
- pilot,
- legs,
- segments.

**Refers to Acceptance Criteria:** US120.7

---

### Acceptance Test 8 – Valid regular and charter flights

**Objective:**  
Validate support for both regular and charter scheduling models.

**Procedure:**
1. Execute the parser with:
    - a valid regular flight,
    - a valid charter flight.

**Expected Result:**
- Regular flights correctly accept:
  ```text
  day + datetime
  ```
- Charter flights correctly accept:
  ```text
  datetime only
  ```

Both files are successfully validated.

**Refers to Acceptance Criteria:** US120.2, US120.4, US120.5
---

# 5. Implementation

## Main Files

| File | Location |
|---|---|
| `FlightPlan.g4` | `src/main/antlr4/` |
| `FlightPlanRunner.java` | `src/main/java/.../dsl/` |
| `FlightPlanErrorListener.java` | `src/main/java/.../errors/` |
| `FlightPlanSemanticVisitor.java` | `src/main/java/.../visitor/` |
| `FlightPlanModelBuilderVisitor.java` | `src/main/java/.../visitor/` |

---

## Generated ANTLR Files

Generated automatically during:

```bash
mvn compile
```

Generated files include:

- `FlightPlanLexer.java`
- `FlightPlanParser.java`
- `FlightPlanVisitor.java`
- `FlightPlanBaseVisitor.java`
- `FlightPlanListener.java`
- `FlightPlanBaseListener.java`

These files are generated into `target/` and are not committed.

---

## Error Reporting

Custom error handling extends `BaseErrorListener`.

Examples:

```text
[LEXER] line 17:16 - token recognition error at: '@'
```

```text
[PARSER] line 13:8 - missing ';' at '}'
```

```text
[SEMANTIC] line 24:15 - fuel quantity must be positive
```

---

# 6. Integration / Demonstration

The DSL can be executed using the `FlightPlanRunner`.

Example:

```bash
mvn exec:java
```

Example valid execution:

```text
Validation OK: valid_charter.flightplan
```

Example invalid execution:

```text
Validation FAILED: invalid_semantic.flightplan

[SEMANTIC] line 31:12 - wind direction must be between 0 and 360
```

The implementation integrates:

- ANTLR lexer/parser generation,
- custom error listeners,
- semantic visitors,
- and domain model construction.

---

# 7. Observations

- The grammar evolved significantly from the previous sprint while preserving the original DSL structure.
- The DSL now supports both regular and charter scheduling models.
- Semantic validation is separated from syntax validation using visitors.
- ISO-8601 timestamps improved realism and compatibility with aviation scheduling systems.
- The implementation follows the visitor/listener architecture presented in the LPROG lectures.
- Weather systems, cargo manifests, and passenger management remain outside the scope of the DSL.
- Generative AI tools (Claude and ChatGPT) were used to support grammar refinement and validation ideas.