package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AerodynamicCoefficients;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.AircraftPerformance;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.aircraftmodel.domain.AircraftWeights;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddEngineVariantControllerTest {

    private AuthorizationService authz;
    private AircraftModelRepository aircraftModelRepo;
    private EngineModelRepository engineModelRepo;
    private AddEngineVariantController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        aircraftModelRepo = mock(AircraftModelRepository.class);
        engineModelRepo = mock(EngineModelRepository.class);
        controller = new AddEngineVariantController(authz, aircraftModelRepo, engineModelRepo);
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
    void ensureAddVariantSavesUpdatedModel() {
        // Arrange
        final AircraftModel model = makeModel();
        when(aircraftModelRepo.ofIdentity(AircraftModelCode.valueOf("B737")))
                .thenReturn(Optional.of(model));
        when(aircraftModelRepo.save(any(AircraftModel.class))).thenReturn(model);

        // Act
        final AircraftModel result = controller.addVariant("B737", "CFM56", MotorizationType.TURBOFAN);

        // Assert
        verify(aircraftModelRepo).save(any(AircraftModel.class));
        assertNotNull(result);
    }

    @Test
    void ensureAddVariantChecksAuthorization() {
        // Arrange
        final AircraftModel model = makeModel();
        when(aircraftModelRepo.ofIdentity(any())).thenReturn(Optional.of(model));
        when(aircraftModelRepo.save(any())).thenReturn(model);

        // Act
        controller.addVariant("B737", "CFM56", MotorizationType.TURBOFAN);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllAircraftModelsDelegatesToRepo() {
        // Arrange
        when(aircraftModelRepo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<AircraftModel> result = controller.allAircraftModels();

        // Assert
        verify(aircraftModelRepo).findAll();
        assertNotNull(result);
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureAddVariantWithUnknownModelCodeThrows() {
        // Arrange
        when(aircraftModelRepo.ofIdentity(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.addVariant("UNKNOWN", "CFM56", MotorizationType.TURBOFAN),
                "Unknown aircraft model code must throw");
    }

    @Test
    void ensureAddDuplicateVariantThrows() {
        // Arrange
        final AircraftModel model = makeModel();
        model.addVariant(
                eapli.aisafe.enginemodel.domain.EngineModelCode.valueOf("CFM56"),
                MotorizationType.TURBOFAN);
        when(aircraftModelRepo.ofIdentity(AircraftModelCode.valueOf("B737")))
                .thenReturn(Optional.of(model));

        // Act & Assert
        assertThrows(Exception.class,
                () -> controller.addVariant("B737", "CFM56", MotorizationType.TURBOFAN),
                "Duplicate engine variant must be rejected");
    }

    @Test
    void ensureAllEngineModelsDelegatesToRepo() {
        when(engineModelRepo.findAll()).thenReturn(List.of());
        final var result = controller.allEngineModels();
        verify(engineModelRepo).findAll();
        assertNotNull(result);
    }
}
