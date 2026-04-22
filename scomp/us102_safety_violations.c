/*
 * US102_safety_violations.c
 * Team: 2dc1 - SCOMP Sprint 2
 * Detect aircraft safety violations in real time
 * Check separation thresholds. Send signals. Log violations.
 */

#define _POSIX_C_SOURCE 200809L

#include <unistd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

#include "shared.h"

/* Calculate horizontal distance (2D) */
float calc_h_distance(const Position3D *p1, const Position3D *p2) {
    float dx = p1->x - p2->x;
    float dy = p1->y - p2->y;
    return sqrtf(dx * dx + dy * dy);
}

/* Calculate vertical distance */
float calc_v_distance(const Position3D *p1, const Position3D *p2) {
    return fabsf(p1->z - p2->z);
}

/* Check if violation exists */
int check_violation(const Position3D *p1, const Position3D *p2,
                    float *h_dist, float *v_dist) {
    *h_dist = calc_h_distance(p1, p2);
    *v_dist = calc_v_distance(p1, p2);

    if (*h_dist < MIN_SEPARATION || *v_dist < MIN_SEPARATION) {
        return 1;
    }
    return 0;
}

/* Record violation in report */
void record_violation(SimulationReport *report, time_t ts,
                      const Flight *f1, const Flight *f2,
                      float h_dist, float v_dist) {
    if (report->total_violations >= MAX_VIOLATIONS) return;

    int idx = report->total_violations;
    report->violations[idx].timestamp = ts;
    strncpy(report->violations[idx].flight1, f1->flight_id, MAX_FLIGHT_ID - 1);
    strncpy(report->violations[idx].flight2, f2->flight_id, MAX_FLIGHT_ID - 1);
    report->violations[idx].distance_h = h_dist;
    report->violations[idx].distance_v = v_dist;
    report->violations[idx].pos1 = f1->current_position;
    report->violations[idx].pos2 = f2->current_position;

    report->total_violations++;
}

/* Notify aircraft via signal */
void notify_aircraft(pid_t pid, const char *flight_id) {
    if (kill(pid, SIGUSR1) == -1) {
        perror("kill");
    } else {
        printf("[SIMULATOR] SIGUSR1 sent to %s\n", flight_id);
    }
}

/* Detect all violations this time step */
int detect_violations(SimulationConfig *config, SimulationReport *report,
                      time_t timestamp) {
    int violations_this_step = 0;

    for (int i = 0; i < config->num_flights; i++) {
        if (config->flights[i].status != 0) continue;

        for (int j = i + 1; j < config->num_flights; j++) {
            if (config->flights[j].status != 0) continue;

            float h_dist, v_dist;
            if (check_violation(&config->flights[i].current_position,
                               &config->flights[j].current_position,
                               &h_dist, &v_dist)) {

                record_violation(report, timestamp,
                               &config->flights[i], &config->flights[j],
                               h_dist, v_dist);
                violations_this_step++;

                printf("[SIMULATOR] VIOLATION: %s <-> %s H=%.1fm V=%.1fm\n",
                       config->flights[i].flight_id,
                       config->flights[j].flight_id,
                       h_dist, v_dist);

                notify_aircraft(config->flights[i].pid,
                              config->flights[i].flight_id);
                notify_aircraft(config->flights[j].pid,
                              config->flights[j].flight_id);
            }
        }
    }

    return violations_this_step;
}

/* Terminate all flights */
void terminate_all(SimulationConfig *config) {
    printf("[SIMULATOR] CRITICAL: Terminating all flights\n");

    for (int i = 0; i < config->num_flights; i++) {
        if (config->flights[i].status == 0) {
            kill(config->flights[i].pid, SIGINT);
            config->flights[i].status = 1;
        }
    }
}

int us102_detect_violations(SimulationConfig *config, SimulationReport *report,
                            time_t timestamp) {
    int violations = detect_violations(config, report, timestamp);

    if (violations > 0) {
        printf("[SIMULATOR] %d violations detected (total: %d)\n",
               violations, report->total_violations);
    }

    /* Check threshold */
    if (report->total_violations >= config->max_violations) {
        terminate_all(config);
        report->passed = 0;
        return 1;  /* Signal to stop */
    }

    return 0;
}