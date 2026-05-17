package eapli.aisafe.collaborator.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller for US061 — Add Customer's Collaborator.
 * Actor: BackOffice Operator.
 *
 * Creates a SystemUser in the framework, then creates the corresponding
 * Collaborator entity linked to it via @OneToOne.
 * Also creates a UserSecurityProfile so the clearance check in AISafeBackoffice works.
 */
@UseCaseController
public class AddCollaboratorController {

    private final AuthorizationService authz;
    private final UserManagementService userSvc;
    private final CollaboratorRepository collaboratorRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AirControlAreaRepository acaRepo;
    private final UserSecurityProfileRepository profileRepo;

    /** Production constructor — uses framework registries. */
    public AddCollaboratorController() {
        this(AuthzRegistry.authorizationService(),
                AuthzRegistry.userService(),
                PersistenceContext.repositories().collaborators(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().userSecurityProfiles());
    }

    /** Testing constructor — allows injecting mocks. */
    AddCollaboratorController(final AuthorizationService authz,
                              final UserManagementService userSvc,
                              final CollaboratorRepository collaboratorRepo,
                              final AirTransportCompanyRepository companyRepo,
                              final AirControlAreaRepository acaRepo,
                              final UserSecurityProfileRepository profileRepo) {
        this.authz = authz;
        this.userSvc = userSvc;
        this.collaboratorRepo = collaboratorRepo;
        this.companyRepo = companyRepo;
        this.acaRepo = acaRepo;
        this.profileRepo = profileRepo;
    }

    /**
     * US061: Add an ATC Collaborator.
     * Creates a SystemUser with role ATC_COLLABORATOR, then a Collaborator entity (ATC type),
     * and a UserSecurityProfile so the clearance check passes.
     */
    public Collaborator addATCCollaborator(final String username, final String password,
                                            final String firstName, final String lastName,
                                            final String email,
                                            final String name, final String position,
                                            final LocalDate clearanceExpiry,
                                            final LocalDate assessmentDate,
                                            final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.ATC_COLLABORATOR);
        final SystemUser su = userSvc.registerNewUser(username, password, firstName, lastName, email, roles);

        // AC 031.7-equivalent: persist security clearance profile for collaborator's SystemUser
        profileRepo.save(new UserSecurityProfile(username, clearanceExpiry));

        final Collaborator collab = Collaborator.ofATC(
                su, name, position,
                new SecurityClearance(clearanceExpiry),
                new SkillsAssessment(assessmentDate),
                CompanyIATA.valueOf(companyIata));

        return collaboratorRepo.save(collab);
    }

    /**
     * US061: Add a Flight Control Operator.
     * Creates a SystemUser with role FLIGHT_CONTROL_OPERATOR, then a Collaborator entity (FCO type),
     * and a UserSecurityProfile so the clearance check passes.
     */
    public Collaborator addFlightControlOperator(final String username, final String password,
                                                   final String firstName, final String lastName,
                                                   final String email,
                                                   final String name, final String position,
                                                   final LocalDate clearanceExpiry,
                                                   final LocalDate assessmentDate,
                                                   final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final SystemUser su = userSvc.registerNewUser(username, password, firstName, lastName, email, roles);

        // AC 031.7-equivalent: persist security clearance profile for collaborator's SystemUser
        profileRepo.save(new UserSecurityProfile(username, clearanceExpiry));

        final Collaborator fco = Collaborator.ofFlightControlOperator(
                su, name, position,
                new SecurityClearance(clearanceExpiry),
                new SkillsAssessment(assessmentDate),
                AreaCode.valueOf(areaCode));

        return collaboratorRepo.save(fco);
    }

    /**
     * US061: Add a Weather Person.
     * Creates a SystemUser with role WEATHER_PERSON, then a Collaborator entity (WEATHER type),
     * and a UserSecurityProfile so the clearance check passes.
     */
    public Collaborator addWeatherPerson(final String username, final String password,
                                          final String firstName, final String lastName,
                                          final String email,
                                          final String name, final String position,
                                          final LocalDate clearanceExpiry,
                                          final LocalDate assessmentDate,
                                          final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.WEATHER_PERSON);
        final SystemUser su = userSvc.registerNewUser(username, password, firstName, lastName, email, roles);

        // AC 031.7-equivalent: persist security clearance profile for collaborator's SystemUser
        profileRepo.save(new UserSecurityProfile(username, clearanceExpiry));

        final Collaborator wp = Collaborator.ofWeatherPerson(
                su, name, position,
                new SecurityClearance(clearanceExpiry),
                new SkillsAssessment(assessmentDate),
                AreaCode.valueOf(areaCode));

        return collaboratorRepo.save(wp);
    }

    public Iterable<eapli.aisafe.company.domain.AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return companyRepo.findAll();
    }

    public Iterable<eapli.aisafe.aircontrolarea.domain.AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return acaRepo.findAll();
    }
}
