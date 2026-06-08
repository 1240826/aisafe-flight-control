package eapli.aisafe.remote.weather;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.aisafe.weatherdata.domain.WeatherData;

import java.time.LocalDateTime;

/**
 * Facade between the Weather TCP handler and the EAPLI application layer for US044.
 *
 * <p>Keeps the handler thin: all business logic and repository access
 * goes through the existing {@link RegisterWeatherDataController}.
 */
public class RemoteWeatherService {

    private final RegisterWeatherDataController weatherController;

    public RemoteWeatherService() {
        this.weatherController = new RegisterWeatherDataController();
    }

    /** Package-private testing constructor — allows injecting a mock controller. */
    RemoteWeatherService(final RegisterWeatherDataController weatherController) {
        this.weatherController = weatherController;
    }

    /** US041 — Register a single weather observation for an ACA. */
    public void registerWeatherData(final String areaCode,
                                    final double latitude, final double longitude,
                                    final int altitudeMetres,
                                    final double windSpeedKnots, final double windDirectionDeg,
                                    final double temperatureCelsius,
                                    final String sourceProvider,
                                    final LocalDateTime recordedDateTime) {
        weatherController.registerWeatherData(areaCode, latitude, longitude, altitudeMetres,
                windSpeedKnots, windDirectionDeg, temperatureCelsius, sourceProvider, recordedDateTime);
    }

    /** US043 — Return all weather data stored for a given ACA. */
    public Iterable<WeatherData> weatherDataForArea(final String areaCode) {
        return weatherController.weatherDataForArea(areaCode);
    }

    /** Support — list all available air control areas. */
    public Iterable<AirControlArea> listAreas() {
        return weatherController.allAirControlAreas();
    }
}
