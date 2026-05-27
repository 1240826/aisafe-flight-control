#ifndef PHYSICS_H
#define PHYSICS_H

#include "common.h"

double h_dist(Pos3D a, Pos3D b);
int in_area(Pos3D p);
int in_area_full(Pos3D p);
int safety_breach(Pos3D a, Pos3D b, double *h_m, double *v_m);
double interp_spd(ProfileEntry *t, int n, double alt, double def);
double interp_rate(ProfileEntry *t, int n, double alt, double def);
int advance(FlightPlan *plan, Segment *seg, Pos3D *pos, Vel3D *vel, double dt);

#endif 
