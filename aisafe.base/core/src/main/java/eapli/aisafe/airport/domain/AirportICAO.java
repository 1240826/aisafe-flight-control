package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: 4-letter ICAO airport code (e.g. "LPPR", "LPPT").
 * US052.
 */
@Embeddable
public class AirportICAO implements ValueObject, Comparable<AirportICAO> {

    private static final long serialVersionUID = 1L;

    private String icaoCode;

    public AirportICAO(final String icaoCode) {
        Preconditions.noneNull(icaoCode);
        final String trimmed = icaoCode.trim().toUpperCase();
        Invariants.ensure(trimmed.length() == 4, "Airport ICAO code must be exactly 4 letters");
        Invariants.ensure(trimmed.matches("[A-Z]{4}"), "Airport ICAO code must contain only uppercase letters");
        this.icaoCode = trimmed;
    }

    protected AirportICAO() {
        // for ORM
    }

    public static AirportICAO valueOf(final String code) {
        return new AirportICAO(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportICAO)) return false;
        return this.icaoCode.equals(((AirportICAO) o).icaoCode);
    }

    @Override
    public int hashCode() {
        return icaoCode.hashCode();
    }

    @Override
    public String toString() {
        return icaoCode;
    }

    @Override
    public int compareTo(final AirportICAO other) {
        return this.icaoCode.compareTo(other.icaoCode);
    }
}
