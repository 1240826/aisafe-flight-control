package eapli.aisafe.manufacturer.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root: aircraft manufacturer.
 * Client clarification: "obviously a manufacturer cannot be a VO" — full aggregate.
 * US055.
 */
@Entity
@Table(name = "MANUFACTURER")
public class Manufacturer implements AggregateRoot<ManufacturerName> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private ManufacturerName name;

    @Column(name = "COUNTRY", nullable = false)
    private String country;

    public Manufacturer(final ManufacturerName name, final String country) {
        Preconditions.noneNull(name, country);
        eapli.framework.validations.Invariants.ensure(!country.isBlank(), "Country must not be blank");
        this.name = name;
        this.country = country.trim();
    }

    public Manufacturer(final String name, final String country) {
        this(new ManufacturerName(name), country);
    }

    protected Manufacturer() {
        // for ORM
    }

    public ManufacturerName name() {
        return name;
    }

    public String country() {
        return country;
    }

    @Override
    public ManufacturerName identity() {
        return name;
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
        return name.toString();
    }
}
