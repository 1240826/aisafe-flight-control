package eapli.aisafe.aircontrolarea.domain;

import eapli.aisafe.shared.domain.Coordinates;
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
 * Boundary defined by a {@link BoundingBox} VO (min/max lat-lon rectangle).
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

    /** Rectangular boundary of this Air Control Area. */
    @Embedded
    private BoundingBox boundary;

    /** Configurable maximum altitude in metres (default 14000). */
    @Column(name = "MAX_ALTITUDE_METRES", nullable = false)
    private int maxAltitudeMetres;

    /**
     * Primary constructor using the {@link BoundingBox} VO.
     */
    public AirControlArea(final AreaCode code, final AreaName name,
                          final BoundingBox boundary,
                          final int maxAltitudeMetres) {
        Preconditions.noneNull(code, name, boundary);
        Invariants.ensure(maxAltitudeMetres > 0, "maxAltitudeMetres must be positive");
        this.code = code;
        this.name = name;
        this.boundary = boundary;
        this.maxAltitudeMetres = maxAltitudeMetres;
    }

    /**
     * Convenience constructor accepting raw lat/lon doubles — delegates to the primary constructor.
     * Kept for backward compatibility with controllers, tests, and bootstrappers.
     */
    public AirControlArea(final AreaCode code, final AreaName name,
                          final double minLat, final double maxLat,
                          final double minLon, final double maxLon,
                          final int maxAltitudeMetres) {
        this(code, name, new BoundingBox(minLat, maxLat, minLon, maxLon), maxAltitudeMetres);
    }

    protected AirControlArea() {
        // for ORM
    }

    public AreaCode code() { return code; }
    public AreaName name() { return name; }

    /** Returns the rectangular boundary of this Air Control Area. */
    public BoundingBox boundary() { return boundary; }

    /** Convenience accessor — delegates to {@code boundary().minLat()}. */
    public double minLat() { return boundary.minLat(); }

    /** Convenience accessor — delegates to {@code boundary().maxLat()}. */
    public double maxLat() { return boundary.maxLat(); }

    /** Convenience accessor — delegates to {@code boundary().minLon()}. */
    public double minLon() { return boundary.minLon(); }

    /** Convenience accessor — delegates to {@code boundary().maxLon()}. */
    public double maxLon() { return boundary.maxLon(); }

    public int maxAltitudeMetres() { return maxAltitudeMetres; }

    /**
     * Returns true if the given lat/lon point lies within this area's boundary.
     * Delegates to {@link BoundingBox#contains(Coordinates)}.
     */
    public boolean containsCoordinates(final double lat, final double lon) {
        return boundary.contains(new Coordinates(lat, lon));
    }

    /**
     * Returns true if the given rectangle overlaps with this area.
     * Delegates to {@link BoundingBox#overlaps(BoundingBox)}.
     */
    public boolean overlapsWith(final double oMinLat, final double oMaxLat,
                                 final double oMinLon, final double oMaxLon) {
        return boundary.overlaps(new BoundingBox(oMinLat, oMaxLat, oMinLon, oMaxLon));
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
        return code + " - " + name;
    }
}
