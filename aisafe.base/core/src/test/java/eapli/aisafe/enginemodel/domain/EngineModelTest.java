package eapli.aisafe.enginemodel.domain;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineModelTest {

    private EngineModelCode code() { return EngineModelCode.valueOf("CFM56-7B"); }
    private EngineName name() { return new EngineName("CFM International CFM56-7B"); }
    private Power power() { return new Power(27300.0, "hp"); }
    private Thrust staticThrust() { return new Thrust(120.0, "kN", "static"); }
    private Thrust cruiseThrust() { return new Thrust(26.0, "kN", "cruise"); }
    private TSFC tsfc() { return new TSFC(0.55, "lb/(lbf.h)"); }

    @Test
    void ensureValidEngineModelCanBeCreated() {
        final var em = new EngineModel(code(), name(), "CFM International",
                "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc());
        assertEquals(code(), em.identity());
        assertEquals(name(), em.engineName());
        assertEquals("CFM International", em.manufacturerName());
        assertEquals("Jet-A1", em.fuelType());
        assertEquals(MotorizationType.TURBOFAN, em.motorizationType());
        assertEquals(power(), em.power());
        assertEquals(staticThrust(), em.staticThrust());
        assertEquals(cruiseThrust(), em.cruiseThrust());
        assertEquals(tsfc(), em.tsfc());
    }

    @Test
    void ensureNullCodeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(null, name(),
                "CFM", "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureNullEngineNameIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), null,
                "CFM", "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureBlankManufacturerNameIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "", "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "   ", "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureNullFuelTypeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", null, MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureInvalidFuelTypeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", "Diesel", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureNullMotorizationTypeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", "Jet-A1", null,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureNullPowerIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", "Jet-A1", MotorizationType.TURBOFAN,
                null, staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureNullStaticThrustIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", "Jet-A1", MotorizationType.TURBOFAN,
                power(), null, cruiseThrust(), tsfc()));
    }

    @Test
    void ensureNullCruiseThrustIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), null, tsfc()));
    }

    @Test
    void ensureNullTsfcIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(code(), name(),
                "CFM", "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), null));
    }

    @Test
    void ensureEngineModelsWithSameCodeAreEqual() {
        final var e1 = new EngineModel(code(), name(), "CFM",
                "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc());
        final var e2 = new EngineModel(code(), new EngineName("Different"),
                "Other", "Jet-A1", MotorizationType.TURBOPROP,
                new Power(100.0, "kW"), cruiseThrust(), cruiseThrust(), new TSFC(1.0, "g/kN/s"));
        assertEquals(e1, e2, "Equality based on code identity only");
    }

    @Test
    void ensureToStringContainsCodeAndName() {
        final var em = new EngineModel(code(), name(), "CFM",
                "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc());
        final var s = em.toString();
        assertTrue(s.contains("CFM56-7B"));
        assertTrue(s.contains("CFM International CFM56-7B"));
    }

    @Test
    void ensureManufacturerNameIsTrimmed() {
        final var em = new EngineModel(code(), name(), "  CFM  ",
                "Jet-A1", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc());
        assertEquals("CFM", em.manufacturerName());
    }

    @Test
    void ensureFuelTypeIsTrimmed() {
        final var em = new EngineModel(code(), name(), "CFM",
                "  Jet-A1  ", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc());
        assertEquals("Jet-A1", em.fuelType());
    }

    @Test
    void ensureSafFuelTypeIsAccepted() {
        assertDoesNotThrow(() -> new EngineModel(code(), name(), "CFM",
                "SAF", MotorizationType.TURBOFAN,
                power(), staticThrust(), cruiseThrust(), tsfc()));
    }

    @Test
    void ensureAvGasFuelTypeIsAccepted() {
        assertDoesNotThrow(() -> new EngineModel(EngineModelCode.valueOf("L550"), name(),
                "Lycoming", "AvGas 100LL", MotorizationType.TURBOPROP,
                new Power(300.0, "hp"), cruiseThrust(), cruiseThrust(),
                new TSFC(0.4, "lb/(lbf.h)")));
    }
}
