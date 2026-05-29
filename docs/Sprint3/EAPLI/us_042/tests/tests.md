# US042 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
Tests are aligned with the actual domain implementation (`WindCondition`, `WeatherData`).
The `CSVWeatherDataImporterTest` reads from `src/test/resources/`.

---

## 1. WindCondition Value Object

```java
class WindConditionTest {

    // --- speedKnots (must be strictly > 0) ---

    @Test
    void speedKnots_strictlyPositive_isValid() {
        // Arrange + Act + Assert
        assertDoesNotThrow(() ->
            new WindCondition(0.1, 180, 38.7, -9.1, 5000)
        );
    }

    @Test
    void speedKnots_zero_throwsException() {
        // Arrange
        double invalidSpeed = 0.0;

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(invalidSpeed, 180, 38.7, -9.1, 5000)
        );
    }

    @Test
    void speedKnots_negative_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(-1.0, 180, 38.7, -9.1, 5000)
        );
    }

    // --- directionDegrees (>= 0 and < 360, exclusive) ---

    @Test
    void directionDegrees_zero_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 0, 38.7, -9.1, 5000)
        );
    }

    @Test
    void directionDegrees_359_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 359, 38.7, -9.1, 5000)
        );
    }

    @Test
    void directionDegrees_360_throwsException() {
        // 360 is exclusive — must throw
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, 360, 38.7, -9.1, 5000)
        );
    }

    @Test
    void directionDegrees_negative_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, -1, 38.7, -9.1, 5000)
        );
    }

    // --- latitude (-90 to 90, inclusive) ---

    @Test
    void latitude_minus90_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 90, -90.0, -9.1, 5000)
        );
    }

    @Test
    void latitude_plus90_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 90, 90.0, -9.1, 5000)
        );
    }

    @Test
    void latitude_belowMinus90_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, 90, -90.1, -9.1, 5000)
        );
    }

    @Test
    void latitude_abovePlus90_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, 90, 90.1, -9.1, 5000)
        );
    }

    // --- longitude (-180 to 180, inclusive) ---

    @Test
    void longitude_minus180_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 90, 38.7, -180.0, 5000)
        );
    }

    @Test
    void longitude_plus180_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 90, 38.7, 180.0, 5000)
        );
    }

    @Test
    void longitude_belowMinus180_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, 90, 38.7, -180.1, 5000)
        );
    }

    @Test
    void longitude_abovePlus180_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, 90, 38.7, 180.1, 5000)
        );
    }

    // --- altitudeMetres (>= 0, int) ---

    @Test
    void altitudeMetres_zero_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(10.0, 90, 38.7, -9.1, 0)
        );
    }

    @Test
    void altitudeMetres_negative_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WindCondition(10.0, 90, 38.7, -9.1, -1)
        );
    }
}
```

---

## 2. WeatherData Aggregate Root

