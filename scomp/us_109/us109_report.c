/*
 * AISafe Flight Control Simulation
 * us109_report.c - US109: Generate and store final simulation report
 * SCOMP Sprint 2 - 2025/2026
 *
 * Prof clarification (Luís Nogueira):
 * "In Sprint 2, 'report generation thread' should be read as 'process'."
 * This implementation uses the parent process — correct for Sprint 2.
 */

#define _POSIX_C_SOURCE 200809L

#include "common.h"

#include <stdio.h>
#include <time.h>
#include <math.h>

static const char *phase_str(Phase p)
{
    if (p == CLIMB)   return "CLIMB";
    if (p == CRUISE)  return "CRUISE";
    return "DESCEND";
}

void write_report(int steps, int timestep,
                  FlightPlan *plans, int n_flights,
                  FlightState *flights,
                  Violation *violations, int n_viol)
{
    FILE *f;
    time_t now;
    char ts[32], evts[32], filename[64];
    const char *status;
    Violation *v;
    int i, j, resolved;

    now = time(NULL);
    strftime(ts, sizeof(ts), "%Y-%m-%d %H:%M:%S", localtime(&now));
    strftime(filename, sizeof(filename), "report_%Y-%m-%d_%H-%M-%S.txt", localtime(&now));

    f = fopen(filename, "w");
    if (!f) { perror("fopen"); return; }

    fprintf(f,
        "=======================================================\n"
        "  AISafe Flight Control Simulation — Report\n"
        "  Generated: %s\n"
        "=======================================================\n\n",
        ts);

    fprintf(f, "SIMULATION AREA\n");
    fprintf(f, "  Latitude:  [%.2f, %.2f] degrees\n", AREA_LAT_MIN, AREA_LAT_MAX);
    fprintf(f, "  Longitude: [%.2f, %.2f] degrees\n", AREA_LON_MIN, AREA_LON_MAX);
    fprintf(f, "  Altitude:  [0, %.0f] m\n\n", AREA_MAX_ALT_M);

    fprintf(f, "SIMULATION PARAMETERS\n");
    fprintf(f, "  Timestep:  %d s\n", timestep);
    fprintf(f, "  Steps run: %d  (%.1f min simulated)\n",
            steps, (double)(steps * timestep) / 60.0);
    fprintf(f, "  Safety cylinder:\n");
    fprintf(f, "    Horizontal: %.0f m  (%.1f NM)  [ICAO Doc 4444]\n",
            SAFETY_H_M, SAFETY_H_NM);
    fprintf(f, "    Vertical:   %.0f m  (%.0f ft)  [ICAO RVSM]\n\n",
            SAFETY_V_M, SAFETY_V_M / 0.3048);

    /* flight summary table */
    fprintf(f, "FLIGHTS  (%d total)\n", n_flights);
    fprintf(f, "  %-10s  %-6s  %-12s  %-10s  Route\n",
            "Flight", "PID", "Status", "Violations");
    for (i = 0; i < n_flights; i++) {
        if (flights[i].active)        status = "RUNNING";
        else if (flights[i].killed)   status = "TERMINATED";
        else                          status = "COMPLETED";

        fprintf(f, "  %-10s  %-6d  %-12s  %-10d  %d seg",
                flights[i].id, flights[i].pid, status,
                flights[i].n_viol, plans[i].n_seg);

        fprintf(f, " (");
        for (j = 0; j < plans[i].n_seg; j++) {
            if (j > 0) fprintf(f, " → ");
            fprintf(f, "%s", phase_str(plans[i].seg[j].phase));
        }
        fprintf(f, ")\n");
    }
    fprintf(f, "\n");

    /* per-flight details including duration */
    fprintf(f, "FLIGHT DETAILS\n");
    for (i = 0; i < n_flights; i++) {
        double duration_min = (flights[i].hist_count * timestep) / 60.0;

        fprintf(f, "  %s  fuel=%.0f kg  cruise=%.0f kt\n",
                flights[i].id, plans[i].fuel_kg, plans[i].cruise_kt);
        fprintf(f, "  Started %s area  |  Last position: (%.4f, %.4f, %.0fm)  %s\n",
                flights[i].hist_count > 0 ? "inside" : "outside",
                flights[i].last_pos.lat,
                flights[i].last_pos.lon,
                flights[i].last_pos.alt,
                phase_str(flights[i].last_phase));
        fprintf(f, "  Position history: %d snapshots stored\n",
                flights[i].hist_count);
        fprintf(f, "  Flight duration: %.1f min\n\n", duration_min);
    }

    /* count resolved violations */
    resolved = 0;
    for (i = 0; i < n_viol; i++) {
        if (violations[i].resolved) resolved++;
    }

    fprintf(f, "SAFETY EVENTS  (%d total)\n\n", n_viol);

    if (n_viol == 0) {
        fprintf(f, "  No violations detected.\n\n");
    } else {
        for (i = 0; i < n_viol; i++) {
            v = &violations[i];
            strftime(evts, sizeof(evts), "%H:%M:%S", localtime(&v->ts));

            fprintf(f, "  Event #%d  step=%d  time=%s\n", i+1, v->step, evts);
            fprintf(f, "    %s: pos=(%.4f, %.4f, %.0fm)  vel=(%.1f, %.1f, %.1f) m/s\n",
                    flights[v->fa].id,
                    v->pa.lat, v->pa.lon, v->pa.alt,
                    v->va.vx, v->va.vy, v->va.vz);
            fprintf(f, "    %s: pos=(%.4f, %.4f, %.0fm)  vel=(%.1f, %.1f, %.1f) m/s\n",
                    flights[v->fb].id,
                    v->pb.lat, v->pb.lon, v->pb.alt,
                    v->vb.vx, v->vb.vy, v->vb.vz);
            fprintf(f, "    Separation: H=%.0fm (limit %.0fm)  V=%.0fm (limit %.0fm)\n",
                    v->h_m, SAFETY_H_M, v->v_m, SAFETY_V_M);

            if (v->resolved)
                fprintf(f, "    Resolution: %s ordered to climb %.0fm (separation restored)\n",
                        flights[v->adjusted_flt].id, v->adj_m);
            else
                fprintf(f, "    Resolution: none — violation threshold exceeded\n");

            fprintf(f, "\n");
        }
    }

    fprintf(f, "=======================================================\n");
    if (n_viol == 0) {
        fprintf(f, "  RESULT: PASS — no safety violations detected\n");
    } else {
        fprintf(f, "  RESULT: FAIL — %d safety event(s) detected\n", n_viol);
        fprintf(f, "    Resolved by altitude adjustment: %d\n", resolved);
        fprintf(f, "    Unresolved (threshold exceeded): %d\n", n_viol - resolved);
        /* resolution rate as percentage */
        fprintf(f, "    Resolution rate: %.0f%%\n",
                n_viol > 0 ? (100.0 * resolved / n_viol) : 100.0);
    }
    fprintf(f, "=======================================================\n");

    fclose(f);
    printf("[CTRL]   Report saved: %s\n", filename);
}
