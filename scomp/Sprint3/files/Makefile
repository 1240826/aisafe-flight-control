CC = gcc
TEST_DIR = ../test
CFLAGS = -Wall -Wextra -g
LDFLAGS = -lpthread -lrt -lm

OBJS = main.o physics.o ui.o us105_init.o us106_threads.o \
       us107_report_notify.o us108_sync.o us109_report.o \
       us110_env.o us110_weather.o

P = 10

simulation: $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o simulation $(LDFLAGS)

sim_server: sim_server.c
	$(CC) $(CFLAGS) sim_server.c -o sim_server

main.o: main.c common.h json_parser.h us110_env.h
	$(CC) $(CFLAGS) -c main.c

physics.o: physics.c physics.h common.h
	$(CC) $(CFLAGS) -c physics.c

ui.o: ui.c ui.h common.h
	$(CC) $(CFLAGS) -c ui.c

us105_init.o: us105_init.c us105_init.h common.h
	$(CC) $(CFLAGS) -c us105_init.c

us106_threads.o: us106_threads.c us106_threads.h common.h physics.h
	$(CC) $(CFLAGS) -c us106_threads.c

us107_report_notify.o: us107_report_notify.c us107_report_notify.h us109_report.h common.h
	$(CC) $(CFLAGS) -c us107_report_notify.c

us108_sync.o: us108_sync.c us108_sync.h common.h physics.h
	$(CC) $(CFLAGS) -c us108_sync.c

us109_report.o: us109_report.c us109_report.h common.h
	$(CC) $(CFLAGS) -c us109_report.c

us110_env.o: us110_env.c us110_env.h us110_weather.h common.h
	$(CC) $(CFLAGS) -c us110_env.c

us110_weather.o: us110_weather.c us110_weather.h json_parser.h common.h
	$(CC) $(CFLAGS) -c us110_weather.c

run: simulation
	./simulation $(TEST_DIR)/scenario_heavy_traffic.json 1 $(P)

clean:
	rm -f *.o simulation sim_server report_*.txt

transatlantic: simulation
	./simulation $(TEST_DIR)/scenario_transatlantic_arrivals.json 1 $(P)

mixed: simulation
	./simulation $(TEST_DIR)/scenario_mixed_traffic.json 1 $(P)

conflict: simulation
	./simulation $(TEST_DIR)/scenario_conflict_detection.json 1 $(P)

safe: simulation
	./simulation $(TEST_DIR)/scenario_safe_passage.json 1 $(P)

heavy: simulation
	./simulation $(TEST_DIR)/scenario_heavy_traffic.json 1 $(P)

demo: simulation
	./simulation $(TEST_DIR)/scenario_demo_comprehensive.json 1 $(P)

debug: simulation
	./simulation $(TEST_DIR)/scenario_transatlantic_arrivals.json 1 1
