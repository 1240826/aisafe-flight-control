package eapli.aisafe.aircontrolarea.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: commonly-used name of an Air Control Area (unique).
 * Client clarification: "each area must have the name commonly used in air control business, must be unique".
 * US050.
 */
@Embeddable
public class AreaName implements ValueObject, Comparable<AreaName> {

    private static final long serialVersionUID = 1L;

    private String name;

    public AreaName(final String name) {
        Preconditions.noneNull(name);
        Invariants.ensure(!name.isBlank(), "Area name must not be blank");
        Invariants.ensure(name.matches(".*\\p{L}.*"),
                "Area name must contain at least one letter (e.g. 'Lisbon ACC')");
        this.name = name.trim();
    }

    protected AreaName() {
        // for ORM
    }

    public static AreaName valueOf(final String name) {
        return new AreaName(name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AreaName)) return false;
        return this.name.equalsIgnoreCase(((AreaName) o).name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final AreaName other) {
        return this.name.compareToIgnoreCase(other.name);
    }
}
