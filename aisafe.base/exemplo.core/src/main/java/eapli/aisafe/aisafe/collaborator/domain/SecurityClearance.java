package eapli.aisafe.collaborator.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * Value Object: security clearance with an expiry date.
 * Invariant: expiryDate must be today or in the future.
 * US061, US063.
 */
@Embeddable
public class SecurityClearance implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "CLEARANCE_EXPIRY", nullable = false)
    private LocalDate expiryDate;

    public SecurityClearance(final LocalDate expiryDate) {
        Preconditions.noneNull(expiryDate);
        Invariants.ensure(!expiryDate.isBefore(LocalDate.now()),
                "Security clearance expiry date must be today or in the future");
        this.expiryDate = expiryDate;
    }

    protected SecurityClearance() {
        // for ORM
    }

    public LocalDate expiryDate() {
        return expiryDate;
    }

    public boolean isValid() {
        return !expiryDate.isBefore(LocalDate.now());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityClearance)) return false;
        return this.expiryDate.equals(((SecurityClearance) o).expiryDate);
    }

    @Override
    public int hashCode() {
        return expiryDate.hashCode();
    }

    @Override
    public String toString() {
        return "SecurityClearance{expiry=" + expiryDate + "}";
    }
}
