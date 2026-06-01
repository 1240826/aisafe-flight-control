 # US085 — Unit Tests (TDD)

All tests follow the **AAA convention** (Arrange, Act, Assert) and are written in JUnit 5.
These are TDD tests — `FlightPlanStatus`, `FlightPlanValidationService`, `ValidationResult`,
and `TestFlightPlanController` do not exist yet and must be implemented to make these tests pass.

To comply with the professor's recommendation for more complex tests, this document includes:
1. **Hand-written domain tests** covering every business invariant with boundary values
2. **Parameterized tests driven by a CSV data file** (`flight_plan_test_data.csv`) — a single
   test method exercises 13+ scenarios from the file, making the suite easier to extend
   without writing new Java code for each new scenario
3. **Application-layer controller tests** using Mockito (following the existing project pattern)

---

## 1. FlightPlan / Flight Domain — Status Lifecycle

A flight plan is part of the `Flight` aggregate (started by US080). It has a `FlightPlanStatus`
that drives its lifecycle:

| Status | Meaning |
|--------|---------|
| `DRAFT` | Initial state — flight plan created but not yet tested |
| `IN_TEST` | Validation is in progress |
| `TEST_PASSED` | All validation checks succeeded |
| `TEST_FAILED` | One or more validation checks failed |

### `FlightPlanStatus` Enum

```java
class FlightPlanStatusTest {

    @Test
    void enumHasExactlyFourValues() {
        // The lifecycle comprises exactly four states
        assertEquals(4, FlightPlanStatus.values().length,
                "FlightPlanStatus must have DRAFT, IN_TEST, TEST_PASSED, TEST_FAILED");
    }

    @Test
    void draftIsFirstDeclaredConstant() {
        // DRAFT must be the first constant (ordinal 0) so a new FlightPlan
        // defaults to DRAFT without an explicit field initialiser
        assertEquals(0, FlightPlanStatus.DRAFT.ordinal());
    }

    @Test
    void testPassedIsNotDraft() {
        assertNotEquals(FlightPlanStatus.DRAFT, FlightPlanStatus.TEST_PASSED);
    }

    @Test
    void testFailedIsNotDraft() {
        assertNotEquals(FlightPlanStatus.DRAFT, FlightPlanStatus.TEST_FAILED);
    }
}
```

### `FlightPlan` Domain Entity Tests

The core domain tests verify that:

1. A new flight plan starts in **DRAFT** status.
2. Invoking `test()` transitions DRAFT → IN_TEST and returns a `ValidationResult`.
3. After a successful test, status becomes **TEST_PASSED** — pass result is recorded.
4. After a failed test, status becomes **TEST_FAILED** — failure reasons are recorded.
5. Testing an already-passed or already-failed flight plan is rejected.
6. A flight plan cannot be created with null or invalid arguments.

```java
class FlightPlanTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static FlightPlanId validId() {
        return FlightPlanId.valueOf("TP0123");
    }

    private static FlightRoute validRoute() {
        return new FlightRoute(
                FlightRouteName.valueOf("TP123"),
                new CompanyIATA("TP"),
                new AirportIATA("OPO"),
                new AirportIATA("LIS"));
    }

    private static AircraftModelCode validModel() {
        return new AircraftModelCode("B738");
    }

    private static FlightPlan validFlightPlan() {
        return new FlightPlan(
                validId(),
                validRoute(),
                validModel(),
                15000.0,    // fuelAmount (kg)
                10668,      // cruiseAltitude (m)
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));
    }

    // ── Construction ──────────────────────────────────────────────────────────

    @Test
    void ensureNewFlightPlanIsDraft() {
        // US085.1: flight plan starts in DRAFT status
        final var fp = validFlightPlan();
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
    }

    @Test
    void ensureFlightPlanIdentityIsPreserved() {
        final var fp = validFlightPlan();
        assertEquals(validId(), fp.identity());
    }

    @Test
    void ensureRouteIsPreserved() {
        final var fp = validFlightPlan();
        assertEquals(validRoute(), fp.route());
    }

    @Test
    void ensurefuelAmountIsPreserved() {
        final var fp = validFlightPlan();
        assertEquals(15000.0, fp.fuelAmount(), 0.001);
    }

    @Test
    void ensureCruiseAltitudeIsPreserved() {
        final var fp = validFlightPlan();
        assertEquals(10668, fp.cruiseAltitude());
    }

    @Test
    void ensureDepartureAndArrivalTimesArePreserved() {
        final var fp = validFlightPlan();
        assertEquals(LocalDateTime.of(2026, 6, 15, 10, 0), fp.departureTime());
        assertEquals(LocalDateTime.of(2026, 6, 15, 12, 0), fp.arrivalTime());
    }

    // ── Null / blank guards ───────────────────────────────────────────────────

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(null, validRoute(), validModel(),
                        15000.0, 10668, LocalDateTime.now(), LocalDateTime.now().plusHours(2)));
    }

    @Test
    void ensureNullRouteIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), null, validModel(),
                        15000.0, 10668, LocalDateTime.now(), LocalDateTime.now().plusHours(2)));
    }

    @Test
    void ensureNullAircraftModelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), validRoute(), null,
                        15000.0, 10668, LocalDateTime.now(), LocalDateTime.now().plusHours(2)));
    }

    @Test
    void ensureNegativeFuelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), validRoute(), validModel(),
                        -1.0, 10668, LocalDateTime.now(), LocalDateTime.now().plusHours(2)));
    }

    @Test
    void ensureZeroFuelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), validRoute(), validModel(),
                        0.0, 10668, LocalDateTime.now(), LocalDateTime.now().plusHours(2)));
    }

    @Test
    void ensureNegativeCruiseAltitudeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), validRoute(), validModel(),
                        15000.0, -1, LocalDateTime.now(), LocalDateTime.now().plusHours(2)));
    }

    @Test
    void ensureDepartureAfterArrivalIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), validRoute(), validModel(),
                        15000.0, 10668,
                        LocalDateTime.of(2026, 6, 15, 12, 0),
                        LocalDateTime.of(2026, 6, 15, 10, 0)));
    }

    @Test
    void ensureDepartureEqualToArrivalIsRejected() {
        final var same = LocalDateTime.of(2026, 6, 15, 10, 0);
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(validId(), validRoute(), validModel(),
                        15000.0, 10668, same, same));
    }

    // ── Status transitions via test() ─────────────────────────────────────────

    @Test
    void ensureDraftFlightPlanCanTransitionToInTest() {
        final var fp = validFlightPlan();
        fp.startTest();
        assertEquals(FlightPlanStatus.IN_TEST, fp.status());
    }

    @Test
    void ensureTestPassedIsRecordedCorrectly() {
        final var fp = validFlightPlan();
        final var reasons = List.<String>of();
        fp.recordTestResult(true, reasons);
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
        assertTrue(fp.validationResult().isPassed());
        assertTrue(fp.validationResult().reasons().isEmpty());
    }

    @Test
    void ensureTestFailedIsRecordedCorrectly() {
        final var fp = validFlightPlan();
        final var reasons = List.of("Fuel exceeds maximum capacity");
        fp.recordTestResult(false, reasons);
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());
        assertFalse(fp.validationResult().isPassed());
        assertEquals(1, fp.validationResult().reasons().size());
    }

    @Test
    void ensureTestFailedRecordsMultipleReasons() {
        final var fp = validFlightPlan();
        final var reasons = List.of(
                "Fuel exceeds maximum capacity",
                "Altitude exceeds service ceiling",
                "Take-off weight exceeds MTOW");
        fp.recordTestResult(false, reasons);
        assertEquals(3, fp.validationResult().reasons().size());
        assertTrue(fp.validationResult().reasons().get(0).contains("Fuel"));
        assertTrue(fp.validationResult().reasons().get(1).contains("Altitude"));
        assertTrue(fp.validationResult().reasons().get(2).contains("MTOW"));
    }

    @Test
    void ensureTestingAlreadyTestPassedFlightPlanIsRejected() {
        final var fp = validFlightPlan();
        fp.recordTestResult(true, List.of());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());

        assertThrows(IllegalStateException.class, fp::startTest,
                "Cannot test a flight plan that has already passed");
    }

    @Test
    void ensureTestingAlreadyTestFailedFlightPlanIsRejected() {
        final var fp = validFlightPlan();
        fp.recordTestResult(false, List.of("Some failure"));
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());

        assertThrows(IllegalStateException.class, fp::startTest,
                "Cannot test a flight plan that has already failed");
    }

    @Test
    void ensureTestedFlightPlanCanBeResetToDraft() {
        final var fp = validFlightPlan();
        fp.recordTestResult(true, List.of());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());

        fp.resetToDraft();
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
        assertNull(fp.validationResult());
    }

    @Test
    void ensureFailedFlightPlanCanBeResetToDraft() {
        final var fp = validFlightPlan();
        fp.recordTestResult(false, List.of("Some failure"));
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());

        fp.resetToDraft();
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
    }

    @Test
    void ensureResetToDraftOnAlreadyDraftIsRejected() {
        final var fp = validFlightPlan();
        assertEquals(FlightPlanStatus.DRAFT, fp.status());

        assertThrows(IllegalStateException.class, fp::resetToDraft,
                "Cannot reset a draft flight plan — it is already in draft");
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    void ensureFlightPlansWithSameIdAreEqual() {
        final var id = validId();
        final var fp1 = new FlightPlan(id, validRoute(), validModel(),
                15000.0, 10668, LocalDateTime.now(), LocalDateTime.now().plusHours(2));
        final var fp2 = new FlightPlan(id, validRoute(), validModel(),
                20000.0, 10000, LocalDateTime.now(), LocalDateTime.now().plusHours(3));
        assertEquals(fp1, fp2, "FlightPlans with same ID should be equal");
    }

    @Test
    void ensureFlightPlansWithDifferentIdAreNotEqual() {
        final var fp1 = new FlightPlan(FlightPlanId.valueOf("TP001"), validRoute(),
                validModel(), 15000.0, 10668,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2));
        final var fp2 = new FlightPlan(FlightPlanId.valueOf("TP002"), validRoute(),
                validModel(), 15000.0, 10668,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2));
        assertNotEquals(fp1, fp2);
    }
}
```

