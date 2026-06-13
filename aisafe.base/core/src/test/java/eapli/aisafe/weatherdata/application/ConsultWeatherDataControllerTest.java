package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConsultWeatherDataControllerTest {

    private AuthorizationService authz;
    private WeatherDataRepository repo;
    private ConsultWeatherDataController controller;

    @BeforeEach
    public void setup() {
        authz = mock(AuthorizationService.class);
        repo = mock(WeatherDataRepository.class);
        controller = new ConsultWeatherDataController(authz, repo);
    }

    @ParameterizedTest
    @CsvFileSource(resources =  "/us043/consult_weather_test.csv", numLinesToSkip = 1)
    void ensureConsultWeatherDataBehaviour(
            final String role,
            final String areaCode,
            final String dateStr,
            final boolean mockHasData,
            final boolean expectAuthPass,
            final boolean expectEmptyList) {

        final LocalDate date = LocalDate.parse(dateStr);
        final Role roleObj = Role.valueOf(role);

        if (!expectAuthPass) {
            doThrow(new SecurityException("Access denied"))
                    .when(authz).ensureAuthenticatedUserHasAnyOf(any(Role.class), any(Role.class), any(Role.class));
            assertThrows(SecurityException.class,
                    () -> controller.consultWeatherData(areaCode, date),
                    "User with role " + role + " must be denied access");
            return;
        }

        // Auth passes — set up mock data
        if (mockHasData) {
            final WeatherData mockWd = mock(WeatherData.class);
            when(repo.findByAreaCodeAndDate(AreaCode.valueOf(areaCode), date))
                    .thenReturn(List.of(mockWd));
        } else {
            when(repo.findByAreaCodeAndDate(AreaCode.valueOf(areaCode), date))
                    .thenReturn(List.of());
        }

        final Iterable<WeatherData> result = controller.consultWeatherData(areaCode, date);

        verify(repo).findByAreaCodeAndDate(AreaCode.valueOf(areaCode), date);

        if (expectEmptyList) {
            assertFalse(result.iterator().hasNext(), "Expected no results for " + areaCode + " on " + date);
        } else if (mockHasData) {
            assertTrue(result.iterator().hasNext(), "Expected results for " + areaCode + " on " + date);
        }
    }

    @Test
    void ensureConsultWeatherDataWithNullAreaCodeThrows() {
        assertThrows(Exception.class,
                () -> controller.consultWeatherData(null, LocalDate.now()));
    }

    @Test
    void ensureConsultWeatherDataWithEmptyAreaCodeThrows() {
        assertThrows(Exception.class,
                () -> controller.consultWeatherData("", LocalDate.now()));
    }
}
