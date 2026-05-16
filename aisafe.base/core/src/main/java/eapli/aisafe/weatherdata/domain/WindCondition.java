package eapli.aisafe.weatherdata.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: wind observation at a specific geographic point and altitude.
 * Per domain model: WindCondition *-- Coordinates_WD (each condition has its own location).
 * US041.
 */
@Embeddable
public class WindCondition implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "WIND_SPEED_KT", nullable = false)
    private double speedKnots;

    @Column(name = "WIND_DIR_DEG", nullable = false)
    private int directionDegrees;

    /** Geographic latitude of this observation (-90 to 90). */
    @Column(name = "WD_LATITUDE", nullable = false)
    private double latitude;

    /** Geographic longitude of this observation (-180 to 180). */
    @Column(name = "WD_LONGITUDE", nullable = false)
    private double longitude;

    /** Altitude of this observation in metres (>= 0). */
    @Column(name = "WD_ALTITUDE_M", nullable = false)
    private int altitudeMetres;

    public WindCondition(final double speedKnots, final int directionDegrees,
                         final double latitude, final double longitude,
                         final int altitudeMetres) {
        Invariants.ensure(speedKnots > 0, "Wind speed must be strictly positive");
        Invariants.ensure(directionDegrees >= 0 && directionDegrees < 360,
                "Wind direction must be between 0 (inclusive) and 360 (exclusive) degrees");
        Invariants.ensure(latitude >= -90 && latitude <= 90,
                "Latitude must be between -90 and 90");
        Invariants.ensure(longitude >= -180 && longitude <= 180,
                "Longitude must be between -180 and 180");
        Invariants.ensure(altitudeMetres >= 0, "Altitude must be non-negative");
        this.speedKnots = speedKnots;
        this.directionDegrees = directionDegrees;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMetres = altitudeMetres;
    }

    protected WindCondition() {
        // for ORM
    }

    public double speedKnots() { return speedKnots; }
    public int directionDegrees() { return directionDegrees; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
    public int altitudeMetres() { return altitudeMetres; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WindCondition)) return false;
        final WindCondition w = (WindCondition) o;
        return Double.compare(speedKnots, w.speedKnots) == 0
                && directionDegrees == w.directionDegrees
                && Double.compare(latitude, w.latitude) == 0
                && Double.compare(longitude, w.longitude) == 0
                && altitudeMetres == w.altitudeMetres;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(speedKnots, directionDegrees, latitude, longitude, altitudeMetres);
    }

    @Override
    public String toString() {
        return String.format("Wind(%.1f kt @ %d deg, lat=%.4f lon=%.4f alt=%d m)",
                speedKnots, directionDegrees, latitude, longitude, altitudeMetres);
    }
}