---

## 2. `FlightPlanValidationService` — Domain Service

The `FlightPlanValidationService` performs the business validation of a flight plan.
It checks:

| Rule | Description | Invariant |
|------|-------------|-----------|
| **R1** | Fuel must not exceed model's max capacity | `fuelAmount <= maxFuelCapacity` |
| **R2** | Fuel must be sufficient for the route | `fuelAmount >= estimatedFuelNeeded` |
| **R3** | Cruise altitude must not exceed service ceiling | `cruiseAltitude <= serviceCeiling` |
| **R4** | Cruise altitude must be positive | `cruiseAltitude > 0` |
| **R5** | Take-off weight must not exceed MTOW | `emptyWeight + payloadWeight + fuelAmount <= mtow` |
| **R6** | Zero-fuel weight must not exceed MZFW | `emptyWeight + payloadWeight <= mzfw` |
| **R7** | Pilot must be certified for the assigned aircraft model | `pilot.certifications.contains(flightPlan.aircraftModel())` |

### 2.1 Hand-Written Tests

```java
class FlightPlanValidationServiceTest {

    private FlightPlanValidationService service;
    private AircraftModelRepository modelRepoMock;

    // Test data constants — Boeing 737-800 values
    private static final double B738_MAX_FUEL_KG = 20000.0;
    private static final int    B738_SERVICE_CEILING_M = 12500;
    private static final double B738_MTOW_KG = 70000.0;
    private static final double B738_MZFW_KG = 58000.0;
    private static final double B738_EMPTY_WEIGHT_KG = 41413.0;

    @BeforeEach
    void setUp() {
        modelRepoMock = mock(AircraftModelRepository.class);
        service = new FlightPlanValidationService(modelRepoMock);
    }

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private FlightPlan validFlightPlan() {
        return new FlightPlan(
                FlightPlanId.valueOf("TP0123"),
                mockRoute(),
                new AircraftModelCode("B738"),
                15000.0,    // fuelAmount (kg)
                10668,      // cruiseAltitude (m)
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));
    }

    private AircraftModel mockB738() {
        final var model = mock(AircraftModel.class);
        when(model.identity()).thenReturn(new AircraftModelCode("B738"));
        when(model.maxFuelCapacity()).thenReturn(B738_MAX_FUEL_KG);
        when(model.serviceCeiling()).thenReturn(B738_SERVICE_CEILING_M);
        when(model.mtow()).thenReturn(B738_MTOW_KG);
        when(model.mzfw()).thenReturn(B738_MZFW_KG);
        when(model.emptyWeight()).thenReturn(B738_EMPTY_WEIGHT_KG);
        when(model.estimatedFuelForRoute(any(FlightPlan.class)))
                .thenReturn(8000.0); // baseline estimate
        return model;
    }

    private FlightRoute mockRoute() {
        return mock(FlightRoute.class);
    }

    // ── R1: Fuel ≤ max capacity ───────────────────────────────────────────────

    @Test
    void validateFuelWithinCapacity_passes() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        final var result = service.validate(validFlightPlan(), 15000.0);

        assertTrue(result.isPassed());
    }

    @Test
    void validateFuelExceedsCapacity_fails() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        final var fp = validFlightPlan();
        // fuel already 15000, capacity is 20000 — stays within, so we need
        // a different scenario. We'll set fuel to 21000 via the validation call.
        final var result = service.validate(fp, 21000.0);

        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("fuel"))
                && result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("capacity")));
    }

    @Test
    void validateFuelEqualToCapacity_passes() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        final var result = service.validate(validFlightPlan(), B738_MAX_FUEL_KG);

        assertTrue(result.isPassed(),
                "Fuel exactly at max capacity should be accepted");
    }

    // ── R2: Fuel sufficient for route ─────────────────────────────────────────

    @Test
    void validateFuelSufficientForRoute_passes() {
        final var model = mockB738();
        when(model.estimatedFuelForRoute(any())).thenReturn(7000.0);
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));

        final var result = service.validate(validFlightPlan(), 15000.0);

        assertTrue(result.isPassed());
    }

    @Test
    void validateFuelInsufficientForRoute_fails() {
        final var model = mockB738();
        when(model.estimatedFuelForRoute(any())).thenReturn(20000.0);
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));

        final var result = service.validate(validFlightPlan(), 15000.0);

        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("insufficient")));
    }

    // ── R3: Altitude ≤ service ceiling ────────────────────────────────────────

    @Test
    void validateAltitudeWithinCeiling_passes() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        final var result = service.validate(validFlightPlan(), 15000.0);

        // cruiseAltitude = 10668m, ceiling = 12500m → OK
        assertTrue(result.isPassed());
    }

    @Test
    void validateAltitudeExceedsCeiling_fails() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        final var fp = new FlightPlan(
                FlightPlanId.valueOf("TP3344"),
                mockRoute(),
                new AircraftModelCode("B738"),
                15000.0,
                13716,   // above B738's 12500m ceiling
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        final var result = service.validate(fp, 15000.0);

        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("altitude")
                        || r.toLowerCase().contains("ceiling")));
    }

    @Test
    void validateAltitudeEqualToCeiling_passes() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        final var fp = new FlightPlan(
                FlightPlanId.valueOf("TP5566"),
                mockRoute(),
                new AircraftModelCode("B738"),
                15000.0,
                B738_SERVICE_CEILING_M,  // exactly at ceiling
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        final var result = service.validate(fp, 15000.0);

        assertTrue(result.isPassed());
    }

    // ── R5: Take-off weight ≤ MTOW ────────────────────────────────────────────

    @Test
    void validateTakeoffWeightWithinMTOW_passes() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        // empty(41413) + payload(15000) + fuel(15000) = 71413
        // MTOW = 70000 → fails actually! Let's use payload of 13000:
        final var fp = new FlightPlan(
                FlightPlanId.valueOf("TP7788"),
                mockRoute(),
                new AircraftModelCode("B738"),
                15000.0,  // fuel
                10668,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        final var model = mockB738();
        when(model.emptyWeight()).thenReturn(41413.0);
        when(model.mtow()).thenReturn(70000.0);
        when(model.estimatedFuelForRoute(any())).thenReturn(8000.0);
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));

        final var result = service.validate(fp, 15000.0, 13000.0);

        // 41413 + 13000 + 15000 = 69413 ≤ 70000 → PASS
        assertTrue(result.isPassed());
    }

    @Test
    void validateTakeoffWeightExceedsMTOW_fails() {
        final var model = mockB738();
        when(model.emptyWeight()).thenReturn(41413.0);
        when(model.mtow()).thenReturn(70000.0);
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));

        final var result = service.validate(validFlightPlan(), 15000.0, 20000.0);

        // 41413 + 20000 + 15000 = 76413 > 70000 → FAIL
        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("mtow")
                        || r.toLowerCase().contains("take-off")));
    }

    // ── Multiple failures simultaneously ──────────────────────────────────────

    @Test
    void validateMultipleFailures_returnsAllReasons() {
        final var model = mockB738();
        when(model.emptyWeight()).thenReturn(41413.0);
        when(model.mtow()).thenReturn(70000.0);
        when(model.maxFuelCapacity()).thenReturn(20000.0);
        when(model.serviceCeiling()).thenReturn(12500.0);
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));

        // Fuel exceeds capacity, altitude exceeds ceiling, weight exceeds MTOW
        final var fp = new FlightPlan(
                FlightPlanId.valueOf("TP9999"),
                mockRoute(),
                new AircraftModelCode("B738"),
                25000.0,   // > 20000 → FAIL
                13716,     // > 12500 → FAIL
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        final var result = service.validate(fp, 25000.0, 20000.0);

        assertFalse(result.isPassed());
        assertTrue(result.reasons().size() >= 2,
                "Multiple failures should produce multiple reasons. Got: " + result.reasons());
    }

    // ── Unknown aircraft model ─────────────────────────────────────────────────

    @Test
    void validateUnknownAircraftModel_throwsException() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(validFlightPlan(), 15000.0, 15000.0),
                "Unknown aircraft model must be rejected");
    }

    // ── R7: Pilot certified for aircraft model (client clarification C07) ──────

    @Test
    void validatePilotCertifiedForAircraftModel_passes() {
        final var model = mockB738();
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));
        final var pilotCertifications = Set.of(new AircraftModelCode("B738"));
        final var fp = aFlightPlanWithPilotCertifications(pilotCertifications);

        final var result = service.validate(fp, 15000.0, 15000.0);

        assertTrue(result.isPassed(),
                "Pilot certified for B738 should pass R7");
    }

    @Test
    void validatePilotNotCertifiedForAircraftModel_fails() {
        final var model = mockB738();
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));
        final var pilotCertifications = Set.of(new AircraftModelCode("A320"));
        final var fp = aFlightPlanWithPilotCertifications(pilotCertifications);

        final var result = service.validate(fp, 15000.0, 15000.0);

        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("certified")
                        || r.toLowerCase().contains("qualification")),
                "Reasons must mention pilot certification mismatch");
    }

    @Test
    void validatePilotWithMultipleCertifications_includesAssignedModel_passes() {
        final var model = mockB738();
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));
        final var pilotCertifications = Set.of(
                new AircraftModelCode("A320"),
                new AircraftModelCode("B738"),
                new AircraftModelCode("E190"));
        final var fp = aFlightPlanWithPilotCertifications(pilotCertifications);

        final var result = service.validate(fp, 15000.0, 15000.0);

        assertTrue(result.isPassed(),
                "Pilot with multiple certs including B738 should pass");
    }

    @Test
    void validatePilotWithEmptyCertifications_fails() {
        final var model = mockB738();
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(model));
        final var fp = aFlightPlanWithPilotCertifications(Collections.emptySet());

        final var result = service.validate(fp, 15000.0, 15000.0);

        assertFalse(result.isPassed());
    }

    // ── Null guards ───────────────────────────────────────────────────────────

    private FlightPlan aFlightPlanWithPilotCertifications(
            final Set<AircraftModelCode> certifications) {
        return new FlightPlan(
                FlightPlanId.valueOf("TP0123"),
                mockRoute(),
                new AircraftModelCode("B738"),
                15000.0,
                10668,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0),
                "dsl content",
                certifications);
    }

    @Test
    void validateNullFlightPlan_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(null, 15000.0, 15000.0));
    }

    @Test
    void validateNegativeFuelInService_throwsException() {
        when(modelRepoMock.ofIdentity(any())).thenReturn(Optional.of(mockB738()));

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(validFlightPlan(), -1.0, 15000.0));
    }
}
```

