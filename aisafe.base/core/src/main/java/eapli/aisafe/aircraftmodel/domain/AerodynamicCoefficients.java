package eapli.aisafe.aircraftmodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: aerodynamic coefficients used in lift/drag formulas.
 * US055.
 */
@Embeddable
public class AerodynamicCoefficients implements ValueObject {

    private static final long serialVersionUID = 1L;

    /** Wing area in m². */
    @Column(name = "WING_AREA", nullable = false)
    private double wingArea;

    /** Drag coefficient (dimensionless). */
    @Column(name = "DRAG_COEFFICIENT", nullable = false)
    private double dragCoefficient;

    /** Lift coefficient (dimensionless). */
    @Column(name = "LIFT_COEFFICIENT", nullable = false)
    private double liftCoefficient;

    public AerodynamicCoefficients(final double wingArea, final double dragCoefficient,
                                   final double liftCoefficient) {
        Invariants.ensure(wingArea > 0, "Wing area must be positive");
        Invariants.ensure(dragCoefficient > 0, "Drag coefficient must be positive");
        Invariants.ensure(liftCoefficient > 0, "Lift coefficient must be positive");
        this.wingArea = wingArea;
        this.dragCoefficient = dragCoefficient;
        this.liftCoefficient = liftCoefficient;
    }

    protected AerodynamicCoefficients() {
        // for ORM
    }

    public double wingArea() { return wingArea; }
    public double dragCoefficient() { return dragCoefficient; }
    public double liftCoefficient() { return liftCoefficient; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AerodynamicCoefficients)) return false;
        final AerodynamicCoefficients other = (AerodynamicCoefficients) o;
        return Double.compare(wingArea, other.wingArea) == 0
                && Double.compare(dragCoefficient, other.dragCoefficient) == 0
                && Double.compare(liftCoefficient, other.liftCoefficient) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(wingArea);
    }

    @Override
    public String toString() {
        return "S=" + wingArea + "m², Cd=" + dragCoefficient + ", Cl=" + liftCoefficient;
    }
}
