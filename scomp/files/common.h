#ifndef COMMON_H
#define COMMON_H


#include <sys/types.h>
#include <time.h>

/*  Simulation limits                                                   */

#define MAX_FLIGHTS      10
#define MAX_SEGMENTS     20
#define MAX_PROFILE      16
#define MAX_VIOLATIONS   1024
#define MAX_HISTORY      600    /* position history per flight (10 min) */
#define ID_LEN           16


/*  Time step                                                           */
/*  At 460 kt (237 m/s) head-on closure = 474 m/s.                    */
/*  Horizontal limit = 9260 m (5 NM).  With dt=30s: 7100 m/step →    */
/*  aircraft can enter AND exit the cylinder undetected. With dt=1s:   */
/*  237 m/step — detection is guaranteed step-by-step.                */
/* ------------------------------------------------------------------ */
#define TIMESTEP_S       1

/* Print position every N steps. At 1s/step, every 60 = 1 min shown.  */

/* Violation threshold before early termination                         */
#define VIOL_THRESHOLD    5

#define AREA_LAT_MIN      38.0
#define AREA_LAT_MAX      44.0
#define AREA_LON_MIN     -10.0
#define AREA_LON_MAX      -2.0
#define AREA_MAX_ALT_M    14000.0   /* metres AMSL — configurable     */


#define SAFETY_H_NM       5.0
#define SAFETY_H_M        (5.0 * 1852.0)    /* 9260 m                */
#define SAFETY_V_M        305.0             /* 1000 ft (RVSM)         */


#define ALT_ADJUST_M      400.0   /* > SAFETY_V_M so separation is restored */
#define ALT_ADJUST_STEPS  30      /* hold the adjustment for 30 seconds     */

/* Violation threshold before early shutdown */


/*  Physics constants                                                   */

#define EARTH_R           6371000.0
#define KT_TO_MS(k)       ((k) * 0.514444)
#define D2R(d)            ((d) * 3.14159265358979 / 180.0)
#define R2D(r)            ((r) * 180.0 / 3.14159265358979)

/*  Types                                                               */
typedef enum { CLIMB = 0, CRUISE = 1, DESCEND = 2 } Phase;

typedef struct {
    double lat;    /* degrees North */
    double lon;    /* degrees East  */
    double alt;    /* metres AMSL   */
} Pos3D;

typedef struct {
    double vx;     /* East  m/s     */
    double vy;     /* North m/s     */
    double vz;     /* Up    m/s     */
} Vel3D;

typedef struct {
    Phase  phase;
    Pos3D  start;
    Pos3D  end;
} Segment;

typedef struct {
    double alt_m;
    double spd_kt;
    double rate_ms;
} ProfileEntry;

typedef struct {
    char         id[ID_LEN];
    double       fuel_kg;
    double       cruise_kt;
    int          n_seg;
    Segment      seg[MAX_SEGMENTS];
    int          n_climb;
    ProfileEntry climb[MAX_PROFILE];
    int          n_desc;
    ProfileEntry desc[MAX_PROFILE];
} FlightPlan;

/* Position history snapshot */
typedef struct {
    int   step;
    Pos3D pos;
    Vel3D vel;
    Phase phase;
} Snapshot;

/* Child -> parent via report pipe (US101) */
typedef struct {
    int   idx;
    int   step;
    Pos3D pos;
    Vel3D vel;
    Phase phase;
    int   done;      /* 1 = all segments complete */
} PosUpdate;

/*
 * Parent -> child via go pipe (US103 barrier + US102 route adjustment).
 *
 * safe:       1 = compute next position, 0 = hold current position
 * alt_adjust: metres to add to altitude this step (controller intervention)
 *             0 = no adjustment, positive = climb order
 */
typedef struct {
    int    step;
    int    safe;
    double alt_adjust;
} GoToken;

typedef struct {
    int    step;
    time_t ts;
    int    fa, fb;
    Pos3D  pa, pb;
    Vel3D  va, vb;
    double h_m, v_m;
    int    resolved;     /* 1 = altitude adjustment was applied */
    double adj_m;        /* altitude adjustment given (metres)   */
    int    adjusted_flt; /* which flight got the adjustment      */
} Violation;

typedef struct {
    char     id[ID_LEN];
    pid_t    pid;
    int      rfd;
    int      wfd;
    Pos3D    last_pos;
    Vel3D    last_vel;
    Phase    last_phase;
    int      active;
    int      in_area;
    int      n_viol;
    int      killed;     /* 1 = terminated by SIGTERM           */
    int      adj_steps_left;  /* remaining steps of altitude adjustment */
    Snapshot history[MAX_HISTORY];
    int      hist_count;
} FlightState;

#endif /* COMMON_H */
