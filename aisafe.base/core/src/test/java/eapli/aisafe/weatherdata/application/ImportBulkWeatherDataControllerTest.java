package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportBulkWeatherDataControllerTest {

    @TempDir
    Path tempDir;

    private AuthorizationService authz;
    private WeatherDataRepository repo;
    private AirControlAreaRepository acaRepo;
    private ImportBulkWeatherDataController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(WeatherDataRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        controller = new ImportBulkWeatherDataController(authz, repo, acaRepo);

        when(acaRepo.ofIdentity(AreaCode.valueOf("LPPC"))).thenReturn(Optional.of(mock()));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureImportFromCsvSavesWeatherData() throws IOException {
        final Path csv = tempDir.resolve("valid.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "1;38,0;-10,0;39,0;-9,0;0;2000;270;15,5;14/05/2026;10:00;12:00\n");

        final ImportBulkWeatherDataController.ImportResult result = controller.importFromCsv(csv);

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());
        assertFalse(result.hasErrors());
        verify(repo).save(any());
    }

    @Test
    void ensureImportChecksAuthorization() throws IOException {
        final Path csv = tempDir.resolve("empty.csv");
        Files.writeString(csv, "");

        controller.importFromCsv(csv);

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── CSV format ────────────────────────────────────────────────────────────

    @Test
    void ensureMultipleValidLinesAreAllImported() throws IOException {
        when(acaRepo.ofIdentity(AreaCode.valueOf("LPPC"))).thenReturn(Optional.of(mock()));
        when(acaRepo.ofIdentity(AreaCode.valueOf("WEFIR"))).thenReturn(Optional.of(mock()));

        final Path csv = tempDir.resolve("multi.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "# ACA 2 = WEFIR\n" +
                "1;38,0;-10,0;39,0;-9,0;0;2000;270;15,5;14/05/2026;10:00;12:00\n" +
                "2;40,0;-8,0;41,0;-7,0;500;2500;180;20,0;15/05/2026;14:30;16:30\n");

        final var result = controller.importFromCsv(csv);

        assertEquals(2, result.imported());
        assertEquals(0, result.skipped());
        verify(repo, times(2)).save(any());
    }

    @Test
    void ensureBlankLinesAreSkipped() throws IOException {
        final Path csv = tempDir.resolve("blanks.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "\n" +
                "1;38,0;-10,0;39,0;-9,0;0;2000;270;15,5;14/05/2026;10:00;12:00\n" +
                "   \n");

        final var result = controller.importFromCsv(csv);

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    void ensureInvalidColumnCountIsSkipped() throws IOException {
        final Path csv = tempDir.resolve("badcols.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "1;38,0;-10,0;39,0;-9,0\n");  // only 6 columns

        final var result = controller.importFromCsv(csv);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().get(0).contains("Expected 12 columns"));
    }

    @Test
    void ensureMissingAcaHeaderIsSkipped() throws IOException {
        final Path csv = tempDir.resolve("noheader.csv");
        Files.writeString(csv,
                "1;38,0;-10,0;39,0;-9,0;0;2000;270;15,5;14/05/2026;10:00;12:00\n");

        final var result = controller.importFromCsv(csv);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("Unknown ACA ID"));
    }

    @Test
    void ensureInvalidNumberFormatIsSkipped() throws IOException {
        final Path csv = tempDir.resolve("badnum.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "1;abc;-10,0;39,0;-9,0;0;2000;270;15,5;14/05/2026;10:00;12:00\n");

        final var result = controller.importFromCsv(csv);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("Invalid lat1"));
    }

    @Test
    void ensureInvalidDateIsSkipped() throws IOException {
        final Path csv = tempDir.resolve("baddate.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "1;38,0;-10,0;39,0;-9,0;0;2000;270;15,5;bad-date;10:00;12:00\n");

        final var result = controller.importFromCsv(csv);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("Invalid date/time"));
    }

    @Test
    void ensureUnknownAcaInHeaderThrows() throws IOException {
        final Path csv = tempDir.resolve("unknownaca.csv");
        when(acaRepo.ofIdentity(AreaCode.valueOf("XXXX"))).thenReturn(Optional.empty());
        Files.writeString(csv, "# ACA 1 = XXXX\n");

        assertThrows(IllegalArgumentException.class, () -> controller.importFromCsv(csv),
                "Unknown ACA in header must be rejected");
    }

    @Test
    void ensureEuropeanDecimalIsParsedCorrectly() throws IOException {
        when(acaRepo.ofIdentity(AreaCode.valueOf("LPPC"))).thenReturn(Optional.of(mock()));
        when(acaRepo.ofIdentity(AreaCode.valueOf("KZNE"))).thenReturn(Optional.of(mock()));
        when(acaRepo.ofIdentity(AreaCode.valueOf("WEFIR"))).thenReturn(Optional.of(mock()));

        final Path csv = tempDir.resolve("eu.csv");
        Files.writeString(csv,
                "# ACA 1 = LPPC\n" +
                "# ACA 2 = KZNE\n" +
                "# ACA 3 = WEFIR\n" +
                "1;38,7;-9,1;39,1;-8,5;0;1000;270;15,5;14/05/2026;10:00;12:00\n" +
                "2;41,3;-8,7;42,0;-7,9;500;2500;180;20,0;15/05/2026;14:30;16:30\n" +
                "3;65,0;-18,0;66,0;-17,0;1000;3000;90;30,0;16/05/2026;08:00;10:00\n");

        final var result = controller.importFromCsv(csv);

        assertEquals(3, result.imported());
        assertEquals(0, result.skipped());
    }

    @Test
    void ensureResultToString() {
        final var result = new ImportBulkWeatherDataController.ImportResult(5, 2, java.util.List.of("err1", "err2"));
        assertTrue(result.toString().contains("Imported: 5"));
        assertTrue(result.toString().contains("Skipped: 2"));
        assertTrue(result.toString().contains("Errors: 2"));
    }

    @Test
    void ensureResultHasErrorsReturnsTrueWhenErrorsExist() {
        final var result = new ImportBulkWeatherDataController.ImportResult(0, 1, java.util.List.of("error"));
        assertTrue(result.hasErrors());
    }

    @Test
    void ensureResultHasErrorsReturnsFalseWhenNoErrors() {
        final var result = new ImportBulkWeatherDataController.ImportResult(1, 0, java.util.List.of());
        assertFalse(result.hasErrors());
    }
}
