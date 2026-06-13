# US077 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
These are TDD tests — `RemovePilotController`
does not exist yet and must be implemented to make these tests pass.
`Pilot.deactivate()` is already defined in US075 and is reused here.

---

## 1. RemovePilotController

```java
class RemovePilotControllerTest {

    private RemovePilotController controller;
    private PilotRepository pilotRepoMock;
    private FlightPlanRepository flightPlanRepoMock;
    private Pilot activePilotMock;
    private AirTransportCompany company;

    @BeforeEach
    void setUp() {
        pilotRepoMock      = mock(PilotRepository.class);
        flightPlanRepoMock = mock(FlightPlanRepository.class);
        controller = new RemovePilotController(
            pilotRepoMock, flightPlanRepoMock
        );

        company          = buildCompany();
        activePilotMock  = mock(Pilot.class);

        when(activePilotMock.isActive()).thenReturn(true);
    }

    // --- happy path ---

    @Test
    void deactivatePilot_noActiveFlightPlans_deactivatesCalled() {
        // Arrange
        Long pilotId = 1L;
        when(pilotRepoMock.findActiveByIdAndCompany(pilotId, any()))
            .thenReturn(Optional.of(activePilotMock));
        when(flightPlanRepoMock.existsActiveByPilot(activePilotMock))
            .thenReturn(false);

        // Act
        controller.deactivatePilot(pilotId);

        // Assert — domain method called
        verify(activePilotMock).deactivate();
    }

    @Test
    void deactivatePilot_noActiveFlightPlans_savesCalled() {
        // Arrange
        Long pilotId = 1L;
        when(pilotRepoMock.findActiveByIdAndCompany(pilotId, any()))
            .thenReturn(Optional.of(activePilotMock));
        when(flightPlanRepoMock.existsActiveByPilot(activePilotMock))
            .thenReturn(false);

        // Act
        controller.deactivatePilot(pilotId);

        // Assert — pilot persisted after deactivation
        verify(pilotRepoMock).save(activePilotMock);
    }

    // --- pilot not found ---

    @Test
    void deactivatePilot_pilotNotFound_throwsException() {
        // Arrange
        Long unknownId = 99L;
        when(pilotRepoMock.findActiveByIdAndCompany(unknownId, any()))
            .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
            controller.deactivatePilot(unknownId)
        );
    }

    @Test
    void deactivatePilot_pilotNotFound_saveNeverCalled() {
        // Arrange
        Long unknownId = 99L;
        when(pilotRepoMock.findActiveByIdAndCompany(unknownId, any()))
            .thenReturn(Optional.empty());

        // Act
        assertThrows(IllegalArgumentException.class, () ->
            controller.deactivatePilot(unknownId)
        );

        // Assert — nothing persisted
        verify(pilotRepoMock, never()).save(any());
    }

    // --- active flight plans block deactivation ---

    @Test
    void deactivatePilot_hasActiveFlightPlans_throwsException() {
        // Arrange
        Long pilotId = 1L;
        when(pilotRepoMock.findActiveByIdAndCompany(pilotId, any()))
            .thenReturn(Optional.of(activePilotMock));
        when(flightPlanRepoMock.existsActiveByPilot(activePilotMock))
            .thenReturn(true);

        // Act + Assert
        assertThrows(IllegalStateException.class, () ->
            controller.deactivatePilot(pilotId)
        );
    }

    @Test
    void deactivatePilot_hasActiveFlightPlans_deactivateNeverCalled() {
        // Arrange
        Long pilotId = 1L;
        when(pilotRepoMock.findActiveByIdAndCompany(pilotId, any()))
            .thenReturn(Optional.of(activePilotMock));
        when(flightPlanRepoMock.existsActiveByPilot(activePilotMock))
            .thenReturn(true);

        // Act
        assertThrows(IllegalStateException.class, () ->
            controller.deactivatePilot(pilotId)
        );

        // Assert — domain method never reached
        verify(activePilotMock, never()).deactivate();
    }

    @Test
    void deactivatePilot_hasActiveFlightPlans_saveNeverCalled() {
        // Arrange
        Long pilotId = 1L;
        when(pilotRepoMock.findActiveByIdAndCompany(pilotId, any()))
            .thenReturn(Optional.of(activePilotMock));
        when(flightPlanRepoMock.existsActiveByPilot(activePilotMock))
            .thenReturn(true);

        // Act
        assertThrows(IllegalStateException.class, () ->
            controller.deactivatePilot(pilotId)
        );

        // Assert — nothing persisted
        verify(pilotRepoMock, never()).save(any());
    }

    // --- active roster listing (reused from US076) ---

    @Test
    void activePilotsOfCompany_returnsPilotList() {
        // Arrange
        List<Pilot> expected = List.of(activePilotMock);
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(expected);

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert
        assertEquals(expected, result);
    }

    // --- helper ---

    private AirTransportCompany buildCompany() {
        // create a minimal AirTransportCompany — adjust to actual constructor
        return new AirTransportCompany(/* name, iata, icao, ... */);
    }
}
```

---

## 2. Notes for Implementation

The following classes/methods must be created for these tests to compile and pass:

| Class / Method | Note |
|---|---|
| `RemovePilotController` | Application layer — auth, resolve ATCC's company from session, check flight plans, delegate deactivation, save |
| `RemovePilotController.deactivatePilot(Long)` | Loads pilot by ID and company, checks for active flight plans, calls `pilot.deactivate()`, saves |
| `RemovePilotController.activePilotsOfCompany()` | Reuses `PilotRepository.findActiveByCompany()` to populate the selection list in the UI |
| `PilotRepository.findActiveByIdAndCompany(Long, CompanyIATA)` | Scoped lookup — returns empty if the pilot does not belong to the ATCC's company |
| `FlightPlanRepository.existsActiveByPilot(Pilot)` | Returns true if the pilot has at least one flight plan in a non-terminal state |
| `Pilot.deactivate()` | Already defined in US075 — reused here without modification |
| `PilotRepository.findActiveByCompany(CompanyIATA)` | Already defined in US075/US076 — reused here |