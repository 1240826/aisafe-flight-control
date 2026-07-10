# LPROG — Flight DSL: Guia Completo para Defesa

Este documento explica tudo o que foi feito na parte de LPROG do projeto, desde a
gramática até aos testes. Assume-se que o leitor não sabe nada de ANTLR — cada
conceito é explicado antes de mostrar como o usámos.

---

## 1. O que é uma DSL e como se processa

Uma **DSL** (Domain-Specific Language) é uma linguagem inventada para um domínio
específico — neste caso, planos de voo. Para a processar, fazemos três análises:

1. **Análise léxica** — pega no texto e separa-o em *tokens* (pedaços com sentido:
   palavras-chave, números, símbolos). Detecta caracteres ilegais.

2. **Análise sintática** — pega na sequência de tokens e verifica se respeitam a
   estrutura definida pela gramática (ex: depois de `flight` vem um identificador,
   depois `:`, depois o tipo, depois `{`...). Constrói uma **árvore** (parse tree).

3. **Análise semântica** — pega na árvore e verifica regras que a gramática não
   consegue exprimir (ex: o combustível não pode ser zero, o aeroporto de chegada
   da perna 1 tem de ser igual ao de partida da perna 2).

O ANTLR é a ferramenta que usamos para fazer tudo isto. Damos-lhe a gramática
(ficheiro `.g4`) e ele gera o lexer, o parser, e as classes base para listeners
e visitors.

---

## 2. A Gramática Completa

Ficheiro: `src/main/antlr4/aisafe/lprog/FlightPlan.g4` (149 linhas)

### 2.1 Gramática completa (tudo junto para referência)

```antlr
grammar FlightPlan;

// ========== PARSER RULES (estrutura) ==========

flightFile : flightDecl EOF;

flightDecl
    : FLIGHT flightId COLON flightType LBRACE
          routeDecl
          AIRCRAFT COLON IDENTIFIER SEMI
          PILOT    COLON IDENTIFIER SEMI
          legDecl+
      RBRACE
    ;

flightId   : IDENTIFIER | NUMBER ;
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
    : DATETIME COLON TIMESTAMP SEMI       // charter: data+hora exata
    | daySchedule+                        // regular: dia da semana + data+hora
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
numericValue     : NUMBER unit? ;
unit             : UNIT_KG | UNIT_L | UNIT_MS | UNIT_M | UNIT_KMH | UNIT_KM | UNIT_FT | UNIT_KT ;

// ========== LEXER RULES (tokens) ==========

// Keywords — case-insensitive (cada letra aceita maiúscula e minúscula)
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

// Units — compostos antes dos simples (maximal munch)
UNIT_MS  : 'm/s' ;    // antes de UNIT_M
UNIT_M   : 'm' ;
UNIT_KMH : 'km/h' ;   // antes de UNIT_KM
UNIT_KM  : 'km' ;
UNIT_KG  : 'kg' ;
UNIT_L   : 'l' ;
UNIT_FT  : 'ft' ;
UNIT_KT  : 'kt' ;

// ISO 8601 timestamp com timezone obrigatório
TIMESTAMP
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
      'T'
      [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])?
      ( 'Z' | ('+' | '-') [0-9][0-9] ':' [0-9][0-9] )
    ;

DAY_OF_WEEK
    : [mM][oO][nN][dD][aA][yY]      // Monday
    | [tT][uU][eE][sS][dD][aA][yY]  // Tuesday
    | [wW][eE][dD][nN][eE][sS][dD][aA][yY]
    | [tT][hH][uU][rR][sS][dD][aA][yY]
    | [fF][rR][iI][dD][aA][yY]
    | [sS][aA][tT][uU][rR][dD][aA][yY]
    | [sS][uU][nN][dD][aA][yY]
    ;

ICAO_CODE  : [A-Z][A-Z][A-Z][A-Z] ;   // 4 letras maiúsculas — declarado PRIMEIRO
IATA_CODE  : [A-Z][A-Z][A-Z] ;        // 3 letras maiúsculas

IDENTIFIER : [a-zA-Z][a-zA-Z0-9_-]* ;
NUMBER     : '-'? [0-9]+ ('.' [0-9]+)? ;

LBRACE : '{' ;  RBRACE : '}' ;
LPAREN : '(' ;  RPAREN : ')' ;
LBRACKET : '[' ;  RBRACKET : ']' ;
COMMA  : ',' ;  COLON  : ':' ;  SEMI : ';' ;

WS            : [ \t\r\n]+    -> skip ;   // espaços, tabs, newlines → ignorar
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;   // comentários de linha
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;   // comentários de bloco
```

### 2.2 Explicação da gramática parte a parte

#### Estrutura geral

A gramática tem duas secções: **regras de parser** (minúsculas, definem a estrutura)
e **regras de lexer** (MAIÚSCULAS, definem os tokens). Está tudo num só ficheiro —
chama-se *combined grammar*.

