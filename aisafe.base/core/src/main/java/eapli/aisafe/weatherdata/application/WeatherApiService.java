package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

public class WeatherApiService {

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast"
                    + "?current=temperature_2m,wind_speed_10m,wind_direction_10m"
                    + "&timezone=auto";

    private static final String SOURCE_PROVIDER = "Open-Meteo API";
    private static final int HTTP_TIMEOUT_SECONDS = 15;

    private final HttpClient httpClient;
    private final WeatherDataRepository weatherRepo;

    public WeatherApiService(final WeatherDataRepository weatherRepo) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build();
        this.weatherRepo = weatherRepo;
    }

    public WeatherData fetchAndSave(final AreaCode areaCode,
                                     final double latitude,
                                     final double longitude,
                                     final int altitudeMetres) {
        final var json = callApi(latitude, longitude);
        final var windSpeed = extractWindSpeed(json);
        final var windDirection = extractWindDirection(json);
        final var temperature = extractTemperature(json);

        final var wd = new WeatherData(
                areaCode,
                new WindCondition(windSpeed, windDirection, latitude, longitude, altitudeMetres),
                temperature,
                SOURCE_PROVIDER,
                LocalDateTime.now());
        return weatherRepo.save(wd);
    }

    public WeatherApiResult fetch(final double latitude, final double longitude) {
        final var json = callApi(latitude, longitude);
        return new WeatherApiResult(
                extractWindSpeed(json),
                extractWindDirection(json),
                extractTemperature(json));
    }

    private String callApi(final double latitude, final double longitude) {
        final var url = OPEN_METEO_URL + "&latitude=" + latitude + "&longitude=" + longitude;
        try {
            final var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                    .GET()
                    .build();
            final var response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WeatherApiException(
                        "Open-Meteo API returned HTTP " + response.statusCode()
                                + ": " + response.body());
            }
            return response.body();
        } catch (final IOException e) {
            throw new WeatherApiException(
                    "Failed to call Open-Meteo API: " + e.getMessage(), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherApiException(
                    "Open-Meteo API call was interrupted", e);
        }
    }

    public static double extractWindSpeed(final String json) {
        final var marker = "\"wind_speed_10m\":";
        final var idx = json.indexOf(marker);
        if (idx < 0) return 10.0;
        final var start = idx + marker.length();
        final var end = json.indexOf(',', start);
        if (end < 0) return 10.0;
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (final NumberFormatException e) {
            return 10.0;
        }
    }

    public static int extractWindDirection(final String json) {
        final var marker = "\"wind_direction_10m\":";
        final var idx = json.indexOf(marker);
        if (idx < 0) return 180;
        final var start = idx + marker.length();
        final var end = json.indexOf('}', start);
        final var comma = json.indexOf(',', start);
        final var actualEnd = (comma > 0 && comma < end) ? comma : end;
        if (actualEnd < 0) return 180;
        try {
            return Integer.parseInt(json.substring(start, actualEnd).trim());
        } catch (final NumberFormatException e) {
            return 180;
        }
    }

    public static double extractTemperature(final String json) {
        final var marker = "\"temperature_2m\":";
        final var idx = json.indexOf(marker);
        if (idx < 0) return 15.0;
        final var start = idx + marker.length();
        final var end = json.indexOf(',', start);
        if (end < 0) return 15.0;
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (final NumberFormatException e) {
            return 15.0;
        }
    }

    public record WeatherApiResult(double windSpeedKnots,
                                    int windDirectionDegrees,
                                    double temperatureCelsius) {
    }
}
