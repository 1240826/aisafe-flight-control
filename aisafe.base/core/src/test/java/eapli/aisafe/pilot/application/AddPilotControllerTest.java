package eapli.aisafe.pilot.application;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * US075 — AddPilotController unit tests.
 */
class AddPilotControllerTest {

    private AuthorizationService          authz;
    private PilotRepository               pilotRepo;
    private AirTransportCompanyRepository companyRepo;
    private AircraftModelRepository       modelRepo;
    private AddPilotController            controller;

    private static final CompanyIATA         COMPANY  = CompanyIATA.valueOf("TP");
    private static final Set<AircraftModelCode> MODELS = Set.of(AircraftModelCode.valueOf("B738"));
    private static final LocalDate           CERT_DATE = LocalDate.of(2022, 3, 10);

    @BeforeEach
    void setUp() {
        authz       = mock(AuthorizationService.class);
        pilotRepo   = mock(PilotRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        modelRepo   = mock(AircraftModelRepository.class);
        controller  = new AddPilotController(authz, pilotRepo, companyRepo, modelRepo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureAddPilotSavesPilot() {
        // Arrange
        final Pilot expected = new Pilot(PilotId.valueOf("P12345"), COMPANY, MODELS, CERT_DATE);
        when(pilotRepo.save(any(Pilot.class))).thenReturn(expected);

        // Act
        final Pilot result = controller.addPilot("P12345", COMPANY, MODELS, CERT_DATE);

        // Assert
        verify(pilotRepo).save(any(Pilot.class));
        assertNotNull(result);
    }

    @Test
    void ensureAddPilotChecksAuthorization() {
        when(pilotRepo.save(any())).thenReturn(mock(Pilot.class));

        controller.addPilot("P12345", COMPANY, MODELS, CERT_DATE);

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Domain invariants ─────────────────────────────────────────────────────

    @Test
    void ensureAddPilotWithBlankLicenseThrows() {
        assertThrows(Exception.class,
                () -> controller.addPilot("", COMPANY, MODELS, CERT_DATE));
    }

    @Test
    void ensureAddPilotWithInvalidLicenseFormatThrows() {
        // Format must be letter + 4-10 digits
        assertThrows(Exception.class,
                () -> controller.addPilot("123ABC", COMPANY, MODELS, CERT_DATE));
    }

    @Test
    void ensureAddPilotWithEmptyCertifiedModelsThrows() {
        assertThrows(Exception.class,
                () -> controller.addPilot("P12345", COMPANY, Set.of(), CERT_DATE));
    }

    @Test
    void ensureAddPilotWithFutureCertificationDateThrows() {
        final LocalDate future = LocalDate.now().plusDays(1);
        assertThrows(Exception.class,
                () -> controller.addPilot("P12345", COMPANY, MODELS, future));
    }

    @Test
    void ensureAddPilotWithNullCompanyThrows() {
        assertThrows(Exception.class,
                () -> controller.addPilot("P12345", null, MODELS, CERT_DATE));
    }

    // ── Support queries ───────────────────────────────────────────────────────

    @Test
    void ensureAllCompaniesCallsRepo() {
        controller.allCompanies();
        verify(companyRepo).findAll();
    }

    @Test
    void ensureAllAircraftModelsCallsRepo() {
        controller.allAircraftModels();
        verify(modelRepo).findAll();
    }
}
