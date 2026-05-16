package eapli.aisafe.enginemodel.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;

import java.util.Arrays;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root: engine model.
 * Identified by EngineModelCode.
 * EngineName + manufacturer must be unique (US056).
 * Has: rated Power, two Thrust measurements (static + cruise), TSFC, fuelType, motorizationType.
 * US056.
 */
@Entity
@Table(name = "ENGINE_MODEL")
public class EngineModel implements AggregateRoot<EngineModelCode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private EngineModelCode code;

    @Embedded
    private EngineName engineName;

    /** Manufacturer name — cross-aggregate ref by name (US056: name+manufacturer must be unique). */
    @Column(name = "MANUFACTURER_NAME", nullable = false)
    private String manufacturerName;

    @Column(name = "FUEL_TYPE", nullable = false)
    private String fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOTORIZATION_TYPE", nullable = false)
    private MotorizationType motorizationType;

    @Embedded
    private Power power;

    /** Thrust at static (take-off) conditions. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value",          column = @Column(name = "STATIC_THRUST_VALUE")),
        @AttributeOverride(name = "unit",           column = @Column(name = "STATIC_THRUST_UNIT")),
        @AttributeOverride(name = "speedReference", column = @Column(name = "STATIC_THRUST_REF"))
    })
    private Thrust staticThrust;

    /** Thrust at cruise conditions. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value",          column = @Column(name = "CRUISE_THRUST_VALUE")),
        @AttributeOverride(name = "unit",           column = @Column(name = "CRUISE_THRUST_UNIT")),
        @AttributeOverride(name = "speedReference", column = @Column(name = "CRUISE_THRUST_REF"))
    })
    private Thrust cruiseThrust;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "TSFC_VALUE")),
        @AttributeOverride(name = "unit",  column = @Column(name = "TSFC_UNIT"))
    })
    private TSFC tsfc;

    public EngineModel(final EngineModelCode code, final EngineName engineName,
                       final String manufacturerName,
                       final String fuelType, final MotorizationType motorizationType,
                       final Power power,
                       final Thrust staticThrust, final Thrust cruiseThrust,
                       final TSFC tsfc) {
        Preconditions.noneNull(code, engineName, manufacturerName, fuelType, motorizationType,
                power, staticThrust, cruiseThrust, tsfc);
        Invariants.ensure(!manufacturerName.isBlank(), "Manufacturer name must not be blank");
        final String trimmedFuel = fuelType.trim();
        Invariants.ensure(Arrays.asList(FuelType.ALL).contains(trimmedFuel),
                "Fuel type must be one of: " + String.join(", ", FuelType.ALL));
        this.code = code;
        this.engineName = engineName;
        this.manufacturerName = manufacturerName.trim();
        this.fuelType = trimmedFuel;
        this.motorizationType = motorizationType;
        this.power = power;
        this.staticThrust = staticThrust;
        this.cruiseThrust = cruiseThrust;
        this.tsfc = tsfc;
    }

    protected EngineModel() {
        // for ORM
    }

    public EngineModelCode code() { return code; }
    public EngineName engineName() { return engineName; }
    public String manufacturerName() { return manufacturerName; }
    public String fuelType() { return fuelType; }
    public MotorizationType motorizationType() { return motorizationType; }
    public Power power() { return power; }
    public Thrust staticThrust() { return staticThrust; }
    public Thrust cruiseThrust() { return cruiseThrust; }
    public TSFC tsfc() { return tsfc; }

    @Override
    public EngineModelCode identity() {
        return code;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return code + " — " + engineName + " by " + manufacturerName
                + " (" + motorizationType + ", " + fuelType + ")";
    }
}
