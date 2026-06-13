package eapli.aisafe.remote.weather;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.domain.AreaName;
import eapli.aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteWeatherServiceTest {

    private RegisterWeatherDataController weatherController;
    private RemoteWeatherService service;

    @BeforeEach
    void setUp() {
        weatherController = mock(RegisterWeatherDataController.class);
        service = new RemoteWeatherService(weatherController);
    }

    @Test
    void registerWeatherDataDelegatesToController() {
        service.registerWeatherData("LPPC", 41.0, -8.0, 100, 15.0, 90.0,
                22.5, "WEATHER_FORECAST", LocalDateTime.of(2026, 6, 15, 10, 0));
        verify(weatherController).registerWeatherData("LPPC", 41.0, -8.0, 100,
                15.0, 90.0, 22.5, "WEATHER_FORECAST",
                LocalDateTime.of(2026, 6, 15, 10, 0));
    }

    @Test
    void weatherDataForAreaReturnsDTOs() {
        final var wd = new WeatherData(new AreaCode("LPPC"),
                new WindCondition(10.0, 180, 41.0, -8.0, 100),
                22.5, "WEATHER_FORECAST",
                LocalDateTime.of(2026, 6, 15, 10, 0));
        when(weatherController.weatherDataForArea("LPPC")).thenReturn(List.of(wd));
        final var result = service.weatherDataForArea("LPPC");
        verify(weatherController).weatherDataForArea("LPPC");
        assertEquals(1, result.size());
        final var dto = result.get(0);
        assertEquals("LPPC", dto.areaCode());
    }

    @Test
    void weatherDataForAreaReturnsEmptyWhenNoData() {
        when(weatherController.weatherDataForArea("LPPC")).thenReturn(List.of());
        final var result = service.weatherDataForArea("LPPC");
        assertTrue(result.isEmpty());
    }

    @Test
    void listAreasReturnsDTOs() {
        final var aca = new AirControlArea(new AreaCode("LPPC"),
                new AreaName("Lisboa FIR"),
                36.0, 42.0, -10.0, -6.0, 66000);
        when(weatherController.allAirControlAreas()).thenReturn(List.of(aca));
        final var result = service.listAreas();
        verify(weatherController).allAirControlAreas();
        assertEquals(1, result.size());
        assertEquals("LPPC", result.get(0).areaCode());
    }

    @Test
    void listAreasReturnsEmptyWhenNoAreas() {
        when(weatherController.allAirControlAreas()).thenReturn(List.of());
        final var result = service.listAreas();
        assertTrue(result.isEmpty());
    }
}
