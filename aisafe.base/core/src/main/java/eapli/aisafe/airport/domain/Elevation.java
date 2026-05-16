package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Arrays;

/**
 * Value Object: airport elevation above sea level.
 * Invariant: value must be positive (> 0) per US052.6; unit must be a valid aviation unit.
 *
 * Acceptance criteria (US052.6):
 *   - Elevation must have a positive value and a unit (e.g. metres).
 *   - Negative or zero values are rejected.
 * US052.
 */
@Embeddable
public class Elevation implements ValueObject {

    private static final long serialVersionUID = 1L;

    /** Accepted elevation units. */
    public static final String[] VALID_UNITS = { "m", "ft" };

    @Column(name = "ELEVATION_VALUE", nullable = false)
    private double value;

    @Column(name = "ELEVATION_UNIT", nullable = false)
    private String unit;

    /**
     * Creates an Elevation VO.
     *
     * @param value the elevation value — must be positive (> 0) per US052.6
     * @param unit  the unit of measure ("m" or "ft") — must be a valid unit
     */
    public Elevation(final double value, final String unit) {
        Preconditions.noneNull(unit);
        Invariants.ensure(value > 0, "Elevation value must be positive (> 0)");
        final String trimmed = unit.trim();
        Invariants.ensure(Arrays.asList(VALID_UNITS).contains(trimmed),
                "Elevation unit must be one of: " + String.join(", ", VALID_UNITS));
        this.value = value;
        this.unit = trimmed;
    }

    protected Elevation() {
        // for ORM
    }

    /** The numeric elevation value. Always > 0. */
    public double value() {
        return value;
    }

    /** The unit of measure (e.g. "m", "ft"). Never blank. */
    public String unit() {
        return unit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Elevation)) return false;
        final Elevation other = (Elevation) o;
        return Double.compare(value, other.value) == 0 && unit.equals(other.unit);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return value + " " + unit;
    }
}
