package eapli.aisafe.collaborator.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * Value Object: skills assessment with assessment date.
 * Invariant: assessmentDate must not be in the future.
 * US061, US063.
 */
@Embeddable
public class SkillsAssessment implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "ASSESSMENT_DATE", nullable = false)
    private LocalDate assessmentDate;

    public SkillsAssessment(final LocalDate assessmentDate) {
        Preconditions.noneNull(assessmentDate);
        Invariants.ensure(!assessmentDate.isAfter(LocalDate.now()),
                "Skills assessment date must not be in the future");
        this.assessmentDate = assessmentDate;
    }

    protected SkillsAssessment() {
        // for ORM
    }

    public LocalDate assessmentDate() {
        return assessmentDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillsAssessment)) return false;
        return this.assessmentDate.equals(((SkillsAssessment) o).assessmentDate);
    }

    @Override
    public int hashCode() {
        return assessmentDate.hashCode();
    }

    @Override
    public String toString() {
        return "SkillsAssessment{date=" + assessmentDate + "}";
    }
}
