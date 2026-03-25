# US004 – Continuous Integration Server

As Project Manager, I want the team to setup a continuous integration server.

## 1. Context

This task was assigned in Sprint 1 as an initial infrastructure requirement. It is the first time this task is being developed. The goal is to configure a Continuous Integration (CI) pipeline using GitHub Actions so that every change pushed to the repository is automatically built, tested, and analysed — ensuring the project always remains in a valid, compilable, and testable state as required by NFR06.

---

## 2. Requirements

**US004** As Project Manager, I want the team to setup a continuous integration server. GitHub Actions/Workflows should be used.

### Acceptance Criteria:

- **US004.1** A GitHub Actions workflow must be configured in the repository to automatically trigger on push and pull request events to `main`.
- **US004.2** The workflow must perform a full Maven build of the `aisafe.base` multi-module project using the existing `build-all.sh` script, including ANTLR code generation.
- **US004.3** The workflow must execute all unit tests via Maven Surefire and fail the build if any test fails (NFR06).
- **US004.4** The workflow must run Checkstyle aggregate analysis as part of the build.
- **US004.5** A nightly build must be scheduled to provide overnight build results and metrics (NFR05).
- **US004.6** JaCoCo coverage reports must be generated and published as downloadable workflow artifacts to support the 90% coverage requirement (NFR03).
- **US004.7** Surefire aggregate and Checkstyle reports must be published as downloadable workflow artifacts.

**Dependencies/References:**

- NFR03 – Test coverage of Java controller and domain packages cannot be below 90%.
- NFR04 – All source code and artifacts versioned in GitHub.
- NFR05 – Maven as build automation tool; nightly builds with publishing of results and metrics.
- NFR06 – Any commit must change the system from a valid state to another valid state. Failure to compile or pass tests results in a grade of zero in LAPR4 for that sprint.
- US003 – Project structure and `build-all.sh` script must be in place before CI can be configured.

---

## 3. Analysis

### 3.1 CI Tool Selection

GitHub Actions was chosen because:

- The repository is already hosted on GitHub (GitHub Classroom), so no external CI service is needed.
- GitHub Actions is natively integrated with the repository — triggers on push, pull request, and schedule are built-in.
- Workflows are stored as YAML files inside `.github/workflows/`, versioned alongside the source code (NFR04).
- The GitHub-hosted free tier covers the team's usage needs without additional infrastructure.

### 3.2 Trigger Strategy

Three trigger types were configured:

| Trigger | Event | Purpose |
|---|---|---|
| `push` | to `main` | Validate every commit merged into the main branch (NFR06) |
| `pull_request` | to `main` | Validate changes before they are merged |
| `schedule` | cron `0 2 * * *` | Nightly build at 02:00 UTC for overnight metrics (NFR05) |

### 3.3 Build Strategy

The workflow delegates to the existing `build-all.sh` script (located in `aisafe.base/`) with the `clean` argument, which executes:

```
mvn clean package dependency:copy-dependencies \
    surefire-report:report -Daggregate=true \
    checkstyle:checkstyle-aggregate
```

This single command:
1. Triggers the ANTLR4 Maven plugin to generate Lexer/Parser Java classes from `.g4` grammar files.
2. Compiles all 11 modules in the correct Maven reactor order.
3. Executes all unit tests via Maven Surefire.
4. Copies all runtime dependencies (required for the run scripts).
5. Generates an aggregated Surefire HTML test report.
6. Runs Checkstyle aggregate analysis across all modules.

Using `build-all.sh` instead of calling `mvn` directly ensures CI uses exactly the same build entry point as local development, avoiding divergence between environments.

### 3.4 JaCoCo Coverage Reports

The root `pom.xml` configures the JaCoCo Maven plugin with the `prepare-agent` goal only — this instruments the JVM during tests but does not generate HTML/XML reports automatically. A separate explicit `mvn jacoco:report` step is added after the main build to produce the per-module coverage reports. This is consistent with how the plugin is declared in the root `pom.xml` and requires no changes to the existing Maven configuration.

### 3.5 Script Permissions

The shell scripts are developed on Windows, which does not preserve the Unix executable bit in Git. On the GitHub Actions Linux runner (`ubuntu-latest`), a `chmod +x *.sh` step is required before calling any script.

---

## 4. Design

### 4.1 Workflow File Location

```
.github/
└── workflows/
    └── ci.yml    ← single workflow file covering all CI requirements
```

