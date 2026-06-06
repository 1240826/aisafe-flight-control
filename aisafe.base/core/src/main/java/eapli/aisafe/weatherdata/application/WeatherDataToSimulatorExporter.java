package eapli.aisafe.weatherdata.application;

import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;

import java.util.List;

public class WeatherDataToSimulatorExporter {

    private static final double METRES_TO_FEET = 1.0 / 0.3048;
    private static final double ZONE_HALF_SPAN_DEG = 0.5;
    private static final double ZONE_ALT_BUFFER_FT = 500.0;

    public String export(final List<WeatherData> weatherDataList) {
        final var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"provider\": \"AISafe Weather API Integration\",\n");
        sb.append("  \"duration_hours\": 12.0,\n");
        sb.append("  \"zones\": [\n");

        for (int i = 0; i < weatherDataList.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append(exportZone(weatherDataList.get(i)));
        }

        sb.append("\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String exportZone(final WeatherData wd) {
        final WindCondition wind = wd.windCondition();
        final double lat = wind.latitude();
        final double lon = wind.longitude();
        final double altM = wind.altitudeMetres();
        final double altFt = altM * METRES_TO_FEET;

        final double latNorth = lat + ZONE_HALF_SPAN_DEG;
        final double latSouth = lat - ZONE_HALF_SPAN_DEG;
        final double lonWest = lon - ZONE_HALF_SPAN_DEG;
        final double lonEast = lon + ZONE_HALF_SPAN_DEG;
        final double altLo = Math.max(0, altFt - ZONE_ALT_BUFFER_FT);
        final double altHi = altFt + ZONE_ALT_BUFFER_FT;

        final var sb = new StringBuilder();
        sb.append("    {\n");
        sb.append("      \"lat_north\": ").append(latNorth).append(",\n");
        sb.append("      \"lat_south\": ").append(latSouth).append(",\n");
        sb.append("      \"lon_west\": ").append(lonWest).append(",\n");
        sb.append("      \"lon_east\": ").append(lonEast).append(",\n");
        sb.append("      \"alt_ft_lo\": ").append(altLo).append(",\n");
        sb.append("      \"alt_ft_hi\": ").append(altHi).append(",\n");
        sb.append("      \"dir_deg\": ").append(wind.directionDegrees()).append(",\n");
        sb.append("      \"speed_kt\": ").append(wind.speedKnots()).append("\n");
        sb.append("    }");
        return sb.toString();
    }
}
