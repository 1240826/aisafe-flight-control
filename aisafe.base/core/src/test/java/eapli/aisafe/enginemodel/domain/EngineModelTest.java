package eapli.aisafe.enginemodel.domain;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EngineModel aggregate root.
 * Covers US056 invariants: code, name, manufacturer, fuel type, power, thrust, TSFC.
 */
class EngineModelTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static EngineModel validEngineModel() {
        return new EngineModel(
                new EngineModelCode("CFM56-7B"),
                new EngineName("CFM56"),
                "CFM International",
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidEngineModelCanBeCreated() {
        final var em = validEngineModel();
        assertNotNull(em);
        assertEquals("CFM56-7B", em.code().toString());
        assertEquals("CFM56", em.engineName().toString());
        assertEquals("CFM International", em.manufacturerName());
        assertEquals("Jet-A1", em.fuelType());
        assertEquals(MotorizationType.TURBOFAN, em.motorizationType());
    }

    @Test
    void ensureIdentityReturnsEngineModelCode() {
        final var em = validEngineModel();
        assertEquals(new EngineModelCode("CFM56-7B"), em.identity());
    }

    @Test
    void ensureThrustValuesArePreserved() {
        final var em = validEngineModel();
        assertEquals(27000.0, em.staticThrust().value(), 0.001);
        assertEquals(5000.0, em.cruiseThrust().value(), 0.001);
    }

    @Test
    void ensurePowerIsPreserved() {
        final var em = validEngineModel();
        assertEquals(27000.0, em.power().value(), 0.001);
    }

    @Test
    void ensureTSFCIsPreserved() {
        final var em = validEngineModel();
        assertEquals(0.37, em.tsfc().value(), 0.001);
    }

    // ── Invariant violations — manufacturer ───────────────────────────────────

    @Test
    void ensureBlankManufacturerNameIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(
                new EngineModelCode("CFM56-7B"),
                new EngineName("CFM56"),
                "",
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")));
    }

    @Test
    void ensureNullManufacturerNameIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(
                new EngineModelCode("CFM56-7B"),
                new EngineName("CFM56"),
                null,
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")));
    }

    // ── Invariant violations — engine model ───────────────────────────────────

    @Test
    void ensureBlankFuelTypeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(
                new EngineModelCode("CFM56-7B"),
                new EngineName("CFM56"),
                "CFM International",
                "",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")));
    }

    @Test
    void ensureNullEngineModelCodeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(
                null,
                new EngineName("CFM56"),
                "CFM International",
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")));
    }

    @Test
    void ensureNullEngineNameIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(
                new EngineModelCode("CFM56-7B"),
                null,
                "CFM International",
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")));
    }

    @Test
    void ensureNullMotorizationTypeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModel(
                new EngineModelCode("CFM56-7B"),
                new EngineName("CFM56"),
                "CFM International",
                "Jet-A1",
                null,
                new Power(27000.0, "kW"),
                new Thrust(27000.0, "kN", "static"),
                new Thrust(5000.0,  "kN", "cruise"),
                new TSFC(0.37, "lb/(lbf.h)")));
    }

    // ── EngineModelCode VO ────────────────────────────────────────────────────

    @Test
    void ensureBlankEngineModelCodeIsRejected() {
        assertThrows(Exception.class, () -> new EngineModelCode(""));
    }

    @Test
    void ensureNullEngineModelCodeStringIsRejected() {
        assertThrows(Exception.class, () -> new EngineModelCode(null));
    }

    // ── EngineName VO ─────────────────────────────────────────────────────────

    @Test
    void ensureBlankEngineNameIsRejected() {
        assertThrows(Exception.class, () -> new EngineName(""));
    }

    @Test
    void ensureNullEngineNameStringIsRejected() {
        assertThrows(Exception.class, () -> new EngineName(null));
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    void ensureEngineModelsWithSameCodeAreEqual() {
        final var em1 = validEngineModel();
        final var em2 = new EngineModel(
                new EngineModelCode("CFM56-7B"),
                new EngineName("CFM56-7B Variant"),
                "CFM International",
                "Jet-A1",
                MotorizationType.TURBOFAN,
                new Power(28000.0, "kW"),
                new Thrust(28000.0, "kN", "static"),
                new Thrust(5500.0,  "kN", "cruise"),
                new TSFC(0.38, "lb/(lbf.h)"));
        assertEquals(em1, em2, "Engine models with same code must be equal (identity-based)");
    }
}
