package eapli.aisafe.flightplan.domain;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    private FlightPlan validFlightPlan() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        return new FlightPlan(flight, FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
    }

    @Test
    void ensureNewFlightPlanIsDraft() {
        final var fp = validFlightPlan();
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
    }

    @Test
    void ensureMarkAsInTestChangesStatus() {
        final var fp = validFlightPlan();
        fp.markAsInTest();
        assertEquals(FlightPlanStatus.IN_TEST, fp.status());
    }

    @Test
    void ensureMarkAsInTestFromDraftOnly() {
        final var fp = validFlightPlan();
        fp.markAsInTest();
        assertThrows(IllegalStateException.class, fp::markAsInTest);
    }

    @Test
    void ensureMarkAsTestPassedFromInTestOnly() {
        final var fp = validFlightPlan();
        assertThrows(IllegalStateException.class, fp::markAsTestPassed);
        fp.markAsInTest();
        fp.markAsTestPassed();
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
    }

    @Test
    void ensureMarkAsTestFailedFromInTestOnly() {
        final var fp = validFlightPlan();
        assertThrows(IllegalStateException.class, fp::markAsTestFailed);
        fp.markAsInTest();
        fp.markAsTestFailed();
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());
    }

    @Test
    void ensureResetToDraft() {
        final var fp = validFlightPlan();
        fp.markAsInTest();
        fp.markAsTestPassed();
        fp.resetToDraft();
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
        assertNull(fp.reportFilePath());
        assertNull(fp.reportContent());
    }

    @Test
    void ensureNullFlightPlanIdIsRejected() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(flight, null, "dsl content"));
    }

    @Test
    void ensureNullDslContentIsRejected() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(flight, FlightPlanId.valueOf("FP001"), null));
    }

    @Test
    void ensureBlankDslContentIsRejected() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        assertThrows(IllegalStateException.class,
                () -> new FlightPlan(flight, FlightPlanId.valueOf("FP001"), "   "));
    }

    @Test
    void ensureRecordTestResultStoresReport() {
        final var fp = validFlightPlan();
        fp.markAsInTest();
        fp.recordTestResult(true, "/tmp/report.txt", "RESULT: PASS");
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
        assertEquals("/tmp/report.txt", fp.reportFilePath());
        assertEquals("RESULT: PASS", fp.reportContent());
        assertNotNull(fp.lastTestedAt());
    }

    @Test
    void ensureRecordTestResultFailed() {
        final var fp = validFlightPlan();
        fp.markAsInTest();
        fp.recordTestResult(false, "/tmp/report.txt", "RESULT: FAIL");
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());
    }

    @Test
    void ensureUpdateDslContentResetsToDraft() {
        final var fp = validFlightPlan();
        fp.markAsInTest();
        fp.markAsTestPassed();
        fp.updateDslContent("new dsl content");
        assertEquals(FlightPlanStatus.DRAFT, fp.status());
        assertEquals("new dsl content", fp.dslContent());
    }

    @Test
    void ensureSameAsWorks() {
        final var fp1 = validFlightPlan();
        final var flight2 = new Flight(FlightDesignator.valueOf("TP5678"), DEP_TIME);
        final var fp2 = new FlightPlan(flight2, FlightPlanId.valueOf("FP001"), "other dsl");
        assertTrue(fp1.identity().equals(fp2.identity()));
    }

    @Test
    void ensureIdentity() {
        final var fp = validFlightPlan();
        assertEquals(FlightPlanId.valueOf("FP001"), fp.identity());
    }
}
