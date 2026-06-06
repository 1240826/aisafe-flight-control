package eapli.aisafe.weatherdata.application;

public class WeatherApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WeatherApiException(final String message) {
        super(message);
    }

    public WeatherApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
