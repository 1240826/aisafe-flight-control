package eapli.aisafe.aircraftmodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: aircraft performance parameters.
 * All values in SI units (metres, m/s, metres).
 * US055.
 */
@Embeddable
public class AircraftPerformance implements ValueObject {

    private static final long serialVersionUID = 1L;

    /** Service ceiling in metres. */
    @Column(name = "SERVICE_CEILING", nullable = false)
    private double serviceCeiling;

    /** Cruise speed in knots (kt). */
    @Column(name = "CRUISE_SPEED", nullable = false)
    private double cruiseSpeed;

    /** Maximum range in nautical miles (NM). */
    @Column(name = "MAXIMUM_RANGE", nullable = false)
    private double maximumRange;

    public AircraftPerformance(final double serviceCeiling, final double cruiseSpeed,
                               final double maximumRange) {
        Invariants.ensure(serviceCeiling > 0, "Service ceiling must be positive");
        Invariants.ensure(cruiseSpeed > 0, "Cruise speed must be positive");
        Invariants.ensure(maximumRange > 0, "Maximum range must be positive");
        this.serviceCeiling = serviceCeiling;
        this.cruiseSpeed = cruiseSpeed;
        this.maximumRange = maximumRange;
    }

    protected AircraftPerformance() {
        // for ORM
    }

    public double serviceCeiling() { return serviceCeiling; }
    public double cruiseSpeed() { return cruiseSpeed; }
    public double maximumRange() { return maximumRange; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftPerformance)) return false;
        final AircraftPerformance other = (AircraftPerformance) o;
        return Double.compare(serviceCeiling, other.serviceCeiling) == 0
                && Double.compare(cruiseSpeed, other.cruiseSpeed) == 0
                && Double.compare(maximumRange, other.maximumRange) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(maximumRange);
    }

    @Override
    public String toString() {
        return "ceiling=" + serviceCeiling + "m, cruise=" + cruiseSpeed + "kt, range=" + maximumRange + "NM";
    }
}
