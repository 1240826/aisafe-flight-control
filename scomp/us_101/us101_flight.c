/*
 * AISafe Flight Control Simulation
 * us101_flight.c - US101: Capture and process flight movements
 * SCOMP Sprint 2 
 */

#include "common.h"
#include "physics.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>

static volatile sig_atomic_t got_violation = 0;
static volatile sig_atomic_t do_terminate  = 0;

/* called when parent detects a safety violation near this flight */
static void handle_usr1(int sig){
	const char msg[] = "[FLIGHT] SIGUSR1 - violation detected\n";
	(void)sig;
	write(STDOUT_FILENO, msg, sizeof(msg) - 1);
	got_violation = 1;
}

/* called when parent orders this flight to shut down */
static void handle_term(int sig){
	const char msg[] = "[FLIGHT] SIGTERM - shutting down\n";
	(void)sig;
	write(STDOUT_FILENO, msg, sizeof(msg) - 1);
	do_terminate = 1;
}

static void setup_signals(void){
	struct sigaction act;

    /* block all signals while handling SIGUSR1 (US102 requirement) */
	memset(&act, 0, sizeof(struct sigaction));
	sigfillset(&act.sa_mask);
	act.sa_handler = handle_usr1;
	act.sa_flags   = SA_RESTART;
	sigaction(SIGUSR1, &act, NULL);

	memset(&act, 0, sizeof(struct sigaction));
	sigemptyset(&act.sa_mask);
	act.sa_handler = handle_term;
	act.sa_flags   = SA_RESTART;
	sigaction(SIGTERM, &act, NULL);
	sigaction(SIGINT,  &act, NULL);
}

/* child process - runs one flight, never returns */
void run_flight(FlightPlan *plan, int idx, int rfd, int gfd, int timestep){
	Pos3D pos;
	Vel3D vel;
	PosUpdate upd;
	GoToken tok;
	int cur, step, done;
	ssize_t n;

	setup_signals();

	if (plan->n_seg == 0) {
        	fprintf(stderr, "[FLIGHT %s] no segments\n", plan->id);
        	exit(1);
    	}

	pos  = plan->seg[0].start;
	memset(&vel, 0, sizeof(Vel3D));
	cur  = 0;
	step = 0;

	printf("[FLIGHT %s] PID=%d  start=(%.4f, %.4f, %.0fm)  %s\n", plan->id, getpid(), pos.lat, pos.lon, pos.alt,plan->seg[0].phase == CLIMB   ? "CLIMB"   : plan->seg[0].phase == CRUISE  ? "CRUISE"  : "DESCEND");
	fflush(stdout);

	while (!do_terminate) {

            /* wait for parent to release this step (US103) */
        	n = read(gfd, &tok, sizeof(GoToken));
        	if (n <= 0) break;
        	if (do_terminate) break;

            /* parent ordered an altitude change to avoid collision (US102) */
        	if (tok.alt_adjust != 0.0) {
            		pos.alt += tok.alt_adjust;
            		printf("[FLIGHT %s] step=%d  CTRL ORDER: altitude adjusted" " %+.0fm -> now %.0fm\n", plan->id, step, tok.alt_adjust, pos.alt);
            		fflush(stdout);
        	}

            /* safe=0 means hold position this step */
        	if (!tok.safe) {
            		upd.idx   = idx;  upd.step  = step;
            		upd.pos   = pos;  upd.vel   = vel;
            		upd.phase = (cur < plan->n_seg) ? plan->seg[cur].phase : CRUISE;
            		upd.done  = 0;
            		write(rfd, &upd, sizeof(PosUpdate));
            		step++;
            		continue;
        	}

        	/* Advance position */
        	done = advance(plan, &plan->seg[cur], &pos, &vel, (double)timestep);

        	if (done) {
            		cur++;
            		if (cur >= plan->n_seg) {
            		    /* all segments finished, notify parent and exit */
                		upd.idx   = idx;  upd.step  = step;
                		upd.pos   = pos;  upd.vel   = vel;
                		upd.phase = plan->seg[plan->n_seg - 1].phase;
                		upd.done  = 1;
                		write(rfd, &upd, sizeof(PosUpdate));
                		printf("[FLIGHT %s] PID=%d  all segments done at step %d\n",
                       		plan->id, getpid(), step);
                		fflush(stdout);
                		break;
            		}
            		pos = plan->seg[cur].start;
        	}

        	/* US101: send current position to parent via pipe */
        	upd.idx   = idx;  upd.step  = step;
        	upd.pos   = pos;  upd.vel   = vel;
        	upd.phase = plan->seg[cur].phase;
        	upd.done  = 0;
        	write(rfd, &upd, sizeof(PosUpdate));

        	if (got_violation) {
            		printf("[FLIGHT %s] step=%d  VIOLATION at (%.4f, %.4f, %.0fm)\n",
                   	plan->id, step, pos.lat, pos.lon, pos.alt);
            		fflush(stdout);
            		got_violation = 0;
        	}

        	step++;
    	}

    	close(rfd);
    	close(gfd);
    	printf("[FLIGHT %s] PID=%d  exit after %d steps\n", plan->id, getpid(), step);
    	fflush(stdout);
    	exit(0);
}
