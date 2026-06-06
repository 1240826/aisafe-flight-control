package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.domain.Aircraft;

public record AircraftDTO(
        String registrationNumber,
        String aircraftModelCode,
        String operationalStatus,
        int totalCapacity
) {
    public static AircraftDTO from(final Aircraft a) {
        return new AircraftDTO(
                a.registrationNumber().number(),
                a.aircraftModelCode().toString(),
                a.operationalStatus().name(),
                a.totalCapacity()
        );
    }
}