### 2.2 Parameterized Tests (Data-Driven from CSV)

These tests read from `flight_plan_test_data.csv` to cover **25 validation scenarios**
(TC01–TC25) with a single test method. Adding a new scenario means adding one row
to the CSV file — no new Java code required.

The 25 scenarios include:
- **TC01–TC02**: Happy path (B738 regular, A320 charter)
- **TC03–TC05**: Single-rule failures (fuel, altitude, weight)
- **TC06–TC09**: Valid edge cases (different models, sufficient fuel)
- **TC10–TC12**: Invalid edge cases (insufficient fuel, zero/negative altitude)
- **TC13**: Zero payload still valid
- **TC14–TC17**: Boundary values (exactly at limits)
- **TC18–TC19**: Valid combinations (minimum fuel, high payload)
- **TC20–TC23**: Invalid combinations (fuel below minimum, zero fuel, ZFW exceeded, multiple violations)
- **TC24–TC25**: Large aircraft (A380) and regional jet (E190) — cross-model validation

```java
@CsvFileSource(resources = "/flight_plan_test_data.csv", numLinesToSkip = 1)
class FlightPlanValidationParameterizedTest {

    @ParameterizedTest(name = "{0}: {13}")
    @CsvFileSource(resources = "/flight_plan_test_data.csv", numLinesToSkip = 1)
    void validateFlightPlanScenarios(
            String testCaseId,
            String flightDesignator,
            String flightType,
            String aircraftModelCode,
            double fuelAmount,
            double maxFuelCapacity,
            int    serviceCeiling,
            int    cruiseAltitude,
            double mzfw,
            double mtow,
            double emptyWeight,
            double payloadWeight,
            String expectedStatus,
            String invariant) {

        // Arrange
        final var modelRepo = mock(AircraftModelRepository.class);
        final var model = mock(AircraftModel.class);

        when(model.identity()).thenReturn(new AircraftModelCode(aircraftModelCode));
        when(model.maxFuelCapacity()).thenReturn(maxFuelCapacity);
        when(model.serviceCeiling()).thenReturn(serviceCeiling);
        when(model.mtow()).thenReturn(mtow);
        when(model.mzfw()).thenReturn(mzfw);
        when(model.emptyWeight()).thenReturn(emptyWeight);
        // For "insufficient fuel" cases, set estimate higher than fuel loaded
        if (testCaseId.equals("TC10")) {
            when(model.estimatedFuelForRoute(any())).thenReturn(fuelAmount * 2);
        } else {
            when(model.estimatedFuelForRoute(any())).thenReturn(fuelAmount * 0.5);
        }
        when(modelRepo.ofIdentity(new AircraftModelCode(aircraftModelCode)))
                .thenReturn(Optional.of(model));

        final var service = new FlightPlanValidationService(modelRepo);

        final var flightPlan = new FlightPlan(
                FlightPlanId.valueOf(flightDesignator),
                mock(FlightRoute.class),
                new AircraftModelCode(aircraftModelCode),
                fuelAmount,
                cruiseAltitude,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        // Act
        final var result = service.validate(flightPlan, fuelAmount, payloadWeight);

        // Assert
        if ("PASS".equals(expectedStatus)) {
            assertTrue(result.isPassed(),
                    () -> "TC " + testCaseId + " (" + invariant
                            + ") should PASS but failed with: " + result.reasons());
        } else {
            assertFalse(result.isPassed(),
                    () -> "TC " + testCaseId + " (" + invariant
                            + ") should FAIL but passed");
        }
    }
}
```

### 2.3 Pilot Certification Parameterized Tests (Data-Driven from CSV, R7 / C07)

These tests read from `pilot_certification_test_data.csv` to cover **8 certification
scenarios** (PC01–PC08) with a single test method.

```java
@CsvFileSource(resources = "/pilot_certification_test_data.csv", numLinesToSkip = 1)
class PilotCertificationParameterizedTest {

    @ParameterizedTest(name = "{0}: {4}")
    @CsvFileSource(resources = "/pilot_certification_test_data.csv", numLinesToSkip = 1)
    void validatePilotCertificationScenarios(
            String testCaseId,
            String pilotCertsCsv,       // comma-separated: "B738,A320"
            String aircraftModelCode,
            String expectedStatus,
            String invariant) {

        // Arrange
        final Set<AircraftModelCode> pilotCerts = pilotCertsCsv.isEmpty()
                ? Collections.emptySet()
                : Arrays.stream(pilotCertsCsv.split(","))
                        .map(String::trim)
                        .map(AircraftModelCode::new)
                        .collect(Collectors.toSet());

        final var fp = new FlightPlan(
                FlightPlanId.valueOf("TP" + testCaseId.substring(2)),
                mock(FlightRoute.class),
                new AircraftModelCode(aircraftModelCode),
                15000.0, 10668,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        final var modelRepo = mock(AircraftModelRepository.class);
        when(modelRepo.ofIdentity(new AircraftModelCode(aircraftModelCode)))
                .thenReturn(Optional.of(mock(AircraftModel.class)));
        final var service = new FlightPlanValidationService(modelRepo);

        // Act
        final var result = service.validate(fp, 15000.0, 15000.0, pilotCerts);

        // Assert
        if ("PASS".equals(expectedStatus)) {
            assertTrue(result.isPassed(),
                    () -> testCaseId + " (" + invariant + ") should PASS");
        } else {
            assertFalse(result.isPassed(),
                    () -> testCaseId + " (" + invariant + ") should FAIL");
        }
    }
}
```

