package eapli.aisafe.remote.atc;

import eapli.aisafe.aircraft.domain.Aircraft;

public record AircraftDTO(
        String registrationNumber,
        String registrationCountry,
        String aircraftModelCode,
        String companyIata,
        int crewMembers,
        int totalCapacity,
        String operationalStatus,
        String registrationDate,
        int ageInYears
) {
    public static AircraftDTO from(final Aircraft a) {
        return new AircraftDTO(
                a.registrationNumber().number(),
                a.registrationNumber().registrationCountry(),
                a.aircraftModelCode().toString(),
                a.companyId().toString(),
                a.numberOfFlightCrewMembers(),
                a.totalCapacity(),
                a.operationalStatus().name(),
                a.registrationDate().toString(),
                a.ageInYears()
        );
    }
}