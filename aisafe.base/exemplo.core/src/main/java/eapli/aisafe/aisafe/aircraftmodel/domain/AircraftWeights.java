package eapli.aisafe.aircraftmodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: aircraft weight parameters.
 * Invariant: MTOW > MZFW > emptyWeight; all values positive.
 * US055.
 */
@Embeddable
public class AircraftWeights implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "EMPTY_WEIGHT", nullable = false)
    private double emptyWeight;

    /** Maximum Take-Off Weight. */
    @Column(name = "MTOW", nullable = false)
    private double mtow;

    /** Maximum Zero Fuel Weight. */
    @Column(name = "MZFW", nullable = false)
    private double mzfw;

    @Column(name = "MAX_FUEL_CAPACITY", nullable = false)
    private double maxFuelCapacity;

    public AircraftWeights(final double emptyWeight, final double mtow,
                           final double mzfw, final double maxFuelCapacity) {
        Invariants.ensure(emptyWeight > 0, "Empty weight must be positive");
        Invariants.ensure(mtow > 0, "MTOW must be positive");
        Invariants.ensure(mzfw > 0, "MZFW must be positive");
        Invariants.ensure(maxFuelCapacity > 0, "Max fuel capacity must be positive");
        Invariants.ensure(mtow > mzfw, "MTOW must be greater than MZFW");
        Invariants.ensure(mzfw > emptyWeight, "MZFW must be greater than empty weight");
        this.emptyWeight = emptyWeight;
        this.mtow = mtow;
        this.mzfw = mzfw;
        this.maxFuelCapacity = maxFuelCapacity;
    }

    protected AircraftWeights() {
        // for ORM
    }

    public double emptyWeight() { return emptyWeight; }
    public double mtow() { return mtow; }
    public double mzfw() { return mzfw; }
    public double maxFuelCapacity() { return maxFuelCapacity; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftWeights)) return false;
        final AircraftWeights other = (AircraftWeights) o;
        return Double.compare(emptyWeight, other.emptyWeight) == 0
                && Double.compare(mtow, other.mtow) == 0
                && Double.compare(mzfw, other.mzfw) == 0
                && Double.compare(maxFuelCapacity, other.maxFuelCapacity) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(mtow);
    }

    @Override
    public String toString() {
        return "emptyWeight=" + emptyWeight + ", MTOW=" + mtow
                + ", MZFW=" + mzfw + ", maxFuel=" + maxFuelCapacity;
    }
}
