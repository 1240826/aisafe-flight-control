#ifndef US110_ENV_H
#define US110_ENV_H

#include "common.h"

void env_set_weather_file(const char *path);
void *environment_thread(void *arg);

#endif