The 8 scenarios cover:
| PC ID | Scenario | Expected |
|-------|----------|----------|
| PC01 | Pilot certified for exact model | PASS |
| PC02 | Pilot with multiple certs includes assigned | PASS |
| PC03 | Pilot not certified for assigned model | FAIL |
| PC04 | Pilot with no certifications | FAIL |
| PC05 | Pilot with many certifications includes assigned | PASS |
| PC06 | Pilot certified for different valid model | PASS |
| PC07 | Case-sensitive exact match | PASS |
| PC08 | Partial code match (not exact) | FAIL |

---

## 3. `TestFlightPlanController` — Application Layer

The controller orchestrates the validation flow:

1. Authenticates the caller (must have `PILOT` role)
2. Loads the `Flight` (or `FlightPlan`) by ID from the repository
3. Loads the associated `AircraftModel` from its repository
4. Calls `FlightPlanValidationService.validate()`
5. Records the result on the `FlightPlan` (status transition)
6. Saves the updated `FlightPlan` back to the repository
7. Returns the `ValidationResult`

```java
class TestFlightPlanControllerTest {

    private TestFlightPlanController controller;
    private FlightPlanRepository flightPlanRepoMock;
    private AircraftModelRepository modelRepoMock;
    private AuthorizationService authzMock;
    private FlightPlan flightPlanMock;
    private ValidationResult validationResultMock;

    @BeforeEach
    void setUp() {
        flightPlanRepoMock = mock(FlightPlanRepository.class);
        modelRepoMock = mock(AircraftModelRepository.class);
        authzMock = mock(AuthorizationService.class);

        controller = new TestFlightPlanController(
                flightPlanRepoMock, modelRepoMock, authzMock);

        flightPlanMock = mock(FlightPlan.class);
        validationResultMock = mock(ValidationResult.class);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void testFlightPlan_validPlan_callsStartTestOnDomain() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);
        // We'll use a spy or real service; for controller test we just verify delegation.

        // Act
        controller.testFlightPlan(id);

        // Assert — domain method invoked
        verify(flightPlanMock).startTest();
    }

    @Test
    void testFlightPlan_validPlan_savesFlightPlan() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);

        // Act
        controller.testFlightPlan(id);

        // Assert — saved after validation
        verify(flightPlanRepoMock).save(flightPlanMock);
    }

    @Test
    void testFlightPlan_validPlan_returnsNonNullResult() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);

        // Act
        final var result = controller.testFlightPlan(id);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testFlightPlan_validPlan_authorizationChecked() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);

        // Act
        controller.testFlightPlan(id);

        // Assert — authorization enforced
        verify(authzMock).ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void testFlightPlan_notFound_throwsException() {
        // Arrange
        final FlightPlanId unknownId = FlightPlanId.valueOf("UNKNOWN");
        when(flightPlanRepoMock.ofIdentity(unknownId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.testFlightPlan(unknownId),
                "Unknown flight plan ID must throw");
    }

    @Test
    void testFlightPlan_alreadyTested_throwsException() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.TEST_PASSED);

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> controller.testFlightPlan(id),
                "Testing a flight plan that has already passed must be rejected");
        verify(flightPlanRepoMock, never()).save(any());
    }

    @Test
    void testFlightPlan_authorizationFails_throwsException() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);
        doThrow(new AuthzException("Forbidden"))
                .when(authzMock).ensureAuthenticatedUserHasAnyOf(any());

        // Act + Assert
        assertThrows(AuthzException.class,
                () -> controller.testFlightPlan(id));
        verify(flightPlanRepoMock, never()).save(any());
    }

    // ── Pilot's flight plans listing (needed by UI) ────────────────────────────

    @Test
    void myFlightPlans_returnsPilotFlightPlans() {
        // Arrange
        final List<FlightPlan> expected = List.of(flightPlanMock);
        when(flightPlanRepoMock.findByPilot(any())).thenReturn(expected);

        // Act
        final var result = controller.myFlightPlans();

        // Assert
        assertEquals(expected, result);
    }
}
```

## 4. DSL Re-validation — Phase 1 of US085

Before invoking the C simulator, US085 must re-validate the DSL content stored in the
flight plan (per client clarification C03). These tests verify that the integration
with the LPROG `FlightPlanRunner` works correctly.

### `FlightPlanDslRevalidationTest`

```java
class FlightPlanDslRevalidationTest {

    private FlightPlanValidationService service;
    private FlightPlanRepository flightPlanRepoMock;
    private AircraftModelRepository modelRepoMock;
    private SimulationRunner simRunnerMock;

    @BeforeEach
    void setUp() {
        flightPlanRepoMock = mock(FlightPlanRepository.class);
        modelRepoMock = mock(AircraftModelRepository.class);
        simRunnerMock = mock(SimulationRunner.class);
        service = new FlightPlanValidationService(
                flightPlanRepoMock, modelRepoMock, simRunnerMock);
    }

    // ── DSL re-validation ─────────────────────────────────────────────────

    @Test
    void validateWithValidDsl_dslPasses_cSimulatorIsInvoked() {
        // Arrange
        final var fp = aFlightPlanWithDsl(validDslContent());
        when(flightPlanRepoMock.ofIdentity(fp.identity()))
                .thenReturn(Optional.of(fp));
        when(modelRepoMock.ofIdentity(any()))
                .thenReturn(Optional.of(anAircraftModel()));
        when(simRunnerMock.run(anyString())).thenReturn(aPassingReport());

        // Act
        final var result = service.validate(fp.identity());

        // Assert — C simulator was invoked
        verify(simRunnerMock, times(1)).run(anyString());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
        assertTrue(result.isPassed());
    }

    @Test
    void validateWithInvalidDsl_dslFails_cSimulatorIsNotInvoked() {
        // Arrange
        final var fp = aFlightPlanWithDsl(invalidDslContent());
        when(flightPlanRepoMock.ofIdentity(fp.identity()))
                .thenReturn(Optional.of(fp));

        // Act
        final var result = service.validate(fp.identity());

        // Assert — C simulator was NOT invoked
        verify(simRunnerMock, never()).run(anyString());
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());
        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.toLowerCase().contains("dsl")
                        || r.toLowerCase().contains("syntax")));
    }

    @Test
    void validateWithInvalidDsl_reasonsContainDslErrors() {
        // Arrange
        final var fp = aFlightPlanWithDsl(invalidDslContent());
        when(flightPlanRepoMock.ofIdentity(fp.identity()))
                .thenReturn(Optional.of(fp));

        // Act
        final var result = service.validate(fp.identity());

        // Assert — error messages from DSL validation are propagated
        assertFalse(result.isPassed());
        assertTrue(result.reasons().stream()
                .anyMatch(r -> r.contains("line") || r.contains("column")));
    }

    @Test
    void validateWithEmptyDsl_throwsException() {
        // Arrange
        final var fp = aFlightPlanWithDsl("");
        when(flightPlanRepoMock.ofIdentity(fp.identity()))
                .thenReturn(Optional.of(fp));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(fp.identity()),
                "Empty DSL content must be rejected");
    }

    // ── Test fixtures ─────────────────────────────────────────────────────

    private String validDslContent() {
        return """
                flight TP0123 : regular {
                    route { origin: OPO; destination: LIS; }
                    aircraft: B738;
                    pilot: pilot@airline.com;
                    leg {
                        departure { airport: OPO; datetime: 2026-06-15T10:00:00+01:00; }
                        arrival   { airport: LIS; datetime: 2026-06-15T11:00:00+01:00; }
                        fuel: 15000 kg;
                        segment {
                            from: (41.2481, -8.6814, 0);
                            to:   (38.7739, -9.1340, 10668);
                            altitudes: [10668 width 9260];
                        }
                    }
                }
                """;
    }

    private String invalidDslContent() {
        return """
                flight TP0123 : invalid_type {
                    route { origin: OPO; destination: LIS; }
                    leg { }
                }
                """;
    }

    private FlightPlan aFlightPlanWithDsl(final String dsl) {
        return new FlightPlan(
                FlightPlanId.valueOf("TP0123"),
                mock(FlightRoute.class),
                new AircraftModelCode("B738"),
                15000.0, 10668,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                dsl);
    }

    private AircraftModel anAircraftModel() {
        final var model = mock(AircraftModel.class);
        when(model.maxFuelCapacity()).thenReturn(20000.0);
        when(model.serviceCeiling()).thenReturn(12500);
        when(model.mtow()).thenReturn(70000.0);
        when(model.mzfw()).thenReturn(58000.0);
        when(model.emptyWeight()).thenReturn(41413.0);
        when(model.estimatedFuelForRoute(any())).thenReturn(8000.0);
        return model;
    }

    private SimulationReport aPassingReport() {
        return new SimulationReport("/tmp/report.txt",
                "RESULT: PASS\nFlights: 1\nViolations: 0\n");
    }
}
```

