# US110 — Integrate Environmental Influences (Wind) into Simulation

## 1. Context

This task is assigned in Sprint 3 as part of the SCOMP-related work. It is the first time this feature is being developed. The objective is to introduce a dedicated environment thread in the parent process that loads weather data (wind) from JSON files and applies it to each flight per simulation step, making the simulation more realistic by modelling wind drift.

**Assigned to:** All team

### 1.1 List of Issues

- Analysis: #82
- Design: #82
- Implement: #82
- Test: #82

---

## 2. Requirements

**US110** As a Flight Control Operator, I want environmental influences (wind) to be integrated into the simulation, so that aircraft trajectories reflect realistic meteorological conditions.

### Acceptance Criteria

- **US110.1** The parent process creates an environment thread that loads weather data concurrently.
- **US110.2** Wind data is read from JSON files with zone-based speed and direction.
- **US110.3** Wind is applied to each active flight per simulation step as a perturbation to position and velocity.
- **US110.4** A fallback synthetic wind generator is used when no weather file is provided.

### Dependencies/References

- US105 — Shared memory must exist and be initialised before threads are created.
- US106 — Function-specific threads in the parent process (environment thread is one of the three dedicated threads).
- US108 — Step-by-step synchronisation (environment thread runs after children complete each step).

---

## 3. Analysis

### 3.0 LLM Assistance

Generative AI was used to support the analysis and design of this user story.

**Prompt 1:** "In a C POSIX simulation with shared memory and multiple threads, how should an environment thread load weather data and update wind for each flight per simulation step?"

**LLM suggestions adopted:**
- A dedicated `environment_thread()` is created via `pthread_create()` after shared memory is initialised
- The thread uses a condition variable (`env_cond`) to wait for the main loop to signal that a new step is ready, avoiding busy-waiting
- Weather data is stored in a static structure loaded from JSON at startup
- Each flight has `wind_speed_kt` and `wind_dir_deg` fields written by the environment thread and read by the flight process

**Decisions made by the team:**
- The weather data (`WeatherDataSet`) is stored in a file-scope static variable rather than in shared memory, since only the environment thread writes and no other process needs the raw zone data — each flight's `fd->wind_speed_kt`/`fd->wind_dir_deg` in shared memory carries the resolved wind for that step
- `env_mutex` is used with `pos_mutex` to coordinate access to flight positions
- Three weather datasets are provided: `CW.json` (Crazy Weather), `HP.json` (Happy Weather), `SYNTHETIC.json` (full coverage)
- A synthetic sine-wave fallback is used when no file is loaded, ensuring the simulation runs without external dependencies

### 3.1 Thread Interaction

The environment thread (`environment_thread`) follows this flow:

```
Main loop                          Environment Thread
    │                                     │
    │  set step_ready = 1                 │
    │  pthread_cond_broadcast(env_cond)──►│  wakes up
    │                                     │  lock pos_mutex
    │                                     │  for each active flight:
    │                                     │    lookup wind zone (lat, lon, alt)
    │                                     │    set fd->wind_speed_kt, fd->wind_dir_deg
    │                                     │  unlock pos_mutex
    │  simulation_step()                  │
    │  (children read fd->wind_*)         │
```

### 3.2 Weather Data Structures

```c
typedef struct {
    double lat_north, lat_south;
    double lon_west, lon_east;
    double alt_ft_lo, alt_ft_hi;
    double dir_deg;
    double speed_kt;
} WeatherZone;

typedef struct {
    char provider[64];
    double duration_hours;
    int n_zones;
    WeatherZone zones[MAX_WEATHER_ZONES];  /* MAX_WEATHER_ZONES = 50 */
} WeatherDataSet;
```

### 3.3 Wind Application (in flight process)

Wind drift is applied in `run_flight_process()` after the planned motion (`advance()`):

```c
/* After advance() applies the planned motion: */
wind_rad = D2R(fd->wind_dir_deg);
wvx = -KT_TO_MS(fd->wind_speed_kt) * sin(wind_rad);
wvy = -KT_TO_MS(fd->wind_speed_kt) * cos(wind_rad);

pos.lon += R2D((wvx * dt) / (cos(lat_mid) * EARTH_R));
pos.lat += R2D((wvy * dt) / EARTH_R);
vel.vx += wvx;
vel.vy += wvy;
```

The aircraft is carried by the moving air mass (wind). This affects ground track and ground speed while leaving indicated airspeed and heading unchanged, which is physically correct for ATC simulation.

### 3.4 Acceptance Tests

**AT1 — Environment thread is created and runs (US110.1)**

Given the simulation starts,
When `init_hybrid_simulation()` calls `pthread_create()` for `environment_thread`,
Then the thread is running, blocked on `env_cond`, and wakes when `env_ready` is set.

**AT2 — Wind data loaded from JSON (US110.2)**

