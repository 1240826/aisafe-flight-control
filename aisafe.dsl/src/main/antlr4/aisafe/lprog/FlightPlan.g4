grammar FlightPlan;

flightFile : flightDecl;

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

routeDecl
    : ROUTE LBRACE
          ORIGIN      COLON airportCode SEMI
          DESTINATION COLON airportCode SEMI
      RBRACE
    ;

legDecl
    : LEG IDENTIFIER LBRACE
          departureDecl
          arrivalDecl
          fuelDecl
          segmentDecl+
      RBRACE
    ;


departureDecl
    : DEPARTURE LBRACE
          AIRPORT COLON airportCode SEMI
          scheduleField
      RBRACE
    ;

// charter: single datetime with timezone offset (e.g. 2026-05-20T08:00+01:00)
// regular: day-of-week + full datetime with timezone (e.g. day: Monday; datetime: 2026-05-20T08:00+01:00;)
scheduleField
    : DATETIME COLON TIMESTAMP_LITERAL                                  SEMI
    | DAY      COLON DAY_LITERAL  SEMI DATETIME COLON TIMESTAMP_LITERAL SEMI
    ;

arrivalDecl
    : ARRIVAL LBRACE
          AIRPORT COLON airportCode SEMI
          arrivalSchedule
      RBRACE
    ;

arrivalSchedule
    : DATETIME COLON TIMESTAMP_LITERAL SEMI
    ;

fuelDecl
    : FUEL LBRACE
          QUANTITY COLON numericValue SEMI
      RBRACE
    ;

segmentDecl
    : SEGMENT IDENTIFIER LBRACE
          FROM      COLON coordinatePair   SEMI
          TO        COLON coordinatePair   SEMI
          ALTITUDES COLON altitudeSlotList SEMI
          WIND      COLON windDecl         SEMI
      RBRACE
    ;

airportCode      : IATA_CODE | ICAO_CODE ;
coordinatePair   : LPAREN numericValue COMMA numericValue RPAREN ;

// Wind: (direction [degrees], speed unit) — direction accepts optional degree marker
windDecl : LPAREN windDir COMMA numericValue RPAREN ;
windDir  : NUMBER DEGREES? ;

// Altitude slot: target altitude with optional corridor WIDTH
altitudeSlotList : LBRACKET altitudeSlot (COMMA altitudeSlot)* RBRACKET ;
altitudeSlot     : numericValue (WIDTH numericValue)? ;

numericValue : NUMBER unit? ;
unit : UNIT_KG | UNIT_L | UNIT_MS | UNIT_M | UNIT_KMH | UNIT_KM | UNIT_FT | UNIT_KT ;


// ── Lexer rules ───────────────────────────────────────────────────────────────

// Keywords — case-insensitive (each letter matches upper or lower)
FLIGHT      : [fF][lL][iI][gG][hH][tT] ;
LEG         : [lL][eE][gG] ;
DEPARTURE   : [dD][eE][pP][aA][rR][tT][uU][rR][eE] ;
ARRIVAL     : [aA][rR][rR][iI][vV][aA][lL] ;
ROUTE       : [rR][oO][uU][tT][eE] ;
SEGMENT     : [sS][eE][gG][mM][eE][nN][tT] ;
FUEL        : [fF][uU][eE][lL] ;
AIRPORT     : [aA][iI][rR][pP][oO][rR][tT] ;
DATETIME    : [dD][aA][tT][eE][tT][iI][mM][eE] ;
DAY         : [dD][aA][yY] ;
TIME        : [tT][iI][mM][eE] ;
ORIGIN      : [oO][rR][iI][gG][iI][nN] ;
DESTINATION : [dD][eE][sS][tT][iI][nN][aA][tT][iI][oO][nN] ;
FROM        : [fF][rR][oO][mM] ;
TO          : [tT][oO] ;
ALTITUDES   : [aA][lL][tT][iI][tT][uU][dD][eE][sS] ;
WIND        : [wW][iI][nN][dD] ;
QUANTITY    : [qQ][uU][aA][nN][tT][iI][tT][yY] ;
WIDTH       : [wW][iI][dD][tT][hH] ;
REGULAR     : [rR][eE][gG][uU][lL][aA][rR] ;
CHARTER     : [cC][hH][aA][rR][tT][eE][rR] ;
// Extension: aircraft registration and responsible pilot — US080
AIRCRAFT    : [aA][iI][rR][cC][rR][aA][fF][tT] ;
PILOT       : [pP][iI][lL][oO][tT] ;
// Extension: degree marker for wind direction — 'degrees' (case-insensitive) or the degree symbol
DEGREES     : [dD][eE][gG][rR][eE][eE][sS] | '°' ;

// Units — compound tokens before simple (maximal munch)
UNIT_KG  : 'kg' ;
UNIT_MS  : 'm/s' ;   // before UNIT_M
UNIT_M   : 'm' ;
UNIT_L   : 'l' ;
UNIT_FT  : 'ft' ;
UNIT_KMH : 'km/h' ;  // before UNIT_KM
UNIT_KM  : 'km' ;
UNIT_KT  : 'kt' ;

// ISO 8601 timestamp with mandatory timezone — e.g. 2026-05-20T08:00+01:00 or 2026-05-20T08:00Z
// Placed before TIME_LITERAL: both start with digits but TIMESTAMP requires 4 digits + '-'
TIMESTAMP_LITERAL
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
      'T'
      [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])?
      ( 'Z' | ('+' | '-') [0-9][0-9] ':' [0-9][0-9] )
    ;

TIME_LITERAL : [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])? ;  // HH:MM[:SS]

DAY_LITERAL  : [mM][oO][nN][dD][aA][yY]
             | [tT][uU][eE][sS][dD][aA][yY]
             | [wW][eE][dD][nN][eE][sS][dD][aA][yY]
             | [tT][hH][uU][rR][sS][dD][aA][yY]
             | [fF][rR][iI][dD][aA][yY]
             | [sS][aA][tT][uU][rR][dD][aA][yY]
             | [sS][uU][nN][dD][aA][yY]
             ;

ICAO_CODE  : [A-Z][A-Z][A-Z][A-Z] ; // 4 uppercase letters — before IATA (maximal munch)
IATA_CODE  : [A-Z][A-Z][A-Z] ;      // 3 uppercase letters

IDENTIFIER : [a-zA-Z][a-zA-Z0-9_-]* ;
NUMBER     : '-'? [0-9]+ ('.' [0-9]+)? ; // optional minus for negative coordinates

LBRACE   : '{' ;  RBRACE   : '}' ;
LPAREN   : '(' ;  RPAREN   : ')' ;
LBRACKET : '[' ;  RBRACKET : ']' ;
COMMA    : ',' ;  COLON    : ':' ;  SEMI : ';' ;

WS            : [ \t\r\n]+    -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
