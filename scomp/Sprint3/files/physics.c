#include "physics.h"
#include <math.h>
#include <string.h>
#include <stdio.h>

double h_dist(Pos3D a, Pos3D b)
{
    double mid = D2R((a.lat + b.lat) / 2.0);
    double dx = D2R(b.lon - a.lon) * cos(mid);
    double dy = D2R(b.lat - a.lat);
    return sqrt(dx*dx + dy*dy) * EARTH_R;
}

int in_area(Pos3D p)
{
    return p.lat >= AREA_LAT_MIN && p.lat <= AREA_LAT_MAX &&
           p.lon >= AREA_LON_MIN && p.lon <= AREA_LON_MAX;
}

int in_area_full(Pos3D p)
{
    return in_area(p) && p.alt >= 0.0 && p.alt <= AREA_MAX_ALT_M;
}

int safety_breach(Pos3D a, Pos3D b, double *h_m, double *v_m)
{
    double h = h_dist(a, b);
    double v = fabs(a.alt - b.alt);
    *h_m = h;
    *v_m = v;
    return (h < SAFETY_H_M) && (v < SAFETY_V_M);
}

double interp_spd(ProfileEntry *t, int n, double alt, double def)
{
    int i;
    double f;
    if (n <= 0) return def;
    if (alt <= t[0].alt_m)   return t[0].spd_kt;
    if (alt >= t[n-1].alt_m) return t[n-1].spd_kt;
    for (i = 0; i < n-1; i++) {
        if (alt >= t[i].alt_m && alt <= t[i+1].alt_m) {
            f = (alt - t[i].alt_m) / (t[i+1].alt_m - t[i].alt_m);
            return t[i].spd_kt + f * (t[i+1].spd_kt - t[i].spd_kt);
        }
    }
    return def;
}

double interp_rate(ProfileEntry *t, int n, double alt, double def)
{
    int i;
    double f;
    if (n <= 0) return def;
    if (alt <= t[0].alt_m) return t[0].rate_ms;
    if (alt >= t[n-1].alt_m) return t[n-1].rate_ms;
    for (i = 0; i < n-1; i++) {
        if (alt >= t[i].alt_m && alt <= t[i+1].alt_m) {
            f = (alt - t[i].alt_m) / (t[i+1].alt_m - t[i].alt_m);
            return t[i].rate_ms + f * (t[i+1].rate_ms - t[i].rate_ms);
        }
    }
    return def;
}

int advance(FlightPlan *plan, Segment *seg, Pos3D *pos, Vel3D *vel, double dt)
{
    double hrem, vrem, totrem;
    double spd_ms, vz, hspd, hmag;
    double dx_m, dy_m, ux, uy, dh;
    double lat_m;

    hrem = h_dist(*pos, seg->end);
    vrem = seg->end.alt - pos->alt;

    if (seg->phase == CRUISE) {
        totrem = hrem;
    } else {
        totrem = sqrt(hrem*hrem + vrem*vrem);
    }

    if (totrem < 1.0) {
        *pos = seg->end;
        vel->vx = 0.0; vel->vy = 0.0; vel->vz = 0.0;
        return 1;
    }

    if (seg->phase == CRUISE) {
        spd_ms = KT_TO_MS(plan->cruise_kt);
        vz = 0.0;
    } else if (seg->phase == CLIMB) {
        spd_ms = KT_TO_MS(interp_spd(plan->climb, plan->n_climb, pos->alt, plan->cruise_kt));
        vz = interp_rate(plan->climb, plan->n_climb, pos->alt, 8.0);
    } else {
        spd_ms = KT_TO_MS(interp_spd(plan->desc, plan->n_desc, pos->alt, plan->cruise_kt));
        vz = interp_rate(plan->desc, plan->n_desc, pos->alt, -8.0);
    }
    if (spd_ms < 1.0) spd_ms = 1.0;

    if (spd_ms * dt >= totrem) {
        *pos = seg->end;
        vel->vx = 0.0; vel->vy = 0.0; vel->vz = 0.0;
        return 1;
    }

    lat_m = D2R((pos->lat + seg->end.lat) / 2.0);
    dx_m = D2R(seg->end.lon - pos->lon) * cos(lat_m) * EARTH_R;
    dy_m = D2R(seg->end.lat - pos->lat) * EARTH_R;
    hmag = sqrt(dx_m*dx_m + dy_m*dy_m);

    if (hmag > 0.001) { ux = dx_m / hmag; uy = dy_m / hmag; }
    else { ux = 0.0; uy = 0.0; }

    if (fabs(vz) > spd_ms) vz = (vz > 0) ? spd_ms : -spd_ms;
    hspd = sqrt(spd_ms*spd_ms - vz*vz);

    vel->vx = ux * hspd;
    vel->vy = uy * hspd;
    vel->vz = vz;

    dh = hspd * dt;
    if (hmag > 0.001) {
        pos->lon += R2D((ux * dh) / (cos(lat_m) * EARTH_R));
        pos->lat += R2D((uy * dh) / EARTH_R);
    }
    pos->alt += vz * dt;

    if (seg->phase == CLIMB && pos->alt > seg->end.alt)
        pos->alt = seg->end.alt;
    if (seg->phase == DESCEND && pos->alt < seg->end.alt)
        pos->alt = seg->end.alt;

    return 0;
}

void fast_forward_flight(FlightPlan *plan, double seconds, Pos3D *out_pos, Vel3D *out_vel, Phase *out_phase, int *out_seg_idx)
{
    Pos3D pos;
    Vel3D vel;
    int cur = 0;
    double remaining = seconds;
    double dt_step = 1.0;

    if (plan->n_seg <= 0) {
        memset(out_pos, 0, sizeof(Pos3D));
        memset(out_vel, 0, sizeof(Vel3D));
        *out_phase = CRUISE;
        *out_seg_idx = 0;
        return;
    }

    pos = plan->seg[0].start;
    memset(&vel, 0, sizeof(Vel3D));

    while (remaining > 0.0 && cur < plan->n_seg) {
        double step = (remaining < dt_step) ? remaining : dt_step;
        int done = advance(plan, &plan->seg[cur], &pos, &vel, step);
        remaining -= step;
        if (done) {
            cur++;
            if (cur < plan->n_seg)
                pos = plan->seg[cur].start;
        }
    }

    *out_pos = pos;
    *out_vel = vel;
    *out_phase = (cur < plan->n_seg) ? plan->seg[cur].phase : plan->seg[plan->n_seg - 1].phase;
    *out_seg_idx = (cur < plan->n_seg) ? cur : plan->n_seg - 1;
}

int parse_time(const char *tstr) {
    int h, m;
    if (sscanf(tstr, "%d:%d", &h, &m) == 2)
        return h * 3600 + m * 60;
    return 0;
}
