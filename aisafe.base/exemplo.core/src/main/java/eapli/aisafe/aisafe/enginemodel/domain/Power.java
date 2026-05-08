package eapli.aisafe.enginemodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: engine rated power (used for turboprop/electricPropeller engines).
 * US056.
 */
@Embeddable
public class Power implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "POWER_VALUE", nullable = false)
    private double value;

    @Column(name = "POWER_UNIT", nullable = false)
    private String unit;

    public Power(final double value, final String unit) {
        Preconditions.noneNull(unit);
        Invariants.ensure(value > 0, "Power value must be positive");
        Invariants.ensure(!unit.isBlank(), "Power unit must not be blank");
        this.value = value;
        this.unit = unit.trim();
    }

    protected Power() {
        // for ORM
    }

    public double value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Power)) return false;
        final Power other = (Power) o;
        return Double.compare(this.value, other.value) == 0
                && this.unit.equalsIgnoreCase(other.unit);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value) * 31 + unit.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return value + " " + unit;
    }
}
