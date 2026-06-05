package eapli.aisafe.pilot.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value Object: pilot license number (unique across the system).
 * Format: letter followed by 4-10 digits (e.g. "P12345").
 * US075.
 */
@Embeddable
public class PilotId implements ValueObject, Comparable<PilotId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "LICENSE_NUMBER", nullable = false, unique = true, length = 20)
    private String licenseNumber;

    protected PilotId() {
    }

    public PilotId(final String licenseNumber) {
        Preconditions.noneNull(licenseNumber);
        final String trimmed = licenseNumber.trim().toUpperCase();
        Invariants.ensure(!trimmed.isBlank(), "Pilot license number must not be blank");
        Invariants.ensure(trimmed.matches("[A-Z][0-9]{4,10}"),
                "Pilot license must be a letter followed by 4-10 digits (e.g. 'P12345')");
        this.licenseNumber = trimmed;
    }

    public static PilotId valueOf(final String licenseNumber) {
        return new PilotId(licenseNumber);
    }

    public String licenseNumber() {
        return licenseNumber;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof PilotId other)) return false;
        return Objects.equals(licenseNumber, other.licenseNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenseNumber);
    }

    @Override
    public String toString() {
        return licenseNumber;
    }

    @Override
    public int compareTo(final PilotId other) {
        return this.licenseNumber.compareTo(other.licenseNumber);
    }
}
