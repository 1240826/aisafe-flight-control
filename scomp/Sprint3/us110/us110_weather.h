#ifndef US110_WEATHER_H
#define US110_WEATHER_H

#include "common.h"

int load_weather_from_json(const char *path, WeatherDataSet *out);

int get_wind_at(WeatherDataSet *wds, double lat, double lon, double alt_m,
                double *speed_kt, double *dir_deg);

#endif
