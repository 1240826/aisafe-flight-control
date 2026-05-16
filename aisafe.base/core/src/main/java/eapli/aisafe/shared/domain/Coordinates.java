package eapli.aisafe.shared.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Shared value object representing a geographic point (latitude / longitude).
 * Used by Airport, Simulation, and any other aggregate that references a point location.
 *
 * Invariants:
 *   latitude  ∈ [-90, 90]
 *   longitude ∈ [-180, 180]
 */
@Embeddable
public class Coordinates implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "LATITUDE", nullable = false)
    private double latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private double longitude;

    public Coordinates(final double latitude, final double longitude) {
        Invariants.ensure(latitude >= -90.0 && latitude <= 90.0,
                "Latitude must be in [-90, 90], got: " + latitude);
        Invariants.ensure(longitude >= -180.0 && longitude <= 180.0,
                "Longitude must be in [-180, 180], got: " + longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** For ORM. */
    protected Coordinates() {
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates other)) return false;
        return Double.compare(other.latitude, latitude) == 0
                && Double.compare(other.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }
}
