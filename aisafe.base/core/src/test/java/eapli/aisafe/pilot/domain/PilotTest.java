package eapli.aisafe.pilot.domain;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PilotTest {

    private static final AircraftModelCode DEFAULT_MODEL = AircraftModelCode.valueOf("B738");

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us075/pilot_full_test.csv", numLinesToSkip = 1)
    void ensurePilotInvariants(final String testCaseId, final String pilotId,
                               final String companyIata, final String certifiedModels,
                               final String certDate, final boolean expectedValid) {
        final var models = (certifiedModels == null || certifiedModels.isBlank())
                ? Set.<AircraftModelCode>of()
                : Set.<AircraftModelCode>of(AircraftModelCode.valueOf(certifiedModels.trim()));
        if (expectedValid) {
            assertDoesNotThrow(() -> new Pilot(
                    PilotId.valueOf(pilotId),
                    CompanyIATA.valueOf(companyIata),
                    models.isEmpty() ? Set.<AircraftModelCode>of(DEFAULT_MODEL) : models,
                    certDate == null || certDate.isBlank() ? LocalDate.now() : LocalDate.parse(certDate)));
        } else {
            assertThrows(Exception.class, () -> new Pilot(
                    pilotId == null || pilotId.isBlank() ? null : PilotId.valueOf(pilotId),
                    companyIata == null || companyIata.isBlank() ? null : CompanyIATA.valueOf(companyIata),
                    models,
                    certDate == null || certDate.isBlank() ? LocalDate.now() : LocalDate.parse(certDate)));
        }
    }
}
