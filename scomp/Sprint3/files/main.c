#include "json_parser.h"
#include "us105_init.h"
#include "us106_threads.h"
#include "us108_sync.h"
#include "us109_report.h"
#include "us110_env.h"
#include "ui.h"
#include "physics.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/types.h>

static SharedData *shm = NULL;
static FlightPlan plans[MAX_FLIGHTS];
static int n_plans = 0;
static char scenario_path[512] = "";
static int sim_start_sec = 8 * 3600 + 30 * 60;  // 08:30 UTC = 09:30 UTC+1
static int print_interval = PRINT_INTERVAL_DEFAULT;
static int local_tz_offset = 60;  // UTC+1 (Iberian time)
static volatile sig_atomic_t stop_sim = 0;
static char weather_path[512] = "";
static int weather_provider = 0;  // 0=Crazy Weather, 1=Happy Weather, -1=none

static void handle_sigint(int sig) {
    (void)sig;
    stop_sim = 1;
    if (shm) shm->running = 0;
}

static char *jp_obj(const char *src, const char *key) {
    char *buf;
    int len;
    const char *p = strstr(src, key);
    if (!p) return NULL;
    p = strchr(p, '{');
    if (!p) return NULL;
    const char *e = jp_close(p, '{', '}');
    len = (int)(e - p + 1);
    buf = malloc(len + 1);
    if (!buf) return NULL;
    memcpy(buf, p, len); buf[len] = '\0';
    return buf;
}

static void jp_pos(const char *obj, Pos3D *pos) {
    double v;
    char *a;
    if (jp_dbl(obj, "\"Latitude\":",  &v)) pos->lat = v;
    if (jp_dbl(obj, "\"Longitude\":", &v)) pos->lon = v;
    a = jp_obj(obj, "\"Altitude\"");
    if (a) {
        if (jp_dbl(a, "\"Quantity\":", &v) || jp_dbl(a, "\"Value\":", &v))
            pos->alt = v;
        free(a);
    }
}

static int jp_profile(const char *arr, ProfileEntry *t, int max, int is_desc) {
    int n = 0;
    double v;
    const char *p = arr;
    char *buf, *a, *s, *r;
    while (n < max) {
        while (*p && *p != '{' && *p != ']') p++;
        if (!*p || *p == ']') break;
        const char *e = jp_close(p, '{', '}');
        int len = (int)(e - p + 1);
        buf = malloc(len + 1);
        if (!buf) break;
        memcpy(buf, p, len); buf[len] = '\0';
        a = jp_obj(buf, "\"Altitude\"");
        if (a) { if (jp_dbl(a, "\"Value\":", &v)) t[n].alt_m = v; free(a); }
        s = jp_obj(buf, "\"Speed\"");
        if (s) { if (jp_dbl(s, "\"Value\":", &v)) t[n].spd_kt = v; free(s); }
        r = jp_obj(buf, is_desc ? "\"RateDescent\"" : "\"RateClimb\"");
        if (r) { if (jp_dbl(r, "\"Value\":", &v)) t[n].rate_ms = v; free(r); }
        free(buf);
        n++;
        p = e + 1;
    }
    return n;
}

static int jp_segments(const char *arr, Segment *segs, int max) {
    int n = 0;
    char mode[16];
    const char *p = arr;
    char *buf, *so, *eo;
    while (n < max) {
        while (*p && *p != '{' && *p != ']') p++;
        if (!*p || *p == ']') break;
        const char *e = jp_close(p, '{', '}');
        int len = (int)(e - p + 1);
        buf = malloc(len + 1);
        if (!buf) break;
        memcpy(buf, p, len); buf[len] = '\0';
        mode[0] = '\0';
        jp_str(buf, "\"Mode\":", mode, sizeof(mode));
        if      (strcmp(mode, "climb") == 0)   segs[n].phase = CLIMB;
        else if (strcmp(mode, "descend") == 0) segs[n].phase = DESCEND;
        else                                   segs[n].phase = CRUISE;
        so = jp_obj(buf, "\"Start\""); if (so) { jp_pos(so, &segs[n].start); free(so); }
        eo = jp_obj(buf, "\"End\"");   if (eo) { jp_pos(eo, &segs[n].end);   free(eo); }
        free(buf);
        n++;
        p = e + 1;
    }
    return n;
}

