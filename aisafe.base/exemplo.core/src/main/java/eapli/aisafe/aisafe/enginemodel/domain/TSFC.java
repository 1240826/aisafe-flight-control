package eapli.aisafe.enginemodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: Thrust-Specific Fuel Consumption (TSFC).
 * Measures fuel efficiency of the engine.
 * US056.
 */
@Embeddable
public class TSFC implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "TSFC_VALUE", nullable = false)
    private double value;

    @Column(name = "TSFC_UNIT", nullable = false)
    private String unit;

    public TSFC(final double value, final String unit) {
        Preconditions.noneNull(unit);
        Invariants.ensure(value > 0, "TSFC value must be positive");
        Invariants.ensure(!unit.isBlank(), "TSFC unit must not be blank");
        this.value = value;
        this.unit = unit.trim();
    }

    protected TSFC() {
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
        if (!(o instanceof TSFC)) return false;
        final TSFC other = (TSFC) o;
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
