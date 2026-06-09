package eapli.aisafe.report.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GenerateMonthlyReportControllerTest {

    private static final YearMonth PERIOD = YearMonth.of(2026, 6);
    private static final YearMonth PERIOD_JAN = YearMonth.of(2026, 1);
    private static final YearMonth PERIOD_NEXT_YEAR = YearMonth.of(2027, 3);
    private static final AreaCode FCO_AREA = new AreaCode("LPPC");
    private static final AreaCode OTHER_AREA = new AreaCode("WEFIR");

    private AuthorizationService authz;
    private CollaboratorRepository collaboratorRepo;
    private MonthlyReportDataProvider provider;
    private GenerateMonthlyReportController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        collaboratorRepo = mock(CollaboratorRepository.class);
        provider = mock(MonthlyReportDataProvider.class);
        controller = new GenerateMonthlyReportController(authz, collaboratorRepo, provider);

        final SystemUser currentUser = dummyUser("fco1");
        final UserSession session = new UserSession(currentUser);
        when(authz.session()).thenReturn(Optional.of(session));

        final Collaborator collab = mock(Collaborator.class);
        when(collab.areaCode()).thenReturn(FCO_AREA);
        when(collaboratorRepo.findBySystemUser(currentUser)).thenReturn(Optional.of(collab));
    }

    private static SystemUser dummyUser(final String username) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "Password1", "Test", "User", username + "@aisafe.pt")
                .withRoles(Role.valueOf("FLIGHT_CONTROL_OPERATOR"))
                .build();
    }

    private static SystemUser dummyUserWithRole(final String username, final String role) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "Password1", "Test", "User", username + "@aisafe.pt")
                .withRoles(Role.valueOf(role))
                .build();
    }

    @Test
    void ensureDelegatesToProviderWithAreaCode() {
        final MonthlyReport expected = mock(MonthlyReport.class);
        when(provider.generateForMonth(PERIOD, FCO_AREA)).thenReturn(expected);

        final MonthlyReport result = controller.generateForMonth(PERIOD);

        assertSame(expected, result);
        verify(provider).generateForMonth(PERIOD, FCO_AREA);
    }

    @Test
    void ensureAuthorizationIsChecked() {
        when(provider.generateForMonth(any(), any())).thenReturn(mock(MonthlyReport.class));

        controller.generateForMonth(PERIOD);

        verify(authz).ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
    }

    @Test
    void ensureThrowsWhenNoAuthenticatedUser() {
        when(authz.session()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> controller.generateForMonth(PERIOD));
    }

    @Test
    void ensureThrowsWhenNoCollaboratorProfile() {
        final SystemUser user = dummyUser("orphan");
        final UserSession session = new UserSession(user);
        when(authz.session()).thenReturn(Optional.of(session));
        when(collaboratorRepo.findBySystemUser(user)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> controller.generateForMonth(PERIOD));
    }

    @Test
    void ensureGeneratesForDifferentPeriod() {
        final MonthlyReport expected = mock(MonthlyReport.class);
        when(provider.generateForMonth(PERIOD_JAN, FCO_AREA)).thenReturn(expected);

        final MonthlyReport result = controller.generateForMonth(PERIOD_JAN);

        assertSame(expected, result);
        verify(provider).generateForMonth(PERIOD_JAN, FCO_AREA);
    }

    @Test
    void ensureGeneratesForFuturePeriod() {
        final MonthlyReport expected = mock(MonthlyReport.class);
        when(provider.generateForMonth(PERIOD_NEXT_YEAR, FCO_AREA)).thenReturn(expected);

        final MonthlyReport result = controller.generateForMonth(PERIOD_NEXT_YEAR);

        assertSame(expected, result);
        verify(provider).generateForMonth(PERIOD_NEXT_YEAR, FCO_AREA);
    }

    @Test
    void ensureUserWithDifferentAreaCodeUsesTheirOwnArea() {
        final SystemUser otherUser = dummyUser("fco2");
        final UserSession otherSession = new UserSession(otherUser);
        when(authz.session()).thenReturn(Optional.of(otherSession));

        final Collaborator otherCollab = mock(Collaborator.class);
        when(otherCollab.areaCode()).thenReturn(OTHER_AREA);
        when(collaboratorRepo.findBySystemUser(otherUser)).thenReturn(Optional.of(otherCollab));

        final MonthlyReport expected = mock(MonthlyReport.class);
        when(provider.generateForMonth(PERIOD, OTHER_AREA)).thenReturn(expected);

        final MonthlyReport result = controller.generateForMonth(PERIOD);

        assertSame(expected, result);
        verify(provider).generateForMonth(PERIOD, OTHER_AREA);
    }

    @Test
    void ensureProviderIsCalledExactlyOnce() {
        when(provider.generateForMonth(any(), any())).thenReturn(mock(MonthlyReport.class));

        controller.generateForMonth(PERIOD);

        verify(provider, times(1)).generateForMonth(any(), any());
    }

    @Test
    void ensureProviderExceptionPropagates() {
        when(provider.generateForMonth(any(), any()))
                .thenThrow(new RuntimeException("Data unavailable"));

        assertThrows(RuntimeException.class, () -> controller.generateForMonth(PERIOD));
    }

    @Test
    void ensureCollaboratorRepoIsQueriedWithCorrectUser() {
        when(provider.generateForMonth(any(), any())).thenReturn(mock(MonthlyReport.class));

        controller.generateForMonth(PERIOD);

        final var session = authz.session().orElseThrow();
        verify(collaboratorRepo).findBySystemUser(session.authenticatedUser());
    }
}
