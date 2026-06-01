package eapli.aisafe.flightroute.domain;

import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
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

import java.time.LocalDate;

/**
 * Aggregate root: FlightRoute.
 * A route connects two airports and belongs to one air transport company.
 * A route can be deactivated from a given date onwards (US074).
 * Once deactivated, no new flights may be created on it.
 * US073, US074.
 */
@Entity
@Table(name = "FLIGHT_ROUTE")
public class FlightRoute implements AggregateRoot<FlightRouteName> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightRouteName name;

    /** Cross-aggregate reference by CompanyIATA. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "COMPANY_IATA"))})
    private CompanyIATA companyIATA;

    /** Departure airport. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "ORIGIN_IATA"))})
    private AirportIATA origin;

    /** Arrival airport. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "DESTINATION_IATA"))})
    private AirportIATA destination;

    /**
     * Date from which the route is no longer active.
     * Null means the route is currently active.
     */
    @Column(name = "DEACTIVATION_DATE")
    private LocalDate deactivationDate;

    /**
     * US073: create a new active flight route.
     *
     * @param name        unique route name (e.g. "TP123")
     * @param companyIATA owning company
     * @param origin      departure airport
     * @param destination arrival airport
     */
    public FlightRoute(final FlightRouteName name,
                       final CompanyIATA companyIATA,
                       final AirportIATA origin,
                       final AirportIATA destination) {
        Preconditions.noneNull(name, companyIATA, origin, destination);
        Invariants.ensure(!origin.equals(destination),
                "Origin and destination airports must be different");
        this.name = name;
        this.companyIATA = companyIATA;
        this.origin = origin;
        this.destination = destination;
        this.deactivationDate = null;
    }

    /** For ORM. */
    protected FlightRoute() {
    }

    /**
     * US074: deactivate this route from the given date onwards.
     * No new flights may be created on this route on or after this date.
     *
     * @param date the date from which the route is inactive (must not be null)
     * @throws IllegalStateException if the route is already inactive
     */
    public void deactivate(final LocalDate date) {
        Preconditions.noneNull(date);
        Invariants.ensure(isActive(), "Route is already deactivated");
        this.deactivationDate = date;
    }

    /**
     * Returns true if the route is currently active (not yet deactivated).
     */
    public boolean isActive() {
        return deactivationDate == null;
    }

    public FlightRouteName routeName() {
        return name;
    }

    public CompanyIATA companyIATA() {
        return companyIATA;
    }

    public AirportIATA origin() {
        return origin;
    }

    public AirportIATA destination() {
        return destination;
    }

    public LocalDate deactivationDate() {
        return deactivationDate;
    }

    @Override
    public FlightRouteName identity() {
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
        return "FlightRoute[" + name + ", " + origin + " → " + destination
                + (isActive() ? ", ACTIVE" : ", DEACTIVATED from " + deactivationDate) + "]";
    }
}