#### Regra raiz (linha 4)

```
flightFile : flightDecl EOF;
```

`flightFile` é o ponto de entrada. Diz que um ficheiro é exatamente **uma**
declaração de voo seguida do fim do ficheiro (`EOF`). Isto impede que um ficheiro
tenha dois voos — se aparecer um segundo `flight`, o parser dá erro porque esperava
`EOF`. Isto implementa a regra semântica **R1** diretamente na gramática.

#### Declaração de voo

```
flightDecl
    : FLIGHT flightId COLON flightType LBRACE
          routeDecl
          AIRCRAFT COLON IDENTIFIER SEMI
          PILOT    COLON IDENTIFIER SEMI
          legDecl+
      RBRACE
    ;
```

Tradução: a palavra `flight`, seguida de um identificador (ex: `TP1234`), `:`,
o tipo (`regular` ou `charter`), `{`, depois o bloco de rota, a matrícula da
aeronave, o piloto, **uma ou mais** pernas (`+`), e finalmente `}`.

`AIRCRAFT` e `PILOT` são extensões — a especificação base não os exige, mas foram
adicionados porque um plano de voo real precisa dessa informação.

#### Tipo de voo

```
flightType : REGULAR | CHARTER ;
```

Só existem dois tipos. Se o ficheiro tiver `cargo`, o parser dá erro:
`mismatched input 'cargo' expecting {REGULAR, CHARTER}`.

#### Rota

```
routeDecl
    : ROUTE LBRACE
          ORIGIN      COLON airportCode SEMI
          DESTINATION COLON airportCode SEMI
      RBRACE
    ;
```

A rota está ao nível do voo, não dentro de cada perna. Tem uma origem e um destino.
Os códigos de aeroporto podem ser IATA (3 letras, ex: `LIS`) ou ICAO (4 letras,
ex: `LPPT`). A validação de que a origem coincide com a primeira partida e o
destino com a última chegada é feita na análise semântica (regras R5 e R6).

#### Perna (leg)

```
legDecl
    : LEG LBRACE
          departureDecl
          arrivalDecl
          fuelDecl
          segmentDecl+
      RBRACE
    ;
```

Cada perna tem 4 sub-blocos obrigatórios por esta ordem: partida, chegada,
combustível, e **um ou mais** segmentos. O `+` em `segmentDecl+` significa que
a gramática rejeita uma perna sem segmentos — é um erro sintático, não semântico.

#### Partida — dois formatos

```
departureSchedule
    : DATETIME COLON TIMESTAMP SEMI       // voo charter
    | daySchedule+                        // voo regular
    ;

daySchedule
    : DAY COLON DAY_OF_WEEK SEMI DATETIME COLON TIMESTAMP SEMI
    ;
```

A gramática **aceita os dois formatos** para qualquer tipo de voo. Quem verifica
qual é o formato correto é a **regra semântica R11**:

- Voo **charter**: usa `datetime:` com timestamp exato (ex: `2026-06-15T06:00+01:00`)
- Voo **regular**: usa `day:` + `datetime:` (ex: `day: Monday; datetime: 2026-05-18T08:30+01:00`)
- A chegada usa sempre `datetime:` (não precisa de dia da semana)

#### Combustível

```
fuelDecl
    : FUEL LBRACE
          QUANTITY COLON numericValue SEMI
      RBRACE
    ;
```

`numericValue` é `NUMBER unit?` — um número com unidade opcional (`kg`, `l`, etc.).
A gramática permite zero e negativos; a **regra R2** na análise semântica rejeita
valores ≤ 0.

#### Segmento, coordenadas, altitudes

```
segmentDecl
    : SEGMENT LBRACE
          FROM      COLON coordinatePair   SEMI
          TO        COLON coordinatePair   SEMI
          ALTITUDES COLON altitudeSlotList SEMI
      RBRACE
    ;

coordinatePair   : LPAREN numericValue COMMA numericValue RPAREN ;
altitudeSlot     : numericValue WIDTH numericValue ;
```

Cada segmento tem coordenadas de origem e destino, e uma lista de altitudes.
Cada altitude tem obrigatoriamente uma **largura de corredor** (`WIDTH`) — extensão
nossa. Várias altitudes podem ser declaradas: `[10000 m WIDTH 60 m, 11000 m WIDTH 80 m]`.

#### Palavras-chave case-insensitive

```antlr
FLIGHT : [fF][lL][iI][gG][hH][tT] ;
```

Cada letra usa `[xX]` — aceita maiúscula e minúscula. Isto é o padrão ANTLR para
*case-insensitive keywords*. Assim, `flight`, `FLIGHT`, `Flight` produzem todas o
mesmo token.

As keywords são declaradas **antes** do `IDENTIFIER`. Quando o input pode ser
interpretado como keyword ou identificador, o ANTLR escolhe a keyword (primeira
regra que casa).

#### Códigos de aeroporto — ordem importa

