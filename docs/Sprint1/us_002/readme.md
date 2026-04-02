# US002 – Project Repository

As Project Manager, I want the team to use the defined project repository in GitHub and setup a GitHub tool for project management.

## 1. Context

This is an initial setup task to ensure the team uses the official GitHub repository and adopts proper project management practices.

### 1.1 List of Issues

- Analysis: #14
- Design: #14
- Implement: #14
- Test: N/A
---

## 2. Requirements

**US002** As Project Manager, I want the team to use the defined project repository in GitHub and setup a GitHub tool for project management.

### Acceptance Criteria:

- **US002.1** The team must use the GitHub repository provided via GitHub Classroom.
- **US002.2** The repository must be updated frequently (daily commits recommended).
- **US002.3** GitHub must be used to manage tasks (issues, boards, or similar).
- **US002.4** Only valid states can be committed (NFR06).

**Dependencies/References:**

This requirement is foundational to all other User Stories, as every development task depends on the repository being properly set up and actively used.

---
## 3. Analysis

The team uses **GitHub Projects** as the primary tool for task management. The board (`sem4pi2526_2dc1-board`) was configured with the following columns:

- **Backlog** – List of all items to be handled in the project, including the project objectives, and predicted overall tasks.
- **Todo** – This item hasn't been started: include all detailed (sliced) tasks, referring to the assignees, and the due dates of each task (described in each added item).
- **In Progress** – This is actively being worked on: list of tasks/items being handled.
- **Testing** – Items that are waiting for testing. Only after all testing stages are concluded can the item be moved to "done".
- **Done** – This has been completed. Meets the ACs (acceptance criteria).
- **Logbook** – Record of the team daily meetings, and project progress insights. Record of all client sessions.
- **Sprint Retrospective** – Gather feedback from the sprint. List what went well, what didn’t, and action items for next time.

Custom fields were defined to enrich each issue:

| Field | Purpose                                                         |
|---|-----------------------------------------------------------------|
| Status | Current state of the item (Todo, In Progress, Done, etc.)       |
| Sub-issues progress | Tracks sub-task completion                                      |
| Priority | High, Medium, Low                                               |
| Iteration | Sprint association (Sprint 1, Sprint 2, Sprint 3)               |
| Start date / End date | Timeline planning                                               |
| Size | Effort estimation (XS, S, M, L, XL)                             |
| Estimated effort (1-13) | Fibonnaci sequence (1,2,3,4,5,8,13) effort estimation for tasks |
| Area | EAPLI, RCOMP, LAPR4, LPROG, SCOMP                               |
| Real executed time (hours) | Actual time spent                                               |
| Estimated time (hours) | Planned time                                                    |

Custom **Insights charts** were also configured:

- Burn Down
- Burn Up / Burn Up Custom
- Execution Time (Hours, Planned vs Real)
- Status chart
- Work Assigned per Team Member
- Work Assigned per UC

### LLM Assistance

There was no need for LLM assistance so no prompts were created.

---
## 4. Implementation

The following was set up:

- Repository created via **GitHub Classroom** under the organisation `Departamento-de-Engenharia-Informatica` following the provided information
- **GitHub Projects board** (`sem4pi2526_2dc1-board`) configured
- **Insights charts** configured
- **Team Workflow and Guidelines** documented in the repository Wiki, covering:
    - Definition of Done (DoD)
    - Daily work routine and commit conventions
    - Smart commits and issue references
    - GitHub Actions / CI pipeline rules
    - Code coverage requirements (≥90%)
    - Documentation standards

*Major commits:*

- 86986deca3f3664fd50d570e65962cd6838353ce

---

## 5. Observations

This US does not involve code development. It is a process and tooling setup task critical for team coordination and evaluation.
Note that irregular commits or poor repository usage negatively impact evaluation.

---