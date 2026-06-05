package eapli.aisafe.flight.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class FlightDesignator implements ValueObject, Comparable<FlightDesignator> {

    private static final long serialVersionUID = 1L;

    @Column(name = "FLIGHT_DESIGNATOR", nullable = false, unique = true, length = 10)
    private String designator;

    protected FlightDesignator() {
    }

    public FlightDesignator(final String designator) {
        Preconditions.noneNull(designator);
        final var trimmed = designator.trim().toUpperCase();
        Invariants.ensure(!trimmed.isBlank(), "Flight designator must not be blank");
        Invariants.ensure(trimmed.matches("[A-Z]{2}[0-9]{1,4}[A-Z]?"),
                "Flight designator must match format: xxn(n)(n)(n)(a) (e.g. TP1234)");
        this.designator = trimmed;
    }

    public static FlightDesignator valueOf(final String designator) {
        return new FlightDesignator(designator);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightDesignator other)) return false;
        return Objects.equals(designator, other.designator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(designator);
    }

    @Override
    public String toString() {
        return designator;
    }

    @Override
    public int compareTo(final FlightDesignator other) {
        return this.designator.compareTo(other.designator);
    }
}
