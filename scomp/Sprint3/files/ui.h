#ifndef UI_H
#define UI_H

#include "common.h"

#ifndef TEST_DIR
#define TEST_DIR "../test"
#endif
#define SCENARIO_DIR TEST_DIR

void clear_screen(void);
void show_banner(void);
int show_menu(void);
int browse_scenarios(const char *dir, char *out_path, int max_len);
void draw_airspace(SharedData *shm, int step);
void show_summary(SharedData *shm, int total_steps);
void show_sim_status(SharedData *shm, int step, int active);

#endif
