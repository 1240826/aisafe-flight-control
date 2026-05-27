#ifndef US105_INIT_H
#define US105_INIT_H

#include "common.h"
#include <pthread.h>

int init_hybrid_simulation(SharedData **out_shm, FlightPlan *plans, int n,
                           int sim_start_sec, int timestep,
                           pthread_t *out_detect_thr, pthread_t *out_report_thr,
                           pthread_t *out_env_thr);
void cleanup_shared_memory(SharedData *shm, int n_flights);

#endif
