#include "us107_report_notify.h"
#include "us109_report.h"
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

void *report_generator_thread(void *arg)
{
    SharedData *shm = (SharedData *)arg;
    char buf[1024];
    int last_printed = -1;

    while (shm->running) {
        struct timespec ts;
        clock_gettime(CLOCK_REALTIME, &ts);
        ts.tv_sec += 1;

        pthread_mutex_lock(&shm->viol_mutex);
        int rc = 0;
        while (shm->n_violations <= last_printed + 1 && shm->running && rc == 0)
            rc = pthread_cond_timedwait(&shm->viol_cond, &shm->viol_mutex, &ts);

        if (!shm->running) {
            pthread_mutex_unlock(&shm->viol_mutex);
            break;
        }

        while (last_printed + 1 < shm->n_violations) {
            last_printed++;
            Violation *v = &shm->violations[last_printed];
            snprintf(buf, sizeof(buf),
                "[REPORT] Step %d: %s <-> %s  h_dist=%.0fm  v_dist=%.0fm\n",
                v->step,
                shm->flights[v->fa].id, shm->flights[v->fb].id,
                v->h_m, v->v_m);
            write(STDERR_FILENO, buf, strlen(buf));
        }
        pthread_mutex_unlock(&shm->viol_mutex);
    }

    if (shm->generate_report) {
        write_report(shm, shm->report_total_steps);
    }

    pthread_exit(NULL);
}
