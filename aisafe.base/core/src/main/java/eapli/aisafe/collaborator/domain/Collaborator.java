package eapli.aisafe.collaborator.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root: Collaborator.
 * Single concrete class — no inheritance. Role variant identified by {@link CollaboratorType}.
 * Use the static factory methods to create instances:
 * {@link #ofATC}, {@link #ofFlightControlOperator}, {@link #ofWeatherPerson}.
 *
 * Cross-aggregate refs (one or the other, never both):
 *  - companyId: only for CollaboratorType.ATC
 *  - areaCode:  only for CollaboratorType.FCO and CollaboratorType.WEATHER
 *
 * US061, US062, US063, US064.
 */
@Entity
@Table(name = "COLLABORATOR")
public class Collaborator implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "COLLABORATOR_TYPE", nullable = false)
    private CollaboratorType collaboratorType;

    @OneToOne()
    private SystemUser systemUser;

    @Column(name = "COLLAB_NAME", nullable = false)
    private String name;

    @Column(name = "POSITION", nullable = false)
    private String position;

    @Embedded
    private SecurityClearance securityClearance;

    @Embedded
    private SkillsAssessment skillsAssessment;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "PHONE")
    private String phone;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "COMPANY_IATA"))})
    private CompanyIATA companyId;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode areaCode;

    // ── Factory methods ───────────────────────────────────────────────────────

    public static Collaborator ofATC(final SystemUser systemUser, final String name,
                                      final String position, final SecurityClearance securityClearance,
                                      final SkillsAssessment skillsAssessment,
                                      final CompanyIATA companyId) {
        Preconditions.noneNull(companyId, "ATC Collaborator requires a company");
        return new Collaborator(systemUser, name, position, securityClearance, skillsAssessment,
                companyId, null, CollaboratorType.ATC);
    }

    public static Collaborator ofFlightControlOperator(final SystemUser systemUser, final String name,
                                                        final String position, final SecurityClearance securityClearance,
                                                        final SkillsAssessment skillsAssessment,
                                                        final AreaCode areaCode) {
        Preconditions.noneNull(areaCode, "Flight Control Operator requires an area code");
        return new Collaborator(systemUser, name, position, securityClearance, skillsAssessment,
                null, areaCode, CollaboratorType.FCO);
    }

    public static Collaborator ofWeatherPerson(final SystemUser systemUser, final String name,
                                                final String position, final SecurityClearance securityClearance,
                                                final SkillsAssessment skillsAssessment,
                                                final AreaCode areaCode) {
        Preconditions.noneNull(areaCode, "Weather Person requires an area code");
        return new Collaborator(systemUser, name, position, securityClearance, skillsAssessment,
                null, areaCode, CollaboratorType.WEATHER);
    }

    // ── Private constructor (used by factory methods only) ────────────────────

    private Collaborator(final SystemUser systemUser, final String name,
                         final String position, final SecurityClearance securityClearance,
                         final SkillsAssessment skillsAssessment,
                         final CompanyIATA companyId, final AreaCode areaCode,
                         final CollaboratorType collaboratorType) {
        Preconditions.noneNull(systemUser, name, position, securityClearance, skillsAssessment, collaboratorType);
        Invariants.ensure(!name.isBlank(), "Collaborator name must not be blank");
        Invariants.ensure(name.matches(".*\\p{L}.*"),
                "Collaborator name must contain at least one letter");
        Invariants.ensure(!position.isBlank(), "Position must not be blank");
        Invariants.ensure(position.matches(".*\\p{L}.*"),
                "Position must contain at least one letter (e.g. 'ATC Controller')");
        this.systemUser = systemUser;
        this.name = name.trim();
        this.position = position.trim();
        this.securityClearance = securityClearance;
        this.skillsAssessment = skillsAssessment;
        this.companyId = companyId;
        this.areaCode = areaCode;
        this.collaboratorType = collaboratorType;
        this.active = true;
    }

    protected Collaborator() {
        // for ORM
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public void disable() {
        if (!active) {
            throw new IllegalStateException("Collaborator is already disabled");
        }
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void updateName(final String newName) {
        Preconditions.noneNull(newName);
        Invariants.ensure(!newName.isBlank(), "Name must not be blank");
        Invariants.ensure(newName.matches(".*\\p{L}.*"),
                "Collaborator name must contain at least one letter");
        this.name = newName.trim();
    }

    public void updatePosition(final String newPosition) {
        Preconditions.noneNull(newPosition);
        Invariants.ensure(!newPosition.isBlank(), "Position must not be blank");
        Invariants.ensure(newPosition.matches(".*\\p{L}.*"),
                "Position must contain at least one letter (e.g. 'ATC Controller')");
        this.position = newPosition.trim();
    }

    public void renewSecurityClearance(final SecurityClearance newClearance) {
        Preconditions.noneNull(newClearance);
        this.securityClearance = newClearance;
    }

    public void updateSkillsAssessment(final SkillsAssessment newAssessment) {
        Preconditions.noneNull(newAssessment);
        this.skillsAssessment = newAssessment;
    }

    public void updatePhone(final String newPhone) {
        this.phone = (newPhone == null || newPhone.isBlank()) ? null : newPhone.trim();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Long id() { return id; }
    public CollaboratorType collaboratorType() { return collaboratorType; }
    public SystemUser systemUser() { return systemUser; }
    public String name() { return name; }
    public String position() { return position; }
    public SecurityClearance securityClearance() { return securityClearance; }
    public SkillsAssessment skillsAssessment() { return skillsAssessment; }
    public CompanyIATA companyId() { return companyId; }
    public AreaCode areaCode() { return areaCode; }
    public String phone() { return phone; }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public String toString() {
        return name + " (" + position + ") [" + collaboratorType + " | " + (active ? "ACTIVE" : "DISABLED") + "]";
    }
}
