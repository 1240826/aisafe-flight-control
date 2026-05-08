package eapli.aisafe.aircontrolarea.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: ICAO area code (e.g. "LPPO", "LPPC").
 * US050.
 */
@Embeddable
public class AreaCode implements ValueObject, Comparable<AreaCode> {

    private static final long serialVersionUID = 1L;

    private String code;

    public AreaCode(final String code) {
        Preconditions.noneNull(code);
        Invariants.ensure(!code.isBlank(), "Area code must not be blank");
        this.code = code.trim().toUpperCase();
    }

    protected AreaCode() {
        // for ORM
    }

    public static AreaCode valueOf(final String code) {
        return new AreaCode(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AreaCode)) return false;
        return this.code.equals(((AreaCode) o).code);
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
    public int compareTo(final AreaCode other) {
        return this.code.compareTo(other.code);
    }
}
