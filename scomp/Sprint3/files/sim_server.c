#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/wait.h>
#include <signal.h>
#include <sys/stat.h>
#include <time.h>

#define DEFAULT_PORT 9999
#define MAX_BUF (10 * 1024 * 1024)

static volatile int keep_running = 1;

static void handle_signal(int sig) {
    (void)sig;
    keep_running = 0;
}

static int read_n(int fd, unsigned char *buf, int n) {
    int total = 0;
    while (total < n) {
        int r = (int)read(fd, buf + total, n - total);
        if (r <= 0) return -1;
        total += r;
    }
    return 0;
}

static int write_n(int fd, const unsigned char *buf, int n) {
    int total = 0;
    while (total < n) {
        int w = (int)write(fd, buf + total, n - total);
        if (w <= 0) return -1;
        total += w;
    }
    return 0;
}

int main(int argc, char **argv) {
    int port = DEFAULT_PORT;
    int start_sec = 32340;
    int print_interval = 30;
    int draw_delay_ms = 500;
    if (argc >= 2) port = atoi(argv[1]);
    if (port <= 0 || port > 65535) port = DEFAULT_PORT;
    if (argc >= 3) { int s = atoi(argv[2]); if (s >= 0 && s < 86400) start_sec = s; }
    if (argc >= 4) { int p = atoi(argv[3]); if (p >= 0) print_interval = p; }
    if (argc >= 5) { int d = atoi(argv[4]); if (d >= 0) draw_delay_ms = d; }
    printf("[SERVER] Start time: %02d:%02d UTC\n", start_sec / 3600, (start_sec % 3600) / 60);
    printf("[SERVER] Print interval: %ds (0=fast)\n", print_interval);
    printf("[SERVER] Draw delay: %dms (0=fast)\n", draw_delay_ms);
    fflush(stdout);

    signal(SIGINT, handle_signal);
    signal(SIGTERM, handle_signal);

    mkdir("temp", 0755);

    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) { perror("socket"); return 1; }

    int opt = 1;
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        perror("setsockopt"); return 1;
    }

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    if (bind(server_fd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        perror("bind"); return 1;
    }

    if (listen(server_fd, 5) < 0) {
        perror("listen"); return 1;
    }

    printf("[SERVER] Listening on port %d (PID %d)\n", port, getpid());
    fflush(stdout);

    while (keep_running) {
        struct sockaddr_in client;
        socklen_t client_len = sizeof(client);
        int client_fd = accept(server_fd, (struct sockaddr*)&client, &client_len);
        if (client_fd < 0) {
            if (keep_running) perror("accept");
            continue;
        }

        char client_ip[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &client.sin_addr, client_ip, sizeof(client_ip));
        printf("[SERVER] Connection from %s:%d\n", client_ip, ntohs(client.sin_port));
        fflush(stdout);

        int32_t json_len_be;
        if (read_n(client_fd, (unsigned char*)&json_len_be, 4) < 0) {
            close(client_fd);
            continue;
        }
        int json_len = ntohl(json_len_be);
        if (json_len <= 0 || json_len > MAX_BUF) {
            fprintf(stderr, "[SERVER] Invalid JSON length: %d\n", json_len);
            close(client_fd);
            continue;
        }

        unsigned char *json_buf = malloc(json_len + 1);
        if (!json_buf) { close(client_fd); continue; }
        if (read_n(client_fd, json_buf, json_len) < 0) {
            free(json_buf);
            close(client_fd);
            continue;
        }
        json_buf[json_len] = '\0';

        int32_t weather_len_be = 0;
        unsigned char *weather_buf = NULL;
        int weather_len = 0;

        if (read_n(client_fd, (unsigned char*)&weather_len_be, 4) == 0) {
            weather_len = ntohl(weather_len_be);
            if (weather_len > 0 && weather_len <= MAX_BUF) {
                weather_buf = malloc(weather_len + 1);
                if (weather_buf) {
                    if (read_n(client_fd, weather_buf, weather_len) < 0) {
                        free(weather_buf);
                        weather_buf = NULL;
                        weather_len = 0;
                    } else {
                        weather_buf[weather_len] = '\0';
                    }
                } else {
                    weather_len = 0;
                }
            }
        }

        char ts[64];
        time_t now = time(NULL);
        snprintf(ts, sizeof(ts), "%ld", (long)now);

        char scen_path[256];
        snprintf(scen_path, sizeof(scen_path), "temp/scenario_%s.json", ts);
        FILE *f = fopen(scen_path, "w");
        if (!f) {
            fprintf(stderr, "[SERVER] Cannot write %s\n", scen_path);
            free(json_buf);
            free(weather_buf);
            close(client_fd);
            continue;
        }
        fwrite(json_buf, 1, json_len, f);
        fclose(f);

        char report_path[256];
        snprintf(report_path, sizeof(report_path), "temp/report_%s.txt", ts);
        char weather_path[256] = "";
        int has_weather = 0;

        if (weather_buf && weather_len > 0) {
            char wpath[256];
            snprintf(wpath, sizeof(wpath), "temp/weather_%s.json", ts);
            FILE *wf = fopen(wpath, "w");
            if (wf) {
                fwrite(weather_buf, 1, weather_len, wf);
                fclose(wf);
                snprintf(weather_path, sizeof(weather_path), "%s", wpath);
                has_weather = 1;
            }
        }

        pid_t pid = fork();
        if (pid == 0) {
            char delay_str[16];
            snprintf(delay_str, sizeof(delay_str), "%d", draw_delay_ms * 1000);
            setenv("SIM_DRAW_DELAY_US", delay_str, 1);
            char start_str[16], print_str[16];
            snprintf(start_str, sizeof(start_str), "%d", start_sec);
            snprintf(print_str, sizeof(print_str), "%d", print_interval);
            if (has_weather) {
                execlp("./simulation", "./simulation",
                       scen_path, start_str, print_str, report_path, weather_path, NULL);
            } else {
                execlp("./simulation", "./simulation",
                       scen_path, start_str, print_str, report_path, NULL);
            }
            _exit(1);
        } else if (pid > 0) {
            int status;
            waitpid(pid, &status, 0);
        } else {
            fprintf(stderr, "[SERVER] fork failed\n");
        }

        f = fopen(report_path, "rb");
        if (f) {
            fseek(f, 0, SEEK_END);
            long report_len = ftell(f);
            rewind(f);

            if (report_len > 0 && report_len <= MAX_BUF) {
                unsigned char *report_buf = malloc(report_len);
                if (report_buf) {
                    fread(report_buf, 1, report_len, f);

                    int32_t report_len_be = htonl((int32_t)report_len);
                    write_n(client_fd, (unsigned char*)&report_len_be, 4);
                    write_n(client_fd, report_buf, (int)report_len);

                    printf("[SERVER] Sent %ld bytes report to %s:%d\n",
                           report_len, client_ip, ntohs(client.sin_port));

                    free(report_buf);
                }
            }
            fclose(f);
        } else {
            int32_t zero = 0;
            write_n(client_fd, (unsigned char*)&zero, 4);
        }

        unlink(scen_path);
        unlink(report_path);
        if (has_weather) unlink(weather_path);

        free(json_buf);
        free(weather_buf);
        close(client_fd);
        printf("[SERVER] Done with %s:%d\n", client_ip, ntohs(client.sin_port));
        fflush(stdout);
    }

    close(server_fd);
    printf("[SERVER] Shutdown.\n");
    return 0;
}
