package eapli.aisafe.simulation.application;

import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenerateSimulationReportControllerTest {

    private AuthorizationService authz;
    private SimulationRepository repo;
    private SimulationReportFileWriter writer;
    private GenerateSimulationReportController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(SimulationRepository.class);
        writer = mock(SimulationReportFileWriter.class);
        controller = new GenerateSimulationReportController(authz, repo, writer);
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Test
    void ensureGenerateReportChecksAuthorization() {
        // Arrange
        when(repo.findByAreaCode(any())).thenReturn(List.of(mock(Simulation.class)));
        when(writer.writeToFile(any(), any())).thenReturn("/tmp/report.txt");

        // Act
        controller.generateReport("LPPC");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    @Test
    void ensureAllSimulationsChecksAuthorization() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        controller.allSimulations();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureGenerateReportWithNullAreaCodeThrows() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.generateReport(null),
                "Null area code must be rejected");
    }

    @Test
    void ensureGenerateReportWithBlankAreaCodeThrows() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.generateReport("   "),
                "Blank area code must be rejected");
    }

    // ── Happy path ────────────────────────────────────────────────────────────

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
    void ensureGenerateReportReturnsFilePath() {
        // Arrange
        final Simulation simulation = mock(Simulation.class);
        when(repo.findByAreaCode(any())).thenReturn(List.of(simulation));
        when(writer.writeToFile(any(), any())).thenReturn("/tmp/simulation_report_LPPC.txt");

        // Act
        final String result = controller.generateReport("LPPC");

        // Assert
        assertNotNull(result);
    }
}
