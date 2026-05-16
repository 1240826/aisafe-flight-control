package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AerodynamicCoefficients;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.AircraftPerformance;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.aircraftmodel.domain.AircraftWeights;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US055 — Create Aircraft Model.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class CreateAircraftModelController {

    private final AuthorizationService authz;
    private final AircraftModelRepository repo;
    private final ManufacturerRepository manufacturerRepo;

    /** Production constructor — uses framework registries. */
    public CreateAircraftModelController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().manufacturers());
    }

    /** Testing constructor — allows injecting mocks. */
    CreateAircraftModelController(final AuthorizationService authz,
                                  final AircraftModelRepository repo,
                                  final ManufacturerRepository manufacturerRepo) {
        this.authz = authz;
        this.repo = repo;
        this.manufacturerRepo = manufacturerRepo;
    }

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
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

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
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    public Iterable<Manufacturer> allManufacturers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return manufacturerRepo.findAll();
    }

    public AircraftType[] aircraftTypes() {
        return AircraftType.values();
    }
}
