# US101 — Explicação para colegas

**Ficheiro:** `us101_flight.c` | **Função:** `run_flight()`  
**Física:** `physics.h` — `advance()`

---

## O que esta US faz

Corre dentro do **processo filho**. É o motor de cada avião.

A cada segundo simulado:
1. Espera pela permissão do pai (barreira US103)
2. Avança a posição com a física vetorial
3. Envia a posição ao pai pelo pipe
4. Verifica se recebeu SIGUSR1 (violação)

---

## A física — climb, cruise, descend

O JSON do prof tem 3 segmentos: subida, cruzeiro, descida.

```
Climb:   velocidade e taxa de subida lidas da tabela de perfil
         interpolação linear por altitude
         vz > 0 (subindo)

Cruise:  velocidade constante = cruise_kt
         vz = 0

Descend: velocidade e taxa de descida lidas da tabela de perfil
         vz < 0 (descendo)
```

Posição nova a cada segundo (dt=1):
```
Δlat = vy / R_terra
Δlon = vx / (R_terra × cos(lat))
Δalt = vz × 1
```

---

## Porquê timestep = 1 segundo?

A 460 kt (237 m/s), dois aviões a convergir fecham a ~474 m/s. O cilindro tem 14 816 m. Com 30s por passo, cobrem 7 100 m — podem entrar e sair do cilindro sem ser detetados. Com 1s, cada passo é 237 m — deteção gradual.

O prof disse: **"se o time step for muito grande não apanha as interseções"**.

---

## Histórico de posições (requisito US101)

US101 diz: *"store past positions to anticipate and detect potential safety violations"*.

```c
/* Em FlightState (common.h) */
Snapshot history[MAX_HISTORY];   /* 300 entradas */
int      hist_count;

/* Em store_history() — chamado após cada collect() */
idx = f->hist_count % MAX_HISTORY;   /* circular */
f->history[idx].pos = f->last_pos;
f->hist_count++;
```

---

## Handlers de sinais — padrão seguro das aulas

```c
static volatile sig_atomic_t got_violation = 0;

/* Handler SIGUSR1: sigfillset bloqueia TUDO durante a execução */
static void handle_usr1(int sig) {
    (void)sig;
    write(STDOUT_FILENO, msg, ...);   /* write() = async-signal-safe */
    got_violation = 1;
}

/* Registo */
memset(&act, 0, sizeof(struct sigaction));
sigfillset(&act.sa_mask);      /* bloquear todos durante handler */
act.sa_handler = handle_usr1;
act.sa_flags   = SA_RESTART;   /* read() reinicia após handler */
sigaction(SIGUSR1, &act, NULL);
```

---

## GoToken safe=0 — segurar o avião

Quando o pai detecta uma violação, envia `GoToken{safe=0}`:

```c
if (!tok.safe) {
    /* Não avançar — enviar posição actual sem mover */
    write(rfd, &upd, sizeof(PosUpdate));
    step++;
    continue;
}
/* safe=1: avançar normalmente */
done = advance(plan, ...);
```

Isto implementa o que o prof disse: *"pai diz aos filhos para calcular a próxima posição **se não houver colisão**"*.

---

## Testar

```bash
./simulation ../test/scenario0_single.json 5400 1 2>&1 | grep "CLIMB\|CRUISE\|DESCEND"
```
