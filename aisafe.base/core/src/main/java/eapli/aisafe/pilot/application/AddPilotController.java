package eapli.aisafe.pilot.application;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDate;
import java.util.Set;

/**
 * US075: Add a pilot to the company.
 * A pilot is a system user certified for one or more aircraft models.
 * Base implementation — complete integration with SystemUser by responsible colleague.
 */
@UseCaseController
public class AddPilotController {

    private final AuthorizationService authz;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository modelRepo;

    public AddPilotController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    AddPilotController(final AuthorizationService authz,
                        final PilotRepository pilotRepo,
                        final AirTransportCompanyRepository companyRepo,
                        final AircraftModelRepository modelRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
        this.modelRepo = modelRepo;
    }

    public Iterable<AirTransportCompany> allCompanies() {
        return companyRepo.findAll();
    }

    public Iterable<AircraftModel> allAircraftModels() {
        return modelRepo.findAll();
    }

    public Pilot addPilot(final String licenseNumber, final CompanyIATA company,
                          final Set<AircraftModelCode> certifiedModels,
                          final LocalDate certificationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);

        final var pilotId = PilotId.valueOf(licenseNumber);
        final var pilot = new Pilot(pilotId, company, certifiedModels, certificationDate);
        return pilotRepo.save(pilot);
    }
}
