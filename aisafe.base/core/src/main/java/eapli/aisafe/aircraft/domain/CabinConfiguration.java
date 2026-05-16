package eapli.aisafe.aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Value Object: cabin configuration containing 1..* SeatClass VOs.
 * Total capacity = sum of all SeatClass.numberOfSeats (client: "capacity = number of passengers").
 * US070.
 */
@Embeddable
public class CabinConfiguration implements ValueObject {

    private static final long serialVersionUID = 1L;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SEAT_CLASS",
            joinColumns = {
                @JoinColumn(name = "AIRCRAFT_REG_NUMBER", referencedColumnName = "REG_NUMBER"),
                @JoinColumn(name = "AIRCRAFT_REG_COUNTRY", referencedColumnName = "REGISTRATION_COUNTRY")
            })
    private List<SeatClass> seatClasses = new ArrayList<>();

    public CabinConfiguration(final List<SeatClass> seatClasses) {
        Preconditions.noneNull(seatClasses);
        Invariants.ensure(!seatClasses.isEmpty(), "CabinConfiguration must have at least one SeatClass");
        this.seatClasses = new ArrayList<>(seatClasses);
    }

    protected CabinConfiguration() {
        // for ORM
    }

    /** Total passenger capacity = sum of all seat class sizes. */
    public int totalCapacity() {
        return seatClasses.stream().mapToInt(SeatClass::numberOfSeats).sum();
    }

    public List<SeatClass> seatClasses() {
        return Collections.unmodifiableList(seatClasses);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CabinConfiguration)) return false;
        return this.seatClasses.equals(((CabinConfiguration) o).seatClasses);
    }

    @Override
    public int hashCode() {
        return seatClasses.hashCode();
    }

    @Override
    public String toString() {
        return "CabinConfig" + seatClasses + " (total=" + totalCapacity() + ")";
    }
}
