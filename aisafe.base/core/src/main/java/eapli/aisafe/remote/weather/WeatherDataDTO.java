package eapli.aisafe.remote.weather;

import eapli.aisafe.weatherdata.domain.WeatherData;

import java.time.LocalDateTime;

public record WeatherDataDTO(
        String areaCode,
        double latitude,
        double longitude,
        int altitudeMetres,
        double windSpeedKnots,
        double windDirectionDegrees,
        double temperatureCelsius,
        String sourceProvider,
        LocalDateTime recordedDateTime
) {
    public static WeatherDataDTO from(final WeatherData wd) {
        return new WeatherDataDTO(
                wd.areaCode().toString(),
                wd.windCondition().latitude(),
                wd.windCondition().longitude(),
                wd.windCondition().altitudeMetres(),
                wd.windCondition().speedKnots(),
                wd.windCondition().directionDegrees(),
                wd.temperatureCelsius(),
                wd.sourceProvider(),
                wd.recordedDateTime()
        );
    }
}