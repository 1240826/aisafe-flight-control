package eapli.aisafe.weatherdata.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Invariants;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Aggregate root: Weather observation for a sub-area of an ACA.
 * Client clarification: uses validFrom/validTo pair (NOT a single recordedDateTime).
 * Client clarification: weather is for rectangular sub-areas of ACAs.
 * US041.
 */
@Entity
@Table(name = "WEATHER_DATA")
public class WeatherData implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cross-aggregate reference by AreaCode. */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "code", column = @Column(name = "AREA_CODE"))})
    private AreaCode areaCode;

    @Embedded
    private WeatherSubArea subArea;

    @Embedded
    private WindCondition windCondition;

    @Column(name = "TEMPERATURE_CELSIUS", nullable = false)
    private double temperatureCelsius;

    @Column(name = "VALID_FROM", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "VALID_TO", nullable = false)
    private LocalDateTime validTo;

    public WeatherData(final AreaCode areaCode, final WeatherSubArea subArea,
                       final WindCondition windCondition, final double temperatureCelsius,
                       final LocalDateTime validFrom, final LocalDateTime validTo) {
        Preconditions.noneNull(areaCode, subArea, windCondition, validFrom, validTo);
        Invariants.ensure(validTo.isAfter(validFrom), "validTo must be after validFrom");
        this.areaCode = areaCode;
        this.subArea = subArea;
        this.windCondition = windCondition;
        this.temperatureCelsius = temperatureCelsius;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    protected WeatherData() {
        // for ORM
    }

    public AreaCode areaCode() { return areaCode; }
    public WeatherSubArea subArea() { return subArea; }
    public WindCondition windCondition() { return windCondition; }
    public double temperatureCelsius() { return temperatureCelsius; }
    public LocalDateTime validFrom() { return validFrom; }
    public LocalDateTime validTo() { return validTo; }

    @Override
    public Long identity() {
        return id;
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
        return "WeatherData[area=" + areaCode + ", " + subArea + ", " + windCondition
                + ", T=" + temperatureCelsius + "°C, " + validFrom + " → " + validTo + "]";
    }
}
