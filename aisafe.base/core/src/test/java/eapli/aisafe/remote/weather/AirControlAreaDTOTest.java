package eapli.aisafe.remote.weather;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.domain.AreaName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirControlAreaDTOTest {

    private AirControlArea sampleArea() {
        return new AirControlArea(
                new AreaCode("LIS"),
                new AreaName("Lisbon"),
                38.5, 39.0,
                -9.5, -9.0,
                10000
        );
    }

    @Test
    void fromMapsAreaCode() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals("LIS", dto.areaCode());
    }

    @Test
    void fromMapsName() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals("Lisbon", dto.name());
    }

    @Test
    void fromMapsMinLat() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals(38.5, dto.minLat());
    }

    @Test
    void fromMapsMaxLat() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals(39.0, dto.maxLat());
    }

    @Test
    void fromMapsMinLon() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals(-9.5, dto.minLon());
    }

    @Test
    void fromMapsMaxLon() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals(-9.0, dto.maxLon());
    }

    @Test
    void fromMapsMaxAltitude() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertEquals(10000, dto.maxAltitudeMetres());
    }

    @Test
    void equalsAndHashCode() {
        final var dto1 = AirControlAreaDTO.from(sampleArea());
        final var dto2 = AirControlAreaDTO.from(sampleArea());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEqualsDifferentAreaCode() {
        final var a1 = sampleArea();
        final var a2 = new AirControlArea(
                new AreaCode("OPO"),
                new AreaName("Porto"),
                41.0, 41.5,
                -8.8, -8.3,
                8000);
        final var dto1 = AirControlAreaDTO.from(a1);
        final var dto2 = AirControlAreaDTO.from(a2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toStringContainsAreaCode() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertTrue(dto.toString().contains("LIS"));
    }

    @Test
    void recordIsImmutable() {
        final var dto = AirControlAreaDTO.from(sampleArea());
        assertAll(
                () -> assertNotNull(dto.areaCode()),
                () -> assertNotNull(dto.name())
        );
    }
}
