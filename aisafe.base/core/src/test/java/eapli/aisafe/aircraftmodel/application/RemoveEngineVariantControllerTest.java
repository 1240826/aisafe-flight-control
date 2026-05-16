package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AerodynamicCoefficients;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.AircraftPerformance;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.aircraftmodel.domain.AircraftWeights;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoveEngineVariantControllerTest {

    private AuthorizationService authz;
    private AircraftModelRepository repo;
    private AircraftRepository aircraftRepo;
    private RemoveEngineVariantController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AircraftModelRepository.class);
        aircraftRepo = mock(AircraftRepository.class);
        controller = new RemoveEngineVariantController(authz, repo, aircraftRepo);

        // By default, no active aircraft uses any model
        when(aircraftRepo.findActiveByAircraftModelCode(any())).thenReturn(List.of());
    }

    /** Returns a model with TWO variants so removal of one is legal. */
    private AircraftModel makeModelWithTwoVariants() {
        final AircraftModel model = new AircraftModel(
                AircraftModelCode.valueOf("B737"),
                "Boeing 737-800",
                ManufacturerName.valueOf("Boeing"),
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 71100, 20894),
                new AircraftPerformance(12500, 842, 5765),
                new AerodynamicCoefficients(125.0, 0.026, 1.5));
        model.addVariant(EngineModelCode.valueOf("CFM56"), MotorizationType.TURBOFAN);
        model.addVariant(EngineModelCode.valueOf("GE-CF6"), MotorizationType.TURBOFAN);
        return model;
    }

    /** Returns a model with only ONE variant (removal is blocked by domain invariant). */
    private AircraftModel makeModelWithOneVariant() {
        final AircraftModel model = new AircraftModel(
                AircraftModelCode.valueOf("B737"),
                "Boeing 737-800",
                ManufacturerName.valueOf("Boeing"),
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 71100, 20894),
                new AircraftPerformance(12500, 842, 5765),
                new AerodynamicCoefficients(125.0, 0.026, 1.5));
        model.addVariant(EngineModelCode.valueOf("CFM56"), MotorizationType.TURBOFAN);
        return model;
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureRemoveVariantSavesUpdatedModel() {
        // Arrange
        final AircraftModel model = makeModelWithTwoVariants();
        when(repo.ofIdentity(AircraftModelCode.valueOf("B737"))).thenReturn(Optional.of(model));
        when(repo.save(any(AircraftModel.class))).thenReturn(model);

        // Act
        final AircraftModel result = controller.removeVariant("B737", "CFM56");

        // Assert
        verify(repo).save(any(AircraftModel.class));
        assertNotNull(result);
    }

    @Test
    void ensureRemoveVariantChecksAuthorization() {
        // Arrange
        final AircraftModel model = makeModelWithTwoVariants();
        when(repo.ofIdentity(any())).thenReturn(Optional.of(model));
        when(repo.save(any())).thenReturn(model);

        // Act
        controller.removeVariant("B737", "CFM56");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Business rule: aircraft in use ────────────────────────────────────────

    @Test
    void ensureRemoveVariantBlockedWhenActiveAircraftExists() {
        // Arrange — simulate an active aircraft using model B737
        final var activeAircraft = mock(eapli.aisafe.aircraft.domain.Aircraft.class);
        when(aircraftRepo.findActiveByAircraftModelCode(AircraftModelCode.valueOf("B737")))
                .thenReturn(List.of(activeAircraft));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> controller.removeVariant("B737", "CFM56"),
                "Should block variant removal when active aircraft use the model");
    }

    // ── Domain invariant: last variant ────────────────────────────────────────

    @Test
    void ensureRemoveLastVariantThrows() {
        // Arrange
        final AircraftModel model = makeModelWithOneVariant();
        when(repo.ofIdentity(AircraftModelCode.valueOf("B737"))).thenReturn(Optional.of(model));

        // Act & Assert
        assertThrows(Exception.class,
                () -> controller.removeVariant("B737", "CFM56"),
                "Removing the last variant must be rejected (domain invariant)");
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

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureRemoveVariantWithUnknownModelCodeThrows() {
        // Arrange
        when(repo.ofIdentity(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.removeVariant("UNKNOWN", "CFM56"),
                "Unknown aircraft model code must throw");
    }

    @Test
    void ensureRemoveVariantWithUnknownEngineCodeThrows() {
        // Arrange
        final AircraftModel model = makeModelWithTwoVariants();
        when(repo.ofIdentity(AircraftModelCode.valueOf("B737"))).thenReturn(Optional.of(model));

        // Act & Assert
        assertThrows(Exception.class,
                () -> controller.removeVariant("B737", "UNKNOWN_ENGINE"),
                "Unknown engine model code must throw when removing variant");
    }
}
