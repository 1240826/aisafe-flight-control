package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListCompanyFleetControllerTest {

    private AuthorizationService authz;
    private AircraftRepository aircraftRepo;
    private AirTransportCompanyRepository companyRepo;
    private ListCompanyFleetController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        aircraftRepo = mock(AircraftRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        controller = new ListCompanyFleetController(authz, aircraftRepo, companyRepo);
    }

    private Aircraft makeAircraft(final String reg, final String country) {
        return new Aircraft(
                RegistrationNumber.valueOf(reg, country),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
    }

    private AirTransportCompany makeCompany() {
        return new AirTransportCompany(CompanyIATA.valueOf("TP"), CompanyICAO.valueOf("TAP"), "TAP Air Portugal");
    }

    // ── Fleet of company ──────────────────────────────────────────────────────

    @Test
    void ensureFleetOfCompanyDelegatesToRepo() {
        // Arrange
        final List<Aircraft> fleet = List.of(makeAircraft("CS-TUI", "Portugal"));
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(fleet);

        // Act
        final Iterable<Aircraft> result = controller.fleetOfCompany("TP");

        // Assert
        verify(aircraftRepo).findByCompanyId(CompanyIATA.valueOf("TP"));
        assertNotNull(result);
    }

    @Test
    void ensureFleetOfCompanyChecksAuthorization() {
        // Arrange
        when(aircraftRepo.findByCompanyId(any())).thenReturn(List.of());

        // Act
        controller.fleetOfCompany("TP");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── All active aircraft ───────────────────────────────────────────────────

    @Test
    void ensureAllActiveAircraftDelegatesToRepo() {
        // Arrange
        final List<Aircraft> fleet = List.of(makeAircraft("CS-TUI", "Portugal"));
        when(aircraftRepo.findAllActive()).thenReturn(fleet);

        // Act
        final Iterable<Aircraft> result = controller.allActiveAircraft();

        // Assert
        verify(aircraftRepo).findAllActive();
        assertNotNull(result);
    }

    @Test
    void ensureAllActiveAircraftChecksAuthorization() {
        // Arrange — US072: ATC_COLLABORATOR and FLIGHT_CONTROL_OPERATOR must both be allowed
        when(aircraftRepo.findAllActive()).thenReturn(List.of());

        // Act
        controller.allActiveAircraft();

        // Assert — two roles must be passed (ATC_COLLABORATOR, FLIGHT_CONTROL_OPERATOR)
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── All companies ─────────────────────────────────────────────────────────

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of(makeCompany()));

        // Act
        final Iterable<AirTransportCompany> result = controller.allCompanies();

        // Assert
        verify(companyRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllCompaniesChecksAuthorization() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of());

        // Act
        controller.allCompanies();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }
}
