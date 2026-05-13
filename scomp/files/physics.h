#ifndef PHYSICS_H
#define PHYSICS_H


#include "common.h"
#include <math.h>

/* Horizontal distance between two positions (metres) */
static inline double h_dist(Pos3D a, Pos3D b)
{
    double mid = D2R((a.lat + b.lat) / 2.0);
    double dx  = D2R(b.lon - a.lon) * cos(mid);
    double dy  = D2R(b.lat - a.lat);
    return sqrt(dx*dx + dy*dy) * EARTH_R;
}

/* Returns 1 if position is inside the simulation area */
static inline int in_area(Pos3D p)
{
    return p.lat >= AREA_LAT_MIN && p.lat <= AREA_LAT_MAX &&
           p.lon >= AREA_LON_MIN && p.lon <= AREA_LON_MAX;
}

/*
 * safety_breach
 *   Returns 1 if the two positions violate the safety cylinder.
 *   Writes the measured distances to h_m and v_m.
 */
static inline int safety_breach(Pos3D a, Pos3D b, double *h_m, double *v_m)
{
    double h = h_dist(a, b);
    double v = fabs(a.alt - b.alt);
    *h_m = h;
    *v_m = v;
    return (h < SAFETY_H_M) && (v < SAFETY_V_M);
}

/* Linear interpolation of speed from profile table */
static inline double interp_spd(ProfileEntry *t, int n, double alt, double def)
{
    int i;
    double f;
    if (n <= 0)               return def;
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

/* Linear interpolation of vertical rate from profile table */
static inline double interp_rate(ProfileEntry *t, int n, double alt, double def)
{
    int i;
    double f;
    if (n <= 0)               return def;
    if (alt <= t[0].alt_m)   return t[0].rate_ms;
    if (alt >= t[n-1].alt_m) return t[n-1].rate_ms;
    for (i = 0; i < n-1; i++) {
        if (alt >= t[i].alt_m && alt <= t[i+1].alt_m) {
            f = (alt - t[i].alt_m) / (t[i+1].alt_m - t[i].alt_m);
            return t[i].rate_ms + f * (t[i+1].rate_ms - t[i].rate_ms);
        }
    }
    return def;
}

/*
 * advance
 *   Move pos along seg by dt seconds.
 *   Fills vel with the current velocity vector.
 *   Returns 0 = still on this segment, 1 = segment complete.
 */
static inline int advance(FlightPlan *plan, Segment *seg,
                           Pos3D *pos, Vel3D *vel, double dt)
{
    double hrem, vrem, totrem, spd_ms, vz, hspd, hmag, dx_m, dy_m, ux, uy, dh;
    double lat_m;

    hrem   = h_dist(*pos, seg->end);
    vrem   = seg->end.alt - pos->alt;
    totrem = sqrt(hrem*hrem + vrem*vrem);

    /* Already at end */
    if (totrem < 1.0) {
        *pos = seg->end;
        vel->vx = vel->vy = vel->vz = 0.0;
        return 1;
    }

    /* Determine speed and vertical rate for this phase */
    if (seg->phase == CRUISE) {
        spd_ms = KT_TO_MS(plan->cruise_kt);
        vz     = 0.0;
    } else if (seg->phase == CLIMB) {
        spd_ms = KT_TO_MS(interp_spd(plan->climb, plan->n_climb, pos->alt, plan->cruise_kt));
        vz     = interp_rate(plan->climb, plan->n_climb, pos->alt, 8.0);
    } else { /* DESCEND */
        spd_ms = KT_TO_MS(interp_spd(plan->desc, plan->n_desc, pos->alt, plan->cruise_kt));
        vz     = interp_rate(plan->desc, plan->n_desc, pos->alt, -8.0);
    }
    if (spd_ms < 1.0) spd_ms = 1.0;

    /* Overshoot: snap to end */
    if (spd_ms * dt >= totrem) {
        *pos = seg->end;
        vel->vx = vel->vy = vel->vz = 0.0;
        return 1;
    }

    /* Horizontal direction unit vector */
    lat_m = D2R((pos->lat + seg->end.lat) / 2.0);
    dx_m  = D2R(seg->end.lon - pos->lon) * cos(lat_m) * EARTH_R;
    dy_m  = D2R(seg->end.lat - pos->lat) * EARTH_R;
    hmag  = sqrt(dx_m*dx_m + dy_m*dy_m);
    ux    = (hmag > 0.001) ? dx_m / hmag : 0.0;
    uy    = (hmag > 0.001) ? dy_m / hmag : 0.0;

    /* Clamp vz so it does not exceed total speed */
    if (fabs(vz) > spd_ms) vz = (vz > 0 ? 1.0 : -1.0) * spd_ms;
    hspd = sqrt(spd_ms*spd_ms - vz*vz);

    /* Write velocity vector */
    vel->vx = ux * hspd;
    vel->vy = uy * hspd;
    vel->vz = vz;

    /* Integrate position */
    dh = hspd * dt;
    if (hmag > 0.001) {
        pos->lon += R2D((ux * dh) / (cos(lat_m) * EARTH_R));
        pos->lat += R2D((uy * dh) / EARTH_R);
    }
    pos->alt += vz * dt;

    /* Altitude clamping */
    if (seg->phase == CLIMB   && pos->alt > seg->end.alt) pos->alt = seg->end.alt;
    if (seg->phase == DESCEND && pos->alt < seg->end.alt) pos->alt = seg->end.alt;

    return 0;
}

#endif /* PHYSICS_H */

/* Returns 1 if pos is inside the simulation area including altitude check */
static inline int in_area_full(Pos3D p)
{
    return p.lat >= AREA_LAT_MIN && p.lat <= AREA_LAT_MAX &&
           p.lon >= AREA_LON_MIN && p.lon <= AREA_LON_MAX &&
           p.alt >= 0.0          && p.alt <= AREA_MAX_ALT_M;
}
