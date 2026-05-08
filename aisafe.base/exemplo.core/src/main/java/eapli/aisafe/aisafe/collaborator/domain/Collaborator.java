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
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Abstract aggregate root: Collaborator.
 * JPA SINGLE_TABLE inheritance — concrete subtypes: ATCCollaborator, FlightControlOperator, WeatherPerson.
 *
 * SystemUser linked via @OneToOne (cascade = NONE — SystemUser belongs to a separate framework aggregate).
 * Pattern follows eapli.base Utente.java.
 *
 * Cross-aggregate refs on ROOT (per US010 domain model):
 *  - companyId (employedBy 0..1): only ATCCollaborator; FCO/WeatherPerson leave null.
 *  - areaCode  (worksFor  0..1): only FCO/WeatherPerson; ATCCollaborator leaves null.
 *
 * US061, US062, US063, US064.
 */
@Entity
@Table(name = "COLLABORATOR")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "COLLABORATOR_TYPE")
public abstract class Collaborator implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Link to the framework SystemUser aggregate.
     * cascade = NONE: SystemUser lifecycle is managed by the framework, not by us.
     * Pattern: same as Utente.java in eapli.base.
     */
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

    /**
     * Cross-aggregate ref: company (employedBy).
     * Only ATCCollaborator; null for FCO and WeatherPerson.
     */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "COMPANY_IATA"))})
    private CompanyIATA companyId;

    /**
     * Cross-aggregate ref: air control area (worksFor).
     * Only FCO and WeatherPerson; null for ATCCollaborator.
     */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode areaCode;

    protected Collaborator(final SystemUser systemUser, final String name,
                           final String position, final SecurityClearance securityClearance,
                           final SkillsAssessment skillsAssessment,
                           final CompanyIATA companyId, final AreaCode areaCode) {
        Preconditions.noneNull(systemUser, name, position, securityClearance, skillsAssessment);
        Invariants.ensure(!name.isBlank(), "Collaborator name must not be blank");
        Invariants.ensure(!position.isBlank(), "Position must not be blank");
        this.systemUser = systemUser;
        this.name = name.trim();
        this.position = position.trim();
        this.securityClearance = securityClearance;
        this.skillsAssessment = skillsAssessment;
        this.companyId = companyId;   // nullable — ATCCollaborator only
        this.areaCode = areaCode;     // nullable — FCO/WeatherPerson only
        this.active = true;
    }

    protected Collaborator() {
        // for ORM
    }

    /** US064: disable collaborator. Irreversible. */
    public void disable() {
        if (!active) {
            throw new IllegalStateException("Collaborator is already disabled");
        }
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    /** US063: update name. */
    public void updateName(final String newName) {
        Preconditions.noneNull(newName);
        Invariants.ensure(!newName.isBlank(), "Name must not be blank");
        this.name = newName.trim();
    }

    /** US063: update position. */
    public void updatePosition(final String newPosition) {
        Preconditions.noneNull(newPosition);
        Invariants.ensure(!newPosition.isBlank(), "Position must not be blank");
        this.position = newPosition.trim();
    }

    /** US063: renew security clearance — replaces VO (immutable). */
    public void renewSecurityClearance(final SecurityClearance newClearance) {
        Preconditions.noneNull(newClearance);
        this.securityClearance = newClearance;
    }

    /** US063: update skills assessment — replaces VO (immutable). */
    public void updateSkillsAssessment(final SkillsAssessment newAssessment) {
        Preconditions.noneNull(newAssessment);
        this.skillsAssessment = newAssessment;
    }

    public Long id() { return id; }
    public SystemUser systemUser() { return systemUser; }
    public String name() { return name; }
    public String position() { return position; }
    public SecurityClearance securityClearance() { return securityClearance; }
    public SkillsAssessment skillsAssessment() { return skillsAssessment; }
    public CompanyIATA companyId() { return companyId; }
    public AreaCode areaCode() { return areaCode; }

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
        return name + " (" + position + ") [" + (active ? "ACTIVE" : "DISABLED") + "]";
    }
}
