# US003 – Project Structure

As Project Manager, I want the team to configure the project structure to facilitate/accelerate the development of upcoming user stories.

## 1. Context

This task was assigned in Sprint 1 as an initial setup requirement. It is the first time this task is being developed. The goal is to configure the project structure to support the envisioned architecture and facilitate the development of all upcoming user stories, including support for adopted technologies such as ANTLR.

### 1.1 List of Issues

- Analysis: #16
- Design: #16
- Implement: #16
- Test: N/A

---

## 2. Requirements

**US003** As Project Manager, I want the team to configure the project structure to facilitate and accelerate the development of upcoming user stories. Define the structure of the project to support the envisioned architecture, including support for adopted technologies (e.g., ANTLR).


### Acceptance Criteria:

- US003.1 The project must follow a multi-module Maven structure reflecting the envisioned layered architecture.
- US003.2 The structure must be ready to support all adopted technologies, including ANTLR for DSL processing (US083).
- US003.3 The repository structure must accommodate all course components (EAPLI, LPROG, SCOMP, RCOMP) in a clean and organised way.

**Dependencies/References:**

- NFR02 – Documentation in `docs/` folder, markdown format, PlantUML diagrams.
- NFR04 – All source code and artifacts versioned in GitHub.
- NFR05 – Maven as build automation tool.
- NFR08 – Structure must support both in-memory and relational database persistence.
- NFR10 – Java as the main programming language.
- US083 – ANTLR support is set up now so the DSL module is ready for implementation in a future sprint.

---

## 3. Analysis

### 3.0 LLM Assistance

**Prompt used for LLM-assisted analysis:**

> "We are developing a Java-based system called AISafe as part of a university project integrating multiple course units: EAPLI (DDD/Java), LPROG (DSL/ANTLR), SCOMP (C language), and RCOMP (networking). We need to define a clean repository and Maven multi-module project structure that supports all these components. The project is based on an existing template called eapli.base. What is the best way to organise the repository and the Maven modules, considering that SCOMP uses C and RCOMP involves network configurations — neither of which are managed by Maven? Also, how should ANTLR4 support be set up for a future DSL module?"

**LLM suggestions adopted:**
- Keep `eapli.base` as an untouched reference and work on a copy (`aisafe.base`)
- Place `scomp/` and `rcomp/` at the repository root, outside the Maven project
- Create a dedicated `aisafe.dsl` module for ANTLR4, separate from `exemplo.core`

**No changes were made to the LLM suggestions** — the proposed structure was evaluated by the team and accepted as-is, as it aligned with clean architecture principles and the project requirements.

### 3.1 Repository Structure

The project integrates components from multiple course units (EAPLI, LPROG, SCOMP, RCOMP), each with different technologies and build systems. Placing all components inside a single Maven project would be incorrect — Maven does not manage C code (SCOMP) and network configurations (RCOMP) have no place in a Java build system.

The adopted structure separates concerns at the repository level:

```
sem4pi2526-sem4pi2526_2dc1/     ← repository root
├── docs/                        ← all project documentation (NFR02)
│   └── Sprint1/
│       ├── us_001/ ... us_011/
│       └── Glossary.md
├── eapli.base/                  ← original template (reference only, not modified)
├── aisafe.base/                 ← working Java/Maven project (EAPLI + LPROG)
├── scomp/                       ← C language component (SCOMP)
├── rcomp/                       ← Network component (RCOMP)
├── libs/                        ← shared libraries (e.g. PlantUML)
├── generate-plantuml-diagrams.sh
└── readme.md
```

`eapli.base` is the original project template provided and is kept untouched as a reference. `aisafe.base` is a copy of that template adapted for this project — it is where all development takes place. It will be progressively updated throughout the sprints as user stories are implemented, including renaming packages, adding new modules, and replacing example code with the real AISafe domain.

### 3.2 Maven Multi-Module Structure (aisafe.base)

The Java project follows a multi-module Maven layout. Each module has a single, well-defined responsibility aligned with the layered architecture:

| Module | Layer | Purpose |
|---|---|---|
| `exemplo.core` | Domain | Aggregates, entities, value objects, domain services |
| `exemplo.infrastructure.application` | Infrastructure | Application configuration, persistence settings |
| `exemplo.persistence.impl` | Infrastructure | JPA and in-memory repository implementations |
| `exemplo.bootstrappers` | Infrastructure | Data bootstrappers for initial system data |
| `exemplo.app.common.console` | Presentation | Shared console UI utilities |
| `exemplo.app.backoffice.console` | Presentation | Backoffice console application |
| `exemplo.app.user.console` | Presentation | User/Utente console application |
| `exemplo.app.other.console` | Presentation | Other console application |
| `exemplo.app.bootstrap` | Presentation | Bootstrap application entry point |
| `aisafe.dsl` | DSL/LPROG | ANTLR4-based Flight DSL parser (for US083) |

### 3.3 ANTLR4 Module (aisafe.dsl)

