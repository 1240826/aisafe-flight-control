package eapli.aisafe.pilot.application;

import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * US077: Remove (deactivate) a pilot.
 * Cannot deactivate a pilot with flight plans assigned.
 * Base implementation — complete validation by responsible colleague.
 */
@UseCaseController
public class RemovePilotController {

    private final AuthorizationService authz;
    private final PilotRepository pilotRepo;
    private final FlightRepository flightRepo;

    public RemovePilotController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().flights());
    }

    RemovePilotController(final AuthorizationService authz,
                           final PilotRepository pilotRepo,
                           final FlightRepository flightRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.flightRepo = flightRepo;
    }

    /**
     * Deactivates a pilot. Business rule:
     * Cannot deactivate if the pilot has any flight plans assigned.
     *
     * @param pilotId the pilot's license number
     * @return the deactivated pilot
     * @throws IllegalStateException if pilot has flights assigned
     */
    public List<Pilot> allPilots() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        final List<Pilot> result = new ArrayList<>();
        pilotRepo.findAll().forEach(result::add);
        return result;
    }

    public Pilot deactivatePilot(final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);

        final var pilot = pilotRepo.findByLicenseNumber(pilotId)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + pilotId));

        if (flightRepo.existsByPilotLicense(pilotId)) {
            throw new IllegalStateException(
                    "Cannot deactivate pilot " + pilotId + " — flight plans are assigned");
        }

        pilot.deactivate();
        return pilotRepo.save(pilot);
    }
}
