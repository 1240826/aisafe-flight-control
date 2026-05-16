package eapli.aisafe.company.application;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterAirTransportCompanyControllerTest {

    private AuthorizationService authz;
    private AirTransportCompanyRepository repo;
    private RegisterAirTransportCompanyController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(AirTransportCompanyRepository.class);
        controller = new RegisterAirTransportCompanyController(authz, repo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureRegisterCompanySavesCompany() {
        // Arrange
        final AirTransportCompany expected = new AirTransportCompany(
                CompanyIATA.valueOf("TP"),
                CompanyICAO.valueOf("TAP"),
                "TAP Air Portugal");
        when(repo.save(any(AirTransportCompany.class))).thenReturn(expected);

        // Act
        final AirTransportCompany result = controller.registerCompany("TP", "TAP", "TAP Air Portugal");

        // Assert
        verify(repo).save(any(AirTransportCompany.class));
        assertNotNull(result);
    }

    @Test
    void ensureRegisterCompanyChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(AirTransportCompany.class));

        // Act
        controller.registerCompany("TP", "TAP", "TAP Air Portugal");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of(
                new AirTransportCompany(CompanyIATA.valueOf("TP"), CompanyICAO.valueOf("TAP"), "TAP Air Portugal")));

        // Act
        final Iterable<AirTransportCompany> result = controller.allCompanies();

        // Assert
        verify(repo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllCompaniesChecksAuthorization() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of());

        // Act
        controller.allCompanies();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureRegisterCompanyWithNullIataThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerCompany(null, "TAP", "TAP Air Portugal"),
                "Null IATA code must be rejected");
    }

    @Test
    void ensureRegisterCompanyWithBlankNameThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerCompany("TP", "TAP", ""),
                "Blank company name must be rejected");
    }
}
