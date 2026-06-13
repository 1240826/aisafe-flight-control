# US075 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
These are TDD tests — `Pilot` constructor/factory and `AddPilotController`
do not exist yet and must be implemented to make these tests pass.

---

## 1. Pilot Domain

```java
class PilotTest {

    private SystemUser systemUser;
    private AirTransportCompany company;
    private List<AircraftModelCode> certifiedModels;

    @BeforeEach
    void setUp() {
        systemUser = buildSystemUser();
        company = buildCompany();
        certifiedModels = List.of(
            AircraftModelCode.valueOf("B737"),
            AircraftModelCode.valueOf("A320")
        );
    }

    // --- construction ---

    @Test
    void pilot_validArguments_isCreatedAsActive() {
        // Arrange — valid user, company and at least one model

        // Act
        Pilot pilot = new Pilot(systemUser, company, certifiedModels);

        // Assert — pilot starts active
        assertTrue(pilot.isActive());
    }

    @Test
    void pilot_validArguments_certifiedModelsAreStored() {
        // Arrange

        // Act
        Pilot pilot = new Pilot(systemUser, company, certifiedModels);

        // Assert
        assertEquals(certifiedModels.size(), pilot.certifiedModels().size());
        assertTrue(pilot.certifiedModels().containsAll(certifiedModels));
    }

    @Test
    void pilot_validArguments_linkedToCorrectCompany() {
        // Arrange

        // Act
        Pilot pilot = new Pilot(systemUser, company, certifiedModels);

        // Assert
        assertEquals(company.iataCode(), pilot.companyId());
    }

    // --- null / empty guards ---

    @Test
    void pilot_nullSystemUser_throwsException() {
        // Arrange — null user

        // Act + Assert
        assertThrows(Exception.class, () ->
            new Pilot(null, company, certifiedModels)
        );
    }

    @Test
    void pilot_nullCompany_throwsException() {
        // Arrange — null company

        // Act + Assert
        assertThrows(Exception.class, () ->
            new Pilot(systemUser, null, certifiedModels)
        );
    }

    @Test
    void pilot_nullCertifiedModels_throwsException() {
        // Arrange — null model list

        // Act + Assert
        assertThrows(Exception.class, () ->
            new Pilot(systemUser, company, null)
        );
    }

    @Test
    void pilot_emptyCertifiedModels_throwsException() {
        // Arrange — empty model list (at least one required)

        // Act + Assert
        assertThrows(Exception.class, () ->
            new Pilot(systemUser, company, List.of())
        );
    }

    // --- deactivate ---

    @Test
    void deactivate_activePilot_becomesInactive() {
        // Arrange
        Pilot pilot = new Pilot(systemUser, company, certifiedModels);
        assertTrue(pilot.isActive());

        // Act
        pilot.deactivate();

        // Assert
        assertFalse(pilot.isActive());
    }

    @Test
    void deactivate_alreadyInactivePilot_throwsException() {
        // Arrange
        Pilot pilot = new Pilot(systemUser, company, certifiedModels);
        pilot.deactivate();
        assertFalse(pilot.isActive());

        // Act + Assert — cannot deactivate twice
        assertThrows(IllegalStateException.class, pilot::deactivate);
    }

    // --- helpers ---

    private SystemUser buildSystemUser() {
        // create a minimal SystemUser for testing — adjust to actual constructor
        return new SystemUser(/* username, password, roles, ... */);
    }

    private AirTransportCompany buildCompany() {
        // create a minimal AirTransportCompany — adjust to actual constructor
        return new AirTransportCompany(/* name, iata, icao, ... */);
    }
}
```

---

## 2. AddPilotController

