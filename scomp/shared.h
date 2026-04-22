/*
 * shared.h - SCOMP flight simulation shared data structures
 * Team: 2dc1 - SCOMP Sprint 2
 * Date: April 2026
 */

#ifndef SHARED_H
#define SHARED_H

#include <time.h>

/* Constants */
#define MAX_FLIGHTS 50
#define MAX_VIOLATIONS 100
#define MAX_FLIGHT_ID 8
#define MIN_SEPARATION 600.0f

/* Data types */
typedef struct {
    float x, y, z;      /* Position meters */
    float vx, vy, vz;   /* Velocity m/s */
} Position3D;

typedef struct {
    time_t timestamp;
    char flight_id[MAX_FLIGHT_ID];
    Position3D position;
    int status;         /* 0=OK, 1=VIOLATION */
} FlightMovement;

typedef struct {
    char id[MAX_FLIGHT_ID];
    char departure[5];
    char arrival[5];
    Position3D start_pos;
    Position3D end_pos;
    float cruise_speed;
    int duration;       /* seconds */
    time_t departure_time;
} FlightPlan;

typedef struct {
    pid_t pid;
    char flight_id[MAX_FLIGHT_ID];
    FlightPlan plan;
    Position3D current_position;
    Position3D previous_position;
    int violation_count;
    int status;         /* 0=RUNNING, 1=COMPLETED */
    time_t start_time;
} Flight;

typedef struct {
    time_t timestamp;
    char flight1[MAX_FLIGHT_ID];
    char flight2[MAX_FLIGHT_ID];
    float distance_h;
    float distance_v;
    Position3D pos1;
    Position3D pos2;
} SafetyViolation;

typedef struct {
    int num_flights;
    int total_time;
    int time_step;
    int max_violations;
    Flight flights[MAX_FLIGHTS];
} SimulationConfig;

typedef struct {
    time_t start_time;
    time_t end_time;
    int total_flights;
    int total_violations;
    int passed;
    SafetyViolation violations[MAX_VIOLATIONS];
} SimulationReport;

#endif