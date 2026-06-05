package eapli.aisafe.flightplan.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FlightPlanId implements ValueObject, Comparable<FlightPlanId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "FLIGHT_PLAN_ID", nullable = false, unique = true, length = 20)
    private String id;

    protected FlightPlanId() {
    }

    public FlightPlanId(final String id) {
        Preconditions.noneNull(id);
        final String trimmed = id.trim().toUpperCase();
        Invariants.ensure(!trimmed.isBlank(), "FlightPlanId must not be blank");
        Invariants.ensure(trimmed.length() <= 20, "FlightPlanId must not exceed 20 characters");
        Invariants.ensure(trimmed.matches("[A-Z0-9]+"),
                "FlightPlanId must contain only alphanumeric characters");
        this.id = trimmed;
    }

    public static FlightPlanId valueOf(final String id) {
        return new FlightPlanId(id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightPlanId other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(final FlightPlanId other) {
        return this.id.compareTo(other.id);
    }
}
