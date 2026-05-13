/*
 * us103_sync.c - US103: Time-step barrier synchronisation
 * SCOMP Sprint 2 - 2025/2026
 */

#include "common.h"
#include "physics.h"
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>

void store_history(FlightState *f, int step);

/* Phase 1: write GoToken to each active child — unblocks their read() */
void send_go(int step, FlightState *flights, int n_flights,
             int go_pipe[][2], int *safe, double *alt_adj) {
    GoToken tok;
    int i;

    for (i = 0; i < n_flights; i++) {
        if (!flights[i].active) { continue; }
        tok.step       = step;
        tok.safe       = safe[i];    /* 0 = hold position */
        tok.alt_adjust = alt_adj[i]; /* controller altitude order */
        if (write(go_pipe[i][1], &tok, sizeof(GoToken)) == -1) {
            perror("write GoToken");
        }
    }
}

/* Phase 2: read() blocks until child i sends — loop ends only when
 * ALL children have reported. This is the barrier guarantee. */
void collect(int step, int print_every,
             FlightState *flights, int n_flights,
             int report_pipe[][2]) {
    PosUpdate upd;
    int i, was_in, now_in;
    ssize_t n;

    for (i = 0; i < n_flights; i++) {
        if (!flights[i].active) { continue; }

        n = read(report_pipe[i][0], &upd, sizeof(PosUpdate)); /* blocks */

        if (n == (ssize_t)sizeof(PosUpdate)) {

            was_in = flights[i].in_area;
            now_in = in_area_full(upd.pos);

            if (!was_in && now_in) {
                printf("[AREA]   %s ENTERING area at step %d\n",
                       flights[i].id, step);
            } else if (was_in && !now_in) {
                printf("[AREA]   %s LEAVING area at step %d\n",
                       flights[i].id, step);
            }

            flights[i].last_pos   = upd.pos;
            flights[i].last_vel   = upd.vel;
            flights[i].last_phase = upd.phase;
            flights[i].in_area    = now_in;
            store_history(&flights[i], step); /* US101: position history */

            if (step % print_every == 0) {
                const char *phase_str, *area_str;

                if (upd.phase == CLIMB)       { phase_str = "CLIMB";   }
                else if (upd.phase == CRUISE) { phase_str = "CRUISE";  }
                else                          { phase_str = "DESCEND"; }

                if (now_in) { area_str = "IN";  }
                else        { area_str = "OUT"; }

                printf("[CTRL]   step=%5d  %-8s  (%.4f, %.4f, %.0fm)  %s  %s\n",
                       step, flights[i].id,
                       upd.pos.lat, upd.pos.lon, upd.pos.alt,
                       phase_str, area_str);
            }

            if (upd.done) {
                printf("[CTRL]   %s completed at step %d\n",
                       flights[i].id, step);
                flights[i].active = 0;
            }

        } else if (n == 0) {
            printf("[CTRL]   %s: pipe closed\n", flights[i].id); /* EOF */
            flights[i].active = 0;
        } else {
            fprintf(stderr, "[CTRL]   read error %s: %s\n",
                    flights[i].id, strerror(errno));
            flights[i].active = 0;
        }
    }
}
