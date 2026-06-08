# US106 — Explicação da Implementação

## O que faz

Implementa uma **thread dedicada à deteção de violações de segurança** (conflitos entre voos)
durante a simulação. Esta thread corre **concorrentemente** no processo pai, analisando pares de
voos em cada passo da simulação e reportando quando a distância entre eles viola os limites de
segurança.

---

## Ficheiros

| Ficheiro | O que contém |
|----------|-------------|
| `us106_threads.h` | Declaração das funções públicas |
| `us106_threads.c` | Implementação da thread detetora + `detect_initial_violations()` |

A thread de **relatório** (`report_generator_thread`) está noutro ficheiro (`us107_report_notify.c`)
porque faz parte da US107 (notificação por condição variável).

---

## Como funciona

### 1. Criação da thread

Em `us105_init.c:94`:
```c
pthread_create(out_detect_thr, NULL, violation_detector_thread, shm);
```

A thread é criada **depois** da memória partilhada estar inicializada e **antes** dos processos
filho serem lançados.

### 2. Ciclo de vida da thread (`violation_detector_thread`)

```
1. Bloqueia em detect_cond à espera que step_ready = 1
2. Quando acorda (main loop fez broadcast):
   a. Faz lock de pos_mutex
   b. Percorre todos os pares de voos (i, j)
   c. Para cada par, chama safety_breach() da physics.h
   d. Se há violação:
      - Lock viol_mutex
      - Regista violação em shm->violations[]
      - Incrementa n_violations
      - Dá signal a viol_cond (acorda a thread de report)
      - Unlock viol_mutex
   e. Unlock pos_mutex
3. Volta ao passo 1
4. Quando shm->running = 0, sai do loop e faz pthread_exit
```

### 3. `detect_initial_violations()`

Função separada (NÃO é uma thread) que corre **no processo pai antes do loop principal**.
Verifica se já existem conflitos entre voos ativos no passo 0 (sim_start).

```c
// Chamada em main.c ANTES do loop de simulação:
detect_initial_violations(shm);
```

### 4. Sincronização

| Mutex | Protege | Usado por |
|-------|---------|-----------|
| `pos_mutex` | Posições dos voos (`flights[].pos`) | Detector (leitura) + child processes (escrita) |
| `viol_mutex` | Array de violações (`violations[]`) | Detector (escrita) + Report thread (leitura) |
| `detect_mutex` | Sinal `step_ready` | Detector (wait) + Main loop (broadcast) |

### 5. Fluxo completo (main loop)

```
PARENT PROCESS:
  init_hybrid_simulation()
    → cria shared memory, mutexes, condvars
    → cria violation_detector_thread    ← US106
    → cria report_generator_thread      ← US107
    → cria environment_thread           ← US110
    → fork() flight processes           ← US108

  detect_initial_violations(shm)        ← US106

  loop para cada step:
    sem_post(sem_step_start)            ← child processes correm
    sem_wait(sem_step_done)             ← espera que todos acabem
    shm->step_ready = 1
    pthread_cond_broadcast(&detect_cond) ← acorda detector
    (detector corre, regista violações, sinaliza viol_cond)
    (report thread acorda e imprime [REPORT])

  shm->running = 0
  pthread_cond_broadcast(&detect_cond)  ← acorda detector para sair
  pthread_join(violation_detector_thread)
  pthread_join(report_generator_thread)
  cleanup_shared_memory()
```

---

## Exemplo de output

```
[DETECTOR] Step 0: TP201 <-> IB202  h=2340m v=89m    ← detect_initial_violations
[REPORT]   Step 0: TP201 <-> IB202  h_dist=2340m  v_dist=89m

... (simulação corre vários steps) ...

[DETECTOR] Step 42: TP201 <-> IB202  h=1120m v=45m
[REPORT]   Step 42: TP201 <-> IB202  h_dist=1120m  v_dist=45m
```

---

## Porque é que o código está correto (checklist US106)

| Critério | Como é satisfeito |
|----------|-------------------|
| US106.1 — Thread de deteção de violações | `violation_detector_thread()` criada com `pthread_create` |
| US106.2 — Thread de relatório | `report_generator_thread()` criada em separado (US107) |
| US106.3 — Possibilidade de mais threads | `environment_thread()` (US110) é uma terceira thread |
| US106.4 — Mutexes + condition variables | 4 mutexes + 3 condvars, todas `PTHREAD_PROCESS_SHARED` |
