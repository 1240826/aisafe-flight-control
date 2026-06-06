package eapli.aisafe.flight.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UseCaseController
public class AddWeatherToFlightController {

    private static final Map<String, double[]> AIRPORT_COORDS = new java.util.HashMap<>();
    static {
        AIRPORT_COORDS.put("LIS", new double[]{38.774, -9.134});
        AIRPORT_COORDS.put("OPO", new double[]{41.248, -8.681});
        AIRPORT_COORDS.put("MAD", new double[]{40.472, -3.561});
        AIRPORT_COORDS.put("BCN", new double[]{41.297, 2.083});
        AIRPORT_COORDS.put("CDG", new double[]{49.009, 2.547});
        AIRPORT_COORDS.put("FRA", new double[]{50.033, 8.570});
        AIRPORT_COORDS.put("LHR", new double[]{51.470, -0.454});
        AIRPORT_COORDS.put("AMS", new double[]{52.308, 4.764});
        AIRPORT_COORDS.put("FNC", new double[]{32.698, -16.774});
        AIRPORT_COORDS.put("PDL", new double[]{37.741, -25.698});
        AIRPORT_COORDS.put("TER", new double[]{38.762, -27.091});
        AIRPORT_COORDS.put("FAO", new double[]{37.014, -7.965});
        AIRPORT_COORDS.put("ALC", new double[]{38.282, -0.558});
        AIRPORT_COORDS.put("AGP", new double[]{36.675, -4.499});
        AIRPORT_COORDS.put("GRO", new double[]{41.898, 2.767});
        AIRPORT_COORDS.put("IBZ", new double[]{38.873, 1.373});
        AIRPORT_COORDS.put("MAH", new double[]{39.863, 4.219});
        AIRPORT_COORDS.put("PMI", new double[]{39.553, 2.731});
        AIRPORT_COORDS.put("SCQ", new double[]{42.896, -8.415});
        AIRPORT_COORDS.put("VGO", new double[]{42.232, -8.627});
        AIRPORT_COORDS.put("BIO", new double[]{43.301, -2.911});
        AIRPORT_COORDS.put("SVQ", new double[]{37.418, -5.893});
        AIRPORT_COORDS.put("TFN", new double[]{28.483, -16.342});
        AIRPORT_COORDS.put("TFS", new double[]{28.044, -16.572});
        AIRPORT_COORDS.put("LPA", new double[]{27.932, -15.387});
        AIRPORT_COORDS.put("ACE", new double[]{28.946, -13.605});
    }

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository acaRepo;

    public AddWeatherToFlightController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    AddWeatherToFlightController(final AuthorizationService authz,
                                  final FlightRepository flightRepo,
                                  final WeatherDataRepository weatherRepo,
                                  final AirControlAreaRepository acaRepo) {
        this.authz = authz;
        this.flightRepo = flightRepo;
        this.weatherRepo = weatherRepo;
        this.acaRepo = acaRepo;
    }

    public Iterable<Flight> allFlights() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        return flightRepo.findAll();
    }

    public Flight flightByDesignator(final String designator) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        return flightRepo.ofIdentity(FlightDesignator.valueOf(designator))
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + designator));
    }

    public List<WeatherData> weatherDataForFlight(final Flight flight) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final var midpoint = computeMidpoint(flight);
        final var aca = findAcaForMidpoint(midpoint.lat, midpoint.lon);
        final var acaCode = aca.code().toString();
        final var result = new ArrayList<WeatherData>();
        for (final var wd : weatherRepo.findAll()) {
            if (wd.areaCode().equals(acaCode)) {
                result.add(wd);
            }
        }
        return result;
    }

    public AirControlArea findAcaForMidpoint(final double lat, final double lon) {
        for (final var aca : acaRepo.findAll()) {
            if (aca.containsCoordinates(lat, lon)) {
                return aca;
            }
        }
        throw new IllegalStateException("No Air Control Area found for coordinates: " + lat + ", " + lon);
    }

    public Flight assignWeather(final String flightDesignator, final Long weatherDataId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);

        weatherRepo.ofIdentity(weatherDataId)
                .orElseThrow(() -> new IllegalArgumentException("Weather data not found: " + weatherDataId));

        final Flight flight = flightByDesignator(flightDesignator);
        flight.assignWeatherData(weatherDataId);
        return flightRepo.save(flight);
    }

    private RouteMidpoint computeMidpoint(final Flight flight) {
        final String route = flight.routeName().toString();
        final String[] parts = route.split("-");
        if (parts.length < 2) {
            return new RouteMidpoint(40.0, -8.0, "LIS", "OPO");
        }
        final String origin = parts[0].trim().toUpperCase();
        final String dest = parts[1].trim().toUpperCase();
        final double[] origCoords = AIRPORT_COORDS.getOrDefault(origin, new double[]{40.0, -8.0});
        final double[] destCoords = AIRPORT_COORDS.getOrDefault(dest, new double[]{38.0, -9.0});
        final double midLat = (origCoords[0] + destCoords[0]) / 2.0;
        final double midLon = (origCoords[1] + destCoords[1]) / 2.0;
        return new RouteMidpoint(midLat, midLon, origin, dest);
    }

    private record RouteMidpoint(double lat, double lon, String origin, String destination) {
    }
}
