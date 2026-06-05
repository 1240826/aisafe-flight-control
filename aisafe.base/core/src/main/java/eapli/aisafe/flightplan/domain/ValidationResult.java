package eapli.aisafe.flightplan.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ValidationResult implements ValueObject {

    private static final long serialVersionUID = 1L;

    private final boolean passed;
    private final List<String> reasons;

    private ValidationResult(final boolean passed, final List<String> reasons) {
        this.passed = passed;
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static ValidationResult passed() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failed(final String reason) {
        Preconditions.noneNull(reason);
        Invariants.ensure(!reason.isBlank(), "Failure reason must not be blank");
        return new ValidationResult(false, List.of(reason));
    }

    public static ValidationResult failed(final List<String> reasons) {
        Preconditions.noneNull(reasons);
        Invariants.ensure(!reasons.isEmpty(), "At least one failure reason is required");
        return new ValidationResult(false, reasons);
    }

    public boolean isPassed() {
        return passed;
    }

    public List<String> reasons() {
        return reasons;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ValidationResult other)) return false;
        return passed == other.passed && Objects.equals(reasons, other.reasons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passed, reasons);
    }

    @Override
    public String toString() {
        if (passed) return "PASSED";
        return "FAILED: " + String.join("; ", reasons);
    }
}
