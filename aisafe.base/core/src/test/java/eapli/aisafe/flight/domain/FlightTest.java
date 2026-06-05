package eapli.aisafe.flight.domain;

import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightplan.domain.FlightPlanStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

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

    // ── US082 – assignWeatherData ─────────────────────────────────────────────

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
}
