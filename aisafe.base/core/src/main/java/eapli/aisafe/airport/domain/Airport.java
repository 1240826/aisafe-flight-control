package eapli.aisafe.airport.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
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
 * Aggregate root: Airport.
 * Belongs to an AirControlArea (cross-aggregate ref by AreaCode).
 * Country selected from bootstrapped list; city is free text.
 * Elevation is a VO containing a positive value and a unit (US052.6).
 * Location is a shared {@link Coordinates} VO (lat/lon point).
 * US052.
 */
@Entity
@Table(name = "AIRPORT")
public class Airport implements AggregateRoot<AirportIATA> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "IATA_CODE"))})
    private AirportIATA iata;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "icaoCode", column = @Column(name = "ICAO_CODE", unique = true))})
    private AirportICAO icao;

    @Column(name = "AIRPORT_NAME", nullable = false)
    private String name;

    @Column(name = "CITY", nullable = false)
    private String city;

    @Column(name = "COUNTRY", nullable = false)
    private String country;

    /** Geographic location of the airport — shared VO. */
    @Embedded
    private Coordinates location;

    /** Elevation above sea level (US052.6): positive value + unit. */
    @Embedded
    private Elevation elevation;

    /** Cross-aggregate reference by AreaCode. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode areaCode;

    /**
     * Primary constructor using the {@link Coordinates} VO.
     */
    public Airport(final AirportIATA iata, final AirportICAO icao, final String name,
                   final String city, final String country,
                   final Coordinates location,
                   final Elevation elevation,
                   final AreaCode areaCode) {
        Preconditions.noneNull(iata, icao, name, city, country, location, elevation, areaCode);
        Invariants.ensure(!name.isBlank(), "Airport name must not be blank");
        Invariants.ensure(name.matches(".*\\p{L}.*"),
                "Airport name must contain at least one letter");
        Invariants.ensure(!city.isBlank(), "City must not be blank");
        Invariants.ensure(city.matches(".*\\p{L}.*"),
                "City must contain at least one letter (e.g. 'Lisbon')");
        Invariants.ensure(!country.isBlank(), "Country must not be blank");
        Invariants.ensure(country.matches(".*\\p{L}.*"),
                "Country must contain at least one letter");
        this.iata = iata;
        this.icao = icao;
        this.name = name.trim();
        this.city = city.trim();
        this.country = country.trim();
        this.location = location;
        this.elevation = elevation;
        this.areaCode = areaCode;
    }

    /**
     * Convenience constructor accepting raw lat/lon doubles — delegates to the primary constructor.
     * Kept for backward compatibility with controllers and bootstrappers.
     */
    public Airport(final AirportIATA iata, final AirportICAO icao, final String name,
                   final String city, final String country,
                   final double latitude, final double longitude,
                   final Elevation elevation,
                   final AreaCode areaCode) {
        this(iata, icao, name, city, country,
                new Coordinates(latitude, longitude),
                elevation, areaCode);
    }

    protected Airport() {
        // for ORM
    }

    public AirportIATA iata() { return iata; }
    public AirportICAO icao() { return icao; }
    public String name() { return name; }
    public String city() { return city; }
    public String country() { return country; }

    /** Returns the geographic location of this airport as a {@link Coordinates} VO. */
    public Coordinates location() { return location; }

    /** Convenience accessor — delegates to {@code location().latitude()}. */
    public double latitude() { return location.latitude(); }

    /** Convenience accessor — delegates to {@code location().longitude()}. */
    public double longitude() { return location.longitude(); }

    public Elevation elevation() { return elevation; }
    public AreaCode areaCode() { return areaCode; }

    @Override
    public AirportIATA identity() {
        return iata;
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
        return iata + " / " + icao + " — " + name + " (" + city + ", " + country
                + ") elev=" + elevation;
    }
}
