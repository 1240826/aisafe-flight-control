package eapli.aisafe.server;

import eapli.aisafe.remote.RemoteProtocol;
import eapli.aisafe.remote.weather.AirControlAreaDTO;
import eapli.aisafe.remote.weather.RemoteWeatherService;
import eapli.aisafe.remote.weather.WeatherDataDTO;
import eapli.aisafe.usermanagement.domain.AISafeRoles;

import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * US044 — handles one Weather Person TCP session.
 *
 * <p>Delegates all business logic to {@link RemoteWeatherService},
 * keeping this handler focused purely on protocol parsing and response formatting.
 *
 * <p>Supported commands (after AUTH):
 * <ul>
 *   <li>{@code REGISTER_WEATHER|areaCode|lat|lon|altM|windKnots|windDeg|tempC|provider|datetime}</li>
 *   <li>{@code IMPORT_WEATHER|areaCode|csvRows} — rows: {@code lat,lon,alt,speed,dir,temp,provider[,datetime]} separated by {@code ;}</li>
 *   <li>{@code CONSULT_WEATHER|areaCode|date} — date: ISO (e.g. 2026-06-01)</li>
 *   <li>{@code LIST_AREAS}</li>
 * </ul>
 */
class WeatherClientHandler extends AbstractClientHandler {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RemoteWeatherService weatherService;

    WeatherClientHandler(final Socket clientSocket) {
        super(clientSocket, RemoteProtocol.SVC_WEATHER, AISafeRoles.WEATHER_PERSON);
        this.weatherService = new RemoteWeatherService();
    }

    @Override
    protected String handleCommand(final String cmd, final String[] fields) {
        return switch (cmd) {
            case RemoteProtocol.CMD_REGISTER_WEATHER -> doRegisterWeather(fields);
            case RemoteProtocol.CMD_IMPORT_WEATHER   -> doImportWeather(fields);
            case RemoteProtocol.CMD_CONSULT_WEATHER  -> doConsultWeather(fields);
            case RemoteProtocol.CMD_LIST_AREAS       -> doListAreas();
            default -> RemoteProtocol.err("Unknown command: " + cmd);
        };
    }

    // ── REGISTER_WEATHER|areaCode|lat|lon|altM|windKnots|windDeg|tempC|provider|datetime ──

    private String doRegisterWeather(final String[] f) {
        if (f.length < 10) {
            return RemoteProtocol.err(
                    "Usage: REGISTER_WEATHER|areaCode|lat|lon|altM|windKnots|windDeg|tempC|provider|datetime");
        }
        try {
            weatherService.registerWeatherData(
                    f[1],
                    Double.parseDouble(f[2]),
                    Double.parseDouble(f[3]),
                    Integer.parseInt(f[4]),
                    Double.parseDouble(f[5]),
                    Double.parseDouble(f[6]),
                    Double.parseDouble(f[7]),
                    f[8],
                    LocalDateTime.parse(f[9], DT_FMT));
            return RemoteProtocol.ok("Weather data registered for area " + f[1]);
        } catch (final NumberFormatException e) {
            return RemoteProtocol.err("Invalid number: " + e.getMessage());
        } catch (final DateTimeParseException e) {
            return RemoteProtocol.err("Invalid datetime (expected ISO, e.g. 2026-06-01T14:30:00)");
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── IMPORT_WEATHER|areaCode|lat,lon,alt,speed,dir,temp,provider[,datetime];... ──

    private String doImportWeather(final String[] f) {
        if (f.length < 3) {
            return RemoteProtocol.err("Usage: IMPORT_WEATHER|areaCode|csvRows");
        }
        final String areaCode = f[1];
        final String csv      = f[2];
        int count = 0;
        final StringBuilder errors = new StringBuilder();

        for (final String row : csv.split(";")) {
            final String trimmed = row.trim();
            if (trimmed.isEmpty()) continue;
            final String[] cols = trimmed.split(",", -1);
            if (cols.length < 7) {
                errors.append("Skipped malformed row: ").append(trimmed).append("; ");
                continue;
            }
            try {
                final LocalDateTime dt = cols.length > 7
                        ? LocalDateTime.parse(cols[7], DT_FMT)
                        : LocalDateTime.now();
                weatherService.registerWeatherData(
                        areaCode,
                        Double.parseDouble(cols[0]),
                        Double.parseDouble(cols[1]),
                        Integer.parseInt(cols[2]),
                        Double.parseDouble(cols[3]),
                        Double.parseDouble(cols[4]),
                        Double.parseDouble(cols[5]),
                        cols[6],
                        dt);
                count++;
            } catch (final Exception e) {
                errors.append("Row error: ").append(e.getMessage()).append("; ");
            }
        }

        final String msg = "Imported " + count + " records"
                + (errors.length() > 0 ? " | Warnings: " + errors : "");
        return RemoteProtocol.ok(msg);
    }

    // ── CONSULT_WEATHER|areaCode|date ──

    private String doConsultWeather(final String[] f) {
        if (f.length < 3) {
            return RemoteProtocol.err("Usage: CONSULT_WEATHER|areaCode|date (e.g. 2026-06-01)");
        }
        try {
            final String areaCode  = f[1];
            final LocalDate filter = LocalDate.parse(f[2], DateTimeFormatter.ISO_LOCAL_DATE);
            final var records = weatherService.weatherDataForArea(areaCode);

            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final WeatherDataDTO wd : records) {
                if (!wd.recordedDateTime().toLocalDate().equals(filter)) continue;
                if (count > 0) sb.append(";");
                sb.append(wd.areaCode()).append(",")
                        .append(wd.latitude()).append(",")
                        .append(wd.longitude()).append(",")
                        .append(wd.altitudeMetres()).append(",")
                        .append(wd.windSpeedKnots()).append(",")
                        .append(wd.windDirectionDegrees()).append(",")
                        .append(wd.temperatureCelsius()).append(",")
                        .append(wd.sourceProvider()).append(",")
                        .append(wd.recordedDateTime().format(DT_FMT));
                count++;
            }

            if (count == 0) {
                return RemoteProtocol.ok("No records found for " + areaCode + " on " + f[2]);
            }
            return RemoteProtocol.ok(count + " records: " + sb);
        } catch (final DateTimeParseException e) {
            return RemoteProtocol.err("Invalid date (expected ISO, e.g. 2026-06-01)");
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── LIST_AREAS ──

    private String doListAreas() {
        try {
            final var areas = weatherService.listAreas();
            final StringBuilder sb = new StringBuilder();
            for (final AirControlAreaDTO a : areas) {
                if (sb.length() > 0) sb.append(";");
                sb.append(a.areaCode()).append(":").append(a.name());
            }
            return RemoteProtocol.ok(sb.length() > 0 ? sb.toString() : "No areas");
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }
}
