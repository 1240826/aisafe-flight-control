package eapli.aisafe.server;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.remote.RemoteProtocol;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.aisafe.weatherdata.domain.WeatherData;

import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * US044 — handles one Weather Person TCP session.
 *
 * <p>Supported commands (after AUTH):
 * <ul>
 *   <li>{@code REGISTER_WEATHER|areaCode|lat|lon|altM|windKnots|windDeg|tempC|provider|datetime}</li>
 *   <li>{@code IMPORT_WEATHER|areaCode|csvRows}  — rows: {@code lat,lon,alt,speed,dir,temp,provider[,datetime]} separated by {@code ;}</li>
 *   <li>{@code CONSULT_WEATHER|areaCode|date}   — date: ISO (2026-06-01)</li>
 *   <li>{@code LIST_AREAS}</li>
 * </ul>
 */
class WeatherClientHandler extends AbstractClientHandler {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RegisterWeatherDataController controller;

    WeatherClientHandler(final Socket clientSocket) {
        super(clientSocket, RemoteProtocol.SVC_WEATHER, AISafeRoles.WEATHER_PERSON);
        this.controller = new RegisterWeatherDataController();
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
            final String areaCode   = f[1];
            final double lat        = Double.parseDouble(f[2]);
            final double lon        = Double.parseDouble(f[3]);
            final int    alt        = Integer.parseInt(f[4]);
            final double windSpeed  = Double.parseDouble(f[5]);
            final double windDir    = Double.parseDouble(f[6]);
            final double temp       = Double.parseDouble(f[7]);
            final String provider   = f[8];
            final LocalDateTime dt  = LocalDateTime.parse(f[9], DT_FMT);

            controller.registerWeatherData(areaCode, lat, lon, alt, windSpeed, windDir, temp, provider, dt);
            return RemoteProtocol.ok("Weather data registered for area " + areaCode);
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
                final double lat       = Double.parseDouble(cols[0]);
                final double lon       = Double.parseDouble(cols[1]);
                final int    alt       = Integer.parseInt(cols[2]);
                final double speed     = Double.parseDouble(cols[3]);
                final double dir       = Double.parseDouble(cols[4]);
                final double temp      = Double.parseDouble(cols[5]);
                final String provider  = cols[6];
                final LocalDateTime dt = cols.length > 7
                        ? LocalDateTime.parse(cols[7], DT_FMT)
                        : LocalDateTime.now();

                controller.registerWeatherData(areaCode, lat, lon, alt, speed, dir, temp, provider, dt);
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
            final Iterable<WeatherData> records = controller.weatherDataForArea(areaCode);

            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final WeatherData wd : records) {
                if (!wd.recordedDateTime().toLocalDate().equals(filter)) continue;
                if (count > 0) sb.append(";");
                sb.append(wd.areaCode()).append(",")
                        .append(wd.windCondition().latitude()).append(",")
                        .append(wd.windCondition().longitude()).append(",")
                        .append(wd.windCondition().altitudeMetres()).append(",")
                        .append(wd.windCondition().speedKnots()).append(",")
                        .append(wd.windCondition().directionDegrees()).append(",")
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
            final Iterable<AirControlArea> areas =
                    PersistenceContext.repositories().airControlAreas().findAll();
            final StringBuilder sb = new StringBuilder();
            for (final AirControlArea a : areas) {
                if (sb.length() > 0) sb.append(";");
                sb.append(a.identity()).append(":").append(a.name());
            }
            return RemoteProtocol.ok(sb.length() > 0 ? sb.toString() : "No areas");
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }
}
