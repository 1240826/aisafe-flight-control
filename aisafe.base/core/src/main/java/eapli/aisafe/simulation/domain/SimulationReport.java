package eapli.aisafe.simulation.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;

import java.util.Objects;

/**
 * Immutable value object wrapping the SCOMP simulation output file.
 *
 * The {@code filePath} records the original path / name of the SCOMP output (.txt).
 * The {@code content} stores the full text content of that file, persisted as a CLOB.
 *
 * Invariants: filePath must not be blank; content must not be null.
 */
@Embeddable
public class SimulationReport implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "REPORT_FILE_PATH", nullable = false)
    private String filePath;

    @Lob
    @Column(name = "REPORT_CONTENT", nullable = false)
    private String content;

    /**
     * @param filePath path / name of the SCOMP output file (non-blank)
     * @param content  full text content of the file (non-null)
     */
    public SimulationReport(final String filePath, final String content) {
        Preconditions.noneNull(filePath, content);
        Invariants.ensure(!filePath.isBlank(), "SimulationReport filePath must not be blank");
        this.filePath = filePath.trim();
        this.content = content;
    }

    /** For ORM. */
    protected SimulationReport() {
    }

    public String filePath() { return filePath; }
    public String content() { return content; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SimulationReport r)) return false;
        return Objects.equals(filePath, r.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }

    @Override
    public String toString() {
        return "SimulationReport[" + filePath + "]";
    }
}
