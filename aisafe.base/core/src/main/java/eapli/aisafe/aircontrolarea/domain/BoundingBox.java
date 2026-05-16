package eapli.aisafe.aircontrolarea.domain;

import eapli.aisafe.shared.domain.Coordinates;
import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value object representing an axis-aligned bounding rectangle for an Air Control Area.
 * Stores the min/max latitude and longitude of the ACA boundary.
 *
 * Invariants:
 *   minLat &lt; maxLat
 *   minLon &lt; maxLon
 *   latitudes ∈ [-90, 90], longitudes ∈ [-180, 180]
 */
@Embeddable
public class BoundingBox implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "MIN_LAT", nullable = false)
    private double minLat;

    @Column(name = "MAX_LAT", nullable = false)
    private double maxLat;

    @Column(name = "MIN_LON", nullable = false)
    private double minLon;

    @Column(name = "MAX_LON", nullable = false)
    private double maxLon;

    public BoundingBox(final double minLat, final double maxLat,
                       final double minLon, final double maxLon) {
        Invariants.ensure(minLat < maxLat, "minLat must be strictly less than maxLat");
        Invariants.ensure(minLon < maxLon, "minLon must be strictly less than maxLon");
        Invariants.ensure(minLat >= -90.0 && maxLat <= 90.0,
                "Latitudes must be within [-90, 90]");
        Invariants.ensure(minLon >= -180.0 && maxLon <= 180.0,
                "Longitudes must be within [-180, 180]");
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    /** For ORM. */
    protected BoundingBox() {
    }

    public double minLat() { return minLat; }
    public double maxLat() { return maxLat; }
    public double minLon() { return minLon; }
    public double maxLon() { return maxLon; }

    /** Returns true when the given geographic point lies within (or on the boundary of) this box. */
    public boolean contains(final Coordinates point) {
        return point.latitude() >= minLat && point.latitude() <= maxLat
                && point.longitude() >= minLon && point.longitude() <= maxLon;
    }

    /** Returns true when this bounding box overlaps with the given one (touching counts as overlap). */
    public boolean overlaps(final BoundingBox other) {
        return !(other.maxLat < this.minLat || other.minLat > this.maxLat
                || other.maxLon < this.minLon || other.minLon > this.maxLon);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof BoundingBox b)) return false;
        return Double.compare(b.minLat, minLat) == 0
                && Double.compare(b.maxLat, maxLat) == 0
                && Double.compare(b.minLon, minLon) == 0
                && Double.compare(b.maxLon, maxLon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLat, maxLat, minLon, maxLon);
    }

    @Override
    public String toString() {
        return "lat[" + minLat + "," + maxLat + "] lon[" + minLon + "," + maxLon + "]";
    }
}
