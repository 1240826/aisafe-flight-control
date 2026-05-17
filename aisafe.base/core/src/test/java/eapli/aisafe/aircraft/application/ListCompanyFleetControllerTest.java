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

class ListCompanyFleetControllerTest {

    private AuthorizationService authz;
    private AircraftRepository aircraftRepo;
    private AirTransportCompanyRepository companyRepo;
    private AircraftModelRepository aircraftModelRepo;
    private ListCompanyFleetController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        aircraftRepo = mock(AircraftRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        aircraftModelRepo = mock(AircraftModelRepository.class);
        controller = new ListCompanyFleetController(authz, aircraftRepo, companyRepo, aircraftModelRepo);
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
        final List<Aircraft> fleet = List.of(makeAircraft("CS-TUI", "Portugal"));
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(fleet);
        final Iterable<Aircraft> result = controller.fleetOfCompany("TP");
        verify(aircraftRepo).findByCompanyId(CompanyIATA.valueOf("TP"));
        assertNotNull(result);
    }

    @Test
    void ensureFleetOfCompanyChecksAuthorization() {
        when(aircraftRepo.findByCompanyId(any())).thenReturn(List.of());
        controller.fleetOfCompany("TP");
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── All active aircraft ───────────────────────────────────────────────────

    @Test
    void ensureAllActiveAircraftDelegatesToRepo() {
        final List<Aircraft> fleet = List.of(makeAircraft("CS-TUI", "Portugal"));
        when(aircraftRepo.findAllActive()).thenReturn(fleet);
        final Iterable<Aircraft> result = controller.allActiveAircraft();
        verify(aircraftRepo).findAllActive();
        assertNotNull(result);
    }

    @Test
    void ensureAllActiveAircraftChecksAuthorization() {
        when(aircraftRepo.findAllActive()).thenReturn(List.of());
        controller.allActiveAircraft();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── All companies ─────────────────────────────────────────────────────────

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        when(companyRepo.findAll()).thenReturn(List.of(makeCompany()));
        final Iterable<AirTransportCompany> result = controller.allCompanies();
        verify(companyRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllCompaniesChecksAuthorization() {
        when(companyRepo.findAll()).thenReturn(List.of());
        controller.allCompanies();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── US072a: filter by model ───────────────────────────────────────────────

    @Test
    void ensureFleetByModelDelegatesToRepo() {
        final List<Aircraft> fleet = List.of(makeAircraft("CS-TUI", "Portugal"));
        when(aircraftRepo.findByCompanyIdAndModel(CompanyIATA.valueOf("TP"),
                AircraftModelCode.valueOf("A320"))).thenReturn(fleet);
        final Iterable<Aircraft> result = controller.fleetByModel("TP", "A320");
        verify(aircraftRepo).findByCompanyIdAndModel(CompanyIATA.valueOf("TP"),
                AircraftModelCode.valueOf("A320"));
        assertNotNull(result);
    }

    @Test
    void ensureFleetByModelChecksAuthorization() {
        when(aircraftRepo.findByCompanyIdAndModel(any(), any())).thenReturn(List.of());
        controller.fleetByModel("TP", "A320");
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── US072b: filter by maker ───────────────────────────────────────────────

    @Test
    void ensureFleetByMakerFiltersCorrectly() {
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(List.of(aircraft));
        final AircraftModel model = mock(AircraftModel.class);
        when(model.manufacturerName()).thenReturn("Airbus");
        when(aircraftModelRepo.ofIdentity(AircraftModelCode.valueOf("A320")))
                .thenReturn(Optional.of(model));
        final Iterable<Aircraft> result = controller.fleetByMaker("TP", "Airbus");
        assertTrue(result.iterator().hasNext(), "Should return aircraft matching maker");
    }

    @Test
    void ensureFleetByMakerExcludesNonMatchingMaker() {
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(List.of(aircraft));
        final AircraftModel model = mock(AircraftModel.class);
        when(model.manufacturerName()).thenReturn("Airbus");
        when(aircraftModelRepo.ofIdentity(AircraftModelCode.valueOf("A320")))
                .thenReturn(Optional.of(model));
        final Iterable<Aircraft> result = controller.fleetByMaker("TP", "Boeing");
        assertFalse(result.iterator().hasNext(), "Should not return aircraft of different maker");
    }

    // ── US072c: filter by exact capacity ─────────────────────────────────────

    @Test
    void ensureFleetByCapacityFiltersCorrectly() {
        // Arrange — aircraft has capacity = 180
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(List.of(aircraft));
        // Act
        final Iterable<Aircraft> result = controller.fleetByCapacity("TP", 180);
        // Assert
        assertTrue(result.iterator().hasNext(), "Should return aircraft with exact capacity 180");
    }

    @Test
    void ensureFleetByCapacityExcludesNonMatchingCapacity() {
        // Arrange — aircraft has capacity = 180, filter for 150 should return nothing
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(List.of(aircraft));
        // Act
        final Iterable<Aircraft> result = controller.fleetByCapacity("TP", 150);
        // Assert
        assertFalse(result.iterator().hasNext(), "Should not return aircraft with different capacity");
    }

    // ── US072d: filter by exact age ───────────────────────────────────────────

    @Test
    void ensureFleetByAgeFiltersCorrectly() {
        // Arrange — aircraft registered in 2018
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(List.of(aircraft));
        final int expectedAge = aircraft.ageInYears();
        // Act
        final Iterable<Aircraft> result = controller.fleetByAge("TP", expectedAge);
        // Assert
        assertTrue(result.iterator().hasNext(), "Should return aircraft with matching age");
    }

    @Test
    void ensureFleetByAgeExcludesNonMatchingAge() {
        // Arrange
        final Aircraft aircraft = makeAircraft("CS-TUI", "Portugal");
        when(aircraftRepo.findByCompanyId(CompanyIATA.valueOf("TP"))).thenReturn(List.of(aircraft));
        // Act
        final Iterable<Aircraft> result = controller.fleetByAge("TP", 999);
        // Assert
        assertFalse(result.iterator().hasNext(), "Should not return aircraft with different age");
    }
}