package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddAircraftControllerTest {

    private AuthorizationService authz;
    private AircraftRepository aircraftRepo;
    private AircraftModelRepository modelRepo;
    private AirTransportCompanyRepository companyRepo;
    private AddAircraftController controller;

    /** A mocked AircraftModel that returns null for maxPassengers (no capacity limit). */
    private AircraftModel noLimitModel;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        aircraftRepo = mock(AircraftRepository.class);
        modelRepo = mock(AircraftModelRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        controller = new AddAircraftController(authz, aircraftRepo, modelRepo, companyRepo);

        // Default: aircraft model exists and has no passenger limit
        noLimitModel = mock(AircraftModel.class);
        when(noLimitModel.maxPassengers()).thenReturn(null);
        when(modelRepo.ofIdentity(any())).thenReturn(Optional.of(noLimitModel));
    }

    /** A past registration date used across tests. */
    private static final LocalDate REG_DATE = LocalDate.of(2020, 1, 1);

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureAddAircraftSavesAircraft() {
        // Arrange
        final List<SeatClass> seats = List.of(new SeatClass("Economy", 150));
        final Aircraft expected = new Aircraft(
                RegistrationNumber.valueOf("CS-TUI", "Portugal"),
                AircraftModelCode.valueOf("B737"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(seats),
                REG_DATE);
        when(aircraftRepo.save(any(Aircraft.class))).thenReturn(expected);

        // Act
        final Aircraft result = controller.addAircraft(
                "CS-TUI", "Portugal", "B737", "TP", 2, seats, REG_DATE);

        // Assert
        verify(aircraftRepo).save(any(Aircraft.class));
        assertNotNull(result);
    }

    @Test
    void ensureAddAircraftChecksAuthorization() {
        // Arrange
        final List<SeatClass> seats = List.of(new SeatClass("Economy", 150));
        when(aircraftRepo.save(any())).thenReturn(mock(Aircraft.class));

        // Act
        controller.addAircraft("CS-TUI", "Portugal", "B737", "TP", 2, seats, REG_DATE);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureAddAircraftRejectsExcessPassengerCapacity() {
        // Arrange — model allows max 100 passengers, cabin configured for 200
        final AircraftModel limitedModel = mock(AircraftModel.class);
        when(limitedModel.maxPassengers()).thenReturn(100);
        when(modelRepo.ofIdentity(any())).thenReturn(Optional.of(limitedModel));
        final List<SeatClass> seats = List.of(new SeatClass("Economy", 200));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft("CS-TST", "Portugal", "B737", "TP", 2, seats, REG_DATE),
                "Capacity exceeding model maximum must be rejected");
    }

    @Test
    void ensureAddAircraftAcceptsCapacityWithinLimit() {
        // Arrange — model allows max 200 passengers, cabin configured for 150
        final AircraftModel limitedModel = mock(AircraftModel.class);
        when(limitedModel.maxPassengers()).thenReturn(200);
        when(modelRepo.ofIdentity(any())).thenReturn(Optional.of(limitedModel));
        final List<SeatClass> seats = List.of(new SeatClass("Economy", 150));
        when(aircraftRepo.save(any(Aircraft.class))).thenReturn(mock(Aircraft.class));

        // Act & Assert — must not throw
        assertDoesNotThrow(
                () -> controller.addAircraft("CS-TST", "Portugal", "B737", "TP", 2, seats, REG_DATE));
    }

    @Test
    void ensureAddAircraftWithUnknownModelThrows() {
        // Arrange
        when(modelRepo.ofIdentity(any())).thenReturn(Optional.empty());
        final List<SeatClass> seats = List.of(new SeatClass("Economy", 150));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft("CS-TUI", "Portugal", "UNKNOWN", "TP", 2, seats, REG_DATE),
                "Unknown aircraft model must be rejected");
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllAircraftModelsDelegatesToRepo() {
        // Arrange
        when(modelRepo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<AircraftModel> result = controller.allAircraftModels();

        // Assert
        verify(modelRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of(
                new AirTransportCompany(CompanyIATA.valueOf("TP"), CompanyICAO.valueOf("TAP"), "TAP Air Portugal")));

        // Act
        final Iterable<AirTransportCompany> result = controller.allCompanies();

        // Assert
        verify(companyRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAddAircraftWithZeroCrewMembersThrows() {
        // Arrange
        final List<SeatClass> seats = List.of(new SeatClass("Economy", 150));

        // Act & Assert — Aircraft constructor rejects 0 crew members
        assertThrows(Exception.class,
                () -> controller.addAircraft("CS-TUI", "Portugal", "B737", "TP", 0, seats, REG_DATE),
                "Zero crew members must be rejected");
    }
}
