package eapli.aisafe.simulation.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.simulation.domain.SafetyThreshold;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.domain.SimulationReport;
import eapli.aisafe.simulation.domain.SimulationTimeRange;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDateTime;

/**
 * Controller for saving a SCOMP simulation result.
 * Actor: Flight Control Operator / Admin.
 *
 * Usage: read the SCOMP output file, supply its path and content, and
 * call {@link #saveSimulation} to persist the result.
 */
@UseCaseController
public class SaveSimulationController {

    private final AuthorizationService authz;
    private final SimulationRepository repo;
    private final AirControlAreaRepository acaRepo;

    /** Production constructor - uses framework registries. */
    public SaveSimulationController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().simulations(),
                PersistenceContext.repositories().airControlAreas());
    }

    /** Testing constructor - allows injecting mocks. */
    SaveSimulationController(final AuthorizationService authz,
                              final SimulationRepository repo,
                              final AirControlAreaRepository acaRepo) {
        this.authz = authz;
        this.repo = repo;
        this.acaRepo = acaRepo;
    }

    /**
     * Save the SCOMP simulation output for a given Air Control Area.
     *
     * @param areaCode       code of the ACA being simulated
     * @param startDateTime  start of the simulation time window
     * @param endDateTime    end of the simulation time window
     * @param thresholdValue safety threshold value (positive)
     * @param thresholdUnit  safety threshold unit (non-blank)
     * @param reportFilePath path of the SCOMP output file
     * @param reportContent  full text content of the SCOMP output file
     * @return the persisted Simulation
     */
    public Simulation saveSimulation(final String areaCode,
                                     final LocalDateTime startDateTime,
                                     final LocalDateTime endDateTime,
                                     final double thresholdValue,
                                     final String thresholdUnit,
                                     final String reportFilePath,
                                     final String reportContent) {
        authz.ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.ADMIN,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final Simulation simulation = new Simulation(
                AreaCode.valueOf(areaCode),
                new SimulationTimeRange(startDateTime, endDateTime),
                new SafetyThreshold(thresholdValue, thresholdUnit),
                new SimulationReport(reportFilePath, reportContent));

        return repo.save(simulation);
    }

    /** Support method: list ACAs for selection. */
    public Iterable<AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return acaRepo.findAll();
    }

    /** List all simulations (for display). */
    public Iterable<Simulation> allSimulations() {
        authz.ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.ADMIN,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return repo.findAll();
    }
}
