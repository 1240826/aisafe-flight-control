package eapli.aisafe.pilot.application;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * US077 — RemovePilotController unit tests.
 */
class RemovePilotControllerTest {

    private AuthorizationService authz;
    private PilotRepository      pilotRepo;
    private FlightRepository     flightRepo;
    private RemovePilotController controller;

    private static final PilotId     PILOT_ID  = PilotId.valueOf("P12345");
    private static final CompanyIATA COMPANY   = CompanyIATA.valueOf("TP");
    private static final LocalDate   CERT_DATE = LocalDate.of(2022, 3, 10);

    /** A real active Pilot used across tests. */
    private Pilot activePilot;

    @BeforeEach
    void setUp() {
        authz      = mock(AuthorizationService.class);
        pilotRepo  = mock(PilotRepository.class);
        flightRepo = mock(FlightRepository.class);
        controller = new RemovePilotController(authz, pilotRepo, flightRepo);

        activePilot = new Pilot(PILOT_ID, COMPANY,
                Set.of(AircraftModelCode.valueOf("B738")), CERT_DATE);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureDeactivatePilotSavesPilot() {
        // Arrange
        when(pilotRepo.findByLicenseNumber(PILOT_ID)).thenReturn(Optional.of(activePilot));
        when(flightRepo.existsByPilotLicense(PILOT_ID)).thenReturn(false);
        when(pilotRepo.save(activePilot)).thenReturn(activePilot);

        // Act
        final Pilot result = controller.deactivatePilot(PILOT_ID);

        // Assert
        verify(pilotRepo).save(activePilot);
        assertNotNull(result);
        assertFalse(result.isActive());
    }

    @Test
    void ensureDeactivatePilotChecksAuthorization() {
        when(pilotRepo.findByLicenseNumber(PILOT_ID)).thenReturn(Optional.of(activePilot));
        when(flightRepo.existsByPilotLicense(PILOT_ID)).thenReturn(false);
        when(pilotRepo.save(any())).thenReturn(activePilot);

        controller.deactivatePilot(PILOT_ID);

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Business rule: cannot deactivate if flights assigned ─────────────────

    @Test
    void ensureDeactivatePilotWithFlightsAssignedThrows() {
        when(pilotRepo.findByLicenseNumber(PILOT_ID)).thenReturn(Optional.of(activePilot));
        when(flightRepo.existsByPilotLicense(PILOT_ID)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> controller.deactivatePilot(PILOT_ID));

        verify(pilotRepo, never()).save(any());
    }

    // ── Business rule: pilot must exist ──────────────────────────────────────

    @Test
    void ensureDeactivatePilotNotFoundThrows() {
        when(pilotRepo.findByLicenseNumber(PILOT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.deactivatePilot(PILOT_ID));
    }

    // ── Business rule: cannot deactivate already inactive ────────────────────

    @Test
    void ensureDeactivateAlreadyInactivePilotThrows() {
        // Deactivate the pilot first (outside the controller)
        activePilot.deactivate();
        when(pilotRepo.findByLicenseNumber(PILOT_ID)).thenReturn(Optional.of(activePilot));
        when(flightRepo.existsByPilotLicense(PILOT_ID)).thenReturn(false);

        assertThrows(Exception.class,
                () -> controller.deactivatePilot(PILOT_ID));
    }

    // ── Support queries ───────────────────────────────────────────────────────

    @Test
    void ensureAllPilotsCallsRepo() {
        controller.allPilots();
        verify(pilotRepo).findAll();
    }

    @Test
    void ensureAllPilotsChecksAuthorization() {
        controller.allPilots();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }
}
