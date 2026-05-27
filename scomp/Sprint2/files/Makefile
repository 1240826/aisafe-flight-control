C = gcc
CFLAGS = -Wall -Wextra -g

OBJS = main.o physics.o us100_init.o us101_flight.o us102_detector.o us103_sync.o us109_report.o

# Print interval — default
P = 60

simulation: $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o simulation -lm

main.o: main.c common.h json_parser.h
	$(CC) $(CFLAGS) -c main.c

physics.o: physics.c physics.h common.h
	$(CC) $(CFLAGS) -c physics.c

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

run: simulation
	./simulation ../test/scenario0_single.json 1 $(P)

scenario1: simulation
	./simulation ../test/scenario1_area_cases.json 1 $(P)

scenario2: simulation
	./simulation ../test/scenario2_normal.json 1 $(P)

scenario3: simulation
	./simulation ../test/scenario3_violations.json 1 $(P)

scenario4: simulation
	./simulation ../test/scenario4_large_normal.json 1 $(P)

scenario5: simulation
	./simulation ../test/scenario5_termination.json 1 $(P)

debug: simulation
	./simulation ../test/scenario3_violations.json 1 1
