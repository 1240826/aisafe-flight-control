#include "ui.h"
#include "physics.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>
#include <dirent.h>
#include <unistd.h>
#include <time.h>

#define MAP_W 70
#define MAP_H 20
#define MAX_TRAIL 200

static int lat_to_row(double lat)
{
    int row = (int)((AREA_LAT_MAX - lat) / (AREA_LAT_MAX - AREA_LAT_MIN) * (MAP_H - 3));
    if (row < 0) row = 0;
    if (row >= MAP_H - 2) row = MAP_H - 3;
    return row + 1;
}

static int lon_to_col(double lon)
{
    int col = (int)((lon - AREA_LON_MIN) / (AREA_LON_MAX - AREA_LON_MIN) * (MAP_W - 5));
    if (col < 0) col = 0;
    if (col >= MAP_W - 4) col = MAP_W - 5;
    return col + 2;
}

static int flight_in_violation(SharedData *shm, int idx, int step)
{
    for (int k = 0; k < shm->n_flights; k++) {
        if (k == idx) continue;
        if ((shm->last_viol_step[idx][k] != -1 && shm->last_viol_step[idx][k] == step) ||
            (shm->last_viol_step[k][idx] != -1 && shm->last_viol_step[k][idx] == step))
            return 1;
    }
    return 0;
}

void clear_screen(void)
{
    printf("\033[2J\033[H");
    fflush(stdout);
}

void show_banner(void)
{
    printf("\n");
    printf("  +----- AISafe Flight Control Simulation -----+\n");
    printf("  |            SCOMP Sprint 3                  |\n");
    printf("  +--------------------------------------------+\n");
    printf("\n");
}

int show_menu(void)
{
    int choice;
    printf("  +---------------- MAIN MENU ----------------+\n");
    printf("  |  1. Load Scenario                         |\n");
    printf("  |  2. Set Simulation Start Time             |\n");
    printf("  |  3. Set Print Interval                    |\n");
    printf("  |  4. Start Simulation                      |\n");
    printf("  |  5. Display Flight Summary                |\n");
    printf("  |  6. Select Weather Provider               |\n");
    printf("  |  7. Run Demo (select scenario)            |\n");
    printf("  |  8. Exit                                  |\n");
    printf("  +-------------------------------------------+\n");
    printf("\n  Choice: ");
    fflush(stdout);

    char buf[16];
    if (!fgets(buf, sizeof(buf), stdin)) return 0;
    choice = atoi(buf);
    return choice;
}

int browse_scenarios(const char *dir, char *out_path, int max_len)
{
    DIR *d = opendir(dir);
    if (!d) {
        printf("  Scenario directory '%s' not found.\n", dir);
        return -1;
    }

    char files[64][256];
    int n = 0;

    struct dirent *ent;
    while ((ent = readdir(d)) != NULL && n < 64) {
        int len = strlen(ent->d_name);
        if (len > 5 && strcmp(ent->d_name + len - 5, ".json") == 0)
            snprintf(files[n++], sizeof(files[0]), "%s", ent->d_name);
    }
    closedir(d);

    if (n == 0) {
        printf("  No .json scenarios found in '%s'.\n", dir);
        return -1;
    }

    printf("\n  Available scenarios:\n\n");
    for (int i = 0; i < n; i++) {
        char name[64];
        int flen = strlen(files[i]);
        int start = 0;
        for (int k = 0; k < flen; k++) {
            if (files[i][k] == '_' || files[i][k] == '.') {
                start = k;
                break;
            }
        }
        snprintf(name, sizeof(name), "%.*s", start > 0 ? start : flen - 5, files[i]);
        printf("  %2d. %s  (%s)\n", i + 1, name, files[i]);
    }

    printf("\n  Select scenario (1-%d): ", n);
    fflush(stdout);

    char buf[16];
    if (!fgets(buf, sizeof(buf), stdin)) return -1;
    int idx = atoi(buf) - 1;
    if (idx < 0 || idx >= n) {
        printf("  Invalid choice.\n");
        return -1;
    }

    snprintf(out_path, max_len, "%s/%s", dir, files[idx]);
    return 0;
}

static const char *flight_colors[MAX_FLIGHTS] = {
    ANSI_RED, ANSI_GREEN, ANSI_YELLOW, ANSI_BLUE, ANSI_MAGENTA,
    ANSI_CYAN, ANSI_BOLD ANSI_RED, ANSI_BOLD ANSI_GREEN,
    ANSI_BOLD ANSI_YELLOW, ANSI_BOLD ANSI_BLUE
};

