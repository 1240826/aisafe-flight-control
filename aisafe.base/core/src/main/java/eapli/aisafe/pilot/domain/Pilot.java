package eapli.aisafe.pilot.domain;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate root: Pilot.
 * A pilot belongs to a company and is certified for one or more aircraft models.
 * US075, US076, US077.
 */
@Entity
@Table(name = "PILOT")
public class Pilot implements AggregateRoot<PilotId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private PilotId pilotId;

    /** Cross-aggregate reference: the company this pilot works for. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "COMPANY_IATA"))})
    private CompanyIATA company;

    /** Cross-aggregate reference: aircraft models this pilot is certified to fly. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "PILOT_CERTIFIED_MODEL", joinColumns = @JoinColumn(name = "LICENSE_NUMBER"))
    private Set<AircraftModelCode> certifiedModels = new HashSet<>();

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "CERTIFICATION_DATE", nullable = false)
    private LocalDate certificationDate;

    protected Pilot() {
    }

    /**
     * US075: add a new pilot.
     *
     * @param pilotId           unique license number
     * @param company           the company this pilot belongs to
     * @param certifiedModels   set of aircraft models the pilot is certified to fly
     * @param certificationDate date of certification
     */
    public Pilot(final PilotId pilotId, final CompanyIATA company,
                 final Set<AircraftModelCode> certifiedModels,
                 final LocalDate certificationDate) {
        Preconditions.noneNull(pilotId, company, certifiedModels, certificationDate);
        Invariants.ensure(!certifiedModels.isEmpty(),
                "Pilot must be certified for at least one aircraft model");
        Invariants.ensure(!certificationDate.isAfter(LocalDate.now()),
                "Certification date must not be in the future");
        this.pilotId = pilotId;
        this.company = company;
        this.certifiedModels = new HashSet<>(certifiedModels);
        this.certificationDate = certificationDate;
        this.active = true;
    }

    /**
     * US077: deactivate this pilot.
     * Cannot deactivate if already inactive.
     * Business rule check for flight plan assignment is done by the controller.
     */
    public void deactivate() {
        Invariants.ensure(active, "Pilot is already inactive");
        this.active = false;
    }

    /**
     * Activates this pilot.
     * Cannot activate if already active.
     */
    public void activate() {
        Invariants.ensure(!active, "Pilot is already active");
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public PilotId pilotId() {
        return pilotId;
    }

    public CompanyIATA company() {
        return company;
    }

    public Set<AircraftModelCode> certifiedModels() {
        return Collections.unmodifiableSet(certifiedModels);
    }

    public LocalDate certificationDate() {
        return certificationDate;
    }

    @Override
    public PilotId identity() {
        return pilotId;
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
        return pilotId + " [" + company + "] " + (active ? "ACTIVE" : "INACTIVE");
    }
}