```antlr
ICAO_CODE : [A-Z][A-Z][A-Z][A-Z] ;   // 4 letras — PRIMEIRO
IATA_CODE : [A-Z][A-Z][A-Z] ;        // 3 letras — DEPOIS
```

`ICAO_CODE` tem de estar primeiro. Se `IATA_CODE` estivesse primeiro, o ANTLR
fazia *maximal munch* e para `EDDF` (4 letras) tokenizava só as 3 primeiras como
IATA e a quarta como outra coisa. Com ICAO primeiro, 4 letras casam com ICAO,
3 letras caem para IATA.

#### Timestamps

```antlr
TIMESTAMP
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
      'T'
      [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])?
      ( 'Z' | ('+' | '-') [0-9][0-9] ':' [0-9][0-9] )
    ;
```

Formato ISO 8601 estrito: `2026-05-20T08:30+01:00`. Timezone é **obrigatório**
(`+01:00`, `-03:00` ou `Z`). Segundos são opcionais (`:SS`?). Qualquer outro
formato de data (ex: `20-05-2026`) não casa com este token — as partes viram
`NUMBER`s separados e o parser rejeita.

A validade de calendário (ex: 30 de Fevereiro, hora 25) não se verifica no lexer
— é a **regra R10** na análise semântica que tenta fazer `OffsetDateTime.parse()`
e rejeita se falhar.

#### Comentários (extensão)

```antlr
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
```

`-> skip` descarta o token. O parser nunca vê comentários. São uma extensão além
da DSL base, justificada porque ficheiros de plano de voo reais têm anotações.

#### Unidades — ordem importa

```antlr
UNIT_MS  : 'm/s' ;    // ANTES de UNIT_M
UNIT_M   : 'm' ;
UNIT_KMH : 'km/h' ;   // ANTES de UNIT_KM
UNIT_KM  : 'km' ;
```

As unidades compostas (`m/s`, `km/h`) são declaradas antes das simples (`m`, `km`).
Se `UNIT_M` viesse primeiro, `m/s` seria tokenizado como `UNIT_M` + `/s` (erro
léxico). As unidades `ft`, `kt`, `km`, `km/h` são extensões para suportar
notação de aviação real.

---

## 3. Conceito: O que são Listeners e Visitors

Antes de ver o código, é preciso perceber a teoria.

### 3.1 A Parse Tree

Quando o parser termina, produz uma **árvore** (parse tree). Cada nó da árvore
corresponde a uma regra da gramática. Por exemplo:

```
flightFile
└── flightDecl
    ├── flightId ("TP1234")
    ├── flightType ("regular")
    ├── routeDecl
    │   ├── origin ("LIS")
    │   └── destination ("LHR")
    ├── legDecl (1)
    │   ├── departureDecl (LIS, Monday, 2026-05-18T08:30+01:00)
    │   ├── arrivalDecl   (LHR, 2026-05-18T10:45+01:00)
    │   ├── fuelDecl      (15000 kg)
    │   └── segmentDecl   ((38.78,-9.13) → (51.47,-0.46))
    └── legDecl (2)
        └── ...
```

É esta árvore que os listeners e visitors percorrem.

### 3.2 Listener — travessia automática

Um **Listener** é um objeto que reage a eventos. O `ParseTreeWalker` percorre a
árvore automaticamente em profundidade (depth-first) e chama os nossos métodos:

- `enterXxx()` — chamado quando **entra** num nó (antes de visitar os filhos)
- `exitXxx()` — chamado quando **sai** de um nó (depois de visitar todos os filhos)

**Características:**
- Não controlamos a travessia — o walker faz tudo
- Os métodos devolvem `void` — não podem retornar valores
- Para partilhar dados entre métodos, usamos **variáveis de instância** (campos)
- Ideal para tarefas que precisam de visitar **todos** os nós e acumular resultados

**Exemplo da teoria:** Imagina validar que todas as variáveis foram declaradas.
O Listener entra em cada declaração e regista a variável; entra em cada uso e
verifica se existe. A travessia automática garante que nada é esquecido.

### 3.3 Visitor — travessia manual

Um **Visitor** é um objeto que visita nós explicitamente. Somos **nós** que
decidimos quais os filhos a visitar e em que ordem, chamando `visit(filho)`.

**Características:**
- Controlamos totalmente a travessia — se não chamarmos `visit()`, a subárvore é ignorada
- Os métodos podem devolver um valor (definido por um tipo genérico, ex: `String`)
- Ideal para construir resultados hierárquicos (cada nó devolve algo que depende
  do que os filhos devolveram)

**Exemplo da teoria:** Uma calculadora de expressões. O visitor visita `3 + 5`:
`visit(3)` devolve 3, `visit(5)` devolve 5, o nó `+` devolve 3+5=8. O resultado
sobe na árvore naturalmente.

### 3.4 Quando usar cada um

