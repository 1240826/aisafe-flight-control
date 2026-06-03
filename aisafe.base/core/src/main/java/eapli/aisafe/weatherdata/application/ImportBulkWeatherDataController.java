package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UseCaseController
public class ImportBulkWeatherDataController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String CSV_SEPARATOR = ";";
    private static final int EXPECTED_COLUMNS = 12;
    private static final double FEET_TO_METRES = 0.3048;

    private final AuthorizationService authz;
    private final WeatherDataRepository repo;
    private final AirControlAreaRepository acaRepo;

    public ImportBulkWeatherDataController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    ImportBulkWeatherDataController(final AuthorizationService authz,
                                     final WeatherDataRepository repo,
                                     final AirControlAreaRepository acaRepo) {
        this.authz = authz;
        this.repo = repo;
        this.acaRepo = acaRepo;
    }

    public ImportResult importFromCsv(final Path csvPath) throws IOException {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON);

        final List<String> lines = Files.readAllLines(csvPath);
        final Map<String, AreaCode> acaMapping = new HashMap<>();
        final List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
            final String line = lines.get(lineNum).trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("#")) {
                parseHeader(line, acaMapping);
                continue;
            }
            try {
                final WeatherData wd = parseLine(line, lineNum + 1, acaMapping);
                repo.save(wd);
                imported++;
            } catch (final Exception e) {
                errors.add("Line " + (lineNum + 1) + ": " + e.getMessage());
                skipped++;
            }
        }

        return new ImportResult(imported, skipped, errors);
    }

    private void parseHeader(final String line, final Map<String, AreaCode> acaMapping) {
        final String header = line.substring(1).trim();
        if (header.startsWith("ACA")) {
            final String[] parts = header.split("=");
            if (parts.length == 2) {
                final String numericId = parts[0].replace("ACA", "").trim();
                final String areaCodeStr = parts[1].trim();
                acaMapping.put(numericId, AreaCode.valueOf(areaCodeStr));

                acaRepo.ofIdentity(AreaCode.valueOf(areaCodeStr))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown ACA code in header: " + areaCodeStr));
            }
        }
    }

    private WeatherData parseLine(final String line, final int lineNum,
                                   final Map<String, AreaCode> acaMapping) {
        final String[] parts = line.split(CSV_SEPARATOR, -1);
        if (parts.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException("Expected " + EXPECTED_COLUMNS
                    + " columns but found " + parts.length);
        }

        final String acaId = parts[0].trim();
        final AreaCode areaCode = acaMapping.get(acaId);
        if (areaCode == null) {
            throw new IllegalArgumentException("Unknown ACA ID: " + acaId
                    + ". Add a header mapping like '# ACA " + acaId + " = LPPC'");
        }

        final double lat1 = parseEuropeanDouble(parts[1], "lat1", lineNum);
        final double lon1 = parseEuropeanDouble(parts[2], "lon1", lineNum);
        final double lat2 = parseEuropeanDouble(parts[3], "lat2", lineNum);
        final double lon2 = parseEuropeanDouble(parts[4], "lon2", lineNum);
        final int altInfFt = parseInt(parts[5], "alt_inf", lineNum);
        final int altSupFt = parseInt(parts[6], "alt_sup", lineNum);
        final int direction = parseInt(parts[7], "direction", lineNum);
        final double windSpeed = parseEuropeanDouble(parts[8], "value", lineNum);

        final String dayStr = parts[9].trim();
        final String startStr = parts[10].trim();
        final String endStr = parts[11].trim();

        final LocalDateTime dateTime = parseDateTime(dayStr, startStr, lineNum);

        final double centerLat = (lat1 + lat2) / 2.0;
        final double centerLon = (lon1 + lon2) / 2.0;
        final int midAltMetres = (int) Math.round(((altInfFt + altSupFt) / 2.0) * FEET_TO_METRES);

        return new WeatherData(
                areaCode,
                new WindCondition(windSpeed, direction, centerLat, centerLon, midAltMetres),
                0.0,
                "WEATHER_FORECAST",
                dateTime);
    }

    private double parseEuropeanDouble(final String raw, final String field, final int lineNum) {
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + " value: '" + raw + "' (line " + lineNum + ")");
        }
    }

    private int parseInt(final String raw, final String field, final int lineNum) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + " value: '" + raw + "' (line " + lineNum + ")");
        }
    }

    private LocalDateTime parseDateTime(final String dayStr, final String startStr, final int lineNum) {
        try {
            final LocalDate day = LocalDate.parse(dayStr, DATE_FMT);
            final LocalTime start = LocalTime.parse(startStr, TIME_FMT);
            return LocalDateTime.of(day, start);
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date/time: day='" + dayStr + "' start='" + startStr
                            + "'. Expected dd/MM/yyyy and HH:mm (line " + lineNum + ")");
        }
    }

    public static final class ImportResult {
        private final int imported;
        private final int skipped;
        private final List<String> errors;

        public ImportResult(final int imported, final int skipped, final List<String> errors) {
            this.imported = imported;
            this.skipped = skipped;
            this.errors = errors;
        }

        public int imported() { return imported; }
        public int skipped() { return skipped; }
        public List<String> errors() { return errors; }
        public boolean hasErrors() { return !errors.isEmpty(); }

        @Override
        public String toString() {
            return String.format("Imported: %d | Skipped: %d | Errors: %d",
                    imported, skipped, errors.size());
        }
    }
}
