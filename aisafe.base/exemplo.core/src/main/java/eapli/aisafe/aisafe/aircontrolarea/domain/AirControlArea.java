package eapli.aisafe.aircontrolarea.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root: Air Control Area (ACA).
 * Boundary defined by min/max lat-lon rectangle.
 * maxAltitudeMetres read from Application.settings() — NOT hardcoded (default 14000 m).
 * name is unique (client clarification).
 * No overlap with existing ACAs (enforced by controller).
 * US050.
 */
@Entity
@Table(name = "AIR_CONTROL_AREA")
public class AirControlArea implements AggregateRoot<AreaCode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode code;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "name", column = @Column(name = "AREA_NAME", unique = true))})
    private AreaName name;

    @Column(name = "MIN_LAT", nullable = false)
    private double minLat;

    @Column(name = "MAX_LAT", nullable = false)
    private double maxLat;

    @Column(name = "MIN_LON", nullable = false)
    private double minLon;

    @Column(name = "MAX_LON", nullable = false)
    private double maxLon;

    /** Configurable maximum altitude in metres (default 14000). */
    @Column(name = "MAX_ALTITUDE_METRES", nullable = false)
    private int maxAltitudeMetres;

    public AirControlArea(final AreaCode code, final AreaName name,
                          final double minLat, final double maxLat,
                          final double minLon, final double maxLon,
                          final int maxAltitudeMetres) {
        Preconditions.noneNull(code, name);
        Invariants.ensure(minLat < maxLat, "minLat must be less than maxLat");
        Invariants.ensure(minLon < maxLon, "minLon must be less than maxLon");
        Invariants.ensure(maxAltitudeMetres > 0, "maxAltitudeMetres must be positive");
        this.code = code;
        this.name = name;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.maxAltitudeMetres = maxAltitudeMetres;
    }

    protected AirControlArea() {
        // for ORM
    }

    public AreaCode code() {
        return code;
    }

    public AreaName name() {
        return name;
    }

    public double minLat() { return minLat; }
    public double maxLat() { return maxLat; }
    public double minLon() { return minLon; }
    public double maxLon() { return maxLon; }
    public int maxAltitudeMetres() { return maxAltitudeMetres; }

    /** Returns true if the given lat/lon point lies within this area's boundary. */
    public boolean containsCoordinates(final double lat, final double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    /** Returns true if the given rectangle overlaps with this area. */
    public boolean overlapsWith(final double oMinLat, final double oMaxLat,
                                 final double oMinLon, final double oMaxLon) {
        return !(oMaxLat < this.minLat || oMinLat > this.maxLat
                || oMaxLon < this.minLon || oMinLon > this.maxLon);
    }

    @Override
    public AreaCode identity() {
        return code;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return code + " — " + name;
    }
}
