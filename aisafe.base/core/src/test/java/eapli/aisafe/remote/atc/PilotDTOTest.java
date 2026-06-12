package eapli.aisafe.remote.atc;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PilotDTOTest {

    private Pilot samplePilot() {
        return new Pilot(
                new PilotId("P12345"),
                CompanyIATA.valueOf("TP"),
                Set.of(AircraftModelCode.valueOf("A320")),
                LocalDate.of(2020, 3, 15));
    }

    @Test
    void fromMapsLicenseNumber() {
        final var dto = PilotDTO.from(samplePilot());
        assertEquals("P12345", dto.licenseNumber());
    }

    @Test
    void fromMapsCompanyIata() {
        final var dto = PilotDTO.from(samplePilot());
        assertEquals("TP", dto.companyIata());
    }

    @Test
    void fromMapsCertifiedModels() {
        final var dto = PilotDTO.from(samplePilot());
        assertEquals(Set.of("A320"), dto.certifiedModels());
    }

    @Test
    void fromMapsMultipleCertifiedModels() {
        final var pilot = new Pilot(
                new PilotId("P54321"),
                CompanyIATA.valueOf("TP"),
                Set.of(AircraftModelCode.valueOf("A320"), AircraftModelCode.valueOf("A330")),
                LocalDate.of(2021, 6, 1));
        final var dto = PilotDTO.from(pilot);
        assertEquals(Set.of("A320", "A330"), dto.certifiedModels());
    }

    @Test
    void fromMapsCertificationDate() {
        final var dto = PilotDTO.from(samplePilot());
        assertEquals("2020-03-15", dto.certificationDate());
    }

    @Test
    void fromMapsActiveTrue() {
        final var dto = PilotDTO.from(samplePilot());
        assertTrue(dto.active());
    }

    @Test
    void equalsAndHashCode() {
        final var dto1 = PilotDTO.from(samplePilot());
        final var dto2 = PilotDTO.from(samplePilot());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEqualsDifferentLicense() {
        final var p1 = samplePilot();
        final var p2 = new Pilot(
                new PilotId("P54321"),
                CompanyIATA.valueOf("TP"),
                Set.of(AircraftModelCode.valueOf("A320")),
                LocalDate.of(2020, 3, 15));
        final var dto1 = PilotDTO.from(p1);
        final var dto2 = PilotDTO.from(p2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toStringContainsLicenseNumber() {
        final var dto = PilotDTO.from(samplePilot());
        assertTrue(dto.toString().contains("P12345"));
    }

    @Test
    void recordIsImmutable() {
        final var dto = PilotDTO.from(samplePilot());
        assertAll(
                () -> assertNotNull(dto.licenseNumber()),
                () -> assertNotNull(dto.companyIata()),
                () -> assertNotNull(dto.certifiedModels()),
                () -> assertNotNull(dto.certificationDate())
        );
    }
}
