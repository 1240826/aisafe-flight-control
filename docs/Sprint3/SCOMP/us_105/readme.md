# US105 – Initialize hybrid simulation environment with shared memory

## 1. Context

As a Flight Control Operator, I want to start the simulation with a multi-threaded parent process and multiple child flight processes communicating through a shared memory area, so that the system efficiently coordinates simulation data across processes.

**Assigned to:** Jaime Simões

### List of Issues

- Analysis: #70
- Design: #70
- Implement: #70
- Test: #70

---

## 2. Requirements

- The parent process spawns dedicated threads for its functionalities.
- Each flight is launched as an independent process.
- A shared memory segment is allocated and properly initialized for inter-process communication.
- Flight processes are configured to use semaphores for synchronization.
- This component must be implemented in C and must utilize threads, mutexes, condition variables, and signals.

## 3. Analysis

### Input

The simulation reads flight plan data (JSON) directly or receives processed DSL input. The key difference from Sprint 2 (US100) is the architectural shift from pipe-based communication to a hybrid model: processes for flights and threads for parent functionalities, all coordinated via POSIX Shared Memory (`shm_open`, `mmap`) and POSIX Semaphores (`sem_open`).

### Shared Memory Layout

Instead of sending updates via pipes, all flight data is written to a shared memory block accessible by the parent threads and all child processes.

| Data Structure | Responsibility |
|---|---|
| `SimulationState` | Contains global simulation time step and status flags. |
| `FlightData[N]` | Array storing the current position, altitude, and status of each flight. |
| `Mutex/CondVars`| Pthread mutexes and condition variables (stored in shared memory using `PTHREAD_PROCESS_SHARED` attribute if accessed across processes, or just used internally by parent threads). |

### Synchronization (Named Semaphores)

To enforce step-by-step synchronization (US108), the system uses named POSIX semaphores:
- `sem_step_start` (`/aisafe_start`): signals children to compute the next step.
- `sem_step_done` (`/aisafe_done`): signals the parent that a child has finished its calculation and written to shared memory.

Named semaphores (`sem_open`) are used instead of unnamed (`sem_init`) because they are reliably process-shared across forks on all POSIX systems. Unnamed semaphores with `pshared=1` are not consistently supported on all kernels (particularly on macOS/BSD), whereas named semaphores are portable and well-tested.

### LLM Assistance

Generative AI was used to support the analysis and design of this user story. Below are the main prompts used, the suggestions adopted, and the decisions the team made independently or where we deviated from the AI output.

---

#### Prompt 1 — Hybrid architecture using fork, pthreads, and shared memory

> "We are implementing a C simulation where a parent process spawns multiple threads for monitoring, and forks multiple child processes for flight simulation. They need to communicate via shared memory and synchronize via semaphores. How should we structure the initialization of the shared memory and semaphores before the forks?"

**LLM suggestions adopted:**
- Use `shm_open`, `ftruncate`, and `mmap` to allocate the shared memory block **before** calling `fork()` or `pthread_create()`, ensuring all entities inherit the mapped memory.

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested unnamed semaphores (`sem_init` with `pshared=1`) — we chose named semaphores (`sem_open`) instead, for portability and reliable process-sharing across forks. Named semaphores are the approach used in the classroom theory examples (Exercise 2 — fork + named semaphores).

## 4. Design

```c
init_hybrid_simulation()
  // 1. Initialize Shared Memory
  shm_fd = shm_open(SHM_NAME, O_CREAT | O_RDWR, 0666)
  ftruncate(shm_fd, sizeof(SharedData))
  shared_data = mmap(0, sizeof(SharedData), PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0)

  // 2. Initialize Named Semaphores (process-shared, initial value 0)
  sem_unlink(SEM_START_NAME)
  sem_unlink(SEM_DONE_NAME)
  shared_data->sem_step_start = sem_open(SEM_START_NAME, O_CREAT, 0666, 0)
  shared_data->sem_step_done  = sem_open(SEM_DONE_NAME, O_CREAT, 0666, 0)

  // 3. Initialize Process-Shared Mutexes and Condition Variables
  pthread_mutexattr_t mattr;
  pthread_mutexattr_setpshared(&mattr, PTHREAD_PROCESS_SHARED);
  pthread_mutex_init(&shared_data->pos_mutex, &mattr);
  pthread_mutex_init(&shared_data->viol_mutex, &mattr);
  pthread_mutex_init(&shared_data->detect_mutex, &mattr);
  pthread_mutex_init(&shared_data->env_mutex, &mattr);

  pthread_condattr_t cattr;
  pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
  pthread_cond_init(&shared_data->viol_cond, &cattr);
  pthread_cond_init(&shared_data->detect_cond, &cattr);
  pthread_cond_init(&shared_data->env_cond, &cattr);

  // 4. Spawn Parent Threads (Violation Detector, Report Generator, Environment)
  pthread_create(&detect_thr, NULL, violation_detector_thread, shared_data)
  pthread_create(&report_thr, NULL, report_generator_thread, shared_data)
  pthread_create(&env_thr, NULL, environment_thread, shared_data)

  // 5. Fork Child Processes (Flights)
  for i in 0..n:
    fork()
    if child:
      run_flight_process(i, shared_data)
      exit(0)
      
  // Parent continues as the simulation controller