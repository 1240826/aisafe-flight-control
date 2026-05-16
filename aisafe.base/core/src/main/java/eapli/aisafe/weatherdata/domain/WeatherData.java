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
 * Aggregate root: Weather observation for a specific geographic point within an ACA.
 * Per domain model (US041, US042):
 *   - belongs to an ACA (cross-aggregate ref by AreaCode)
 *   - has a sourceProvider (US042: data from multiple providers)
 *   - records a WindCondition at specific coordinates and altitude
 *   - recorded at a single instant (recordedDateTime)
 *   - temperature is recorded at that same point
 *
 * Design note: the domain model shows 1..* WindConditions per WeatherData.
 * For simplicity, this implementation records exactly one WindCondition per observation.
 * Multiple observations at different points/times are registered as separate WeatherData records.
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

    /**
     * Wind condition recorded at a specific coordinate and altitude.
     * Embeds the Coordinates_WD from the domain model.
     */
    @Embedded
    private WindCondition windCondition;

    @Column(name = "TEMPERATURE_CELSIUS", nullable = false)
    private double temperatureCelsius;

    /**
     * Data source identifier (e.g. "IPMA", "EUROCONTROL", "METAR LPPC").
     * US042: data may come from multiple providers.
     */
    @Column(name = "SOURCE_PROVIDER", nullable = false)
    private String sourceProvider;

    /** The instant at which this observation was recorded. */
    @Column(name = "RECORDED_DATE_TIME", nullable = false)
    private LocalDateTime recordedDateTime;

    public WeatherData(final AreaCode areaCode,
                       final WindCondition windCondition,
                       final double temperatureCelsius,
                       final String sourceProvider,
                       final LocalDateTime recordedDateTime) {
        Preconditions.noneNull(areaCode, windCondition, recordedDateTime, sourceProvider);
        Invariants.ensure(!sourceProvider.isBlank(), "Source provider must not be blank");
        this.areaCode = areaCode;
        this.windCondition = windCondition;
        this.temperatureCelsius = temperatureCelsius;
        this.sourceProvider = sourceProvider.trim();
        this.recordedDateTime = recordedDateTime;
    }

    protected WeatherData() {
        // for ORM
    }

    public AreaCode areaCode() { return areaCode; }
    public WindCondition windCondition() { return windCondition; }
    public double temperatureCelsius() { return temperatureCelsius; }
    public String sourceProvider() { return sourceProvider; }
    public LocalDateTime recordedDateTime() { return recordedDateTime; }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public String toString() {
        return String.format("WeatherData[area=%s, %s, T=%.1f°C, provider=%s, at=%s]",
                areaCode, windCondition, temperatureCelsius, sourceProvider, recordedDateTime);
    }
}
