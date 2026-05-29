# US112 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
These are TDD tests — `ReportGenerator`, `MonthlyStatisticsReportGenerator`, `ReportWriter`,
and `GenerateMonthlyReportController` do not exist yet and must be implemented to make these
tests pass.

---

## 1. ReportGenerator — Template Method Structure

```java
class ReportGeneratorTest {

    /**
     * Concrete stub that implements the abstract methods with minimal content,
     * used to test the shared skeleton defined in ReportGenerator.
     */
    static class StubReportGenerator extends ReportGenerator {

        @Override
        protected ReportData collectData() {
            return new ReportData("Stub Report", YearMonth.of(2026, 5));
        }

        @Override
        protected List<ReportSection> buildSections(ReportData data) {
            return List.of(new ReportSection("Section 1", "Content"));
        }
    }

    private StubReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StubReportGenerator();
    }

    @Test
    void generate_returnsNonNullReport() {
        // Arrange + Act
        Report report = generator.generate(YearMonth.of(2026, 5));

        // Assert
        assertNotNull(report);
    }

    @Test
    void generate_reportContainsHeader() {
        // Arrange + Act
        Report report = generator.generate(YearMonth.of(2026, 5));

        // Assert — header is part of the shared skeleton
        assertNotNull(report.header());
        assertFalse(report.header().isBlank());
    }

    @Test
    void generate_reportContainsFooter() {
        Report report = generator.generate(YearMonth.of(2026, 5));
        assertNotNull(report.footer());
        assertFalse(report.footer().isBlank());
    }

    @Test
    void generate_reportContainsBranding() {
        // Arrange + Act
        Report report = generator.generate(YearMonth.of(2026, 5));

        // Assert — AISafe branding present in header or footer
        String content = report.header() + report.footer();
        assertTrue(content.contains("AISafe"));
    }

    @Test
    void generate_reportContainsSections() {
        Report report = generator.generate(YearMonth.of(2026, 5));
        assertFalse(report.sections().isEmpty());
    }

    @Test
    void generate_nullYearMonth_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            generator.generate(null)
        );
    }

    @Test
    void generate_futureMonth_throwsException() {
        // Cannot generate a report for a month that hasn't happened yet
        YearMonth futureMonth = YearMonth.now().plusMonths(1);
        assertThrows(IllegalArgumentException.class, () ->
            generator.generate(futureMonth)
        );
    }

    @Test
    void generate_differentGenerators_sameHeaderStructure() {
        // Arrange — two different concrete generators
        ReportGenerator stub1 = new StubReportGenerator();
        ReportGenerator stub2 = new StubReportGenerator();
        YearMonth month = YearMonth.of(2026, 5);

        // Act
        Report r1 = stub1.generate(month);
        Report r2 = stub2.generate(month);

        // Assert — header structure is identical (Template Method guarantee)
        assertEquals(r1.header(), r2.header());
        assertEquals(r1.footer(), r2.footer());
    }
}
```

---

## 2. MonthlyStatisticsReportGenerator

```java
class MonthlyStatisticsReportGeneratorTest {

    private MonthlyStatisticsReportGenerator generator;
    private FlightRepository flightRepoMock;

    @BeforeEach
    void setUp() {
        flightRepoMock = mock(FlightRepository.class);
        generator = new MonthlyStatisticsReportGenerator(flightRepoMock);
    }

    @Test
    void generate_monthWithFlights_reportContainsFlightCount() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        when(flightRepoMock.findByMonth(month)).thenReturn(buildFlightList(3));

        // Act
        Report report = generator.generate(month);

        // Assert — flight count present in report content
        String content = report.sectionsContent();
        assertTrue(content.contains("3"));
    }

    @Test
    void generate_monthWithNoFlights_reportIndicatesZeroFlights() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 3);
        when(flightRepoMock.findByMonth(month)).thenReturn(List.of());

        // Act
        Report report = generator.generate(month);

        // Assert — zero flights reported, no exception
        assertNotNull(report);
        String content = report.sectionsContent();
        assertTrue(content.contains("0"));
    }

    @Test
    void generate_reportContainsTargetMonth() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        when(flightRepoMock.findByMonth(month)).thenReturn(List.of());

        // Act
        Report report = generator.generate(month);

        // Assert — month and year present in report
        String content = report.sectionsContent();
        assertTrue(content.contains("May") || content.contains("2026-05"));
    }

    @Test
    void generate_inheritsBrandingFromReportGenerator() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        when(flightRepoMock.findByMonth(month)).thenReturn(List.of());

        // Act
        Report report = generator.generate(month);

        // Assert — branding from parent skeleton
        assertTrue((report.header() + report.footer()).contains("AISafe"));
    }

    @Test
    void generate_queriesOnlyTargetMonth() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        when(flightRepoMock.findByMonth(month)).thenReturn(List.of());

        // Act
        generator.generate(month);

        // Assert — repository queried exactly once for the correct month
        verify(flightRepoMock, times(1)).findByMonth(month);
        verify(flightRepoMock, never()).findByMonth(argThat(m -> !m.equals(month)));
    }

    // --- helper ---

    private List<Flight> buildFlightList(int count) {
        List<Flight> flights = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            flights.add(mock(Flight.class));
        }
        return flights;
    }
}
```

