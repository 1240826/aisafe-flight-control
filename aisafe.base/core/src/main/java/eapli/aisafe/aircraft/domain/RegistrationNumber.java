package eapli.aisafe.aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: aircraft registration number (unique worldwide).
 * Contains the registration number and the country of registration.
 * US070.
 */
@Embeddable
public class RegistrationNumber implements ValueObject, Comparable<RegistrationNumber> {

    private static final long serialVersionUID = 1L;

    @Column(name = "REG_NUMBER", nullable = false)
    private String number;

    @Column(name = "REGISTRATION_COUNTRY", nullable = false)
    private String registrationCountry;

    public RegistrationNumber(final String number, final String registrationCountry) {
        Preconditions.noneNull(number, registrationCountry);
        final String trimmedNum = number.trim().toUpperCase();
        Invariants.ensure(!trimmedNum.isBlank(), "Registration number must not be blank");
        Invariants.ensure(trimmedNum.matches("[A-Z][A-Z0-9\\-]{0,7}"),
                "Registration number must start with a letter and contain only uppercase letters, digits, "
                        + "and hyphens (e.g. 'CS-TUI', 'G-BNWA', 'N12345')");
        final String trimmedCountry = registrationCountry.trim();
        Invariants.ensure(!trimmedCountry.isBlank(), "Registration country must not be blank");
        Invariants.ensure(trimmedCountry.matches(".*\\p{L}.*"),
                "Registration country must contain at least one letter (e.g. 'Portugal')");
        this.number = trimmedNum;
        this.registrationCountry = trimmedCountry;
    }

    protected RegistrationNumber() {
        // for ORM
    }

    public static RegistrationNumber valueOf(final String number, final String registrationCountry) {
        return new RegistrationNumber(number, registrationCountry);
    }

    public String number() {
        return number;
    }

    public String registrationCountry() {
        return registrationCountry;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistrationNumber)) return false;
        return this.number.equals(((RegistrationNumber) o).number);
    }

    @Override
    public int hashCode() {
        return number.hashCode();
    }

    @Override
    public String toString() {
        return number + " (" + registrationCountry + ")";
    }

    @Override
    public int compareTo(final RegistrationNumber other) {
        return this.number.compareTo(other.number);
    }
}
