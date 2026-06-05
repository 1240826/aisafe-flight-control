package eapli.aisafe.company.domain;

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
 * Aggregate root: Air Transport Company.
 * Identity = CompanyIATA. Both IATA and ICAO are unique (3 independent checks by controller).
 * US060.
 */
@Entity
@Table(name = "AIR_TRANSPORT_COMPANY")
public class AirTransportCompany implements AggregateRoot<CompanyIATA> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "IATA_CODE"))})
    private CompanyIATA iata;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "icaoCode", column = @Column(name = "ICAO_CODE", unique = true))})
    private CompanyICAO icao;

    @Column(name = "COMPANY_NAME", nullable = false, unique = true)
    private String name;

    public AirTransportCompany(final CompanyIATA iata, final CompanyICAO icao, final String name) {
        Preconditions.noneNull(iata, icao, name);
        Invariants.ensure(!name.isBlank(), "Company name must not be blank");
        Invariants.ensure(name.matches(".*\\p{L}.*"),
                "Company name must contain at least one letter");
        this.iata = iata;
        this.icao = icao;
        this.name = name.trim();
    }

    protected AirTransportCompany() {
        // for ORM
    }

    public CompanyIATA iata() { return iata; }
    public CompanyICAO icao() { return icao; }
    public String name() { return name; }

    @Override
    public CompanyIATA identity() {
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
        return iata + "/" + icao + " - " + name;
    }
}
