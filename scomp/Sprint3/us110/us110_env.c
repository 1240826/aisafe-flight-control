#include "us110_env.h"
#include "us110_weather.h"
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <math.h>

static WeatherDataSet g_weather_data;
static int g_weather_loaded = 0;
static char g_weather_path[256] = "";

void env_set_weather_file(const char *path)
{
    strncpy(g_weather_path, path, sizeof(g_weather_path) - 1);
    g_weather_path[sizeof(g_weather_path) - 1] = '\0';
    g_weather_loaded = 0;
}

void *environment_thread(void *arg)
{
    SharedData *shm = (SharedData *)arg;
    char buf[512];

    if (g_weather_path[0] && !g_weather_loaded) {
        if (load_weather_from_json(g_weather_path, &g_weather_data) == 0) {
            g_weather_loaded = 1;
            snprintf(buf, sizeof(buf),
                "[ENV] Loaded %s: %d zones, %.2f hours\n",
                g_weather_data.provider, g_weather_data.n_zones,
                g_weather_data.duration_hours);
            write(STDERR_FILENO, buf, strlen(buf));
        } else {
            snprintf(buf, sizeof(buf),
                "[ENV] Failed to load weather from %s\n", g_weather_path);
            write(STDERR_FILENO, buf, strlen(buf));
        }
    }

    while (shm->running) {
        pthread_mutex_lock(&shm->env_mutex);
        while (!shm->env_ready && shm->running)
            pthread_cond_wait(&shm->env_cond, &shm->env_mutex);
        if (!shm->running) {
            pthread_mutex_unlock(&shm->env_mutex);
            break;
        }
        shm->env_ready = 0;
        pthread_mutex_unlock(&shm->env_mutex);

        int step = shm->current_step;

        pthread_mutex_lock(&shm->pos_mutex);
        for (int i = 0; i < shm->n_flights; i++) {
            FlightData *fd = &shm->flights[i];
            if (!fd->active && !fd->in_area) continue;
            double speed = 15.0, dir = 180.0;
            if (g_weather_loaded) {
                if (!get_wind_at(&g_weather_data, fd->pos.lat, fd->pos.lon,
                                 fd->pos.alt, &speed, &dir)) {
                    speed = 15.0;
                    dir = 180.0;
                }
            } else {
                double t = step * shm->timestep / 3600.0;
                speed = 15.0 + 12.0 * sin(t * 2.0 * 3.14159265 / 6.0);
                if (speed < 0.0) speed = 0.0;
                dir = 180.0 + 40.0 * sin(t * 2.0 * 3.14159265 / 4.0);
                if (dir < 0.0) dir += 360.0;
                if (dir >= 360.0) dir -= 360.0;
            }
            fd->wind_speed_kt = speed;
            fd->wind_dir_deg = dir;
        }

        int env_set = 0;
        for (int i = 0; i < shm->n_flights && !env_set; i++) {
            FlightData *fd = &shm->flights[i];
            if (fd->active && fd->in_area) {
                shm->env.wind_speed_kt = fd->wind_speed_kt;
                shm->env.wind_dir_deg = fd->wind_dir_deg;
                shm->env.step_updated = step;
                env_set = 1;
            }
        }
        pthread_mutex_unlock(&shm->pos_mutex);

        if (step % 600 == 0) {
            snprintf(buf, sizeof(buf),
                "[ENV] Step %d: wind %.1fkt from %.0f%s\n",
                step, shm->env.wind_speed_kt, shm->env.wind_dir_deg,
                g_weather_loaded ? " (file)" : " (synthetic)");
            write(STDERR_FILENO, buf, strlen(buf));
        }
    }

    pthread_exit(NULL);
}