| Situação | Listener | Visitor |
|----------|----------|---------|
| Preciso de visitar tudo | ✅ automático | ❌ tenho de me lembrar |
| Preciso de retornar um valor | ❌ métodos void | ✅ tipo genérico |
| O resultado depende da hierarquia | ❌ preciso de estrutura externa | ✅ natural |
| Quero controlar a ordem | ❌ walker decide | ✅ eu decido |

---

## 4. Implementação do Listener — SemanticValidationListener

**Ficheiro:** `src/main/java/aisafe/lprog/listener/SemanticValidationListener.java` (246 linhas)

### 4.1 O que faz

Valida **11 regras semânticas** depois de o ficheiro ter passado a análise léxica
e sintática. Usa o padrão **Listener** porque precisa de visitar **todos** os nós
da árvore e acumular uma lista de erros.

### 4.2 Como é invocado

```java
SemanticValidationListener semantic = new SemanticValidationListener();
ParseTreeWalker.DEFAULT.walk(semantic, tree);
```

Uma linha. O walker trata de tudo o resto.

### 4.3 Estrutura da classe

```java
public class SemanticValidationListener extends FlightPlanBaseListener {
```

`FlightPlanBaseListener` é gerada pelo ANTLR. Tem **todos** os métodos `enterXxx`
e `exitXxx` já implementados com corpo vazio. Só fazemos override dos que nos
interessam — chama-se **padrão Adaptador**.

### 4.4 Estado interno (como partilhamos dados entre métodos)

Como os métodos do Listener devolvem `void`, não podemos passar valores pelo retorno.
Usamos **campos da classe**:

```java
private String currentFlightType;    // "REGULAR" ou "CHARTER" — capturado no enter
private String routeOrigin;          // preenchido no exitRouteDecl
private String routeDestination;     // preenchido no exitRouteDecl
private String currentDepAirport;    // preenchido no exitDepartureDecl
private String currentDepTimestamp;  // preenchido no exitDepartureDecl
private String currentArrAirport;    // preenchido no exitArrivalDecl
private String currentArrTimestamp;  // preenchido no exitArrivalDecl
private final List<String[]> legs;   // acumulado ao longo das pernas
private final List<String> errors;   // lista de erros acumulados
```

**Raciocínio teórico:** Na teoria de análise semântica, isto corresponde a
**atributos sintetizados** (valores calculados a partir dos filhos) e **atributos
herdados** (valores recebidos do contexto pai). Por exemplo:

- `routeOrigin` é um atributo **sintetizado**: calculado em `exitRouteDecl` a partir
  dos filhos `airportCode(0)` e `airportCode(1)`, e usado mais tarde no pai
  (`exitFlightDecl`).

- `currentFlightType` é um atributo **herdado**: lido em `enterFlightDecl` (do nó
  pai) e usado três níveis abaixo em `exitDepartureDecl` para validar a regra R11.

### 4.5 Fluxo de execução (eventos pela ordem em que acontecem)

```
enterFlightDecl        → captura currentFlightType, limpa estado
  enterRouteDecl
  exitRouteDecl        → guarda routeOrigin, routeDestination
  enterLegDecl (1)
    enterDepartureDecl
    exitDepartureDecl  → valida R10 e R11; guarda depAirport, depTimestamp
    enterArrivalDecl
    exitArrivalDecl    → valida R10; guarda arrAirport, arrTimestamp
    enterFuelDecl
    exitFuelDecl       → valida R2 (fuel > 0)
    enterSegmentDecl
    exitSegmentDecl    → valida R8 (from != to)
    exitAltitudeSlot   → valida R9 (altitude>0, width>0)
  exitLegDecl (1)      → adiciona [dep, depTs, arr, arrTs] à lista legs
  enterLegDecl (2)
    ... (repete para a perna 2)
  exitLegDecl (2)
exitFlightDecl         → valida R3, R4, R5, R6, R7 (usa a lista legs)
```

### 4.6 Porque validamos nos `exit` e não nos `enter`

Quando entramos num nó, os **filhos ainda não foram visitados**. Só no `exit` é que
temos acesso aos dados dos filhos. Por exemplo, `exitFuelDecl` só pode ler o valor
do combustível depois de o filho `numericValue` ter sido processado.

### 4.7 As 11 regras em detalhe

**R1 — Um voo por ficheiro** (linha 4 da gramática)
```
flightFile : flightDecl EOF;
```
A própria gramática força que há exatamente um `flightDecl` antes de `EOF`. Se
aparecer um segundo `flight`, o parser rejeita. Não precisa de código no Listener.

**R2 — Combustível estritamente positivo** (linhas 184-191)
```java
public void exitFuelDecl(FlightPlanParser.FuelDeclContext ctx) {
    double qty = number(ctx.numericValue());
    if (qty <= 0) {
        error(linha, "R2", "fuel quantity must be positive, got: " + qty);
    }
}
```
Lê o número do combustível. Se for ≤ 0, adiciona erro.

