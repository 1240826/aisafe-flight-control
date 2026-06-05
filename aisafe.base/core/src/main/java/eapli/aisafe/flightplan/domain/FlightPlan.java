package eapli.aisafe.flightplan.domain;

import eapli.aisafe.flight.domain.Flight;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "FLIGHT_PLAN")
public class FlightPlan {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private FlightPlanId flightPlanId;

    @ManyToOne
    @JoinColumn(name = "FLIGHT_DESIGNATOR", nullable = false)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private FlightPlanStatus status;

    @Lob
    @Column(name = "DSL_CONTENT", nullable = false)
    private String dslContent;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "LAST_TESTED_AT")
    private LocalDateTime lastTestedAt;

    @Column(name = "REPORT_FILE_PATH", length = 500)
    private String reportFilePath;

    @Lob
    @Column(name = "REPORT_CONTENT")
    private String reportContent;

    protected FlightPlan() {
    }

    public FlightPlan(final Flight flight, final FlightPlanId flightPlanId,
                      final String dslContent) {
        Preconditions.noneNull(flight, flightPlanId, dslContent);
        Invariants.ensure(!dslContent.isBlank(), "DSL content must not be blank");
        this.flight = flight;
        this.flightPlanId = flightPlanId;
        this.dslContent = dslContent;
        this.status = FlightPlanStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsInTest() {
        if (status != FlightPlanStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT flight plans can be submitted for testing");
        }
        this.status = FlightPlanStatus.IN_TEST;
    }

    public void markAsTestPassed() {
        if (status != FlightPlanStatus.IN_TEST) {
            throw new IllegalStateException(
                    "Only IN_TEST flight plans can be marked as passed");
        }
        this.status = FlightPlanStatus.TEST_PASSED;
    }

    public void markAsTestFailed() {
        if (status != FlightPlanStatus.IN_TEST) {
            throw new IllegalStateException(
                    "Only IN_TEST flight plans can be marked as failed");
        }
        this.status = FlightPlanStatus.TEST_FAILED;
    }

    public void resetToDraft() {
        this.status = FlightPlanStatus.DRAFT;
        this.lastTestedAt = null;
        this.reportFilePath = null;
        this.reportContent = null;
    }

    public void recordTestResult(final boolean passed,
                                  final String reportFilePath,
                                  final String reportContent) {
        if (passed) {
            markAsTestPassed();
        } else {
            markAsTestFailed();
        }
        this.lastTestedAt = LocalDateTime.now();
        this.reportFilePath = reportFilePath;
        this.reportContent = reportContent;
    }

    public void updateDslContent(final String newDslContent) {
        Preconditions.noneNull(newDslContent);
        Invariants.ensure(!newDslContent.isBlank(), "DSL content must not be blank");
        this.dslContent = newDslContent;
        this.status = FlightPlanStatus.DRAFT;
    }

    public FlightPlanId identity() {
        return flightPlanId;
    }

    public Flight flight() {
        return flight;
    }

    public FlightPlanStatus status() {
        return status;
    }

    public String dslContent() {
        return dslContent;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime lastTestedAt() {
        return lastTestedAt;
    }

    public String reportFilePath() {
        return reportFilePath;
    }

    public String reportContent() {
        return reportContent;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightPlan other)) return false;
        return Objects.equals(flightPlanId, other.flightPlanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightPlanId);
    }

    @Override
    public String toString() {
        return flightPlanId + " [" + status + "]";
    }
}
