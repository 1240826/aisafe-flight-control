package eapli.aisafe.flight.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddWeatherToFlightControllerTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    private AuthorizationService authz;
    private FlightRepository flightRepo;
    private WeatherDataRepository weatherRepo;
    private AirControlAreaRepository acaRepo;
    private AddWeatherToFlightController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        flightRepo = mock(FlightRepository.class);
        weatherRepo = mock(WeatherDataRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        controller = new AddWeatherToFlightController(authz, flightRepo, weatherRepo, acaRepo);
    }

    // ── allFlights ────────────────────────────────────────────────────────────

    @Test
    void ensureAllFlightsDelegatesToRepo() {
        when(flightRepo.findAll()).thenReturn(List.of(
                new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME)));

        final var result = controller.allFlights();

        verify(flightRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllFlightsChecksAuthorization() {
        when(flightRepo.findAll()).thenReturn(List.of());
        controller.allFlights();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── flightByDesignator ────────────────────────────────────────────────────

    @Test
    void ensureFlightByDesignatorReturnsFlight() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));

        final var result = controller.flightByDesignator("TP1234");

        assertNotNull(result);
        assertEquals("TP1234", result.identity().toString());
    }

    @Test
    void ensureFlightByDesignatorThrowsForUnknown() {
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP9999"))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.flightByDesignator("TP9999"),
                "Unknown flight must be rejected");
    }

    @Test
    void ensureFlightByDesignatorChecksAuthorization() {
        when(flightRepo.ofIdentity(any())).thenReturn(Optional.of(
                new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME)));
        controller.flightByDesignator("TP1234");
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── assignWeather ─────────────────────────────────────────────────────────

    @Test
    void ensureAssignWeatherSavesFlight() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(weatherRepo.ofIdentity(1L)).thenReturn(Optional.of(mock(WeatherData.class)));
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));
        when(flightRepo.save(any())).thenReturn(flight);

        final var result = controller.assignWeather("TP1234", 1L);

        assertNotNull(result);
        assertEquals(1L, result.weatherDataId());
        verify(flightRepo).save(any());
    }

    @Test
    void ensureAssignWeatherWithUnknownFlightThrows() {
        when(weatherRepo.ofIdentity(1L)).thenReturn(Optional.of(mock(WeatherData.class)));
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP9999"))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.assignWeather("TP9999", 1L));
    }

    @Test
    void ensureAssignWeatherWithUnknownWeatherThrows() {
        when(weatherRepo.ofIdentity(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.assignWeather("TP1234", 99L));
    }

    @Test
    void ensureAssignWeatherChecksAuthorization() {
        when(weatherRepo.ofIdentity(1L)).thenReturn(Optional.of(mock(WeatherData.class)));
        when(flightRepo.ofIdentity(any())).thenReturn(Optional.of(
                new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME)));
        when(flightRepo.save(any())).thenReturn(mock(Flight.class));

        controller.assignWeather("TP1234", 1L);

        verify(authz, times(2)).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureAssignWeatherIsIdempotent() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.assignWeatherData(1L);
        when(weatherRepo.ofIdentity(1L)).thenReturn(Optional.of(mock(WeatherData.class)));
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));
        when(flightRepo.save(any())).thenReturn(flight);

        final var result = controller.assignWeather("TP1234", 1L);

        assertEquals(1L, result.weatherDataId());
        verify(flightRepo).save(any());
    }
}
