package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecommissionAircraftControllerTest {

    private AuthorizationService authz;
    private AircraftRepository repo;
    private DecommissionAircraftController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AircraftRepository.class);
        controller = new DecommissionAircraftController(authz, repo);
    }

    private Aircraft makeAircraft(final String regNumber, final String regCountry) {
        return new Aircraft(
                RegistrationNumber.valueOf(regNumber, regCountry),
                AircraftModelCode.valueOf("B737"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 150))),
                LocalDate.of(2019, 3, 20));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureDecommissionAircraftCallsDecommissioning() {
        // Arrange
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(repo.ofIdentity(RegistrationNumber.valueOf("CS-TUI", "Portugal")))
                .thenReturn(Optional.of(aircraft));
        when(repo.save(aircraft)).thenReturn(aircraft);

        // Act
        final Aircraft result = controller.decommissionAircraft("CS-TUI", "Portugal");

        // Assert
        assertFalse(result.isActive(), "Aircraft must be decommissioned after the call");
        verify(repo).save(aircraft);
    }

    @Test
    void ensureDecommissionAircraftChecksAuthorization() {
        // Arrange
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(repo.ofIdentity(RegistrationNumber.valueOf("CS-TUI", "Portugal")))
                .thenReturn(Optional.of(aircraft));
        when(repo.save(aircraft)).thenReturn(aircraft);

        // Act
        controller.decommissionAircraft("CS-TUI", "Portugal");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void ensureDecommissionNotFoundThrowsException() {
        // Arrange
        when(repo.ofIdentity(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.decommissionAircraft("XX-999", "Unknown"),
                "decommissionAircraft must throw IllegalArgumentException when aircraft not found");
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    void ensureActiveAircraftReturnsListFromRepo() {
        // Arrange
        final List<Aircraft> fleet = List.of(makeAircraft("CS-TUI", "Portugal"));
        when(repo.findAllActive()).thenReturn(fleet);

        // Act
        final Iterable<Aircraft> result = controller.activeAircraft();

        // Assert
        verify(repo).findAllActive();
        assertNotNull(result);
    }

    @Test
    void ensureActiveAircraftChecksAuthorization() {
        // Arrange
        when(repo.findAllActive()).thenReturn(List.of());

        // Act
        controller.activeAircraft();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureDecommissioningAlreadyDecommissionedThrows() {
        // Arrange
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        aircraft.decommission(); // already decommissioned
        when(repo.ofIdentity(RegistrationNumber.valueOf("CS-TUI", "Portugal")))
                .thenReturn(Optional.of(aircraft));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> controller.decommissionAircraft("CS-TUI", "Portugal"),
                "Decommissioning an already-decommissioned aircraft must throw IllegalStateException");
    }
}
