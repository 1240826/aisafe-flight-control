package eapli.aisafe.flight.domain;

import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightplan.domain.FlightPlanStatus;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "FLIGHT")
public class Flight implements AggregateRoot<FlightDesignator> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightDesignator designator;

    @Column(name = "DEPARTURE_TIME", nullable = false)
    private LocalDateTime departureTime;

    /** Cross-aggregate reference: the route this flight follows (US080). */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "name", column = @Column(name = "ROUTE_NAME"))})
    private FlightRouteName routeName;

    /** Cross-aggregate reference: aircraft registration code (US080). */
    @Column(name = "AIRCRAFT_REGISTRATION", length = 20)
    private String aircraftRegistration;

    /** Cross-aggregate reference: pilot license number (US075, US080). */
    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "licenseNumber", column = @Column(name = "PILOT_LICENSE", length = 20))})
    private PilotId pilotLicense;

    /** Cross-aggregate reference: weather data associated to this flight (US082). */
    @Column(name = "WEATHER_DATA_ID")
    private Long weatherDataId;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightPlan> flightPlans = new ArrayList<>();

    protected Flight() {
    }

    /** Minimal constructor (backwards-compatible for tests — does NOT set route/aircraft/pilot). */
    public Flight(final FlightDesignator designator, final LocalDateTime departureTime) {
        this.designator = designator;
        this.departureTime = departureTime;
    }

    /**
     * Full constructor (US080/081).
     * Flight must have a route, an aircraft, and a pilot as per DSL specification.
     */
    public Flight(final FlightDesignator designator, final LocalDateTime departureTime,
                  final FlightRouteName routeName, final String aircraftRegistration,
                  final PilotId pilotLicense) {
        Preconditions.noneNull(designator, departureTime, routeName, aircraftRegistration, pilotLicense);
        this.designator = designator;
        this.departureTime = departureTime;
        this.routeName = routeName;
        this.aircraftRegistration = aircraftRegistration;
        this.pilotLicense = pilotLicense;
    }

    public LocalDateTime departureTime() {
        return departureTime;
    }

    public FlightRouteName routeName() {
        return routeName;
    }

    public String aircraftRegistration() {
        return aircraftRegistration;
    }

    public PilotId pilotLicense() {
        return pilotLicense;
    }

    public Long weatherDataId() {
        return weatherDataId;
    }

    /**
     * US082: assign weather data to this flight.
     * When weather data is assigned, any TEST_PASSED or TEST_FAILED flight plans
     * are reset to DRAFT so they can be re-tested with the new weather conditions.
     * If the same weather data is already assigned, the operation is a no-op.
     *
     * @param weatherDataId the ID of the WeatherData aggregate
     */
    public void assignWeatherData(final Long weatherDataId) {
        if (this.weatherDataId != null && this.weatherDataId.equals(weatherDataId)) {
            return;
        }
        this.weatherDataId = weatherDataId;
        for (final FlightPlan fp : flightPlans) {
            if (fp.status() == FlightPlanStatus.TEST_PASSED
                    || fp.status() == FlightPlanStatus.TEST_FAILED) {
                fp.resetToDraft();
            }
        }
    }

    public FlightPlan addFlightPlan(final FlightPlanId flightPlanId, final String dslContent) {
        if (flightPlan(flightPlanId).isPresent()) {
            throw new IllegalArgumentException(
                    "FlightPlan " + flightPlanId + " already exists for flight " + designator);
        }
        final var plan = new FlightPlan(this, flightPlanId, dslContent);
        this.flightPlans.add(plan);
        return plan;
    }

    public Optional<FlightPlan> flightPlan(final FlightPlanId flightPlanId) {
        return flightPlans.stream()
                .filter(fp -> fp.identity().equals(flightPlanId))
                .findFirst();
    }

    public List<FlightPlan> flightPlans() {
        return Collections.unmodifiableList(flightPlans);
    }

    @Override
    public FlightDesignator identity() {
        return designator;
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
        return designator.toString();
    }
}
