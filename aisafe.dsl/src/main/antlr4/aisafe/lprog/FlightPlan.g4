grammar FlightPlan;


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

routeDecl
    : ROUTE LBRACE
          ORIGIN      COLON airportCode SEMI
          DESTINATION COLON airportCode SEMI
      RBRACE
    ;

legDecl
    : LEG LBRACE
          departureDecl
          arrivalDecl
          fuelDecl
          segmentDecl+
      RBRACE
    ;

segmentDecl
    : SEGMENT LBRACE
          FROM      COLON coordinatePair   SEMI
          TO        COLON coordinatePair   SEMI
          ALTITUDES COLON altitudeSlotList SEMI
      RBRACE
    ;

departureDecl
    : DEPARTURE LBRACE
          AIRPORT COLON airportCode SEMI
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

fuelDecl
    : FUEL LBRACE
          QUANTITY COLON numericValue SEMI
      RBRACE
    ;

airportCode      : IATA_CODE | ICAO_CODE ;
coordinatePair   : LPAREN numericValue COMMA numericValue RPAREN ;

altitudeSlotList : LBRACKET altitudeSlot (COMMA altitudeSlot)* RBRACKET ;
altitudeSlot     : numericValue WIDTH numericValue ;

numericValue : NUMBER unit? ;
unit         : UNIT_KG | UNIT_L | UNIT_MS | UNIT_M | UNIT_KMH | UNIT_KM | UNIT_FT | UNIT_KT ;


// Keywords — case-insensitive
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
ORIGIN      : [oO][rR][iI][gG][iI][nN] ;
DESTINATION : [dD][eE][sS][tT][iI][nN][aA][tT][iI][oO][nN] ;
FROM        : [fF][rR][oO][mM] ;
TO          : [tT][oO] ;
ALTITUDES   : [aA][lL][tT][iI][tT][uU][dD][eE][sS] ;
QUANTITY    : [qQ][uU][aA][nN][tT][iI][tT][yY] ;
WIDTH       : [wW][iI][dD][tT][hH] ;
REGULAR     : [rR][eE][gG][uU][lL][aA][rR] ;
CHARTER     : [cC][hH][aA][rR][tT][eE][rR] ;
AIRCRAFT    : [aA][iI][rR][cC][rR][aA][fF][tT] ;
PILOT       : [pP][iI][lL][oO][tT] ;

// Units — compound tokens before simple
UNIT_KG  : 'kg' ;
UNIT_MS  : 'm/s' ;
UNIT_M   : 'm' ;
UNIT_L   : 'l' ;
UNIT_FT  : 'ft' ;
UNIT_KMH : 'km/h' ;
UNIT_KM  : 'km' ;
UNIT_KT  : 'kt' ;

// ISO 8601 timestamp with mandatory timezone — e.g. 2026-05-20T08:00+01:00 or 2026-05-20T08:00Z
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

ICAO_CODE  : [A-Z][A-Z][A-Z][A-Z] ;
IATA_CODE  : [A-Z][A-Z][A-Z] ;

IDENTIFIER : [a-zA-Z][a-zA-Z0-9_-]* ;
NUMBER     : '-'? [0-9]+ ('.' [0-9]+)? ;

LBRACE   : '{' ;  RBRACE   : '}' ;
LPAREN   : '(' ;  RPAREN   : ')' ;
LBRACKET : '[' ;  RBRACKET : ']' ;
COMMA    : ',' ;  COLON    : ':' ;  SEMI : ';' ;

WS            : [ \t\r\n]+    -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
