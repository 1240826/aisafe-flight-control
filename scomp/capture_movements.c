/*
 * US101_capture_movements.c
 * Team: 2dc1 - SCOMP Sprint 2
 * Capture and process flight movements
 * Child sends position updates via pipe. Parent reads and stores.
 */

#define _POSIX_C_SOURCE 200809L

#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

#include "shared.h"

/* Calculate position at time t using linear interpolation */
void calc_position(const FlightPlan *plan, int elapsed, Position3D *pos) {
    float progress = (float)elapsed / plan->duration;
    if (progress > 1.0f) progress = 1.0f;

    /* Linear position */
    pos->x = plan->start_pos.x + (plan->end_pos.x - plan->start_pos.x) * progress;
    pos->y = plan->start_pos.y + (plan->end_pos.y - plan->start_pos.y) * progress;
    pos->z = plan->start_pos.z + (plan->end_pos.z - plan->start_pos.z) * progress;

    /* Constant velocity */
    if (plan->duration > 0) {
        pos->vx = (plan->end_pos.x - plan->start_pos.x) / plan->duration;
        pos->vy = (plan->end_pos.y - plan->start_pos.y) / plan->duration;
        pos->vz = (plan->end_pos.z - plan->start_pos.z) / plan->duration;
    } else {
        pos->vx = pos->vy = pos->vz = 0.0f;
    }
}

/* Child process: send position updates */
int child_send_positions(Flight *flight, int write_fd, int time_step) {
    int elapsed = 0;
    int step = 0;
    time_t base_time = flight->plan.departure_time;

    printf("[%s] Starting flight\n", flight->flight_id);

    while (elapsed <= flight->plan.duration) {
        calc_position(&flight->plan, elapsed, &flight->current_position);

        FlightMovement mov;
        memset(&mov, 0, sizeof(FlightMovement));
        mov.timestamp = base_time + elapsed;
        strncpy(mov.flight_id, flight->flight_id, MAX_FLIGHT_ID - 1);
        mov.position = flight->current_position;
        mov.status = 0;

        ssize_t n = write(write_fd, &mov, sizeof(FlightMovement));
        if (n != sizeof(FlightMovement)) {
            perror("write pipe");
            return -1;
        }

        printf("[%s] Step %d: pos(%.1f,%.1f,%.1f) vel(%.2f,%.2f,%.2f)\n",
               flight->flight_id, step,
               flight->current_position.x,
               flight->current_position.y,
               flight->current_position.z,
               flight->current_position.vx,
               flight->current_position.vy,
               flight->current_position.vz);

        sleep(time_step);
        elapsed += time_step;
        step++;
    }

    printf("[%s] Completed\n", flight->flight_id);
    return 0;
}

/* Parent process: read position updates */
int parent_read_positions(SimulationConfig *config, int pipes[][2]) {
    int active = 0;

    for (int i = 0; i < config->num_flights; i++) {
        FlightMovement mov;
        ssize_t n = read(pipes[i][0], &mov, sizeof(FlightMovement));

        if (n == sizeof(FlightMovement)) {
            /* Valid update */
            config->flights[i].current_position = mov.position;
            active++;
        } else if (n == 0) {
            /* EOF: flight completed */
            config->flights[i].status = 1;
            printf("[SIMULATOR] Flight %s completed\n",
                   config->flights[i].flight_id);
        }
    }

    return active;
}

int us101_capture_movements(SimulationConfig *config, int pipes[][2]) {
    int active = parent_read_positions(config, pipes);

    /* Update history */
    for (int i = 0; i < config->num_flights; i++) {
        if (config->flights[i].status == 0) {
            config->flights[i].previous_position = config->flights[i].current_position;
        }
    }

    return active;
}