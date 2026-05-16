package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: 2-3 letter ICAO airline designator (e.g. "TAP", "RYR").
 * Client clarification: company ICAO = 2-3 letters.
 * US060.
 */
@Embeddable
public class CompanyICAO implements ValueObject, Comparable<CompanyICAO> {

    private static final long serialVersionUID = 1L;

    private String icaoCode;

    public CompanyICAO(final String icaoCode) {
        Preconditions.noneNull(icaoCode);
        final String trimmed = icaoCode.trim().toUpperCase();
        Invariants.ensure(trimmed.matches("[A-Z]{2,3}"),
                "Company ICAO code must be 2-3 letters (A-Z), got: '" + trimmed + "'");
        this.icaoCode = trimmed;
    }

    protected CompanyICAO() {
        // for ORM
    }

    public static CompanyICAO valueOf(final String code) {
        return new CompanyICAO(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyICAO)) return false;
        return this.icaoCode.equals(((CompanyICAO) o).icaoCode);
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
    public int compareTo(final CompanyICAO other) {
        return this.icaoCode.compareTo(other.icaoCode);
    }
}
