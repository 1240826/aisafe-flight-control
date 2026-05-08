package eapli.aisafe.weatherdata.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: rectangular sub-area of an ACA for weather observations.
 * Parallelepiped slice defined by lat/lon bounds + altitude band.
 * Client clarification: "weather is for rectangular sub-areas of ACAs".
 * US041.
 */
@Embeddable
public class WeatherSubArea implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "WA_MIN_LAT", nullable = false)
    private double minLat;
    @Column(name = "WA_MAX_LAT", nullable = false)
    private double maxLat;
    @Column(name = "WA_MIN_LON", nullable = false)
    private double minLon;
    @Column(name = "WA_MAX_LON", nullable = false)
    private double maxLon;
    @Column(name = "WA_MIN_ALT_M", nullable = false)
    private int minAltMetres;
    @Column(name = "WA_MAX_ALT_M", nullable = false)
    private int maxAltMetres;

    public WeatherSubArea(final double minLat, final double maxLat,
                          final double minLon, final double maxLon,
                          final int minAltMetres, final int maxAltMetres) {
        Invariants.ensure(minLat < maxLat, "minLat must be less than maxLat");
        Invariants.ensure(minLon < maxLon, "minLon must be less than maxLon");
        Invariants.ensure(minAltMetres >= 0, "minAlt must be non-negative");
        Invariants.ensure(maxAltMetres > minAltMetres, "maxAlt must be greater than minAlt");
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minAltMetres = minAltMetres;
        this.maxAltMetres = maxAltMetres;
    }

    protected WeatherSubArea() {
        // for ORM
    }

    public double minLat() { return minLat; }
    public double maxLat() { return maxLat; }
    public double minLon() { return minLon; }
    public double maxLon() { return maxLon; }
    public int minAltMetres() { return minAltMetres; }
    public int maxAltMetres() { return maxAltMetres; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WeatherSubArea)) return false;
        final WeatherSubArea w = (WeatherSubArea) o;
        return Double.compare(minLat, w.minLat) == 0 && Double.compare(maxLat, w.maxLat) == 0
                && Double.compare(minLon, w.minLon) == 0 && Double.compare(maxLon, w.maxLon) == 0
                && minAltMetres == w.minAltMetres && maxAltMetres == w.maxAltMetres;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(minLat, maxLat, minLon, maxLon, minAltMetres, maxAltMetres);
    }

    @Override
    public String toString() {
        return String.format("SubArea[lat(%.2f,%.2f) lon(%.2f,%.2f) alt(%d,%d)]",
                minLat, maxLat, minLon, maxLon, minAltMetres, maxAltMetres);
    }
}
