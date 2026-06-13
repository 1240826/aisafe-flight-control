package eapli.aisafe.aircontrolarea.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.domain.AreaName;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterAirControlAreaControllerTest {

    private AuthorizationService authz;
    private AirControlAreaRepository repo;
    private RegisterAirControlAreaController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AirControlAreaRepository.class);
        controller = new RegisterAirControlAreaController(authz, repo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureRegisterAirControlAreaSavesArea() {
        // Arrange
        final AirControlArea expected = new AirControlArea(
                AreaCode.valueOf("LPPC"),
                new AreaName("Lisboa Oceânico"),
                36.0, 44.0, -25.0, -6.0,
                14000);
        when(repo.save(any(AirControlArea.class))).thenReturn(expected);

        // Act
        final AirControlArea result = controller.registerAirControlArea(
                "LPPC", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, 14000);

        // Assert
        verify(repo).save(any(AirControlArea.class));
        assertNotNull(result);
    }

    @Test
    void ensureRegisterAirControlAreaChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(AirControlArea.class));

        // Act
        controller.registerAirControlArea(
                "LPPC", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, 14000);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllAirControlAreasDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<AirControlArea> result = controller.allAirControlAreas();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllAirControlAreasChecksAuthorization() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        controller.allAirControlAreas();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any(), any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureRegisterAirControlAreaWithNegativeAltitudeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerAirControlArea(
                        "LPPC", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, -1),
                "Negative maxAltitudeMetres must be rejected");
    }

    @Test
    void ensureRegisterAirControlAreaWithBlankCodeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerAirControlArea(
                        "", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0, 14000),
                "Blank area code must be rejected");
    }

    // ── 6-param overload (reads from settings) ──────────────────────────────

    @Test
    void ensureRegisterAirControlAreaWithSixParamsWorks() {
        final AirControlArea expected = new AirControlArea(
                AreaCode.valueOf("LPPC"),
                new AreaName("Lisboa Oceânico"),
                36.0, 44.0, -25.0, -6.0,
                14000);
        when(repo.save(any(AirControlArea.class))).thenReturn(expected);

        final AirControlArea result = controller.registerAirControlArea(
                "LPPC", "Lisboa Oceânico", 36.0, 44.0, -25.0, -6.0);

        assertNotNull(result);
        verify(repo).save(any(AirControlArea.class));
    }

    // ── Overlapping boundaries ──────────────────────────────────────────────

    @Test
    void ensureRegisterAirControlAreaWithOverlappingBoundariesThrows() {
        final AirControlArea existing = mock(AirControlArea.class);
        when(existing.identity()).thenReturn(AreaCode.valueOf("LPPC"));
        when(repo.findOverlapping(36.0, 44.0, -25.0, -6.0))
                .thenReturn(List.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> controller.registerAirControlArea(
                        "NEWA", "New Area", 36.0, 44.0, -25.0, -6.0, 14000),
                "Overlapping boundaries must be rejected");
    }
}
