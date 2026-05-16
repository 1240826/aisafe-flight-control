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
 * Companion aggregate for AISafe-specific user data that cannot be stored in the framework's
 * SystemUser: security-clearance expiry date and contact phone number.
 * Keyed by username string.
 *
 * Spec: "A user also has a name and phone number" (§3.1.1).
 * Clarification §3: security clearance applies to ALL users; expiry blocks login.
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

    /**
     * Contact phone number (e.g. "+351912345678").
     * Nullable to preserve backward-compatibility with bootstrapped users.
     * All users registered through the UI must have a phone number.
     */
    @Column(name = "PHONE", nullable = true)
    private String phone;

    /**
     * Full constructor — used by {@code AddUserController} when registering via UI.
     *
     * @param username                   unique login name
     * @param securityClearanceExpiryDate must be today or in the future
     * @param phone                      contact phone number (non-blank)
     */
    public UserSecurityProfile(final String username,
                               final LocalDate securityClearanceExpiryDate,
                               final String phone) {
        Preconditions.noneNull(username, securityClearanceExpiryDate, phone);
        Invariants.ensure(!username.isBlank(), "Username must not be blank");
        Invariants.ensure(!securityClearanceExpiryDate.isBefore(LocalDate.now()),
                "Security clearance expiry date must be today or in the future");
        Invariants.ensure(!phone.isBlank(), "Phone number must not be blank");
        this.username = username;
        this.securityClearanceExpiryDate = securityClearanceExpiryDate;
        this.phone = phone.trim();
    }

    /**
     * Backward-compatible constructor without phone (used by bootstrapper).
     * Sets phone to {@code null}.
     */
    public UserSecurityProfile(final String username, final LocalDate securityClearanceExpiryDate) {
        Preconditions.noneNull(username, securityClearanceExpiryDate);
        Invariants.ensure(!username.isBlank(), "Username must not be blank");
        Invariants.ensure(!securityClearanceExpiryDate.isBefore(LocalDate.now()),
                "Security clearance expiry date must be today or in the future");
        this.username = username;
        this.securityClearanceExpiryDate = securityClearanceExpiryDate;
        this.phone = null;
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

    /** Contact phone number. May be {@code null} for bootstrapped users. */
    public String phone() {
        return phone;
    }

    public void renewClearance(final LocalDate newExpiryDate) {
        Preconditions.noneNull(newExpiryDate);
        Invariants.ensure(!newExpiryDate.isBefore(LocalDate.now()),
                "New expiry date must be today or in the future");
        this.securityClearanceExpiryDate = newExpiryDate;
    }

    public void updatePhone(final String newPhone) {
        Preconditions.noneNull(newPhone);
        Invariants.ensure(!newPhone.isBlank(), "Phone number must not be blank");
        this.phone = newPhone.trim();
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
        return "UserSecurityProfile{username='" + username
                + "', expiryDate=" + securityClearanceExpiryDate
                + ", phone=" + (phone != null ? phone : "N/A") + "}";
    }
}
