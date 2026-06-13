package eapli.aisafe.flight.domain;

import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightplan.domain.FlightPlanStatus;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.pilot.domain.PilotId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us082/flight_test.csv", numLinesToSkip = 1)
    void ensureFlightDesignatorInvariants(final String testCaseId, final String designator, final boolean expectValid) {
        if (expectValid) {
            final var fd = FlightDesignator.valueOf(designator);
            assertEquals(designator.toUpperCase().trim(), fd.toString());
        } else {
            if (designator == null || designator.isBlank()) {
                assertThrows(Exception.class, () -> FlightDesignator.valueOf(""));
            } else {
                assertThrows(Exception.class, () -> FlightDesignator.valueOf(designator));
            }
        }
    }

    @Test
    void ensureFlightIsCreatedWithDesignator() {
        final var d = FlightDesignator.valueOf("TP1234");
        final var flight = new Flight(d, DEP_TIME);
        assertEquals(d, flight.identity());
        assertEquals(DEP_TIME, flight.departureTime());
    }

    @Test
    void ensureFlightStartsWithNoPlans() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        assertTrue(flight.flightPlans().isEmpty());
    }

    @Test
    void ensureAddFlightPlanWorks() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS;");
        assertNotNull(fp);
        assertEquals(FlightPlanId.valueOf("FP001"), fp.identity());
        assertEquals(1, flight.flightPlans().size());
    }

    @Test
    void ensureFlightPlanLookupWorks() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS;");
        flight.addFlightPlan(FlightPlanId.valueOf("FP002"), "departure OPO;");
        final var found = flight.flightPlan(FlightPlanId.valueOf("FP001"));
        assertTrue(found.isPresent());
        assertEquals("FP001", found.get().identity().toString());
    }

    @Test
    void ensureFlightPlanLookupReturnsEmptyForUnknown() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS;");
        assertTrue(flight.flightPlan(FlightPlanId.valueOf("UNKNOWN")).isEmpty());
    }

    @Test
    void ensureMultiplePlansCanBeAdded() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl1");
        flight.addFlightPlan(FlightPlanId.valueOf("FP002"), "dsl2");
        flight.addFlightPlan(FlightPlanId.valueOf("FP003"), "dsl3");
        assertEquals(3, flight.flightPlans().size());
    }

    @Test
    void ensureDuplicateFlightPlanThrows() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl1");
        assertThrows(IllegalArgumentException.class,
                () -> flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl2"));
    }

    @Test
    void ensureFlightPlanListIsUnmodifiable() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl");
        assertThrows(UnsupportedOperationException.class,
                () -> flight.flightPlans().clear());
    }

    @Test
    void ensureSameAsWithSameIdentity() {
        final var f1 = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var f2 = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        assertTrue(f1.sameAs(f2));
    }

    @Test
    void ensureSameAsWithDifferentIdentity() {
        final var f1 = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var f2 = new Flight(FlightDesignator.valueOf("TP5678"), DEP_TIME);
        assertFalse(f1.sameAs(f2));
    }

    @Test
    void ensureAssignWeatherDataSetsId() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.assignWeatherData(42L);
        assertEquals(42L, flight.weatherDataId());
    }

    @Test
    void ensureAssignWeatherDataIsIdempotentWhenSameId() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.assignWeatherData(42L);
        flight.assignWeatherData(42L);
        assertEquals(42L, flight.weatherDataId());
    }

    @Test
    void ensureAssignWeatherDataOverwritesPreviousId() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.assignWeatherData(42L);
        flight.assignWeatherData(99L);
        assertEquals(99L, flight.weatherDataId());
    }

    @Test
    void ensureAssignWeatherDataResetsTestPassedPlansToDraft() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl content");
        fp.markAsInTest();
        fp.markAsTestPassed();
        flight.assignWeatherData(42L);
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
    }

    @Test
    void ensureAssignWeatherDataResetsTestFailedPlansToDraft() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl content");
        fp.markAsInTest();
        fp.markAsTestFailed();
        flight.assignWeatherData(42L);
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
    }

    @Test
    void ensureAssignWeatherDataDoesNotResetDraftPlans() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl content");
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
        flight.assignWeatherData(42L);
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
    }

    @Test
    void ensureAssignWeatherDataDoesNotResetInTestPlans() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl content");
        fp.markAsInTest();
        flight.assignWeatherData(42L);
        assertEquals(FlightPlanStatus.IN_TEST, fp.status());
    }

    @Test
    void ensureAssignWeatherDataNullIdIsAccepted() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.assignWeatherData(42L);
        flight.assignWeatherData(null);
        assertNull(flight.weatherDataId());
    }

    @Test
    void ensureAssignWeatherDataDoesNotResetPlansWhenSameId() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl content");
        fp.markAsInTest();
        fp.markAsTestPassed();
        flight.assignWeatherData(42L);
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
        fp.markAsInTest();
        fp.markAsTestPassed();
        flight.assignWeatherData(42L);
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status(),
                "Re-assigning same weather data must not reset plans again");
    }

    @Test
    void ensureUpdateFlightPlanUpdatesExisting() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "original dsl");
        final var updated = flight.updateFlightPlan(FlightPlanId.valueOf("FP001"), "updated dsl");
        assertEquals("updated dsl", updated.dslContent());
    }

    @Test
    void ensureUpdateFlightPlanAddsIfNotExists() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var added = flight.updateFlightPlan(FlightPlanId.valueOf("FP001"), "new dsl");
        assertNotNull(added);
        assertEquals(1, flight.flightPlans().size());
    }

    @Test
    void ensureUpdateFromDslUpdatesFields() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var newTime = LocalDateTime.of(2026, 7, 4, 15, 30);
        flight.updateFromDsl(newTime, null, "CS-TTT", null);
        assertEquals(newTime, flight.departureTime());
        assertEquals("CS-TTT", flight.aircraftRegistration());
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us074/flight_test.csv", numLinesToSkip = 1)
    void ensureFlightCsvInvariants(final String testCaseId, final String designator,
                                    final String departureDate, final String departureTime,
                                    final String routeName, final String aircraftReg,
                                    final String pilotLicense, final boolean expectedValid) {
        final var fd = (designator == null || designator.isBlank()) ? null : FlightDesignator.valueOf(designator);
        final var dt = LocalDateTime.of(LocalDate.parse(departureDate), LocalTime.parse(departureTime));
        final var rn = (routeName == null || routeName.isBlank()) ? null : FlightRouteName.valueOf(routeName);
        final var pl = (pilotLicense == null || pilotLicense.isBlank()) ? null : PilotId.valueOf(pilotLicense);
        if (expectedValid) {
            assertDoesNotThrow(() -> new Flight(fd, dt, rn, aircraftReg, pl));
        } else {
            assertThrows(Exception.class, () -> new Flight(fd, dt, rn, aircraftReg, pl));
        }
    }
}
