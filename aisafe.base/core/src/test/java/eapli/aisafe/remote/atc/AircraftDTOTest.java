package eapli.aisafe.remote.atc;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AircraftDTOTest {

    private Aircraft sampleAircraft() {
        return new Aircraft(
                new RegistrationNumber("CS-TRA", "Portugal"),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
    }

    @Test
    void fromMapsRegistrationNumber() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("CS-TRA", dto.registrationNumber());
    }

    @Test
    void fromMapsRegistrationCountry() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("Portugal", dto.registrationCountry());
    }

    @Test
    void fromMapsAircraftModelCode() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("A320", dto.aircraftModelCode());
    }

    @Test
    void fromMapsCompanyIata() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("TP", dto.companyIata());
    }

    @Test
    void fromMapsCrewMembers() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals(2, dto.crewMembers());
    }

    @Test
    void fromMapsTotalCapacity() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals(180, dto.totalCapacity());
    }

    @Test
    void fromMapsOperationalStatus() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("ACTIVE", dto.operationalStatus());
    }

    @Test
    void fromMapsRegistrationDate() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertEquals("2018-06-15", dto.registrationDate());
    }

    @Test
    void fromMapsAgeInYears() {
        final var ac = sampleAircraft();
        final var dto = AircraftDTO.from(ac);
        assertTrue(dto.ageInYears() >= 7);
    }

    @Test
    void fromMapsDecommissionedStatus() {
        final var ac = sampleAircraft();
        ac.decommission();
        final var dto = AircraftDTO.from(ac);
        assertEquals("DECOMMISSIONED", dto.operationalStatus());
    }

    @Test
    void equalsAndHashCode() {
        final var dto1 = AircraftDTO.from(sampleAircraft());
        final var dto2 = AircraftDTO.from(sampleAircraft());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEqualsDifferentRegistration() {
        final var ac1 = sampleAircraft();
        final var ac2 = new Aircraft(
                new RegistrationNumber("CS-TPJ", "Portugal"),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
        final var dto1 = AircraftDTO.from(ac1);
        final var dto2 = AircraftDTO.from(ac2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toStringContainsRegistration() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertTrue(dto.toString().contains("CS-TRA"));
    }

    @Test
    void recordIsImmutable() {
        final var dto = AircraftDTO.from(sampleAircraft());
        assertAll(
                () -> assertNotNull(dto.registrationNumber()),
                () -> assertNotNull(dto.aircraftModelCode()),
                () -> assertNotNull(dto.operationalStatus())
        );
    }
}
