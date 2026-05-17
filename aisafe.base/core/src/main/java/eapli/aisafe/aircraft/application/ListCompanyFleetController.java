package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for US072 — List Company Fleet.
 * Actor: Admin / BackOffice Operator / Flight Control Operator / ATC Collaborator.
 */
@UseCaseController
public class ListCompanyFleetController {

    private final AuthorizationService authz;
    private final AircraftRepository aircraftRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository aircraftModelRepo;

    /**
     * Production constructor — uses framework registries.
     */
    public ListCompanyFleetController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    /**
     * Testing constructor — allows injecting mocks.
     */
    ListCompanyFleetController(final AuthorizationService authz,
                               final AircraftRepository aircraftRepo,
                               final AirTransportCompanyRepository companyRepo,
                               final AircraftModelRepository aircraftModelRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.companyRepo = companyRepo;
        this.aircraftModelRepo = aircraftModelRepo;
    }

    /**
     * List the fleet (all aircraft) of a given company.
     */
    public Iterable<Aircraft> fleetOfCompany(final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return aircraftRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
    }

    /**
     * List all active aircraft across all companies.
     */
    public Iterable<Aircraft> allActiveAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return aircraftRepo.findAllActive();
    }

    /**
     * Support method: list all companies for selection.
     */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return companyRepo.findAll();
    }

    /**
     * US072a: filter fleet by model.
     */
    public Iterable<Aircraft> fleetByModel(final String companyIata, final String modelCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return aircraftRepo.findByCompanyIdAndModel(CompanyIATA.valueOf(companyIata),
                AircraftModelCode.valueOf(modelCode));
    }

    /**
     * US072b: filter fleet by maker — filtered in memory (maker is on AircraftModel, not Aircraft).
     */
    public Iterable<Aircraft> fleetByMaker(final String companyIata, final String makerName) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final Iterable<Aircraft> fleet = aircraftRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : fleet) {
            aircraftModelRepo.ofIdentity(a.aircraftModelCode()).ifPresent(m -> {
                if (m.manufacturerName().toLowerCase().contains(makerName.toLowerCase())) {
                    result.add(a);
                }
            });
        }
        return result;
    }

    /**
     * US072c: filter fleet by minimum capacity — filtered in memory.
     */
    public Iterable<Aircraft> fleetByCapacity(final String companyIata, final int minCapacity) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final Iterable<Aircraft> fleet = aircraftRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : fleet) {
            if (a.totalCapacity() == minCapacity) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * US072d: filter fleet by exact age in years.
     */
    public Iterable<Aircraft> fleetByAge(final String companyIata, final int ageYears) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final Iterable<Aircraft> fleet = aircraftRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : fleet) {
            if (a.ageInYears() == ageYears) {
                result.add(a);
            }
        }
        return result;
    }
}