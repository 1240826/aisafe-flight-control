#ifndef COMMON_H
#define COMMON_H

#include <sys/types.h>
#include <time.h>
#include <semaphore.h>
#include <pthread.h>
#include <unistd.h>
#include <signal.h>

#define MAX_FLIGHTS      10
#define MAX_SEGMENTS     20
#define MAX_PROFILE      16
#define MAX_VIOLATIONS   10800
#define MAX_HISTORY      600
#define ID_LEN           16
#define MAX_WEATHER_ZONES 50

#define TIMESTEP_S       1

#define AREA_LAT_MIN     38.0
#define AREA_LAT_MAX     44.0
#define AREA_LON_MIN    -10.0
#define AREA_LON_MAX     -2.0
#define AREA_MAX_ALT_M   14000.0

#define SAFETY_H_NM      5.0
#define SAFETY_H_M       (5.0 * 1852.0)
#define SAFETY_V_M       305.0

#define EARTH_R          6371000.0
#define KT_TO_MS(k)      ((k) * 0.514444)
#define D2R(d)           ((d) * 3.14159265358979 / 180.0)
#define R2D(r)           ((r) * 180.0 / 3.14159265358979)

#define SHM_NAME         "/aisafe_sim_v3"
#define SEM_START_NAME   "/aisafe_start"
#define SEM_DONE_NAME    "/aisafe_done"
#define SAFETY_CAP_S     28800  // 8h — covers full flight duration for demo scenarios (LIS-CDG ~5.5h)
#define PRINT_INTERVAL_DEFAULT 10

#define ANSI_RED     "\033[31m"
#define ANSI_GREEN   "\033[32m"
#define ANSI_YELLOW  "\033[33m"
#define ANSI_BLUE    "\033[34m"
#define ANSI_MAGENTA "\033[35m"
#define ANSI_CYAN    "\033[36m"
#define ANSI_WHITE   "\033[37m"
#define ANSI_BOLD    "\033[1m"
#define ANSI_DIM     "\033[2m"
#define ANSI_RESET   "\033[0m"

typedef enum { CLIMB = 0, CRUISE = 1, DESCEND = 2 } Phase;

typedef struct {
    double lat;
    double lon;
    double alt;
} Pos3D;

typedef struct {
    double vx;
    double vy;
    double vz;
} Vel3D;

typedef struct {
    Phase phase;
    Pos3D start;
    Pos3D end;
} Segment;

typedef struct {
    double alt_m;
    double spd_kt;
    double rate_ms;
} ProfileEntry;

typedef struct {
    char id[ID_LEN];
    double fuel_kg;
    double cruise_kt;
    int n_seg;
    Segment seg[MAX_SEGMENTS];
    int n_climb;
    ProfileEntry climb[MAX_PROFILE];
    int n_desc;
    ProfileEntry desc[MAX_PROFILE];
    int departure_sec;     // UTC seconds from midnight
    int departure_tz;      // Timezone offset in minutes (e.g., -300 for EST, 60 for CET)
} FlightPlan;

typedef struct {
    int step;
    Pos3D pos;
    Vel3D vel;
    Phase phase;
} Snapshot;

typedef struct {
    int step;
    time_t ts;
    int fa, fb;
    Pos3D pa, pb;
    Vel3D va, vb;
    double h_m, v_m;
} Violation;

typedef struct {
    double wind_speed_kt;
    double wind_dir_deg;
    int step_updated;
} EnvironmentData;

typedef struct {
    double lat_north;
    double lat_south;
    double lon_west;
    double lon_east;
    double alt_ft_lo;
    double alt_ft_hi;
    double dir_deg;
    double speed_kt;
} WeatherZone;

typedef struct {
    char provider[64];
    double duration_hours;
    int n_zones;
    WeatherZone zones[MAX_WEATHER_ZONES];
} WeatherDataSet;

typedef struct {
    char id[ID_LEN];
    pid_t pid;
    Pos3D pos;
    Vel3D vel;
    Phase phase;
    int active;
    int in_area;
    int ever_in_area;
    int n_viol;
    int completed;
    int cur_seg;
    double wind_speed_kt;
    double wind_dir_deg;
    Snapshot history[MAX_HISTORY];
    int hist_count;
} FlightData;

typedef struct {
    sem_t *sem_step_start;
    sem_t *sem_step_done;

    volatile int running;
    volatile int current_step;
    int n_flights;
    int timestep;
    int sim_start_sec;

    FlightData flights[MAX_FLIGHTS];
    FlightPlan plans[MAX_FLIGHTS];

    Violation violations[MAX_VIOLATIONS];
    int n_violations;

    EnvironmentData env;

    pthread_mutex_t pos_mutex;
    pthread_mutex_t viol_mutex;
    pthread_mutex_t detect_mutex;
    pthread_mutex_t env_mutex;
    pthread_cond_t viol_cond;
    pthread_cond_t detect_cond;
    pthread_cond_t env_cond;
    volatile int step_ready;
    volatile int env_ready;

    int children_done;
    int children_active;
    int generate_report;
    int report_total_steps;
    int last_viol_step[MAX_FLIGHTS][MAX_FLIGHTS];
} SharedData;

#endif
