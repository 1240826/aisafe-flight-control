package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AerodynamicCoefficients;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.AircraftPerformance;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.aircraftmodel.domain.AircraftWeights;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateAircraftModelControllerTest {

    private AuthorizationService authz;
    private AircraftModelRepository repo;
    private ManufacturerRepository manufacturerRepo;
    private CreateAircraftModelController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AircraftModelRepository.class);
        manufacturerRepo = mock(ManufacturerRepository.class);
        controller = new CreateAircraftModelController(authz, repo, manufacturerRepo);
    }

    private AircraftModel makeModel() {
        return new AircraftModel(
                AircraftModelCode.valueOf("B737"),
                "Boeing 737-800",
                ManufacturerName.valueOf("Boeing"),
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 71100, 20894),
                new AircraftPerformance(12500, 842, 5765),
                new AerodynamicCoefficients(125.0, 0.026, 1.5));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureCreateAircraftModelSavesModel() {
        // Arrange
        when(repo.save(any(AircraftModel.class))).thenReturn(makeModel());

        // Act
        final AircraftModel result = controller.createAircraftModel(
                "B737", "Boeing 737-800", "Boeing",
                AircraftType.PASSENGER, 189,
                41140, 79016, 71100, 20894,
                12500, 842, 5765,
                125.0, 0.026, 1.5);

        // Assert
        verify(repo).save(any(AircraftModel.class));
        assertNotNull(result);
    }

    @Test
    void ensureCreateAircraftModelChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(AircraftModel.class));

        // Act
        controller.createAircraftModel(
                "B737", "Boeing 737-800", "Boeing",
                AircraftType.PASSENGER, 189,
                41140, 79016, 71100, 20894,
                12500, 842, 5765,
                125.0, 0.026, 1.5);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllAircraftModelsDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<AircraftModel> result = controller.allAircraftModels();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAircraftTypesReturnsNonEmpty() {
        // Act
        final AircraftType[] types = controller.aircraftTypes();

        // Assert
        assertNotNull(types);
        assertTrue(types.length > 0);
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureCreateAircraftModelWithBlankNameThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createAircraftModel(
                        "B737", "", "Boeing",
                        AircraftType.PASSENGER, 189,
                        41140, 79016, 71100, 20894,
                        12500, 842, 5765,
                        125.0, 0.026, 1.5),
                "Blank model name must be rejected");
    }

    @Test
    void ensureCreateAircraftModelWithZeroMaxPassengersThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createAircraftModel(
                        "B737", "Boeing 737-800", "Boeing",
                        AircraftType.PASSENGER, 0,
                        41140, 79016, 71100, 20894,
                        12500, 842, 5765,
                        125.0, 0.026, 1.5),
                "Zero max passengers must be rejected");
    }
}
