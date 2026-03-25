# Project AIControl

## 1. Description of the Project

AISafe is a startup developing a prototype for aircraft flight control management. This project implements the software to manage the back office of the system, which includes:

* Basic configuration (register and update aircraft models, aircraft engine models, aircraft and engine manufacturers, air transport companies, airports, etc.)
* Flight management (create, verify, etc.)
* Weather service (AI enhanced weather service to be used by all system instances)
* Flight simulation using a Domain Specific Language (DSL) to describe flights
* Flight control coordination with safety violation detection and collision avoidance

The system is designed to be scalable and support many simultaneous flights, with the capability to parallelize simulation across multiple subareas of airspace.

## 2. Planning and Technical Documentation

[Planning and Technical Documentation](docs/Sprint1/Scrum/SprintPlanning.md)

## 3. How to Build

Make sure `JAVA_HOME` is set to the JDK folder and Maven is on the system `PATH`.

**Full build** (packages all modules, copies dependencies, generates reports):

```bash
cd aisafe.base
./build-all.sh
```

**Quick build** (skips Javadoc generation):

```bash
cd aisafe.base
./quickbuild.sh
```

**Clean and full rebuild:**

```bash
cd aisafe.base
./rebuild-all.sh
```

## 4. How to Execute Tests

Tests run automatically during the build. To run them explicitly:

```bash
cd aisafe.base
mvn test
```

To generate a test coverage report (JaCoCo):

```bash
cd aisafe.base
mvn clean install
mvn jacoco:report
```

The report is available at `target/site/jacoco/index.html` inside each module.

## 5. How to Run

**Before running any application**, the project must be built first (see section 3).

> **Note:** The current application names and scripts (e.g. `run-backoffice`, `run-user`) are inherited from the **eapli.base** template. As the AISafe domain is implemented throughout the sprints, these will be renamed and updated to reflect the actual system applications.

**Backoffice console application:**

```bash
cd aisafe.base
./run-backoffice.sh
```

**User/Utente console application:**

```bash
cd aisafe.base
./run-user.sh
```

**Other console application:**

```bash
cd aisafe.base
./run-other.sh
```

**Bootstrap application** (loads initial data into the system):

```bash
cd aisafe.base
./run-bootstrap.sh
```

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)

1. Ensure the target machine has **Java 21** and **Maven** installed and configured.
2. Clone the repository:

```bash
git clone <repository-url>
```

3. Build the project:

```bash
cd aisafe.base
./build-all.sh
```

4. Run the desired application using the appropriate script (see section 5).

> For Windows, use the equivalent `.bat` scripts (e.g., `build-all.bat`, `run-backoffice.bat`).

## 7. How to Generate PlantUML Diagrams

To generate PlantUML diagrams for documentation, execute the script (for Linux/Unix/macOS):

```bash
./generate-plantuml-diagrams.sh
```