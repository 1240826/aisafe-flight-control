package eapli.aisafe.aircraftmodel.domain;

import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Local entity: a variant of an AircraftModel — a specific engine configuration.
 * Has local identity within the AircraftModel aggregate.
 * Decision (US010): "each combination is individually identifiable" → @Entity, not @Embeddable.
 * US057.
 */
@Entity
@Table(name = "AIRCRAFT_VARIANT")
public class AircraftVariant {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cross-aggregate reference to EngineModel by code. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "ENGINE_MODEL_CODE", nullable = false))})
    private EngineModelCode engineModelCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOTORIZATION_TYPE", nullable = false)
    private MotorizationType motorizationType;

    public AircraftVariant(final EngineModelCode engineModelCode, final MotorizationType motorizationType) {
        Preconditions.noneNull(engineModelCode, motorizationType);
        this.engineModelCode = engineModelCode;
        this.motorizationType = motorizationType;
    }

    protected AircraftVariant() {
        // for ORM
    }

    public Long id() {
        return id;
    }

    public EngineModelCode engineModelCode() {
        return engineModelCode;
    }

    public MotorizationType motorizationType() {
        return motorizationType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftVariant)) return false;
        final AircraftVariant other = (AircraftVariant) o;
        if (this.id != null && other.id != null) {
            return this.id.equals(other.id);
        }
        return this.engineModelCode.equals(other.engineModelCode);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : engineModelCode.hashCode();
    }

    @Override
    public String toString() {
        return engineModelCode + " (" + motorizationType + ")";
    }
}
