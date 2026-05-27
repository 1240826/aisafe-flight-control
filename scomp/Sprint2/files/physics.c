/*
 * physics.c - Physics engine implementation
*/

#include "physics.h"
#include <math.h>

/* Horizontal distance in metres between two positions (Equirectangular) */
double h_dist(Pos3D a, Pos3D b)
{
	double mid = D2R((a.lat + b.lat) / 2.0);
    	double dx = D2R(b.lon - a.lon) * cos(mid);
    	double dy = D2R(b.lat - a.lat);
    	return sqrt(dx*dx + dy*dy) * EARTH_R;
}

/* 1 if position is inside the area (lat/lon only — used at startup) */
int in_area(Pos3D p)
{
    	return p.lat >= AREA_LAT_MIN && p.lat <= AREA_LAT_MAX && p.lon >= AREA_LON_MIN && p.lon <= AREA_LON_MAX;
}

/* 1 if position is inside the area including altitude */
int in_area_full(Pos3D p)
{
    	return p.lat >= AREA_LAT_MIN && p.lat <= AREA_LAT_MAX && p.lon >= AREA_LON_MIN && p.lon <= AREA_LON_MAX && p.alt >= 0.0 && p.alt <= AREA_MAX_ALT_M;
}

/* 1 if two positions violate the ICAO safety cylinder; fills h_m, v_m */
int safety_breach(Pos3D a, Pos3D b, double *h_m, double *v_m)
{
    	double h = h_dist(a, b);
    	double v = fabs(a.alt - b.alt);
    	*h_m = h;
    	*v_m = v;
    	return (h < SAFETY_H_M) && (v < SAFETY_V_M);
}

/* Linear interpolation of speed (kt) from profile table at altitude */
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

/* Linear interpolation of vertical rate (m/s) from profile table at altitude */
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

/*
 * advance — move pos along seg by dt seconds.
 * Fills vel. Returns 0 = segment ongoing, 1 = segment complete.
 */
int advance(FlightPlan *plan, Segment *seg, Pos3D *pos, Vel3D *vel, double dt)
{
    	double hrem, vrem, totrem;
    	double spd_ms, vz, hspd, hmag;
    	double dx_m, dy_m, ux, uy, dh;
    	double lat_m;

    	hrem = h_dist(*pos, seg->end);
    	vrem = seg->end.alt - pos->alt;

    /* CRUISE: vz=0, altitude never changes — use horizontal distance only.
     * Needed when alt_adjust (US102) shifted pos.alt above end.alt. */
    	if (seg->phase == CRUISE) {
        	totrem = hrem;
    	} else {
        	totrem = sqrt(hrem*hrem + vrem*vrem);
    	}

    	if (totrem < 1.0) {
        	*pos = seg->end;
        	vel->vx = 0.0;
        	vel->vy = 0.0;
        	vel->vz = 0.0;
        	return 1;
    	}

    	if (seg->phase == CRUISE) {
        	spd_ms = KT_TO_MS(plan->cruise_kt);
        	vz = 0.0;
    	} else if (seg->phase == CLIMB) {
        	spd_ms = KT_TO_MS(interp_spd(plan->climb, plan->n_climb,
                                      pos->alt, plan->cruise_kt));
        	vz = interp_rate(plan->climb, plan->n_climb, pos->alt, 8.0);
    	} else {
        	spd_ms = KT_TO_MS(interp_spd(plan->desc, plan->n_desc,
                                      pos->alt, plan->cruise_kt));
        	vz = interp_rate(plan->desc, plan->n_desc, pos->alt, -8.0);
    	}
    	if (spd_ms < 1.0) { spd_ms = 1.0; }

    	/* Overshoot: snap to endpoint */
    	if (spd_ms * dt >= totrem) {
        	*pos = seg->end;
        	vel->vx = 0.0;
        	vel->vy = 0.0;
        	vel->vz = 0.0;
        	return 1;
    	}

    	/* Horizontal unit vector toward endpoint */
    	lat_m = D2R((pos->lat + seg->end.lat) / 2.0);
    	dx_m = D2R(seg->end.lon - pos->lon) * cos(lat_m) * EARTH_R;
    	dy_m = D2R(seg->end.lat - pos->lat) * EARTH_R;
    	hmag = sqrt(dx_m*dx_m + dy_m*dy_m);

    	if (hmag > 0.001) {
        	ux = dx_m / hmag;
        	uy = dy_m / hmag;
    	} else {
        	ux = 0.0;
        	uy = 0.0;
    	}

    	/* Clamp vz so it does not exceed total airspeed */
    	if (fabs(vz) > spd_ms) {
        	if (vz > 0.0) {
            		vz = spd_ms;
        	} else {
            		vz = -spd_ms;
        	}
    	}
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

    	/* Clamp altitude to avoid overshoot */
    	if (seg->phase == CLIMB && pos->alt > seg->end.alt) { pos->alt = seg->end.alt; }
    	if (seg->phase == DESCEND && pos->alt < seg->end.alt) { pos->alt = seg->end.alt; }

    	return 0;
}