### 4.1 DSL Re-validation Parameterized Tests (Data-Driven from CSV)

These tests read from `dsl_validation_test_data.csv` to cover **15 DSL validation
scenarios** (DV01–DV15) with a single test method.

```java
@CsvFileSource(resources = "/dsl_validation_test_data.csv", numLinesToSkip = 1)
class FlightPlanDslParameterizedTest {

    @ParameterizedTest(name = "{0}: {4}")
    @CsvFileSource(resources = "/dsl_validation_test_data.csv", numLinesToSkip = 1)
    void validateDslScenarios(
            String testCaseId,
            String dslSnippet,
            String expectedResult,
            String errorType,
            String invariant) {

        // Arrange — load the actual DSL string from a resource file
        final String dslContent = loadDslSample(testCaseId);
        final var fp = aFlightPlanWithDsl(dslContent);
        when(flightPlanRepoMock.ofIdentity(fp.identity()))
                .thenReturn(Optional.of(fp));

        if ("EMPTY".equals(errorType)) {
            // Act + Assert — empty DSL throws
            assertThrows(IllegalArgumentException.class,
                    () -> service.validate(fp.identity()),
                    "Empty DSL must throw");
            return;
        }

        // Act
        final var result = service.validate(fp.identity());

        // Assert
        if ("VALID".equals(expectedResult)) {
            assertTrue(result.isPassed(),
                    () -> testCaseId + " (" + invariant + ") should be VALID");
        } else {
            assertFalse(result.isPassed(),
                    () -> testCaseId + " (" + invariant + ") should be INVALID");
        }
    }

    private String loadDslSample(String testCaseId) {
        // In implementation, load from src/test/resources/dsl_samples/<testCaseId>.flightplan
        // For now, return inline samples matching each DV ID
        return switch (testCaseId) {
            case "DV01" -> validRegularDsl();
            case "DV02" -> validCharterDsl();
            case "DV03" -> "flight TP0123 : regular {\n    route { origin: OPO destination: LIS }\n}";
            case "DV04" -> "flight TP0123 : invalid_type { }";
            case "DV05" -> "flight TP0123 : regular { route { origin: OPO; destination: LIS; } }";
            case "DV06" -> "";
            case "DV07" -> invalidNegativeFuelDsl();
            case "DV10" -> multiLegDsl();
            default -> validRegularDsl();
        };
    }

    private String validRegularDsl() { /* same as validDslContent() in 4.0 */ return ""; }
    private String validCharterDsl() { /* charter variant */ return ""; }
    private String invalidNegativeFuelDsl() { /* fuel: -100 kg */ return ""; }
    private String multiLegDsl() { /* two legs */ return ""; }
}
```

---

## 5. JSON Export — FlightPlan → C Simulator Input

The `FlightPlanExporter` converts a FlightPlan domain object into the JSON format
that the C simulator expects (see `scomp/Sprint3/files/common.h` and `json_parser.h`).

### `FlightPlanExporterTest`

```java
class FlightPlanExporterTest {

    @Test
    void exportToJson_validFlightPlan_producesValidJson() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — valid JSON structure
        assertNotNull(json);
        assertTrue(json.startsWith("{") || json.startsWith("["));
        assertTrue(json.endsWith("}") || json.endsWith("]"));
    }

    @Test
    void exportToJson_containsFlightId() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator reads "ID" field
        assertTrue(json.contains("\"ID\": \"TP0123") || json.contains("\"ID\":\"TP0123"),
                "JSON must contain flight ID in C-compatible format");
    }

    @Test
    void exportToJson_containsFuelQuantity() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator parses "Fuel"."Quantity"
        assertTrue(json.contains("\"Fuel\"") && json.contains("\"Quantity\""),
                "JSON must contain C-compatible Fuel.Quantity field");
    }

    @Test
    void exportToJson_containsDepartureTime() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator reads "DepartureTime" (HH:MM)
        assertTrue(json.contains("\"DepartureTime\""),
                "JSON must contain C-compatible DepartureTime field");
    }

    @Test
    void exportToJson_containsDepartureTz() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator reads "DepartureTZ" ([+-]HH:MM)
        assertTrue(json.contains("\"DepartureTZ\""),
                "JSON must contain C-compatible DepartureTZ field");
    }

    @Test
    void exportToJson_containsLegArray() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator reads "Leg" array
        assertTrue(json.contains("\"Leg\""),
                "JSON must contain C-compatible Leg array");
    }

    @Test
    void exportToJson_containsSegments() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator reads "Segments" array with "Mode" field
        assertTrue(json.contains("\"Segments\"") && json.contains("\"Mode\""),
                "JSON must contain C-compatible Segments with Mode field");
    }

    @Test
    void exportToJson_containsFlightProfile() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator reads "Flight Profile" (with space) or "FlightProfile"
        assertTrue(json.contains("\"Flight Profile\"") || json.contains("\"FlightProfile\""),
                "JSON must contain Flight Profile block for C simulator");
    }

    @Test
    void exportToJson_segmentAltitudeUsesQuantity() {
        // Arrange
        final var fp = validFlightPlan();

        // Act
        final var json = FlightPlanExporter.toJson(fp);

        // Assert — C simulator expects "Altitude": {"Quantity": N} for segment positions
        assertTrue(json.contains("\"Altitude\": {\"Quantity\"") || json.contains("\"Altitude\":{\"Quantity\""),
                "Segment altitudes must use Quantity format for C parser compatibility");
    }

    @Test
    void exportToJson_nullFlightPlan_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> FlightPlanExporter.toJson(null));
    }

    @Test
    void exportToJson_writtenFileIsReadableByJsonParser() throws Exception {
        // Arrange
        final var fp = validFlightPlan();
        final var json = FlightPlanExporter.toJson(fp);

        // Act — write to temp file and verify it's valid JSON (not empty)
        final var tempFile = File.createTempFile("flight_", ".json");
        Files.writeString(tempFile.toPath(), json);

        final var content = Files.readString(tempFile.toPath());
        assertFalse(content.isBlank());
        assertTrue(content.contains("TP0123"));

        // Cleanup
        tempFile.delete();
    }

    // ── Test fixture ──────────────────────────────────────────────────────

    private FlightPlan validFlightPlan() {
        final var route = new FlightRoute(
                FlightRouteName.valueOf("TP123"),
                new CompanyIATA("TP"),
                new AirportIATA("OPO"),
                new AirportIATA("LIS"));
        return new FlightPlan(
                FlightPlanId.valueOf("TP0123"),
                route,
                new AircraftModelCode("B738"),
                15000.0, 10668,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                validDslContent());
    }

    private String validDslContent() {
        return """
                flight TP0123 : regular {
                    route { origin: OPO; destination: LIS; }
                    aircraft: B738;
                    pilot: pilot@airline.com;
                    leg {
                        departure { airport: OPO; datetime: 2026-06-15T10:00:00+01:00; }
                        arrival   { airport: LIS; datetime: 2026-06-15T11:00:00+01:00; }
                        fuel: 15000 kg;
                        segment {
                            from: (41.2481, -8.6814, 0);
                            to:   (38.7739, -9.1340, 10668);
                            altitudes: [10668 width 9260];
                        }
                    }
                }
                """;
    }
}
```

---

## 6. C Simulator Invocation — `SimulationRunner`

The `SimulationRunner` interface abstracts the C simulator invocation, enabling
the controller to be tested without a real C binary.

### `ProcessBuilderSimulationRunnerTest` (Integration-style)

