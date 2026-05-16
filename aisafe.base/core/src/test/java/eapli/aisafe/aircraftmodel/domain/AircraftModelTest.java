package eapli.aisafe.aircraftmodel.domain;

import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AircraftModel aggregate root.
 * Covers US055 (creation), US057 (add variant), US058 (remove variant).
 */
class AircraftModelTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static AircraftModel validModel() {
        return new AircraftModel(
                new AircraftModelCode("B738"),
                "Boeing 737-800",
                new ManufacturerName("Boeing"),
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 62732, 26020),
                new AircraftPerformance(12500, 447, 3500),
                new AerodynamicCoefficients(125.0, 0.032, 1.2)
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidModelCanBeCreated() {
        final var m = validModel();
        assertNotNull(m);
        assertEquals("B738", m.code().toString());
        assertEquals("Boeing 737-800", m.name());
        assertEquals("Boeing", m.manufacturerName());
        assertEquals(AircraftType.PASSENGER, m.aircraftType());
        assertEquals(189, m.maxPassengers());
    }

    @Test
    void ensureIdentityReturnsCode() {
        final var m = validModel();
        assertEquals(new AircraftModelCode("B738"), m.identity());
    }

    @Test
    void ensureNullMaxPassengersIsPermitted() {
        // cargo aircraft may have no passenger count
        final var m = new AircraftModel(
                new AircraftModelCode("B77F"),
                "Boeing 777F Freighter",
                new ManufacturerName("Boeing"),
                AircraftType.CARGO,
                null,
                new AircraftWeights(138_100, 347_452, 229_520, 136_900),
                new AircraftPerformance(13_100, 488, 7_065),
                new AerodynamicCoefficients(427.8, 0.025, 1.7));
        assertNull(m.maxPassengers());
    }

    @Test
    void ensureVariantListIsInitiallyEmpty() {
        final var m = validModel();
        assertTrue(m.variants().isEmpty());
    }

    // ── Invariant violations ──────────────────────────────────────────────────

    @Test
    void ensureBlankModelNameIsRejected() {
        assertThrows(Exception.class, () -> new AircraftModel(
                new AircraftModelCode("B738"),
                "",
                new ManufacturerName("Boeing"),
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 62732, 26020),
                new AircraftPerformance(12500, 447, 3500),
                new AerodynamicCoefficients(125.0, 0.032, 1.2)));
    }

    @Test
    void ensureNullCodeIsRejected() {
        assertThrows(Exception.class, () -> new AircraftModel(
                null,
                "Boeing 737-800",
                new ManufacturerName("Boeing"),
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 62732, 26020),
                new AircraftPerformance(12500, 447, 3500),
                new AerodynamicCoefficients(125.0, 0.032, 1.2)));
    }

    @Test
    void ensureNullManufacturerIsRejected() {
        assertThrows(Exception.class, () -> new AircraftModel(
                new AircraftModelCode("B738"),
                "Boeing 737-800",
                null,
                AircraftType.PASSENGER,
                189,
                new AircraftWeights(41140, 79016, 62732, 26020),
                new AircraftPerformance(12500, 447, 3500),
                new AerodynamicCoefficients(125.0, 0.032, 1.2)));
    }

    @Test
    void ensureNonPositiveMaxPassengersIsRejected() {
        assertThrows(Exception.class, () -> new AircraftModel(
                new AircraftModelCode("B738"),
                "Boeing 737-800",
                new ManufacturerName("Boeing"),
                AircraftType.PASSENGER,
                0,  // invalid
                new AircraftWeights(41140, 79016, 62732, 26020),
                new AircraftPerformance(12500, 447, 3500),
                new AerodynamicCoefficients(125.0, 0.032, 1.2)));
    }

    // ── addVariant (US057) ────────────────────────────────────────────────────

    @Test
    void ensureVariantCanBeAdded() {
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        assertEquals(1, m.variants().size());
        assertEquals(new EngineModelCode("CFM56-7B"), m.variants().get(0).engineModelCode());
    }

    @Test
    void ensureMultipleVariantsWithSameMotorizationTypeCanBeAdded() {
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        m.addVariant(new EngineModelCode("LEAP-1B"), MotorizationType.TURBOFAN);
        assertEquals(2, m.variants().size());
    }

    @Test
    void ensureDuplicateVariantIsRejected() {
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        assertThrows(Exception.class,
                () -> m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN),
                "Duplicate engine code in same model must be rejected");
    }

    @Test
    void ensureVariantsWithDifferentMotorizationTypesAreRejected() {
        // All variants of a model must share the same MotorizationType
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        assertThrows(Exception.class,
                () -> m.addVariant(new EngineModelCode("PW127"), MotorizationType.TURBOPROP));
    }

    // ── removeVariant (US058) ─────────────────────────────────────────────────

    @Test
    void ensureVariantCanBeRemovedWhenMoreThanOneExists() {
        // Add two variants, remove one — one must remain
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        m.addVariant(new EngineModelCode("GE90-94B"), MotorizationType.TURBOFAN);
        m.removeVariant(new EngineModelCode("CFM56-7B"));
        assertEquals(1, m.variants().size());
        assertEquals(new EngineModelCode("GE90-94B"), m.variants().get(0).engineModelCode());
    }

    @Test
    void ensureRemovingLastVariantIsRejected() {
        // Domain invariant: at least one variant must remain
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        assertThrows(Exception.class,
                () -> m.removeVariant(new EngineModelCode("CFM56-7B")),
                "Removing the last engine variant must be rejected");
    }

    @Test
    void ensureRemovingNonExistentVariantIsRejected() {
        // Two variants so the "last variant" invariant is not triggered first
        final var m = validModel();
        m.addVariant(new EngineModelCode("CFM56-7B"), MotorizationType.TURBOFAN);
        m.addVariant(new EngineModelCode("GE90-94B"), MotorizationType.TURBOFAN);
        assertThrows(Exception.class,
                () -> m.removeVariant(new EngineModelCode("UNKNOWN")),
                "Removing a variant that does not exist must throw");
    }

    // ── AircraftModelCode VO ──────────────────────────────────────────────────

    @Test
    void ensureBlankAircraftModelCodeIsRejected() {
        assertThrows(Exception.class, () -> new AircraftModelCode(""));
    }

    @Test
    void ensureNullAircraftModelCodeIsRejected() {
        assertThrows(Exception.class, () -> new AircraftModelCode(null));
    }

    // ── AircraftWeights VO ────────────────────────────────────────────────────

    @Test
    void ensureMTOWMustBeGreaterThanMZFW() {
        assertThrows(Exception.class,
                () -> new AircraftWeights(41140, 62732, 79016, 26020),  // MTOW < MZFW
                "MTOW must be greater than MZFW");
    }

    @Test
    void ensureMZFWMustBeGreaterThanEmptyWeight() {
        assertThrows(Exception.class,
                () -> new AircraftWeights(79016, 90000, 41140, 26020),  // MZFW < emptyWeight
                "MZFW must be greater than empty weight");
    }

    @Test
    void ensureNonPositiveEmptyWeightIsRejected() {
        assertThrows(Exception.class,
                () -> new AircraftWeights(0, 79016, 62732, 26020));
    }
}