Given a valid weather JSON file (e.g., `HP.json`),
When `env_set_weather_file()` is called before simulation start,
Then `load_weather_from_json()` parses the file and populates the zone array.

**AT3 — Wind applied to each flight per step (US110.3)**

Given an active flight inside a weather zone,
When the environment thread processes step N,
Then `fd->wind_speed_kt` and `fd->wind_dir_deg` reflect the zone values for the flight's position.

**AT4 — Synthetic fallback when no file provided (US110.4)**

Given no weather file is loaded,
When the simulation runs,
Then wind follows a periodic sine wave: speed 3–27 kt (6-hour cycle), direction 140–220° (4-hour cycle).

---

## 4. Design

### 4.1 Initialisation Sequence

```
main()
  → env_set_weather_file("CW.json")          ← optional, before init
  → init_hybrid_simulation()
      → ...
      → pthread_create(environment_thread)    ← US110
      → ...
  → simulation loop
      → signal env_cond (step ready)
      → environment thread updates wind
  → simulation ends
  → pthread_join(env_thr)
```

### 4.2 Zone Lookup

The `get_wind_at()` function performs rectangular volume lookup:

```c
int get_wind_at(WeatherDataSet *wds, double lat, double lon, double alt_m,
                double *speed_kt, double *dir_deg)
{
    for (int i = 0; i < wds->n_zones; i++) {
        WeatherZone *z = &wds->zones[i];
        double alt_ft = alt_m / 0.3048;
        if (lat >= z->lat_south && lat <= z->lat_north &&
            lon >= z->lon_west && lon <= z->lon_east &&
            alt_ft >= z->alt_ft_lo && alt_ft <= z->alt_ft_hi) {
            *speed_kt = z->speed_kt;
            *dir_deg = z->dir_deg;
            return 1;  /* found */
        }
    }
    return 0;  /* not in any zone → use defaults */
}
```

### 4.3 Weather JSON Format

```json
{
    "provider": "Crazy Weather Data",
    "duration_hours": 8.25,
    "zones": [
        {
            "lat_north": 43.84, "lat_south": 40.23,
            "lon_west": -9.80,  "lon_east": -7.95,
            "alt_ft_lo": 0, "alt_ft_hi": 45000,
            "dir_deg": 90, "speed_kt": 28.75
        }
    ]
}
```

Alternative key names `lat1/lat2`, `lon1/lon2` are also supported for compatibility.

### 4.4 Synthetic Fallback

When no weather file is loaded:
```
speed = 15 + 12 × sin(t × 2π / 6)   kt   (cycle: 6 hours)
dir   = 180 + 40 × sin(t × 2π / 4)   deg  (cycle: 4 hours)
```

Wind speed varies between 3 and 27 kt, direction between 140° and 220°, representing a moderate westerly wind shifting over time.

---

## 5. Implementation

| File | Responsibility |
|------|---------------|
| `us110/us110_env.c` / `.h` | `environment_thread()`, `env_set_weather_file()` |
| `us110/us110_weather.c` / `.h` | `load_weather_from_json()`, `get_wind_at()` |
| `weather/CW.json` | Crazy Weather — 45 zones, 0–12 000 ft |
| `weather/HP.json` | Happy Weather — 45 zones, 0–12 000 ft |
| `weather/SYNTHETIC.json` | Full coverage — 20 zones, lat 38–44, lon 10W–2W, 0–45 000 ft |
| `files/common.h` | `WeatherZone`, `WeatherDataSet`, `EnvironmentData` structs |
| `us105/us105_init.c` | `pthread_create(environment_thread)` in `init_hybrid_simulation()` |

---

## 6. Integration/Demonstration

1. Compile the simulation.
2. Run with a weather file:
   ```bash
   ./simulation ../test/scenario_heavy_traffic.json 8 300
   ```
3. Select weather provider from the menu (option 6), or pass a weather file path.
4. Observe log output:
   ```
   [ENV] Loaded Crazy Weather: 45 zones, 8.25 hours
   [ENV] Step 600: wind 21.3kt from 135° (file)
   ```
5. Without a weather file, synthetic wind is used:
   ```
   [ENV] Step 0: wind 15.0kt from 180° (synthetic)
   ```

---

## 7. Observations

- The environment thread uses `pthread_cond_wait` on `env_cond` with `env_mutex` (process-shared), matching the theory pattern from Exercise 3 (condition variables).
- Weather data is static (file scope) — only the environment thread accesses it. Raw zone data is not placed in shared memory; only the resolved wind per flight (`fd->wind_speed_kt`, `fd->wind_dir_deg`) is written to shared memory for the flight processes.
- Wind affects ground track and ground speed but not airspeed or heading — physically correct for ATC simulation.
- Three weather datasets are provided: Crazy Weather (dense zones), Happy Weather (moderate), and SYNTHETIC (full coverage over the Iberian domain).
- The `convert-weather.ps1` script can convert xlsx weather data to JSON format offline.
