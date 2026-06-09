package eapli.aisafe.simulation.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.application.FlightPlanExporter;
import eapli.aisafe.flightplan.application.ProcessBuilderSimulationRunner;
import eapli.aisafe.flightplan.application.SimulationRunner;
import eapli.aisafe.flightplan.application.SimulationRunnerException;
import eapli.aisafe.flightplan.application.SocketSimulationRunner;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for US111 — automatically run an area simulation via SCOMP TCP server
 * and persist the result.
 * Actor: Flight Control Operator / Admin.
 */
@UseCaseController
public class RunAreaSimulationController {

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final FlightPlanExporter exporter;
    private final SimulationRunner runner;
    private final AirControlAreaRepository acaRepo;
    private final SaveSimulationController saveCtrl;

    /** Production constructor — uses framework registries. */
    public RunAreaSimulationController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flights(),
                new FlightPlanExporter(),
                createRunner(),
                PersistenceContext.repositories().airControlAreas(),
                new SaveSimulationController());
    }

    /** Testing constructor — allows injecting mocks. */
    RunAreaSimulationController(final AuthorizationService authz,
                                 final FlightRepository flightRepo,
                                 final FlightPlanExporter exporter,
                                 final SimulationRunner runner,
                                 final AirControlAreaRepository acaRepo,
                                 final SaveSimulationController saveCtrl) {
        this.authz = authz;
        this.flightRepo = flightRepo;
        this.exporter = exporter;
        this.runner = runner;
        this.acaRepo = acaRepo;
        this.saveCtrl = saveCtrl;
    }

    private static SimulationRunner createRunner() {
        final var hostProp = System.getProperty("aisafe.simulator.host");
        final int timeout = Integer.getInteger("aisafe.simulator.timeout", 120);
        if (hostProp != null && !hostProp.isEmpty()) {
            final int port = Integer.getInteger("aisafe.simulator.port", 9999);
            return new SocketSimulationRunner(hostProp, port, timeout);
        }
        final String executable = System.getProperty("aisafe.simulator.executable", "aisafe-simulator");
        return new ProcessBuilderSimulationRunner(executable, timeout);
    }

    /** Returns all registered Air Control Areas for selection. */
    public Iterable<AirControlArea> availableAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR, AISafeRoles.ADMIN);
        return acaRepo.findAll();
    }

    /**
     * Runs the area simulation on the SCOMP TCP server and persists the result.
     *
     * <p>Collects all flights whose departure time falls within [start, end] and that
     * have at least one flight plan. Merges them into a single JSON scenario, sends it
     * to the SCOMP simulator, receives the report text, and saves it via
     * {@link SaveSimulationController#saveSimulation}.
     *
     * @param areaCode       code of the Air Control Area being simulated
     * @param start          start of the simulation time window (inclusive)
     * @param end            end of the simulation time window (inclusive, must be after start)
     * @param thresholdValue safety threshold value (must be &gt; 0)
     * @param thresholdUnit  safety threshold unit (non-blank)
     * @return the persisted {@link Simulation}
     * @throws IllegalStateException if no qualifying flights are found or the simulation fails
     */
    public Simulation runSimulation(final String areaCode,
                                     final LocalDateTime start,
                                     final LocalDateTime end,
                                     final double thresholdValue,
                                     final String thresholdUnit) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR, AISafeRoles.ADMIN);

        final List<FlightPlan> plansToSimulate = new ArrayList<>();
        for (final Flight flight : flightRepo.findAll()) {
            if (flight.departureTime() == null) continue;
            if (flight.departureTime().isBefore(start) || flight.departureTime().isAfter(end)) continue;
            final var fps = flight.flightPlans();
            if (fps.isEmpty()) continue;
            plansToSimulate.add(fps.get(0));
        }

        if (plansToSimulate.isEmpty()) {
            throw new IllegalStateException(
                    "No flights with flight plans found departing between " + start + " and " + end + ".");
        }

        // Build merged JSON array — same pattern as TestFlightPlanController.testScenario()
        final var sb = new StringBuilder("[\n");
        for (int i = 0; i < plansToSimulate.size(); i++) {
            if (i > 0) sb.append(",\n");
            final var entryJson = exporter.exportForSimulator(plansToSimulate.get(i)).strip();
            if (entryJson.startsWith("[")) {
                sb.append(entryJson, 1, entryJson.lastIndexOf(']')).append('\n');
            } else {
                sb.append(entryJson).append('\n');
            }
        }
        sb.append("]\n");

        final String reportContent;
        try {
            reportContent = runner.run(sb.toString());
        } catch (final SimulationRunnerException e) {
            throw new IllegalStateException("Simulation execution failed: " + e.getMessage(), e);
        }

        return saveCtrl.saveSimulation(
                areaCode, start, end, thresholdValue, thresholdUnit,
                "reports/simulation_report_" + areaCode + ".txt", reportContent);
    }
}