```java
class WeatherDataTest {

    private static final AreaCode AREA = AreaCode.valueOf("AREA01");
    private static final WindCondition VALID_WIND =
        new WindCondition(12.0, 270, 38.7, -9.1, 4000);
    private static final LocalDateTime VALID_DT =
        LocalDateTime.of(2026, 5, 1, 8, 0);

    // --- areaCode ---

    @Test
    void areaCode_valid_isAccepted() {
        assertDoesNotThrow(() ->
            new WeatherData(AREA, VALID_WIND, 18.5, "IPMA", VALID_DT)
        );
    }

    @Test
    void areaCode_null_throwsException() {
        assertThrows(Exception.class, () ->
            new WeatherData(null, VALID_WIND, 18.5, "IPMA", VALID_DT)
        );
    }

    // --- windCondition ---

    @Test
    void windCondition_null_throwsException() {
        assertThrows(Exception.class, () ->
            new WeatherData(AREA, null, 18.5, "IPMA", VALID_DT)
        );
    }

    // --- temperatureCelsius (no range constraint in domain) ---

    @Test
    void temperatureCelsius_negative_isValid() {
        assertDoesNotThrow(() ->
            new WeatherData(AREA, VALID_WIND, -45.0, "IPMA", VALID_DT)
        );
    }

    @Test
    void temperatureCelsius_positive_isValid() {
        assertDoesNotThrow(() ->
            new WeatherData(AREA, VALID_WIND, 35.0, "IPMA", VALID_DT)
        );
    }

    // --- sourceProvider ---

    @Test
    void sourceProvider_valid_isAccepted() {
        assertDoesNotThrow(() ->
            new WeatherData(AREA, VALID_WIND, 18.5, "IPMA", VALID_DT)
        );
    }

    @Test
    void sourceProvider_null_throwsException() {
        assertThrows(Exception.class, () ->
            new WeatherData(AREA, VALID_WIND, 18.5, null, VALID_DT)
        );
    }

    @Test
    void sourceProvider_blank_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WeatherData(AREA, VALID_WIND, 18.5, "   ", VALID_DT)
        );
    }

    @Test
    void sourceProvider_empty_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new WeatherData(AREA, VALID_WIND, 18.5, "", VALID_DT)
        );
    }

    // --- recordedDateTime ---

    @Test
    void recordedDateTime_valid_isAccepted() {
        assertDoesNotThrow(() ->
            new WeatherData(AREA, VALID_WIND, 18.5, "IPMA", VALID_DT)
        );
    }

    @Test
    void recordedDateTime_null_throwsException() {
        assertThrows(Exception.class, () ->
            new WeatherData(AREA, VALID_WIND, 18.5, "IPMA", null)
        );
    }

    // --- accessors ---

    @Test
    void accessors_returnCorrectValues() {
        // Arrange
        WeatherData wd = new WeatherData(AREA, VALID_WIND, 18.5, "IPMA", VALID_DT);

        // Act + Assert
        assertEquals(AREA, wd.areaCode());
        assertEquals(VALID_WIND, wd.windCondition());
        assertEquals(18.5, wd.temperatureCelsius(), 0.001);
        assertEquals("IPMA", wd.sourceProvider());
        assertEquals(VALID_DT, wd.recordedDateTime());
    }

    @Test
    void sourceProvider_isTrimmed() {
        // Arrange
        WeatherData wd = new WeatherData(AREA, VALID_WIND, 18.5, "  IPMA  ", VALID_DT);

        // Act + Assert
        assertEquals("IPMA", wd.sourceProvider());
    }
}
```

---

## 3. CSVWeatherDataImporter

```java
class CSVWeatherDataImporterTest {

    private CSVWeatherDataImporter importer;

    private static final String TEST_FILE =
        "src/test/resources/weather_data_test.csv";
    private static final String VALID_ONLY_FILE =
        "src/test/resources/weather_data_valid_only.csv";
    private static final String HEADERS_ONLY_FILE =
        "src/test/resources/weather_data_headers_only.csv";
    private static final String EMPTY_FILE =
        "src/test/resources/weather_data_empty.csv";

    @BeforeEach
    void setUp() {
        importer = new CSVWeatherDataImporter();
    }

    // --- valid records ---

    @Test
    void parse_validFile_returnsNonEmptyImportedList() {
        // Arrange + Act
        ImportResult result = importer.parse(TEST_FILE);

        // Assert
        assertFalse(result.importedRecords().isEmpty());
    }

    @Test
    void parse_validFile_firstRecordHasCorrectSourceProvider() {
        ImportResult result = importer.parse(TEST_FILE);
        WeatherData first = result.importedRecords().get(0);
        assertEquals("IPMA", first.sourceProvider());
    }

    @Test
    void parse_validFile_firstRecordHasCorrectTemperature() {
        ImportResult result = importer.parse(TEST_FILE);
        WeatherData first = result.importedRecords().get(0);
        assertEquals(18.5, first.temperatureCelsius(), 0.001);
    }

    @Test
    void parse_validFile_windConditionParsedCorrectly() {
        ImportResult result = importer.parse(TEST_FILE);
        WindCondition wind = result.importedRecords().get(0).windCondition();
        assertEquals(12.0, wind.speedKnots(), 0.001);
        assertEquals(270, wind.directionDegrees());
    }

    // --- partial import ---

    @Test
    void parse_mixedFile_importedAndErrorListsBothNonEmpty() {
        ImportResult result = importer.parse(TEST_FILE);
        assertFalse(result.importedRecords().isEmpty());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void parse_mixedFile_invalidRecordsNotInImportedList() {
        // Arrange + Act
        ImportResult result = importer.parse(TEST_FILE);

        // Assert — no imported record has a blank sourceProvider
        result.importedRecords().forEach(wd ->
            assertFalse(wd.sourceProvider().isBlank())
        );
    }

    // --- specific invalid cases ---

    @Test
    void parse_missingSourceProvider_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("sourceProvider")));
    }

    @Test
    void parse_zeroWindSpeed_producesError() {
        // speedKnots must be > 0; zero is invalid
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("speedKnots")));
    }

    @Test
    void parse_negativeWindSpeed_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("speedKnots")));
    }

    @Test
    void parse_direction360_producesError() {
        // 360 is exclusive — must produce error
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("directionDegrees")));
    }

    @Test
    void parse_latitudeOutOfRange_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("latitude")));
    }

    @Test
    void parse_longitudeOutOfRange_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("longitude")));
    }

    @Test
    void parse_negativeAltitude_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("altitudeMetres")));
    }

    @Test
    void parse_missingColumns_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("missing fields")));
    }

    @Test
    void parse_nonNumericWindSpeed_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("invalid number")));
    }

    @Test
    void parse_malformedDatetime_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("recordedDateTime")));
    }

    @Test
    void parse_missingAreaCode_producesError() {
        ImportResult result = importer.parse(TEST_FILE);
        assertTrue(result.errors().stream()
            .anyMatch(e -> e.contains("areaCode")));
    }

    // --- edge cases ---

    @Test
    void parse_validOnlyFile_errorListIsEmpty() {
        ImportResult result = importer.parse(VALID_ONLY_FILE);
        assertTrue(result.errors().isEmpty());
        assertFalse(result.importedRecords().isEmpty());
    }

    @Test
    void parse_headersOnlyFile_returnsEmptyResult() {
        ImportResult result = importer.parse(HEADERS_ONLY_FILE);
        assertTrue(result.importedRecords().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parse_emptyFile_returnsEmptyResult() {
        ImportResult result = importer.parse(EMPTY_FILE);
        assertTrue(result.importedRecords().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parse_nonExistentFile_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            importer.parse("nonexistent.csv")
        );
    }
}
```

