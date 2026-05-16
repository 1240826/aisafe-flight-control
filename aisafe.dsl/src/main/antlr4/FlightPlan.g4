// FlightPlan.g4
// Grammar name MUST match filename exactly.
grammar FlightPlan;

// Generated classes go into this package
@header {
package aisafe.lprog;
}

// ============================================================
// PARSER RULES  (lowercase)
// ============================================================
//
// Structure follows the informal diagram from the LPROG spec:
//
//   flight
//   ├── ID
//   ├── Type
//   ├── Route          ← at flight level
//   ├── Leg (1)
//   │   ├── Departure
//   │   ├── Arrival
//   │   ├── Fuel
//   │   └── Segments (1..n)
//   └── Leg (n)
//
// Each alternative is labelled #Name so ANTLR generates specific
// visitXxx / enterXxx / exitXxx methods (pattern from LPROG slides).
// ============================================================

// Entry point — a file may contain one or more flight declarations
flightFile
    : flightDecl+ EOF
    ;

// flight <id> : <type> { route leg+ }
flightDecl
    : FLIGHT flightId COLON flightType LBRACE
          routeDecl
          legDecl+
      RBRACE                                                      #flightBlock
    ;

flightId
    : IDENTIFIER                                                  #flightIdentifier
    ;

flightType
    : REGULAR                                                     #regularFlight
    | CHARTER                                                     #charterFlight
    ;

// route { origin: <code>; destination: <code>; }
// Route is at flight level — shared by all legs
routeDecl
    : ROUTE LBRACE
          ORIGIN      COLON airportCode SEMI
          DESTINATION COLON airportCode SEMI
      RBRACE                                                      #routeBlock
    ;

// leg <id> { departure arrival fuel segment+ }
legDecl
    : LEG IDENTIFIER LBRACE
          departureDecl
          arrivalDecl
          fuelDecl
          segmentDecl+
      RBRACE                                                      #legBlock
    ;

// departure { airport: <code>; date: <date>; time: <time>; }
departureDecl
    : DEPARTURE LBRACE
          AIRPORT COLON airportCode  SEMI
          DATE    COLON DATE_LITERAL SEMI
          TIME    COLON TIME_LITERAL SEMI
      RBRACE                                                      #departureBlock
    ;

// arrival { airport: <code>; time: <time>; }
arrivalDecl
    : ARRIVAL LBRACE
          AIRPORT COLON airportCode  SEMI
          TIME    COLON TIME_LITERAL SEMI
      RBRACE                                                      #arrivalBlock
    ;

// fuel { quantity: <num> <unit>; }
fuelDecl
    : FUEL LBRACE
          QUANTITY COLON numericValue SEMI
      RBRACE                                                      #fuelBlock
    ;

// segment <id> { from: (...); to: (...); altitudes: [...]; wind: (...); }
segmentDecl
    : SEGMENT IDENTIFIER LBRACE
          FROM      COLON coordinatePair   SEMI
          TO        COLON coordinatePair   SEMI
          ALTITUDES COLON altitudeSlotList SEMI
          WIND      COLON windDecl         SEMI
      RBRACE                                                      #segmentBlock
    ;

// ---- Supporting rules ----

airportCode
    : IATA_CODE   #iataCode
    | ICAO_CODE   #icaoCode
    ;

coordinatePair
    : LPAREN numericValue COMMA numericValue RPAREN               #coordPair
    ;

altitudeSlotList
    : LBRACKET altitudeSlot (COMMA altitudeSlot)* RBRACKET       #altSlotList
    ;

// Extension: optional WIDTH for each altitude slot
altitudeSlot
    : numericValue (WIDTH numericValue)?                          #altSlot
    ;

windDecl
    : LPAREN numericValue COMMA numericValue RPAREN               #windInfo
    ;

numericValue
    : NUMBER unit?                                               #numVal
    ;

// UNIT_ prefix avoids name collision with fragment letters L and M
unit
    : UNIT_KG | UNIT_L | UNIT_MS | UNIT_M | UNIT_KMH | UNIT_KM | UNIT_FT | UNIT_KT
    ;


// ============================================================
// LEXER RULES  (UPPERCASE)
// ============================================================

// ---- Keywords — case-insensitive via fragment trick ----
FLIGHT      : F L I G H T ;
LEG         : L E G ;
DEPARTURE   : D E P A R T U R E ;
ARRIVAL     : A R R I V A L ;
ROUTE       : R O U T E ;
SEGMENT     : S E G M E N T ;
FUEL        : F U E L ;
AIRPORT     : A I R P O R T ;
DATE        : D A T E ;
TIME        : T I M E ;
ORIGIN      : O R I G I N ;
DESTINATION : D E S T I N A T I O N ;
FROM        : F R O M ;
TO          : T O ;
ALTITUDES   : A L T I T U D E S ;
WIND        : W I N D ;
QUANTITY    : Q U A N T I T Y ;
WIDTH       : W I D T H ;
REGULAR     : R E G U L A R ;
CHARTER     : C H A R T E R ;

// ---- Units — UNIT_ prefix avoids collision with fragments L and M ----
// Longer tokens must appear before shorter ones (ANTLR first-match rule)
UNIT_KG  : K G ;
UNIT_MS  : 'm' '/' 's' ;     // m/s  — before UNIT_M
UNIT_M   : 'm' ;
UNIT_L   : 'l' ;
UNIT_FT  : F T ;
UNIT_KMH : K M '/' H ;       // km/h — before UNIT_KM
UNIT_KM  : K M ;
UNIT_KT  : K T ;

// ---- Date: YYYY-MM-DD ----
DATE_LITERAL
    : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT
    ;

// ---- Time: HH:MM  or  HH:MM:SS ----
TIME_LITERAL
    : DIGIT DIGIT ':' DIGIT DIGIT (':' DIGIT DIGIT)?
    ;

// ---- Airport codes — ICAO (4 letters) BEFORE IATA (3 letters) ----
// ANTLR longest-match: EDDF -> ICAO, not IATA "EDD" + identifier "F"
ICAO_CODE : [A-Z][A-Z][A-Z][A-Z] ;
IATA_CODE : [A-Z][A-Z][A-Z] ;

// ---- General identifier ----
IDENTIFIER : [a-zA-Z][a-zA-Z0-9_\-]* ;

// ---- Numbers: integer or float, optionally signed ----
NUMBER : '-'? DIGIT+ ('.' DIGIT+)? ;

// ---- Symbols ----
LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
COMMA    : ',' ;
COLON    : ':' ;
SEMI     : ';' ;

// ---- Ignored ----
WHITESPACE    : [ \t\r\n]+    -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

// ---- Case-insensitive letter fragments ----
// These are only usable inside lexer rules.
// Unit tokens use UNIT_ prefix to avoid redefinition of M and L.
fragment A : [aA]; fragment B : [bB]; fragment C : [cC];
fragment D : [dD]; fragment E : [eE]; fragment F : [fF];
fragment G : [gG]; fragment H : [hH]; fragment I : [iI];
fragment J : [jJ]; fragment K : [kK]; fragment L : [lL];
fragment M : [mM]; fragment N : [nN]; fragment O : [oO];
fragment P : [pP]; fragment Q : [qQ]; fragment R : [rR];
fragment S : [sS]; fragment T : [tT]; fragment U : [uU];
fragment V : [vV]; fragment W : [wW]; fragment X : [xX];
fragment Y : [yY]; fragment Z : [zZ];
fragment DIGIT : [0-9] ;
