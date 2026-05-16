package eapli.aisafe.simulation.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root: Simulation.
 *
 * Stores the SCOMP simulation output received by the system operator.
 * The actual simulation is performed externally by SCOMP; this aggregate
 * is responsible solely for recording and persisting the result file.
 *
 * Identity: auto-generated Long (surrogate key).
 */
@Entity
@Table(name = "SIMULATION")
public class Simulation implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cross-aggregate reference to the Air Control Area being simulated. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode areaCode;

    /** Time window of the simulation. */
    @Embedded
    private SimulationTimeRange timeRange;

    /** Safety threshold used to evaluate the simulation result. */
    @Embedded
    private SafetyThreshold safetyThreshold;

    /**
     * The SCOMP output file stored verbatim.
     * filePath records the original file location; content stores the full text.
     */
    @Embedded
    private SimulationReport report;

    /** Outcome of the simulation. Defaults to PENDING until assessed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "VALIDATION_RESULT", nullable = false)
    private ValidationResult validationResult;

    public Simulation(final AreaCode areaCode,
                      final SimulationTimeRange timeRange,
                      final SafetyThreshold safetyThreshold,
                      final SimulationReport report) {
        Preconditions.noneNull(areaCode, timeRange, safetyThreshold, report);
        this.areaCode = areaCode;
        this.timeRange = timeRange;
        this.safetyThreshold = safetyThreshold;
        this.report = report;
        this.validationResult = ValidationResult.PENDING;
    }

    /** For ORM. */
    protected Simulation() {
    }

    public AreaCode areaCode() { return areaCode; }
    public SimulationTimeRange timeRange() { return timeRange; }
    public SafetyThreshold safetyThreshold() { return safetyThreshold; }
    public SimulationReport report() { return report; }
    public ValidationResult validationResult() { return validationResult; }

    /**
     * Records the outcome of the simulation assessment.
     *
     * @param result PASSED or FAILED (must not be PENDING)
     */
    public void recordValidationResult(final ValidationResult result) {
        Preconditions.noneNull(result);
        this.validationResult = result;
    }

    @Override
    public Long identity() {
        return id;
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
        return "Simulation[id=" + id + ", area=" + areaCode
                + ", " + timeRange + ", result=" + validationResult + "]";
    }
}
