package eapli.aisafe.aircraftmodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: unique ICAO aircraft type designator (e.g. "B738", "A320").
 * US055.
 */
@Embeddable
public class AircraftModelCode implements ValueObject, Comparable<AircraftModelCode> {

    private static final long serialVersionUID = 1L;

    private String code;

    public AircraftModelCode(final String code) {
        Preconditions.noneNull(code);
        Invariants.ensure(!code.isBlank(), "Aircraft model code must not be blank");
        this.code = code.trim().toUpperCase();
    }

    protected AircraftModelCode() {
        // for ORM
    }

    public static AircraftModelCode valueOf(final String code) {
        return new AircraftModelCode(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftModelCode)) return false;
        return this.code.equals(((AircraftModelCode) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public int compareTo(final AircraftModelCode other) {
        return this.code.compareTo(other.code);
    }
}
