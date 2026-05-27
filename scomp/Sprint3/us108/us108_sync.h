#ifndef US108_SYNC_H
#define US108_SYNC_H

#include "common.h"

void run_flight_process(int idx, SharedData *shm);
int simulation_step(SharedData *shm);

#endif
