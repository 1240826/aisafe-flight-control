CC     = gcc
CFLAGS = -Wall -Wextra -g

OBJS = main.o us100_init.o us101_flight.o us102_detector.o us103_sync.o us109_report.o

simulation: $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o simulation -lm

main.o: main.c common.h json_parser.h
	$(CC) $(CFLAGS) -c main.c

us100_init.o: us100_init.c common.h physics.h
	$(CC) $(CFLAGS) -c us100_init.c

us101_flight.o: us101_flight.c common.h physics.h
	$(CC) $(CFLAGS) -c us101_flight.c

us102_detector.o: us102_detector.c common.h physics.h
	$(CC) $(CFLAGS) -c us102_detector.c

us103_sync.o: us103_sync.c common.h physics.h
	$(CC) $(CFLAGS) -c us103_sync.c

us109_report.o: us109_report.c common.h
	$(CC) $(CFLAGS) -c us109_report.c

clean:
	rm -f *.o simulation simulation_report.txt

# timestep=1s, print every 60 steps (default)
run: simulation
	./simulation ../test/scenario0_single.json

scenario1: simulation
	./simulation ../test/scenario1_area_cases.json

scenario2: simulation
	./simulation ../test/scenario2_normal.json

scenario3: simulation
	./simulation ../test/scenario3_violations.json

scenario4: simulation
	./simulation ../test/scenario4_large_normal.json

scenario5: simulation
	./simulation ../test/scenario5_termination.json

# show every step (useful for debugging)
debug: simulation
	./simulation ../test/scenario3_violations.json 1 1
