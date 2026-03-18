**5 Non-Functional Requirements (NFR)**

This section presents some specific non-functional requirements. This includes some constraints and concerns that should be considered when designing and implementing the solution.

**NFR01 - Project management using scrum**

Scrum should be used for project management. The group's LAPR4 PL teacher will be the Scrum Master and there will be one weekly scrum meeting with the scrum Master during the LAPR4 PL class.

The Development Process document, available at Moodle, provides detailed information regarding this requirement.

**NFR02 - Technical Documentation**

Project documentation should be always available on the project repository ("docs" folder, markdown format) and, when applicable, in accordance with the UML notation. The development process of every US (e.g.: analysis, design, testing, etc.) must be reported (as part of the documentation).

Whenever possible, the PlantUML tool shall be used to generate diagrams. The diagrams' source files and the actual diagrams in a vector format (PNG) must be included in the repository.

**NFR04 - Source Control**

The source code of the solution as well as all the documentation and related artifacts should be versioned in a GitHub repository to be provided to the students. Only the main (master/main) branch will be used (e.g., as a source for releases).

**NFR05 - Continuous Integration**

The Github repository will provide night builds with publishing of results and metrics. The project will use Maven as build automation tool.

**NFR06 – Commits in a continuous integration environment**

Any commit must change the system from a valid state to another valid state. If a commit leads to the system failing to compile and pass the tests, the author will get a grade of zero in LAPR4 in that sprint.

**NFR03 – Unit testing requirements**

It is recommended that each team member adopts a test-driven development approach locally. Test coverage of Java controller and domain packages cannot be below 90% at any time.

Unit testing should follow industry best practices, such as the AAA convention.

**NFR07 - Deployment and Scripts**

The repository should include the necessary scripts to build and deploy the solution in a Linux system. It should also include a readme.md file in the root folder explaining how to build, deploy and execute the solution.

**NFR08 - Database by configuration**

The system must support that data persistence is done either "in memory" or in a relational database (RDBMS). Although in-memory database solutions should be used during development and testing, the solution must include a final deployment where a remote persistent relational database is used.

The system should have the ability to initialize some default data.

**NFR09 - Authentication and Authorization**

The system must support and apply authentication and authorization for all its users and functionalities.

**NFR10 - Programming language**

The solution should be implemented using Java as the main language. Other languages can be used in accordance with more specific requirements.

**NFR11 – LPROG Assessment**

The quality of:
 - the DSL conceptual design;
 - grammar clarity and correctness;
 - robustness of lexical and syntactic analysis;
 - completeness and rigor of semantic validation;
 - quality of error reporting;
 - innovation and justified extension of the DSL (lexical and syntactic levels);
 - internal representation design (AST/domain model).

Will be central elements in the assessment of the Languages and Programming course unit.

Projects that go beyond the minimal Core DSL specification in a coherent and well-structured manner will be positively valued.