**R3 — Ligação entre pernas consecutivas** (linhas 92-98)
```java
// Validado em exitFlightDecl
for (int i = 0; i < legs.size() - 1; i++) {
    String arr     = legs.get(i)[2];     // chegada da perna i
    String nextDep = legs.get(i + 1)[0]; // partida da perna i+1
    if (!arr.equals(nextDep)) {
        error(linha, "R3", "leg " + (i+1) + " arrival '" + arr
            + "' must match leg " + (i+2) + " departure '" + nextDep + "'");
    }
}
```
Percorre a lista de pernas e verifica par a par. Exemplo de erro: perna 1 chega a
EDDF, perna 2 parte de LHR → gap nos aeroportos.

**R4 — Ordem temporal entre pernas** (linhas 101-115)
```java
for (int i = 0; i < legs.size() - 1; i++) {
    OffsetDateTime arrDT     = OffsetDateTime.parse(legs.get(i)[3]);
    OffsetDateTime nextDepDT = OffsetDateTime.parse(legs.get(i + 1)[1]);
    if (!arrDT.isBefore(nextDepDT)) {
        error(linha, "R4", "arrival must be before next departure");
    }
}
```
Usa `OffsetDateTime.parse()` que tem em conta o **timezone**. Dois timestamps com
offsets diferentes (ex: `+01:00` vs `+02:00`) são comparados corretamente em UTC.

**R5 — Origem da rota = primeira partida** (linhas 77-82)
```java
String firstDep = legs.get(0)[0];
if (!routeOrigin.equals(firstDep)) {
    error(linha, "R5", "route origin '" + routeOrigin
        + "' must match first leg departure '" + firstDep + "'");
}
```

**R6 — Destino da rota = última chegada** (linhas 84-89)
```java
String lastArr = legs.get(legs.size() - 1)[2];
if (!routeDestination.equals(lastArr)) {
    error(linha, "R6", "route destination '" + routeDestination
        + "' must match last leg arrival '" + lastArr + "'");
}
```

**R7 — Nenhum aeroporto visitado duas vezes** (linhas 117-126)
```java
Set<String> visited = new LinkedHashSet<>();
visited.add(legs.get(0)[0]);       // partida da primeira perna
for (String[] leg : legs) {
    if (!visited.add(leg[2])) {    // tenta adicionar cada chegada
        error(linha, "R7", "airport '" + leg[2] + "' visited more than once");
    }
}
```
Usa `LinkedHashSet.add()` que devolve `false` se o elemento já existir. As partidas
das pernas seguintes não precisam de ser adicionadas explicitamente porque a regra R3
garante que a partida da perna N+1 é igual à chegada da perna N (que já foi adicionada).

**R8 — Coordenadas do segmento diferentes** (linhas 194-207)
```java
public void exitSegmentDecl(FlightPlanParser.SegmentDeclContext ctx) {
    double fLat = number(ctx.coordinatePair(0).numericValue(0));
    double fLon = number(ctx.coordinatePair(0).numericValue(1));
    double tLat = number(ctx.coordinatePair(1).numericValue(0));
    double tLon = number(ctx.coordinatePair(1).numericValue(1));
    if (fLat == tLat && fLon == tLon) {
        error(linha, "R8", "segment from and to coordinates must differ");
    }
}
```
Compara coordenadas com `Double.compare()` para segurança com vírgula flutuante.

**R9 — Altitude e largura positivas** (linhas 210-222)
```java
public void exitAltitudeSlot(FlightPlanParser.AltitudeSlotContext ctx) {
    double alt = number(ctx.numericValue(0));
    if (alt <= 0) error(linha, "R9", "altitude must be positive");
    double w = number(ctx.numericValue(1));
    if (w <= 0) error(linha, "R9", "corridor width must be positive");
}
```

**R10 — Timestamps ISO 8601 válidos** (linhas 226-233, chamado de exitDepartureDecl e exitArrivalDecl)
```java
private void r10validate(String ts, int line, String label) {
    try {
        OffsetDateTime.parse(ts);
    } catch (DateTimeParseException e) {
        error(line, "R10", label + " timestamp '" + ts + "' is not valid");
    }
}
```
O lexer já garante o formato, mas não valida o calendário. `OffsetDateTime.parse()`
rejeita 30 de Fevereiro, hora 25, mês 13, etc.

**R11 — Formato do schedule coerente com o tipo de voo** (linhas 140-162)
```java
public void exitDepartureDecl(FlightPlanParser.DepartureDeclContext ctx) {
    var schedule = ctx.departureSchedule();
    if (!schedule.daySchedule().isEmpty()) {
        // O ficheiro usou day: + datetime:
        if ("CHARTER".equals(currentFlightType)) {
            error(linha, "R11", "charter must use datetime: only");
        }
    } else {
        // O ficheiro usou só datetime:
        if ("REGULAR".equals(currentFlightType)) {
            error(linha, "R11", "regular must use day: + datetime:");
        }
    }
}
```
`currentFlightType` foi capturado em `enterFlightDecl` (atributo herdado). Aqui,
três níveis abaixo, verificamos se o formato usado no ficheiro coincide com o tipo.

