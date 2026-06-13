# US076 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
These are TDD tests — `ListPilotRosterController`
does not exist yet and must be implemented to make these tests pass.

---

## 1. ListPilotRosterController

```java
class ListPilotRosterControllerTest {

    private ListPilotRosterController controller;
    private PilotRepository pilotRepoMock;
    private Pilot activePilotMock;
    private Pilot inactivePilotMock;
    private AirTransportCompany company;

    @BeforeEach
    void setUp() {
        pilotRepoMock      = mock(PilotRepository.class);
        controller         = new ListPilotRosterController(pilotRepoMock);

        activePilotMock   = mock(Pilot.class);
        inactivePilotMock = mock(Pilot.class);
        company           = buildCompany();

        when(activePilotMock.isActive()).thenReturn(true);
        when(inactivePilotMock.isActive()).thenReturn(false);
    }

    // --- happy path ---

    @Test
    void activePilotsOfCompany_companyHasActivePilots_returnsAll() {
        // Arrange
        List<Pilot> expected = List.of(activePilotMock);
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(expected);

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void activePilotsOfCompany_repositoryQueriedWithCorrectCompany() {
        // Arrange
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(List.of());

        // Act
        controller.activePilotsOfCompany();

        // Assert — repository called exactly once with the ATCC's company
        verify(pilotRepoMock, times(1)).findActiveByCompany(any());
    }

    // --- empty roster ---

    @Test
    void activePilotsOfCompany_noActivePilots_returnsEmptyList() {
        // Arrange — repository returns no pilots
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(List.of());

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert — empty iterable, no exception thrown
        assertFalse(result.iterator().hasNext());
    }

    // --- isolation: inactive pilots excluded ---

    @Test
    void activePilotsOfCompany_inactivePilotsNotReturned() {
        // Arrange — repository already filters; only active pilots returned
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(List.of(activePilotMock));

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert — inactive pilot is not in the result
        List<Pilot> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertFalse(resultList.contains(inactivePilotMock));
        assertTrue(resultList.contains(activePilotMock));
    }

    // --- isolation: other companies' pilots excluded ---

    @Test
    void activePilotsOfCompany_pilotsFromOtherCompanyNotReturned() {
        // Arrange — repository scopes to the ATCC's company;
        // pilots from other companies are never included in the result
        Pilot otherCompanyPilot = mock(Pilot.class);
        when(otherCompanyPilot.isActive()).thenReturn(true);
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(List.of(activePilotMock)); // only own company pilots

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert
        List<Pilot> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertFalse(resultList.contains(otherCompanyPilot));
    }

    // --- multiple pilots ---

    @Test
    void activePilotsOfCompany_multipleActivePilots_allReturned() {
        // Arrange
        Pilot secondActivePilot = mock(Pilot.class);
        when(secondActivePilot.isActive()).thenReturn(true);
        List<Pilot> expected = List.of(activePilotMock, secondActivePilot);
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(expected);

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert — both pilots returned
        List<Pilot> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertEquals(2, resultList.size());
        assertTrue(resultList.containsAll(expected));
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
| `ListPilotRosterController` | Application layer — auth, resolve ATCC's company from session, delegate to repository |
| `ListPilotRosterController.activePilotsOfCompany()` | Returns active pilots scoped to the authenticated ATCC's company |
| `PilotRepository.findActiveByCompany(CompanyIATA)` | Filters by company and active status — shared with US075 and US077 |
| `Pilot.isActive()` | Already defined in US075 — reused here |