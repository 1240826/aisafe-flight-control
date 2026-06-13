package eapli.aisafe.enginemodel.application;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.domain.EngineName;
import eapli.aisafe.enginemodel.domain.Power;
import eapli.aisafe.enginemodel.domain.Thrust;
import eapli.aisafe.enginemodel.domain.TSFC;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateEngineModelControllerTest {

    private AuthorizationService authz;
    private EngineModelRepository repo;
    private ManufacturerRepository manufacturerRepo;
    private CreateEngineModelController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(EngineModelRepository.class);
        manufacturerRepo = mock(ManufacturerRepository.class);
        controller = new CreateEngineModelController(authz, repo, manufacturerRepo);

        // By default no duplicate found
        when(repo.findByNameAndManufacturer(any(), any())).thenReturn(Optional.empty());
    }

    private EngineModel makeEngineModel() {
        return new EngineModel(
                EngineModelCode.valueOf("CFM56"),
                EngineName.valueOf("CFM International CFM56"),
                "CFM International",
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.36, "lb/(lbf.h)"));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureCreateEngineModelSavesModel() {
        // Arrange
        when(repo.save(any(EngineModel.class))).thenReturn(makeEngineModel());

        // Act
        final EngineModel result = controller.createEngineModel(
                "CFM56", "CFM International CFM56", "CFM International",
                "Jet-A1", MotorizationType.TURBOFAN,
                27000.0, "kW",
                27000.0, "kN",
                5000.0,
                0.36, "lb/(lbf.h)");

        // Assert
        verify(repo).save(any(EngineModel.class));
        assertNotNull(result);
    }

    @Test
    void ensureCreateEngineModelChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(EngineModel.class));

        // Act
        controller.createEngineModel(
                "CFM56", "CFM International CFM56", "CFM International",
                "Jet-A1", MotorizationType.TURBOFAN,
                27000.0, "kW",
                27000.0, "kN",
                5000.0,
                0.36, "lb/(lbf.h)");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureCreateEngineModelWithDuplicateNameAndManufacturerThrows() {
        // Arrange
        when(repo.findByNameAndManufacturer("CFM International CFM56", "CFM International"))
                .thenReturn(Optional.of(makeEngineModel()));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.createEngineModel(
                        "CFM56-NEW", "CFM International CFM56", "CFM International",
                        "Jet-A1", MotorizationType.TURBOFAN,
                        27000.0, "kW",
                        27000.0, "kN",
                        5000.0,
                        0.36, "lb/(lbf.h)"),
                "Duplicate engine name + manufacturer must be rejected (US056)");
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllEngineModelsDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<EngineModel> result = controller.allEngineModels();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureMotorizationTypesReturnsNonEmpty() {
        // Act
        final MotorizationType[] types = controller.motorizationTypes();

        // Assert
        assertNotNull(types);
        assertTrue(types.length > 0);
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureCreateEngineModelWithBlankFuelTypeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createEngineModel(
                        "CFM56", "CFM International CFM56", "CFM International",
                        "", MotorizationType.TURBOFAN,
                        27000.0, "kW",
                        27000.0, "kN",
                        5000.0,
                        0.36, "lb/(lbf.h)"),
                "Blank fuel type must be rejected");
    }

    @Test
    void ensureCreateEngineModelWithNullCodeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createEngineModel(
                        null, "CFM International CFM56", "CFM International",
                        "Jet-A1", MotorizationType.TURBOFAN,
                        27000.0, "kW",
                        27000.0, "kN",
                        5000.0,
                        0.36, "lb/(lbf.h)"),
                "Null engine model code must be rejected");
    }

    @Test
    void ensureCreateEngineModelWithBlankManufacturerThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.createEngineModel(
                        "CFM56", "CFM International CFM56", "",
                        "Jet-A1", MotorizationType.TURBOFAN,
                        27000.0, "kW",
                        27000.0, "kN",
                        5000.0,
                        0.36, "lb/(lbf.h)"),
                "Blank manufacturer name must be rejected");
    }

    @Test
    void ensureAllManufacturersDelegatesToRepo() {
        when(manufacturerRepo.findAll()).thenReturn(List.of());
        final var result = controller.allManufacturers();
        verify(manufacturerRepo).findAll();
        assertNotNull(result);
    }
}
