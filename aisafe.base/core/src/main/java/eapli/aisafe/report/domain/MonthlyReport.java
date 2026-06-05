package eapli.aisafe.report.domain;

import eapli.framework.domain.model.ValueObject;

import java.time.YearMonth;

public class MonthlyReport implements ValueObject {

    private static final long serialVersionUID = 1L;

    private final YearMonth period;
    private final long totalFlights;
    private final long totalFlightPlans;
    private final long flightPlansDraft;
    private final long flightPlansInTest;
    private final long flightPlansPassed;
    private final long flightPlansFailed;
    private final long totalWeatherRecords;
    private final long totalActivePilots;
    private final long totalAircraft;

    public MonthlyReport(final YearMonth period,
                         final long totalFlights,
                         final long totalFlightPlans,
                         final long flightPlansDraft,
                         final long flightPlansInTest,
                         final long flightPlansPassed,
                         final long flightPlansFailed,
                         final long totalWeatherRecords,
                         final long totalActivePilots,
                         final long totalAircraft) {
        this.period = period;
        this.totalFlights = totalFlights;
        this.totalFlightPlans = totalFlightPlans;
        this.flightPlansDraft = flightPlansDraft;
        this.flightPlansInTest = flightPlansInTest;
        this.flightPlansPassed = flightPlansPassed;
        this.flightPlansFailed = flightPlansFailed;
        this.totalWeatherRecords = totalWeatherRecords;
        this.totalActivePilots = totalActivePilots;
        this.totalAircraft = totalAircraft;
    }

    public YearMonth period() { return period; }
    public long totalFlights() { return totalFlights; }
    public long totalFlightPlans() { return totalFlightPlans; }
    public long flightPlansDraft() { return flightPlansDraft; }
    public long flightPlansInTest() { return flightPlansInTest; }
    public long flightPlansPassed() { return flightPlansPassed; }
    public long flightPlansFailed() { return flightPlansFailed; }
    public long totalWeatherRecords() { return totalWeatherRecords; }
    public long totalActivePilots() { return totalActivePilots; }
    public long totalAircraft() { return totalAircraft; }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        final String sep = "--------------------------------------------------";
        sb.append(sep).append("\n");
        sb.append(String.format("  MONTHLY REPORT — %s%n", period));
        sb.append(sep).append("\n");
        sb.append(String.format("  Flights                : %d%n", totalFlights));
        sb.append(String.format("  Flight Plans           : %d%n", totalFlightPlans));
        sb.append(String.format("    ├─ DRAFT             : %d%n", flightPlansDraft));
        sb.append(String.format("    ├─ IN TEST           : %d%n", flightPlansInTest));
        sb.append(String.format("    ├─ TEST PASSED       : %d%n", flightPlansPassed));
        sb.append(String.format("    └─ TEST FAILED       : %d%n", flightPlansFailed));
        sb.append(String.format("  Weather Records        : %d%n", totalWeatherRecords));
        sb.append(String.format("  Active Pilots          : %d%n", totalActivePilots));
        sb.append(String.format("  Total Aircraft         : %d%n", totalAircraft));
        sb.append(sep);
        return sb.toString();
    }
}
