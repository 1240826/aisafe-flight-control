package eapli.aisafe.simulation.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

/**
 * Controller for generating a simulation report for a given Air Control Area.
 * Actor: Flight Control Operator / Admin.
 */
@UseCaseController
public class GenerateSimulationReportController {

    private final AuthorizationService authz;
    private final SimulationRepository repo;
    private final SimulationReportFileWriter writer;

    /** Production constructor - uses framework registries. */
    public GenerateSimulationReportController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().simulations(),
                new SimulationReportFileWriter());
    }

    /** Testing constructor - allows injecting mocks. */
    GenerateSimulationReportController(final AuthorizationService authz,
                                       final SimulationRepository repo,
                                       final SimulationReportFileWriter writer) {
        this.authz = authz;
        this.repo = repo;
        this.writer = writer;
    }

    /** Returns all simulations for selection. */
    public Iterable<Simulation> allSimulations() {
        authz.ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.ADMIN,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return repo.findAll();
    }

    /**
     * Generates a simulation report for the given Air Control Area.
     *
     * @param areaCode code of the ACA (non-null, non-blank)
     * @return path of the generated report file
     * @throws IllegalArgumentException if areaCode is null or blank
     */
    public String generateReport(final String areaCode) {
        if (areaCode == null) {
            throw new IllegalArgumentException("Area code must not be null");
        }
        if (areaCode.isBlank()) {
            throw new IllegalArgumentException("Area code must not be blank");
        }
        authz.ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.ADMIN,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final AreaCode code = AreaCode.valueOf(areaCode);
        final Simulation simulation = StreamSupport
                .stream(repo.findByAreaCode(code).spliterator(), false)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No simulation found for area code: " + areaCode));

        final String outputPath = System.getProperty("java.io.tmpdir")
                + File.separator
                + "simulation_report_" + code + ".txt";

        return writer.writeToFile(simulation, outputPath);
    }
}
