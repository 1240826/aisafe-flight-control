#ifndef US106_THREADS_H
#define US106_THREADS_H

#include "common.h"

void *violation_detector_thread(void *arg);
void detect_initial_violations(SharedData *shm);

#endif
