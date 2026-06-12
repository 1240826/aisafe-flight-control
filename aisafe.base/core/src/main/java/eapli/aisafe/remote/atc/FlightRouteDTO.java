package eapli.aisafe.remote.atc;

import eapli.aisafe.flightroute.domain.FlightRoute;

public record FlightRouteDTO(
        String routeName,
        String companyIata,
        String originIata,
        String destinationIata,
        boolean active,
        String deactivationDate
) {
    public static FlightRouteDTO from(final FlightRoute r) {
        return new FlightRouteDTO(
                r.routeName().toString(),
                r.companyIATA().toString(),
                r.origin().toString(),
                r.destination().toString(),
                r.isActive(),
                r.deactivationDate() != null ? r.deactivationDate().toString() : ""
        );
    }
}