/*
 * us100_init.c - US100: Simulate flights in a given area
 * SCOMP Sprint 2
 */

#define _POSIX_C_SOURCE 200809L

#include "common.h"
#include "physics.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/* Declared in us101_flight.c */
void run_flight(FlightPlan *plan, int idx, int rfd, int gfd, int timestep);

/*
 * init_simulation
 *   Creates pipes and forks one child per flight plan.
 *   All pipes must be created BEFORE any fork() so each child
 *   inherits the full set of file descriptors.
 *   Returns number of children forked, -1 on error.
 */
int init_simulation(FlightPlan *plans, int n,
                    FlightState *flights,
                    int report_pipe[][2],
                    int go_pipe[][2],
                    int timestep)
{
    int i, j;
    pid_t pid;

    /* Pipe pre-allocation, before any fork() */
    for (i = 0; i < n; i++) {
        if (pipe(report_pipe[i]) == -1) { perror("pipe report"); return -1; }
        if (pipe(go_pipe[i])     == -1) { perror("pipe go");     return -1; }
    }

    for (i = 0; i < n; i++) {

        memset(&flights[i], 0, sizeof(FlightState));
        strncpy(flights[i].id, plans[i].id, ID_LEN - 1);
        flights[i].active     = 1;
        flights[i].hist_count = 0;

        flights[i].in_area = in_area(plans[i].seg[0].start);
        if (!flights[i].in_area)
            printf("[CTRL]   %s starts OUTSIDE the area\n", plans[i].id);

        fflush(stdout);

        pid = fork();

        /* Error Handling */
        if (pid == -1) { perror("fork"); return -1; }

        if (pid == 0) {

            /* Child: close all pipe ends except its own to prevent descriptor leaks */
            for (j = 0; j < n; j++) {
                if (j == i) {

                    /* Close the ends this child will not use */
                    close(report_pipe[i][0]);
                    close(go_pipe[i][1]);
                } else {

                    /* Close all file descriptors belonging to other flights */
                    close(report_pipe[j][0]); close(report_pipe[j][1]);
                    close(go_pipe[j][0]);     close(go_pipe[j][1]);
                }
            }

            run_flight(&plans[i], i, report_pipe[i][1], go_pipe[i][0], timestep);
            exit(1); /* unreachable */
        }

        /* Parent: record PID, close child-side ends */
        flights[i].pid = pid;
        flights[i].rfd = report_pipe[i][0];
        flights[i].wfd = go_pipe[i][1];
        close(report_pipe[i][1]);
        close(go_pipe[i][0]);

        printf("[CTRL]   Forked %-8s  PID=%d  in_area=%d\n",
               plans[i].id, pid, flights[i].in_area);
        fflush(stdout);
    }

    return n;
}
