/*
 * US100_flight_simulation.c
 * Team: 2dc1 - SCOMP Sprint 2
 * Simulate flights in a given area
 * Create N processes, one per flight. Establish pipes for communication.
 */

#define _POSIX_C_SOURCE 200809L

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <fcntl.h>

#include "shared.h"

static volatile sig_atomic_t violation_signal = 0;
static volatile sig_atomic_t term_signal = 0;

void handle_violation(int sig) {
    violation_signal = 1;
}

void handle_termination(int sig) {
    term_signal = 1;
}

int setup_signals(void) {
    struct sigaction act;

    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = handle_violation;
    sigemptyset(&act.sa_mask);
    act.sa_flags = SA_RESTART;
    if (sigaction(SIGUSR1, &act, NULL) == -1) {
        perror("sigaction SIGUSR1");
        return -1;
    }

    memset(&act, 0, sizeof(struct sigaction));
    act.sa_handler = handle_termination;
    sigemptyset(&act.sa_mask);
    act.sa_flags = SA_RESTART;
    if (sigaction(SIGINT, &act, NULL) == -1) {
        perror("sigaction SIGINT");
        return -1;
    }

    return 0;
}

int create_flights(SimulationConfig *config, int pipes[][2]) {
    for (int i = 0; i < config->num_flights; i++) {
        if (pipe(pipes[i]) == -1) {
            perror("pipe");
            return -1;
        }

        pid_t pid = fork();
        if (pid == -1) {
            perror("fork");
            return -1;
        }

        if (pid == 0) {
            /* Child process */
            close(pipes[i][0]);

            if (setup_signals() != 0) {
                exit(1);
            }

            printf("[FLIGHT %s] Started\n", config->flights[i].flight_id);
            return 0;  /* Signal to caller: this is child */
        } else {
            /* Parent process */
            close(pipes[i][1]);
            config->flights[i].pid = pid;
            printf("[SIMULATOR] Flight %s created (PID %d)\n",
                   config->flights[i].flight_id, pid);
        }
    }

    return 1;  /* Parent continues */
}

int init_config(SimulationConfig *config, int num_flights,
                int total_time, int time_step, int max_violations) {

    if (num_flights <= 0 || num_flights > MAX_FLIGHTS) {
        fprintf(stderr, "Invalid num_flights: %d\n", num_flights);
        return -1;
    }

    if (total_time <= 0 || time_step <= 0) {
        fprintf(stderr, "Invalid time parameters\n");
        return -1;
    }

    config->num_flights = num_flights;
    config->total_time = total_time;
    config->time_step = time_step;
    config->max_violations = max_violations;

    return 0;
}

int print_banner(SimulationConfig *config) {
    printf("\n╔════════════════════════════════════════════╗\n");
    printf("║     SCOMP FLIGHT SIMULATION SYSTEM         ║\n");
    printf("║         Sprint 2 Implementation            ║\n");
    printf("╚════════════════════════════════════════════╝\n\n");

    printf("Configuration:\n");
    printf("  Flights:  %d\n", config->num_flights);
    printf("  Duration: %d seconds\n", config->total_time);
    printf("  Step:     %d second(s)\n", config->time_step);
    printf("  Max Violations: %d\n", config->max_violations);
    printf("\n");

    printf("Flights:\n");
    for (int i = 0; i < config->num_flights; i++) {
        printf("  [%d] %s: %s -> %s\n",
               i + 1, config->flights[i].flight_id,
               config->flights[i].plan.departure,
               config->flights[i].plan.arrival);
    }
    printf("\n");

    return 0;
}

int us100_init_simulation(SimulationConfig *config, int pipes[][2]) {
    printf("[SIMULATOR] Initializing simulation...\n");
    print_banner(config);

    int result = create_flights(config, pipes);
    if (result == -1) {
        return -1;
    }

    if (result == 0) {
        /* Child process - return 0 to signal caller */
        return 0;
    }

    /* Parent process */
    printf("[SIMULATOR] All flights created. Starting simulation.\n\n");
    return 1;
}