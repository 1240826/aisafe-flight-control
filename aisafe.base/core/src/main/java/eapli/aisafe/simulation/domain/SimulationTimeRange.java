package eapli.aisafe.simulation.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object: time window for a simulation run.
 * Invariant: endDateTime must be strictly after startDateTime.
 */
@Embeddable
public class SimulationTimeRange implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "SIM_START", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "SIM_END", nullable = false)
    private LocalDateTime endDateTime;

    public SimulationTimeRange(final LocalDateTime startDateTime, final LocalDateTime endDateTime) {
        Preconditions.noneNull(startDateTime, endDateTime);
        Invariants.ensure(endDateTime.isAfter(startDateTime),
                "endDateTime must be strictly after startDateTime");
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    /** For ORM. */
    protected SimulationTimeRange() {
    }

    public LocalDateTime startDateTime() { return startDateTime; }
    public LocalDateTime endDateTime() { return endDateTime; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SimulationTimeRange r)) return false;
        return Objects.equals(startDateTime, r.startDateTime)
                && Objects.equals(endDateTime, r.endDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDateTime, endDateTime);
    }

    @Override
    public String toString() {
        return startDateTime + " → " + endDateTime;
    }
}
