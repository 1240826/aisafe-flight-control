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
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.List;

/**
 * Controller for US070 — Add Aircraft to Air Transport Company.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class AddAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
    private final AircraftModelRepository modelRepo = PersistenceContext.repositories().aircraftModels();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();

    /**
     * Add a new aircraft to a company's fleet.
     *
     * @param regNumber           registration number (e.g. "CS-TUI")
     * @param regCountry          registration country
     * @param aircraftModelCode   code of the aircraft model
     * @param companyIata         IATA code of the owning company
     * @param crewMembers         number of flight crew members
     * @param seatClasses         list of seat classes (className, seats)
     * @return the saved Aircraft
     */
    public Aircraft addAircraft(final String regNumber, final String regCountry,
                                 final String aircraftModelCode, final String companyIata,
                                 final int crewMembers,
                                 final List<SeatClass> seatClasses) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final CabinConfiguration cabin = new CabinConfiguration(seatClasses);

        final Aircraft aircraft = new Aircraft(
                RegistrationNumber.valueOf(regNumber, regCountry),
                AircraftModelCode.valueOf(aircraftModelCode),
                CompanyIATA.valueOf(companyIata),
                crewMembers,
                cabin);

        return aircraftRepo.save(aircraft);
    }

    public Iterable<eapli.aisafe.aircraftmodel.domain.AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return modelRepo.findAll();
    }

    public Iterable<eapli.aisafe.company.domain.AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return companyRepo.findAll();
    }
}
