package eapli.aisafe.flightroute.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: flight route name.
 * Format: 2-letter company initials followed by up to 4 digits (e.g. "TP123").
 * Must be unique across the system.
 * US073, US074.
 */
@Embeddable
public class FlightRouteName implements ValueObject, Comparable<FlightRouteName> {

    private static final long serialVersionUID = 1L;

    @Column(name = "ROUTE_NAME", nullable = false, unique = true)
    private String name;

    public FlightRouteName(final String name) {
        Preconditions.noneNull(name);
        final String trimmed = name.trim().toUpperCase();
        Invariants.ensure(!trimmed.isBlank(), "Flight route name must not be blank");
        Invariants.ensure(trimmed.matches("[A-Z]{2}\\d{1,4}"),
                "Flight route name must be 2 letters followed by 1-4 digits (e.g. 'TP123')");
        this.name = trimmed;
    }

    /** For ORM. */
    protected FlightRouteName() {
    }

    public static FlightRouteName valueOf(final String name) {
        return new FlightRouteName(name);
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightRouteName r)) return false;
        return name.equals(r.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final FlightRouteName other) {
        return this.name.compareTo(other.name);
    }
}