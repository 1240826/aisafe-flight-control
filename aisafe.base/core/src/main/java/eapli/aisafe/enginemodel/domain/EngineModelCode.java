package eapli.aisafe.enginemodel.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

/**
 * Value Object: unique code identifying an engine model.
 * US056.
 */
@Embeddable
public class EngineModelCode implements ValueObject, Comparable<EngineModelCode> {

    private static final long serialVersionUID = 1L;

    private String code;

    public EngineModelCode(final String code) {
        Preconditions.noneNull(code);
        Invariants.ensure(!code.isBlank(), "Engine model code must not be blank");
        this.code = code.trim().toUpperCase();
    }

    protected EngineModelCode() {
        // for ORM
    }

    public static EngineModelCode valueOf(final String code) {
        return new EngineModelCode(code);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EngineModelCode)) return false;
        return this.code.equals(((EngineModelCode) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public int compareTo(final EngineModelCode other) {
        return this.code.compareTo(other.code);
    }
}
