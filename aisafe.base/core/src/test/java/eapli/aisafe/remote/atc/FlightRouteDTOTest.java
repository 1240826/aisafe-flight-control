package eapli.aisafe.remote.atc;

import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightRouteDTOTest {

    private FlightRoute sampleRoute() {
        return new FlightRoute(
                new FlightRouteName("TP123"),
                CompanyIATA.valueOf("TP"),
                new AirportIATA("LIS"),
                new AirportIATA("OPO"));
    }

    @Test
    void fromMapsRouteName() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertEquals("TP123", dto.routeName());
    }

    @Test
    void fromMapsCompanyIata() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertEquals("TP", dto.companyIata());
    }

    @Test
    void fromMapsOriginIata() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertEquals("LIS", dto.originIata());
    }

    @Test
    void fromMapsDestinationIata() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertEquals("OPO", dto.destinationIata());
    }

    @Test
    void fromMapsActiveTrue() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertTrue(dto.active());
    }

    @Test
    void fromMapsDeactivationDateEmptyWhenNull() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertEquals("", dto.deactivationDate());
    }

    @Test
    void equalsAndHashCode() {
        final var dto1 = FlightRouteDTO.from(sampleRoute());
        final var dto2 = FlightRouteDTO.from(sampleRoute());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEqualsDifferentRoute() {
        final var r1 = sampleRoute();
        final var r2 = new FlightRoute(
                new FlightRouteName("FR456"),
                CompanyIATA.valueOf("FR"),
                new AirportIATA("FAO"),
                new AirportIATA("LIS"));
        final var dto1 = FlightRouteDTO.from(r1);
        final var dto2 = FlightRouteDTO.from(r2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toStringContainsRouteName() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertTrue(dto.toString().contains("TP123"));
    }

    @Test
    void recordIsImmutable() {
        final var dto = FlightRouteDTO.from(sampleRoute());
        assertAll(
                () -> assertNotNull(dto.routeName()),
                () -> assertNotNull(dto.companyIata()),
                () -> assertNotNull(dto.originIata()),
                () -> assertNotNull(dto.destinationIata())
        );
    }
}
