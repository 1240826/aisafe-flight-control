package eapli.aisafe.manufacturer.application;

import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterManufacturerControllerTest {

    private AuthorizationService authz;
    private ManufacturerRepository repo;
    private RegisterManufacturerController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(ManufacturerRepository.class);
        controller = new RegisterManufacturerController(authz, repo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureRegisterManufacturerSavesManufacturer() {
        // Arrange
        final Manufacturer expected = new Manufacturer("Boeing", "USA");
        when(repo.save(any(Manufacturer.class))).thenReturn(expected);

        // Act
        final Manufacturer result = controller.registerManufacturer("Boeing", "USA");

        // Assert
        verify(repo).save(any(Manufacturer.class));
        assertNotNull(result);
    }

    @Test
    void ensureRegisterManufacturerChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(Manufacturer.class));

        // Act
        controller.registerManufacturer("Boeing", "USA");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllManufacturersDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of(new Manufacturer("Boeing", "USA")));

        // Act
        final Iterable<Manufacturer> result = controller.allManufacturers();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllManufacturersChecksAuthorization() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        controller.allManufacturers();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureRegisterManufacturerWithBlankCountryThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerManufacturer("Boeing", ""),
                "Blank country must be rejected");
    }

    @Test
    void ensureRegisterManufacturerWithNullNameThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerManufacturer(null, "USA"),
                "Null manufacturer name must be rejected");
    }

    // ── Remove manufacturer ─────────────────────────────────────────────────

    @Test
    void ensureRemoveManufacturerDelegatesToRepo() {
        controller.removeManufacturer("Boeing");
        verify(repo).deleteOfIdentity(any());
    }

    @Test
    void ensureRemoveManufacturerChecksAuthorization() {
        controller.removeManufacturer("Boeing");
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }
}
