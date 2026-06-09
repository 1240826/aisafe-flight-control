package eapli.aisafe.flight.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.domain.FlightType;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.pilot.domain.PilotId;
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

    @Test
    void ensureAllFlightsReturnsEmptyListWhenNoFlights() {
        when(flightRepo.findAll()).thenReturn(List.of());

        final var result = controller.allFlights();

        assertNotNull(result);
        assertFalse(result.iterator().hasNext());
    }

    @Test
    void ensureAllFlightsReturnsMultipleFlights() {
        when(flightRepo.findAll()).thenReturn(List.of(
                new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME),
                new Flight(FlightDesignator.valueOf("TP5678"), DEP_TIME)));

        final var result = controller.allFlights();

        int count = 0;
        for (final var f : result) count++;
        assertEquals(2, count);
    }

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
                () -> controller.flightByDesignator("TP9999"));
    }

    @Test
    void ensureFlightByDesignatorChecksAuthorization() {
        when(flightRepo.ofIdentity(any())).thenReturn(Optional.of(
                new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME)));
        controller.flightByDesignator("TP1234");
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureFlightByDesignatorWithNullDesignator() {
        assertThrows(Exception.class, () -> controller.flightByDesignator(null));
    }

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

    @Test
    void ensureAssignWeatherWithZeroId() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(weatherRepo.ofIdentity(0L)).thenReturn(Optional.of(mock(WeatherData.class)));
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));
        when(flightRepo.save(any())).thenReturn(flight);

        final var result = controller.assignWeather("TP1234", 0L);

        assertEquals(0L, result.weatherDataId());
    }

    @Test
    void ensureAssignWeatherWithLargeId() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(weatherRepo.ofIdentity(999999L)).thenReturn(Optional.of(mock(WeatherData.class)));
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));
        when(flightRepo.save(any())).thenReturn(flight);

        final var result = controller.assignWeather("TP1234", 999999L);

        assertEquals(999999L, result.weatherDataId());
    }

    @Test
    void ensureWeatherDataForFlightReturnsData() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME,
                FlightRouteName.valueOf("TP123"), "CS-TTT", new PilotId("P12345"), FlightType.REGULAR);
        final var aca = mock(AirControlArea.class);
        when(aca.containsCoordinates(anyDouble(), anyDouble())).thenReturn(true);
        when(aca.code()).thenReturn(AreaCode.valueOf("LPPC"));
        when(acaRepo.findAll()).thenReturn(List.of(aca));

        final WeatherData wd = mock(WeatherData.class);
        when(wd.areaCode()).thenReturn(AreaCode.valueOf("LPPC"));
        when(weatherRepo.findAll()).thenReturn(List.of(wd));

        final var result = controller.weatherDataForFlight(flight);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void ensureWeatherDataForFlightReturnsEmptyWhenNoAcaFound() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME,
                FlightRouteName.valueOf("TP123"), "CS-TTT", new PilotId("P12345"), FlightType.REGULAR);
        when(acaRepo.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> controller.weatherDataForFlight(flight));
    }

    @Test
    void ensureFindAcaForMidpointReturnsCorrectAca() {
        final var aca = mock(AirControlArea.class);
        when(aca.containsCoordinates(38.7, -9.1)).thenReturn(true);
        when(acaRepo.findAll()).thenReturn(List.of(aca));

        final var result = controller.findAcaForMidpoint(38.7, -9.1);

        assertSame(aca, result);
    }

    @Test
    void ensureFindAcaForMidpointThrowsWhenNoAcaFound() {
        when(acaRepo.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> controller.findAcaForMidpoint(0.0, 0.0));
    }

    @Test
    void ensureFlightByDesignatorWithLowerCaseDesignator() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));

        final var result = controller.flightByDesignator("tp1234");

        assertNotNull(result);
        assertEquals("TP1234", result.identity().toString());
    }
}
