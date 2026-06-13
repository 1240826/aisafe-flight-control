# Listeners and Visitors — Flight DSL

## 1. Parse Tree (AST)

After parsing a `.flightplan` file, ANTLR builds a parse tree. Each grammar rule
becomes a typed context node. This tree **is** the internal representation (AST).

```
flightFile → FlightFileContext
└── flightDecl → FlightDeclContext
    ├── flightId, flightType
    ├── routeDecl → RouteDeclContext
    ├── aircraft, pilot
    └── legDecl+ → LegDeclContext
        ├── departureDecl → DepartureDeclContext
        ├── arrivalDecl   → ArrivalDeclContext
        ├── fuelDecl      → FuelDeclContext
        └── segmentDecl+  → SegmentDeclContext
```

## 2. Listener — Semantic Validation

**What it does:** validates 11 semantic rules (R1–R11) after parsing succeeds.

**How it works:** extends `FlightPlanBaseListener`. The `ParseTreeWalker` calls
`enterXxx`/`exitXxx` automatically in depth-first order — no manual traversal needed.
Methods return `void`. Results are accumulated in instance fields.

```java
ParseTreeWalker.DEFAULT.walk(listener, tree);
```

Walk order for a flight with two legs:

```
enterFlightDecl        → capture flight type
  exitRouteDecl        → store origin/destination
  exitDepartureDecl    → validate R10, R11; store airport + timestamp
  exitArrivalDecl      → validate R10; store airport + timestamp
  exitFuelDecl         → validate R2 (fuel > 0)
  exitSegmentDecl      → validate R8 (from ≠ to)
  exitAltitudeSlot     → validate R9 (altitude > 0, width > 0)
  exitLegDecl          → append leg data to list
  ... (repeat for leg 2)
exitFlightDecl         → validate R3, R4, R5, R6, R7 using accumulated data
```

**Why `exit` and not `enter`:** we validate in `exit` because children must be visited
first for their data to be available.

**Cross-block rules:** rules like R3 (airport connection between legs) and R7 (no
repeated airports) need data from multiple legs. The listener stores each leg's data
in a list during `exitLegDecl` and validates all cross-leg rules in `exitFlightDecl`.

Example error output: `[SEMANTIC] line 6 - [R5] route origin 'OPO' must match first leg departure airport 'LIS'`

## 3. Visitor — Formatted Summary

**What it does:** prints a human-readable summary of the parsed flight plan.

**How it works:** extends `FlightPlanBaseVisitor<String>`. Each `visitXxx` returns a
`String`. We explicitly call `visit()` on each child we want to include.

```java
public String visitFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
    return String.format("Flight %-10s [%s]\n", visit(ctx.flightId()), visit(ctx.flightType()))
         + visit(ctx.routeDecl())
         + ctx.legDecl().stream().map(this::visit).collect(Collectors.joining());
}
```

Example output:
```
=== Flight Plan Summary ===
Flight TP1234      [REGULAR]
  Aircraft : CS-TUB
  Pilot    : P12345
  Route    : LIS -> LHR
  Leg 1
    Departure : LIS  Monday  2026-05-18T08:30+01:00
    Arrival   : LHR  2026-05-18T10:45+01:00
    Fuel      : 15000 kg
    Segment 1 from:(38.7813,-9.1359) to:(51.4775,-0.4614) alt:[10000 m width 50 m]
```

## 4. Why Listener for validation and Visitor for printing

| | Listener | Visitor |
|---|---|---|
| Traversal | Automatic (walker) | Manual (explicit `visit()` calls) |
| Return type | `void` | Generic (`String` in our case) |
| Best for | Visit all nodes, collect results externally | Build hierarchical results bottom-up |

Validation visits every node and accumulates errors in a list — Listener fits
naturally. Printing builds a tree of strings where each node's output depends on its
children's output — Visitor fits naturally.

## 5. Test Coverage

| Test class | Tests | Covers |
|---|---|---|
| `FlightPlanRunnerTest` | 27 | Lexer + parser: valid files, lexical errors, syntactic errors |
| `SemanticValidationTest` | 18 | Each semantic rule R1–R11 with dedicated invalid `.flightplan` files |

Test resources in `src/main/resources/examples/`: 20 valid files, 16 syntactically
invalid files, 12 semantically invalid files.
