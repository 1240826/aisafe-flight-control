/*
 * AISafe Flight Control Simulation
 * us101_flight.c - US101: Capture and process flight movements
 * SCOMP Sprint 2
 */

#define _POSIX_C_SOURCE 200809L

#include "common.h"
#include "physics.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>

static volatile sig_atomic_t got_violation = 0;
static volatile sig_atomic_t do_terminate  = 0;

static void handle_usr1(int sig)
{
    const char msg[] = "[FLIGHT] SIGUSR1 - violation detected\n";
    (void)sig;
    write(STDOUT_FILENO, msg, sizeof(msg) - 1);
    got_violation = 1;
}

static void handle_term(int sig)
{
    const char msg[] = "[FLIGHT] SIGTERM - shutting down\n";
    (void)sig;
    write(STDOUT_FILENO, msg, sizeof(msg) - 1);
    do_terminate = 1;
}

static void setup_signals(void)
{
    struct sigaction act;

    /* ignore SIGPIPE: write() returns EPIPE instead of killing process */
    signal(SIGPIPE, SIG_IGN);

    /* SIGUSR1: block all other signals during handler */
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

void run_flight(FlightPlan *plan, int idx, int rfd, int gfd, int timestep)
{
    Pos3D     pos;
    Vel3D     vel;
    PosUpdate upd;
    GoToken   tok;
    int       cur, step, done;
    ssize_t   n;

    setup_signals();

    if (plan->n_seg == 0) {
        fprintf(stderr, "[FLIGHT %s] no segments\n", plan->id);
        exit(1);
    }

    pos  = plan->seg[0].start;
    memset(&vel, 0, sizeof(Vel3D));
    cur  = 0;
    step = 0;

    printf("[FLIGHT %s] PID=%d  start=(%.4f, %.4f, %.0fm)  phase=%s\n",
           plan->id, getpid(), pos.lat, pos.lon, pos.alt,
           plan->seg[0].phase == CLIMB   ? "CLIMB"   :
           plan->seg[0].phase == CRUISE  ? "CRUISE"  : "DESCEND");
    fflush(stdout);

    while (!do_terminate) {

        /* wait for parent's go token */
        n = read(gfd, &tok, sizeof(GoToken));
        if (n == 0) break;
        if (n  < 0) { perror("[FLIGHT] read gfd"); break; }
        if (do_terminate) break;

        /* tok.safe == 0: hold position this step */
        if (!tok.safe) {
            upd.idx   = idx;  upd.step  = step;
            upd.pos   = pos;  upd.vel   = vel;
            upd.phase = (cur < plan->n_seg) ? plan->seg[cur].phase : CRUISE;
            upd.done  = 0;
            write(rfd, &upd, sizeof(PosUpdate));
            step++;
            continue;
        }

        /* advance position by one timestep */
        done = advance(plan, &plan->seg[cur], &pos, &vel, (double)timestep);

        if (done) {
            cur++;
            if (cur >= plan->n_seg) {
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

        /* send position update to parent */
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
    printf("[FLIGHT %s] PID=%d  exit after %d steps\n",
           plan->id, getpid(), step);
    fflush(stdout);
    exit(0);
}