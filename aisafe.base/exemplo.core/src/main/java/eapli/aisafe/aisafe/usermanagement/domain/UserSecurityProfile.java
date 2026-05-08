package eapli.aisafe.usermanagement.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;

/**
 * Companion aggregate for security-clearance expiry date of a SystemUser.
 * Cannot modify framework's SystemUser, so we store the expiry date here,
 * keyed by username string.
 *
 * US030 / US031.
 */
@Entity
@Table(name = "USER_SECURITY_PROFILE")
public class UserSecurityProfile implements AggregateRoot<String> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @Id
    @Column(name = "USERNAME", nullable = false, unique = true)
    private String username;

    @Column(name = "CLEARANCE_EXPIRY_DATE", nullable = false)
    private LocalDate securityClearanceExpiryDate;

    public UserSecurityProfile(final String username, final LocalDate securityClearanceExpiryDate) {
        Preconditions.noneNull(username, securityClearanceExpiryDate);
        Invariants.ensure(!username.isBlank(), "Username must not be blank");
        Invariants.ensure(!securityClearanceExpiryDate.isBefore(LocalDate.now()),
                "Security clearance expiry date must be today or in the future");
        this.username = username;
        this.securityClearanceExpiryDate = securityClearanceExpiryDate;
    }

    protected UserSecurityProfile() {
        // for ORM only
    }

    public boolean isClearanceValid() {
        return !securityClearanceExpiryDate.isBefore(LocalDate.now());
    }

    public LocalDate clearanceExpiryDate() {
        return securityClearanceExpiryDate;
    }

    public void renewClearance(final LocalDate newExpiryDate) {
        Preconditions.noneNull(newExpiryDate);
        Invariants.ensure(!newExpiryDate.isBefore(LocalDate.now()),
                "New expiry date must be today or in the future");
        this.securityClearanceExpiryDate = newExpiryDate;
    }

    @Override
    public String identity() {
        return username;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return "UserSecurityProfile{username='" + username + "', expiryDate=" + securityClearanceExpiryDate + "}";
    }
}
