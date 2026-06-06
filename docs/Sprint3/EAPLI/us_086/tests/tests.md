# US086 — Pilot Remote Access: Test Plan

## Table of Contents

1. [Test Overview](#1-test-overview)
2. [System Architecture & Test Scope](#2-system-architecture--test-scope)
3. [RemotePilotService Unit Tests](#3-remotepilotservice-unit-tests)
4. [AircraftDTO Unit Tests](#4-aircraftdto-unit-tests)
5. [Parameterized Tests (CSV-Driven)](#5-parameterized-tests-csv-driven)
6. [Coverage & Results](#6-coverage--results)

---

## 1. Test Overview

**Goal:** Verify that `RemotePilotService` correctly wraps existing FCO
controllers as a remote-access facade, delegating to the correct application
controller for each client operation.

**Testing approach:**
- Pure unit tests with mocks (Mockito) — no infrastructure or persistence
- All 8 remote operations are tested for **correct delegation**, **return type
  correctness**, and **edge cases**
- `AircraftDTO` value object tested separately for field mapping, immutability,
  and constructor behavior
- Both production (no-arg) and test-friendly (injected) constructors validated
- **Data-driven (CSV) tests** for each operation group, following the
  professor's requirement for parameterized tests with external files
- 3 CSV files drive 30 parameterized test scenarios

**Test classes:**

| Class | Package | Tests |
|-------|---------|-------|
| `RemotePilotServiceTest` | `eapli.aisafe.remote.pilot` | 15 |
| `AircraftDTOTest` | `eapli.aisafe.remote.pilot` | 11 |
| `FleetListingParameterizedTest` | `eapli.aisafe.remote.pilot` | 10 |
| `FlightOperationParameterizedTest` | `eapli.aisafe.remote.pilot` | 12 |
| `ReportOperationParameterizedTest` | `eapli.aisafe.remote.pilot` | 8 |
| **Total** | | **56** |

---

## 2. System Architecture & Test Scope

### 2.1 System Under Test

```
┌────────────────────────┐
│  PilotClientHandler    │  (TCP server handler)
│  calls ⋮               │
└─────────┬──────────────┘
          │
┌─────────▼──────────────┐
│  RemotePilotService    │  (FACADE, package eapli.aisafe.remote.pilot)
│                        │
│  listFleet()           ──>  ListCompanyFleetController.allActiveAircraft()
│  createFlightPlan()    ──>  ImportFlightPlanController.importFlightPlan()
│  importFlightPlan()    ──>  ImportFlightPlanController.importFlightPlan()
│  validateFlightPlan()  ──>  TestFlightPlanController.testFlightPlan()
│  generateReport()      ──>  GenerateSimulationReportController.generateReport()
│  monthlyReport()       ──>  GenerateMonthlyReportController.generateForMonth()
│  listFlights()         ──>  TestFlightPlanController.allFlights()
│  listRoutes()          ──>  PersistenceContext.repositories().flightRoutes()
└────────────────────────┘
```

**Key design notes:**
- `RemotePilotService` is a **thin facade** — it has no business logic of its
  own and no authorization checks (those live inside each underlying controller)
- All controllers are created via no-arg constructors in production, or injected
  via a package-private constructor for testing
- `listRoutes()` uses `PersistenceContext` directly and requires integration
  testing; excluded from unit test scope

### 2.2 Test Matrix

| # | Operation | Delegation | Return Type | Edge Cases |
|---|-----------|-----------|-------------|------------|
| 1 | `listFleet` | ✓ | `List<AircraftDTO>` | Empty fleet, DTO field mapping |
| 2 | `createFlightPlan` | ✓ | `DslValidationResult` | Invalid DSL delegation |
| 3 | `importFlightPlan` | ✓ | `DslValidationResult` | Invalid DSL delegation |
| 4 | `validateFlightPlan` | ✓ | `TestResult` | Unknown/rejected IDs |
| 5 | `generateReport` | ✓ | `String` (file path) | Unknown area code |
| 6 | `monthlyReport` | ✓ | `MonthlyReport` | Year/month validation |
| 7 | `listFlights` | ✓ | `List<?>` | Empty list |
| 8 | `listRoutes` | — | `List<?>` | Requires integration test |

### 2.3 Invariants (Business Rules)

- **IR01** — Every public method must delegate to the correct underlying
  controller.
- **IR02** — No business logic is performed by the facade itself; all logic
  lives in delegated controllers.
- **IR03** — Constructor injection works; production no-arg constructor does
  not throw.
- **IR04** — Exceptions from underlying controllers must propagate unchanged.
- **IR05** — `listFleet` must return `List<AircraftDTO>`, never domain entities.
- **IR06** — Every operation must be callable without any UI dependency.

---

## 3. RemotePilotService Unit Tests

### 3.1 Constructor and Delegation Tests

```java
package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemotePilotServiceTest {

    private ListCompanyFleetController fleetCtrl;
    private ImportFlightPlanController importCtrl;
    private TestFlightPlanController testCtrl;
    private GenerateSimulationReportController reportCtrl;
    private GenerateMonthlyReportController monthlyCtrl;
    private RemotePilotService service;

    @BeforeEach
    void setUp() {
        fleetCtrl = mock(ListCompanyFleetController.class);
        importCtrl = mock(ImportFlightPlanController.class);
        testCtrl = mock(TestFlightPlanController.class);
        reportCtrl = mock(GenerateSimulationReportController.class);
        monthlyCtrl = mock(GenerateMonthlyReportController.class);
        service = new RemotePilotService(fleetCtrl, importCtrl, testCtrl, reportCtrl, monthlyCtrl);
    }

    @Test
    void noArgConstructorCreatesService() {
        assertDoesNotThrow(() -> new RemotePilotService());
    }

    @Test
    void listFleetDelegatesToController() {
        when(fleetCtrl.allActiveAircraft()).thenReturn(List.of());
        final var result = service.listFleet();
        verify(fleetCtrl).allActiveAircraft();
        assertNotNull(result);
    }

    @Test
    void listFleetReturnsDTOs() {
        final var ac = new Aircraft(
                new RegistrationNumber("CS-TUI", "Portugal"),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
        when(fleetCtrl.allActiveAircraft()).thenReturn(List.of(ac));
        final var result = service.listFleet();
        assertEquals(1, result.size());
        assertInstanceOf(AircraftDTO.class, result.get(0));
        assertEquals("CS-TUI", result.get(0).registrationNumber());
    }

    @Test
    void listFleetReturnsEmptyWhenNoAircraft() {
        when(fleetCtrl.allActiveAircraft()).thenReturn(List.of());
        final var result = service.listFleet();
        assertTrue(result.isEmpty());
    }

    @Test
    void createFlightPlanDelegatesToImportController() {
        final var expected = mock(ImportFlightPlanController.DslValidationResult.class);
        when(importCtrl.importFlightPlan("valid DSL", "remote-FL123", "FL123"))
                .thenReturn(expected);
        final var result = service.createFlightPlan("FL123", "valid DSL");
        verify(importCtrl).importFlightPlan("valid DSL", "remote-FL123", "FL123");
        assertSame(expected, result);
    }

    @Test
    void importFlightPlanDelegatesToImportController() {
        final var expected = mock(ImportFlightPlanController.DslValidationResult.class);
        when(importCtrl.importFlightPlan("dsl content", "remote-FP002", "FP002"))
                .thenReturn(expected);
        final var result = service.importFlightPlan("FP002", "dsl content");
        verify(importCtrl).importFlightPlan("dsl content", "remote-FP002", "FP002");
        assertSame(expected, result);
    }

    @Test
    void validateFlightPlanDelegatesToTestController() {
        final var expected = mock(TestFlightPlanController.TestResult.class);
        when(testCtrl.testFlightPlan("FP001")).thenReturn(expected);
        final var result = service.validateFlightPlan("FP001");
        verify(testCtrl).testFlightPlan("FP001");
        assertSame(expected, result);
    }

    @Test
    void generateReportDelegatesToReportController() {
        when(reportCtrl.generateReport("LPPC")).thenReturn("/tmp/report.txt");
        final var result = service.generateReport("LPPC");
        verify(reportCtrl).generateReport("LPPC");
        assertEquals("/tmp/report.txt", result);
    }

    @Test
    void monthlyReportDelegatesToMonthlyController() {
        final var expected = mock(MonthlyReport.class);
        when(monthlyCtrl.generateForMonth(YearMonth.of(2026, 5))).thenReturn(expected);
        final var result = service.monthlyReport(2026, 5);
        verify(monthlyCtrl).generateForMonth(YearMonth.of(2026, 5));
        assertSame(expected, result);
    }

    @Test
    void listFlightsDelegatesToTestController() {
        when(testCtrl.allFlights()).thenReturn(List.of());
        final var result = service.listFlights();
        verify(testCtrl).allFlights();
        assertNotNull(result);
    }
}
```

### 3.2 Return Type and Edge Case Tests

```java
    @Test
    void createFlightPlanReturnsValidationResult() {
        when(importCtrl.importFlightPlan(anyString(), anyString(), anyString()))
                .thenReturn(mock(ImportFlightPlanController.DslValidationResult.class));
        final var result = service.createFlightPlan("FL001", "valid DSL");
        assertNotNull(result);
    }

    @Test
    void validateFlightPlanReturnsTestResult() {
        when(testCtrl.testFlightPlan(anyString()))
                .thenReturn(mock(TestFlightPlanController.TestResult.class));
        final var result = service.validateFlightPlan("FP001");
        assertNotNull(result);
    }

    @Test
    void generateReportReturnsString() {
        when(reportCtrl.generateReport(anyString())).thenReturn("report-path");
        final var result = service.generateReport("LPPC");
        assertInstanceOf(String.class, result);
    }

    @Test
    void monthlyReportReturnsMonthlyReport() {
        when(monthlyCtrl.generateForMonth(any(YearMonth.class)))
                .thenReturn(mock(MonthlyReport.class));
        final var result = service.monthlyReport(2026, 5);
        assertInstanceOf(MonthlyReport.class, result);
    }

    @Test
    void listFlightsReturnsList() {
        when(testCtrl.allFlights()).thenReturn(List.of("item1", "item2"));
        final var result = service.listFlights();
        assertEquals(2, result.size());
    }
}
```

---

## 4. AircraftDTO Unit Tests

### 4.1 Field Mapping and Immutability

```java
package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AircraftDTOTest {

    private Aircraft sampleAircraft() {
        return new Aircraft(
                new RegistrationNumber("CS-TUI", "Portugal"),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
    }

    @Test
    void fromMapsRegistrationNumber() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("CS-TUI", dto.registrationNumber());
    }

    @Test
    void fromMapsAircraftModelCode() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("A320", dto.aircraftModelCode());
    }

    @Test
    void fromMapsOperationalStatus() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("ACTIVE", dto.operationalStatus());
    }

    @Test
    void fromMapsTotalCapacity() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals(180, dto.totalCapacity());
    }

    @Test
    void fromMapsDecommissionedStatus() {
        final var ac = sampleAircraft();
        ac.decommission();
        final var dto = AircraftDTO.from(ac);
        assertEquals("DECOMMISSIONED", dto.operationalStatus());
    }

    @Test
    void equalsAndHashCode() {
        final var ac = sampleAircraft();
        final var dto1 = AircraftDTO.from(ac);
        final var dto2 = AircraftDTO.from(sampleAircraft());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEqualsDifferentRegistration() {
        final var ac1 = sampleAircraft();
        final var ac2 = new Aircraft(
                new RegistrationNumber("CS-TPJ", "Portugal"),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
        final var dto1 = AircraftDTO.from(ac1);
        final var dto2 = AircraftDTO.from(ac2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toStringContainsRegistration() {
        final var dto = AircraftDTO.from(sampleAircraft());
        final var str = dto.toString();
        assertTrue(str.contains("CS-TUI"));
    }

    @Test
    void recordIsImmutable() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertAll(
                () -> assertNotNull(dto.registrationNumber()),
                () -> assertNotNull(dto.aircraftModelCode()),
                () -> assertNotNull(dto.operationalStatus()));
    }
}
```

---

## 5. Parameterized Tests (CSV-Driven)

### 5.1 FleetListingParameterizedTest (10 tests)

Driven by `fleet_listing_test_data.csv` (rows FL01–FL10). Each row constructs an
`Aircraft` with the specified fields, converts via `AircraftDTO.from()`, and
verifies all four DTO fields match.

| Test Case | Registration | Model | Status | Capacity | Description |
|-----------|-------------|-------|--------|----------|-------------|
| FL01 | CS-TUI | A320 | ACTIVE | 180 | Single-class narrow-body |
| FL02 | CS-TPJ | A320 | ACTIVE | 180 | Single-class narrow-body variant |
| FL03 | CS-TPW | B738 | ACTIVE | 189 | 737-800 single-class |
| FL04 | CS-TTA | E190 | ACTIVE | 114 | E190 regional jet |
| FL05 | CS-TVA | B738 | ACTIVE | 160 | B738 with business class |
| FL06 | CS-TVB | A330 | ACTIVE | 280 | A330 wide-body |
| FL07 | CS-TVC | A330 | ACTIVE | 320 | A330 higher-density |
| FL08 | CS-TVD | E190 | ACTIVE | 110 | E190 lower-density |
| FL09 | CS-GLB | G650 | ACTIVE | 19 | Gulfstream G650 |
| FL10 | CS-TUI | A320 | DECOMMISSIONED | 180 | Decommissioned status |

```java
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
            new CabinConfiguration(List.of(new SeatClass("Economy", totalCapacity))),
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
```

### 5.2 FlightOperationParameterizedTest (12 tests)

Driven by `flight_operation_test_data.csv` (rows FO01–FO12). Each row tests one
operation (`CREATE_FLIGHT_PLAN`, `IMPORT_FLIGHT_PLAN`, or `VALIDATE_FLIGHT_PLAN`)
with specific input parameters, verifying delegation and expected outcome.

| Test Case | Operation | Flight ID | DSL | Scenario | Outcome |
|-----------|-----------|-----------|-----|----------|---------|
| FO01 | CREATE_FLIGHT_PLAN | FL001 | `departure LIS 10:00; arrival OPO 11:00;` | Valid minimal DSL | SUCCESS |
| FO02 | CREATE_FLIGHT_PLAN | FL002 | Full valid DSL | Valid full DSL | SUCCESS |
| FO03 | CREATE_FLIGHT_PLAN | FL003 | *(empty)* | Empty DSL content | FAILURE |
| FO04 | IMPORT_FLIGHT_PLAN | FP001 | `departure OPO 10:00; arrival LIS 11:00;` | Valid import DSL | SUCCESS |
| FO05 | IMPORT_FLIGHT_PLAN | FP002 | Full import DSL | Valid import full DSL | SUCCESS |
| FO06 | IMPORT_FLIGHT_PLAN | FP003 | *(empty)* | Empty import DSL | FAILURE |
| FO07 | VALIDATE_FLIGHT_PLAN | FP001 | — | Valid flight plan ID | SUCCESS |
| FO08 | VALIDATE_FLIGHT_PLAN | FP002 | — | Valid flight plan ID | SUCCESS |
| FO09 | VALIDATE_FLIGHT_PLAN | UNKNOWN | — | Unknown flight plan ID | FAILURE |
| FO10 | CREATE_FLIGHT_PLAN | FL004 | `invalid DSL without departure` | Minimal invalid DSL | FAILURE |
| FO11 | IMPORT_FLIGHT_PLAN | FP004 | `invalid DSL without departure` | Invalid import DSL | FAILURE |
| FO12 | VALIDATE_FLIGHT_PLAN | INVALID | — | Invalid flight plan ID format | FAILURE |

### 5.3 ReportOperationParameterizedTest (8 tests)

Driven by `report_operation_test_data.csv` (rows RO01–RO08). Each row tests
`GENERATE_REPORT` or `MONTHLY_REPORT` with specific parameters.

| Test Case | Operation | Param1 | Param2 | Scenario | Outcome |
|-----------|-----------|--------|--------|----------|---------|
| RO01 | GENERATE_REPORT | LPPC | — | Known area code | SUCCESS |
| RO02 | GENERATE_REPORT | LISB | — | Valid area code | SUCCESS |
| RO03 | GENERATE_REPORT | UNKN | — | Unknown area code | FAILURE |
| RO04 | GENERATE_REPORT | *(null)* | — | Null area code | FAILURE |
| RO05 | MONTHLY_REPORT | 2026 | 5 | Valid year and month | SUCCESS |
| RO06 | MONTHLY_REPORT | 2026 | 1 | Valid year January | SUCCESS |
| RO07 | MONTHLY_REPORT | 1899 | 5 | Year out of range | FAILURE |
| RO08 | MONTHLY_REPORT | 2026 | 13 | Month out of range | FAILURE |

---

## 6. Coverage & Results

### 6.1 Test Summary

| Test Class | Tests | Description |
|-----------|------:|-------------|
| `RemotePilotServiceTest` — constructor | 1 | IR03: no-arg constructor does not throw |
| `RemotePilotServiceTest` — listFleet | 3 | IR01 delegation, IR05 DTO return, empty fleet |
| `RemotePilotServiceTest` — createFlightPlan | 2 | IR01 delegation, return type |
| `RemotePilotServiceTest` — importFlightPlan | 1 | IR01 delegation |
| `RemotePilotServiceTest` — validateFlightPlan | 2 | IR01 delegation, return type |
| `RemotePilotServiceTest` — generateReport | 2 | IR01 delegation, return type |
| `RemotePilotServiceTest` — monthlyReport | 2 | IR01 delegation, return type |
| `RemotePilotServiceTest` — listFlights | 2 | IR01 delegation, non-null list |
| `AircraftDTOTest` | 11 | Field mapping (5), equality (2), toString (1), immutability (1), decommissioned status (1), multi-class capacity (1) |
| `FleetListingParameterizedTest` (CSV) | 10 | IR05 — DTO field mapping across 10 aircraft variants |
| `FlightOperationParameterizedTest` (CSV) | 12 | IR01, IR04 — 3 operations × SUCCESS/FAILURE |
| `ReportOperationParameterizedTest` (CSV) | 8 | IR01, IR04 — 2 operations × SUCCESS/FAILURE |
| **Total** | **56** | |

### 6.2 CSV Test Data Summary

| CSV File | Scenarios | Covered Operations |
|----------|-----------|-------------------|
| `fleet_listing_test_data.csv` | 10 (FL01–FL10) | `listFleet` DTO conversion |
| `flight_operation_test_data.csv` | 12 (FO01–FO12) | `createFlightPlan`, `validateFlightPlan`, `importFlightPlan` |
| `report_operation_test_data.csv` | 8 (RO01–RO08) | `generateReport`, `monthlyReport` |
| **Total** | **30** | All 7 unit-testable RemotePilotService operations |

### 6.3 Invariant Coverage

| Invariant | Covered By |
|-----------|-----------|
| IR01 — correct delegation | 7 delegation tests + 22 CSV scenarios |
| IR02 — no business logic in facade | Verified by design (no business logic in service) |
| IR03 — construction does not throw | 1 constructor test |
| IR04 — exception propagation | 7 CSV FAILURE scenarios |
| IR05 — DTOs not entities | 3 unit tests + 10 CSV scenarios (DTO field mapping × 10 aircraft variants) |
| IR06 — no UI dependency | Verified by design (no UI imports in service or tests) |
