package eapli.aisafe.simulation.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value object: a safety threshold used to assess a simulation result.
 * Invariants: value must be positive; unit must not be blank.
 */
@Embeddable
public class SafetyThreshold implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "THRESHOLD_VALUE", nullable = false)
    private double value;

    @Column(name = "THRESHOLD_UNIT", nullable = false)
    private String unit;

    public SafetyThreshold(final double value, final String unit) {
        Preconditions.noneNull(unit);
        Invariants.ensure(value > 0, "Safety threshold value must be positive");
        Invariants.ensure(!unit.isBlank(), "Safety threshold unit must not be blank");
        this.value = value;
        this.unit = unit.trim();
    }

    /** For ORM. */
    protected SafetyThreshold() {
    }

    public double value() { return value; }
    public String unit() { return unit; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyThreshold t)) return false;
        return Double.compare(t.value, value) == 0 && Objects.equals(t.unit, unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    @Override
    public String toString() {
        return value + " " + unit;
    }
}