static const char *flight_mark(int fi)
{
    return flight_colors[fi % MAX_FLIGHTS];
}

void draw_airspace(SharedData *shm, int step)
{
    char obuf[8192];
    int p = 0;

    p += snprintf(obuf + p, sizeof(obuf) - p, "\033[2J\033[H");

    int sim_time = step * shm->timestep;
    int utc_sec = shm->sim_start_sec + sim_time;
    int local_sec = (utc_sec + 3600 + 86400) % 86400;  // UTC+1 display
    int wall_h = local_sec / 3600;
    int wall_m = (local_sec % 3600) / 60;

    p += snprintf(obuf + p, sizeof(obuf) - p,
        "  %s%sIBERIAN AIRSPACE MONITOR%s  %02d:%02d (UTC+1)  step=%d  "
        "[LAT %d-%dN  LON %d-%dW]\n",
        ANSI_CYAN, ANSI_BOLD, ANSI_RESET, wall_h, wall_m, step,
        (int)AREA_LAT_MIN, (int)AREA_LAT_MAX,
        (int)(-AREA_LON_MAX), (int)(-AREA_LON_MIN));

    if (shm->env.wind_speed_kt > 0.0) {
        p += snprintf(obuf + p, sizeof(obuf) - p,
            "  Wind: %.0fkt from %.0f\xc2\xb0\n",
            shm->env.wind_speed_kt, shm->env.wind_dir_deg);
    }

    p += snprintf(obuf + p, sizeof(obuf) - p, "\n");

    char grid[MAP_H][MAP_W + 1];
    int flight_at[MAP_H][MAP_W];
    int i, j;

    for (i = 0; i < MAP_H; i++) {
        for (j = 0; j < MAP_W; j++) {
            grid[i][j] = ' ';
            flight_at[i][j] = -1;
        }
        grid[i][MAP_W] = '\0';
    }

    for (j = 0; j < MAP_W; j++) {
        grid[0][j] = '=';
        grid[MAP_H-1][j] = '=';
    }
    for (i = 0; i < MAP_H; i++) {
        grid[i][0] = '|';
        grid[i][MAP_W-1] = '|';
    }
    grid[0][0] = '+'; grid[0][MAP_W-1] = '+';
    grid[MAP_H-1][0] = '+'; grid[MAP_H-1][MAP_W-1] = '+';



    for (int fi = 0; fi < shm->n_flights; fi++) {
        FlightData *fd = &shm->flights[fi];
        if (!fd->active || fd->hist_count == 0) continue;
        int step_start = fd->hist_count > MAX_TRAIL ? fd->hist_count - MAX_TRAIL : 0;
        int step_skip = (fd->hist_count - step_start > 100) ? 2 : 1;
        for (int h = step_start; h < fd->hist_count - 1; h += step_skip) {
            if (!in_area(fd->history[h].pos)) continue;
            int pr = lat_to_row(fd->history[h].pos.lat);
            int pc = lon_to_col(fd->history[h].pos.lon);
            if (pr >= 1 && pr < MAP_H-1 && pc >= 1 && pc < MAP_W-1)
                if (grid[pr][pc] == ' ')
                    grid[pr][pc] = '.';
        }
    }

    int n_violating = 0;
    char viol_ids[MAX_FLIGHTS][ID_LEN];
    for (int fi = 0; fi < shm->n_flights; fi++) {
        FlightData *fd = &shm->flights[fi];
        if (!fd->active) continue;
        if (!in_area(fd->pos)) continue;
        int pr = lat_to_row(fd->pos.lat);
        int pc = lon_to_col(fd->pos.lon);
        if (pr < 1 || pr >= MAP_H-1 || pc < 1 || pc >= MAP_W-1) continue;
        int is_viol = flight_in_violation(shm, fi, step);
        grid[pr][pc] = is_viol ? '*' : '@';
        flight_at[pr][pc] = fi;
        if (is_viol && n_violating < MAX_FLIGHTS)
            strncpy(viol_ids[n_violating++], fd->id, ID_LEN);
    }

    for (i = 0; i < MAP_H; i++) {
        p += snprintf(obuf + p, sizeof(obuf) - p, "  ");
        for (j = 0; j < MAP_W; j++) {
            char c = grid[i][j];
            if (c == '*') {
                p += snprintf(obuf + p, sizeof(obuf) - p,
                    "%s%c%s", ANSI_RED ANSI_BOLD, c, ANSI_RESET);
            } else if (c == '@') {
                int fi = flight_at[i][j];
                const char *color = ANSI_BOLD ANSI_WHITE;
                if (fi >= 0) color = flight_mark(fi);
                p += snprintf(obuf + p, sizeof(obuf) - p,
                    "%s\xe2\x97\x8f%s", color, ANSI_RESET);
            } else if (c == '.') {
                p += snprintf(obuf + p, sizeof(obuf) - p,
                    "%s%c%s", ANSI_DIM ANSI_CYAN, c, ANSI_RESET);
            } else {
                obuf[p++] = c;
            }
        }
        obuf[p++] = '\n';
    }

    p += snprintf(obuf + p, sizeof(obuf) - p,
        "  ------------------------------------------------------------------\n");

    for (int fi = 0; fi < shm->n_flights; fi++) {
        FlightData *fd = &shm->flights[fi];
        int viol_now = fd->active ? flight_in_violation(shm, fi, step) : 0;
        int inside = in_area_full(fd->pos);
        const char *status = fd->completed ? "DONE" :
                             !fd->active ? "WAIT" :
                             inside ? "IN" : "OUT";
        double spd = fd->active ? (sqrt(fd->vel.vx*fd->vel.vx + fd->vel.vy*fd->vel.vy) / 0.514444) : 0.0;
        p += snprintf(obuf + p, sizeof(obuf) - p,
            "  %s\xe2\x97\x8f%s %-8s  alt=%6.0fm  spd=%4.0fkt  %s%s\n",
            flight_mark(fi), ANSI_RESET, fd->id,
            fd->pos.alt, spd, status,
            viol_now ? "  ** VIOLATION **" : "");
    }

    if (n_violating > 0) {
        p += snprintf(obuf + p, sizeof(obuf) - p,
            "  %s>>> COLLISION at step %d:%s ",
            ANSI_RED ANSI_BOLD, step, ANSI_RESET);
        for (i = 0; i < n_violating; i++) {
            p += snprintf(obuf + p, sizeof(obuf) - p, "%s%s",
                i > 0 ? ", " : "", viol_ids[i]);
        }
        p += snprintf(obuf + p, sizeof(obuf) - p, "\n");
    }

    write(STDOUT_FILENO, obuf, p);
}

