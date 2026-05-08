package eapli.aisafe.enginemodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object: engine model name.
 * Name + manufacturer must be unique (US056).
 */
@Embeddable
public class EngineName implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "ENGINE_NAME", nullable = false)
    private String name;

    public EngineName(final String name) {
        Preconditions.noneNull(name);
        Invariants.ensure(!name.isBlank(), "Engine name must not be blank");
        this.name = name.trim();
    }

    protected EngineName() {
        // for ORM
    }

    public static EngineName valueOf(final String name) {
        return new EngineName(name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EngineName)) return false;
        return this.name.equalsIgnoreCase(((EngineName) o).name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
