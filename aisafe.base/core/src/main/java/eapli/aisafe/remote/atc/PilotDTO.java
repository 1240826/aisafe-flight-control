package eapli.aisafe.remote.atc;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.pilot.domain.Pilot;

import java.util.Set;
import java.util.stream.Collectors;

public record PilotDTO(
        String licenseNumber,
        String companyIata,
        Set<String> certifiedModels,
        String certificationDate,
        boolean active
) {
    public static PilotDTO from(final Pilot p) {
        return new PilotDTO(
                p.pilotId().toString(),
                p.company().toString(),
                p.certifiedModels().stream()
                        .map(AircraftModelCode::toString)
                        .collect(Collectors.toSet()),
                p.certificationDate().toString(),
                p.isActive()
        );
    }
}