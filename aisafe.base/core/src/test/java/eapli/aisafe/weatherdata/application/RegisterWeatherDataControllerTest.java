package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import eapli.aisafe.aircontrolarea.domain.AirControlArea;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterWeatherDataControllerTest {

    private AuthorizationService authz;
    private WeatherDataRepository repo;
    private AirControlAreaRepository acaRepo;
    private RegisterWeatherDataController controller;

    private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 5, 14, 10, 0);

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(WeatherDataRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        controller = new RegisterWeatherDataController(authz, repo, acaRepo);

        when(acaRepo.ofIdentity(any())).thenReturn(Optional.of(mock(AirControlArea.class)));

    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureRegisterWeatherDataSavesData() {
        // Arrange
        when(repo.save(any(WeatherData.class))).thenReturn(mock(WeatherData.class));

        // Act
        final WeatherData result = controller.registerWeatherData(
                "LPPC",
                38.7, -9.1, 1000,
                15.0, 270.0,
                10.0,
                "IPMA",
                RECORDED_AT);

        // Assert
        verify(repo).save(any(WeatherData.class));
        assertNotNull(result);
    }

    @Test
    void ensureRegisterWeatherDataChecksAuthorization() {
        // Arrange
        when(repo.save(any())).thenReturn(mock(WeatherData.class));

        // Act
        controller.registerWeatherData(
                "LPPC",
                38.7, -9.1, 1000,
                15.0, 270.0,
                10.0,
                "IPMA",
                RECORDED_AT);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureWeatherDataForAreaDelegatesToRepo() {
        // Arrange
        when(repo.findByAreaCode(AreaCode.valueOf("LPPC"))).thenReturn(List.of());

        // Act
        final Iterable<WeatherData> result = controller.weatherDataForArea("LPPC");

        // Assert
        verify(repo).findByAreaCode(AreaCode.valueOf("LPPC"));
        assertNotNull(result);
    }

    @Test
    void ensureWeatherDataForAreaChecksAuthorization() {
        // Arrange
        when(repo.findByAreaCode(any())).thenReturn(List.of());

        // Act
        controller.weatherDataForArea("LPPC");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any(), any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureRegisterWeatherDataWithNullAreaCodeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerWeatherData(
                        null,
                        38.7, -9.1, 1000,
                        15.0, 270.0,
                        10.0,
                        "IPMA",
                        RECORDED_AT),
                "Null area code must be rejected");
    }

    @Test
    void ensureRegisterWeatherDataWithBlankSourceProviderThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerWeatherData(
                        "LPPC",
                        38.7, -9.1, 1000,
                        15.0, 270.0,
                        10.0,
                        "   ",   // blank provider
                        RECORDED_AT),
                "Blank source provider must be rejected");
    }

    @Test
    void ensureRegisterWeatherDataWithNullRecordedDateTimeThrows() {
        // Arrange / Act / Assert
        assertThrows(Exception.class,
                () -> controller.registerWeatherData(
                        "LPPC",
                        38.7, -9.1, 1000,
                        15.0, 270.0,
                        10.0,
                        "IPMA",
                        null),
                "Null recordedDateTime must be rejected");
    }
    @Test
    void ensureRegisterWeatherDataWithNonExistentAcaThrows() {
        when(acaRepo.ofIdentity(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(
                        "XXXX", 38.7, -9.1, 1000, 15.0, 270.0, 10.0, "IPMA", RECORDED_AT),
                "Non-existent ACA must be rejected");
    }

    @Test
    void ensureAllAirControlAreasDelegatesToAcaRepo() {
        when(acaRepo.findAll()).thenReturn(List.of());
        final var result = controller.allAirControlAreas();
        verify(acaRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllAirControlAreasChecksAuthorization() {
        when(acaRepo.findAll()).thenReturn(List.of());
        controller.allAirControlAreas();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }
}
