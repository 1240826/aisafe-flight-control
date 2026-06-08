package eapli.aisafe.report.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.YearMonth;

@UseCaseController
public class GenerateMonthlyReportController {

    private final AuthorizationService authz;
    private final CollaboratorRepository collaboratorRepo;
    private final MonthlyReportDataProvider provider;

    public GenerateMonthlyReportController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().collaborators(),
                new DatabaseMonthlyReportDataProvider(
                        PersistenceContext.repositories().flights(),
                        PersistenceContext.repositories().weatherData(),
                        PersistenceContext.repositories().pilots(),
                        PersistenceContext.repositories().aircraft()));
    }

    GenerateMonthlyReportController(final AuthorizationService authz,
                                     final CollaboratorRepository collaboratorRepo,
                                     final MonthlyReportDataProvider provider) {
        this.authz = authz;
        this.collaboratorRepo = collaboratorRepo;
        this.provider = provider;
    }

    public MonthlyReport generateForMonth(final YearMonth period) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final AreaCode areaCode = areaCodeOfCurrentUser();
        return provider.generateForMonth(period, areaCode);
    }

    private AreaCode areaCodeOfCurrentUser() {
        final var user = authz.session()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"))
                .authenticatedUser();
        final Collaborator collab = collaboratorRepo.findBySystemUser(user)
                .orElseThrow(() -> new IllegalStateException(
                        "Current user has no collaborator profile"));
        return collab.areaCode();
    }
}
