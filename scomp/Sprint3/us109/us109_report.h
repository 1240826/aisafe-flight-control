#ifndef US109_REPORT_H
#define US109_REPORT_H

#include "common.h"

extern char g_report_output_path[512];

void set_report_output_path(const char *path);
void write_report(SharedData *shm, int total_steps);

#endif