The decision to create a dedicated `aisafe.dsl` module for ANTLR, rather than adding it to `exemplo.core`, was made for the following reasons:

- **Separation of concerns** — `exemplo.core` contains domain logic; ANTLR is a parsing technology that should not pollute the domain layer with parser-specific dependencies.
- **Correct dependency direction** — `aisafe.dsl` depends on `exemplo.core` (to produce domain objects from the parsed DSL), but `exemplo.core` does not depend on ANTLR. This respects the clean architecture principle where the domain is independent of external frameworks.
- **Testability** — DSL parsing logic can be tested in isolation without loading the full domain.
- **Future extensibility** — if the DSL grammar evolves or is replaced, only this module needs to change.

The ANTLR4 Maven plugin is configured to automatically generate Java Lexer and Parser classes from `.g4` grammar files located in `src/main/aisafe/` during the build. Both visitor and listener patterns are enabled, as required by US083. At this stage, the `src/main/aisafe/` folder exists but is empty — grammar files will be added when US083 is developed in a future sprint.

### 3.4 SCOMP and RCOMP Folders

The `scomp/` and `rcomp/` folders are placed at the repository root, outside of the Maven project. This decision is justified because:

- Maven does not manage C code — adding SCOMP inside `aisafe.base/` would be architecturally incorrect.
- RCOMP involves network configurations and protocol specifications that are independent of the Java build.
- Keeping them at the root makes the repository structure clear and navigable for all team members regardless of which course unit they are working on.

---

## 4. Design

### 4.1. Realization

The architecture within each Java module follows the standard layered approach:

- **Presentation layer** — console UI classes (`*UI`, `*Action`, `MainMenu`)
- **Application layer** — use case controllers
- **Domain layer** — aggregates, entities, value objects (in `exemplo.core`)
- **Infrastructure/Persistence layer** — JPA repositories, configuration

The `aisafe.dsl` module will follow this internal package structure when implemented (US083):

```
aisafe.dsl/src/main/
├── aisafe/                        ← .g4 grammar files
└── java/
    └── eapli/aisafe/dsl/
        ├── grammar/               ← generated ANTLR classes (do not edit)
        ├── visitor/               ← visitor implementations
        ├── listener/              ← listener implementations
        ├── model/                 ← internal AST / domain representation
        └── validation/            ← semantic validation rules
```

### 4.2. Acceptance Tests

Since this is a structural setup US, validation is done manually.

**Test 1:** Verify the project builds successfully from the root.
```bash
cd aisafe.base
mvn clean install
```
**Refers to Acceptance Criteria:** US003.1 / US003.2

**Test 2:** Verify the `aisafe.dsl` module appears in the reactor build output with `SUCCESS`.
**Refers to Acceptance Criteria:** US003.2

**Test 3:** Verify `scomp/` and `rcomp/` folders exist at the repository root.
**Refers to Acceptance Criteria:** US003.3
 
---

## 5. Implementation

The following was set up:

- `aisafe.base/` — multi-module Maven project copied and adapted from the `eapli.base` template
- Root `pom.xml` updated with:
  - `<aisafe.version>4.13.1</aisafe.version>` property
  - `<module>aisafe.dsl</module>` entry
- `aisafe.dsl/pom.xml` created with ANTLR4 runtime dependency, ANTLR4 Maven plugin with visitor and listener generation enabled, and dependency on `exemplo.core`
- `scomp/` folder created at repository root with `README.md` and `Makefile`
- `rcomp/` folder created at repository root with `README.md`


*Major commits:*

- c3e86d365646b2126653798b5d927036a310fdcc
- ce0a8b885b301f6240e8fb50b0817a3e3c5662b0
- 002d9a5696b997dbf0136a5ec6d5f14d648ed88f

---

## 6. Integration/Demonstration

The full project builds successfully with 11 modules:

```
[INFO] Reactor Summary for exemplo 1.4.0-SNAPSHOT:
[INFO] exemplo .......................... SUCCESS
[INFO] exemplo.infrastructure.application SUCCESS
[INFO] exemplo.core ..................... SUCCESS
[INFO] exemplo.app.common.console ....... SUCCESS
[INFO] exemplo.bootstrapper ............. SUCCESS
[INFO] exemplo.persistence.impl ......... SUCCESS
[INFO] exemplo.app.backoffice.console ... SUCCESS
[INFO] exemplo.app.user.console ......... SUCCESS
[INFO] exemplo.app.other.console ........ SUCCESS
[INFO] exemplo.app.bootstrap ............ SUCCESS
[INFO] aisafe.dsl ....................... SUCCESS
[INFO] BUILD SUCCESS
```

To build from scratch:
```bash
cd aisafe.base
mvn clean install
```
 
---

## 7. Observations

The project structure is based on the **eapli.base** template provided. The `aisafe.base` folder is the working copy where all development takes place; `eapli.base` is kept as the original reference template and is not modified.

The JaCoCo warnings (`Unsupported class file major version 67`) visible during the build are not errors — they occur because the local JDK version is above Java 21. The build and all tests pass successfully.