/*
 * AISafe Flight Control Simulation - main.c
 * SCOMP Sprint 2 - 2025/2026
 *
 * Compile:
 *   gcc -Wall -Wextra main.c us100_init.c us101_flight.c \
 *       us102_detector.c us103_sync.c us109_report.c -o simulation -lm
 *
 * Usage:
 *   ./simulation <plans.json> [timestep_s] [print_every]
 *
 *   timestep_s   - simulation step in seconds (default: 1)
 *   print_every  - print position every N steps (default: 60)
 *
 * Examples:
 *   ./simulation plans.json          # 1s step, print every 60s
 *   ./simulation plans.json 1 1      # 1s step, print every step
 *   ./simulation plans.json 5 12     # 5s step, print every 60s simulated
 *   ./simulation plans.json 30 2     # 30s step, print every 60s simulated
 *
 * The simulation runs until all flights complete or leave the area.
 * A safety cap of SAFETY_CAP_S prevents infinite loops on bad input.
 */

#include "common.h"
#include "json_parser.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>

#define SAFETY_CAP_S   10800   /* max simulated seconds (3 hours) */

/* Function declarations */
int  init_simulation(FlightPlan *plans, int n,
                     FlightState *flights,
                     int report_pipe[][2],
                     int go_pipe[][2],
                     int timestep);

void send_go(int step, FlightState *flights, int n_flights,
             int go_pipe[][2], int *safe, double *alt_adj);

void collect(int step, int print_every,
             FlightState *flights, int n_flights,
             int report_pipe[][2]);

int  detect_violations(int step,
                       FlightState *flights, int n_flights,
                       Violation *violations, int *n_viol,
                       int *safe, double *alt_adj);

void terminate_all(FlightState *flights, int n_flights);

void write_report(int steps, int timestep,
                  FlightPlan *plans, int n_flights,
                  FlightState *flights,
                  Violation *violations, int n_viol);

static volatile sig_atomic_t stop = 0;
static void handle_sigint(int sig) { (void)sig; stop = 1; }

int main(int argc, char *argv[]) {
    FlightPlan  plans[MAX_FLIGHTS];
    FlightState flights[MAX_FLIGHTS];
    Violation   violations[MAX_VIOLATIONS];
    int         report_pipe[MAX_FLIGHTS][2];
    int         go_pipe[MAX_FLIGHTS][2];
    int         safe[MAX_FLIGHTS];
    double      alt_adj[MAX_FLIGHTS];
    struct sigaction act;
    int n_flights, n_viol, step, timestep, print_every;
    int active, i, new_v, status, safety_cap;
    pid_t p;

    if (argc < 2) {
        fprintf(stderr,
            "Usage: %s <plans.json> [timestep_s] [print_every]\n"
            "  timestep_s  - simulation step in seconds  (default: 1)\n"
            "  print_every - print position every N steps (default: 60)\n",
            argv[0]);
        return 1;
    }

    timestep    = TIMESTEP_S;
    print_every = 60;
    if (argc >= 3) { timestep    = atoi(argv[2]); }
    if (argc >= 4) { print_every = atoi(argv[3]); }
    if (timestep    <= 0) { timestep    = TIMESTEP_S; }
    if (print_every <= 0) { print_every = 60; }

    safety_cap = SAFETY_CAP_S / timestep;

    n_flights = load_plans(argv[1], plans, MAX_FLIGHTS);
    if (n_flights <= 0) {
        fprintf(stderr, "Failed to load plans from %s\n", argv[1]);
        return 1;
    }

    printf("[CTRL]   Loaded %d plan(s) from %s\n", n_flights, argv[1]);
    printf("[CTRL]   Area: Lat[%.0f,%.0f] Lon[%.0f,%.0f] Alt[0,%.0fm]\n",
           AREA_LAT_MIN, AREA_LAT_MAX, AREA_LON_MIN, AREA_LON_MAX, AREA_MAX_ALT_M);
    printf("[CTRL]   Safety: H<%.0fm (%.1fNM)  V<%.0fm\n",
           SAFETY_H_M, SAFETY_H_NM, SAFETY_V_M);
    printf("[CTRL]   Timestep: %ds  |  Print every: %d steps (%ds)  |  Cap: %ds\n\n",
           timestep, print_every, print_every * timestep, SAFETY_CAP_S);

    memset(&act, 0, sizeof(struct sigaction));
    sigemptyset(&act.sa_mask);
    act.sa_handler = handle_sigint;
    act.sa_flags   = SA_RESTART;
    sigaction(SIGINT, &act, NULL);

    if (init_simulation(plans, n_flights,
                        flights, report_pipe, go_pipe, timestep) < 0) {
        return 1;
    }
    printf("\n");

    n_viol = 0;
    for (i = 0; i < n_flights; i++) {
        safe[i]    = 1;
        alt_adj[i] = 0.0;
    }

    for (step = 0; step < safety_cap && !stop; step++) {

        active = 0;
        for (i = 0; i < n_flights; i++) {
            if (flights[i].active) { active++; }
        }

        if (active == 0) {
            printf("\n[CTRL]   All flights done at step %d (%ds simulated)\n",
                   step, step * timestep);
            break;
        }

        if (step % print_every == 0) {
            printf("\n[CTRL] === Step %d  (%ds simulated  %d active) ===\n",
                   step, step * timestep, active);
        }

        send_go(step, flights, n_flights, go_pipe, safe, alt_adj);       /* 1 */
        collect(step, print_every, flights, n_flights, report_pipe);     /* 2 */

        for (i = 0; i < n_flights; i++) { alt_adj[i] = 0.0; }

        new_v = detect_violations(step, flights, n_flights,              /* 3 */
                                  violations, &n_viol, safe, alt_adj);
        if (new_v > 0) {
            printf("[CTRL]   %d new event(s). Total: %d\n", new_v, n_viol);
        }

        if (n_viol >= VIOL_THRESHOLD) {                                  /* 4 */
            printf("\n[CTRL]   Threshold (%d) reached — terminating\n",
                   VIOL_THRESHOLD);
            terminate_all(flights, n_flights);
            step++;
            break;
        }
    }

    if (step >= safety_cap) {
        printf("\n[CTRL]   Safety cap (%ds) reached\n", SAFETY_CAP_S);
    }

    printf("\n[CTRL]   Simulation ended: %d step(s), %ds simulated.\n",
           step, step * timestep);

    for (i = 0; i < n_flights; i++) {
        close(go_pipe[i][1]);
        close(report_pipe[i][0]);
    }

    for (i = 0; i < n_flights; i++) {
        p = waitpid(flights[i].pid, &status, 0);
        if (WIFEXITED(status)) {
            printf("[CTRL]   PID %d (%s) exited %d\n",
                   p, flights[i].id, WEXITSTATUS(status));
        } else if (WIFSIGNALED(status)) {
            printf("[CTRL]   PID %d (%s) killed by signal %d\n",
                   p, flights[i].id, WTERMSIG(status));
        }
    }

    write_report(step, timestep, plans, n_flights, flights, violations, n_viol);
    return 0;
}
