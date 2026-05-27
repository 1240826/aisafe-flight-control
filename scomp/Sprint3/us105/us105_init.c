#include "us105_init.h"
#include "us108_sync.h"
#include "us106_threads.h"
#include "us107_report_notify.h"
#include "us110_env.h"
#include "physics.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <errno.h>

int init_hybrid_simulation(SharedData **out_shm, FlightPlan *plans, int n,
                           int sim_start_sec, int timestep,
                           pthread_t *out_detect_thr, pthread_t *out_report_thr,
                           pthread_t *out_env_thr)
{
    shm_unlink(SHM_NAME);
    int fd = shm_open(SHM_NAME, O_CREAT | O_RDWR, 0666);
    if (fd < 0) { perror("shm_open"); return -1; }

    if (ftruncate(fd, sizeof(SharedData)) < 0) { perror("ftruncate"); close(fd); return -1; }

    SharedData *shm = mmap(NULL, sizeof(SharedData), PROT_READ | PROT_WRITE,
                           MAP_SHARED, fd, 0);
    close(fd);
    if (shm == MAP_FAILED) { perror("mmap"); return -1; }

    memset(shm, 0, sizeof(SharedData));

    sem_unlink(SEM_START_NAME);
    sem_unlink(SEM_DONE_NAME);
    shm->sem_step_start = sem_open(SEM_START_NAME, O_CREAT, 0666, 0);
    shm->sem_step_done  = sem_open(SEM_DONE_NAME, O_CREAT, 0666, 0);
    if (shm->sem_step_start == SEM_FAILED || shm->sem_step_done == SEM_FAILED) {
        perror("sem_open"); munmap(shm, sizeof(SharedData)); return -1;
    }

    pthread_mutexattr_t mattr;
    pthread_mutexattr_init(&mattr);
    pthread_mutexattr_setpshared(&mattr, PTHREAD_PROCESS_SHARED);
    pthread_mutex_init(&shm->pos_mutex, &mattr);
    pthread_mutex_init(&shm->viol_mutex, &mattr);
    pthread_mutex_init(&shm->detect_mutex, &mattr);
    pthread_mutex_init(&shm->env_mutex, &mattr);
    pthread_mutexattr_destroy(&mattr);

    pthread_condattr_t cattr;
    pthread_condattr_init(&cattr);
    pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
    pthread_cond_init(&shm->viol_cond, &cattr);
    pthread_cond_init(&shm->detect_cond, &cattr);
    pthread_cond_init(&shm->env_cond, &cattr);
    pthread_condattr_destroy(&cattr);

    for (int i = 0; i < MAX_FLIGHTS; i++)
        for (int j = 0; j < MAX_FLIGHTS; j++)
            shm->last_viol_step[i][j] = -1;

    shm->n_flights = n;
    shm->timestep = timestep;
    shm->sim_start_sec = sim_start_sec;
    shm->running = 1;
    shm->step_ready = 0;
    shm->env_ready = 0;

    for (int i = 0; i < n; i++) {
        memcpy(&shm->plans[i], &plans[i], sizeof(FlightPlan));
        FlightData *fd = &shm->flights[i];
        strncpy(fd->id, plans[i].id, ID_LEN - 1);
        fd->id[ID_LEN - 1] = '\0';

        int offset = sim_start_sec - plans[i].departure_sec;
        if (offset < 0) {
            fd->pos = plans[i].seg[0].start;
            fd->phase = plans[i].seg[0].phase;
            memset(&fd->vel, 0, sizeof(Vel3D));
            fd->active = 0;
            fd->cur_seg = 0;
        } else {
            fast_forward_flight(&plans[i], offset, &fd->pos, &fd->vel,
                                &fd->phase, &fd->cur_seg);
            fd->active = 1;
            fd->in_area = in_area_full(fd->pos);
            fd->ever_in_area = fd->in_area;
        }
        fd->completed = 0;
        fd->n_viol = 0;
        fd->hist_count = 0;
    }

    pthread_create(out_detect_thr, NULL, violation_detector_thread, shm);
    pthread_create(out_report_thr, NULL, report_generator_thread, shm);
    pthread_create(out_env_thr, NULL, environment_thread, shm);

    for (int i = 0; i < n; i++) {
        pid_t pid = fork();
        if (pid < 0) { perror("fork"); shm->running = 0; return -1; }
        if (pid == 0) {
            run_flight_process(i, shm);
            exit(0);
        }
        shm->flights[i].pid = pid;
    }

    *out_shm = shm;
    return 0;
}

void cleanup_shared_memory(SharedData *shm, int n_flights)
{
    (void)n_flights;
    for (int i = 0; i < shm->n_flights; i++) {
        if (shm->flights[i].pid > 0) {
            kill(shm->flights[i].pid, SIGTERM);
            waitpid(shm->flights[i].pid, NULL, WNOHANG);
        }
    }

    pthread_mutex_destroy(&shm->pos_mutex);
    pthread_mutex_destroy(&shm->viol_mutex);
    pthread_mutex_destroy(&shm->detect_mutex);
    pthread_mutex_destroy(&shm->env_mutex);
    pthread_cond_destroy(&shm->viol_cond);
    pthread_cond_destroy(&shm->detect_cond);
    pthread_cond_destroy(&shm->env_cond);

    sem_close(shm->sem_step_start);
    sem_close(shm->sem_step_done);
    sem_unlink(SEM_START_NAME);
    sem_unlink(SEM_DONE_NAME);

    munmap(shm, sizeof(SharedData));
    shm_unlink(SHM_NAME);
}