### 4.2 Job Structure

The workflow contains a single job (`build`) with the following steps:

| Step | Action | Purpose |
|---|---|---|
| 1 | `actions/checkout@v4` | Fetch full repository contents |
| 2 | `actions/setup-java@v4` | Install JDK 21 (Temurin) and restore Maven cache |
| 3 | `chmod +x *.sh` | Ensure scripts are executable on the Linux runner |
| 4 | `./build-all.sh clean` | Full build: compile, test, copy-deps, surefire report, checkstyle |
| 5 | `mvn jacoco:report` | Generate JaCoCo HTML/XML coverage reports per module |
| 6 | `upload-artifact` | Publish JaCoCo reports (14-day retention) |
| 7 | `upload-artifact` | Publish Surefire reports (14-day retention) |
| 8 | `upload-artifact` | Publish Checkstyle results (14-day retention) |

Steps 5–8 run with `if: always()` so that artifacts are uploaded even when the build fails, allowing the team to inspect reports on a broken build.

### 4.3 Acceptance Tests

**Test 1:** Push a commit to `main` and verify a new workflow run appears in the Actions tab with status `SUCCESS`.
**Refers to Acceptance Criteria:** US004.1 / US004.2 / US004.3

**Test 2:** Verify the Maven reactor summary in the workflow logs shows all 11 modules completing with `SUCCESS`.
**Refers to Acceptance Criteria:** US004.2

**Test 3:** Introduce a deliberate compilation error in a branch, open a pull request to `main`, and verify the workflow run fails with `FAILURE`.
**Refers to Acceptance Criteria:** US004.1 / US004.3

**Test 4:** Open a completed workflow run and confirm that `jacoco-reports`, `surefire-reports`, and `checkstyle-reports` artifacts are available for download.
**Refers to Acceptance Criteria:** US004.6 / US004.7

**Test 5:** Wait for the following morning and verify a scheduled nightly build run appears in the Actions tab.
**Refers to Acceptance Criteria:** US004.5

---

## 5. Implementation

The following was created:

- `.github/workflows/ci.yml` — GitHub Actions workflow with:
  - **Triggers:** `push` to `main`, `pull_request` to `main`, nightly `schedule` (cron `0 2 * * *`).
  - **Runner:** `ubuntu-latest` with JDK 21 (Temurin distribution).
  - **Maven cache:** enabled via `actions/setup-java@v4`, keyed on `pom.xml` files and invalidated automatically when dependencies change.
  - **Build step:** `./build-all.sh clean` inside `aisafe.base/`, running `mvn clean package dependency:copy-dependencies surefire-report:report -Daggregate=true checkstyle:checkstyle-aggregate`.
  - **JaCoCo report step:** explicit `mvn jacoco:report` to generate HTML/XML coverage output (the `prepare-agent` goal alone does not produce reports).
  - **Artifact uploads** (`if: always()`): `jacoco-reports`, `surefire-reports`, `checkstyle-reports`, all retained for 14 days.

---

## 6. Integration/Demonstration

After committing `.github/workflows/ci.yml` to `main`, the GitHub Actions tab shows:

- The workflow listed under **All workflows**.
- Automatic runs triggered on each push and pull request to `main`.
- Each run displays the full Maven reactor summary with all 11 modules passing.
- Three downloadable artifacts per completed run: `jacoco-reports`, `surefire-reports`, `checkstyle-reports`.

Example reactor output visible in CI logs:

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

---

## 7. Observations

- The `schedule` trigger only runs on the repository's **default branch** (`main`). If the default branch is renamed, the workflow file must be committed to the new default branch for the schedule to remain active.
- JaCoCo warnings (`Unsupported class file major version 67`) visible in CI logs are not errors. They occur because JaCoCo 0.8.11 has limited support for class files compiled above Java 21. The build and all tests pass successfully despite these warnings.
- The `jacoco:report` step is separate from the main build because the root `pom.xml` only declares the `prepare-agent` execution. Adding a `report` execution directly to the `pom.xml` would be a cleaner long-term solution but is out of scope for this US.
- The Maven dependency cache is keyed automatically by the `setup-java` action using the project's `pom.xml` files. The cache is invalidated whenever any dependency version changes.
- If additional modules are added to `aisafe.base/` in future sprints, no changes to `ci.yml` are required — `build-all.sh` calls Maven at the root level, which automatically picks up new modules declared in the root `pom.xml`.