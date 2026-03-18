# US004 – Continuous Integration Server

As Project Manager, I want the team to setup a continuous integration server. GitHub Actions/Workflows should be used.

## 1. Context

This US ensures automated validation of the project using Continuous Integration, as required by the project constraints.

---

## 2. Requirements

**US004** As Project Manager, I want the team to setup a continuous integration server.

### Acceptance Criteria:

- **US004.1** GitHub Actions must be configured.
- **US004.2** The project must build automatically.
- **US004.3** Tests must run automatically (when applicable).
- **US004.4** Build results must be available (NFR05).
- **US004.5** Invalid commits must break the pipeline (NFR06).

---

## 3. Implementation

- GitHub Actions workflow created:
    - build with Maven
    - run tests
- Pipeline triggered on each commit/push.

---

## 4. Observations

Continuous Integration ensures system stability and enforces good development practices.