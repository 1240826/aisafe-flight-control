package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for US070 — Add Aircraft to Air Transport Company.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class AddAircraftController {

    private final AuthorizationService authz;
    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;

    /** Production constructor — uses framework registries. */
    public AddAircraftController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    /** Testing constructor — allows injecting mocks. */
    AddAircraftController(final AuthorizationService authz,
                          final AircraftRepository aircraftRepo,
                          final AircraftModelRepository modelRepo,
                          final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
    }

    /**
     * Add a new aircraft to a company's fleet.
     *
     * @param regNumber           registration number (e.g. "CS-TUI")
     * @param regCountry          registration country
     * @param aircraftModelCode   code of the aircraft model
     * @param companyIata         IATA code of the owning company
     * @param crewMembers         number of flight crew members
     * @param seatClasses         list of seat classes (className, seats)
     * @param registrationDate    date of first registration — must not be in the future
     * @return the saved Aircraft
     */
    public Aircraft addAircraft(final String regNumber, final String regCountry,
                                 final String aircraftModelCode, final String companyIata,
                                 final int crewMembers,
                                 final List<SeatClass> seatClasses,
                                 final LocalDate registrationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);

        final CabinConfiguration cabin = new CabinConfiguration(seatClasses);

        // AC 070: total cabin capacity must not exceed the aircraft model's maximum passengers
        final var model = modelRepo.ofIdentity(AircraftModelCode.valueOf(aircraftModelCode))
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelCode));
        if (model.maxPassengers() != null && cabin.totalCapacity() > model.maxPassengers()) {
            throw new IllegalArgumentException(
                    "Cabin configuration total capacity (" + cabin.totalCapacity()
                    + ") exceeds aircraft model maximum passengers (" + model.maxPassengers() + ")");
        }

        final Aircraft aircraft = new Aircraft(
                RegistrationNumber.valueOf(regNumber, regCountry),
                AircraftModelCode.valueOf(aircraftModelCode),
                CompanyIATA.valueOf(companyIata),
                crewMembers,
                cabin,
                registrationDate);

        return aircraftRepo.save(aircraft);
    }

    public Iterable<eapli.aisafe.aircraftmodel.domain.AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return modelRepo.findAll();
    }

    public Iterable<eapli.aisafe.company.domain.AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }
}
