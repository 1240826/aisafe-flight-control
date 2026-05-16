# AISafe Management
     _    ___ ____        __
    / \  |_ _/ ___|  __ _/ _| ___
   / _ \  | |\___ \ / _` | |_ / _ \
  / ___ \ | | ___) | (_| |  _|  __/
 /_/   \_\___|____/ \__,_|_|  \___|

Engenharia de Aplicações (EAPLI)

Polytechnic of Porto, School of Engineering

---

AISafe is a flight control information management system developed as part of
the EAPLI integrative project. It manages manufacturers, engine models, aircraft
models, aircraft, air control areas, airports, air transport companies,
collaborators and weather data.

## Who do I talk to?

Nuno Pereira [nap@isep.ipp.pt](mailto:nap@isep.ipp.pt)

## License and copyright

Paulo Gandra de Sousa [pag@isep.ipp.pt](mailto:pag@isep.ipp.pt)

Copyright (c) 2013-2024 the original author or authors.

MIT License

## Build

Make sure Maven and a JDK (Java 21+) are installed and on the PATH.

```
quickbuild.bat          (Windows)
./quickbuild.sh         (Linux/macOS)
```

For a full build including tests and reports:

```
build-all.bat           (Windows)
./build-all.sh          (Linux/macOS)
```

For a clean rebuild:

```
rebuild-all.bat         (Windows)
./rebuild-all.sh        (Linux/macOS)
```

## Running

Make sure a JRE is installed and on the PATH.
Run `quickbuild` first (copies dependencies to `app/target/dependency/`).

**Back office console:**
```
run-backoffice.bat      (Windows)
./run-backoffice.sh     (Linux/macOS)
```

**Bootstrap data (seed master + demo data):**
```
run-bootstrap.bat       (Windows)
./run-bootstrap.sh      (Linux/macOS)
```

## Project structure

```
aisafe/
  core/           Domain model, application controllers, repository interfaces,
                  infrastructure (Application, AppSettings, auth, persistence context)

  persistence/    JPA and InMemory repository implementations
                  persistence.xml, application.properties

  app/            Console UI (ui/), bootstrappers (bootstrap/)
                  Two entry points: AISafeBackoffice, AISafeBootstrapApp

aisafe.dsl/       ANTLR grammar and flight plan DSL (LPROG)
```

## Architecture

The application follows a layered approach:

```
UI  →  Controller  →  Repository (interface)
              |              ↑
              ↓              |
           Domain    Persistence impl (JPA / InMemory)
```

### Domain objects

Domain objects have no persistence logic — the controller retrieves them from the
repository, delegates business logic to the domain object, then stores them back.
This keeps domain classes independently testable.

### JPA persistence

- `@Entity` / `@Embeddable` for aggregates and value objects
- `@EmbeddedId` for natural VO-based primary keys
- `@GeneratedValue` for surrogate keys (Collaborator)
- `@Version` for optimistic locking on all aggregates
- `@Inheritance(SINGLE_TABLE)` for Collaborator hierarchy (ATC / FCO / WeatherPerson)

### Switching between JPA and InMemory

In `app/src/main/resources/application.properties`:

```properties
# JPA (H2 file database — default)
persistence.repositoryFactory=eapli.aisafe.persistence.jpa.JpaRepositoryFactory

# InMemory (for quick local testing, no DB required)
#persistence.repositoryFactory=eapli.aisafe.persistence.inmemory.InMemoryRepositoryFactory
```

### References

- [EAPLI framework](https://bitbucket.org/pag_isep/eapli.framework/src/master/README.md)
- [Entities or DTOs in JPA Queries](https://thoughts-on-java.org/entities-dtos-use-projection/)
- [Primary key mapping](https://thoughts-on-java.org/primary-key-mappings-jpa-hibernate/)
