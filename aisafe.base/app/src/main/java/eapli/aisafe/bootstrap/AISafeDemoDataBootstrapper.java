package eapli.aisafe.bootstrap;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.domain.AreaName;
import eapli.aisafe.aircraftmodel.domain.AerodynamicCoefficients;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.AircraftPerformance;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.aircraftmodel.domain.AircraftWeights;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.aisafe.airport.domain.Elevation;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.domain.EngineName;
import eapli.aisafe.enginemodel.domain.FuelType;
import eapli.aisafe.enginemodel.domain.Power;
import eapli.aisafe.enginemodel.domain.TSFC;
import eapli.aisafe.enginemodel.domain.Thrust;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Seeds all demo domain data for AISafe:
 * manufacturers, engine models, aircraft models (with engine variants),
 * air control areas, airports, air transport companies, and demo collaborators.
 * <p>
 * Run with: bootstrap -bootstrap:demo
 */
@SuppressWarnings("squid:S106")
public class AISafeDemoDataBootstrapper extends AbstractUserBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AISafeDemoDataBootstrapper.class);

    /**
     * Security clearance expiry for all demo collaborators: 5 years from today.
     */
    private static final LocalDate CLEARANCE_EXPIRY = LocalDate.now().plusYears(5);
    /**
     * Skills assessment date for all demo collaborators: 6 months ago.
     */
    private static final LocalDate ASSESSMENT_DATE = LocalDate.now().minusMonths(6);

    // ─── Demo DSL flight plans (valid ANTLR grammar) ─────────────────

    private static final String DSL_TP1234 =
            "flight TP1234 : regular {\n" +
            "    route { origin: LIS; destination: CDG; }\n" +
            "    aircraft: CS-TUB;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: LIS; day: Monday;   datetime: 2026-06-02T10:00+01:00; }\n" +
            "        arrival   { airport: MAD; datetime: 2026-06-02T12:30+02:00; }\n" +
            "        fuel      { quantity: 8000 kg; }\n" +
            "        segment {\n" +
            "            from      : (38.7813, -9.1359);\n" +
            "            to        : (40.4983, -3.5676);\n" +
            "            altitudes : [10000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "    leg {\n" +
            "        departure { airport: MAD; day: Monday;   datetime: 2026-06-02T14:00+02:00; }\n" +
            "        arrival   { airport: CDG; datetime: 2026-06-02T16:30+02:00; }\n" +
            "        fuel      { quantity: 9000 kg; }\n" +
            "        segment {\n" +
            "            from      : (40.4983, -3.5676);\n" +
            "            to        : (49.0097, 2.5479);\n" +
            "            altitudes : [11000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String DSL_TP5678 =
            "flight TP5678 : charter {\n" +
            "    route { origin: OPO; destination: WAW; }\n" +
            "    aircraft: CS-TUB;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: OPO; datetime: 2026-06-02T14:30+01:00; }\n" +
            "        arrival   { airport: EDDF; datetime: 2026-06-02T18:00+02:00; }\n" +
            "        fuel      { quantity: 18500 kg; }\n" +
            "        segment {\n" +
            "            from      : (41.2481, -8.6814);\n" +
            "            to        : (50.0333, 8.5706);\n" +
            "            altitudes : [11000 m WIDTH 80 m];\n" +
            "        }\n" +
            "    }\n" +
            "    leg {\n" +
            "        departure { airport: EDDF; datetime: 2026-06-02T19:30+02:00; }\n" +
            "        arrival   { airport: WAW; datetime: 2026-06-02T21:45+02:00; }\n" +
            "        fuel      { quantity: 12000 kg; }\n" +
            "        segment {\n" +
            "            from      : (50.0333, 8.5706);\n" +
            "            to        : (52.1657, 20.9671);\n" +
            "            altitudes : [10000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String DSL_TP9012 =
            "flight TP9012 : charter {\n" +
            "    route { origin: LIS; destination: OPO; }\n" +
            "    aircraft: CS-TAC;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: LIS; datetime: 2026-06-02T08:00+01:00; }\n" +
            "        arrival   { airport: OPO; datetime: 2026-06-02T09:00+01:00; }\n" +
            "        fuel      { quantity: 5000 kg; }\n" +
            "        segment {\n" +
            "            from      : (38.7813, -9.1359);\n" +
            "            to        : (41.2481, -8.6814);\n" +
            "            altitudes : [9000 m WIDTH 50 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    // ── Crossing conflict flights (same airspace, close times) ──────

    private static final String DSL_TP3000 =
            "flight TP3000 : regular {\n" +
            "    route { origin: LIS; destination: CDG; }\n" +
            "    aircraft: CS-TUB;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: LIS; day: Monday;   datetime: 2026-06-02T10:00+01:00; }\n" +
            "        arrival   { airport: MAD; datetime: 2026-06-02T12:30+02:00; }\n" +
            "        fuel      { quantity: 8000 kg; }\n" +
            "        segment {\n" +
            "            from      : (38.7813, -9.1359);\n" +
            "            to        : (40.4983, -3.5676);\n" +
            "            altitudes : [10000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "    leg {\n" +
            "        departure { airport: MAD; day: Monday;   datetime: 2026-06-02T14:00+02:00; }\n" +
            "        arrival   { airport: CDG; datetime: 2026-06-02T16:30+02:00; }\n" +
            "        fuel      { quantity: 9000 kg; }\n" +
            "        segment {\n" +
            "            from      : (40.4983, -3.5676);\n" +
            "            to        : (49.0097, 2.5479);\n" +
            "            altitudes : [11000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    // TP4000: OPO->WAW — departing same time as TP3000 but crossing path over central Portugal
    private static final String DSL_TP4000 =
            "flight TP4000 : charter {\n" +
            "    route { origin: OPO; destination: WAW; }\n" +
            "    aircraft: CS-TUB;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: OPO; datetime: 2026-06-02T10:05+01:00; }\n" +
            "        arrival   { airport: EDDF; datetime: 2026-06-02T13:35+02:00; }\n" +
            "        fuel      { quantity: 18500 kg; }\n" +
            "        segment {\n" +
            "            from      : (41.2481, -8.6814);\n" +
            "            to        : (50.0333, 8.5706);\n" +
            "            altitudes : [10000 m WIDTH 80 m];\n" +
            "        }\n" +
            "    }\n" +
            "    leg {\n" +
            "        departure { airport: EDDF; datetime: 2026-06-02T15:05+02:00; }\n" +
            "        arrival   { airport: WAW; datetime: 2026-06-02T17:20+02:00; }\n" +
            "        fuel      { quantity: 12000 kg; }\n" +
            "        segment {\n" +
            "            from      : (50.0333, 8.5706);\n" +
            "            to        : (52.1657, 20.9671);\n" +
            "            altitudes : [10000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    // TP5000: LIS->OPO — departs 5 min after TP9012 (same route) = conflict
    private static final String DSL_TP5000 =
            "flight TP5000 : charter {\n" +
            "    route { origin: LIS; destination: OPO; }\n" +
            "    aircraft: CS-TAC;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: LIS; datetime: 2026-06-02T08:05+01:00; }\n" +
            "        arrival   { airport: OPO; datetime: 2026-06-02T09:05+01:00; }\n" +
            "        fuel      { quantity: 5000 kg; }\n" +
            "        segment {\n" +
            "            from      : (38.7813, -9.1359);\n" +
            "            to        : (41.2481, -8.6814);\n" +
            "            altitudes : [9000 m WIDTH 50 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Override
    public boolean execute() {
        bootstrapManufacturers();
        bootstrapEngineModels();
        bootstrapAircraftModels();
        bootstrapAirControlAreas();
        bootstrapAirports();
        bootstrapAirTransportCompanies();
        bootstrapDemoCollaborators();
        bootstrapAircrafts();
        bootstrapFlightRoutes();
        bootstrapPilots();
        bootstrapFlightPlans();
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MANUFACTURERS
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapManufacturers() {
        saveManufacturer("Boeing", "United States");
        saveManufacturer("Airbus", "France");
        saveManufacturer("Embraer", "Brazil");
        saveManufacturer("GE Aviation", "United States");
        saveManufacturer("Rolls-Royce", "United Kingdom");
        saveManufacturer("CFM International", "United States/France");
        saveManufacturer("Pratt & Whitney", "United States");
        saveManufacturer("Engine Alliance", "United States");
    }

    private void saveManufacturer(final String name, final String country) {
        if (PersistenceContext.repositories().manufacturers().findByNameIgnoreCase(name).isPresent()) {
            LOGGER.debug("Manufacturer already exists (skipping): {}", name);
            return;
        }
        try {
            PersistenceContext.repositories().manufacturers()
                    .save(new Manufacturer(name, country));
            LOGGER.debug("Bootstrapped manufacturer: {}", name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Manufacturer concurrency conflict (skipping): {}", name);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ENGINE MODELS
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapEngineModels() {
        // GE90-94B — main engine for Boeing 777-200ER
        // Static thrust: 417 kN (93,900 lbf); Cruise thrust: ~87 kN; TSFC: 0.0173 lb/(lbf·h)
        saveEngineModel(
                "GE90B", "GE90-94B", "GE Aviation", FuelType.JET_A1, MotorizationType.TURBOFAN,
                new Power(70_000, "kW"),
                new Thrust(417.0, "kN", "static"),
                new Thrust(87.0, "kN", "cruise"),
                new TSFC(0.0173, "lb/(lbf.h)")
        );

        // Rolls-Royce Trent 970 — main engine for Airbus A380-800
        // Static thrust: 340 kN (76,500 lbf); Cruise thrust: ~62 kN; TSFC: 0.0178 lb/(lbf·h)
        saveEngineModel(
                "TRENT970", "Trent 970", "Rolls-Royce", FuelType.JET_A1, MotorizationType.TURBOFAN,
                new Power(53_000, "kW"),
                new Thrust(340.0, "kN", "static"),
                new Thrust(62.0, "kN", "cruise"),
                new TSFC(0.0178, "lb/(lbf.h)")
        );

        // CFM56-5B4 — Airbus A320 family
        saveEngineModel(
                "CFM565B4", "CFM56-5B4", "CFM International", FuelType.JET_A1, MotorizationType.TURBOFAN,
                new Power(22_000, "kW"),
                new Thrust(120.0, "kN", "static"),
                new Thrust(26.0, "kN", "cruise"),
                new TSFC(0.0198, "lb/(lbf.h)")
        );

        // CFM LEAP-1B — Boeing 737 MAX
        saveEngineModel(
                "LEAP1B", "CFM LEAP-1B", "CFM International", FuelType.JET_A1, MotorizationType.TURBOFAN,
                new Power(28_000, "kW"),
                new Thrust(130.0, "kN", "static"),
                new Thrust(27.0, "kN", "cruise"),
                new TSFC(0.0178, "lb/(lbf.h)")
        );

        // GE90-115B — Boeing 777-300ER / 777F
        saveEngineModel(
                "GE90115B", "GE90-115B", "GE Aviation", FuelType.JET_A1, MotorizationType.TURBOFAN,
                new Power(85_000, "kW"),
                new Thrust(512.0, "kN", "static"),
                new Thrust(106.0, "kN", "cruise"),
                new TSFC(0.0165, "lb/(lbf.h)")
        );

        // Rolls-Royce Trent XWB-84 — Airbus A350-900
        saveEngineModel(
                "TRENTXWB84", "Trent XWB-84", "Rolls-Royce", FuelType.JET_A1, MotorizationType.TURBOFAN,
                new Power(66_000, "kW"),
                new Thrust(375.0, "kN", "static"),
                new Thrust(72.0, "kN", "cruise"),
                new TSFC(0.0155, "lb/(lbf.h)")
        );
    }

    private void saveEngineModel(final String code, final String name,
                                 final String manufacturerName,
                                 final String fuelType, final MotorizationType type,
                                 final Power power,
                                 final Thrust staticThrust, final Thrust cruiseThrust,
                                 final TSFC tsfc) {
        if (PersistenceContext.repositories().engineModels()
                .ofIdentity(EngineModelCode.valueOf(code)).isPresent()) {
            LOGGER.debug("Engine model already exists (skipping): {}", code);
            return;
        }
        try {
            PersistenceContext.repositories().engineModels().save(
                    new EngineModel(EngineModelCode.valueOf(code), EngineName.valueOf(name),
                            manufacturerName, fuelType, type, power, staticThrust, cruiseThrust, tsfc));
            LOGGER.debug("Bootstrapped engine model: {}", code);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Engine model concurrency conflict (skipping): {}", code);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AIRCRAFT MODELS  (with engine variants)
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapAircraftModels() {
        // ── Boeing 777-200ER (B77W) ────────────────────────────────────────────
        // MTOW 347,452 kg | MZFW 229,520 kg | OEW 138,100 kg | Fuel 136,900 kg
        // Ceiling 13,100 m | Cruise 488 kt | Range 7,065 NM
        // Wing 427.8 m²
        saveAircraftModelWithVariant(
                "B77W", "Boeing 777-200ER", "Boeing", AircraftType.PASSENGER, 396,
                new AircraftWeights(138_100, 347_452, 229_520, 136_900),
                new AircraftPerformance(13_100, 488, 7_065),
                new AerodynamicCoefficients(427.8, 0.0250, 1.70),
                "GE90B", MotorizationType.TURBOFAN
        );

        // ── Airbus A380-800 (A388) ─────────────────────────────────────────────
        // MTOW 575,000 kg | MZFW 361,000 kg | OEW 276,800 kg | Fuel 257,000 kg
        // Ceiling 13,115 m | Cruise 487 kt | Range 8,208 NM
        // Wing 845.0 m²
        saveAircraftModelWithVariant(
                "A388", "Airbus A380-800", "Airbus", AircraftType.PASSENGER, 555,
                new AircraftWeights(276_800, 575_000, 361_000, 257_000),
                new AircraftPerformance(13_115, 487, 8_208),
                new AerodynamicCoefficients(845.0, 0.0220, 2.00),
                "TRENT970", MotorizationType.TURBOFAN
        );

        // ── Boeing 737-800 (B738) ──────────────────────────────────────────────
        // MTOW 79,016 kg | MZFW 62,731 kg | OEW 41,413 kg | Fuel 20,100 kg
        // Ceiling 12,497 m | Cruise 453 kt | Range 2,935 NM
        // Wing 124.6 m²
        saveAircraftModelWithVariant(
                "B738", "Boeing 737-800", "Boeing", AircraftType.PASSENGER, 189,
                new AircraftWeights(41_413, 79_016, 62_731, 20_100),
                new AircraftPerformance(12_497, 453, 2_935),
                new AerodynamicCoefficients(124.6, 0.0275, 1.55),
                "LEAP1B", MotorizationType.TURBOFAN
        );

        // ── Airbus A320-200 (A320) ─────────────────────────────────────────────
        // MTOW 77,000 kg | MZFW 61,000 kg | OEW 42,600 kg | Fuel 18,700 kg
        // Ceiling 11,900 m | Cruise 447 kt | Range 3,300 NM
        // Wing 122.6 m²
        saveAircraftModelWithVariant(
                "A320", "Airbus A320-200", "Airbus", AircraftType.PASSENGER, 180,
                new AircraftWeights(42_600, 77_000, 61_000, 18_700),
                new AircraftPerformance(11_900, 447, 3_300),
                new AerodynamicCoefficients(122.6, 0.0280, 1.50),
                "CFM565B4", MotorizationType.TURBOFAN
        );

        // ── Boeing 777-300ER (B77W_300) ───────────────────────────────────────
        // MTOW 352,441 kg | MZFW 251,290 kg | OEW 167,800 kg | Fuel 145,000 kg
        // Ceiling 13,100 m | Cruise 490 kt | Range 7,930 NM
        saveAircraftModelWithVariant(
                "B773ER", "Boeing 777-300ER", "Boeing", AircraftType.PASSENGER, 396,
                new AircraftWeights(167_800, 352_441, 251_290, 145_000),
                new AircraftPerformance(13_100, 490, 7_930),
                new AerodynamicCoefficients(427.8, 0.0240, 1.75),
                "GE90115B", MotorizationType.TURBOFAN
        );
    }

    private void saveAircraftModelWithVariant(
            final String code, final String name, final String manufacturer,
            final AircraftType type, final Integer maxPax,
            final AircraftWeights weights, final AircraftPerformance perf,
            final AerodynamicCoefficients aero,
            final String engineCode, final MotorizationType motType) {
        if (PersistenceContext.repositories().aircraftModels()
                .ofIdentity(AircraftModelCode.valueOf(code)).isPresent()) {
            LOGGER.debug("Aircraft model already exists (skipping): {}", code);
            return;
        }
        try {
            final AircraftModel model = new AircraftModel(
                    AircraftModelCode.valueOf(code), name,
                    ManufacturerName.valueOf(manufacturer),
                    type, maxPax, weights, perf, aero);
            model.addVariant(EngineModelCode.valueOf(engineCode), motType);
            PersistenceContext.repositories().aircraftModels().save(model);
            LOGGER.debug("Bootstrapped aircraft model: {} — {}", code, name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Aircraft model concurrency conflict (skipping): {}", code);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AIR CONTROL AREAS  (real-world FIR boundaries, slightly simplified)
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapAirControlAreas() {
        // Europe — Atlantic
        saveACA("LPPC", "Lisboa FIR", 25, 44, -32, -2, 13_700); // OPO LIS PDL FNC LPA
        saveACA("LECM", "Madrid FIR", 34, 46, -9, 6, 13_700); // MAD PMI
        // Europe — Continental
        saveACA("WEFIR", "West Europe FIR", 40, 58, -2, 18, 13_700); // CDG FRA BER AMS ZRH MXP FCO
        saveACA("EGTT", "London FIR", 48, 63, -13, 3, 13_700); // LHR GLA SNN
        saveACA("BIRD", "Reykjavik FIR", 60, 68, -28, -12, 13_700); // KEF
        saveACA("ENBD", "Bodo FIR", 58, 72, 4, 30, 13_700); // TRD
        // North America — USA
        saveACA("KZNE", "US Northeast ARTCC", 24, 48, -89, -62, 13_700); // JFK EWR BOS IAD ATL MIA
        saveACA("KZCH", "US Central ARTCC", 36, 48, -102, -80, 13_700); // ORD MCI
        saveACA("KZFW", "US Fort Worth ARTCC", 28, 42, -112, -88, 13_700); // DFW DEN
        saveACA("KZLA", "US West Coast ARTCC", 28, 42, -130, -108, 13_700); // LAX SFO LAS
        saveACA("PHNL", "Pacific ARTCC", 15, 28, -162, -150, 13_700); // HNL
        // North America — Canada / Mexico
        saveACA("CZEG", "Edmonton FIR", 48, 58, -125, -95, 13_700); // YYC
        saveACA("CZUL", "Montreal FIR", 42, 55, -82, -60, 13_700); // YUL
        saveACA("MMFR", "Mexico FIR", 12, 32, -122, -82, 13_700); // MEX
        // South America
        saveACA("SKED", "Colombia FIR", -5, 12, -82, -62, 13_700); // BOG
        saveACA("SBBS", "Brasilia/Atlantico FIR", -15, 8, -55, -30, 13_700); // CIG REC
        // Europe — East / Russia / Central Asia
        saveACA("UUEE", "Moscow FIR", 50, 65, 25, 55, 13_700); // SVO
        saveACA("UAAA", "Almaty FIR", 38, 52, 65, 90, 13_700); // ALA
        // Africa
        saveACA("FAJA", "Johannesburg FIR", -30, 5, 12, 45, 13_700); // JNB FIH MPM
        // Asia — East & Southeast
        saveACA("RJTT", "Tokyo FIR", 24, 47, 125, 150, 13_700); // HND
        saveACA("ZBPE", "Beijing FIR", 20, 45, 83, 130, 13_700); // HKG PEK CTU LXA
        saveACA("WSSS", "Singapore FIR", -5, 15, 95, 115, 13_700); // SIN
        // Oceania
        saveACA("YBBB", "Brisbane FIR", -45, -8, 110, 155, 13_700); // SYD ASP
    }

    private void saveACA(final String code, final String name,
                         final double minLat, final double maxLat,
                         final double minLon, final double maxLon,
                         final int maxAltMetres) {
        if (PersistenceContext.repositories().airControlAreas()
                .ofIdentity(AreaCode.valueOf(code)).isPresent()) {
            LOGGER.debug("ACA already exists (skipping): {}", code);
            return;
        }
        try {
            PersistenceContext.repositories().airControlAreas().save(
                    new AirControlArea(AreaCode.valueOf(code), new AreaName(name),
                            minLat, maxLat, minLon, maxLon, maxAltMetres));
            LOGGER.debug("Bootstrapped ACA: {}", code);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("ACA concurrency conflict (skipping): {}", code);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AIRPORTS  (50 airports from the AISafe dataset)
    // Format: saveAirport(IATA, ICAO, name, city, country, lat, lon, elevMetres, ACA_code)
    // Real-world elevations (m); minimum 1 m used where actual value is 0 or negative
    // (spec requires elevation > 0 — US052.6 acceptance test rejects non-positive values)
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapAirports() {
        // ── Portugal ──────────────────────────────────────────────────────────
        saveAirport("OPO", "LPPR", "Francisco de Sá Carneiro Airport", "Porto", "Portugal", 41.2481, -8.6814, 69, "LPPC");
        saveAirport("LIS", "LPPT", "Humberto Delgado Airport", "Lisbon", "Portugal", 38.7739, -9.1340, 114, "LPPC");
        saveAirport("PDL", "LPPD", "João Paulo II Airport", "Ponta Delgada", "Portugal", 37.7411, -25.6979, 47, "LPPC");
        saveAirport("FNC", "LPMA", "Madeira International Airport", "Funchal", "Portugal", 32.6979, -16.7745, 61, "LPPC");

        // ── Spain ─────────────────────────────────────────────────────────────
        saveAirport("MAD", "LEMD", "Adolfo Suárez Madrid-Barajas", "Madrid", "Spain", 40.4936, -3.5668, 610, "LECM");
        saveAirport("PMI", "LEPA", "Palma de Mallorca Airport", "Palma de Mallorca", "Spain", 39.5517, 2.7388, 8, "LECM");

        // ── Canary Islands (Spain) ─────────────────────────────────────────────
        saveAirport("LPA", "GCLP", "Gran Canaria Airport", "Las Palmas", "Spain", 27.9319, -15.3866, 24, "LPPC");

        // ── France ────────────────────────────────────────────────────────────
        saveAirport("CDG", "LFPG", "Charles de Gaulle Airport", "Paris", "France", 49.0097, 2.5479, 119, "WEFIR");

        // ── Germany ───────────────────────────────────────────────────────────
        saveAirport("FRA", "EDDF", "Frankfurt Airport", "Frankfurt", "Germany", 50.0379, 8.5622, 111, "WEFIR");
        saveAirport("BER", "EDDB", "Berlin Brandenburg Airport", "Berlin", "Germany", 52.3667, 13.5033, 37, "WEFIR");

        // ── Poland ────────────────────────────────────────────────────────────
        saveAirport("WAW", "EPWA", "Warsaw Chopin Airport", "Warsaw", "Poland", 52.1657, 20.9671, 110, "WEFIR");

        // ── United Kingdom ─────────────────────────────────────────────────────
        saveAirport("LHR", "EGLL", "Heathrow Airport", "London", "United Kingdom", 51.4775, -0.4614, 25, "EGTT");
        saveAirport("GLA", "EGPF", "Glasgow International Airport", "Glasgow", "United Kingdom", 55.8719, -4.4331, 8, "EGTT");

        // ── Ireland ───────────────────────────────────────────────────────────
        saveAirport("SNN", "EINN", "Shannon Airport", "Shannon", "Ireland", 52.7022, -8.9248, 15, "EGTT");

        // ── Iceland ───────────────────────────────────────────────────────────
        saveAirport("KEF", "BIKF", "Keflavik International Airport", "Reykjavik", "Iceland", 63.9850, -22.6056, 52, "BIRD");

        // ── Norway ────────────────────────────────────────────────────────────
        saveAirport("TRD", "ENVA", "Trondheim Airport Vaernes", "Trondheim", "Norway", 63.4578, 10.9241, 21, "ENBD");

        // ── Netherlands ───────────────────────────────────────────────────────
        // Note: real elevation is -11 m; spec requires > 0, using 1 m (spec limitation)
        saveAirport("AMS", "EHAM", "Amsterdam Schiphol Airport", "Amsterdam", "Netherlands", 52.3086, 4.7639, 1, "WEFIR");

        // ── Switzerland ───────────────────────────────────────────────────────
        saveAirport("ZRH", "LSZH", "Zurich Airport", "Zurich", "Switzerland", 47.4647, 8.5492, 432, "WEFIR");

        // ── Italy ─────────────────────────────────────────────────────────────
        saveAirport("MXP", "LIMC", "Milan Malpensa Airport", "Milan", "Italy", 45.6306, 8.7281, 234, "WEFIR");
        saveAirport("FCO", "LIRF", "Rome Fiumicino Airport", "Rome", "Italy", 41.8003, 12.2389, 13, "WEFIR");

        // ── USA — Northeast ───────────────────────────────────────────────────
        saveAirport("JFK", "KJFK", "John F. Kennedy International", "New York", "United States", 40.6398, -73.7789, 4, "KZNE");
        saveAirport("EWR", "KEWR", "Newark Liberty International", "Newark", "United States", 40.6925, -74.1687, 9, "KZNE");
        saveAirport("BOS", "KBOS", "Logan International Airport", "Boston", "United States", 42.3656, -71.0096, 9, "KZNE");
        saveAirport("IAD", "KIAD", "Dulles International Airport", "Washington", "United States", 38.9531, -77.4565, 91, "KZNE");

        // ── USA — Southeast ───────────────────────────────────────────────────
        saveAirport("ATL", "KATL", "Hartsfield-Jackson Atlanta", "Atlanta", "United States", 33.6367, -84.4281, 313, "KZNE");
        saveAirport("MIA", "KMIA", "Miami International Airport", "Miami", "United States", 25.7959, -80.2870, 3, "KZNE");

        // ── USA — Central ─────────────────────────────────────────────────────
        saveAirport("ORD", "KORD", "O'Hare International Airport", "Chicago", "United States", 41.9742, -87.9073, 204, "KZCH");
        saveAirport("MCI", "KMCI", "Kansas City International", "Kansas City", "United States", 39.2976, -94.7139, 311, "KZCH");

        // ── USA — Southwest ───────────────────────────────────────────────────
        saveAirport("DFW", "KDFW", "Dallas/Fort Worth International", "Dallas", "United States", 32.8975, -97.0408, 183, "KZFW");
        saveAirport("DEN", "KDEN", "Denver International Airport", "Denver", "United States", 39.8561, -104.6737, 1655, "KZFW");

        // ── USA — West Coast ──────────────────────────────────────────────────
        saveAirport("LAX", "KLAX", "Los Angeles International", "Los Angeles", "United States", 33.9425, -118.4081, 38, "KZLA");
        saveAirport("SFO", "KSFO", "San Francisco International", "San Francisco", "United States", 37.6189, -122.3750, 4, "KZLA");
        saveAirport("LAS", "KLAS", "Harry Reid International Airport", "Las Vegas", "United States", 36.0800, -115.1522, 665, "KZLA");

        // ── USA — Pacific ─────────────────────────────────────────────────────
        saveAirport("HNL", "PHNL", "Honolulu Daniel K. Inouye Intl", "Honolulu", "United States", 21.3194, -157.9225, 4, "PHNL");

        // ── Canada ────────────────────────────────────────────────────────────
        saveAirport("YYC", "CYYC", "Calgary International Airport", "Calgary", "Canada", 51.1139, -114.0200, 1084, "CZEG");
        saveAirport("YUL", "CYUL", "Montreal-Trudeau International", "Montreal", "Canada", 45.4706, -73.7408, 36, "CZUL");

        // ── Mexico ────────────────────────────────────────────────────────────
        saveAirport("MEX", "MMMX", "Mexico City International", "Mexico City", "Mexico", 19.4361, -99.0719, 2230, "MMFR");

        // ── South America ─────────────────────────────────────────────────────
        saveAirport("BOG", "SKBO", "El Dorado International Airport", "Bogota", "Colombia", 4.7016, -74.1469, 2547, "SKED");
        saveAirport("CIG", "SWCG", "Conceicao do Araguaia Airport", "Conceicao do Araguaia", "Brazil", -8.3481, -49.2997, 190, "SBBS");
        saveAirport("REC", "SBRF", "Recife/Guararapes International", "Recife", "Brazil", -8.1256, -34.9236, 10, "SBBS");

        // ── Russia ────────────────────────────────────────────────────────────
        saveAirport("SVO", "UUEE", "Sheremetyevo International", "Moscow", "Russia", 55.9736, 37.4125, 190, "UUEE");

        // ── Central Asia ──────────────────────────────────────────────────────
        saveAirport("ALA", "UAAA", "Almaty International Airport", "Almaty", "Kazakhstan", 43.3521, 77.0405, 681, "UAAA");

        // ── Africa ────────────────────────────────────────────────────────────
        saveAirport("JNB", "FAOR", "OR Tambo International Airport", "Johannesburg", "South Africa", -26.1392, 28.2460, 1694, "FAJA");
        saveAirport("FIH", "FZAA", "N'Djili International Airport", "Kinshasa", "DR Congo", -4.3857, 15.4446, 322, "FAJA");
        saveAirport("MPM", "FQMA", "Maputo International Airport", "Maputo", "Mozambique", -25.9208, 32.5726, 45, "FAJA");

        // ── Japan ─────────────────────────────────────────────────────────────
        saveAirport("HND", "RJTT", "Tokyo Haneda International", "Tokyo", "Japan", 35.5533, 139.7811, 7, "RJTT");

        // ── China / Hong Kong ─────────────────────────────────────────────────
        saveAirport("HKG", "VHHH", "Hong Kong International Airport", "Hong Kong", "China", 22.3080, 113.9185, 9, "ZBPE");
        saveAirport("PEK", "ZBAA", "Beijing Capital International", "Beijing", "China", 40.0800, 116.5843, 35, "ZBPE");
        saveAirport("CTU", "ZUUU", "Chengdu Shuangliu International", "Chengdu", "China", 30.5783, 103.9469, 494, "ZBPE");
        saveAirport("LXA", "ZULS", "Lhasa Gonggar Airport", "Lhasa", "China", 29.2978, 90.9119, 3569, "ZBPE");

        // ── Southeast Asia ────────────────────────────────────────────────────
        saveAirport("SIN", "WSSS", "Singapore Changi Airport", "Singapore", "Singapore", 1.3644, 103.9915, 7, "WSSS");

        // ── Oceania ───────────────────────────────────────────────────────────
        saveAirport("SYD", "YSSY", "Sydney Kingsford Smith Airport", "Sydney", "Australia", -33.9461, 151.1772, 21, "YBBB");
        saveAirport("ASP", "YBAS", "Alice Springs Airport", "Alice Springs", "Australia", -23.7967, 133.9022, 547, "YBBB");
    }

    private void saveAirport(final String iata, final String icao,
                             final String name, final String city, final String country,
                             final double lat, final double lon,
                             final double elevMetres,
                             final String acaCode) {
        if (PersistenceContext.repositories().airports()
                .ofIdentity(AirportIATA.valueOf(iata)).isPresent()) {
            LOGGER.debug("Airport already exists (skipping): {}", iata);
            return;
        }
        try {
            PersistenceContext.repositories().airports().save(
                    new Airport(AirportIATA.valueOf(iata), AirportICAO.valueOf(icao),
                            name, city, country, lat, lon,
                            new Elevation(elevMetres, "m"),
                            AreaCode.valueOf(acaCode)));
            LOGGER.debug("Bootstrapped airport: {} ({})", iata, icao);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Airport concurrency conflict (skipping): {}", iata);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AIR TRANSPORT COMPANIES
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapAirTransportCompanies() {
        // IATA 2-letter | ICAO 2-3 letters | Name
        saveCompany("TP", "TAP", "TAP Air Portugal");
        saveCompany("FR", "RYR", "Ryanair");
        saveCompany("BA", "BAW", "British Airways");
        saveCompany("LH", "DLH", "Lufthansa");
        saveCompany("AF", "AFR", "Air France");
        saveCompany("IB", "IBE", "Iberia");
        saveCompany("AA", "AAL", "American Airlines");
        saveCompany("DL", "DAL", "Delta Air Lines");
        saveCompany("UA", "UAL", "United Airlines");
        saveCompany("KL", "KLM", "KLM Royal Dutch Airlines");
    }

    private void saveCompany(final String iata2, final String icao23, final String name) {
        if (PersistenceContext.repositories().airTransportCompanies()
                .ofIdentity(CompanyIATA.valueOf(iata2)).isPresent()) {
            LOGGER.debug("Company already exists (skipping): {}", iata2);
            return;
        }
        try {
            PersistenceContext.repositories().airTransportCompanies().save(
                    new AirTransportCompany(
                            CompanyIATA.valueOf(iata2),
                            CompanyICAO.valueOf(icao23),
                            name));
            LOGGER.debug("Bootstrapped company: {} ({})", iata2, name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Company concurrency conflict (skipping): {}", iata2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DEMO COLLABORATORS  (SystemUsers + domain Collaborators)
    // ─────────────────────────────────────────────────────────────────────────────

    private void bootstrapDemoCollaborators() {
        bootstrapATCCollaborators();
        bootstrapFlightControlOperators();
        bootstrapWeatherPersons();
    }

    private void bootstrapATCCollaborators() {
        // ATC 1 — TAP (TP)
        final SystemUser atc1 = registerUser("atc1", TestDataConstants.PASSWORD1,
                "Carlos", "Ferreira", "atc1@aisafe.local",
                Set.of(AISafeRoles.ATC_COLLABORATOR));
        saveCollaboratorATC(atc1, "Carlos Ferreira",
                "Senior ATC Controller", CompanyIATA.valueOf("TP"));

        // ATC 2 — Lufthansa (LH)
        final SystemUser atc2 = registerUser("atc2", TestDataConstants.PASSWORD1,
                "Hans", "Mueller", "atc2@aisafe.local",
                Set.of(AISafeRoles.ATC_COLLABORATOR));
        saveCollaboratorATC(atc2, "Hans Mueller",
                "ATC Controller", CompanyIATA.valueOf("LH"));
    }

    private void saveCollaboratorATC(final SystemUser user, final String name,
                                     final String position, final CompanyIATA companyIATA) {
        if (PersistenceContext.repositories().collaborators().findBySystemUser(user).isPresent()) {
            LOGGER.debug("ATCCollaborator already exists for user {} (skipping)", user.username());
            return;
        }
        try {
            PersistenceContext.repositories().collaborators().save(
                    Collaborator.ofATC(user, name, position,
                            new SecurityClearance(CLEARANCE_EXPIRY),
                            new SkillsAssessment(ASSESSMENT_DATE),
                            companyIATA));
            LOGGER.debug("Bootstrapped ATCCollaborator: {}", name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("ATCCollaborator concurrency conflict (skipping): {}", name);
        }
    }

    private void bootstrapFlightControlOperators() {
        // FCO 1 — Lisboa FIR (LPPC)
        final SystemUser fco1 = registerUser("fco1", TestDataConstants.PASSWORD1,
                "Ana", "Santos", "fco1@aisafe.local",
                Set.of(AISafeRoles.FLIGHT_CONTROL_OPERATOR));
        saveCollaboratorFCO(fco1, "Ana Santos",
                "Flight Control Operator", AreaCode.valueOf("LPPC"));

        // FCO 2 — US Northeast (KZNE)
        final SystemUser fco2 = registerUser("fco2", TestDataConstants.PASSWORD1,
                "John", "Smith", "fco2@aisafe.local",
                Set.of(AISafeRoles.FLIGHT_CONTROL_OPERATOR));
        saveCollaboratorFCO(fco2, "John Smith",
                "Flight Control Operator", AreaCode.valueOf("KZNE"));
    }

    private void saveCollaboratorFCO(final SystemUser user, final String name,
                                     final String position, final AreaCode areaCode) {
        if (PersistenceContext.repositories().collaborators().findBySystemUser(user).isPresent()) {
            LOGGER.debug("FlightControlOperator already exists for user {} (skipping)", user.username());
            return;
        }
        try {
            PersistenceContext.repositories().collaborators().save(
                    Collaborator.ofFlightControlOperator(user, name, position,
                            new SecurityClearance(CLEARANCE_EXPIRY),
                            new SkillsAssessment(ASSESSMENT_DATE),
                            areaCode));
            LOGGER.debug("Bootstrapped FlightControlOperator: {}", name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("FlightControlOperator concurrency conflict (skipping): {}", name);
        }
    }

    private void bootstrapWeatherPersons() {
        // WeatherPerson 1 — Lisboa FIR
        final SystemUser met1 = registerUser("met1", TestDataConstants.PASSWORD1,
                "Maria", "Costa", "met1@aisafe.local",
                Set.of(AISafeRoles.WEATHER_PERSON));
        saveCollaboratorWeather(met1, "Maria Costa",
                "Meteorologist", AreaCode.valueOf("LPPC"));

        // WeatherPerson 2 — West Europe FIR
        final SystemUser met2 = registerUser("met2", TestDataConstants.PASSWORD1,
                "Klaus", "Weber", "met2@aisafe.local",
                Set.of(AISafeRoles.WEATHER_PERSON));
        saveCollaboratorWeather(met2, "Klaus Weber",
                "Senior Meteorologist", AreaCode.valueOf("WEFIR"));
    }

    private void saveCollaboratorWeather(final SystemUser user, final String name,
                                         final String position, final AreaCode areaCode) {
        if (PersistenceContext.repositories().collaborators().findBySystemUser(user).isPresent()) {
            LOGGER.debug("WeatherPerson already exists for user {} (skipping)", user.username());
            return;
        }
        try {
            PersistenceContext.repositories().collaborators().save(
                    Collaborator.ofWeatherPerson(user, name, position,
                            new SecurityClearance(CLEARANCE_EXPIRY),
                            new SkillsAssessment(ASSESSMENT_DATE),
                            areaCode));
            LOGGER.debug("Bootstrapped WeatherPerson: {}", name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("WeatherPerson concurrency conflict (skipping): {}", name);
        }
    }

    private void bootstrapAircrafts() {
        // TAP — A320 e B77W
        saveAircraft("CSTUI", "Portugal", "A320", "TP", 2,
                List.of(new SeatClass("BUSINESS", 12), new SeatClass("ECONOMY", 162)),
                LocalDate.of(2018, 3, 15));
        saveAircraft("CSTNL", "Portugal", "B77W", "TP", 2,
                List.of(new SeatClass("FIRST", 8), new SeatClass("BUSINESS", 48), new SeatClass("ECONOMY", 276)),
                LocalDate.of(2015, 6, 10));
        // US080 — Aircraft referenced by DSL flight plans
        saveAircraft("CS-TUB", "Portugal", "B738", "TP", 2,
                List.of(new SeatClass("ECONOMY", 189)),
                LocalDate.of(2020, 1, 15));
        saveAircraft("CS-TAC", "Portugal", "A320", "TP", 2,
                List.of(new SeatClass("BUSINESS", 8), new SeatClass("ECONOMY", 162)),
                LocalDate.of(2021, 6, 1));

        // Ryanair — B738
        saveAircraft("EIRKI", "Ireland", "B738", "FR", 2,
                List.of(new SeatClass("ECONOMY", 189)),
                LocalDate.of(2019, 9, 1));
        saveAircraft("EIFDA", "Ireland", "B738", "FR", 2,
                List.of(new SeatClass("ECONOMY", 189)),
                LocalDate.of(2012, 4, 20));

        // British Airways — A388 e B773ER
        saveAircraft("GBHNA", "United Kingdom", "A388", "BA", 2,
                List.of(new SeatClass("FIRST", 14), new SeatClass("BUSINESS", 56), new SeatClass("ECONOMY", 303)),
                LocalDate.of(2014, 7, 22));
        saveAircraft("GSTBD", "United Kingdom", "B773ER", "BA", 2,
                List.of(new SeatClass("BUSINESS", 48), new SeatClass("ECONOMY", 228)),
                LocalDate.of(2010, 11, 5));

        // Lufthansa — A320 e A388
        saveAircraft("DAIPB", "Germany", "A320", "LH", 2,
                List.of(new SeatClass("BUSINESS", 20), new SeatClass("ECONOMY", 138)),
                LocalDate.of(2020, 2, 14));
        saveAircraft("DAIMA", "Germany", "A388", "LH", 2,
                List.of(new SeatClass("FIRST", 8), new SeatClass("BUSINESS", 76), new SeatClass("ECONOMY", 364)),
                LocalDate.of(2011, 5, 30));
    }

    private void saveAircraft(final String regNumber, final String regCountry,
                              final String modelCode, final String companyIata,
                              final int crewMembers,
                              final List<SeatClass> seats,
                              final LocalDate registrationDate) {
        final RegistrationNumber reg = RegistrationNumber.valueOf(regNumber, regCountry);
        if (PersistenceContext.repositories().aircraft()
                .ofIdentity(reg).isPresent()) {
            LOGGER.debug("Aircraft already exists (skipping): {}", regNumber);
            return;
        }
        try {
            PersistenceContext.repositories().aircraft().save(
                    new Aircraft(reg,
                            AircraftModelCode.valueOf(modelCode),
                            CompanyIATA.valueOf(companyIata),
                            crewMembers,
                            new CabinConfiguration(seats),
                            registrationDate));
            LOGGER.debug("Bootstrapped aircraft: {}", regNumber);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Aircraft concurrency conflict (skipping): {}", regNumber);
        }
    }

    // ─── Pilots (US075) ──────────────────────────────────────────────

    private void bootstrapPilots() {
        // TAP pilots
        savePilot("P12345", "TP", Set.of("B738", "A320"), LocalDate.of(2020, 1, 15));
        savePilot("P54321", "TP", Set.of("B77W"), LocalDate.of(2019, 6, 1));
        // Ryanair pilots
        savePilot("P99999", "FR", Set.of("B738"), LocalDate.of(2021, 3, 10));
    }

    private void savePilot(final String licenseNumber, final String companyIata,
                            final Set<String> modelCodes, final LocalDate certDate) {
        final var pilotId = PilotId.valueOf(licenseNumber);
        if (PersistenceContext.repositories().pilots()
                .ofIdentity(pilotId).isPresent()) {
            LOGGER.debug("Pilot already exists (skipping): {}", licenseNumber);
            return;
        }
        try {
            final var models = new java.util.HashSet<AircraftModelCode>();
            for (final var code : modelCodes) {
                models.add(AircraftModelCode.valueOf(code));
            }
            PersistenceContext.repositories().pilots().save(
                    new Pilot(pilotId, CompanyIATA.valueOf(companyIata),
                            models, certDate));
            LOGGER.debug("Bootstrapped pilot: {}", licenseNumber);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Pilot concurrency conflict (skipping): {}", licenseNumber);
        }
    }

    // ─── Flight Routes (US080) ───────────────────────────────────────

    private void bootstrapFlightRoutes() {
        saveRoute("TP123", CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("LIS"), AirportIATA.valueOf("CDG"));
        saveRoute("TP456", CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("OPO"), AirportIATA.valueOf("WAW"));
        saveRoute("TP901", CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("LIS"), AirportIATA.valueOf("OPO"));
    }

    private void saveRoute(final String routeName, final CompanyIATA company,
                           final AirportIATA origin, final AirportIATA destination) {
        final var name = FlightRouteName.valueOf(routeName);
        if (PersistenceContext.repositories().flightRoutes()
                .existsByName(name)) {
            LOGGER.debug("FlightRoute already exists (skipping): {}", routeName);
            return;
        }
        try {
            PersistenceContext.repositories().flightRoutes().save(
                    new FlightRoute(name, company, origin, destination));
            LOGGER.debug("Bootstrapped FlightRoute: {} ({} → {})",
                    routeName, origin, destination);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("FlightRoute concurrency conflict (skipping): {}", routeName);
        }
    }

    // ─── Flight Plans ───────────────────────────────────────────────

    private void bootstrapFlightPlans() {
        saveFlight("TP1234", "FP001",
                LocalDateTime.of(2026, 6, 2, 10, 0),
                FlightRouteName.valueOf("TP123"),
                "CS-TUB", PilotId.valueOf("P12345"), DSL_TP1234);
        saveFlight("TP5678", "FP002",
                LocalDateTime.of(2026, 6, 2, 14, 30),
                FlightRouteName.valueOf("TP456"),
                "CS-TUB", PilotId.valueOf("P12345"), DSL_TP5678);
        saveFlight("TP9012", "FP003",
                LocalDateTime.of(2026, 6, 2, 8, 0),
                FlightRouteName.valueOf("TP901"),
                "CS-TAC", PilotId.valueOf("P12345"), DSL_TP9012);
        // ── Crossing / conflict flights ──────────────────────────
        saveFlight("TP3000", "FP004",
                LocalDateTime.of(2026, 6, 2, 10, 0),
                FlightRouteName.valueOf("TP123"),
                "CS-TUB", PilotId.valueOf("P12345"), DSL_TP3000);
        saveFlight("TP4000", "FP005",
                LocalDateTime.of(2026, 6, 2, 14, 30),
                FlightRouteName.valueOf("TP456"),
                "CS-TUB", PilotId.valueOf("P12345"), DSL_TP4000);
        saveFlight("TP5000", "FP006",
                LocalDateTime.of(2026, 6, 2, 8, 0),
                FlightRouteName.valueOf("TP901"),
                "CS-TAC", PilotId.valueOf("P12345"), DSL_TP5000);
    }

    private void saveFlight(final String flightDesig, final String planId,
                            final LocalDateTime departureTime,
                            final FlightRouteName routeName,
                            final String aircraftRegistration,
                            final PilotId pilotLicense,
                            final String dslContent) {
        final FlightDesignator designator = FlightDesignator.valueOf(flightDesig);
        if (PersistenceContext.repositories().flights()
                .ofIdentity(designator).isPresent()) {
            LOGGER.debug("Flight already exists (skipping): {}", flightDesig);
            return;
        }
        try {
            final Flight flight = new Flight(designator, departureTime,
                    routeName, aircraftRegistration, pilotLicense);
            flight.addFlightPlan(FlightPlanId.valueOf(planId), dslContent);
            PersistenceContext.repositories().flights().save(flight);
            LOGGER.debug("Bootstrapped flight {} with plan {}", flightDesig, planId);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Flight concurrency conflict (skipping): {}", flightDesig);
        }
    }
}
