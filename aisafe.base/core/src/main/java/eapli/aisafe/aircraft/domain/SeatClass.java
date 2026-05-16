package eapli.aisafe.aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: a seat class with its name and number of seats.
 * Invariant: numberOfSeats > 0.
 * US070.
 */
@Embeddable
public class SeatClass implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "CLASS_NAME", nullable = false)
    private String className;

    @Column(name = "NUMBER_OF_SEATS", nullable = false)
    private int numberOfSeats;

    public SeatClass(final String className, final int numberOfSeats) {
        Preconditions.noneNull(className);
        Invariants.ensure(!className.isBlank(), "Seat class name must not be blank");
        Invariants.ensure(numberOfSeats > 0, "Number of seats must be positive");
        this.className = className.trim();
        this.numberOfSeats = numberOfSeats;
    }

    protected SeatClass() {
        // for ORM
    }

    public String className() { return className; }
    public int numberOfSeats() { return numberOfSeats; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SeatClass)) return false;
        final SeatClass s = (SeatClass) o;
        return this.className.equalsIgnoreCase(s.className);
    }

    @Override
    public int hashCode() {
        return className.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return className + ":" + numberOfSeats;
    }
}