void show_summary(SharedData *shm, int total_steps)
{
    printf("\033[2J\033[H");

    int result = (shm->n_violations == 0) ? 1 : 0;

    printf("  +------------ SIMULATION SUMMARY ------------+\n");
    printf("  | Steps: %-5d  Flights: %-3d  Violations: %-3d|\n",
           total_steps, shm->n_flights, shm->n_violations);
    printf("  | RESULT: %s                               |\n",
           result ? ANSI_GREEN "PASS" ANSI_RESET : ANSI_RED "FAIL" ANSI_RESET);
    printf("  +--------------------------------------------+\n\n");

    char buf[128];
    time_t now = time(NULL);
    struct tm *tm_ptr = localtime(&now);
    strftime(buf, sizeof(buf), "%Y%m%d_%H%M%S", tm_ptr);

    printf("  Flights:\n");
    for (int i = 0; i < shm->n_flights; i++) {
        FlightData *fd = &shm->flights[i];
        const char *st = fd->completed ? "DONE" :
                         fd->ever_in_area ? "AREA" : "IDLE";
        printf("  %s\xe2\x97\x8f%s %-8s  %-4s  violations=%d\n",
               flight_mark(i), ANSI_RESET, fd->id, st, fd->n_viol);
    }

    if (shm->n_violations > 0) {
        printf("\n  Violations:\n");
        for (int i = 0; i < shm->n_violations && i < 10; i++) {
            Violation *v = &shm->violations[i];
            printf("  #%-2d step=%-5d  %s <-> %s  h=%.0fm  v=%.0fm\n",
                   i + 1, v->step,
                   shm->flights[v->fa].id, shm->flights[v->fb].id,
                   v->h_m, v->v_m);
        }
        if (shm->n_violations > 10)
            printf("  ... and %d more violations\n", shm->n_violations - 10);
    }

    printf("\n  Report written to report_%s.txt  RESULT: %s\n",
           buf, result ? "PASS" : "FAIL");
    fflush(stdout);
}

void show_sim_status(SharedData *shm, int step, int active)
{
    (void)step;
    printf("  Active=%d  Violations=%d  Flights=", active, shm->n_violations);
    for (int i = 0; i < shm->n_flights; i++) {
        printf("%s%c", shm->flights[i].id,
               shm->flights[i].completed ? '!' :
               shm->flights[i].active ? '+' : '-');
        if (i < shm->n_flights - 1) printf(",");
    }
    printf("\n");
    fflush(stdout);
}
