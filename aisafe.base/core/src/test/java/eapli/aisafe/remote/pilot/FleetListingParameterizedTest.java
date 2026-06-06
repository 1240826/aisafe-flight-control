package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraft.domain.OperationalStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FleetListingParameterizedTest {

    @ParameterizedTest(name = "{0}: {5}")
    @MethodSource("csvTestData")
    void aircraftDtoConversionScenarios(
            final String testCaseId,
            final String registration,
            final String modelCode,
            final String operationalStatus,
            final int totalCapacity,
            final String description) {

        final var isDecommissioned = "DECOMMISSIONED".equalsIgnoreCase(operationalStatus);
        final var ac = new Aircraft(
                new RegistrationNumber(registration, "Portugal"),
                AircraftModelCode.valueOf(modelCode),
                eapli.aisafe.company.domain.CompanyIATA.valueOf("TP"),
                2,
                cabinConfigForCapacity(totalCapacity),
                LocalDate.of(2020, 1, 15));

        if (isDecommissioned) {
            ac.decommission();
        }

        final var dto = AircraftDTO.from(ac);

        assertAll(
                () -> assertEquals(registration, dto.registrationNumber(),
                        testCaseId + " registration mismatch"),
                () -> assertEquals(modelCode, dto.aircraftModelCode(),
                        testCaseId + " modelCode mismatch"),
                () -> assertEquals(operationalStatus.toUpperCase(), dto.operationalStatus(),
                        testCaseId + " operationalStatus mismatch"),
                () -> assertEquals(totalCapacity, dto.totalCapacity(),
                        testCaseId + " totalCapacity mismatch"));
    }

    private static CabinConfiguration cabinConfigForCapacity(final int capacity) {
        return new CabinConfiguration(List.of(new SeatClass("Economy", capacity)));
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                FleetListingParameterizedTest.class.getResourceAsStream(
                        "/fleet_listing_test_data.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("testCaseId")) continue;
                final var parts = line.split(",", 6);
                if (parts.length < 6) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        Integer.parseInt(parts[4].trim()),
                        parts[5].trim()));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }
}
