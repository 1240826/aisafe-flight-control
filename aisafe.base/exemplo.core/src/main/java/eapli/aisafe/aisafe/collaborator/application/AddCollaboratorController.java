package eapli.aisafe.collaborator.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.collaborator.domain.ATCCollaborator;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.FlightControlOperator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.domain.WeatherPerson;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
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
 * Actor: Admin / BackOffice Operator.
 *
 * Creates a SystemUser in the framework, then creates the corresponding
 * Collaborator entity linked to it via @OneToOne.
 */
@UseCaseController
public class AddCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository acaRepo =
            PersistenceContext.repositories().airControlAreas();

    /**
     * US061: Add an ATC Collaborator.
     * Creates a SystemUser with role ATC_COLLABORATOR, then an ATCCollaborator entity.
     */
    public Collaborator addATCCollaborator(final String username, final String password,
                                            final String firstName, final String lastName,
                                            final String email,
                                            final String name, final String position,
                                            final LocalDate clearanceExpiry,
                                            final LocalDate assessmentDate,
                                            final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.ATC_COLLABORATOR);
        final SystemUser su = userSvc.registerNewUser(username, password, firstName, lastName, email, roles);

        final ATCCollaborator collab = new ATCCollaborator(
                su, name, position,
                new SecurityClearance(clearanceExpiry),
                new SkillsAssessment(assessmentDate),
                CompanyIATA.valueOf(companyIata));

        return collaboratorRepo.save(collab);
    }

    /**
     * US061: Add a Flight Control Operator.
     * Creates a SystemUser with role FLIGHT_CONTROL_OPERATOR, then a FlightControlOperator entity.
     */
    public Collaborator addFlightControlOperator(final String username, final String password,
                                                   final String firstName, final String lastName,
                                                   final String email,
                                                   final String name, final String position,
                                                   final LocalDate clearanceExpiry,
                                                   final LocalDate assessmentDate,
                                                   final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final SystemUser su = userSvc.registerNewUser(username, password, firstName, lastName, email, roles);

        final FlightControlOperator fco = new FlightControlOperator(
                su, name, position,
                new SecurityClearance(clearanceExpiry),
                new SkillsAssessment(assessmentDate),
                AreaCode.valueOf(areaCode));

        return collaboratorRepo.save(fco);
    }

    /**
     * US061: Add a Weather Person.
     * Creates a SystemUser with role WEATHER_PERSON, then a WeatherPerson entity.
     */
    public Collaborator addWeatherPerson(final String username, final String password,
                                          final String firstName, final String lastName,
                                          final String email,
                                          final String name, final String position,
                                          final LocalDate clearanceExpiry,
                                          final LocalDate assessmentDate,
                                          final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.WEATHER_PERSON);
        final SystemUser su = userSvc.registerNewUser(username, password, firstName, lastName, email, roles);

        final WeatherPerson wp = new WeatherPerson(
                su, name, position,
                new SecurityClearance(clearanceExpiry),
                new SkillsAssessment(assessmentDate),
                AreaCode.valueOf(areaCode));

        return collaboratorRepo.save(wp);
    }

    public Iterable<eapli.aisafe.company.domain.AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return companyRepo.findAll();
    }

    public Iterable<eapli.aisafe.aircontrolarea.domain.AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return acaRepo.findAll();
    }
}