```java
class AddPilotControllerTest {

    private AddPilotController controller;
    private PilotRepository pilotRepoMock;
    private UserManagementService userSvcMock;
    private AircraftModelRepository modelRepoMock;
    private Pilot pilotMock;
    private SystemUser systemUser;
    private AirTransportCompany company;

    @BeforeEach
    void setUp() {
        pilotRepoMock  = mock(PilotRepository.class);
        userSvcMock    = mock(UserManagementService.class);
        modelRepoMock  = mock(AircraftModelRepository.class);
        controller = new AddPilotController(
            pilotRepoMock, userSvcMock, modelRepoMock
        );

        systemUser = buildSystemUser();
        company    = buildCompany();
        pilotMock  = mock(Pilot.class);
    }

    @Test
    void addPilot_validArguments_savesCalled() {
        // Arrange
        String username = "jpilot@airline.com";
        List<String> modelIds = List.of("B737");
        when(userSvcMock.findUserByUsername(username))
            .thenReturn(Optional.of(systemUser));
        when(pilotRepoMock.findByUserAndCompany(systemUser, company))
            .thenReturn(Optional.empty());
        when(modelRepoMock.findAllById(any()))
            .thenReturn(List.of(buildAircraftModel("B737")));

        // Act
        controller.addPilot(username, modelIds);

        // Assert — pilot persisted
        verify(pilotRepoMock).save(any(Pilot.class));
    }

    @Test
    void addPilot_userNotFound_throwsException() {
        // Arrange
        String unknownUsername = "unknown@airline.com";
        when(userSvcMock.findUserByUsername(unknownUsername))
            .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
            controller.addPilot(unknownUsername, List.of("B737"))
        );
    }

    @Test
    void addPilot_pilotAlreadyActiveInCompany_throwsException() {
        // Arrange
        String username = "jpilot@airline.com";
        when(userSvcMock.findUserByUsername(username))
            .thenReturn(Optional.of(systemUser));
        when(pilotRepoMock.findByUserAndCompany(systemUser, company))
            .thenReturn(Optional.of(pilotMock));
        when(pilotMock.isActive()).thenReturn(true);

        // Act + Assert
        assertThrows(IllegalStateException.class, () ->
            controller.addPilot(username, List.of("B737"))
        );
    }

    @Test
    void addPilot_noCertifiedModels_throwsException() {
        // Arrange
        String username = "jpilot@airline.com";
        when(userSvcMock.findUserByUsername(username))
            .thenReturn(Optional.of(systemUser));
        when(pilotRepoMock.findByUserAndCompany(systemUser, company))
            .thenReturn(Optional.empty());

        // Act + Assert — empty model list must be rejected
        assertThrows(Exception.class, () ->
            controller.addPilot(username, List.of())
        );
    }

    @Test
    void addPilot_unknownModelId_throwsException() {
        // Arrange
        String username = "jpilot@airline.com";
        List<String> modelIds = List.of("UNKNOWN_MODEL");
        when(userSvcMock.findUserByUsername(username))
            .thenReturn(Optional.of(systemUser));
        when(pilotRepoMock.findByUserAndCompany(systemUser, company))
            .thenReturn(Optional.empty());
        when(modelRepoMock.findAllById(any()))
            .thenReturn(List.of());  // nothing resolved

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () ->
            controller.addPilot(username, modelIds)
        );
    }

    @Test
    void activePilotsOfCompany_returnsPilotList() {
        // Arrange
        List<Pilot> expected = List.of(pilotMock);
        when(pilotRepoMock.findActiveByCompany(any()))
            .thenReturn(expected);

        // Act
        Iterable<Pilot> result = controller.activePilotsOfCompany();

        // Assert
        assertEquals(expected, result);
    }

    // --- helpers ---

    private SystemUser buildSystemUser() {
        return new SystemUser(/* username, password, roles, ... */);
    }

    private AirTransportCompany buildCompany() {
        return new AirTransportCompany(/* name, iata, icao, ... */);
    }

    private AircraftModel buildAircraftModel(String code) {
        return new AircraftModel(/* AircraftModelCode.valueOf(code), ... */);
    }
}
```

---

## 3. Notes for Implementation

The following classes/methods must be created for these tests to compile and pass:

| Class / Method | Note |
|---|---|
| `Pilot(SystemUser, AirTransportCompany, List<AircraftModelCode>)` | Core domain constructor — must enforce invariants (non-null, non-empty models, starts active) |
| `Pilot.isActive()` | Returns whether the pilot is currently active |
| `Pilot.companyId()` | Returns the `CompanyIATA` the pilot belongs to |
| `Pilot.certifiedModels()` | Returns the list of certified `AircraftModelCode` values |
| `Pilot.deactivate()` | Sets pilot to inactive — must reject if already inactive |
| `AddPilotController` | Application layer — auth, lookup user, check duplicate, validate models, save |
| `PilotRepository.findByUserAndCompany(SystemUser, AirTransportCompany)` | Query to detect duplicate active pilot in the same company |
| `PilotRepository.findActiveByCompany(CompanyIATA)` | Query used by the controller's `activePilotsOfCompany()` |
| `AircraftModelRepository.findAllById(Collection<String>)` | Query to resolve and validate the provided model IDs |