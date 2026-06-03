package eapli.aisafe.flight.domain;

import eapli.aisafe.flightplan.domain.FlightPlanId;
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
}