### 4.8 Formato dos erros

```java
private void error(int line, String rule, String msg) {
    errors.add(String.format("[SEMANTIC] line %d - [%s] %s", line, rule, msg));
}
```

Saída: `[SEMANTIC] line 6 - [R5] route origin 'OPO' must match first leg departure airport 'LIS'`

Tem o **tipo de erro** (`SEMANTIC`), a **linha**, a **regra violada** (`R5`), e a
**mensagem descritiva**.

---

## 5. Implementação do Visitor — FlightPlanPrinterVisitor

**Ficheiro:** `src/main/java/aisafe/lprog/visitor/FlightPlanPrinterVisitor.java` (130 linhas)

### 5.1 O que faz

Pega na parse tree e produz um **sumário formatado** do plano de voo. Usa o padrão
**Visitor** porque precisa de construir uma string hierárquica onde cada parte
depende das partes inferiores.

### 5.2 Como é invocado

```java
String summary = new FlightPlanPrinterVisitor().visit(tree);
System.out.println(summary);
```

### 5.3 Porquê Visitor e não Listener

Para construir uma string hierárquica, o Visitor é natural: cada `visitXxx` devolve
a string da sua subárvore, e o nó pai concatena os resultados dos filhos. Com um
Listener (métodos `void`), teríamos de usar uma pilha ou um `StringBuilder` global,
o que é menos limpo.

### 5.4 Estrutura da classe

```java
public class FlightPlanPrinterVisitor extends FlightPlanBaseVisitor<String> {
```

`FlightPlanBaseVisitor<String>` — o tipo genérico `<String>` indica que cada método
`visitXxx` devolve uma `String`.

### 5.5 Como funciona a travessia (controlo explícito)

Ao contrário do Listener, **somos nós que decidimos** quais os filhos a visitar:

```java
public String visitFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
    StringBuilder sb = new StringBuilder();
    // Chamamos visit() explicitamente em cada filho que queremos incluir
    sb.append(String.format("Flight %-10s [%s]\n",
            visit(ctx.flightId()),      // visit filho → devolve "TP1234"
            visit(ctx.flightType())));  // visit filho → devolve "REGULAR"
    sb.append(visit(ctx.routeDecl()));  // visit filho → devolve "Route: LIS -> LHR"
    for (var leg : ctx.legDecl())
        sb.append(visit(leg));          // visit cada perna → devolve string formatada
    return sb.toString();
}
```

Se não chamarmos `visit()` num filho, essa subárvore é **ignorada**. Isto dá-nos
controlo total sobre o que aparece no output.

### 5.6 Exemplo de output

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
  Leg 2
    Departure : LHR  Monday  2026-05-18T12:00+01:00
    Arrival   : CDG  2026-05-18T13:30+02:00
    Fuel      : 8000 kg
    Segment 1 from:(51.4775,-0.4614) to:(49.0097,2.5479) alt:[11000 m width 60 m]
```

### 5.7 Tratamento dos dois formatos de schedule

```java
public String visitDepartureSchedule(DepartureScheduleContext ctx) {
    if (!ctx.daySchedule().isEmpty()) {
        // Formato regular: há vários daySchedule
        StringBuilder sb = new StringBuilder();
        for (var ds : ctx.daySchedule())
            sb.append(visit(ds)).append("  ");
        return sb.toString().trim();
    } else {
        // Formato charter: só um TIMESTAMP
        return ctx.TIMESTAMP().getText();
    }
}
```

O mesmo método `visitDepartureSchedule` trata os dois formatos. Para charter
devolve o timestamp diretamente; para regular visita cada `daySchedule` e
concatena os resultados.

---

## 6. Tratamento de Erros — FlightPlanErrorListener

**Ficheiro:** `src/main/java/aisafe/lprog/errors/FlightPlanErrorListener.java` (62 linhas)

### 6.1 O que é

O ANTLR tem um `ConsoleErrorListener` padrão que imprime erros para o stderr.
Nós substituímo-lo por um **custom error listener** que:

- Formata os erros com `[LEXER]` ou `[PARSER]` (tipo de erro)
- Inclui **linha:coluna**
- Acumula todos os erros numa lista (para serem "collected and reported in a single
  execution" como pede a especificação)

### 6.2 Como funciona

```java
public class FlightPlanErrorListener extends BaseErrorListener {
    private final String phase;               // "LEXER" ou "PARSER"
    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?,?> recognizer,
                            Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        String error = String.format("[%s] line %d:%d - %s",
                phase, line, charPositionInLine, msg);
        errors.add(error);
    }
}
```

O ANTLR chama `syntaxError()` automaticamente quando encontra um erro léxico ou
sintático. Nós formatamos e guardamos. Erros léxicos têm `phase = "LEXER"`, erros
sintáticos têm `phase = "PARSER"`.

### 6.3 Como se instala

```java
lexer.removeErrorListeners();    // remove o listener padrão
lexer.addErrorListener(new FlightPlanErrorListener("LEXER"));
// ... mesmo para o parser com "PARSER"
```

---

## 7. Pipeline Completo — FlightPlanRunner

**Ficheiro:** `src/main/java/aisafe/lprog/dsl/FlightPlanRunner.java` (100 linhas)

### 7.1 As três fases

```
Ficheiro .flightplan
    │
    ▼
