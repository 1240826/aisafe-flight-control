package eapli.aisafe.enginemodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Arrays;

/**
 * Value Object: engine thrust.
 * Manufacturer-supplied at static and cruise speed.
 * Two instances in EngineModel: staticThrust and cruiseThrust.
 * US056.
 */
@Embeddable
public class Thrust implements ValueObject {

    private static final long serialVersionUID = 1L;

    /** Accepted thrust units for aviation engine specifications. */
    public static final String[] VALID_UNITS = { "N", "kN", "lbf" };

    /** Accepted speed reference values. */
    public static final String STATIC = "static";
    public static final String CRUISE = "cruise";
    public static final String[] VALID_SPEED_REFS = { STATIC, CRUISE };

    @Column(name = "THRUST_VALUE", nullable = false)
    private double value;

    @Column(name = "THRUST_UNIT", nullable = false)
    private String unit;

    /** Speed reference context: "static" or "cruise". */
    @Column(name = "THRUST_SPEED_REF", nullable = false)
    private String speedReference;

    public Thrust(final double value, final String unit, final String speedReference) {
        Preconditions.noneNull(unit, speedReference);
        Invariants.ensure(value > 0, "Thrust value must be positive");
        final String trimmedUnit = unit.trim();
        final String trimmedRef  = speedReference.trim();
        Invariants.ensure(Arrays.asList(VALID_UNITS).contains(trimmedUnit),
                "Thrust unit must be one of: " + String.join(", ", VALID_UNITS));
        Invariants.ensure(Arrays.asList(VALID_SPEED_REFS).contains(trimmedRef),
                "Speed reference must be one of: " + String.join(", ", VALID_SPEED_REFS));
        this.value = value;
        this.unit = trimmedUnit;
        this.speedReference = trimmedRef;
    }

    protected Thrust() {
        // for ORM
    }

    public double value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public String speedReference() {
        return speedReference;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Thrust)) return false;
        final Thrust other = (Thrust) o;
        return Double.compare(this.value, other.value) == 0
                && this.unit.equalsIgnoreCase(other.unit)
                && this.speedReference.equalsIgnoreCase(other.speedReference);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value) * 31 + unit.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return value + " " + unit + " @ " + speedReference;
    }
}