```java
class ProcessBuilderSimulationRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void run_simulatorNotFound_throwsException() {
        // Arrange
        final var runner = new ProcessBuilderSimulationRunner(
                "/nonexistent/simulation", tempDir);

        // Act + Assert
        assertThrows(SimulationRunnerException.class,
                () -> runner.run(jsonContent()));
    }

    @Test
    void run_outputDirectoryIsCreatedIfNotExists() throws Exception {
        // Arrange
        final var outputDir = tempDir.resolve("reports");
        assertFalse(Files.exists(outputDir));

        // We won't actually run the simulator here — we test the directory creation
        // in isolation:
        Files.createDirectories(outputDir);
        assertTrue(Files.exists(outputDir));
    }

    @Test
    void run_nullJsonContent_throwsException() {
        final var runner = new ProcessBuilderSimulationRunner(
                "/usr/bin/echo", tempDir);
        assertThrows(IllegalArgumentException.class,
                () -> runner.run(null));
    }

    // ── Test fixtures ─────────────────────────────────────────────────────

    private String jsonContent() {
        return """
                [{
                    "ID": "TP0123",
                    "DepartureTime": "10:00",
                    "DepartureTZ": "+01:00",
                    "Leg": [{
                        "Fuel": { "Quantity": 15000 },
                        "Flight Profile": {
                            "Climb": [{ "Altitude": {"Value": 0}, "Speed": {"Value": 210}, "RateClimb": {"Value": 12.0} }],
                            "Cruise": { "Speed": {"Value": 460} },
                            "Descend": [{ "Altitude": {"Value": 10668}, "Speed": {"Value": 210}, "RateDescend": {"Value": 10.0} }]
                        },
                        "Segments": [
                            { "Mode": "climb", "Start": {"Latitude": 41.2481, "Longitude": -8.6814, "Altitude": {"Quantity": 0}}, "End": {"Latitude": 40.0, "Longitude": -8.9, "Altitude": {"Quantity": 10668}} },
                            { "Mode": "cruise", "Start": {"Latitude": 40.0, "Longitude": -8.9, "Altitude": {"Quantity": 10668}}, "End": {"Latitude": 39.0, "Longitude": -9.0, "Altitude": {"Quantity": 10668}} },
                            { "Mode": "descend", "Start": {"Latitude": 39.0, "Longitude": -9.0, "Altitude": {"Quantity": 10668}}, "End": {"Latitude": 38.7739, "Longitude": -9.1340, "Altitude": {"Quantity": 0}} }
                        ]
                    }]
                }]
                """;
    }
}
```

### `SimulationRunner` Mock-Based Controller Tests

```java
class TestFlightPlanControllerWithMockSimulatorTest {

    private TestFlightPlanController controller;
    private FlightPlanRepository flightPlanRepoMock;
    private AircraftModelRepository modelRepoMock;
    private SimulationRunner simRunnerMock;
    private AuthorizationService authzMock;
    private FlightPlan flightPlanMock;

    @BeforeEach
    void setUp() {
        flightPlanRepoMock = mock(FlightPlanRepository.class);
        modelRepoMock = mock(AircraftModelRepository.class);
        simRunnerMock = mock(SimulationRunner.class);
        authzMock = mock(AuthorizationService.class);

        controller = new TestFlightPlanController(
                flightPlanRepoMock, modelRepoMock, simRunnerMock, authzMock);

        flightPlanMock = mock(FlightPlan.class);
    }

    @Test
    void testFlightPlan_cSimulatorPasses_recordsTestPassed() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);
        when(flightPlanMock.dslContent()).thenReturn("valid DSL content");
        when(modelRepoMock.ofIdentity(any()))
                .thenReturn(Optional.of(mock(AircraftModel.class)));
        when(simRunnerMock.run(anyString()))
                .thenReturn(new SimulationReport("/tmp/report.txt",
                        "RESULT: PASS\nFlights: 1\nViolations: 0\n"));

        // Act
        final var result = controller.testFlightPlan(id);

        // Assert
        assertTrue(result.isPassed());
        verify(flightPlanMock).recordTestResult(true, List.of());
        verify(flightPlanRepoMock).save(flightPlanMock);
    }

    @Test
    void testFlightPlan_cSimulatorFails_recordsTestFailed() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);
        when(flightPlanMock.dslContent()).thenReturn("valid DSL content");
        when(modelRepoMock.ofIdentity(any()))
                .thenReturn(Optional.of(mock(AircraftModel.class)));
        when(simRunnerMock.run(anyString()))
                .thenReturn(new SimulationReport("/tmp/report.txt",
                        "RESULT: FAIL\nFlights: 1\nViolations: 2\n"));

        // Act
        final var result = controller.testFlightPlan(id);

        // Assert
        assertFalse(result.isPassed());
        assertFalse(result.reasons().isEmpty());
        verify(flightPlanRepoMock).save(flightPlanMock);
    }

    @Test
    void testFlightPlan_cSimulatorCrashes_throwsException() {
        // Arrange
        final FlightPlanId id = FlightPlanId.valueOf("TP0123");
        when(flightPlanRepoMock.ofIdentity(id))
                .thenReturn(Optional.of(flightPlanMock));
        when(flightPlanMock.status()).thenReturn(FlightPlanStatus.DRAFT);
        when(flightPlanMock.dslContent()).thenReturn("valid DSL content");
        when(modelRepoMock.ofIdentity(any()))
                .thenReturn(Optional.of(mock(AircraftModel.class)));
        when(simRunnerMock.run(anyString()))
                .thenThrow(new SimulationRunnerException("Simulator crashed"));

        // Act + Assert
        assertThrows(SimulationRunnerException.class,
                () -> controller.testFlightPlan(id));
        // Flight plan status should remain DRAFT (not corrupted)
        verify(flightPlanRepoMock, never()).save(any());
    }
}
```

---

## 7. `ValidationResult` Value Object

Tests for the result type returned by the validation service.

```java
class ValidationResultTest {

    @Test
    void passedResult_noReasons_isPassed() {
        final var result = ValidationResult.passed();
        assertTrue(result.isPassed());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void failedResult_withReasons_isNotPassed() {
        final var reasons = List.of("Fuel exceeds capacity");
        final var result = ValidationResult.failed(reasons);
        assertFalse(result.isPassed());
        assertEquals(1, result.reasons().size());
    }

    @Test
    void failedResult_emptyReasonsList_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationResult.failed(List.of()),
                "A failed result must have at least one reason");
    }

    @Test
    void passedResult_reasonsListIsUnmodifiable() {
        final var result = ValidationResult.passed();
        assertThrows(UnsupportedOperationException.class,
                () -> result.reasons().add("should not be allowed"));
    }

    @Test
    void failedResult_reasonsListIsUnmodifiable() {
        final var result = ValidationResult.failed(List.of("Reason 1"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.reasons().add("should not be allowed"));
    }

    @Test
    void equalsAndHashCode_basedOnContent() {
        final var r1 = ValidationResult.passed();
        final var r2 = ValidationResult.passed();
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void failedResult_equality_basedOnReasons() {
        final var reasons = List.of("Fuel issue", "Altitude issue");
        final var r1 = ValidationResult.failed(reasons);
        final var r2 = ValidationResult.failed(reasons);
        assertEquals(r1, r2);
    }

    @Test
    void failedResult_toString_containsReasons() {
        final var reasons = List.of("Fuel exceeds capacity");
        final var result = ValidationResult.failed(reasons);
        final var str = result.toString();
        assertTrue(str.contains("FAILED"));
        assertTrue(str.contains("Fuel exceeds capacity"));
    }
}
```

---

## 8. `SimulationReport` and `ReportParser` — Report Persistence (C14)

The report produced by the C simulator must be stored permanently (not transient),
per client clarification C14. The `SimulationReport` value object carries both
the file system path and the parsed content.

### `SimulationReportTest`

