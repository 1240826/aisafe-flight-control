# US082 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
These are TDD tests — `Flight.addWeatherData()` and `AddWeatherDataToFlightController`
do not exist yet and must be implemented to make these tests pass.

---

## 1. Flight Domain — addWeatherData()

```java
class FlightAddWeatherDataTest {

    private WeatherData weatherData;
    private Flight flightWithDraftPlan;
    private Flight flightWithTestedPlan;
    private Pilot pilot;

    @BeforeEach
    void setUp() {
        pilot = buildPilot();
        weatherData = buildWeatherData();
        flightWithDraftPlan = buildFlight(pilot, FlightPlanStatus.DRAFT);
        flightWithTestedPlan = buildFlight(pilot, FlightPlanStatus.TESTED);
    }

    // --- status transitions ---

    @Test
    void addWeatherData_draftPlan_statusRemainesDraft() {
        // Arrange — flight plan is DRAFT
        assertEquals(FlightPlanStatus.DRAFT, flightWithDraftPlan.flightPlanStatus());

        // Act
        flightWithDraftPlan.addWeatherData(weatherData);

        // Assert — status unchanged
        assertEquals(FlightPlanStatus.DRAFT, flightWithDraftPlan.flightPlanStatus());
    }

    @Test
    void addWeatherData_testedPlan_statusResetToDraft() {
        // Arrange — flight plan is TESTED
        assertEquals(FlightPlanStatus.TESTED, flightWithTestedPlan.flightPlanStatus());

        // Act
        flightWithTestedPlan.addWeatherData(weatherData);

        // Assert — voided: back to DRAFT
        assertEquals(FlightPlanStatus.DRAFT, flightWithTestedPlan.flightPlanStatus());
    }

    @Test
    void addWeatherData_testedPlan_weatherDataIsAssociated() {
        // Arrange
        assertNull(flightWithTestedPlan.weatherData());

        // Act
        flightWithTestedPlan.addWeatherData(weatherData);

        // Assert — weather data correctly linked
        assertEquals(weatherData, flightWithTestedPlan.weatherData());
    }

    @Test
    void addWeatherData_draftPlan_weatherDataIsAssociated() {
        // Arrange
        assertNull(flightWithDraftPlan.weatherData());

        // Act
        flightWithDraftPlan.addWeatherData(weatherData);

        // Assert
        assertEquals(weatherData, flightWithDraftPlan.weatherData());
    }

    // --- replacing existing weather data ---

    @Test
    void addWeatherData_replaceExisting_draftPlan_statusRemainesDraft() {
        // Arrange — weather data already set, plan in DRAFT
        flightWithDraftPlan.addWeatherData(weatherData);
        WeatherData newWeatherData = buildWeatherData();

        // Act
        flightWithDraftPlan.addWeatherData(newWeatherData);

        // Assert — still DRAFT, new data associated
        assertEquals(FlightPlanStatus.DRAFT, flightWithDraftPlan.flightPlanStatus());
        assertEquals(newWeatherData, flightWithDraftPlan.weatherData());
    }

    @Test
    void addWeatherData_replaceExisting_testedPlan_statusResetToDraft() {
        // Arrange — first add (voids test), then re-test, then add again
        flightWithTestedPlan.addWeatherData(weatherData);
        flightWithTestedPlan.markAsTested();          // simulate re-test
        assertEquals(FlightPlanStatus.TESTED, flightWithTestedPlan.flightPlanStatus());

        WeatherData newWeatherData = buildWeatherData();

        // Act
        flightWithTestedPlan.addWeatherData(newWeatherData);

        // Assert — voided again
        assertEquals(FlightPlanStatus.DRAFT, flightWithTestedPlan.flightPlanStatus());
        assertEquals(newWeatherData, flightWithTestedPlan.weatherData());
    }

    // --- null guard ---

    @Test
    void addWeatherData_null_throwsException() {
        assertThrows(Exception.class, () ->
            flightWithDraftPlan.addWeatherData(null)
        );
    }

    // --- helpers ---

    private WeatherData buildWeatherData() {
        return new WeatherData(
            AreaCode.valueOf("AREA01"),
            new WindCondition(12.0, 270, 38.7, -9.1, 4000),
            18.5, "IPMA",
            LocalDateTime.of(2026, 5, 1, 8, 0)
        );
    }

    private Pilot buildPilot() {
        // create a minimal Pilot for testing — adjust to actual constructor
        return new Pilot(/* ... */);
    }

    private Flight buildFlight(final Pilot pilot, final FlightPlanStatus status) {
        // create a minimal Flight with a FlightPlan in the given status
        // adjust to actual constructor / factory method
        Flight flight = new Flight(/* designator, route, pilot, ... */);
        if (status == FlightPlanStatus.TESTED) {
            flight.flightPlan().markAsTested(); // or however status is set
        }
        return flight;
    }
}
```

