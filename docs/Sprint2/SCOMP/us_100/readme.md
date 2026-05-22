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

### Synchronization (Semaphores)

To enforce step-by-step synchronization (US108), the system requires named or unnamed semaphores:
- `sem_step_start`: Signals children to compute the next step.
- `sem_step_done`: Signals the parent that a child has finished its calculation and written to shared memory.

### LLM Assistance

Generative AI was used to support the analysis and design of this user story. Below are the main prompts used, the suggestions adopted, and the decisions the team made independently or where we deviated from the AI output.

---

#### Prompt 1 — Hybrid architecture using fork, pthreads, and shared memory

> "We are implementing a C simulation where a parent process spawns multiple threads for monitoring, and forks multiple child processes for flight simulation. They need to communicate via shared memory and synchronize via semaphores. How should we structure the initialization of the shared memory and semaphores before the forks?"

**LLM suggestions adopted:**
- Use `shm_open`, `ftruncate`, and `mmap` to allocate the shared memory block **before** calling `fork()` or `pthread_create()`, ensuring all entities inherit the mapped memory.
- Use POSIX unnamed semaphores (`sem_init` with the `pshared` flag set to 1) placed directly inside the mapped shared memory struct for easy access by all processes.

**Decisions made by the team / deviations from LLM output:**
- The LLM suggested using named semaphores (`sem_open`), but we decided to use unnamed semaphores (`sem_init` with `pshared = 1`) embedded directly in the shared memory struct to simplify cleanup and avoid leftover semaphore files in the OS (`/dev/shm`).

## 4. Design

```c
init_hybrid_simulation()
  // 1. Initialize Shared Memory
  shm_fd = shm_open("/sim_shm", O_CREAT | O_RDWR, 0666)
  ftruncate(shm_fd, sizeof(SharedData))
  shared_data = mmap(0, sizeof(SharedData), PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0)

  // 2. Initialize Semaphores and Mutexes in Shared Memory
  sem_init(&shared_data->sem_step_start, 1, 0)
  sem_init(&shared_data->sem_step_done, 1, 0)
  
  // 3. Spawn Parent Threads (Monitoring, Reporting, Environment)
  pthread_create(&safety_thread, NULL, safety_monitor_func, shared_data)
  pthread_create(&report_thread, NULL, report_gen_func, shared_data)

  // 4. Fork Child Processes (Flights)
  for i in 0..n:
    fork()
    if child:
      run_flight_process(i, shared_data)
      exit(0)
      
  // Parent continues as the simulation controller