---

## 4. ImportWeatherDataController

```java
class ImportWeatherDataControllerTest {

    private ImportWeatherDataController controller;
    private WeatherDataRepository repositoryMock;

    private static final String TEST_FILE =
        "src/test/resources/weather_data_test.csv";
    private static final String VALID_ONLY_FILE =
        "src/test/resources/weather_data_valid_only.csv";

    @BeforeEach
    void setUp() {
        repositoryMock = mock(WeatherDataRepository.class);
        controller = new ImportWeatherDataController(
            new WeatherDataImporterFactory(),
            repositoryMock
        );
    }

    @Test
    void importWeatherData_validFile_savesOnlyValidRecords() {
        // Arrange + Act
        ImportResult result = controller.importWeatherData(VALID_ONLY_FILE);

        // Assert
        verify(repositoryMock, times(result.importedRecords().size()))
            .save(any(WeatherData.class));
    }

    @Test
    void importWeatherData_mixedFile_doesNotSaveInvalidRecords() {
        // Arrange + Act
        ImportResult result = controller.importWeatherData(TEST_FILE);

        // Assert — save called exactly for valid records, never for invalid ones
        int expectedSaves = result.importedRecords().size();
        verify(repositoryMock, times(expectedSaves)).save(any(WeatherData.class));
    }

    @Test
    void importWeatherData_returnsNonNullResult() {
        assertNotNull(controller.importWeatherData(TEST_FILE));
    }

    @Test
    void importWeatherData_validOnlyFile_hasNoErrors() {
        ImportResult result = controller.importWeatherData(VALID_ONLY_FILE);
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void importWeatherData_mixedFile_hasErrors() {
        ImportResult result = controller.importWeatherData(TEST_FILE);
        assertFalse(result.errors().isEmpty());
    }
}
```

---

## 5. Tests Based on Professor's Datasets (HP_WeatherData / CW_WeatherData)

> The professor provided two reference datasets — Happy Weather (`HP_WeatherData_v0.xlsx`) and
> Crazy Weather (`CW_WeatherData_v0.xlsx`) — for ACA 121. The original format uses bounding boxes,
> altitude ranges in feet, and a time range (Day + Start + End). For import via `CSVWeatherDataImporter`,
> these are adapted as follows:
> - **Latitude/longitude**: centre point of each bounding box
> - **Altitude**: lower bound of the altitude range converted to metres (1 ft = 0.3048 m)
> - **recordedDateTime**: Day (Excel serial → date) + Start time → `LocalDateTime`
> - **Temperature**: not present in professor's format — a representative value is used

