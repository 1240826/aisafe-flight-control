package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: 3-letter IATA airport code (e.g. "OPO", "LIS").
 * US052.
 */
@Embeddable
public class AirportIATA implements ValueObject, Comparable<AirportIATA> {

    private static final long serialVersionUID = 1L;

    private String iataCode;

    public AirportIATA(final String iataCode) {
        Preconditions.noneNull(iataCode);
        final String trimmed = iataCode.trim().toUpperCase();
        Invariants.ensure(trimmed.length() == 3, "Airport IATA code must be exactly 3 letters");
        Invariants.ensure(trimmed.matches("[A-Z]{3}"), "Airport IATA code must contain only uppercase letters");
        this.iataCode = trimmed;
    }

    protected AirportIATA() {
        // for ORM
    }

    public static AirportIATA valueOf(final String code) {
        return new AirportIATA(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportIATA)) return false;
        return this.iataCode.equals(((AirportIATA) o).iataCode);
    }

    @Override
    public int hashCode() {
        return iataCode.hashCode();
    }

    @Override
    public String toString() {
        return iataCode;
    }

    @Override
    public int compareTo(final AirportIATA other) {
        return this.iataCode.compareTo(other.iataCode);
    }
}
