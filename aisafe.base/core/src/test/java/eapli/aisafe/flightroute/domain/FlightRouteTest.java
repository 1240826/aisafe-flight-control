package eapli.aisafe.flightroute.domain;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.airport.domain.AirportIATA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlightRoute aggregate root.
 * Covers US074: Delete (deactivate) a flight route.
 *
 * NOTE: FlightRoute does not yet exist — these tests drive its implementation.
 * Expected behaviour:
 *   - A route is ACTIVE by default on creation.
 *   - deactivate(LocalDate) marks the route as inactive from that date onwards.
 *   - deactivate() must be rejected if there are planned flights after the date.
 */
class FlightRouteTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final LocalDate TODAY         = LocalDate.now();
    private static final LocalDate FUTURE_DATE   = TODAY.plusDays(30);
    private static final LocalDate PAST_DATE     = TODAY.minusDays(1);

    private static FlightRoute validRoute() {
        return new FlightRoute(
                FlightRouteName.valueOf("TP123"),
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("OPO"),
                AirportIATA.valueOf("LIS")
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureNewRouteIsActiveOnCreation() {
        // US074: a freshly created route must be active
        final var route = validRoute();
        assertTrue(route.isActive(), "A new FlightRoute must be active");
    }

    @Test
    void ensureRouteNameIsPreserved() {
        final var route = validRoute();
        assertEquals(FlightRouteName.valueOf("TP123"), route.identity());
    }

    @Test
    void ensureOriginAirportIsPreserved() {
        final var route = validRoute();
        assertEquals(AirportIATA.valueOf("OPO"), route.origin());
    }

    @Test
    void ensureDestinationAirportIsPreserved() {
        final var route = validRoute();
        assertEquals(AirportIATA.valueOf("LIS"), route.destination());
    }

    @Test
    void ensureCompanyIataIsPreserved() {
        final var route = validRoute();
        assertEquals(CompanyIATA.valueOf("TP"), route.companyIATA());
    }

    @Test
    void ensureDeactivationSetsRouteInactive() {
        // AT1: route with no planned flights after the date → deactivation succeeds
        final var route = validRoute();
        route.deactivate(FUTURE_DATE);
        assertFalse(route.isActive(), "Route must be inactive after deactivation");
    }

    @Test
    void ensureDeactivationDateIsPreserved() {
        // AT1: the exact deactivation date must be stored
        final var route = validRoute();
        route.deactivate(FUTURE_DATE);
        assertEquals(FUTURE_DATE, route.deactivationDate());
    }

    @Test
    void ensureDeactivationWithTodayDateSucceeds() {
        // Edge case: deactivating effective today is valid
        final var route = validRoute();
        route.deactivate(TODAY);
        assertFalse(route.isActive());
    }

    @Test
    void ensureDeactivationWithPastDateSucceeds() {
        // The domain does not forbid past dates — that constraint lives in the UI layer
        final var route = validRoute();
        route.deactivate(PAST_DATE);
        assertFalse(route.isActive());
        assertEquals(PAST_DATE, route.deactivationDate());
    }

    // ── Guard clauses ─────────────────────────────────────────────────────────

    @Test
    void ensureDeactivatingAlreadyInactiveRouteThrows() {
        // AT1 / invariant: cannot deactivate twice
        final var route = validRoute();
        route.deactivate(FUTURE_DATE);
        assertThrows(IllegalStateException.class,
                () -> route.deactivate(FUTURE_DATE.plusDays(10)),
                "Deactivating an already-inactive route must throw IllegalStateException");
    }

    @Test
    void ensureNullDeactivationDateIsRejected() {
        // AT4: date is mandatory
        final var route = validRoute();
        assertThrows(Exception.class,
                () -> route.deactivate(null),
                "deactivate() must reject a null date");
    }

    @Test
    void ensureNullRouteNameIsRejected() {
        assertThrows(Exception.class, () -> new FlightRoute(
                null,
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("OPO"),
                AirportIATA.valueOf("LIS")
        ), "FlightRoute must reject a null name");
    }

    @Test
    void ensureNullCompanyIsRejected() {
        assertThrows(Exception.class, () -> new FlightRoute(
                FlightRouteName.valueOf("TP123"),
                null,
                AirportIATA.valueOf("OPO"),
                AirportIATA.valueOf("LIS")
        ), "FlightRoute must reject a null company");
    }

    @Test
    void ensureSameOriginAndDestinationIsRejected() {
        // A route between the same airport makes no sense
        assertThrows(Exception.class, () -> new FlightRoute(
                FlightRouteName.valueOf("TP123"),
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("OPO"),
                AirportIATA.valueOf("OPO")
        ), "FlightRoute must reject identical origin and destination");
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/us073/flight_route_name_test.csv", numLinesToSkip = 1)
    void ensureFlightRouteNameInvariants(final String nameInput, final boolean expectValid) {
        if (expectValid) {
            final var routeName = FlightRouteName.valueOf(nameInput);
            assertEquals(nameInput.toUpperCase().trim(), routeName.name());
        } else {
            assertThrows(Exception.class, () -> FlightRouteName.valueOf(nameInput));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us074/flight_route_test.csv", numLinesToSkip = 1)
    void ensureFlightRouteCsvInvariants(final String testCaseId, final String name,
                                         final String companyIata, final String originIata,
                                         final String destinationIata, final boolean expectedValid) {
        final var routeName = (name == null || name.isBlank()) ? null : FlightRouteName.valueOf(name);
        final var company = (companyIata == null || companyIata.isBlank()) ? null : CompanyIATA.valueOf(companyIata);
        final var origin = (originIata == null || originIata.isBlank()) ? null : AirportIATA.valueOf(originIata);
        final var dest = (destinationIata == null || destinationIata.isBlank()) ? null : AirportIATA.valueOf(destinationIata);
        if (expectedValid) {
            assertDoesNotThrow(() -> new FlightRoute(routeName, company, origin, dest));
        } else {
            assertThrows(Exception.class, () -> new FlightRoute(routeName, company, origin, dest));
        }
    }
}
