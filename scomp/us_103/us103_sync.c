/*
 * us103_sync.c - US103: Synchronize flight execution with a time step
*/

#include "common.h"
#include "physics.h"

#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <signal.h>

void store_history(FlightState *f, int step);

/*
 * send_go — Phase 1 of the barrier.
 * Writes GoToken to each active child
 *   safe = 0  hold position (violation nearby)
 *   safe = 1  advance normally
 *   alt_adjust > 0 climb order from US102
 */
void send_go(int step, FlightState *flights, int n_flights,int go_pipe[][2], int *safe, double *alt_adj)
{
	GoToken tok;
    	int i;

    	for (i = 0; i < n_flights; i++) {
        	if (!flights[i].active) { 
        		continue; 
        	}

        	tok.step = step;
        	tok.safe = safe[i];
        	tok.alt_adjust = alt_adj[i];

        	if (write(go_pipe[i][1], &tok, sizeof(GoToken)) == -1) {
            		perror("write GoToken");
        	}
    	}
}

/*
 * collect — Phase 2 of the barrier.
 * Reads PosUpdate from each active child.
 * Each read() blocks until that child writes.
 * Loop ends only when all children have reported 
 */
void collect(int step, int print_every,FlightState *flights, int n_flights, int report_pipe[][2])
{
	PosUpdate upd;
    	int i, was_in, now_in;
    	ssize_t n;

    	for (i = 0; i < n_flights; i++) {
        	if (!flights[i].active) { 
        		continue; 
        	}

        	n = read(report_pipe[i][0], &upd, sizeof(PosUpdate)); /* blocks */

        	if (n == (ssize_t)sizeof(PosUpdate)) {

            		was_in = flights[i].in_area;
            		now_in = in_area_full(upd.pos);

            		if (!was_in && now_in) {
                		printf("[AREA] %s ENTERING area at step %d\n",flights[i].id, step);
                		flights[i].ever_in_area = 1;

            		} else if (was_in && !now_in) {
                		printf("[AREA] %s LEAVING area at step %d\n",flights[i].id, step);
                		kill(flights[i].pid, SIGTERM);
                		flights[i].active = 0;
            		}

            		flights[i].last_pos = upd.pos;
            		flights[i].last_vel = upd.vel;
            		flights[i].last_phase = upd.phase;
            		flights[i].in_area = now_in;
            		store_history(&flights[i], step);

            		if (step % print_every == 0) {
                		const char *phase_str, *area_str;

                		if (upd.phase == CLIMB) {
                    			phase_str = "CLIMB";
                		} else if (upd.phase == CRUISE) {
                    			phase_str = "CRUISE";
                		} else {
                    			phase_str = "DESCEND";
                		}

                		if (now_in) {
                    			area_str = "IN";
                		} else {
                    			area_str = "OUT";
                		}

                		printf("[CTRL] step=%5d  %-8s  (%.4f, %.4f, %.0fm)  %s  %s\n", step, flights[i].id, upd.pos.lat, upd.pos.lon, upd.pos.alt, phase_str, area_str);
            		}

            		if (upd.done) {
                		printf("[CTRL] %s completed at step %d\n", flights[i].id, step);
                		flights[i].active = 0;
            		}

        	} else if (n == 0) {
            		printf("[CTRL] %s: pipe closed\n", flights[i].id);
            		flights[i].active = 0;

        	} else {
            		fprintf(stderr, "[CTRL]   read error %s: %s\n",flights[i].id, strerror(errno));
            		flights[i].active = 0;
        	}
    	}
}