static int jp_plan(const char *obj, FlightPlan *fp) {
    const char *p;
    char *la, *leg, *fo, *ca, *da, *co, *so, *pr;
    double v;
    int ll, id_int;
    char dep_str[16];
    char tz_str[16];

    memset(fp, 0, sizeof(FlightPlan));

    p = strstr(obj, "\"ID\":");
    if (p) {
        p = jp_skip(p + 5);
        if (*p == '"') jp_str(obj, "\"ID\":", fp->id, ID_LEN);
        else { id_int = (int)strtol(p, NULL, 10); snprintf(fp->id, ID_LEN, "%d", id_int); }
    }

    dep_str[0] = '\0';
    tz_str[0] = '\0';
    fp->departure_tz = 0;
    if (jp_str(obj, "\"DepartureTZ\":", tz_str, sizeof(tz_str))) {
        char sign = '+';
        int tz_h = 0, tz_m = 0;
        if (tz_str[0] == '-' || tz_str[0] == '+') {
            sign = tz_str[0];
            sscanf(tz_str + 1, "%d:%d", &tz_h, &tz_m);
        } else if (strcmp(tz_str, "Z") == 0) {
            sign = '+'; tz_h = 0; tz_m = 0;
        }
        fp->departure_tz = (sign == '-' ? -1 : 1) * (tz_h * 60 + tz_m);
    }
    if (jp_str(obj, "\"DepartureTime\":", dep_str, sizeof(dep_str))) {
        int h, m;
        if (sscanf(dep_str, "%d:%d", &h, &m) == 2) {
            int local_sec = h * 3600 + m * 60;
            int utc_sec = local_sec - fp->departure_tz * 60;
            if (utc_sec < 0) utc_sec += 86400;
            if (utc_sec >= 86400) utc_sec -= 86400;
            fp->departure_sec = utc_sec;
        }
    }

    la = jp_arr(obj, "\"Leg\"");
    if (!la) return -1;
    p = la;
    while (*p && *p != '{') p++;
    if (!*p) { free(la); return -1; }
    const char *le = jp_close(p, '{', '}');
    ll = (int)(le - p + 1);
    leg = malloc(ll + 1);
    if (!leg) { free(la); return -1; }
    memcpy(leg, p, ll); leg[ll] = '\0';
    free(la);

    fo = jp_obj(leg, "\"Fuel\"");
    if (fo) { if (jp_dbl(fo, "\"Quantity\":", &v)) fp->fuel_kg = v; free(fo); }

    p = strstr(leg, "\"Flight Profile\"");
    if (!p) p = strstr(leg, "\"FlightProfile\"");
    if (p) {
        const char *pb = strchr(p, '{');
        if (pb) {
            const char *pe = jp_close(pb, '{', '}');
            int plen = (int)(pe - pb + 1);
            pr = malloc(plen + 1);
            if (pr) {
                memcpy(pr, pb, plen); pr[plen] = '\0';
                ca = jp_arr(pr, "\"Climb\"");
                if (ca) { fp->n_climb = jp_profile(ca+1, fp->climb, MAX_PROFILE, 0); free(ca); }
                da = jp_arr(pr, "\"Descend\"");
                if (da) { fp->n_desc = jp_profile(da+1, fp->desc, MAX_PROFILE, 1); free(da); }
                co = jp_obj(pr, "\"Cruise\"");
                if (co) {
                    so = jp_obj(co, "\"Speed\"");
                    if (so) { if (jp_dbl(so, "\"Value\":", &v)) fp->cruise_kt = v; free(so); }
                    free(co);
                }
                free(pr);
            }
        }
    }

    ca = jp_arr(leg, "\"Segments\"");
    if (ca) { fp->n_seg = jp_segments(ca+1, fp->seg, MAX_SEGMENTS); free(ca); }

    free(leg);
    return (fp->n_seg > 0) ? 0 : -1;
}