```java
class ProfessorDatasetTest {

    private CSVWeatherDataImporter importer;

    private static final String TEST_FILE =
        "src/test/resources/weather_data_test.csv";
    private static final String VALID_ONLY_FILE =
        "src/test/resources/weather_data_valid_only.csv";

    @BeforeEach
    void setUp() {
        importer = new CSVWeatherDataImporter();
    }

    // --- HP_WeatherData: normal conditions ---

    @Test
    void windCondition_hpDataset_lowAltitude_isValid() {
        // HP data: Box1, 0-1000ft, dir=90, speed=25kt
        assertDoesNotThrow(() ->
            new WindCondition(25.0, 90, 42.0327, -8.8729, 0)
        );
    }

    @Test
    void windCondition_hpDataset_midAltitude_isValid() {
        // HP data: Box1, 1001-2000ft → 305m, dir=105, speed=35kt
        assertDoesNotThrow(() ->
            new WindCondition(35.0, 105, 42.0327, -8.8729, 305)
        );
    }

    @Test
    void windCondition_hpDataset_highAltitude_isValid() {
        // HP data: 7001-12000ft → 2133m, dir=170, speed=85kt
        assertDoesNotThrow(() ->
            new WindCondition(85.0, 170, 42.0327, -8.8729, 2133)
        );
    }

    @Test
    void windCondition_hpDataset_direction15_isValid() {
        // HP data: Box7, dir=15 (minimum direction in dataset)
        assertDoesNotThrow(() ->
            new WindCondition(20.0, 15, 42.0327, 2.718, 0)
        );
    }

    // --- CW_WeatherData: high wind speeds ---

    @Test
    void windCondition_cwDataset_highSpeed_97kt_isValid() {
        // CW data: 7001-12000ft, speed=97.75kt — very high but valid
        assertDoesNotThrow(() ->
            new WindCondition(97.75, 170, 42.0327, -8.8729, 2133)
        );
    }

    @Test
    void windCondition_cwDataset_highSpeed_103kt_isValid() {
        // CW data: maximum speed in dataset — 103.5kt
        assertDoesNotThrow(() ->
            new WindCondition(103.5, 170, 42.482, -5.1451, 2133)
        );
    }

    @Test
    void windCondition_cwDataset_decimalSpeed_isValid() {
        // CW data: fractional knot values (e.g. 28.75, 57.5)
        assertDoesNotThrow(() ->
            new WindCondition(28.75, 90, 42.0327, -8.8729, 0)
        );
    }

    @Test
    void windCondition_cwDataset_speed57pt5_isValid() {
        assertDoesNotThrow(() ->
            new WindCondition(57.5, 115, 39.0882, -5.1451, 609)
        );
    }

    // --- Import of professor data via CSV ---

    @Test
    void parse_hpDatasetRecords_importedSuccessfully() {
        ImportResult result = importer.parse(VALID_ONLY_FILE);
        long hpCount = result.importedRecords().stream()
            .filter(wd -> wd.sourceProvider().equals("HP_WeatherData"))
            .count();
        assertTrue(hpCount > 0);
    }

    @Test
    void parse_cwDatasetRecords_importedSuccessfully() {
        ImportResult result = importer.parse(VALID_ONLY_FILE);
        long cwCount = result.importedRecords().stream()
            .filter(wd -> wd.sourceProvider().equals("CW_WeatherData"))
            .count();
        assertTrue(cwCount > 0);
    }

    @Test
    void parse_hpAndCwRecords_correctAreaCode() {
        ImportResult result = importer.parse(VALID_ONLY_FILE);
        result.importedRecords().stream()
            .filter(wd -> wd.sourceProvider().startsWith("HP_") ||
                          wd.sourceProvider().startsWith("CW_"))
            .forEach(wd ->
                assertEquals("121", wd.areaCode().toString())
            );
    }

    @Test
    void parse_cwDataset_highSpeedRecord_importedCorrectly() {
        // CW max speed: 103.5kt — must be imported without error
        ImportResult result = importer.parse(TEST_FILE);
        boolean found = result.importedRecords().stream()
            .anyMatch(wd -> wd.sourceProvider().equals("CW_WeatherData")
                && wd.windCondition().speedKnots() == 103.5);
        assertTrue(found);
    }

    @Test
    void parse_cwDataset_recordedDateTime_parsedCorrectly() {
        // All professor records use 2026-06-22T05:00:00
        ImportResult result = importer.parse(VALID_ONLY_FILE);
        result.importedRecords().stream()
            .filter(wd -> wd.sourceProvider().startsWith("HP_") ||
                          wd.sourceProvider().startsWith("CW_"))
            .forEach(wd ->
                assertEquals(
                    LocalDateTime.of(2026, 6, 22, 5, 0),
                    wd.recordedDateTime()
                )
            );
    }
}
```
