#include "us106_threads.h"
#include "physics.h"
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

void *violation_detector_thread(void *arg)
{
    SharedData *shm = (SharedData *)arg;
    char buf[512];

    while (shm->running) {
        pthread_mutex_lock(&shm->detect_mutex);
        while (!shm->step_ready && shm->running)
            pthread_cond_wait(&shm->detect_cond, &shm->detect_mutex);
        if (!shm->running) {
            pthread_mutex_unlock(&shm->detect_mutex);
            break;
        }
        shm->step_ready = 0;
        pthread_mutex_unlock(&shm->detect_mutex);

        int step = shm->current_step;

        pthread_mutex_lock(&shm->pos_mutex);
        for (int i = 0; i < shm->n_flights; i++) {
            FlightData *fa = &shm->flights[i];
            if (fa->completed || (!fa->active && !fa->in_area)) continue;
            for (int j = i + 1; j < shm->n_flights; j++) {
                FlightData *fb = &shm->flights[j];
                if (fb->completed || (!fb->active && !fb->in_area)) continue;

                double h_m, v_m;
                if (safety_breach(fa->pos, fb->pos, &h_m, &v_m)) {
                    pthread_mutex_lock(&shm->viol_mutex);
                    if (shm->n_violations < MAX_VIOLATIONS) {
                        Violation *v = &shm->violations[shm->n_violations++];
                        v->step = step;
                        v->ts = time(NULL);
                        v->fa = i;
                        v->fb = j;
                        v->pa = fa->pos;
                        v->pb = fb->pos;
                        v->va = fa->vel;
                        v->vb = fb->vel;
                        v->h_m = h_m;
                        v->v_m = v_m;
                        shm->flights[i].n_viol++;
                        shm->flights[j].n_viol++;
                        if (step < MAX_HISTORY) {
                            shm->last_viol_step[i][j] = step;
                            shm->last_viol_step[j][i] = step;
                        }

                        snprintf(buf, sizeof(buf),
                            "[DETECTOR] Step %d: %s <-> %s  h=%.0fm v=%.0fm\n",
                            step, fa->id, fb->id, h_m, v_m);
                        write(STDERR_FILENO, buf, strlen(buf));

                        pthread_cond_signal(&shm->viol_cond);
                    }
                    pthread_mutex_unlock(&shm->viol_mutex);
                }
            }
        }
        pthread_mutex_unlock(&shm->pos_mutex);
    }

    pthread_exit(NULL);
}

void detect_initial_violations(SharedData *shm)
{
    char buf[512];

    for (int i = 0; i < shm->n_flights; i++) {
        FlightData *fa = &shm->flights[i];
        if (!fa->active) continue;
        for (int j = i + 1; j < shm->n_flights; j++) {
            FlightData *fb = &shm->flights[j];
            if (!fb->active) continue;

            double h_m, v_m;
            if (safety_breach(fa->pos, fb->pos, &h_m, &v_m)) {
                pthread_mutex_lock(&shm->viol_mutex);
                if (shm->n_violations < MAX_VIOLATIONS) {
                    Violation *v = &shm->violations[shm->n_violations++];
                    v->step = 0;
                    v->ts = time(NULL);
                    v->fa = i;
                    v->fb = j;
                    v->pa = fa->pos;
                    v->pb = fb->pos;
                    v->va = fa->vel;
                    v->vb = fb->vel;
                    v->h_m = h_m;
                    v->v_m = v_m;
                    shm->flights[i].n_viol++;
                    shm->flights[j].n_viol++;
                    shm->last_viol_step[i][j] = 0;
                    shm->last_viol_step[j][i] = 0;
                    pthread_cond_signal(&shm->viol_cond);
                }
                pthread_mutex_unlock(&shm->viol_mutex);

                snprintf(buf, sizeof(buf),
                    "[DETECTOR] INITIAL VIOLATION at step 0: %s <-> %s  "
                    "h=%.0fm v=%.0fm\n",
                    fa->id, fb->id, h_m, v_m);
                write(STDERR_FILENO, buf, strlen(buf));
            }
        }
    }
}