┌─────────────────────────────────────────┐
│ FASE 1 — Análise Léxica                 │
│   Ficheiro → CharStream                 │
│   CharStream → FlightPlanLexer          │
│   Erros → [LEXER] line L:C - message    │
│   Saída → CommonTokenStream (tokens)    │
└──────────────────┬──────────────────────┘
                   ▼
┌─────────────────────────────────────────┐
│ FASE 2 — Análise Sintática              │
│   Tokens → FlightPlanParser             │
│   Erros → [PARSER] line L:C - message   │
│   Saída → ParseTree (árvore)            │
└──────────────────┬──────────────────────┘
                   ▼  (só se fases 1 e 2 OK)
┌─────────────────────────────────────────┐
│ FASE 3 — Análise Semântica              │
│   ParseTree → ParseTreeWalker           │
│   + SemanticValidationListener          │
│   Erros → [SEMANTIC] line L - [RX] msg  │
│                                         │
│   (se verbose) FlightPlanPrinterVisitor │
│   Saída → Sumário formatado             │
└─────────────────────────────────────────┘
```

### 7.2 Pormenor importante

A fase 3 **não executa** se as fases 1 ou 2 tiverem erros. Isto evita falsos erros
semânticos sobre uma árvore malformada. Por exemplo, se faltar uma chaveta `}`, a
árvore fica partida e as verificações semânticas iam produzir erros confusos.

---

## 8. Testes

### 8.1 Testes de sintaxe — FlightPlanRunnerTest (27 testes)

**Ficheiro:** `src/test/java/aisafe/lprog/dsl/FlightPlanRunnerTest.java`

Dois métodos auxiliares:
- `resource(nome)` — carrega ficheiros `.flightplan` do classpath
- `parse(conteudo)` — escreve string inline para ficheiro temporário e corre o pipeline

Testes **válidos** (14): voo direto, multi-perna charter, multi-perna regular,
códigos ICAO, keywords em UPPERCASE, mixedCase, tipo charter, múltiplos segmentos,
combustível em litros, comentários, coordenadas negativas, timestamp com segundos,
múltiplos altitude slots, voo regular com day+datetime.

Testes **inválidos** (13): tipo de voo inválido, falta de rota/partida/chegada/
combustível/segmento, chaveta por fechar, formato de data errado, código de
aeroporto inválido, caractere ilegal `@`, ponto e vírgula em falta, ficheiro vazio.

### 8.2 Testes semânticos — SemanticValidationTest (18 testes)

**Ficheiro:** `src/test/java/aisafe/lprog/semantic/SemanticValidationTest.java`

Cada regra R1–R11 tem pelo menos um teste dedicado:

| Regra | Testes | O que valida |
|-------|--------|-------------|
| R1 | `r1DuplicateFlightId`, `r1SameIdInDifferentRuns` | dois voos no mesmo ficheiro → erro; mesmo ID em execuções diferentes → OK |
| R2 | `r2ZeroFuel`, `r2NegativeFuel` | combustível ≤ 0 → erro |
| R3 | `r3LegAirportGap` | gap entre aeroportos de pernas consecutivas → erro |
| R4 | `r4LegTimeOrder` | chegada depois da partida seguinte → erro |
| R5 | `r5RouteOriginMismatch` | origem da rota ≠ primeira partida → erro |
| R6 | `r6RouteDestMismatch` | destino da rota ≠ última chegada → erro |
| R7 | `r7AirportRevisited` | round-trip revisitando aeroporto → erro |
| R8 | `r8SameCoordinates`, `r8DifferentCoordinatesPass` | coordenadas iguais → erro; diferentes → OK |
| R9 | `r9ZeroAltitude`, `r9NegativeWidth` | altitude≤0 ou width≤0 → erro |
| R10 | `r10InvalidCalendarDate`, `r10InvalidTime` | data inválida (Feb 30) ou hora 25 → erro |
| R11 | `r11RegularUsesDatetime`, `r11CharterUsesDay`, `r11RegularWithDayPasses` | charter com day: → erro; regular com datetime: → erro; regular com day: → OK |

Há também testes positivos (`validDirectFlightPassesSemantic`,
`validMultiLegPassesSemantic`) para garantir que o Listener **não** dispara em
ficheiros corretos (sem falsos positivos).

### 8.3 Ficheiros de dados de teste

```
src/main/resources/examples/
├── valid_direct_flight.flightplan            ← voo direto regular
├── valid_multi_leg.flightplan               ← charter com 2 pernas
├── valid_regular_multi_leg.flightplan        ← regular com 2 pernas
├── valid_icao_codes.flightplan              ← códigos ICAO de 4 letras
├── valid_different_units.flightplan          ← altitudes em pés
├── valid_long_haul_charter.flightplan        ← longo curso com 3 segmentos
├── ... (20 ficheiros válidos)
├── invalid_bad_flight_type.flightplan        ← tipo 'cargo' inválido
├── invalid_missing_route.flightplan          ← falta bloco route
├── invalid_missing_semicolon.flightplan      ← falta ';'
├── invalid_unknown_token.flightplan          ← caractere '@'
├── ... (16 ficheiros com erros sintáticos)
├── invalid_sem_duplicate_id.flightplan       ← R1: dois voos
├── invalid_sem_zero_fuel.flightplan          ← R2: fuel = 0
├── invalid_sem_leg_airport_gap.flightplan    ← R3: gap de aeroportos
├── invalid_sem_leg_time_order.flightplan     ← R4: ordem temporal errada
├── ... (12 ficheiros com erros semânticos)
```

---

## 9. Extensões além da DSL base

| Extensão | Onde na gramática | Justificação |
|----------|-------------------|-------------|
| Comentários `//` e `/* */` | `LINE_COMMENT`, `BLOCK_COMMENT` → skip | Anotações humanas em ficheiros de voo |
| Largura de corredor `WIDTH` | `altitudeSlot : numericValue WIDTH numericValue` | Cada altitude tem largura lateral (ATM real) |
| Múltiplos altitude slots | `altitudeSlotList : altitudeSlot (',' altitudeSlot)*` | Transições de altitude (climb/cruise) |
| Unidades `ft`, `kt`, `km`, `km/h` | `UNIT_FT`, `UNIT_KT`, `UNIT_KM`, `UNIT_KMH` | Padrão ICAO/aviação |
| Timestamp com timezone obrigatório | `TIMESTAMP : ... ('Z' \| '+'/'-' HH:MM)` | Comparação correta entre timezones |
| `DAY_OF_WEEK` + duplo formato schedule | `departureSchedule : datetime \| daySchedule+` | Voos regulares (dia da semana) vs charter (data exata) |
| `aircraft` e `pilot` | `AIRCRAFT COLON IDENTIFIER SEMI`, `PILOT COLON IDENTIFIER SEMI` | Plano de voo real precisa de matrícula e piloto |