---

## 2. AddWeatherDataToFlightController

```java
class AddWeatherDataToFlightControllerTest {

    private AddWeatherDataToFlightController controller;
    private FlightRepository flightRepoMock;
    private WeatherDataRepository weatherDataRepoMock;
    private Flight flightMock;
    private WeatherData weatherData;

    @BeforeEach
    void setUp() {
        flightRepoMock = mock(FlightRepository.class);
        weatherDataRepoMock = mock(WeatherDataRepository.class);
        controller = new AddWeatherDataToFlightController(
            flightRepoMock, weatherDataRepoMock
        );

        weatherData = buildWeatherData();
        flightMock = mock(Flight.class);
    }

    @Test
    void addWeatherData_flightExists_callsDomainMethod() {
        // Arrange
        Long flightId = 1L;
        Long weatherDataId = 10L;
        when(flightRepoMock.ofIdentity(flightId))
            .thenReturn(Optional.of(flightMock));
        when(weatherDataRepoMock.ofIdentity(weatherDataId))
            .thenReturn(Optional.of(weatherData));

        // Act
        controller.addWeatherData(flightId, weatherDataId);

        // Assert — domain method called
        verify(flightMock).addWeatherData(weatherData);
    }

    @Test
    void addWeatherData_flightExists_savesCalled() {
        // Arrange
        Long flightId = 1L;
        Long weatherDataId = 10L;
        when(flightRepoMock.ofIdentity(flightId))
            .thenReturn(Optional.of(flightMock));
        when(weatherDataRepoMock.ofIdentity(weatherDataId))
            .thenReturn(Optional.of(weatherData));

        // Act
        controller.addWeatherData(flightId, weatherDataId);

        // Assert — flight persisted after modification
        verify(flightRepoMock).save(flightMock);
    }

    @Test
    void addWeatherData_flightNotFound_throwsException() {
        // Arrange
        Long unknownId = 99L;
        when(flightRepoMock.ofIdentity(unknownId))
            .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
            controller.addWeatherData(unknownId, 10L)
        );
    }

    @Test
    void addWeatherData_weatherDataNotFound_throwsException() {
        // Arrange
        Long flightId = 1L;
        Long unknownWeatherId = 99L;
        when(flightRepoMock.ofIdentity(flightId))
            .thenReturn(Optional.of(flightMock));
        when(weatherDataRepoMock.ofIdentity(unknownWeatherId))
            .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
            controller.addWeatherData(flightId, unknownWeatherId)
        );
    }

    @Test
    void myFlights_returnsPilotFlights() {
        // Arrange
        List<Flight> expected = List.of(flightMock);
        when(flightRepoMock.findByPilot(any()))
            .thenReturn(expected);

        // Act
        Iterable<Flight> result = controller.myFlights();

        // Assert
        assertEquals(expected, result);
    }

    // --- helper ---

    private WeatherData buildWeatherData() {
        return new WeatherData(
            AreaCode.valueOf("AREA01"),
            new WindCondition(12.0, 270, 38.7, -9.1, 4000),
            18.5, "IPMA",
            LocalDateTime.of(2026, 5, 1, 8, 0)
        );
    }
}
```

---

## 3. Notes for Implementation

The following classes/methods must be created for these tests to compile and pass:

| Class / Method | Note |
|---|---|
| `Flight.addWeatherData(WeatherData)` | Core domain method — must enforce status invariant |
| `Flight.flightPlanStatus()` | Exposes internal FlightPlan status to tests |
| `Flight.weatherData()` | Returns the currently associated WeatherData (nullable) |
| `Flight.markAsTested()` | Sets FlightPlan status to TESTED — needed for setup in tests |
| `AddWeatherDataToFlightController` | Application layer — auth, load, delegate, save |
| `FlightRepository.findByPilot(Pilot)` | Query needed by the controller's `myFlights()` |