---

## 3. ReportWriter

```java
class ReportWriterTest {

    private ReportWriter writer;
    private Path outputDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        outputDir = tempDir;
        writer = new ReportWriter(outputDir);
    }

    @Test
    void write_validReport_fileIsCreated() throws Exception {
        // Arrange
        Report report = buildReport("monthly_2026_05");

        // Act
        Path filePath = writer.write(report);

        // Assert
        assertTrue(Files.exists(filePath));
    }

    @Test
    void write_validReport_fileIsNotEmpty() throws Exception {
        // Arrange
        Report report = buildReport("monthly_2026_05");

        // Act
        Path filePath = writer.write(report);

        // Assert
        assertTrue(Files.size(filePath) > 0);
    }

    @Test
    void write_validReport_fileNameContainsReportTitle() throws Exception {
        // Arrange
        Report report = buildReport("monthly_2026_05");

        // Act
        Path filePath = writer.write(report);

        // Assert
        assertTrue(filePath.getFileName().toString().contains("monthly_2026_05"));
    }

    @Test
    void write_nullReport_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            writer.write(null)
        );
    }

    @Test
    void write_calledTwice_producesTwoFiles() throws Exception {
        // Arrange
        Report r1 = buildReport("monthly_2026_05");
        Report r2 = buildReport("monthly_2026_06");

        // Act
        Path p1 = writer.write(r1);
        Path p2 = writer.write(r2);

        // Assert — two distinct files
        assertNotEquals(p1, p2);
        assertTrue(Files.exists(p1));
        assertTrue(Files.exists(p2));
    }

    // --- helper ---

    private Report buildReport(String title) {
        return Report.builder()
            .title(title)
            .header("AISafe Report Header")
            .footer("AISafe Report Footer")
            .sections(List.of(new ReportSection("Stats", "3 flights")))
            .build();
    }
}
```

---

## 4. GenerateMonthlyReportController

```java
class GenerateMonthlyReportControllerTest {

    private GenerateMonthlyReportController controller;
    private MonthlyStatisticsReportGenerator generatorMock;
    private ReportWriter writerMock;

    @BeforeEach
    void setUp() {
        generatorMock = mock(MonthlyStatisticsReportGenerator.class);
        writerMock = mock(ReportWriter.class);
        controller = new GenerateMonthlyReportController(generatorMock, writerMock);
    }

    @Test
    void generateMonthlyReport_callsGenerator() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        Report report = mock(Report.class);
        when(generatorMock.generate(month)).thenReturn(report);
        when(writerMock.write(report)).thenReturn(Path.of("report.pdf"));

        // Act
        controller.generateMonthlyReport(month);

        // Assert
        verify(generatorMock).generate(month);
    }

    @Test
    void generateMonthlyReport_callsWriter() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        Report report = mock(Report.class);
        when(generatorMock.generate(month)).thenReturn(report);
        when(writerMock.write(report)).thenReturn(Path.of("report.pdf"));

        // Act
        controller.generateMonthlyReport(month);

        // Assert
        verify(writerMock).write(report);
    }

    @Test
    void generateMonthlyReport_returnsFilePath() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        Report report = mock(Report.class);
        Path expectedPath = Path.of("report.pdf");
        when(generatorMock.generate(month)).thenReturn(report);
        when(writerMock.write(report)).thenReturn(expectedPath);

        // Act
        Path result = controller.generateMonthlyReport(month);

        // Assert
        assertEquals(expectedPath, result);
    }

    @Test
    void generateMonthlyReport_nullMonth_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            controller.generateMonthlyReport(null)
        );
    }

    @Test
    void generateMonthlyReport_futureMonth_throwsException() {
        YearMonth futureMonth = YearMonth.now().plusMonths(1);
        assertThrows(IllegalArgumentException.class, () ->
            controller.generateMonthlyReport(futureMonth)
        );
    }
}
```

---

## 5. Notes for Implementation

The following classes/methods must be created for these tests to compile and pass:

| Class / Method | Note |
|---|---|
| `ReportGenerator` | Abstract class — Template Method: `generate()`, `collectData()`, `buildSections()` |
| `Report` | Immutable result object — header, footer, sections, title |
| `ReportSection` | A single named section with content |
| `ReportData` | Data container passed from `collectData()` to `buildSections()` |
| `MonthlyStatisticsReportGenerator` | Extends `ReportGenerator` — queries `FlightRepository.findByMonth()` |
| `ReportWriter` | Writes `Report` to a file — returns the file path |
| `GenerateMonthlyReportController` | Auth + orchestration — calls generator then writer |
| `FlightRepository.findByMonth(YearMonth)` | New query needed by `MonthlyStatisticsReportGenerator` |
