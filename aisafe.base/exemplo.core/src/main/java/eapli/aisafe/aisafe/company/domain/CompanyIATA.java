package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: 2-letter IATA airline code (e.g. "TP", "FR").
 * Client clarification: company IATA = 2 letters (different from airport IATA).
 * US060.
 */
@Embeddable
public class CompanyIATA implements ValueObject, Comparable<CompanyIATA> {

    private static final long serialVersionUID = 1L;

    private String iataCode;

    public CompanyIATA(final String iataCode) {
        Preconditions.noneNull(iataCode);
        final String trimmed = iataCode.trim().toUpperCase();
        Invariants.ensure(trimmed.length() == 2, "Company IATA code must be exactly 2 letters");
        this.iataCode = trimmed;
    }

    protected CompanyIATA() {
        // for ORM
    }

    public static CompanyIATA valueOf(final String code) {
        return new CompanyIATA(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyIATA)) return false;
        return this.iataCode.equals(((CompanyIATA) o).iataCode);
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
    public int compareTo(final CompanyIATA other) {
        return this.iataCode.compareTo(other.iataCode);
    }
}
