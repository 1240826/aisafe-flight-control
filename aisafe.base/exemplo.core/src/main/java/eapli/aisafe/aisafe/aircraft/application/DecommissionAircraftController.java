package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US071 — Decommission Aircraft.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class DecommissionAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository repo = PersistenceContext.repositories().aircraft();

    public Iterable<Aircraft> activeAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAllActive();
    }

    /**
     * Mark the aircraft as DECOMMISSIONED. Irreversible.
     *
     * @param regNumber         registration number (e.g. "CS-TUI")
     * @param regCountry        registration country
     * @return the updated Aircraft
     */
    public Aircraft decommissionAircraft(final String regNumber, final String regCountry) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Aircraft aircraft = repo.ofIdentity(RegistrationNumber.valueOf(regNumber, regCountry))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aircraft not found: " + regNumber + " / " + regCountry));

        aircraft.decommission();
        return repo.save(aircraft);
    }
}
