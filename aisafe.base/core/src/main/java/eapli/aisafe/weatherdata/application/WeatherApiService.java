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
import java.time.format.DateTimeFormatter;

public class WeatherApiService {

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast"
                    + "?hourly=temperature_2m,wind_speed_10m,wind_direction_10m"
                    + "&timezone=auto";

    private static final String SOURCE_PROVIDER = "Open-Meteo API (hourly)";
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
                                     final int altitudeMetres,
                                     final LocalDateTime targetTime) {
        final var json = callApi(latitude, longitude);
        final var targetHour = targetTime.withMinute(0).withSecond(0).withNano(0);
        final var windSpeed = extractHourlyWindSpeed(json, targetHour);
        final var windDirection = extractHourlyWindDirection(json, targetHour);
        final var temperature = extractHourlyTemperature(json, targetHour);

        final var wd = new WeatherData(
                areaCode,
                new WindCondition(windSpeed, windDirection, latitude, longitude, altitudeMetres),
                temperature,
                SOURCE_PROVIDER,
                LocalDateTime.now());
        return weatherRepo.save(wd);
    }

    public WeatherApiResult fetch(final double latitude, final double longitude,
                                   final LocalDateTime targetTime) {
        final var json = callApi(latitude, longitude);
        final var targetHour = targetTime.withMinute(0).withSecond(0).withNano(0);
        return new WeatherApiResult(
                extractHourlyWindSpeed(json, targetHour),
                extractHourlyWindDirection(json, targetHour),
                extractHourlyTemperature(json, targetHour));
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

    public static double extractHourlyWindSpeed(final String json, final LocalDateTime targetHour) {
        final var timeIdx = findTimeIndex(json, targetHour);
        if (timeIdx < 0) {
            return extractWindSpeed(json);
        }
        final var marker = "\"wind_speed_10m\":[";
        final var arrStart = json.indexOf(marker);
        if (arrStart < 0) return 10.0;
        final var values = json.substring(arrStart + marker.length(), json.indexOf(']', arrStart));
        final var parts = values.split(",");
        if (timeIdx < parts.length) {
            try { return Double.parseDouble(parts[timeIdx].trim()); }
            catch (final NumberFormatException ignored) {}
        }
        return 10.0;
    }

    public static int extractHourlyWindDirection(final String json, final LocalDateTime targetHour) {
        final var timeIdx = findTimeIndex(json, targetHour);
        if (timeIdx < 0) {
            return extractWindDirection(json);
        }
        final var marker = "\"wind_direction_10m\":[";
        final var arrStart = json.indexOf(marker);
        if (arrStart < 0) return 180;
        final var endBracket = json.indexOf(']', arrStart);
        if (endBracket < 0) return 180;
        final var values = json.substring(arrStart + marker.length(), endBracket);
        final var parts = values.split(",");
        if (timeIdx < parts.length) {
            try { return Integer.parseInt(parts[timeIdx].trim()); }
            catch (final NumberFormatException ignored) {}
        }
        return 180;
    }

    public static double extractHourlyTemperature(final String json, final LocalDateTime targetHour) {
        final var timeIdx = findTimeIndex(json, targetHour);
        if (timeIdx < 0) {
            return extractTemperature(json);
        }
        final var marker = "\"temperature_2m\":[";
        final var arrStart = json.indexOf(marker);
        if (arrStart < 0) return 15.0;
        final var endBracket = json.indexOf(']', arrStart);
        if (endBracket < 0) return 15.0;
        final var values = json.substring(arrStart + marker.length(), endBracket);
        final var parts = values.split(",");
        if (timeIdx < parts.length) {
            try { return Double.parseDouble(parts[timeIdx].trim()); }
            catch (final NumberFormatException ignored) {}
        }
        return 15.0;
    }

    private static int findTimeIndex(final String json, final LocalDateTime targetHour) {
        final var marker = "\"time\":[";
        final var arrStart = json.indexOf(marker);
        if (arrStart < 0) return -1;
        final var endBracket = json.indexOf(']', arrStart);
        if (endBracket < 0) return -1;
        final var timesStr = json.substring(arrStart + marker.length(), endBracket);
        final var parts = timesStr.split(",");
        final var targetStr = "\"" + targetHour.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"";
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().equals(targetStr)) {
                return i;
            }
        }
        return -1;
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
