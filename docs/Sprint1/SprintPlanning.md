# Sprint Planning – Semester 4 Integrative Project
## AISafe Flight Control System

**Sprint Duration:** 4 weeks  
**Scrum Master:** LAPR4 PL teacher
**Team:** Jaime, Cláudio, Dinis, Fábio, André
---
## Sprint Goal

Establish foundational project infrastructure and create a well-designed domain model for the AISafe flight control system. This includes team formation and requirements analysis, setting up the GitHub repository, defining the domain model using DDD principles, and providing comprehensive documentation with synchronized domain model diagram and glossary. Deliver a solid architectural and organizational foundation ready for implementation in Sprint 2.
 
---

## Sprint 1 Weekly Plan
### Duration: 9 March – 5 April 2026 (4 weeks)
### Deadline: 5 April 2026, 20:00 (GitHub commit)

### Week 1 (9 March – 15 March)

* **Team Formation & Requirements Analysis**
    - Formalize team composition and roles assignment
    - Read and thoroughly analyze project requirements document
    - Document glossary of domain concepts based on requirements analysis

* **User Stories in Scope:**
    - **US001** – Technical constraints review and documentation
    - **US002** – Project repository setup and GitHub tools configuration (preparation)
    - **US003** – Project structure definition (initial design)

* **Team Activities:**
    - All team members read and annotate requirements document
    - Team discussion to identify unclear requirements
    - Prepare list of clarification questions (functional acceptance criteria)
    - Begin initial glossary of domain concepts
    - Create Scrum board and establish team workflow
    - Assign responsibilities ensuring cross-disciplinary participation

### Week 2 (16 March – 22 March)

* **GitHub Setup & Domain Model Analysis**
    - Complete GitHub repository configuration with CI/CD pipeline
    - Setup Maven project structure with multi-module support (domain, persistence, application layers)
    - Configure GitHub Actions/Workflows for continuous integration
    - Implement build scripts for Unix-compatible systems (build.sh, deploy.sh)
    - Incorporate clarifications from Product Owner into updated glossary
    - Begin domain model design using DDD approach
    - Identify bounded contexts and potential aggregates

* **User Stories in Scope:**
    - **US001** – Technical constraints review and finalized documentation
    - **US002** – Project repository setup and GitHub tools configuration (completion)
    - **US003** – Project structure definition supporting envisioned architecture
    - **US004** – Continuous integration server setup (GitHub Actions)
    - **US005** – Automated deployment scripts (Unix-compatible machines)
    - **US010** – Domain Model elaboration - initial design using DDD
    - **US011** – Aggregate justification - preliminary aggregate identification

* **Team Activities:**
    - Setup Maven multi-module project structure
    - Configure GitHub Actions for automatic builds
    - Create build.sh and deploy.sh scripts for Unix systems
    - Refine glossary with Product Owner clarifications
    - Create Scrum board and backlog with story prioritization
    - Estimate effort/story points for Sprint 1 User Stories
    - Team collaboration on domain model design
    - Document bounded contexts and initial aggregate candidates
    - Create Definitions of Done (DoDs) for all User Stories

### Week 3 (23 March – 29 March)

* **Domain Model Refinement & Documentation**
    - Refine aggregate designs based on peer review feedback from Week 2
    - Complete detailed documentation of domain concepts with UML diagrams
    - Finalize and validate all sequence diagrams for aggregate interactions
    - Update domain model documentation with rationale and design decisions
    - Ensure domain model diagram and glossary are synchronized

* **User Stories in Scope:**
    - **US010** – Domain Model elaboration - refinement and finalization
    - **US011** – Aggregate justification - complete sequence diagrams and explanations

* **Team Activities:**
    - Generate UML diagrams with PlantUML (source files + PNG vector format)
    - Document aggregate responsibilities and invariants
    - Create sequence diagrams for representative scenarios per aggregate
    - Peer review all domain model design documentation
    - Verify consistency between domain model diagram and glossary
    - Update technical documentation in "docs" folder (markdown format)
    - Prepare EAPLI deliverables (see Deliverables section)

### Week 4 (30 March - 5 April)

* **Final Integration & Sprint Completion**
    - Complete all remaining documentation requirements
    - Conduct final peer reviews of domain model and design decisions
    - Verify all Acceptance Criteria met for US001-US011
    - Prepare comprehensive EAPLI deliverables package
    - Ensure all code compiles and builds successfully

* **User Stories in Scope:**
    - **US001** through **US011** – Final verification and closure

* **Team Activities:**
    - Ensure all commits maintain system in valid, compilable state
    - Final review of domain model UML diagrams
    - Generate complete UML documentation (PlantUML source + PNG diagrams)
    - Complete documentation in "docs" folder (markdown format)
    - Finalize glossary with all domain concepts

    - Prepare EAPLI Sprint 1 deliverables ZIP/PDF with:
        * Team composition document
        * Domain model diagram (indicating Value Objects, Entities, and Aggregates)
        * Glossary of domain concepts
        * Aggregate justification with sequence diagrams
        * Honor commitment declaration (signed by all team members)

* **Sprint Review & Retrospective:**
    - Presentation of domain model and architectural decisions
    - Team retrospective to identify improvements for Sprint 2
---