package eapli.aisafe.manufacturer.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: name of an aircraft manufacturer. Case-insensitive equality.
 * US055.
 */
@Embeddable
public class ManufacturerName implements ValueObject, Comparable<ManufacturerName> {

    private static final long serialVersionUID = 1L;

    private String name;

    public ManufacturerName(final String name) {
        Preconditions.noneNull(name);
        Invariants.ensure(!name.isBlank(), "Manufacturer name must not be blank");
        this.name = name.trim();
    }

    protected ManufacturerName() {
        // for ORM
    }

    public static ManufacturerName valueOf(final String name) {
        return new ManufacturerName(name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ManufacturerName)) return false;
        final ManufacturerName that = (ManufacturerName) o;
        return this.name.equalsIgnoreCase(that.name);
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
    public int compareTo(final ManufacturerName other) {
        return this.name.compareToIgnoreCase(other.name);
    }
}
