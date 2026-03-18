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

[Planning and Technical Documentation](docs/Sprint1/SprintPlanning.md)

## 3. How to Build

*To Do*

## 4. How to Execute Tests

*To Do*

## 5. How to Run

*To Do*

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)

*To Do*

## 7. How to Generate PlantUML Diagrams

To generate plantuml diagrams for documentation execute the script (for the moment, only for linux/unix/macos):

    ./generate-plantuml-diagrams.sh


