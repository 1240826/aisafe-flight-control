package eapli.aisafe.simulation.application;

import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaveSimulationControllerTest {

    private AuthorizationService authz;
    private SimulationRepository repo;
    private AirControlAreaRepository acaRepo;
    private SaveSimulationController controller;

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 14, 0, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 14, 6, 0);

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(SimulationRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        controller = new SaveSimulationController(authz, repo, acaRepo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureSaveSimulationPersistsSimulation() {
        // Arrange
        when(repo.save(any(Simulation.class))).thenReturn(mock(Simulation.class));

        // Act
        final Simulation result = controller.saveSimulation(
                "LPPC", START, END,
                500.0, "kg/h",
                "/tmp/scomp_output.txt", "SCOMP RESULT: OK");

        // Assert
        verify(repo).save(any(Simulation.class));
        assertNotNull(result);
    }

    @Test
    void ensureSaveSimulationChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(Simulation.class));

        // Act
        controller.saveSimulation(
                "LPPC", START, END,
                500.0, "kg/h",
                "/tmp/scomp_output.txt", "SCOMP RESULT: OK");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllSimulationsDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<Simulation> result = controller.allSimulations();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllSimulationsChecksAuthorization() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        controller.allSimulations();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any(), any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureSaveSimulationWithNullAreaCodeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.saveSimulation(
                        null, START, END,
                        500.0, "kg/h",
                        "/tmp/scomp_output.txt", "SCOMP RESULT: OK"),
                "Null area code must be rejected");
    }

    @Test
    void ensureSaveSimulationWithBlankThresholdUnitThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.saveSimulation(
                        "LPPC", START, END,
                        500.0, "",
                        "/tmp/scomp_output.txt", "SCOMP RESULT: OK"),
                "Blank threshold unit must be rejected");
    }
}
