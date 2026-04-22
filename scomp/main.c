/*
 * main.c - SCOMP Flight Simulation System
 * Team: 2dc1 - SCOMP Sprint 2
 * Main entry point and sample flight data
 */

#define _POSIX_C_SOURCE 200809L

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include "shared.h"

extern int us103_run_simulation(SimulationConfig *config, SimulationReport *report);
extern int child_send_positions(Flight *flight, int write_fd, int time_step);
extern int us109_generate_report(SimulationReport *report);

/* Create sample flight data */
void create_sample_flights(SimulationConfig *config) {
    struct {
        const char *id;
        const char *dep;
        const char *arr;
        float sx, sy, sz;
        float ex, ey, ez;
        int dur;
    } flights[] = {
        {"TAP100", "OPO", "LIS", 40.25, -8.68, 1000, 40.00, -9.13, 2000, 900},
        {"TAP101", "OPO", "LIS", 40.25, -8.68, 3000, 40.00, -9.13, 4000, 900},
        {"TAP200", "LIS", "OPO", 40.00, -9.13, 2500, 40.25, -8.68, 1500, 900},
    };

    time_t base = time(NULL);

    for (int i = 0; i < config->num_flights; i++) {
        config->flights[i].flight_id[0] = '\0';
        strncpy(config->flights[i].flight_id, flights[i].id, MAX_FLIGHT_ID - 1);

        config->flights[i].plan.departure[0] = '\0';
        strncpy(config->flights[i].plan.departure, flights[i].dep, 4);

        config->flights[i].plan.arrival[0] = '\0';
        strncpy(config->flights[i].plan.arrival, flights[i].arr, 4);

        config->flights[i].plan.start_pos.x = flights[i].sx;
        config->flights[i].plan.start_pos.y = flights[i].sy;
        config->flights[i].plan.start_pos.z = flights[i].sz;
        config->flights[i].plan.start_pos.vx = 0;
        config->flights[i].plan.start_pos.vy = 0;
        config->flights[i].plan.start_pos.vz = 0;

        config->flights[i].plan.end_pos.x = flights[i].ex;
        config->flights[i].plan.end_pos.y = flights[i].ey;
        config->flights[i].plan.end_pos.z = flights[i].ez;
        config->flights[i].plan.end_pos.vx = 0;
        config->flights[i].plan.end_pos.vy = 0;
        config->flights[i].plan.end_pos.vz = 0;

        config->flights[i].plan.cruise_speed = 200;
        config->flights[i].plan.duration = flights[i].dur;
        config->flights[i].plan.departure_time = base + (i * 10);

        config->flights[i].current_position = config->flights[i].plan.start_pos;
        config->flights[i].previous_position = config->flights[i].plan.start_pos;
        config->flights[i].status = 0;
        config->flights[i].violation_count = 0;
    }
}

int main(int argc, char *argv[]) {
    SimulationConfig config;
    SimulationReport report;

    memset(&config, 0, sizeof(SimulationConfig));
    memset(&report, 0, sizeof(SimulationReport));

    /* Configuration */
    config.num_flights = 3;
    config.total_time = 60;
    config.time_step = 1;
    config.max_violations = 100;

    /* Create sample flights */
    create_sample_flights(&config);

    /* Run simulation (US100, US101, US102, US103) */
    int result = us103_run_simulation(&config, &report);

    /* Generate report (US109) */
    if (result == 1) {
        /* Parent process completed simulation */
        us109_generate_report(&report);
    } else if (result == 0) {
        /* Child process - send positions and wait for signals */
        for (int i = 0; i < config.num_flights; i++) {
            if (config.flights[i].pid == 0 || config.flights[i].pid == getpid()) {
                /* This child */
                int fd[2];
                /* Get write fd from parent... */
                /* This is handled in child_send_positions() */
                break;
            }
        }
    } else {
        fprintf(stderr, "Simulation failed\n");
        return 1;
    }

    return 0;
}