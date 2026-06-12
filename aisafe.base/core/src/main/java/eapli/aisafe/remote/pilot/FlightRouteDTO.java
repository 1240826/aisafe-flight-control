package eapli.aisafe.remote.pilot;

import eapli.aisafe.flightroute.domain.FlightRoute;

public record FlightRouteDTO(
        String routeName,
        String origin,
        String destination,
        boolean active
) {
    public static FlightRouteDTO from(final FlightRoute r) {
        return new FlightRouteDTO(
                r.routeName().toString(),
                r.origin().toString(),
                r.destination().toString(),
                r.isActive()
        );
    }
}