---

## 10. Configuração Maven

**Ficheiro:** `aisafe.dsl/pom.xml`

```xml
<antlr4.version>4.13.1</antlr4.version>

<plugin>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-maven-plugin</artifactId>
    <configuration>
        <visitor>true</visitor>      <!-- gera BaseVisitor + Visitor -->
        <listener>true</listener>    <!-- gera BaseListener + Listener -->
    </configuration>
</plugin>
```

`mvn compile` gera 8 ficheiros em `target/generated-sources/antlr4/`:
`FlightPlanLexer.java`, `FlightPlanParser.java`, `FlightPlanBaseListener.java`,
`FlightPlanListener.java`, `FlightPlanBaseVisitor.java`, `FlightPlanVisitor.java`,
`FlightPlan.tokens`, `FlightPlanLexer.tokens`.

`mvn test` corre os 45 testes com JUnit 5. JaCoCo gera relatório de cobertura.

---

## 11. Resumo para a defesa

**O que fizemos:** Implementámos uma DSL para planos de voo com ANTLR, seguindo o
pipeline completo de compilação (léxico → sintático → semântico).

**Gramática:** 149 linhas com 22 keywords case-insensitive, tokens para IATA/ICAO,
timestamps ISO 8601, unidades com maximal munch correto, comentários. A estrutura
hierárquica espelha o domínio: voo → rota + pernas → partida/chegada/combustível/
segmentos.

**Listener (SemanticValidationListener):** Valida 11 regras semânticas usando
travessia automática via `ParseTreeWalker`. Acumula estado em campos para regras
cross-block (R3-R7). Usa `OffsetDateTime.parse()` para comparação temporal com
timezone.

**Visitor (FlightPlanPrinterVisitor):** Formata a parse tree como sumário legível.
Travessia manual — cada `visitXxx` chama explicitamente os filhos. Retorna `String`
composta hierarquicamente.

**Erros:** `FlightPlanErrorListener` customizado substitui o padrão do ANTLR.
Formato `[FASE] line L:C - mensagem` cumpre o requisito de tipo+linha+coluna.

**Testes:** 45 testes (27 sintáticos + 18 semânticos) com ficheiros `.flightplan`
dedicados. Cada regra semântica tem testes positivos e negativos.

**Extensões:** Comentários, WIDTH para corredor de altitude, unidades de aviação
(ft, kt, km, km/h), timestamps com timezone, duplo formato de schedule
(charter vs regular), aircraft e pilot.