static int load_plans(const char *path, FlightPlan *plans, int max) {
    FILE *f;
    char *buf;
    long sz;
    int n = 0;
    const char *p;

    f = fopen(path, "r");
    if (!f) { perror("fopen"); return -1; }
    fseek(f, 0, SEEK_END);
    sz = ftell(f);
    rewind(f);
    buf = malloc(sz + 1);
    if (!buf) { fclose(f); return -1; }
    fread(buf, 1, sz, f);
    buf[sz] = '\0';
    fclose(f);

    p = jp_skip(buf);
    if (*p == '[') {
        p++;
        while (n < max) {
            p = jp_skip(p);
            if (*p == ']' || !*p) break;
            if (*p == ',') { p++; continue; }
            if (*p != '{') { p++; continue; }
            const char *e = jp_close(p, '{', '}');
            int len = (int)(e - p + 1);
            char *obj = malloc(len + 1);
            if (!obj) break;
            memcpy(obj, p, len); obj[len] = '\0';
            if (jp_plan(obj, &plans[n]) == 0) n++;
            else fprintf(stderr, "json: skipping plan #%d\n", n + 1);
            free(obj);
            p = e + 1;
        }
    } else if (*p == '{') {
        if (jp_plan(p, &plans[0]) == 0) n = 1;
    }

    free(buf);
    return n;
}

static int run_simulation(void)
{
    if (n_plans <= 0) {
        printf("  No scenario loaded!\n");
        return 1;
    }

    clear_screen();
    printf("\n  Starting simulation...\n\n");

    if (weather_path[0]) {
        env_set_weather_file(weather_path);
    }

    pthread_t detect_thr, report_thr, env_thr;

    if (init_hybrid_simulation(&shm, plans, n_plans, sim_start_sec, TIMESTEP_S,
                                &detect_thr, &report_thr, &env_thr) < 0) {
        fprintf(stderr, "Init failed\n");
        return 1;
    }

    int step;
    int safety_cap = SAFETY_CAP_S / shm->timestep;

    printf("  SIMULATION RUNNING — Ctrl+C to stop\n");
    printf("\n");

    detect_initial_violations(shm);

    for (step = 0; step < safety_cap && shm->running && !stop_sim; step++) {
        shm->current_step = step;

        int remaining = simulation_step(shm);
        if (remaining == 0) {
            printf("\n  All flights completed at step %d\n", step);
            fflush(stdout);
            break;
        }

        pthread_mutex_lock(&shm->detect_mutex);
        shm->step_ready = 1;
        pthread_cond_signal(&shm->detect_cond);
        pthread_mutex_unlock(&shm->detect_mutex);

        pthread_mutex_lock(&shm->env_mutex);
        shm->env_ready = 1;
        pthread_cond_signal(&shm->env_cond);
        pthread_mutex_unlock(&shm->env_mutex);

        int any_in = 0;
        for (int f = 0; f < shm->n_flights; f++)
            if (shm->flights[f].active && shm->flights[f].in_area) { any_in = 1; break; }
        int eff_interval = any_in ? print_interval : (print_interval * 20);
        if (eff_interval > 600) eff_interval = 600;
        if (eff_interval > 0 && step % eff_interval == 0) {
            draw_airspace(shm, step);
            usleep(any_in ? 80000 : 20000);
        }
    }

    shm->generate_report = 1;
    shm->report_total_steps = step;
    shm->running = 0;

    for (int i = 0; i < shm->n_flights; i++)
        sem_post(shm->sem_step_start);

    for (int i = 0; i < shm->n_flights; i++)
        waitpid(shm->flights[i].pid, NULL, 0);

    pthread_mutex_lock(&shm->detect_mutex);
    shm->step_ready = 1;
    pthread_cond_signal(&shm->detect_cond);
    pthread_mutex_unlock(&shm->detect_mutex);

    pthread_mutex_lock(&shm->env_mutex);
    shm->env_ready = 1;
    pthread_cond_signal(&shm->env_cond);
    pthread_mutex_unlock(&shm->env_mutex);

    pthread_mutex_lock(&shm->viol_mutex);
    pthread_cond_signal(&shm->viol_cond);
    pthread_mutex_unlock(&shm->viol_mutex);

    void *ret;
    pthread_join(detect_thr, &ret);
    pthread_join(report_thr, &ret);
    pthread_join(env_thr, &ret);

    printf("\n  Simulation ended: %d steps, %ds simulated.\n",
           step, step * shm->timestep);
    fflush(stdout);

    draw_airspace(shm, step);
    show_summary(shm, step);
    cleanup_shared_memory(shm, n_plans);
    shm = NULL;

    return 0;
}

