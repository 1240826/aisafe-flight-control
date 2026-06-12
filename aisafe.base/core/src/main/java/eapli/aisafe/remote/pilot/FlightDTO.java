package eapli.aisafe.remote.pilot;

import eapli.aisafe.flight.domain.Flight;

public record FlightDTO(
        String flightDesignator,
        String departureTime,
        String routeName,
        String aircraftRegistration,
        String pilotLicense,
        String flightType
) {
    public static FlightDTO from(final Flight f) {
        return new FlightDTO(
                f.identity() != null ? f.identity().toString() : "",
                f.departureTime() != null ? f.departureTime().toString() : "",
                f.routeName() != null ? f.routeName().toString() : "",
                f.aircraftRegistration() != null ? f.aircraftRegistration() : "",
                f.pilotLicense() != null ? f.pilotLicense().toString() : "",
                f.flightType() != null ? f.flightType().name() : ""
        );
    }
}