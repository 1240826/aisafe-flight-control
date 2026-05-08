package eapli.aisafe.airport.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
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

    @Column(name = "LATITUDE", nullable = false)
    private double latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private double longitude;

    /** Cross-aggregate reference by AreaCode. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode areaCode;

    public Airport(final AirportIATA iata, final AirportICAO icao, final String name,
                   final String city, final String country,
                   final double latitude, final double longitude,
                   final AreaCode areaCode) {
        Preconditions.noneNull(iata, icao, name, city, country, areaCode);
        Invariants.ensure(!name.isBlank(), "Airport name must not be blank");
        Invariants.ensure(!city.isBlank(), "City must not be blank");
        Invariants.ensure(!country.isBlank(), "Country must not be blank");
        this.iata = iata;
        this.icao = icao;
        this.name = name.trim();
        this.city = city.trim();
        this.country = country.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaCode = areaCode;
    }

    protected Airport() {
        // for ORM
    }

    public AirportIATA iata() { return iata; }
    public AirportICAO icao() { return icao; }
    public String name() { return name; }
    public String city() { return city; }
    public String country() { return country; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
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
        return iata + " / " + icao + " — " + name + " (" + city + ", " + country + ")";
    }
}
