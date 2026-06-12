package eapli.aisafe.remote.weather;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;

public record AirControlAreaDTO(
        String areaCode,
        String name,
        double minLat,
        double maxLat,
        double minLon,
        double maxLon,
        int maxAltitudeMetres
) {
    public static AirControlAreaDTO from(final AirControlArea aca) {
        return new AirControlAreaDTO(
                aca.code().toString(),
                aca.name().toString(),
                aca.minLat(),
                aca.maxLat(),
                aca.minLon(),
                aca.maxLon(),
                aca.maxAltitudeMetres()
        );
    }
}