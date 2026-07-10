# SCOMP Sprint 3 — Guia de Defesa

---

## Índice

1. [Fundamentos Teóricos](#1-fundamentos-teóricos)
   - 1.1 Processos vs Threads
   - 1.2 POSIX Threads (pthreads)
   - 1.3 Mutexes (Mutual Exclusion)
   - 1.4 Condition Variables
   - 1.5 Semáforos
   - 1.6 Shared Memory (Memória Partilhada)
   - 1.7 Sinais (Signals)
   - 1.8 fork()
2. [Arquitetura do Sistema](#2-arquitetura-do-sistema)
3. [Estruturas de Dados (common.h)](#3-estruturas-de-dados-commonh)
4. [Walkthrough do Código — Todos os Ficheiros](#4-walkthrough-do-código)
   - 4.1 main.c
   - 4.2 us105_init.c
   - 4.3 us106_threads.c
   - 4.4 us107_report_notify.c
   - 4.5 us108_sync.c
   - 4.6 us109_report.c
   - 4.7 us110_env.c
   - 4.8 us110_weather.c
   - 4.9 physics.c
   - 4.10 ui.c
   - 4.11 json_parser.h
5. [Verificação US por US (Critérios de Aceitação)](#5-verificação-us-por-us)
6. [Ligações Cruzadas (Quem chama quem)](#6-ligações-cruzadas)
7. [Perguntas Prováveis do Professor](#7-perguntas-prováveis-do-professor)

---

## 1. Fundamentos Teóricos

### 1.1 Processos vs Threads

| | Processo | Thread |
|---|---|---|
| **Memória** | Espaço de endereçamento próprio (isolado) | Partilha o espaço do processo pai |
| **Criação** | `fork()` — cópia completa do processo pai | `pthread_create()` — partilha memória |
| **Comunicação** | IPC necessário (shared memory, pipes, sockets) | Acesso direto a variáveis globais |
| **Custo** | Pesado (cópia de memória, novo PID) | Leve (só stack novo) |
| **Scheduling** | Kernel schedule cada processo | Kernel schedule cada thread |
| **Independência** | Se um processo crasha, outros sobrevivem | Se uma thread crasha, processo morre |

**No nosso projeto:** O processo pai tem 3 threads (detetor, report, ambiente) porque partilham a mesma memória e precisam de comunicação rápida. Os voos são processos filhos (`fork()`) porque simulam entidades independentes — se um voo crashar, os outros continuam.

### 1.2 POSIX Threads (pthreads)

API standard IEEE 1003.1c para criar e gerir threads em C:

```c
#include <pthread.h>

// Criar thread
int pthread_create(pthread_t *thread, const pthread_attr_t *attr,
                   void *(*start_routine)(void*), void *arg);

// Esperar thread terminar
int pthread_join(pthread_t thread, void **retval);

// Sair da thread
void pthread_exit(void *retval);
```

**No nosso projeto:** `pthread_create` é chamado 3 vezes em `us105_init.c:94-96` para criar as threads do detetor, report e ambiente. `pthread_join` é chamado 3 vezes em `main.c:344-346` para esperar que todas terminem.

### 1.3 Mutexes (Mutual Exclusion)

Um mutex garante que **apenas uma thread de cada vez** acede a uma secção crítica (dados partilhados).

```c
pthread_mutex_t mutex;

// Inicialização
pthread_mutex_init(&mutex, NULL);              // entre threads do mesmo processo
pthread_mutex_init(&mutex, &attr_shared);      // entre processos diferentes (PTHREAD_PROCESS_SHARED)

// Lock e unlock
pthread_mutex_lock(&mutex);    // bloqueia se já estiver locked
pthread_mutex_unlock(&mutex);  // liberta
```

**Analogia:** Um mutex é como uma chave de uma casa de banho pública. Só uma pessoa entra de cada vez. As outras esperam.

**No nosso projeto:** Usamos **4 mutexes process-shared** (inicializados em `us105_init.c:41-48`):
| Mutex | Protege | Quem escreve | Quem lê |
|---|---|---|---|
| `pos_mutex` | Posições/velocidades dos voos | Filhos (flight process) | Detetor, Ambiente |
| `viol_mutex` | Fila de violações | Detetor | Report |
| `detect_mutex` | Flag `step_ready` | Pai (main loop) | Detetor |
| `env_mutex` | Flag `env_ready` | Pai (main loop) | Ambiente |

**Porquê 4 mutexes e não 1 só?** Para evitar contenção. O detetor pode estar a ler posições (`pos_mutex`) enquanto o report está a imprimir violações (`viol_mutex`). Com 1 mutex global, uma thread bloqueava a outra desnecessariamente.

### 1.4 Condition Variables

Permitem que uma thread **espere** até que uma condição seja verdadeira, e que outra thread **sinalize** quando essa condição muda.

```c
pthread_cond_t cond;

// Esperar (liberta o mutex enquanto espera, readquire quando acordar)
pthread_cond_wait(&cond, &mutex);
pthread_cond_timedwait(&cond, &mutex, &abstime);  // com timeout

// Sinalizar
pthread_cond_signal(&cond);   // acorda 1 thread
pthread_cond_broadcast(&cond); // acorda todas
```

**Padrão canónico:**
```c
// Thread A (consumidora)
pthread_mutex_lock(&mutex);
while (condição não satisfeita)
    pthread_cond_wait(&cond, &mutex);  // adormece, liberta mutex
// condição satisfeita — processa
pthread_mutex_unlock(&mutex);

// Thread B (produtora)
pthread_mutex_lock(&mutex);
// altera dados que satisfazem a condição
pthread_cond_signal(&cond);  // acorda thread A
pthread_mutex_unlock(&mutex);
```

**Analogia:** Uma condition variable é como um despertador. A thread A diz "acorda-me quando X acontecer" e adormece. A thread B faz X acontecer e toca o despertador.

**No nosso projeto:** Usamos 3 condition variables:
| Cond Var | Produtor | Consumidor | Propósito |
|---|---|---|---|
| `detect_cond` | Pai (main loop) | Detetor | "Filhos acabaram o step, podes detetar" |
| `viol_cond` | Detetor | Report | "Nova violação detetada, regista" |
| `env_cond` | Pai (main loop) | Ambiente | "Filhos acabaram o step, atribui vento" |

**Porquê `pthread_cond_timedwait` com 1s no report?** Para evitar deadlock no shutdown. Se usássemos `pthread_cond_wait` sem timeout e o detetor já tivesse terminado, o report ficava bloqueado para sempre. Com timeout de 1s, o report acorda periodicamente, verifica `shm->running` e sai limpo.

### 1.5 Semáforos

Contador atómico para sincronização entre processos/threads. Diferente de mutex — um mutex é binário (locked/unlocked), um semáforo pode ter valor N.

```c
#include <semaphore.h>

// Semáforos nomeados (entre processos)
sem_t *sem_open(const char *name, int oflag, mode_t mode, unsigned int value);
int sem_wait(sem_t *sem);   // decrementa; bloqueia se valor == 0
int sem_post(sem_t *sem);   // incrementa; desbloqueia waiters
int sem_close(sem_t *sem);
int sem_unlink(const char *name);

// Semáforos não-nomeados (entre threads do mesmo processo)
int sem_init(sem_t *sem, int pshared, unsigned int value);
```

**Analogia:** Um semáforo é como um restaurante com N mesas. `sem_wait` = ocupar uma mesa (esperar se estiver cheio). `sem_post` = libertar uma mesa.

**No nosso projeto:** Usamos **2 semáforos nomeados** (`/aisafe_start` e `/aisafe_done`) para a barreira de step. São nomeados (não `sem_init`) porque precisam de ser acedidos por processos diferentes (pai e filhos) após `fork()`. `sem_init` com `pshared=1` é problemático em alguns sistemas POSIX; `sem_open` é portável.

**Counting semaphore:** O pai faz `sem_post(start)` N vezes (N = número de voos ativos), e cada filho faz 1 `sem_wait(start)`. Depois cada filho faz 1 `sem_post(done)` e o pai faz N `sem_wait(done)`. Isto funciona como barreira: o pai não avança enquanto todos os filhos não terminarem o step atual.

**EINTR safety:** Se um sinal (ex: Ctrl+C → SIGINT) interromper `sem_wait()`, a chamada retorna -1 com `errno = EINTR` mas o semáforo **não foi decrementado**. Sem retry, o contador pai/filho dessincroniza. O nosso wrapper `sem_wait_retry()` resolve isto:
```c
do { ret = sem_wait(sem); } while (ret != 0 && errno == EINTR);
```

### 1.6 Shared Memory (Memória Partilhada)

Mecanismo POSIX para partilhar um bloco de memória entre processos independentes.

```c
#include <sys/mman.h>
#include <fcntl.h>

int fd = shm_open("/nome", O_CREAT | O_RDWR, 0666);  // cria/abre segmento
ftruncate(fd, tamanho);                                // define tamanho
void *ptr = mmap(NULL, tamanho, PROT_READ | PROT_WRITE,
                 MAP_SHARED, fd, 0);                   // mapeia para memória
close(fd);                                             // fd já não precisa
// ... usar ptr ...
munmap(ptr, tamanho);                                  // desmapeia
shm_unlink("/nome");                                   // remove segmento
```

**`MAP_SHARED`** é crucial — as escritas são visíveis imediatamente por todos os processos que mapearam o mesmo segmento. `MAP_PRIVATE` faria copy-on-write.

**No nosso projeto:** Todo o estado da simulação está num único segmento de shared memory (`/aisafe_sim_v3`). O struct `SharedData` contém:
- Arrays de voos, planos, violações
- Mutexes e condition variables (process-shared)
- Semáforos (ponteiros, não os objetos em si)
- Flags de controlo

### 1.7 Sinais (Signals)

Mecanismo para notificar processos de eventos assíncronos.

```c
#include <signal.h>

void handler(int sig) { /* tratar sinal */ }

struct sigaction act;
act.sa_handler = handler;
sigemptyset(&act.sa_mask);
act.sa_flags = SA_RESTART;  // reinicia chamadas interrompidas
sigaction(SIGINT, &act, NULL);
```

**`SA_RESTART`** faz com que chamadas bloqueantes (como `read()`, `sem_wait()`) sejam reiniciadas automaticamente em vez de devolverem `EINTR`.

**No nosso projeto:** O handler de `SIGINT` (`main.c:36-40`) define `shm->running = 0` para parar a simulação de forma limpa. O `SA_RESTART` é usado para reduzir interrupções, mas o `sem_wait_retry()` é uma camada extra de segurança.

### 1.8 fork()

Cria um processo filho que é uma cópia quase exata do pai.

```c
pid_t pid = fork();
if (pid == 0) {
    // Código do FILHO
    exit(0);
} else if (pid > 0) {
    // Código do PAI — pid é o PID do filho
} else {
    // Erro
}
```

**O que é copiado:** O espaço de endereçamento inteiro (código, dados, heap, stack), file descriptors, sinais. O mapeamento de shared memory (`mmap MAP_SHARED`) **não é copiado** — pai e filho veem a mesma memória física.

**No nosso projeto:** `fork()` é chamado N vezes em `us105_init.c:98-106`. Cada filho executa `run_flight_process(i, shm)` e partilha o `SharedData*` com o pai e com os outros filhos.

---

## 2. Arquitetura do Sistema

```
                    ┌─────────── PROCESSO PAI ───────────┐
                    │                                      │
  scenario.json ──► main()                                │
                    │   │                                  │
                    │   ├─► load_plans()                   │
                    │   │                                  │
                    │   └─► init_hybrid_simulation()       │
                    │        │                             │
                    │        ├─ shm_open + mmap ──┐        │
                    │        ├─ sem_open × 2       │        │
                    │        ├─ mutex_init × 4     │   SHARED MEMORY
                    │        ├─ cond_init × 3      ├──── SharedData
                    │        ├─ pthread_create × 3  │    ├ flights[]
                    │        └─ fork() × N          │    ├ plans[]
                    │              │                │    ├ violations[]
                    │              │                │    ├ env
                    │         ┌────┴────┐           │    ├ sem_step_start
                    │         │  FILHOS  │           │    ├ sem_step_done
                    │         │ child 0  │───────────┘    ├ pos_mutex
                    │         │ child 1  │────────────────┤ viol_mutex
                    │         │  ...     │                ├ detect_mutex
                    │         │ child N-1│                ├ env_mutex
                    │         └─────────┘                ├ viol_cond
                    │                                      ├ detect_cond
                    │  ┌──────────────┐                    ├ env_cond
                    │  │ DETECTOR     │────────────────────┤ step_ready
                    │  │ (US106)      │                    ├ env_ready
                    │  └──────┬───────┘                    ├ running
                    │         │ viol_cond                   └ current_step
                    │         ▼                           
                    │  ┌──────────────┐
                    │  │ REPORT       │
                    │  │ (US107)      │──► write_report() ──► ficheiro .txt
                    │  └──────────────┘
                    │
                    │  ┌──────────────┐
                    │  │ ENVIRONMENT  │
                    │  │ (US110)      │──► JSON weather ──► vento por voo
                    │  └──────────────┘
                    └──────────────────────────────────────┘
```

**Números:**
- **1 processo pai** com **4 threads** (main + detetor + report + ambiente)
- **N processos filho** (1 por voo, máximo 10)
- **1 segmento** de shared memory
- **4 mutexes** process-shared
- **3 condition variables** process-shared
- **2 semáforos** nomeados

**Porquê processos filho em vez de threads para os voos?**
- Cada voo é uma entidade independente
- Se um voo tiver um bug, não derruba os outros
- Demonstra o uso de IPC com `fork()` + shared memory (requisito do enunciado)
- O isolamento de memória entre voos é mais realista

---

## 3. Estruturas de Dados (common.h)

### SharedData — O bloco de shared memory

```c
typedef struct {
    // Semáforos (ponteiros para named semaphores)
    sem_t *sem_step_start;        // "/aisafe_start"
    sem_t *sem_step_done;         // "/aisafe_done"

    // Controlo
    volatile int running;         // 1=enquanto simulacao ativa
    volatile int current_step;    // step atual (0, 1, 2, ...)
    int n_flights;                // número de voos
    int timestep;                 // 1 segundo
    int sim_start_sec;            // hora de início em segundos UTC

    // Dados dos voos
    FlightData flights[MAX_FLIGHTS];   // estado atual de cada voo (pos, vel, etc.)
    FlightPlan plans[MAX_FLIGHTS];     // planos de voo (segmentos, partida, etc.)

    // Fila de violações
    Violation violations[MAX_VIOLATIONS];  // array circular de violações
    int n_violations;                      // contador de violações

    // Ambiente
    EnvironmentData env;            // vento atual para display

    // Mutexes (todos PTHREAD_PROCESS_SHARED)
    pthread_mutex_t pos_mutex;      // protege flights[].pos/vel/phase
    pthread_mutex_t viol_mutex;     // protege violations[] / n_violations
    pthread_mutex_t detect_mutex;   // protege step_ready
    pthread_mutex_t env_mutex;      // protege env_ready

    // Condition Variables
    pthread_cond_t viol_cond;       // detetor → report
    pthread_cond_t detect_cond;     // main loop → detetor
    pthread_cond_t env_cond;        // main loop → ambiente

    // Flags de sincronização
    volatile int step_ready;        // 1 = filhos terminaram step
    volatile int env_ready;         // 1 = hora de atualizar vento

    // Controlo de fim
    int children_done;
    int children_active;
    int generate_report;            // 1 = escrever ficheiro no fim
    int report_total_steps;         // total de steps executados

    // Tracking de violações
    int last_viol_step[MAX_FLIGHTS][MAX_FLIGHTS];  // matriz: último step com violação
} SharedData;
```

### FlightData — Estado de um voo

```c
typedef struct {
    char id[ID_LEN];              // ex: "TP001"
    pid_t pid;                    // PID do processo filho
    Pos3D pos;                    // posição atual (lat, lon, alt)
    Vel3D vel;                    // velocidade atual (vx, vy, vz) em m/s
    Phase phase;                  // CLIMB, CRUISE, ou DESCEND
    int active;                   // 1 = voo já partiu
    int in_area;                  // 1 = dentro da área monitorizada
    int ever_in_area;             // 1 = já esteve na área
    int n_viol;                   // contador de violações deste voo
    int completed;                // 1 = voo terminou a rota
    int cur_seg;                  // índice do segmento atual
    double wind_speed_kt;         // vento atribuído pela thread ambiente
    double wind_dir_deg;          // direção do vento
    Snapshot history[MAX_HISTORY]; // histórico para trail no UI
    int hist_count;               // nº de snapshots guardados
} FlightData;
```

### FlightPlan — Plano de voo (vem do JSON)

```c
typedef struct {
    char id[ID_LEN];
    double fuel_kg;
    double cruise_kt;             // velocidade de cruzeiro em knots
    int n_seg;                    // número de segmentos
    Segment seg[MAX_SEGMENTS];    // array de segmentos
    int n_climb;                  // entradas na tabela de subida
    ProfileEntry climb[MAX_PROFILE];
    int n_desc;                   // entradas na tabela de descida
    ProfileEntry desc[MAX_PROFILE];
    int departure_sec;            // hora de partida em segundos UTC
    int departure_tz;             // timezone offset em minutos
} FlightPlan;
```

### Violation — Uma violação de segurança

```c
typedef struct {
    int step;           // step em que ocorreu
    time_t ts;          // timestamp UNIX
    int fa, fb;         // índices dos voos envolvidos
    Pos3D pa, pb;       // posições no momento da violação
    Vel3D va, vb;       // velocidades no momento
    double h_m, v_m;    // distâncias horizontal e vertical
} Violation;
```

### Constantes principais

```c
#define MAX_FLIGHTS      10       // máximo de voos em simultâneo
#define MAX_SEGMENTS     20       // máximo de segmentos por voo
#define MAX_VIOLATIONS   10800    // capacidade da fila de violações
#define MAX_HISTORY      600      // snapshots de histórico por voo

#define TIMESTEP_S       1        // 1 segundo por step
#define SAFETY_H_M       9260.0   // 5 NM (ICAO)
#define SAFETY_V_M       305.0    // 1000 ft (RVSM)
#define SAFETY_CAP_S     28800    // limite máximo: 8 horas

#define AREA_LAT_MIN     38.0     // Sul da Península Ibérica
#define AREA_LAT_MAX     44.0     // Norte
#define AREA_LON_MIN    -10.0     // Oeste
#define AREA_LON_MAX     -2.0     // Este
#define AREA_MAX_ALT_M   14000.0  // altitude máxima monitorizada

#define SHM_NAME         "/aisafe_sim_v3"
#define SEM_START_NAME   "/aisafe_start"
#define SEM_DONE_NAME    "/aisafe_done"
```

---

## 4. Walkthrough do Código

### 4.1 main.c — Ponto de entrada e orquestrador

**Função:** Ponto de entrada do programa. Menu interativo, parsing de JSON, loop de simulação, servidor TCP.

**Linhas 1-33 — Includes e variáveis globais:**
```c
#include "json_parser.h"  // parser JSON minimalista (header-only)
#include "us105_init.h"   // init_hybrid_simulation, cleanup_shared_memory
#include "us106_threads.h"// violation_detector_thread, detect_initial_violations
#include "us108_sync.h"   // simulation_step, run_flight_process
#include "us109_report.h" // write_report, set_report_output_path
#include "us110_env.h"    // env_set_weather_file
#include "ui.h"           // draw_airspace, show_summary, menus

static SharedData *shm = NULL;          // ponteiro para shared memory
static FlightPlan plans[MAX_FLIGHTS];   // planos carregados do JSON
static int n_plans = 0;                // número de planos carregados
static int sim_start_sec = 8*3600+30*60; // 08:30 UTC (hora padrão)
static int print_interval = 10;         // intervalo de impressão do mapa
static volatile sig_atomic_t stop_sim = 0; // flag para Ctrl+C
```

**Linhas 36-40 — SIGINT handler:**
```c
static void handle_sigint(int sig) {
    stop_sim = 1;
    if (shm) shm->running = 0;  // desliga todos os loops
}
```
Quando o utilizador prime Ctrl+C, todas as threads e processos veem `running = 0` e terminam.

**Linhas 42-55 — jp_obj() — extrai objeto JSON:**
Procura uma chave no JSON, localiza `{...}`, devolve a substring. É chamado pelo parser de planos e segmentos.

**Linhas 57-68 — jp_pos() — extrai posição 3D:**
Extrai Latitude, Longitude e Altitude de um objeto JSON.

**Linhas 70-94 — jp_profile() — extrai tabela de perfil:**
Extrai tabelas Climb/Descend do JSON. Cada entrada tem Altitude, Speed e RateClimb/RateDescent. Usa `jp_dbl()` para extrair valores numéricos.

**Linhas 96-121 — jp_segments() — extrai segmentos da rota:**
Extrai array de segmentos do JSON. Cada segmento tem:
- `Mode`: "climb" → CLIMB, "descend" → DESCEND, outro → CRUISE
- `Start` / `End`: posições 3D

**Linhas 123-210 — jp_plan() — extrai um plano de voo completo:**
Extrai de um objeto JSON todos os campos de um voo:
1. ID do voo
2. DepartureTime + DepartureTZ → converte para `departure_sec` (UTC)
3. Fuel
4. Flight Profile (Climb, Descend, Cruise speed)
5. Segments

**Linhas 212-254 — load_plans() — carrega cenário JSON:**
Abre o ficheiro, lê para memória, deteta se é array `[...]` ou objeto `{...}`, chama `jp_plan()` para cada voo.

**Linhas 256-358 — run_simulation() — O CORAÇÃO DO SISTEMA:**

```
Passo 1: Configurar weather
   if (weather_path[0]) env_set_weather_file(weather_path);

Passo 2: Inicializar (US105)
   init_hybrid_simulation(&shm, plans, n_plans, ...);
   → cria shared memory, 4 mutexes, 3 cond vars, 2 sems
   → cria 3 threads (detetor, report, ambiente)
   → faz fork de N filhos (1 por voo)

Passo 3: Detetar violações iniciais
   detect_initial_violations(shm);
   → voos fast-forwarded podem já estar em conflito

Passo 4: Loop principal
   for (step = 0; step < safety_cap; step++) {
       shm->current_step = step;

       // BARREIRA: liberta filhos e espera todos terminarem
       int remaining = simulation_step(shm);
       if (remaining == 0) break;  // todos os voos completaram

       // Acorda o DETETOR
       shm->step_ready = 1;
       pthread_cond_signal(&shm->detect_cond);

       // Acorda o AMBIENTE
       shm->env_ready = 1;
       pthread_cond_signal(&shm->env_cond);

       // Desenha mapa a cada print_interval
       if (step % print_interval == 0)
           draw_airspace(shm, step);
   }

Passo 5: Fim da simulação
   shm->generate_report = 1;     // manda o report thread gerar ficheiro
   shm->running = 0;              // sinaliza todas as threads para parar

Passo 6: Parar filhos
   sem_post(sem_step_start) × N;  // desbloqueia filhos presos no sem_wait
   waitpid() × N;                 // espera cada filho terminar

Passo 7: Parar threads
   sinaliza detect_cond, env_cond, viol_cond  // acorda threads para verificarem running==0
   pthread_join() × 3;                         // espera cada thread terminar

Passo 8: Cleanup
   show_summary();               // ecrã final com PASS/FAIL
   cleanup_shared_memory();      // destrói tudo, unlink shm/sems
```

**Linhas 360-380 — read_all/write_all — leitura/escrita fiável em sockets:**
Funções auxiliares para o modo servidor TCP — leem/escrevem N bytes garantidamente (socket pode fragmentar).

**Linhas 382-512 — run_server() — Modo servidor TCP:**
- Cria socket TCP, bind, listen
- Aceita conexão, lê 4 bytes (tamanho), depois N bytes (JSON)
- Guarda JSON em ficheiro temporário
- Carrega planos, executa `run_simulation()`
- Lê ficheiro de report, envia de volta com prefixo de tamanho
- **NOTA:** Este modo é para LAPR testar flight plans. Não faz parte dos US do SCOMP.

**Linhas 514-805 — main() — Menu interativo:**
- Regista handler de SIGINT
- Se `argv[1] == "--server"` → modo servidor TCP
- Se `argv[1]` é ficheiro → modo linha de comandos
- Senão → menu interativo com 8 opções:
  1. Load Scenario (browser de ficheiros)
  2. Set Start Time
  3. Set Print Interval
  4. Start Simulation → chama `run_simulation()`
  5. Display Flight Summary (pré-simulação)
  6. Select Weather Provider (CW / HP / Synthetic / None)
  7. Run Demo (13 cenários pré-definidos)
  8. Exit

---

### 4.2 us105_init.c — Inicialização híbrida

**Função:** Criar shared memory, inicializar todos os mecanismos IPC, criar threads e processos.

**Linhas 15-18 — Assinatura:**
```c
int init_hybrid_simulation(SharedData **out_shm, FlightPlan *plans, int n,
                           int sim_start_sec, int timestep,
                           pthread_t *out_detect_thr, pthread_t *out_report_thr,
                           pthread_t *out_env_thr)
```
Recebe os planos de voo e devolve:
- `out_shm` — ponteiro para a shared memory
- `out_detect_thr`, `out_report_thr`, `out_env_thr` — IDs das threads (para `pthread_join` depois)

**Linhas 20-29 — Shared Memory:**
```c
shm_unlink(SHM_NAME);                     // remove segmento anterior (se existir)
int fd = shm_open(SHM_NAME, O_CREAT|O_RDWR, 0666); // cria/abre
ftruncate(fd, sizeof(SharedData));        // aloca espaço
SharedData *shm = mmap(NULL, sizeof(SharedData),
                       PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0); // mapeia
close(fd);                                // fd já não é necessário
memset(shm, 0, sizeof(SharedData));       // zera tudo
```
Ordem: unlink primeiro (limpa), shm_open, ftruncate, mmap. `MAP_SHARED` é essencial.

**Linhas 33-39 — Semáforos nomeados:**
```c
sem_unlink(SEM_START_NAME);    // limpa anteriores
sem_unlink(SEM_DONE_NAME);
shm->sem_step_start = sem_open(SEM_START_NAME, O_CREAT, 0666, 0); // valor inicial 0
shm->sem_step_done  = sem_open(SEM_DONE_NAME,  O_CREAT, 0666, 0);
```
Valor inicial 0 — os filhos bloqueiam no primeiro `sem_wait` até o pai fazer `sem_post`.

**Linhas 41-48 — Mutexes process-shared:**
```c
pthread_mutexattr_t mattr;
pthread_mutexattr_init(&mattr);
pthread_mutexattr_setpshared(&mattr, PTHREAD_PROCESS_SHARED);  // CRUCIAL
pthread_mutex_init(&shm->pos_mutex, &mattr);
pthread_mutex_init(&shm->viol_mutex, &mattr);
pthread_mutex_init(&shm->detect_mutex, &mattr);
pthread_mutex_init(&shm->env_mutex, &mattr);
pthread_mutexattr_destroy(&mattr);
```
Sem `PTHREAD_PROCESS_SHARED`, os mutexes só funcionam entre threads do mesmo processo. Com este atributo, funcionam entre o pai, os filhos, e as threads.

**Linhas 50-56 — Condition variables process-shared:**
```c
pthread_condattr_t cattr;
pthread_condattr_init(&cattr);
pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
pthread_cond_init(&shm->viol_cond, &cattr);
pthread_cond_init(&shm->detect_cond, &cattr);
pthread_cond_init(&shm->env_cond, &cattr);
pthread_condattr_destroy(&cattr);
```

**Linhas 58-68 — Inicialização de arrays e flags:**
- `last_viol_step[][]` = -1 (sem violações)
- `n_flights = n`
- `running = 1`
- `step_ready = 0`, `env_ready = 0`

**Linhas 69-92 — Cópia de planos e fast-forward:**
```c
for (int i = 0; i < n; i++) {
    memcpy(&shm->plans[i], &plans[i], sizeof(FlightPlan));  // copia plano para shm
    strncpy(fd->id, plans[i].id, ID_LEN - 1);

    int offset = sim_start_sec - plans[i].departure_sec;
    if (offset < 0) {
        // Voo ainda não partiu
        fd->pos = plans[i].seg[0].start;
        fd->active = 0;  // WAIT
    } else {
        // Voo já partiu — avança até ao momento atual
        fast_forward_flight(&plans[i], offset, &fd->pos, &fd->vel,
                            &fd->phase, &fd->cur_seg);
        fd->active = 1;  // já está em rota
    }
}
```

**Linhas 94-96 — Criação das 3 threads:**
```c
pthread_create(out_detect_thr, NULL, violation_detector_thread, shm);
pthread_create(out_report_thr, NULL, report_generator_thread, shm);
pthread_create(out_env_thr,    NULL, environment_thread, shm);
```
O argumento `shm` é passado a cada thread. Todas partilham o mesmo ponteiro.

**Linhas 98-106 — Criação dos N processos filho:**
```c
for (int i = 0; i < n; i++) {
    pid_t pid = fork();
    if (pid == 0) {
        run_flight_process(i, shm);  // FILHO: simula voo
        exit(0);
    }
    shm->flights[i].pid = pid;       // PAI: guarda PID
}
```

**Linhas 112-137 — cleanup_shared_memory():**
Ordem de limpeza:
1. `kill(SIGTERM)` para cada filho (caso ainda estejam vivos)
2. `waitpid(WNOHANG)` para recolher zombies
3. `pthread_mutex_destroy()` × 4
4. `pthread_cond_destroy()` × 3
5. `sem_close()` × 2 + `sem_unlink()` × 2
6. `munmap()` + `shm_unlink()`

---

### 4.3 us106_threads.c — Thread Detetora de Violações

**Função:** Uma thread que a cada step verifica todos os pares de voos para detectar violações de segurança.

**Linhas 8-71 — violation_detector_thread():**
```c
void *violation_detector_thread(void *arg) {
    SharedData *shm = (SharedData *)arg;

    while (shm->running) {
        // PASSO 1: Esperar o pai sinalizar que os filhos terminaram
        pthread_mutex_lock(&shm->detect_mutex);
        while (!shm->step_ready && shm->running)
            pthread_cond_wait(&shm->detect_cond, &shm->detect_mutex);
        if (!shm->running) break;
        shm->step_ready = 0;  // reset
        pthread_mutex_unlock(&shm->detect_mutex);

        // PASSO 2: Varrer todos os pares de voos
        pthread_mutex_lock(&shm->pos_mutex);
        for (int i = 0; i < shm->n_flights; i++) {
            if (fa->completed || (!fa->active && !fa->in_area)) continue;
            for (int j = i + 1; j < shm->n_flights; j++) {
                if (fb->completed || (!fb->active && !fb->in_area)) continue;

                // PASSO 3: Verificar safety breach
                double h_m, v_m;
                if (safety_breach(fa->pos, fb->pos, &h_m, &v_m)) {
                    // PASSO 4: Registar violação na shared memory
                    pthread_mutex_lock(&shm->viol_mutex);
                    Violation *v = &shm->violations[shm->n_violations++];
                    v->step = step; v->ts = time(NULL);
                    v->fa = i; v->fb = j;
                    v->pa = fa->pos; v->pb = fb->pos;
                    v->va = fa->vel; v->vb = fb->vel;
                    v->h_m = h_m; v->v_m = v_m;
                    fa->n_viol++; fb->n_viol++;

                    // PASSO 5: Sinalizar o report thread
                    pthread_cond_signal(&shm->viol_cond);
                    pthread_mutex_unlock(&shm->viol_mutex);
                }
            }
        }
        pthread_mutex_unlock(&shm->pos_mutex);
    }
    pthread_exit(NULL);
}
```

**Padrão de locking:**
1. `detect_mutex` + `detect_cond` — espera pelo sinal do main loop
2. `pos_mutex` — protege a leitura das posições (os filhos escrevem posições sob o mesmo mutex em `us108_sync.c:71-87`)
3. `viol_mutex` — protege a escrita na fila de violações (o report lê sob o mesmo mutex)

**Linhas 73-115 — detect_initial_violations():**
Igual ao detetor, mas chamado **uma vez** antes do loop principal. Detecta violações que existem nas posições iniciais (após fast-forward). Não usa `detect_cond` porque é chamada diretamente pelo main.

---

### 4.4 us107_report_notify.c — Thread de Report

**Função:** Espera notificações de violações, imprime em tempo real, gera ficheiro no final.

**Linhas 10-49 — report_generator_thread():**
```c
void *report_generator_thread(void *arg) {
    SharedData *shm = (SharedData *)arg;
    int last_printed = -1;  // índice da última violação já impressa

    while (shm->running) {
        // TIMEOUT de 1 segundo
        struct timespec ts;
        clock_gettime(CLOCK_REALTIME, &ts);
        ts.tv_sec += 1;

        // Espera com timeout
        pthread_mutex_lock(&shm->viol_mutex);
        while (shm->n_violations <= last_printed + 1 && shm->running)
            pthread_cond_timedwait(&shm->viol_cond, &shm->viol_mutex, &ts);

        if (!shm->running) break;

        // Imprime novas violações
        while (last_printed + 1 < shm->n_violations) {
            last_printed++;
            Violation *v = &shm->violations[last_printed];
            fprintf(stderr, "[REPORT] Step %d: %s <-> %s  h_dist=%.0fm  v_dist=%.0fm\n",
                    v->step, fa->id, fb->id, v->h_m, v->v_m);
        }
        pthread_mutex_unlock(&shm->viol_mutex);
    }

    // Gera ficheiro final
    if (shm->generate_report)
        write_report(shm, shm->report_total_steps);
}
```

**Porquê `pthread_cond_timedwait` e não `pthread_cond_wait`?**
O `timedwait` com 1s garante que a thread verifica `shm->running` pelo menos a cada segundo. Quando a simulação acaba (`running = 0`), a thread sai no máximo 1s depois. Com `pthread_cond_wait` sem timeout, se o detetor já tiver terminado e não houver mais violações, a thread ficava bloqueada para sempre.

---

### 4.5 us108_sync.c — Sincronização de Step (US108)

**Função:** Barreira de semáforos que garante lockstep entre pai e todos os filhos.

**Linhas 7-13 — sem_wait_retry():**
```c
static int sem_wait_retry(sem_t *sem) {
    int ret;
    do { ret = sem_wait(sem); } while (ret != 0 && errno == EINTR);
    return ret;
}
```
Wrapper que repete `sem_wait` se for interrompido por um sinal (`EINTR`). Sem isto, um Ctrl+C durante `sem_wait` dessincroniza os contadores.

**Linhas 15-97 — run_flight_process() — O loop de cada filho:**

```
Estrutura:
  1. Inicializa variáveis locais (pos, vel, phase, started, cur_seg)
     a partir do que o US105 calculou (fast-forward ou posição inicial)

  2. Loop principal:
     a) sem_wait(sem_step_start)        → espera ordem do pai
     b) Verifica running
     c) Se current_time >= departure_sec → ATIVA o voo (só acontece 1 vez)
     d) advance(plan, seg, &pos, &vel, 1s) → move 1 segundo na rota
        - CLIMB:   altitude sobe, velocidade da tabela climb
        - CRUISE:  altitude fixa, velocidade = cruise_kt
        - DESCEND: altitude desce, velocidade da tabela descend
     e) Se wind_speed > 0.5 kt → APLICA WIND DRIFT:
        wind_rad = D2R(wind_dir)
        wvx = -KT_TO_MS(speed) * sin(wind_rad)
        wvy = -KT_TO_MS(speed) * cos(wind_rad)
        pos.lon += R2D(wvx * dt / (cos(lat_mid) * EARTH_R))
        pos.lat += R2D(wvy * dt / EARTH_R)
        vel.vx += wvx; vel.vy += wvy
        → A posição e velocidade GROUND são afetadas
        → Airspeed e heading NÃO mudam
     f) Se advance() retornou done → próximo segmento
        Se era o último segmento → flight_completed = 1
     g) Sob pos_mutex, escreve pos/vel/phase/status na shared memory
        Guarda snapshot no histórico (para trail no UI)
     h) sem_post(sem_step_done) → avisa o pai

  3. No final, desativa o voo (active = 0) sob pos_mutex
```

**Linhas 99-122 — simulation_step() — O lado do pai:**
```c
int simulation_step(SharedData *shm) {
    // Conta voos ativos
    int count = 0;
    for (int i = 0; i < shm->n_flights; i++)
        if (!shm->flights[i].completed) count++;
    if (count == 0) return 0;  // todos completaram

    // Liberta N filhos (counting semaphore)
    for (int i = 0; i < count; i++)
        sem_post(shm->sem_step_start);

    // Espera N filhos terminarem
    for (int i = 0; i < count; i++)
        sem_wait_retry(shm->sem_step_done);

    // Conta quantos ainda faltam
    int remaining = 0;
    for (int i = 0; i < shm->n_flights; i++)
        if (!shm->flights[i].completed) remaining++;
    return remaining;
}
```

**Garantia de lockstep:** O pai faz `sem_wait(done)` N vezes. Só retorna quando os N filhos fizeram `sem_post(done)`. Portanto, **todos os filhos terminam o step t antes de o pai começar o step t+1.**

---

### 4.6 us109_report.c — Geração de Relatório

**Função:** Escrever o ficheiro de relatório final com todas as estatísticas.

**Linhas 8-16 — set_report_output_path():**
Define o caminho do ficheiro de output. Se não for chamada, usa o nome timestamped por omissão.

**Linhas 18-80 — write_report():**
```c
void write_report(SharedData *shm, int total_steps) {
    // 1. Determinar nome do ficheiro
    //    Se g_report_output_path definido → usa esse
    //    Senão → "report_YYYYMMDD_HHMMSS.txt"

    // 2. Abrir ficheiro
    FILE *f = fopen(path, "w");

    // 3. Cabeçalho
    //    ============================================
    //      AISafe Simulation Report
    //      Generated: <data>
    //      Total steps: N  (N seconds simulated)
    //      Flights: N
    //      Total violations detected: N
    //    ============================================

    // 4. Flight Summary
    //    Para cada voo: ID, n_viol, ever_in_area, completed

    // 5. Violation Log (se houver violações)
    //    Para cada violação: #N step=X  ID1 <-> ID2
    //      pos_a=(lat, lon, alt)  pos_b=(lat, lon, alt)
    //      h_dist=...m  v_dist=...m

    // 6. Veredito final
    //    RESULT: PASS (se 0 violações) ou FAIL

    // 7. Fechar ficheiro
}
```

**Quem chama write_report()?** A thread de report (`us107_report_notify.c:45`), quando `shm->generate_report == 1`. Esta flag é posta a 1 pelo main loop após o fim da simulação (`main.c:319`).

---

### 4.7 us110_env.c — Thread de Ambiente

**Função:** Atribuir vento a cada voo em cada step. Carrega dados de ficheiros JSON de weather.

**Linhas 8-17 — Variáveis globais e env_set_weather_file():**
```c
static WeatherDataSet g_weather_data;  // dados carregados do JSON
static int g_weather_loaded = 0;       // 1 = ficheiro carregado com sucesso
static char g_weather_path[256];       // caminho do ficheiro

void env_set_weather_file(const char *path) {
    strncpy(g_weather_path, path, ...);
    g_weather_loaded = 0;  // força recarregar
}
```
Chamada pelo main antes de `run_simulation()`.

**Linhas 19-97 — environment_thread():**
```c
void *environment_thread(void *arg) {
    SharedData *shm = (SharedData *)arg;

    // 1. Carregar ficheiro weather (uma vez, no arranque)
    if (g_weather_path[0] && !g_weather_loaded) {
        if (load_weather_from_json(g_weather_path, &g_weather_data) == 0)
            g_weather_loaded = 1;
    }

    // 2. Loop principal
    while (shm->running) {
        // Esperar sinal do main loop (env_cond)
        pthread_mutex_lock(&shm->env_mutex);
        while (!shm->env_ready && shm->running)
            pthread_cond_wait(&shm->env_cond, &shm->env_mutex);
        shm->env_ready = 0;
        pthread_mutex_unlock(&shm->env_mutex);

        // 3. Atribuir vento a cada voo
        pthread_mutex_lock(&shm->pos_mutex);
        for (int i = 0; i < shm->n_flights; i++) {
            FlightData *fd = &shm->flights[i];
            if (!fd->active && !fd->in_area) continue;

            if (g_weather_loaded) {
                // Usar dados do ficheiro JSON
                get_wind_at(&g_weather_data, fd->pos.lat, fd->pos.lon,
                            fd->pos.alt, &speed, &dir);
                // se não encontrar zona → fallback 15kt / 180°
            } else {
                // Fórmula senoidal sintética
                speed = 15 + 12*sin(t * 2π / 6);  // 3-27 kt, ciclo 6h
                dir   = 180 + 40*sin(t * 2π / 4);  // 140-220°, ciclo 4h
            }

            fd->wind_speed_kt = speed;  // ESCREVE na shared memory
            fd->wind_dir_deg = dir;     // o filho lê daqui
        }

        // 4. Atualizar env para display no UI
        //    (usa o vento do primeiro voo ativo na área)
        shm->env.wind_speed_kt = ...;
        shm->env.wind_dir_deg = ...;
    }
}
```

---

### 4.8 us110_weather.c — Parser de Weather JSON e Zone Lookup

**Função:** Carregar ficheiros JSON de weather e procurar o vento numa posição 3D.

**Linhas 14-73 — load_weather_from_json():**
- Abre ficheiro JSON
- Extrai `provider`, `duration_hours`
- Extrai array `zones` → para cada zona extrai:
  - `lat_north`, `lat_south`, `lon_west`, `lon_east` (bounding box)
  - `alt_ft_lo`, `alt_ft_hi` (altitude em pés)
  - `dir_deg`, `speed_kt` (vento)
- Aceita aliases: `lat1`/`lat2`, `lon1`/`lon2` (compatibilidade com formatos diferentes)

**Linhas 75-89 — get_wind_at():**
```c
int get_wind_at(WeatherDataSet *wds, double lat, double lon, double alt_m,
                double *speed_kt, double *dir_deg) {
    for (int i = 0; i < wds->n_zones; i++) {
        double alt_ft = alt_m / 0.3048;  // converte metros → pés
        if (lat >= z->lat_south && lat <= z->lat_north &&
            lon >= z->lon_west  && lon <= z->lon_east &&
            alt_ft >= z->alt_ft_lo && alt_ft <= z->alt_ft_hi) {
            *speed_kt = z->speed_kt;
            *dir_deg = z->dir_deg;
            return 1;  // encontrou
        }
    }
    return 0;  // não encontrou zona para esta posição
}
```
Procura a primeira zona cujo volume 3D contenha a posição do voo. Se não encontrar, a thread de ambiente usa fallback 15kt/180°.

---

### 4.9 physics.c — Motor de Física

**Função:** Cálculos de distância, área, safety breach, movimento de aeronaves.

**Linhas 6-12 — h_dist():**
Distância horizontal entre dois pontos (fórmula simplificada de Haversine, usando cos(latitude média)). Precisão suficiente para distâncias curtas (escala da península ibérica).

**Linhas 14-23 — in_area() / in_area_full():**
- `in_area()`: verifica se lat/lon estão dentro do retângulo da península
- `in_area_full()`: igual + verifica altitude (0 a 14000m)

**Linhas 25-32 — safety_breach():**
```c
int safety_breach(Pos3D a, Pos3D b, double *h_m, double *v_m) {
    double h = h_dist(a, b);
    double v = fabs(a.alt - b.alt);
    return (h < 9260.0) && (v < 305.0);  // ICAO 5NM / 1000ft
}
```

**Linhas 34-64 — interp_spd() / interp_rate():**
Interpolação linear nas tabelas de perfil. Dada uma altitude, devolve a velocidade (knots) ou razão de subida/descida (m/s) interpolada entre as entradas mais próximas.

**Linhas 66-134 — advance():**
Move uma aeronave `dt` segundos ao longo de um segmento. A lógica depende da fase:
- **CRUISE:** velocidade horizontal = `cruise_kt`, velocidade vertical = 0
- **CLIMB:** velocidade horizontal = interpolação da tabela climb, velocidade vertical = razão de subida positiva
- **DESCEND:** igual ao climb mas com razão negativa (desce)

A direção horizontal é calculada como o vetor unitário do start ao end do segmento. A posição avança `velocidade * dt` nessa direção. A altitude avança `vz * dt`.

Retorna 1 quando chega ao fim do segmento.

**Linhas 136-170 — fast_forward_flight():**
Avança um voo `seconds` segundos chamando `advance()` repetidamente com `dt = 1s`. Itera por todos os segmentos até consumir o tempo ou chegar ao fim. Usado para calcular a posição de voos que partiram antes de `sim_start_sec`.

---

### 4.10 ui.c — Interface de Terminal

**Função:** Renderização do mapa, menus, sumário.

**Linhas 16-30 — lat_to_row() / lon_to_col():**
Convertem coordenadas geográficas para posições no grid 70×20 caracteres.

**Linhas 32-41 — flight_in_violation():**
Verifica se um voo está em violação no step atual usando a matriz `last_viol_step[][]`.

**Linhas 145-282 — draw_airspace():**
- Limpa ecrã
- Mostra cabeçalho com hora, step, área, vento
- Desenha grelha 70×20 com:
  - `●` (unicode circle, colorido) = voo ativo na área, sem violação
  - `*` (vermelho bold) = voo em violação
  - `.` (cyan dim) = trail dots (posições históricas)
- Lista de voos com ID, altitude, velocidade, status (WAIT/IN/OUT/DONE)
- Banner de colisão se houver violações ativas

**Linhas 284-327 — show_summary():**
Ecrã final pós-simulação com PASS/FAIL, lista de voos, top 10 violações.

---

### 4.11 json_parser.h — Parser JSON Minimalista

**Função:** 4 funções header-only para parsing JSON sem dependências externas.

| Função | Propósito |
|---|---|
| `jp_skip(p)` | Avança espaços em branco |
| `jp_close(p, '{', '}')` | Encontra o `}` que fecha o `{` atual (depth-aware) |
| `jp_dbl(src, key, &val)` | Extrai valor double após uma chave |
| `jp_str(src, key, buf, max)` | Extrai string após uma chave |
| `jp_arr(src, key)` | Extrai array `[...]` como substring |

Todas são `static` — cada .c que inclui o header tem a sua própria cópia (header-only).

---

## 5. Verificação US por US

### US105 — Initialize hybrid simulation environment

| Critério | Onde | Como |
|---|---|---|
| Parent spawns dedicated threads | `us105_init.c:94-96` | `pthread_create` × 3 (detector, report, environment) |
| Each flight is independent process | `us105_init.c:98-106` | `fork()` × N, cada filho executa `run_flight_process()` |
| Shared memory allocated and initialized | `us105_init.c:20-30` | `shm_open` + `ftruncate` + `mmap(MAP_SHARED)` |
| Flights use semaphores | `us105_init.c:35-38` | `sem_open` de 2 named semaphores, usados em `us108_sync.c` |
| C + threads + mutexes + cond vars + signals | Todo o código | C standard, pthreads, 4 mutexes, 3 cond vars, SIGINT handler |

### US106 — Function-specific threads in parent

| Critério | Onde | Como |
|---|---|---|
| Safety violation detection thread | `us106_threads.c:8-71` | `violation_detector_thread()` — scan O(n²) de pares |
| Report generation thread created | `us105_init.c:95` | `pthread_create` de `report_generator_thread` |
| Additional thread | `us105_init.c:96` | `pthread_create` de `environment_thread` (US110) |
| Threads managed with mutexes and cond vars | `us106_threads.c:14-67` | `detect_mutex`/`detect_cond`, `pos_mutex`, `viol_mutex` no detector |

### US107 — Notify report thread via condition variables

| Critério | Onde | Como |
|---|---|---|
| Detector monitors shared memory | `us106_threads.c:26-67` | `safety_breach()` em todos os pares, sob `pos_mutex` |
| Detector signals report via cond var | `us106_threads.c:61` | `pthread_cond_signal(&shm->viol_cond)` após cada violação |
| Report waits on cond var, processes | `us107_report_notify.c:21-40` | `pthread_cond_timedwait(&shm->viol_cond)` → imprime `[REPORT]` |
| Proper mutex locking | Ambos os ficheiros | `viol_mutex` protege a fila de violações em todas as operações |

### US108 — Step-by-step synchronization

| Critério | Onde | Como |
|---|---|---|
| Semaphores control step progression | `us108_sync.c:99-122` | Counting semaphore barrier: pai post N × start, wait N × done |
| | `us108_sync.c:26-97` | Cada filho: wait start → advance → post done |

### US109 — Generate and store final report

| Critério | Onde | Como |
|---|---|---|
| Report aggregates at conclusion | `us107_report_notify.c:44-46` | `if (generate_report) write_report(...)` |
| Includes flights, statuses, violations | `us109_report.c:32-67` | Flight summary + violation log com posições |
| PASS/FAIL indicated | `us109_report.c:69-72` | `RESULT: PASS` ou `RESULT: FAIL` |
| Saved to file | `us109_report.c:29,74` | `fopen(path)` → timestamped `.txt` |

### US110 — Environmental influences (wind)

| Critério | Onde | Como |
|---|---|---|
| Environment thread spawned | `us105_init.c:96` | `pthread_create(&env_thr, ..., environment_thread, shm)` |
| Loads wind from weather service | `us110_env.c:24-37` | `load_weather_from_json()` — 3 providers JSON |
| Data written to shared memory each step | `us110_env.c:71-72` | `fd->wind_speed_kt = speed; fd->wind_dir_deg = dir;` |

---

## 6. Ligações Cruzadas

### Quem cria o quê?

```
main.c:run_simulation()
  └─► us105_init.c:init_hybrid_simulation()     ← CRIA TUDO
        ├─► shm_open + mmap                      ← shared memory
        ├─► sem_open × 2                         ← semáforos
        ├─► pthread_mutex_init × 4               ← mutexes
        ├─► pthread_cond_init × 3                ← condition vars
        ├─► pthread_create(detector)             ← thread US106
        │     └─► us106_threads.c:violation_detector_thread()
        ├─► pthread_create(report)               ← thread US107
        │     └─► us107_report_notify.c:report_generator_thread()
        │           └─► us109_report.c:write_report()  ← ficheiro US109
        ├─► pthread_create(environment)          ← thread US110
        │     └─► us110_env.c:environment_thread()
        │           └─► us110_weather.c:load_weather_from_json()
        │           └─► us110_weather.c:get_wind_at()
        └─► fork() × N                           ← processos filho
              └─► us108_sync.c:run_flight_process()
                    └─► physics.c:advance()       ← movimento
                    └─► physics.c:in_area_full()  ← área
```

### Quem chama simulation_step()?

Apenas `main.c:289` — o main loop. Garantia de que o pai é o único maestro.

### Quem chama write_report()?

Apenas `us107_report_notify.c:45` — a thread de report, quando `shm->generate_report == 1`.

### Quem lê/escreve o quê na shared memory?

| Campo | Escritor | Leitores |
|---|---|---|
| `flights[].pos/vel/phase/active/completed` | Filhos (`us108_sync.c:71-87`) | Detetor, Ambiente, UI |
| `flights[].wind_speed_kt/wind_dir_deg` | Ambiente (`us110_env.c:71-72`) | Filhos (`us108_sync.c:45-46`) |
| `violations[] / n_violations` | Detetor (`us106_threads.c:36-63`) | Report (`us107_report_notify.c:31-40`) |
| `env.wind_*` | Ambiente (`us110_env.c:79-81`) | UI (`ui.c:165-169`) |
| `step_ready` | Main (`main.c:297`) | Detetor (`us106_threads.c:14-22`) |
| `env_ready` | Main (`main.c:302`) | Ambiente (`us110_env.c:40-48`) |
| `running` | Main (`main.c:321`), SIGINT (`main.c:39`) | Todos (todas as threads e filhos verificam) |
| `current_step` | Main (`main.c:287`) | Todos |
| `generate_report` | Main (`main.c:319`) | Report (`us107_report_notify.c:44`) |

### Quem usa cada mutex?

| Mutex | Bloqueia |
|---|---|
| `pos_mutex` | Filhos a escrever posições, Detetor a ler posições, Ambiente a ler posições |
| `viol_mutex` | Detetor a escrever violações, Report a ler violações |
| `detect_mutex` | Main a sinalizar step_ready, Detetor a esperar/resetar step_ready |
| `env_mutex` | Main a sinalizar env_ready, Ambiente a esperar/resetar env_ready |

---

## 7. Perguntas Prováveis do Professor

### Q1: "Porque é que usaram processos para os voos em vez de threads?"

**Resposta:** Cada voo é uma entidade independente. Usar processos (`fork()`) em vez de threads demonstra o uso de IPC com shared memory (requisito do enunciado). Além disso, se um voo tiver um bug ou crash, não afeta os outros. A comunicação entre eles é feita exclusivamente através da shared memory — os voos não comunicam diretamente entre si.

### Q2: "Explique o mecanismo de barreira com semáforos."

**Resposta:** Usamos um counting semaphore com dois semáforos nomeados (`/aisafe_start` e `/aisafe_done`). O pai conta quantos voos estão ativos (N), faz `sem_post(start)` N vezes e depois `sem_wait(done)` N vezes. Cada filho faz 1 `sem_wait(start)`, executa o step, e 1 `sem_post(done)`. Como o pai espera N `sem_wait(done)`, garante-se que todos os N filhos terminaram o step antes de o pai continuar. É como uma barreira — ninguém avança para o step t+1 enquanto houver alguém no step t.

### Q3: "O que acontece se o utilizador carregar em Ctrl+C?"

**Resposta:** O handler de `SIGINT` (`main.c:36-40`) define `shm->running = 0` e `stop_sim = 1`. O main loop deteta `stop_sim` e sai. Todas as threads verificam `shm->running` em cada iteração e saem. Os filhos são desbloqueados com `sem_post(start)` extra e o pai faz `waitpid()` para recolher todos. As threads recebem `pthread_join()`. O `cleanup_shared_memory()` destrói todos os recursos. É um shutdown limpo.

### Q4: "Porque é que usaram named semaphores (sem_open) em vez de unnamed (sem_init)?"

**Resposta:** `sem_init` com `pshared=1` devia funcionar entre processos, mas na prática não é suportado de forma fiável em todos os sistemas POSIX (ex: macOS não suporta). `sem_open` cria semáforos nomeados no kernel (`/dev/shm/`) que são garantidamente process-shared em qualquer sistema. É uma escolha de portabilidade.

### Q5: "Porque é que têm 4 mutexes e não 1 só?"

**Resposta:** Para reduzir contenção. Se usássemos 1 mutex global, quando o detetor estivesse a varrer posições, o report ficava bloqueado (e vice-versa). Com 4 mutexes, o detetor pode estar a ler posições (`pos_mutex`) enquanto o report está a processar violações (`viol_mutex`) — operações independentes correm em paralelo. Seguimos o princípio de granularidade fina: cada mutex protege exatamente um recurso.

### Q6: "Como é que o vento afeta a simulação?"

**Resposta:** A thread de ambiente (`us110_env.c`) carrega dados de vento de um ficheiro JSON (3 providers: Crazy Weather, Happy Weather, Synthetic) ou usa uma fórmula senoidal. Em cada step, atribui `wind_speed_kt` e `wind_dir_deg` a cada voo na shared memory. O processo filho (`us108_sync.c:45-57`), após `advance()`, aplica o wind drift à posição e velocidade ground. A fórmula usa trigonometria esférica: converte direção do vento para componentes zonal/meridional, ajusta com cos(latitude) para a projeção de Mercator, e adiciona à posição. A velocidade ground (`vel.vx/vy`) também é afetada. O vento NÃO altera a airspeed nem o heading — apenas a trajetória no solo e a ground speed, que é o comportamento físico correto.

### Q7: "Explique o fluxo de uma violação desde a deteção até ao ficheiro."

**Resposta:**
1. O detetor (`us106_threads.c`) varre todos os pares de voos e chama `safety_breach()`
2. Se `h_dist < 9260m && |alt_a - alt_b| < 305m` → violação
3. Regista no array `shm->violations[]` sob `viol_mutex`
4. Sinaliza `viol_cond` com `pthread_cond_signal()`
5. A thread de report (`us107_report_notify.c`) está em `pthread_cond_timedwait(viol_cond, 1s)`
6. Acorda, lê as novas violações sob `viol_mutex`
7. Imprime `[REPORT] Step N: ID1 <-> ID2 h_dist=... v_dist=...` para stderr (tempo real)
8. No fim da simulação, `shm->generate_report = 1`, `shm->running = 0`
9. A thread de report chama `write_report()` → ficheiro `report_YYYYMMDD_HHMMSS.txt`

### Q8: "Como é tratado um voo que parte depois do início da simulação?"

**Resposta:** No `us105_init.c:76-81`, se `sim_start_sec < departure_sec`, o voo é inicializado com `active = 0` e posição no primeiro waypoint. No `us108_sync.c:34-40`, a cada step o filho verifica `current_time >= plan->departure_sec`. Quando esta condição se torna verdadeira, o voo ativa-se (`started = 1`) e começa a mover-se. Até lá, faz `sem_wait(start)` e `sem_post(done)` sem alterar posição — apenas sincroniza com a barreira. No UI aparece com status `WAIT`.

### Q9: "E um voo que partiu antes do início da simulação?"

**Resposta:** No `us105_init.c:82-88`, calcula-se `offset = sim_start_sec - departure_sec`. Se `offset > 0`, chama-se `fast_forward_flight()` (`physics.c:136-170`) que simula `offset` segundos chamando `advance()` repetidamente. O voo é avançado através de todos os segmentos (climb, cruise, descend) até à posição que teria no momento `sim_start_sec`. Fica `active = 1` e pronto para continuar no step 0.

### Q10: "O que garante que não há race conditions?"

**Resposta:** Várias camadas:
1. Cada recurso na shared memory tem o seu mutex dedicado (`pos_mutex`, `viol_mutex`, `detect_mutex`, `env_mutex`)
2. Os filhos escrevem posições sob `pos_mutex`; o detetor e o ambiente leem sob o mesmo mutex
3. O detetor escreve violações sob `viol_mutex`; o report lê sob o mesmo mutex
4. As flags `step_ready`/`env_ready` são protegidas por `detect_mutex`/`env_mutex`
5. A barreira de semáforos garante que os filhos não escrevem enquanto o detetor está a ler (lockstep)
6. As condition variables são sempre usadas com o padrão correto: lock → while(!cond) wait → unlock

### Q11: "Quem gera o ficheiro de report final?"

**Resposta:** A **thread de report** (`report_generator_thread`), que corre no **processo pai**. Os filhos apenas simulam o voo — escrevem posições na shared memory, mas não geram ficheiros. O fluxo é: filhos escrevem posições → detetor (pai) deteta violações → report (pai) imprime em tempo real e gera ficheiro final.

### Q12: "Qual é a relação entre US106 e US107? Porque estão em ficheiros separados?"

**Resposta:** O US106 implementa a thread detetora (produz violações). O US107 implementa a thread de report (consome violações). Estão em ficheiros separados (`us106_threads.c` e `us107_report_notify.c`) por separação de preocupações. A comunicação entre elas é via `viol_cond` + `viol_mutex`. O detetor sinaliza; o report consome. A inicialização de ambas é feita no US105 (`us105_init.c:94-95`), que é o orquestrador.

### Q13: "O simulador pode correr indefinidamente?"

**Resposta:** Não. Existem 3 mecanismos de paragem:
1. **Terminação natural:** Todos os voos completam as suas rotas → `simulation_step()` retorna 0 → loop termina
2. **Safety cap:** `SAFETY_CAP_S = 28800` (8 horas). Se a simulação atingir 28800 steps sem todos os voos terminarem, o loop para. Existe para evitar loops infinitos se um segmento JSON for inalcançável.
3. **Ctrl+C:** O utilizador pode interromper a qualquer momento. O handler de SIGINT faz shutdown limpo.

### Q14: "O que são os 3 ficheiros JSON de weather e qual a diferença?"

**Resposta:**
- **CW.json** ("Crazy Weather"): 45 zonas, altitude 0-12.000 ft. Dados reais, cobre apenas baixa altitude.
- **HP.json** ("Happy Weather"): 45 zonas, altitude 0-12.000 ft. Dados reais alternativos, mesma cobertura.
- **SYNTHETIC.json**: 20 zonas, altitude 0-45.000 ft, cobre toda a península ibérica (lat 38-44N, lon 2-10W). Dados sintéticos gerados para preencher a lacuna de altitude (voos em cruzeiro a FL350-450).

### Q15: "Os voos comunicam diretamente entre si?"

**Resposta:** Não. Toda a comunicação é via shared memory. Um voo escreve a sua posição na shared memory; o detetor lê as posições de todos os voos e deteta conflitos. Os voos não sabem da existência uns dos outros — apenas o detetor (no pai) tem visão global. Este design é mais realista (os pilotos não comunicam diretamente; o controlador de tráfego aéreo — aqui o detetor — monitoriza todos).

---

**Fim do Guia de Defesa**
