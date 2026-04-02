# US005 – Automated Deployment

## 1. Context

This task was assigned in Sprint 1 as an initial setup requirement. It is the first time this task is being developed. The goal is to provide scripts that allow build, execution, and deployment to be performed effortlessly on a Unix-compatible machine, as required by NFR07.

### 1.1 List of Issues

- Analysis: #18
- Design: #18
- Implement: #18
- Test: N/A

---

## 2. Requirements

*US005* As Project Manager, I want the team to add to the project the necessary scripts, so that build/executions/deployments can be executed effortlessly in a Unix compatible machine. Include scripts for all the major tasks and execution of applications.

*Acceptance Criteria:*

- US005.1 Scripts must exist for all major build tasks.
- US005.2 Scripts must exist for executing each application.
- US005.3 Scripts must be compatible with Unix (.sh) and Windows (.bat).
- US005.4 A README.md file must exist at the root explaining how to build, deploy and execute the solution (NFR07).

*Dependencies/References:*

- NFR07 – The repository must include scripts to build and deploy the solution on Linux, and a readme.md explaining how to build, deploy and execute.
- US003 – Depends on the project structure being complete, as scripts operate on the Maven modules.

---

## 3. Analysis

The project template provides a set of shell scripts (.sh) and batch files (.bat) at the root of aisafe.base/ covering all major tasks. Each script wraps the corresponding Maven command or Java execution. Both Unix and Windows variants are provided for every operation, ensuring compatibility across development environments.

The scripts operate on the assumption that:
- JAVA_HOME is set to the JDK folder
- Maven is on the system PATH
- The build has been executed with maven copy-dependencies before running any application

### Scripts available

| Script | Purpose |
|---|---|
| build-all.sh / build-all.bat | Full build: packages all modules, copies dependencies, generates surefire and checkstyle reports |
| quickbuild.sh / quickbuild.bat | Quick build: copies dependencies and runs verify, skipping Javadoc generation |
| rebuild-all.sh / rebuild-all.bat | Clean and full rebuild (calls build-all.sh clean) |
| run-backoffice.sh / run-backoffice.bat | Runs the backoffice console application |
| run-bootstrap.sh / run-bootstrap.bat | Runs the bootstrap application (loads initial data) |
| run-user.sh / run-user.bat | Runs the user/utente console application |
| run-other.sh / run-other.bat | Runs the other console application |
| generate-plantuml-diagrams.sh | Generates PlantUML diagrams from .puml source files |

### LLM Assistance

**Prompt used for LLM-assisted analysis:**

> "Can you validate our drafted shell scripts to ensure they fully cover the requirements of US005? Specifically, check if they handle all major build, deployment, and execution tasks required to run the Java and C applications effortlessly on a Unix machine."

---

## 4. Design

### 4.1. Realization

*Build scripts* wrap Maven goals:

- build-all.sh runs:
  bash
  mvn $1 package dependency:copy-dependencies surefire-report:report \
  -Daggregate=true checkstyle:checkstyle-aggregate


- quickbuild.sh runs:
  bash
  mvn -B $1 dependency:copy-dependencies verify -Dmaven.javadoc.skip=true


- rebuild-all.sh delegates to:
  bash
  ./build-all.sh clean


*Application run scripts* set the classpath using the JARs built by Maven and launch the Java main class directly. For example, run-backoffice.sh:
bash
export BASE_CP=exemplo.app.backoffice.console/target/exemplo.app.backoffice.console-1.4.0-SNAPSHOT.jar:exemplo.app.backoffice.console/target/dependency/*
java -cp $BASE_CP eapli.exemplo.app.backoffice.console.ExemploBackoffice


All run scripts follow the same pattern — they set BASE_CP to point to the module's JAR and its dependency folder, then invoke java -cp.

### 4.2. Acceptance Tests

Since this is a setup US, validation is done manually on a Unix machine.

*Test 1:* Run build-all.sh and verify the project builds successfully.
bash
cd aisafe.base
./build-all.sh

*Refers to Acceptance Criteria:* US005.1 / US005.3

*Test 2:* Run run-backoffice.sh and verify the backoffice application starts.
bash
./run-backoffice.sh

*Refers to Acceptance Criteria:* US005.2 / US005.3

*Test 3:* Run run-bootstrap.sh and verify the bootstrap application loads initial data.
bash
./run-bootstrap.sh

*Refers to Acceptance Criteria:* US005.2

---

## 5. Implementation

All scripts are located at the root of aisafe.base/ and were provided as part of the *eapli.base* project template. Both .sh (Unix) and .bat (Windows) variants exist for every major task.

The scripts were not modified from the template — they cover all required tasks as specified in NFR07.

*Major commits:*

- dca375908aa731d80e23b7b278a08e1c3492a670
- d48f986a60de3b29c8c26914487efa515cf84d5e

---

## 6. Integration/Demonstration

*Before running any application*, the project must be built first:

bash
cd aisafe.base
./build-all.sh


*To run each application:*

bash
# Backoffice console
./run-backoffice.sh

# Bootstrap (load initial data)
./run-bootstrap.sh

# User/Utente console
./run-user.sh

# Other console
./run-other.sh


*Quick build (skips Javadoc):*
bash
./quickbuild.sh


*Clean and rebuild:*
bash
./rebuild-all.sh


*Generate PlantUML diagrams:*
bash
cd ..
./generate-plantuml-diagrams.sh


---

## 7. Observations

The scripts are part of the *eapli.base* template and were not modified. They cover all major tasks required for build, execution, and diagram generation on both Unix and Windows environments, satisfying NFR07.

Note that the run scripts assume that build-all.sh (or quickbuild.sh) has been executed first, as they rely on the JARs and dependency folders generated by Maven's dependency:copy-dependencies goal.