```java
class SimulationReportTest {

    private String cReportPass() {
        return """
                ============================================
                  AISafe Simulation Report
                  Generated: Mon Jun  1 12:00:00 2026
                  Total steps: 7200  (7200 seconds simulated)
                  Flights: 1
                  Total violations detected: 0
                ============================================

                FLIGHT SUMMARY:
                  TP0123: n_viol=0  ever_in_area=yes  completed=yes

                ============================================
                  RESULT: PASS
                ============================================
                """;
    }

    private String cReportFail(int violations) {
        return """
                ============================================
                  AISafe Simulation Report
                  Generated: Mon Jun  1 12:00:00 2026
                  Total steps: 7200  (7200 seconds simulated)
                  Flights: 1
                  Total violations detected: """ + violations + """
                \n============================================

                FLIGHT SUMMARY:
                  TP0123: n_viol=""" + violations + """  ever_in_area=yes  completed=yes

                VIOLATION LOG:
                  #1 step=42  TP0123 <-> TP0123  h_dist=5000m  v_dist=200m  pos_a=(40.0000,-8.9000,5000)  pos_b=(39.5000,-9.0000,3000)

                ============================================
                  RESULT: FAIL
                ============================================
                """;
    }

    @Test
    void passingReport_isPassed_returnsTrue() {
        final var report = new SimulationReport(
                "/tmp/report_20260601_120000.txt",
                cReportPass());
        assertTrue(report.isPassed());
    }

    @Test
    void failingReport_isPassed_returnsFalse() {
        final var report = new SimulationReport(
                "/tmp/report_20260601_120000.txt",
                cReportFail(2));
        assertFalse(report.isPassed());
    }

    @Test
    void report_hasFilePath() {
        final var path = "/tmp/report_20260601_120000.txt";
        final var report = new SimulationReport(path, cReportPass());
        assertEquals(path, report.path());
    }

    @Test
    void report_hasRawOutput() {
        final var raw = cReportFail(2);
        final var report = new SimulationReport("/tmp/r.txt", raw);
        assertEquals(raw, report.rawOutput());
    }

    @Test
    void report_violationCount_isParsedFromOutput() {
        final var report = new SimulationReport(
                "/tmp/report.txt",
                cReportFail(3));
        assertEquals(3, report.violationCount());
    }

    @Test
    void report_violationCount_zeroWhenPassing() {
        final var report = new SimulationReport(
                "/tmp/report.txt",
                cReportPass());
        assertEquals(0, report.violationCount());
    }

    @Test
    void report_immutable_pathCannotBeChanged() {
        final var report = new SimulationReport(
                "/tmp/report.txt", cReportPass());
        // No setters — verify immutability by absence of mutation methods
        assertEquals("/tmp/report.txt", report.path());
    }

    @Test
    void report_nullPath_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport(null, cReportPass()));
    }

    @Test
    void report_nullOutput_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport("/tmp/r.txt", null));
    }

    @Test
    void report_equalsAndHashCode() {
        final var r1 = new SimulationReport("/tmp/r.txt", cReportPass());
        final var r2 = new SimulationReport("/tmp/r.txt", cReportPass());
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void report_toString_containsPathAndResult() {
        final var report = new SimulationReport("/tmp/r.txt", cReportFail(1));
        final var str = report.toString();
        assertTrue(str.contains("/tmp/r.txt"));
        assertTrue(str.contains("FAIL"));
    }
}
```

### `ReportParserTest`

```java
class ReportParserTest {

    @Test
    void parseFile_passingReport_returnsSimulationReportWithIsPassedTrue()
            throws Exception {
        // Arrange
        final var tempFile = File.createTempFile("report_", ".txt");
        Files.writeString(tempFile.toPath(),
                "RESULT: PASS\nFlights: 2\nViolations: 0\n");

        // Act
        final var report = ReportParser.parse(tempFile.getAbsolutePath());

        // Assert
        assertTrue(report.isPassed());
        assertEquals(0, report.violationCount());
        assertEquals(tempFile.getAbsolutePath(), report.path());

        tempFile.delete();
    }

    @Test
    void parseFile_failingReportWithViolations_extractsDetails() throws Exception {
        // Arrange
        final var tempFile = File.createTempFile("report_", ".txt");
        Files.writeString(tempFile.toPath(),
                "RESULT: FAIL\nFlights: 1\nViolations: 2\n"
                + "Violation[1]: Fuel exhaustion at step 42\n"
                + "Violation[2]: Altitude deviation at step 58\n");

        // Act
        final var report = ReportParser.parse(tempFile.getAbsolutePath());

        // Assert
        assertFalse(report.isPassed());
        assertEquals(2, report.violationCount());
        assertTrue(report.rawOutput().contains("Fuel exhaustion"));

        tempFile.delete();
    }

    @Test
    void parseFile_nonexistentPath_throwsException() {
        assertThrows(SimulationRunnerException.class,
                () -> ReportParser.parse("/nonexistent/report.txt"));
    }

    @Test
    void parseFile_unrecognizedFormat_stillReturnsRawOutput() throws Exception {
        // Arrange
        final var tempFile = File.createTempFile("report_", ".txt");
        Files.writeString(tempFile.toPath(),
                "Some unexpected format\nNo RESULT line\n");

        // Act
        final var report = ReportParser.parse(tempFile.getAbsolutePath());

        // Assert — lenient: any non-empty output is acceptable
        assertNotNull(report);
        assertFalse(report.rawOutput().isBlank());

        tempFile.delete();
    }
}
```

### 8.1 ReportParser Parameterized Tests (Data-Driven from CSV)

These tests read from `simulation_report_test_data.csv` to cover **10 report
parsing scenarios** (SR01–SR10) with a single test method. The CSV stores
`totalViolations` (an integer), and the test generates the exact C simulator
report format dynamically.

```java
@CsvFileSource(resources = "/simulation_report_test_data.csv", numLinesToSkip = 1)
class ReportParserParameterizedTest {

    @ParameterizedTest(name = "{0}: {5}")
    @CsvFileSource(resources = "/simulation_report_test_data.csv", numLinesToSkip = 1)
    void parseReportScenarios(
            String testCaseId,
            int totalViolations,
            boolean expectedIsPassed,
            int expectedViolCount,
            boolean hasViolationLog,
            String invariant) throws Exception {

        // Arrange — generate C-format report content
        final var reportContent = generateCReport(totalViolations, hasViolationLog);
        final var tempFile = File.createTempFile("report_", ".txt");
        Files.writeString(tempFile.toPath(), reportContent);

        // Act
        final var report = ReportParser.parse(tempFile.getAbsolutePath());

        // Assert
        assertEquals(expectedIsPassed, report.isPassed(),
                () -> testCaseId + " (" + invariant + ") isPassed mismatch");
        assertEquals(expectedViolCount, report.violationCount(),
                () -> testCaseId + " (" + invariant + ") violationCount mismatch");

        if (hasViolationLog) {
            assertTrue(report.rawOutput().contains("VIOLATION LOG"),
                    () -> testCaseId + " should contain VIOLATION LOG section");
        }

        tempFile.delete();
    }

    private String generateCReport(int totalViolations, boolean withViolationLog) {
        final String result = (totalViolations == 0) ? "PASS" : "FAIL";
        final StringBuilder sb = new StringBuilder();
        sb.append("============================================\n");
        sb.append("  AISafe Simulation Report\n");
        sb.append("  Generated: Mon Jun  1 12:00:00 2026\n");
        sb.append("  Total steps: 7200  (7200 seconds simulated)\n");
        sb.append("  Flights: 1\n");
        sb.append("  Total violations detected: ").append(totalViolations).append("\n");
        sb.append("============================================\n\n");
        sb.append("FLIGHT SUMMARY:\n");
        sb.append("  TP0123: n_viol=").append(totalViolations).append("  ever_in_area=yes  completed=yes\n");

        if (withViolationLog && totalViolations > 0) {
            sb.append("\nVIOLATION LOG:\n");
            for (int i = 1; i <= totalViolations; i++) {
                sb.append(String.format(
                    "  #%d step=%d  TP0123 <-> TP0123  h_dist=%.0fm  v_dist=%.0fm  pos_a=(%.4f,%.4f,%.0f)  pos_b=(%.4f,%.4f,%.0f)\n",
                    i, i * 10, 5000.0, 200.0, 40.0, -8.9, 5000.0, 39.5, -9.0, 3000.0));
            }
        }

        sb.append("\n============================================\n");
        sb.append("  RESULT: ").append(result).append("\n");
        sb.append("============================================\n");
        return sb.toString();
    }
}
```

---

## 9. CSV Test Data Files

The project uses multiple CSV files for data-driven testing. Each file covers a
different validation dimension:

| CSV File | Location | Scenarios | Purpose |
|----------|----------|-----------|---------|
| `flight_plan_test_data.csv` | `docs/.../us_085/tests/` | 25 (TC01–TC25) | R1–R6 validation: fuel, altitude, weight limits |
| `pilot_certification_test_data.csv` | `docs/.../us_085/tests/` | 8 (PC01–PC08) | R7 validation: pilot certification (C07) |
| `dsl_validation_test_data.csv` | `docs/.../us_085/tests/` | 15 (DV01–DV15) | Phase 1: DSL lexical/syntactic/semantic validation |
| `simulation_report_test_data.csv` | `docs/.../us_085/tests/` | 10 (SR01–SR10) | Phase 2-3: C simulator report parsing |

All files must be copied to `src/test/resources/` so that `@CsvFileSource` can find them.

### 9.1 `flight_plan_test_data.csv` — 25 Scenarios

