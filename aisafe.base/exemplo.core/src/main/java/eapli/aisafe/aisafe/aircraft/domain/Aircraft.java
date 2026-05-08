package eapli.aisafe.aircraft.domain;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;


/**
 * Aggregate root: Aircraft.
 * Uniquely identified by RegistrationNumber (worldwide).
 * Initial status is always ACTIVE (invariant in constructor).
 * decommission() transitions ACTIVE → DECOMMISSIONED (irreversible).
 * US070, US071, US072.
 */
@Entity
@Table(name = "AIRCRAFT")
public class Aircraft implements AggregateRoot<RegistrationNumber> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private RegistrationNumber registrationNumber;

    /** Cross-aggregate reference by AircraftModelCode. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AIRCRAFT_MODEL_CODE"))})
    private AircraftModelCode aircraftModelCode;

    /** Cross-aggregate reference by CompanyIATA. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "iataCode", column = @Column(name = "COMPANY_IATA"))})
    private CompanyIATA companyId;

    @Column(name = "FLIGHT_CREW_MEMBERS", nullable = false)
    private int numberOfFlightCrewMembers;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = "OPERATIONAL_STATUS", nullable = false)
    private OperationalStatus operationalStatus;

    /**
     * US070: create a new aircraft. Status is always ACTIVE on creation.
     */
    public Aircraft(final RegistrationNumber registrationNumber,
                    final AircraftModelCode aircraftModelCode,
                    final CompanyIATA companyId,
                    final int numberOfFlightCrewMembers,
                    final CabinConfiguration cabinConfiguration) {
        Preconditions.noneNull(registrationNumber, aircraftModelCode, companyId, cabinConfiguration);
        Invariants.ensure(numberOfFlightCrewMembers > 0, "Number of flight crew members must be positive");
        this.registrationNumber = registrationNumber;
        this.aircraftModelCode = aircraftModelCode;
        this.companyId = companyId;
        this.numberOfFlightCrewMembers = numberOfFlightCrewMembers;
        this.cabinConfiguration = cabinConfiguration;
        this.operationalStatus = OperationalStatus.ACTIVE;
    }

    protected Aircraft() {
        // for ORM
    }

    /**
     * US071: decommission this aircraft.
     * Throws IllegalStateException if already decommissioned.
     */
    public void decommission() {
        if (operationalStatus == OperationalStatus.DECOMMISSIONED) {
            throw new IllegalStateException("Aircraft is already decommissioned");
        }
        this.operationalStatus = OperationalStatus.DECOMMISSIONED;
    }

    public boolean isActive() {
        return operationalStatus == OperationalStatus.ACTIVE;
    }

    public OperationalStatus operationalStatus() { return operationalStatus; }
    public RegistrationNumber registrationNumber() { return registrationNumber; }
    public AircraftModelCode aircraftModelCode() { return aircraftModelCode; }
    public CompanyIATA companyId() { return companyId; }
    public int numberOfFlightCrewMembers() { return numberOfFlightCrewMembers; }
    public CabinConfiguration cabinConfiguration() { return cabinConfiguration; }

    /** US072: total passenger capacity. */
    public int totalCapacity() {
        return cabinConfiguration.totalCapacity();
    }

    @Override
    public RegistrationNumber identity() {
        return registrationNumber;
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
        return registrationNumber + " [" + aircraftModelCode + "] — " + operationalStatus
                + " | crew=" + numberOfFlightCrewMembers
                + " | capacity=" + totalCapacity();
    }
}