int main(int argc, char *argv[])
{
    int choice, running = 1;

    {
        struct sigaction act;
        memset(&act, 0, sizeof(struct sigaction));
        sigemptyset(&act.sa_mask);
        act.sa_handler = handle_sigint;
        act.sa_flags = SA_RESTART;
        sigaction(SIGINT, &act, NULL);
    }

    if (argc >= 2) {
        snprintf(scenario_path, sizeof(scenario_path), "%s", argv[1]);
        n_plans = load_plans(scenario_path, plans, MAX_FLIGHTS);
        if (n_plans <= 0) {
            fprintf(stderr, "Error loading %s\n", scenario_path);
            return 1;
        }
        printf("[MAIN] Loaded %d plans from %s\n", n_plans, scenario_path);
    }
    if (argc >= 3) {
        int t = parse_time(argv[2]);
        if (t > 0 || (t == 0 && argv[2][0] == '0')) sim_start_sec = t;
    }
    if (argc >= 4) {
        int val = atoi(argv[3]);
        if (val > 0) print_interval = val;
    }
    if (argc >= 2) {
        return run_simulation();
    }

    while (running) {
        clear_screen();
        show_banner();

        printf("  Scenario: %s  (%d flights)\n",
               scenario_path[0] ? scenario_path : "(none)", n_plans);
        int local_start = (sim_start_sec + local_tz_offset * 60 + 86400) % 86400;
        printf("  Start: %02d:%02d (UTC%+03d:%02d)  Print: every %ds\n\n",
               local_start / 3600, (local_start % 3600) / 60,
               local_tz_offset / 60, abs(local_tz_offset) % 60,
               print_interval);

        if (weather_path[0]) {
            const char *wname = "Synthetic";
            if (strstr(weather_path, "CW.json")) wname = "Crazy Weather";
            else if (strstr(weather_path, "HP.json")) wname = "Happy Weather";
            else if (strstr(weather_path, "SYNTHETIC")) wname = "Synthetic (full coverage)";
            printf("  Weather: %s\n\n", wname);
        } else
            printf("  Weather: synthetic (simple formula)\n\n");

        choice = show_menu();

        switch (choice) {
            case 1: {
                if (browse_scenarios(SCENARIO_DIR, scenario_path,
                                     sizeof(scenario_path)) == 0) {
                    n_plans = load_plans(scenario_path, plans, MAX_FLIGHTS);
                    if (n_plans > 0)
                        printf("  Loaded %d flights from %s.\n",
                               n_plans, scenario_path);
                    else
                        printf("  Error loading scenario.\n");
                }
                printf("  Press Enter..."); fflush(stdout); getchar();
                break;
            }
            case 2: {
                char buf[16];
                printf("\n  Loaded flight departure times:\n");
                if (n_plans <= 0) {
                    printf("  (no flights loaded)\n");
                } else {
                    for (int i = 0; i < n_plans; i++) {
                        int local_sec = (plans[i].departure_sec + plans[i].departure_tz * 60 + 86400) % 86400;
                        int tz_h = plans[i].departure_tz / 60;
                        int tz_m = abs(plans[i].departure_tz) % 60;
                        int utc_h = plans[i].departure_sec / 3600;
                        int utc_m = (plans[i].departure_sec % 3600) / 60;
                        printf("  %-8s dep %02d:%02d (UTC%+03d:%02d) = UTC %02d:%02d\n",
                               plans[i].id, local_sec/3600, (local_sec%3600)/60,
                               tz_h, tz_m, utc_h, utc_m);
                    }
                }
                int local_tz_h = local_tz_offset / 60;
                int local_tz_m = abs(local_tz_offset) % 60;
                printf("  Enter start time (HH:MM, %s UTC%+03d:%02d) [default %02d:%02d]: ",
                       local_tz_offset >= 0 ? "Iberian" : "local",
                       local_tz_h, local_tz_m,
                       (sim_start_sec + local_tz_offset*60 + 86400) % 86400 / 3600,
                       ((sim_start_sec + local_tz_offset*60 + 86400) % 86400 % 3600) / 60);
                fflush(stdout);
                if (fgets(buf, sizeof(buf), stdin)) {
                    buf[strcspn(buf, "\n")] = '\0';
                    if (buf[0] != '\0') {
                        int t = parse_time(buf);
                        if (t >= 0)
                            sim_start_sec = (t - local_tz_offset * 60 + 86400) % 86400;
                    }
                }
                break;
            }
            case 3: {
                char buf[16];
                printf("  Enter print interval in steps (1 step = 1s, e.g. 30): ");
                fflush(stdout);
                if (fgets(buf, sizeof(buf), stdin)) {
                    buf[strcspn(buf, "\n")] = '\0';
                    int val = atoi(buf);
                    if (val > 0) print_interval = val;
                }
                break;
            }
            case 4:
                run_simulation();
                printf("\n  Press Enter to return to menu...");
                fflush(stdout);
                getchar();
                break;
            case 5: {
                if (n_plans <= 0) {
                    printf("  No flights loaded.\n");
                } else {
                    printf("\n  FLIGHT SUMMARY\n");
                    printf("  %-8s %-16s %-8s %-8s %-8s %-8s %s\n",
                           "ID", "Dep (local)", "Cruise", "Altitude", "Segs", "Fuel", "Route");
                    printf("  %-8s %-16s %-8s %-8s %-8s %-8s %s\n",
                           "------", "------", "-------", "--------", "----", "----", "-----");
                    for (int i = 0; i < n_plans; i++) {
                        FlightPlan *p = &plans[i];
                        char alt_str[64] = "";
                        char route[128] = "";
                        char dep_str[32];
                        for (int s = 0; s < p->n_seg; s++) {
                            char buf[32];
                            if (p->seg[s].phase == CLIMB)
                                snprintf(buf, sizeof(buf), "CLB%.0f", p->seg[s].end.alt);
                            else if (p->seg[s].phase == CRUISE)
                                snprintf(buf, sizeof(buf), "CRZ%.0f", p->seg[s].end.alt);
                            else
                                snprintf(buf, sizeof(buf), "DES%.0f", p->seg[s].end.alt);
                            if (s > 0) strncat(route, "->", sizeof(route) - strlen(route) - 1);
                            strncat(route, buf, sizeof(route) - strlen(route) - 1);
                        }
                        double alt = p->seg[0].end.alt;
                        snprintf(alt_str, sizeof(alt_str), "%.0fm", alt);
                        int local_sec = (p->departure_sec + p->departure_tz * 60 + 86400) % 86400;
                        int tz_h = p->departure_tz / 60;
                        int tz_m = abs(p->departure_tz) % 60;
                        snprintf(dep_str, sizeof(dep_str), "%02d:%02d UTC%+03d:%02d",
                                 local_sec/3600, (local_sec%3600)/60, tz_h, tz_m);
                        printf("  %-8s %-16s %-8.0f %-8s %-8d %-8.0f %s\n",
                               p->id, dep_str,
                               p->cruise_kt,
                               alt_str,
                               p->n_seg,
                               p->fuel_kg,
                               route);
                    }
                }
                printf("  Press Enter..."); fflush(stdout); getchar();
                break;
            }
            case 6: {
                printf("\n  Current weather: %s\n",
                       weather_path[0] ? weather_path : "synthetic (no file)");
                printf("  1. Crazy Weather (CW)  — real, up to 12000ft\n");
                printf("  2. Happy Weather (HP)  — real, up to 12000ft\n");
                printf("  3. Synthetic (file)    — full coverage 0-45000ft\n");
                printf("  4. None (formula)      — simple sine wave\n");
                printf("  Choice (1-4) [3]: ");
                fflush(stdout);
                char buf[16];
                int wc = 3;
                if (fgets(buf, sizeof(buf), stdin)) {
                    int c = atoi(buf);
                    if (c >= 1 && c <= 4) wc = c;
                }
                weather_provider = wc;
                switch (wc) {
                    case 1: snprintf(weather_path, sizeof(weather_path), "weather/CW.json"); break;
                    case 2: snprintf(weather_path, sizeof(weather_path), "weather/HP.json"); break;
                    case 3: snprintf(weather_path, sizeof(weather_path), "weather/SYNTHETIC.json"); break;
                    default: weather_path[0] = '\0'; break;
                }
                printf("  Weather set to %s.\n",
                       weather_path[0] ? weather_path : "synthetic formula");
                printf("  Press Enter..."); fflush(stdout); getchar();
                break;
            }
            case 7: {
                printf("\n  Select demo scenario:\n");
                printf("  --- PASS scenarios (no violations) ---\n");
                printf("  1. Transatlantic arrivals (USA -> Iberia)\n");
                printf("  2. Mixed traffic (local + charter)\n");
                printf("  3. Conflict detection\n");
                printf("  4. Safe passage (all clear)\n");
                printf("  5. Heavy traffic (8 flights)\n");
                printf("  6. Comprehensive demo\n");
                printf("  7. Altitude-separated (3 flights, clean)\n");
                printf("  --- FAIL scenarios (guaranteed violations) ---\n");
                printf("  8. Collision horizontal (side-by-side)\n");
                printf("  9. Collision vertical (stacked, 200m apart)\n");
                printf("  10. Collision crossing (head-on)\n");
                printf("  11. Collision overtake (fast catches slow)\n");
                printf("  12. Area entry/exit (boundary visibility)\n");
                printf("  13. Multi-conflict (horizontal + vertical)\n");
                printf("  Choice (1-13) [8]: ");
                fflush(stdout);
                char buf[16];
                int demo_choice = 8;
                if (fgets(buf, sizeof(buf), stdin)) {
                    int c = atoi(buf);
                    if (c >= 1 && c <= 13) demo_choice = c;
                }
                const char *files[] = {
                    "scenario_transatlantic_arrivals.json",
                    "scenario_mixed_traffic.json",
                    "scenario_conflict_detection.json",
                    "scenario_safe_passage.json",
                    "scenario_heavy_traffic.json",
                    "scenario_demo_comprehensive.json",
                    "scenario_pass_altitudes.json",
                    "scenario_collision_guaranteed.json",
                    "scenario_fail_vertical.json",
                    "scenario_fail_crossing.json",
                    "scenario_fail_overtake.json",
                    "scenario_area_entry.json",
                    "scenario_multi_conflict.json"
                };
                int starts[] = {
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 0 * 60,   // 08:00 UTC = 09:00 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    7 * 3600 + 30 * 60,  // 07:30 UTC = 08:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60,  // 08:30 UTC = 09:30 UTC+1
                    8 * 3600 + 30 * 60   // 08:30 UTC = 09:30 UTC+1
                };
                snprintf(scenario_path, sizeof(scenario_path),
                         SCENARIO_DIR "/%s", files[demo_choice - 1]);
                n_plans = load_plans(scenario_path, plans, MAX_FLIGHTS);
                sim_start_sec = starts[demo_choice - 1];
                if (n_plans > 0) {
                    run_simulation();
                }
                printf("\n  Press Enter to return to menu...");
                fflush(stdout);
                getchar();
                break;
            }
            case 8:
                running = 0;
                break;
            default:
                printf("  Invalid choice.\n");
                printf("  Press Enter..."); fflush(stdout); getchar();
                break;
        }
    }

    printf("\n  Goodbye!\n\n");
    return 0;
}
