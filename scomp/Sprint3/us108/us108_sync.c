#include "us108_sync.h"
#include "physics.h"
#include <string.h>
#include <math.h>
#include <errno.h>

static int sem_wait_retry(sem_t *sem) {
    int ret;
    do {
        ret = sem_wait(sem);
    } while (ret != 0 && errno == EINTR);
    return ret;
}

void run_flight_process(int idx, SharedData *shm)
{
    FlightPlan *plan = &shm->plans[idx];
    FlightData *fd = &shm->flights[idx];
    Pos3D pos = fd->pos;
    Vel3D vel = fd->vel;
    Phase phase = fd->phase;
    int started = fd->active;
    int cur_seg = fd->cur_seg;
    int flight_completed = 0;

    while (shm->running && !flight_completed) {
        if (sem_wait_retry(shm->sem_step_start) != 0) {
            break;
        }
        if (!shm->running) break;

        int current_time = shm->sim_start_sec + shm->current_step * shm->timestep;

        if (!started && current_time >= plan->departure_sec) {
            started = 1;
            pos = plan->seg[0].start;
            memset(&vel, 0, sizeof(Vel3D));
            phase = plan->seg[0].phase;
            cur_seg = 0;
        }

        if (started && cur_seg < plan->n_seg) {
            int done = advance(plan, &plan->seg[cur_seg], &pos, &vel, shm->timestep);

            double wind_speed = fd->wind_speed_kt;
            double wind_dir = fd->wind_dir_deg;
            if (wind_speed > 0.5) {
                double wind_rad = D2R(wind_dir);
                double wvx = -KT_TO_MS(wind_speed) * sin(wind_rad);
                double wvy = -KT_TO_MS(wind_speed) * cos(wind_rad);
                double dt = shm->timestep;
                double lat_mid = D2R(pos.lat);
                pos.lon += R2D((wvx * dt) / (cos(lat_mid) * EARTH_R));
                pos.lat += R2D((wvy * dt) / EARTH_R);
                vel.vx += wvx;
                vel.vy += wvy;
            }

            if (done) {
                cur_seg++;
                if (cur_seg >= plan->n_seg) {
                    flight_completed = 1;
                } else {
                    pos = plan->seg[cur_seg].start;
                    memset(&vel, 0, sizeof(Vel3D));
                    phase = plan->seg[cur_seg].phase;
                }
            }
        }

        pthread_mutex_lock(&shm->pos_mutex);
        fd->pos = pos;
        fd->vel = vel;
        fd->phase = phase;
        fd->cur_seg = cur_seg;
        fd->active = started && !flight_completed;
        fd->completed = flight_completed;
        fd->in_area = in_area_full(pos);
        if (fd->in_area) fd->ever_in_area = 1;
        if (fd->hist_count < MAX_HISTORY) {
            fd->history[fd->hist_count].step = shm->current_step;
            fd->history[fd->hist_count].pos = pos;
            fd->history[fd->hist_count].vel = vel;
            fd->history[fd->hist_count].phase = phase;
            fd->hist_count++;
        }
        pthread_mutex_unlock(&shm->pos_mutex);

        if (sem_post(shm->sem_step_done) != 0) {
            break;
        }
    }

    pthread_mutex_lock(&shm->pos_mutex);
    fd->active = 0;
    pthread_mutex_unlock(&shm->pos_mutex);
}

int simulation_step(SharedData *shm)
{
    int count = 0;
    for (int i = 0; i < shm->n_flights; i++) {
        if (!shm->flights[i].completed)
            count++;
    }

    if (count == 0) return 0;

    for (int i = 0; i < count; i++)
        sem_post(shm->sem_step_start);

    for (int i = 0; i < count; i++)
        sem_wait_retry(shm->sem_step_done);

    int remaining = 0;
    for (int i = 0; i < shm->n_flights; i++) {
        if (!shm->flights[i].completed)
            remaining++;
    }

    return remaining;
}
