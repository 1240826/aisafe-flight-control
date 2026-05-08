package eapli.aisafe.weatherdata.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: wind speed (kt) and direction (degrees 0-360).
 * US041.
 */
@Embeddable
public class WindCondition implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "WIND_SPEED_KT", nullable = false)
    private double speedKnots;

    @Column(name = "WIND_DIR_DEG", nullable = false)
    private int directionDegrees;

    public WindCondition(final double speedKnots, final int directionDegrees) {
        Invariants.ensure(speedKnots >= 0, "Wind speed must be non-negative");
        Invariants.ensure(directionDegrees >= 0 && directionDegrees <= 360,
                "Wind direction must be between 0 and 360 degrees");
        this.speedKnots = speedKnots;
        this.directionDegrees = directionDegrees;
    }

    protected WindCondition() {
        // for ORM
    }

    public double speedKnots() { return speedKnots; }
    public int directionDegrees() { return directionDegrees; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WindCondition)) return false;
        final WindCondition w = (WindCondition) o;
        return Double.compare(speedKnots, w.speedKnots) == 0 && directionDegrees == w.directionDegrees;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(speedKnots, directionDegrees);
    }

    @Override
    public String toString() {
        return String.format("Wind(%.1f kt @ %d°)", speedKnots, directionDegrees);
    }
}
