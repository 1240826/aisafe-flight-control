package eapli.aisafe.airport.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Airport aggregate root.
 * Covers US052 invariants: IATA format, ICAO format, name/city/country non-blank,
 * elevation positive, area code non-null.
 */
class AirportTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static Elevation elevation69m() {
        return new Elevation(69.0, "m");
    }

    private static Airport validAirport() {
        return new Airport(
                new AirportIATA("OPO"),
                new AirportICAO("LPPR"),
                "Francisco Sa Carneiro Airport",
                "Porto",
                "Portugal",
                41.2481, -8.6814,
                elevation69m(),
                new AreaCode("LPPC")
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidAirportCanBeCreated() {
        final var airport = validAirport();
        assertNotNull(airport);
        assertEquals("OPO", airport.iata().toString());
        assertEquals("LPPR", airport.icao().toString());
        assertEquals("Francisco Sa Carneiro Airport", airport.name());
        assertEquals("Porto", airport.city());
        assertEquals("Portugal", airport.country());
        assertEquals(69.0, airport.elevation().value(), 0.001);
        assertEquals("m", airport.elevation().unit());
    }

    @Test
    void ensureIdentityReturnsIATA() {
        final var airport = validAirport();
        assertEquals(new AirportIATA("OPO"), airport.identity());
    }

    @Test
    void ensureAreaCodeIsPreserved() {
        final var airport = validAirport();
        assertEquals(new AreaCode("LPPC"), airport.areaCode());
    }

    @Test
    void ensureCoordinatesArePreserved() {
        final var airport = validAirport();
        assertEquals(41.2481, airport.latitude(), 0.0001);
        assertEquals(-8.6814, airport.longitude(), 0.0001);
    }

    // ── IATA code invariants ──────────────────────────────────────────────────

    @Test
    void ensureLowercaseIATACodeIsNormalisedToUppercase() {
        // The VO normalises lowercase to uppercase — it does NOT reject it
        final var iata = new AirportIATA("opo");
        assertEquals("OPO", iata.toString());
    }

    @Test
    void ensureIATACodeWithDigitsIsRejected() {
        assertThrows(Exception.class, () -> new AirportIATA("OP1"),
                "IATA code with digits must be rejected (only A-Z allowed)");
    }

    @Test
    void ensureIATACodeMustNotHaveFewerThanThreeLetters() {
        assertThrows(Exception.class, () -> new AirportIATA("OP"),
                "Two-letter IATA code must be rejected");
    }

    @Test
    void ensureIATACodeMustNotHaveMoreThanThreeLetters() {
        assertThrows(Exception.class, () -> new AirportIATA("OPOO"),
                "Four-letter IATA code must be rejected");
    }

    @Test
    void ensureNullIATACodeIsRejected() {
        assertThrows(Exception.class, () -> new AirportIATA(null),
                "Null IATA code must be rejected");
    }

    // ── ICAO code invariants ──────────────────────────────────────────────────

    @Test
    void ensureICAOCodeMustBeFourUppercaseLetters() {
        assertThrows(Exception.class, () -> new AirportICAO("LPP"),
                "Three-letter ICAO code must be rejected");
    }

    @Test
    void ensureICAOCodeMustNotHaveMoreThanFourLetters() {
        assertThrows(Exception.class, () -> new AirportICAO("LPPRO"),
                "Five-letter ICAO code must be rejected");
    }

    @Test
    void ensureNullICAOCodeIsRejected() {
        assertThrows(Exception.class, () -> new AirportICAO(null),
                "Null ICAO code must be rejected");
    }

    // ── Name / city / country invariants ──────────────────────────────────────

    @Test
    void ensureBlankNameIsRejected() {
        assertThrows(Exception.class, () -> new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "", "Porto", "Portugal",
                41.2481, -8.6814, elevation69m(), new AreaCode("LPPC")));
    }

    @Test
    void ensureBlankCityIsRejected() {
        assertThrows(Exception.class, () -> new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "Francisco Sa Carneiro Airport", "", "Portugal",
                41.2481, -8.6814, elevation69m(), new AreaCode("LPPC")));
    }

    @Test
    void ensureBlankCountryIsRejected() {
        assertThrows(Exception.class, () -> new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "Francisco Sa Carneiro Airport", "Porto", "",
                41.2481, -8.6814, elevation69m(), new AreaCode("LPPC")));
    }

    @Test
    void ensureWhitespaceNameIsRejected() {
        assertThrows(Exception.class, () -> new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "   ", "Porto", "Portugal",
                41.2481, -8.6814, elevation69m(), new AreaCode("LPPC")));
    }

    // ── Elevation invariants ──────────────────────────────────────────────────

    @Test
    void ensureNullElevationIsRejected() {
        assertThrows(Exception.class, () -> new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "Francisco Sa Carneiro Airport", "Porto", "Portugal",
                41.2481, -8.6814, null, new AreaCode("LPPC")));
    }

    // ── Area code invariants ──────────────────────────────────────────────────

    @Test
    void ensureNullAreaCodeIsRejected() {
        assertThrows(Exception.class, () -> new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "Francisco Sa Carneiro Airport", "Porto", "Portugal",
                41.2481, -8.6814, elevation69m(), null));
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    void ensureAirportsWithSameIATACodeAreEqual() {
        final var a1 = validAirport();
        final var a2 = new Airport(
                new AirportIATA("OPO"), new AirportICAO("LPPR"),
                "Different Name", "Porto", "Portugal",
                41.0, -9.0, new Elevation(50.0, "m"), new AreaCode("LPPC"));
        assertEquals(a1, a2, "Airports with same IATA should be equal (identity-based)");
    }

    @Test
    void ensureAirportsWithDifferentIATACodeAreNotEqual() {
        final var a1 = validAirport();
        final var a2 = new Airport(
                new AirportIATA("LIS"), new AirportICAO("LPPT"),
                "Humberto Delgado Airport", "Lisbon", "Portugal",
                38.7739, -9.1340, new Elevation(114.0, "m"), new AreaCode("LPPC"));
        assertNotEquals(a1, a2);
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/airport_test.csv", numLinesToSkip = 1)
    void ensureAirportCsvInvariants(final String testCaseId, final String iata,
                                     final String icao, final String name,
                                     final String city, final String country,
                                     final double lat, final double lon,
                                     final String areaCode, final boolean expectedValid) {
        final var iataVO = (iata == null || iata.isBlank()) ? null : new AirportIATA(iata);
        final var icaoVO = (icao == null || icao.isBlank()) ? null : new AirportICAO(icao);
        final var ac = (areaCode == null || areaCode.isBlank()) ? null : new AreaCode(areaCode);
        final var elev = new Elevation(69.0, "m");
        if (expectedValid) {
            assertDoesNotThrow(() -> new Airport(iataVO, icaoVO, name, city, country, lat, lon, elev, ac));
        } else {
            assertThrows(Exception.class, () -> new Airport(iataVO, icaoVO, name, city, country, lat, lon, elev, ac));
        }
    }
}
