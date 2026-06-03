#include "us109_report.h"
#include <stdio.h>
#include <time.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>

char g_report_output_path[512] = "";

void set_report_output_path(const char *path) {
    if (path) {
        snprintf(g_report_output_path, sizeof(g_report_output_path), "%s", path);
    } else {
        g_report_output_path[0] = '\0';
    }
}

void write_report(SharedData *shm, int total_steps)
{
    char path[512];
    time_t now = time(NULL);
    if (g_report_output_path[0]) {
        snprintf(path, sizeof(path), "%s", g_report_output_path);
    } else {
        struct tm *tm_ptr = localtime(&now);
        strftime(path, sizeof(path), "report_%Y%m%d_%H%M%S.txt", tm_ptr);
    }

    FILE *f = fopen(path, "w");
    if (!f) { perror("fopen report"); return; }

    fprintf(f, "============================================\n");
    fprintf(f, "  AISafe Simulation Report\n");
    fprintf(f, "  Generated: %s", ctime(&now));
    fprintf(f, "  Total steps: %d  (%d seconds simulated)\n",
            total_steps, total_steps * shm->timestep);
    fprintf(f, "  Flights: %d\n", shm->n_flights);
    fprintf(f, "  Total violations detected: %d\n", shm->n_violations);
    fprintf(f, "============================================\n\n");

    fprintf(f, "FLIGHT SUMMARY:\n");
    for (int i = 0; i < shm->n_flights; i++) {
        FlightData *fd = &shm->flights[i];
        fprintf(f, "  %s: n_viol=%d  ever_in_area=%s  completed=%s\n",
                fd->id, fd->n_viol,
                fd->ever_in_area ? "yes" : "no",
                fd->completed ? "yes" : "no");
    }
    fprintf(f, "\n");

    if (shm->n_violations > 0) {
        fprintf(f, "VIOLATION LOG:\n");
        for (int i = 0; i < shm->n_violations; i++) {
            Violation *v = &shm->violations[i];
            fprintf(f, "  #%d step=%d  %s <-> %s  "
                       "h_dist=%.0fm  v_dist=%.0fm  "
                       "pos_a=(%.4f,%.4f,%.0f)  "
                       "pos_b=(%.4f,%.4f,%.0f)\n",
                    i + 1, v->step,
                    shm->flights[v->fa].id,
                    shm->flights[v->fb].id,
                    v->h_m, v->v_m,
                    v->pa.lat, v->pa.lon, v->pa.alt,
                    v->pb.lat, v->pb.lon, v->pb.alt);
        }
        fprintf(f, "\n");
    }

    int result = (shm->n_violations == 0) ? 1 : 0;
    fprintf(f, "============================================\n");
    fprintf(f, "  RESULT: %s\n", result ? "PASS" : "FAIL");
    fprintf(f, "============================================\n");

    fclose(f);

    char buf[1024];
    snprintf(buf, sizeof(buf), "\n  Report written to %s  RESULT: %s\n",
             path, result ? "PASS" : "FAIL");
    write(STDERR_FILENO, buf, strlen(buf));
}
