/*
 * AISafe Flight Control Simulation
 * us102_detector.c - US102: Detect aircraft safety violations in real time
 * SCOMP Sprint 2 - 2025/2026
 *
 * Safety cylinder (ICAO Doc 4444, Chapter 8 — Separation Minima):
 *   Horizontal: 5 NM = 9260 m  (ICAO radar en-route standard)
 *   Vertical:   305 m           (1000 ft, RVSM airspace)
 *
 * On violation:
 *   1. Log the event with positions and velocity vectors
 *   2. Send SIGUSR1 to both involved children (notification)
 *   3. Apply altitude adjustment to the lower aircraft (route change)
 *      to restore vertical separation > SAFETY_V_M
 *   4. Set safe[i]=0 for involved aircraft during adjustment period
 *
 * When total violations reach VIOL_THRESHOLD: send SIGTERM to all.
 */

#define _POSIX_C_SOURCE 200809L

#include "common.h"
#include "physics.h"

#include <stdio.h>
#include <time.h>
#include <signal.h>
#include <math.h>

/*
 * store_history
 *   US101 AC: "store past positions to anticipate and detect violations."
 *   Circular buffer — oldest entry is overwritten when full.
 */
void store_history(FlightState *f, int step)
{
    int idx;
    idx = f->hist_count % MAX_HISTORY;
    f->history[idx].step  = step;
    f->history[idx].pos   = f->last_pos;
    f->history[idx].vel   = f->last_vel;
    f->history[idx].phase = f->last_phase;
    f->hist_count++;
}

/*
 * predict_collision
 *   US102 AC: "detect when two or more aircraft MAY EVENTUALLY violate".
 *   Extrapolates positions LOOKAHEAD_S seconds ahead using current velocity.
 *   Returns 1 if the predicted positions breach the safety cylinder.
 *
 *   This gives the controller time to react before actual breach occurs.
 */
#define LOOKAHEAD_S   30   /* seconds to look ahead */

static int predict_collision(FlightState *a, FlightState *b,
                              double *h_pred, double *v_pred)
{
    Pos3D pa_future, pb_future;
    double dt = (double)LOOKAHEAD_S;

    /* Simple linear extrapolation using current velocity */
    pa_future.lat = a->last_pos.lat + R2D(a->last_vel.vy * dt / EARTH_R);
    pa_future.lon = a->last_pos.lon + R2D(a->last_vel.vx * dt
                    / (EARTH_R * cos(D2R(a->last_pos.lat))));
    pa_future.alt = a->last_pos.alt + a->last_vel.vz * dt;

    pb_future.lat = b->last_pos.lat + R2D(b->last_vel.vy * dt / EARTH_R);
    pb_future.lon = b->last_pos.lon + R2D(b->last_vel.vx * dt
                    / (EARTH_R * cos(D2R(b->last_pos.lat))));
    pb_future.alt = b->last_pos.alt + b->last_vel.vz * dt;

    return safety_breach(pa_future, pb_future, h_pred, v_pred);
}

/*
 * detect_violations
 *   1. Store current positions in history (US101).
 *   2. Check all active in-area pairs for current breaches.
 *   3. Check all pairs for predicted breaches (LOOKAHEAD_S ahead).
 *   4. On breach or predicted breach:
 *        - Log Violation
 *        - SIGUSR1 to both children
 *        - Apply altitude adjustment to the lower aircraft
 *        - Set safe[]=0 for involved aircraft
 *   Returns number of new violations (current or predicted).
 */
int detect_violations(int step,
                      FlightState *flights, int n_flights,
                      Violation *violations, int *n_viol,
                      int *safe, double *alt_adj)
{
    int i, j, found;
    double h_m, v_m, h_pred, v_pred;
    int current_breach, predicted_breach;

    found = 0;

    /* Default: everything safe, no adjustment */
    for (i = 0; i < n_flights; i++) {
        safe[i]    = 1;
        alt_adj[i] = 0.0;
        /* Count down existing adjustment periods */
        if (flights[i].adj_steps_left > 0)
            flights[i].adj_steps_left--;
    }

    for (i = 0; i < n_flights; i++) {
        if (!flights[i].active || !flights[i].in_area) continue;

        for (j = i + 1; j < n_flights; j++) {
            if (!flights[j].active || !flights[j].in_area) continue;

            current_breach   = safety_breach(flights[i].last_pos,
                                             flights[j].last_pos,
                                             &h_m, &v_m);
            predicted_breach = predict_collision(&flights[i], &flights[j],
                                                 &h_pred, &v_pred);

            if (!current_breach && !predicted_breach) continue;

            if (current_breach)
                printf("[VIOL]   step=%d  BREACH  %s <-> %s"
                       "  H=%.0fm(lim %.0fm)  V=%.0fm(lim %.0fm)\n",
                       step, flights[i].id, flights[j].id,
                       h_m, SAFETY_H_M, v_m, SAFETY_V_M);
            else
                printf("[PRED]   step=%d  PREDICTED  %s <-> %s"
                       "  H_pred=%.0fm  V_pred=%.0fm  (in %ds)\n",
                       step, flights[i].id, flights[j].id,
                       h_pred, v_pred, LOOKAHEAD_S);

            /* Log violation event */
            if (*n_viol < MAX_VIOLATIONS) {
                Violation *ev = &violations[*n_viol];
                ev->step     = step;
                ev->ts       = time(NULL);
                ev->fa       = i;   ev->fb     = j;
                ev->pa       = flights[i].last_pos;
                ev->pb       = flights[j].last_pos;
                ev->va       = flights[i].last_vel;
                ev->vb       = flights[j].last_vel;
                ev->h_m      = current_breach ? h_m    : h_pred;
                ev->v_m      = current_breach ? v_m    : v_pred;
                ev->resolved = 0;
                ev->adj_m    = 0.0;

                /*
                 * Route adjustment: order aircraft that is lower to climb
                 * ALT_ADJUST_M metres so vertical separation is restored.
                 * The adjustment is sent via GoToken.alt_adjust next step.
                 */
                if (flights[i].adj_steps_left == 0 &&
                    flights[j].adj_steps_left == 0) {

                    int lower = (flights[i].last_pos.alt <=
                                 flights[j].last_pos.alt) ? i : j;
                    alt_adj[lower]               = ALT_ADJUST_M;
                    flights[lower].adj_steps_left = ALT_ADJUST_STEPS;
                    safe[i] = 0;
                    safe[j] = 0;

                    ev->resolved      = 1;
                    ev->adj_m         = ALT_ADJUST_M;
                    ev->adjusted_flt  = lower;

                    printf("[CTRL]   Ordering %s to climb %.0fm"
                           " to restore separation\n",
                           flights[lower].id, ALT_ADJUST_M);
                }

                (*n_viol)++;
            }

            kill(flights[i].pid, SIGUSR1);
            kill(flights[j].pid, SIGUSR1);
            flights[i].n_viol++;
            flights[j].n_viol++;
            found++;
        }
    }

    return found;
}

/*
 * terminate_all
 *   Send SIGTERM to every active child.
 */
void terminate_all(FlightState *flights, int n_flights)
{
    int i;
    printf("[CTRL]   Sending SIGTERM to all active flights\n");
    for (i = 0; i < n_flights; i++) {
        if (flights[i].active) {
            kill(flights[i].pid, SIGTERM);
            flights[i].killed = 1;
            flights[i].active = 0;
        }
    }
}
