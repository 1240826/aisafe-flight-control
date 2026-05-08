package eapli.aisafe.aircontrolarea.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.domain.AreaName;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.Application;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US050 — Register Air Control Area.
 * Actor: Admin / BackOffice Operator.
 *
 * AC 050.7: maxAltitudeMetres must not be hardcoded — read from
 * application settings key "aisafe.aca.maxAltitudeMetres" (default 14000 m).
 */
@UseCaseController
public class RegisterAirControlAreaController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirControlAreaRepository repo =
            PersistenceContext.repositories().airControlAreas();

    /**
     * Register a new Air Control Area using the configured maximum altitude.
     * maxAltitudeMetres is read from application settings (AC 050.7).
     */
    public AirControlArea registerAirControlArea(final String code, final String name,
                                                  final double minLat, final double maxLat,
                                                  final double minLon, final double maxLon) {
        final String prop = Application.settings().getProperty("aisafe.aca.maxAltitudeMetres");
        final int maxAltitudeMetres = (prop != null && !prop.isBlank())
                ? Integer.parseInt(prop.trim())
                : 14000;
        return registerAirControlArea(code, name, minLat, maxLat, minLon, maxLon, maxAltitudeMetres);
    }

    /**
     * Register a new Air Control Area with an explicit maximum altitude.
     * Used by bootstrappers and tests where the altitude is known.
     */
    public AirControlArea registerAirControlArea(final String code, final String name,
                                                  final double minLat, final double maxLat,
                                                  final double minLon, final double maxLon,
                                                  final int maxAltitudeMetres) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final AirControlArea aca = new AirControlArea(
                AreaCode.valueOf(code),
                new AreaName(name),
                minLat, maxLat, minLon, maxLon,
                maxAltitudeMetres);

        return repo.save(aca);
    }

    /** List all registered air control areas. */
    public Iterable<AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR, AISafeRoles.ATC_COLLABORATOR);
        return repo.findAll();
    }
}
