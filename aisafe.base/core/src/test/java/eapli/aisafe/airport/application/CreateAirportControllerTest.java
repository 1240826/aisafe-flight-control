package eapli.aisafe.airport.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.aisafe.airport.domain.Elevation;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateAirportControllerTest {

    private AuthorizationService authz;
    private AirportRepository repo;
    private AirControlAreaRepository acaRepo;
    private AirControlArea mockAca;
    private CreateAirportController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AirportRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        mockAca = mock(AirControlArea.class);
        // default: ACA exists and coordinates are inside
        when(acaRepo.ofIdentity(any(AreaCode.class))).thenReturn(Optional.of(mockAca));
        when(mockAca.containsCoordinates(anyDouble(), anyDouble())).thenReturn(true);
        controller = new CreateAirportController(authz, repo, acaRepo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureCreateAirportSavesAirport() {
        // Arrange
        final Airport expected = new Airport(
                AirportIATA.valueOf("LIS"),
                AirportICAO.valueOf("LPPT"),
                "Humberto Delgado", "Lisboa", "Portugal",
                38.7667, -9.1333,
                new Elevation(113.0, "m"),
                AreaCode.valueOf("LPPC"));
        when(repo.save(any(Airport.class))).thenReturn(expected);

        // Act
        final Airport result = controller.createAirport(
                "LIS", "LPPT", "Humberto Delgado", "Lisboa", "Portugal",
                38.7667, -9.1333, 113.0, "m", "LPPC");

        // Assert
        verify(repo).save(any(Airport.class));
        assertNotNull(result);
    }

    @Test
    void ensureCreateAirportChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(Airport.class));

        // Act
        controller.createAirport(
                "LIS", "LPPT", "Humberto Delgado", "Lisboa", "Portugal",
                38.7667, -9.1333, 113.0, "m", "LPPC");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllAirControlAreasDelegatesToAcaRepo() {
        // Arrange
        when(acaRepo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<AirControlArea> result = controller.allAirControlAreas();

        // Assert
        verify(acaRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllAirportsDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<Airport> result = controller.allAirports();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureCreateAirportWithBlankNameThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createAirport(
                        "LIS", "LPPT", "", "Lisboa", "Portugal",
                        38.7667, -9.1333, 113.0, "m", "LPPC"),
                "Blank airport name must be rejected");
    }

    @Test
    void ensureCreateAirportWithBlankCountryThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createAirport(
                        "LIS", "LPPT", "Humberto Delgado", "Lisboa", "",
                        38.7667, -9.1333, 113.0, "m", "LPPC"),
                "Blank country must be rejected");
    }

    @Test
    void ensureCreateAirportWithNonExistentAcaThrows() {
        when(acaRepo.ofIdentity(any(AreaCode.class))).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> controller.createAirport(
                        "LIS", "LPPT", "Humberto Delgado", "Lisboa", "Portugal",
                        38.7667, -9.1333, 113.0, "m", "UNKNOWN"),
                "Non-existent ACA must be rejected");
    }

    @Test
    void ensureCreateAirportWithCoordinatesOutsideAcaThrows() {
        when(mockAca.containsCoordinates(anyDouble(), anyDouble())).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> controller.createAirport(
                        "LIS", "LPPT", "Humberto Delgado", "Lisboa", "Portugal",
                        38.7667, -9.1333, 113.0, "m", "LPPC"),
                "Coordinates outside ACA must be rejected");
    }
}
