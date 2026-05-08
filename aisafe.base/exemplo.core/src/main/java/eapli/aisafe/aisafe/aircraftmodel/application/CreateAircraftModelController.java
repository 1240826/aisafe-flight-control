package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AerodynamicCoefficients;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.AircraftPerformance;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.aircraftmodel.domain.AircraftWeights;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US055 — Create Aircraft Model.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class CreateAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository repo = PersistenceContext.repositories().aircraftModels();
    private final ManufacturerRepository manufacturerRepo = PersistenceContext.repositories().manufacturers();

    /**
     * Create and persist a new AircraftModel.
     */
    public AircraftModel createAircraftModel(final String code, final String name,
                                              final String manufacturerName,
                                              final AircraftType aircraftType,
                                              final Integer maxPassengers,
                                              final double emptyWeight, final double mtow,
                                              final double mzfw, final double maxFuel,
                                              final double serviceCeiling, final double cruiseSpeed,
                                              final double maxRange,
                                              final double wingArea, final double drag, final double lift) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final AircraftModel model = new AircraftModel(
                AircraftModelCode.valueOf(code),
                name,
                ManufacturerName.valueOf(manufacturerName),
                aircraftType,
                maxPassengers,
                new AircraftWeights(emptyWeight, mtow, mzfw, maxFuel),
                new AircraftPerformance(serviceCeiling, cruiseSpeed, maxRange),
                new AerodynamicCoefficients(wingArea, drag, lift));

        return repo.save(model);
    }

    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    public AircraftType[] aircraftTypes() {
        return AircraftType.values();
    }
}
