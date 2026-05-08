package eapli.aisafe.aircraftmodel.domain;

import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root: aircraft model (e.g. Boeing 737-800).
 * Composed of: weights, performance, aerodynamic coefficients, aircraft type.
 * Contains local AircraftVariant entities (engine + motorization combos).
 * Invariant: all variants must have the same MotorizationType.
 * US055, US057, US058.
 */
@Entity
@Table(name = "AIRCRAFT_MODEL")
public class AircraftModel implements AggregateRoot<AircraftModelCode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private AircraftModelCode code;

    @Column(name = "MODEL_NAME", nullable = false)
    private String name;

    /** Cross-aggregate reference by manufacturer name (case-insensitive identity). */
    @Column(name = "MANUFACTURER_NAME", nullable = false)
    private String manufacturerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "AIRCRAFT_TYPE", nullable = false)
    private AircraftType aircraftType;

    @Column(name = "MAX_PASSENGERS")
    private Integer maxPassengers;

    @Embedded
    private AircraftWeights aircraftWeights;

    @Embedded
    private AircraftPerformance aircraftPerformance;

    @Embedded
    private AerodynamicCoefficients aerodynamicCoefficients;

    /**
     * AircraftVariant is a local @Entity — owned entirely by this aggregate.
     * cascade=ALL + orphanRemoval ensures lifecycle is controlled here.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "AIRCRAFT_MODEL_CODE")
    private List<AircraftVariant> variants = new ArrayList<>();

    public AircraftModel(final AircraftModelCode code, final String name,
                         final ManufacturerName manufacturerName,
                         final AircraftType aircraftType,
                         final Integer maxPassengers,
                         final AircraftWeights aircraftWeights,
                         final AircraftPerformance aircraftPerformance,
                         final AerodynamicCoefficients aerodynamicCoefficients) {
        Preconditions.noneNull(code, name, manufacturerName, aircraftType,
                aircraftWeights, aircraftPerformance, aerodynamicCoefficients);
        Invariants.ensure(!name.isBlank(), "Aircraft model name must not be blank");
        if (maxPassengers != null) {
            Invariants.ensure(maxPassengers > 0, "Max passengers must be positive");
        }
        this.code = code;
        this.name = name.trim();
        this.manufacturerName = manufacturerName.toString();
        this.aircraftType = aircraftType;
        this.maxPassengers = maxPassengers;
        this.aircraftWeights = aircraftWeights;
        this.aircraftPerformance = aircraftPerformance;
        this.aerodynamicCoefficients = aerodynamicCoefficients;
    }

    protected AircraftModel() {
        // for ORM
    }

    /**
     * Add an engine variant. All variants must share the same MotorizationType.
     * US057.
     */
    public void addVariant(final EngineModelCode engineModelCode, final MotorizationType motorizationType) {
        Preconditions.noneNull(engineModelCode, motorizationType);
        final boolean duplicate = variants.stream()
                .anyMatch(v -> v.engineModelCode().equals(engineModelCode));
        Invariants.ensure(!duplicate, "Variant with engine " + engineModelCode + " already exists");
        if (!variants.isEmpty()) {
            Invariants.ensure(variants.get(0).motorizationType() == motorizationType,
                    "All variants must have the same MotorizationType");
        }
        variants.add(new AircraftVariant(engineModelCode, motorizationType));
    }

    /**
     * Remove a variant by engine model code.
     * US058.
     */
    public void removeVariant(final EngineModelCode engineModelCode) {
        Preconditions.noneNull(engineModelCode);
        final boolean removed = variants.removeIf(v -> v.engineModelCode().equals(engineModelCode));
        Invariants.ensure(removed, "No variant found for engine " + engineModelCode);
    }

    public AircraftModelCode code() { return code; }
    public String name() { return name; }
    public String manufacturerName() { return manufacturerName; }
    public AircraftType aircraftType() { return aircraftType; }
    public Integer maxPassengers() { return maxPassengers; }
    public AircraftWeights aircraftWeights() { return aircraftWeights; }
    public AircraftPerformance aircraftPerformance() { return aircraftPerformance; }
    public AerodynamicCoefficients aerodynamicCoefficients() { return aerodynamicCoefficients; }
    public List<AircraftVariant> variants() { return Collections.unmodifiableList(variants); }

    @Override
    public AircraftModelCode identity() { return code; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public String toString() {
        return code + " — " + name + " (" + manufacturerName + ", " + aircraftType + ")";
    }
}