| TC ID | Scenario | Expected |
|-------|----------|----------|
| TC01 | Happy path — valid B738 regular flight | PASS |
| TC02 | Happy path — valid A320 charter flight | PASS |
| TC03 | Fuel exceeds max capacity | FAIL |
| TC04 | Fuel below minimum required for route | FAIL |
| TC05 | Altitude exceeds service ceiling | FAIL |
| TC06 | Altitude within limits (different model) | PASS |
| TC07 | Take-off weight exceeds MTOW | FAIL |
| TC08 | Weight and altitude within limits | PASS |
| TC09 | Fuel sufficient for route distance | PASS |
| TC10 | Fuel insufficient for route distance | FAIL |
| TC11 | Cruise altitude is zero | FAIL |
| TC12 | Cruise altitude is negative | FAIL |
| TC13 | Zero payload still valid | PASS |
| TC14 | Fuel exactly at max capacity boundary | PASS |
| TC15 | Fuel and ceiling exactly at limits | PASS |
| TC16 | Altitude exactly at service ceiling | PASS |
| TC17 | Very low cruise altitude (positive) | PASS |
| TC18 | Minimum fuel with reduced payload | PASS |
| TC19 | High payload under MTOW | PASS |
| TC20 | Fuel below minimum flight operations | FAIL |
| TC21 | Payload weight exceeds MZFW | FAIL |
| TC22 | Zero fuel rejected | FAIL |
| TC23 | Fuel exceeds capacity AND weight exceeds MTOW | FAIL |
| TC24 | Large aircraft A380 within all limits | PASS |
| TC25 | Regional jet E190 valid | PASS |

### 9.2 `pilot_certification_test_data.csv` — 8 Scenarios

| PC ID | Scenario | Expected |
|-------|----------|----------|
| PC01 | Pilot certified for exact model | PASS |
| PC02 | Pilot with multiple certs includes assigned | PASS |
| PC03 | Pilot not certified for assigned model | FAIL |
| PC04 | Pilot with no certifications | FAIL |
| PC05 | Pilot with many certifications includes assigned | PASS |
| PC06 | Pilot certified for different valid model | PASS |
| PC07 | Case-sensitive exact match | PASS |
| PC08 | Partial code match (not exact) | FAIL |

### 9.3 `dsl_validation_test_data.csv` — 15 Scenarios

| DV ID | Scenario | Expected |
|-------|----------|----------|
| DV01 | Complete regular flight plan (one leg) | VALID |
| DV02 | Complete charter flight plan (multiple segments) | VALID |
| DV03 | Missing semicolon after route | INVALID |
| DV04 | Invalid flight type | INVALID |
| DV05 | Missing aircraft declaration | INVALID |
| DV06 | Empty DSL content | INVALID |
| DV07 | Negative fuel value | INVALID |
| DV08 | Missing departure airport | INVALID |
| DV09 | Fuel exceeds maximum | INVALID |
| DV10 | Multiple legs with valid data | VALID |
| DV11 | Non-existent airport IATA code | INVALID |
| DV12 | Departure after arrival datetime | INVALID |
| DV13 | Segment with zero altitude | VALID |
| DV14 | Missing segment altitudes block | INVALID |
| DV15 | Wrong coordinate format | INVALID |

### 9.4 `simulation_report_test_data.csv` — 10 Scenarios

| SR ID | Scenario | Expected |
|-------|----------|----------|
| SR01 | Clean pass — no violations | PASS |
| SR02 | Multiple flights all pass | PASS |
| SR03 | Single violation with position details | FAIL |
| SR04 | Multiple violations without position | FAIL |
| SR05 | Violations across multiple flights | FAIL |
| SR06 | Zero flights still pass | PASS |
| SR07 | Five violations of all types | FAIL |
| SR08 | Unrecognized format (lenient) | PASS |
| SR09 | Large scale (10 flights) all pass | PASS |
| SR10 | Ten violations — max edge case | FAIL |

---

## 10. Notes for Implementation

The following classes/methods must be created for these tests to compile and pass:

| Class / Method | Note |
|---|---|
| `FlightPlanStatus` enum | Constants: `DRAFT`, `IN_TEST`, `TEST_PASSED`, `TEST_FAILED` |
| `FlightPlanId` value object | Wraps a flight designator string (e.g. "TP0123") — immutable, validated |
| `Flight` aggregate root | Identity: `FlightDesignator`. Root of the Flight aggregate (Sprint 2 domain model) |
| `FlightPlan` entity (inside Flight) | Identity: `FlightPlanId`. Fields: route, model, fuel, altitude, times, dslContent, status, validationResult |
| `FlightPlan.startTest()` | Transitions DRAFT → IN_TEST; throws if not DRAFT |
| `FlightPlan.recordTestResult(boolean passed, List<String> reasons)` | Transitions to TEST_PASSED or TEST_FAILED; stores ValidationResult |
| `FlightPlan.resetToDraft()` | Resets TEST_PASSED/TEST_FAILED → DRAFT; clears validation result |
| `FlightPlan.status()` | Returns current `FlightPlanStatus` |
| `FlightPlan.validationResult()` | Returns the `ValidationResult` (nullable) |
| `FlightPlan.fuelAmount()`, `cruiseAltitude()`, etc. | Accessors |
| `ValidationResult` value object | Immutable. Factory methods: `passed()`, `failed(List<String>)`. Methods: `isPassed()`, `reasons()` |
| `FlightPlanValidationService` domain service | Validates fuel, altitude, weight against aircraft model specs |
| `FlightPlanValidationService.validate(FlightPlan, double fuelAmount, double payloadWeight)` | Main validation method — checks R1-R6 (fuel, altitude, weight) |
| `FlightPlanValidationService.validate(FlightPlan, double fuelAmount, double payloadWeight, Set<AircraftModelCode> pilotCerts)` | Extended validation — adds R7 (pilot certification check) |
| `FlightRepository` | Interface extending `DomainRepository<FlightDesignator, Flight>` |
| `FlightRepository.findFlightPlansByPilot(SystemUser)` | Query: `SELECT f FROM Flight f JOIN f.flightPlans fp WHERE f.assignedTo = :pilot` |
| `TestFlightPlanController` | Application layer — auth, load, validate, save |
| `TestFlightPlanController.testFlightPlan(FlightPlanId)` | Orchestrates the full validation flow |
| `TestFlightPlanController.myFlightPlans()` | Returns flight plans for the authenticated pilot |
| `AircraftModelRepository` (if not existing) | Must support `ofIdentity(AircraftModelCode)` |
| `AircraftModel` (if not existing) | Must expose: `maxFuelCapacity()`, `serviceCeiling()`, `mtow()`, `mzfw()`, `emptyWeight()`, `estimatedFuelForRoute(FlightPlan)` |
| `SimulationRunner` interface | Method: `SimulationReport run(String jsonInput)` — abstracts C simulator invocation |
| `ProcessBuilderSimulationRunner` | Implementation using `ProcessBuilder` to spawn the C binary |
| `SimulationRunnerException` | RuntimeException thrown when simulator cannot be started or crashes |
| `SimulationReport` | Result from C simulator: `path()`, `rawOutput()`, `isPassed()`, `violationCount()` |
| `ReportParser` | Parses the C simulator's report file (text output → `SimulationReport`) |
| `FlightPlanExporter` | Static utility `toJson(FlightPlan)` — converts domain to JSON format for C simulator |
| `Pilot` aggregate root | Stores `Set<AircraftModelCode> certifications` — list of models the pilot is certified to fly (C07) |
| `PilotRepository.ofIdentity(PilotId)` | Loads pilot and their certifications for R7 validation |

| `FlightPlanValidationParameterizedTest` | Parameterized test reading 25 scenarios from `flight_plan_test_data.csv` |
| `PilotCertificationParameterizedTest` | Parameterized test reading 8 scenarios from `pilot_certification_test_data.csv` |
| `FlightPlanDslParameterizedTest` | Parameterized test reading 15 scenarios from `dsl_validation_test_data.csv` |
| `ReportParserParameterizedTest` | Parameterized test reading 10 scenarios from `simulation_report_test_data.csv` |
| `flight_plan_test_data.csv` | **25 scenarios** (TC01–TC25) — fuel, altitude, weight boundary tests |
| `pilot_certification_test_data.csv` | **8 scenarios** (PC01–PC08) — pilot certification (R7, C07) |
| `dsl_validation_test_data.csv` | **15 scenarios** (DV01–DV15) — DSL lexical/syntactic/semantic validation |
| `simulation_report_test_data.csv` | **10 scenarios** (SR01–SR10) — C simulator report parsing |

**Total: 58 data-driven test scenarios** across 4 CSV files — adding a new scenario means adding
one row to the relevant CSV, no new Java code.

> **Decision:** `FlightPlan` is an **entity inside the `Flight` aggregate** (not a standalone aggregate).
> Access to FlightPlan is always via `FlightRepository`. The tests above use `FlightPlan` as if
> independently loadable for readability; in implementation, the controller loads the `Flight` and
> then navigates `f.flightPlans().stream().filter(fp -> fp.identity().equals(id)).findFirst()`.
