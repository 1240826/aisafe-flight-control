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
import eapli.aisafe.weatherdata.domain.WindCondition;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FetchWeatherForFlightControllerTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    private AuthorizationService authz;
    private FlightRepository flightRepo;
    private WeatherDataRepository weatherRepo;
    private AirControlAreaRepository acaRepo;
    private FetchWeatherForFlightController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        flightRepo = mock(FlightRepository.class);
        weatherRepo = mock(WeatherDataRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        controller = new FetchWeatherForFlightController(authz, flightRepo, weatherRepo, acaRepo);
    }

    @Test
    void ensureAllFlightsDelegatesToRepo() {
        when(flightRepo.findAll()).thenReturn(List.of());
        final Iterable<Flight> result = controller.allFlights();
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
    void ensureFlightByDesignatorReturnsFlight() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234"))).thenReturn(Optional.of(flight));
        final Flight result = controller.flightByDesignator("TP1234");
        assertNotNull(result);
    }

    @Test
    void ensureFlightByDesignatorThrowsForUnknown() {
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP9999"))).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> controller.flightByDesignator("TP9999"));
    }

    @Test
    void ensureFlightByDesignatorChecksAuthorization() {
        when(flightRepo.ofIdentity(any())).thenReturn(Optional.of(mock(Flight.class)));
        controller.flightByDesignator("TP1234");
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureComputeMidpointReturnsDefaultForStandardRoute() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME,
                FlightRouteName.valueOf("TP12"), "CS-TTT", new PilotId("P12345"), FlightType.REGULAR);
        final var midpoint = controller.computeMidpoint(flight);
        assertEquals(40.0, midpoint.latitude());
        assertEquals(-8.0, midpoint.longitude());
        assertEquals("LIS", midpoint.originAirport());
        assertEquals("OPO", midpoint.destinationAirport());
    }

    @Test
    void ensureComputeMidpointWithHyphenatedRoute() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME,
                FlightRouteName.valueOf("TP12"), "CS-TTT", new PilotId("P12345"), FlightType.REGULAR);
        final var midpoint = controller.computeMidpoint(flight);
        assertEquals(40.0, midpoint.latitude(), 0.001);
        assertEquals(-8.0, midpoint.longitude(), 0.001);
    }

    @Test
    void ensureFindAcaForMidpointReturnsCorrectAca() {
        final AirControlArea aca = mock(AirControlArea.class);
        when(aca.containsCoordinates(38.7, -9.1)).thenReturn(true);
        when(acaRepo.findAll()).thenReturn(List.of(aca));
        final AirControlArea result = controller.findAcaForMidpoint(38.7, -9.1);
        assertSame(aca, result);
    }

    @Test
    void ensureFindAcaForMidpointThrowsWhenNoAca() {
        when(acaRepo.findAll()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> controller.findAcaForMidpoint(0.0, 0.0));
    }

    private static WeatherData mockWeatherDataWithId() {
        final var wd = mock(WeatherData.class);
        when(wd.identity()).thenReturn(42L);
        when(wd.windCondition()).thenReturn(new WindCondition(15.0, 180, 38.7, -9.1, 3000));
        return wd;
    }

    @Test
    void ensureFetchAndAssignWeatherSavesFlight(@TempDir Path tempDir) {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234"))).thenReturn(Optional.of(flight));
        final var wd = mockWeatherDataWithId();
        when(weatherRepo.save(any(WeatherData.class))).thenReturn(wd);
        when(flightRepo.save(any())).thenReturn(flight);

        final var result = controller.fetchAndAssignWeather("TP1234", "LPPC", 38.7, -9.1);
        assertNotNull(result);
        assertTrue(result.zoneCount() > 0);
        verify(flightRepo).save(any());
    }

    @Test
    void ensureFetchAndAssignWeatherChecksAuthorization() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234"))).thenReturn(Optional.of(flight));
        final var wd = mockWeatherDataWithId();
        when(weatherRepo.save(any())).thenReturn(wd);
        when(flightRepo.save(any())).thenReturn(flight);
        controller.fetchAndAssignWeather("TP1234", "LPPC", 38.7, -9.1);
        verify(authz, atLeastOnce()).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureFetchWeatherJsonReturnsResult() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234"))).thenReturn(Optional.of(flight));
        final var wd = mockWeatherDataWithId();
        when(weatherRepo.save(any(WeatherData.class))).thenReturn(wd);

        final var result = controller.fetchWeatherJson("TP1234", "LPPC", 38.7, -9.1);
        assertNotNull(result);
        assertNotNull(result.weatherJson());
    }

    @Test
    void ensureWriteWeatherFileReturnsPath() {
        final String result = controller.writeWeatherFile("{\"key\":\"value\"}");
        assertNotNull(result);
        assertTrue(result.endsWith(".json"));
    }

    @Test
    void ensureWriteWeatherFileThrowsOnNullContent() {
        assertThrows(IllegalStateException.class, () -> controller.writeWeatherFile((String) null));
    }

    @Test
    void ensureRouteMidpointRecord() {
        final var mp = new FetchWeatherForFlightController.RouteMidpoint(10.0, 20.0, "LIS", "OPO");
        assertEquals(10.0, mp.latitude());
        assertEquals(20.0, mp.longitude());
        assertEquals("LIS", mp.originAirport());
        assertEquals("OPO", mp.destinationAirport());
    }

    @Test
    void ensureWeatherFetchResultRecord() {
        final var r = new FetchWeatherForFlightController.WeatherFetchResult(1L, 6, "{}");
        assertEquals(1L, r.weatherDataId());
        assertEquals(6, r.zoneCount());
        assertEquals("{}", r.weatherJson());
    }
}
