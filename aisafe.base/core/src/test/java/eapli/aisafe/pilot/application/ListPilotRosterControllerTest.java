package eapli.aisafe.pilot.application;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * US076 — ListPilotRosterController unit tests.
 */
class ListPilotRosterControllerTest {

    private AuthorizationService          authz;
    private PilotRepository               pilotRepo;
    private AirTransportCompanyRepository companyRepo;
    private ListPilotRosterController     controller;

    private static final CompanyIATA COMPANY = CompanyIATA.valueOf("TP");

    @BeforeEach
    void setUp() {
        authz       = mock(AuthorizationService.class);
        pilotRepo   = mock(PilotRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        controller  = new ListPilotRosterController(authz, pilotRepo, companyRepo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureListCompanyPilotsCallsRepo() {
        when(pilotRepo.findByCompany(COMPANY)).thenReturn(List.of());

        controller.listCompanyPilots(COMPANY);

        verify(pilotRepo).findByCompany(COMPANY);
    }

    @Test
    void ensureListActiveCompanyPilotsCallsRepo() {
        when(pilotRepo.findActiveByCompany(COMPANY)).thenReturn(List.of());

        controller.listActiveCompanyPilots(COMPANY);

        verify(pilotRepo).findActiveByCompany(COMPANY);
    }

    @Test
    void ensureAllCompaniesCallsRepo() {
        controller.allCompanies();
        verify(companyRepo).findAll();
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Test
    void ensureListCompanyPilotsChecksAuthorization() {
        when(pilotRepo.findByCompany(any())).thenReturn(List.of());

        controller.listCompanyPilots(COMPANY);

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureListActiveCompanyPilotsChecksAuthorization() {
        when(pilotRepo.findActiveByCompany(any())).thenReturn(List.of());

        controller.listActiveCompanyPilots(COMPANY);

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureAllCompaniesChecksAuthorization() {
        controller.allCompanies();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Returns ───────────────────────────────────────────────────────────────

    @Test
    void ensureListCompanyPilotsReturnsRepoResult() {
        final Pilot p = mock(Pilot.class);
        when(pilotRepo.findByCompany(COMPANY)).thenReturn(List.of(p));

        final Iterable<Pilot> result = controller.listCompanyPilots(COMPANY);

        assertNotNull(result);
        assertTrue(result.iterator().hasNext());
    }

    @Test
    void ensureListActiveCompanyPilotsReturnsEmptyWhenNone() {
        when(pilotRepo.findActiveByCompany(COMPANY)).thenReturn(List.of());

        final Iterable<Pilot> result = controller.listActiveCompanyPilots(COMPANY);

        assertNotNull(result);
        assertFalse(result.iterator().hasNext());
    }
}
