package eapli.aisafe.flightplan.application;

import aisafe.lprog.FlightPlanLexer;
import aisafe.lprog.FlightPlanParser;
import aisafe.lprog.errors.FlightPlanErrorListener;
import aisafe.lprog.listener.SemanticValidationListener;
import aisafe.lprog.visitor.FlightPlanPrinterVisitor;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.domain.FlightType;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collections;
import java.util.List;

@UseCaseController
public class ImportFlightPlanController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral('T')
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .toFormatter();

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final FlightRouteRepository flightRouteRepo;
    private final AircraftRepository aircraftRepo;
    private final PilotRepository pilotRepo;
    private final TransactionalContext tx;

    public ImportFlightPlanController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().newTransactionalContext());
    }

    // Test-friendly constructor: inject repos directly (no tx sharing)
    ImportFlightPlanController(final AuthorizationService authz,
                                final FlightRepository flightRepo,
                                final FlightRouteRepository flightRouteRepo,
                                final AircraftRepository aircraftRepo,
                                final PilotRepository pilotRepo) {
        this.authz = authz;
        this.tx = null;
        this.flightRepo = flightRepo;
        this.flightRouteRepo = flightRouteRepo;
        this.aircraftRepo = aircraftRepo;
        this.pilotRepo = pilotRepo;
    }

    ImportFlightPlanController(final AuthorizationService authz,
                                final TransactionalContext tx) {
        this.authz = authz;
        this.tx = tx;
        this.flightRepo = PersistenceContext.repositories().flights(tx);
        this.flightRouteRepo = PersistenceContext.repositories().flightRoutes(tx);
        this.aircraftRepo = PersistenceContext.repositories().aircraft(tx);
        this.pilotRepo = PersistenceContext.repositories().pilots(tx);
    }

    public record DslValidationResult(
            boolean lexicalPassed,
            List<String> lexicalErrors,
            boolean syntacticPassed,
            List<String> syntacticErrors,
            boolean semanticPassed,
            List<String> semanticErrors,
            boolean allPassed,
            String summary,
            FlightPlan flightPlan
    ) {
        public List<String> allErrors() {
            if (allPassed) return List.of();
            final var all = new java.util.ArrayList<String>();
            all.addAll(lexicalErrors);
            all.addAll(syntacticErrors);
            all.addAll(semanticErrors);
            return Collections.unmodifiableList(all);
        }
    }

    public DslValidationResult importFlightPlan(final String dslContent,
                                                final String sourceName,
                                                final String flightPlanIdStr) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final CharStream input = CharStreams.fromString(dslContent, sourceName);

        final FlightPlanLexer lexer = new FlightPlanLexer(input);
        lexer.removeErrorListeners();
        final FlightPlanErrorListener lexerErrors = new FlightPlanErrorListener("LEXER");
        lexer.addErrorListener(lexerErrors);

        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final FlightPlanParser parser = new FlightPlanParser(tokens);
        parser.removeErrorListeners();
        final FlightPlanErrorListener parserErrors = new FlightPlanErrorListener("PARSER");
        parser.addErrorListener(parserErrors);

        final ParseTree tree = parser.flightFile();

        final boolean lexicalPassed = !lexerErrors.hasErrors();
        final boolean syntacticPassed = !parserErrors.hasErrors();

        String summary = null;
        SemanticValidationListener semantic = null;
        boolean semanticPassed = false;
        final java.util.List<String> domainErrors = new java.util.ArrayList<>();

        if (lexicalPassed && syntacticPassed) {
            semantic = new SemanticValidationListener();
            ParseTreeWalker.DEFAULT.walk(semantic, tree);

            semanticPassed = !semantic.hasErrors();

            if (semanticPassed) {
                summary = new FlightPlanPrinterVisitor().visit(tree);
            }
        }

        final boolean allPassed = lexicalPassed && syntacticPassed && semanticPassed;

        FlightPlan flightPlan = null;
        if (allPassed) {
            try {
                if (tx != null) tx.beginTransaction();

                final FlightPlanParser.FlightFileContext ctx =
                        (FlightPlanParser.FlightFileContext) tree;
                final String flightDesigStr = extractFlightDesignator(ctx);
                final FlightDesignator designator =
                        FlightDesignator.valueOf(flightDesigStr);

                // Extract company code from designator (first 2 letters)
                final CompanyIATA company = CompanyIATA.valueOf(
                        flightDesigStr.substring(0, 2));

                // ── Validate route ───────────────────────────────────────
                final String originCode = ctx.flightDecl().routeDecl()
                        .airportCode(0).getText();
                final String destCode = ctx.flightDecl().routeDecl()
                        .airportCode(1).getText();
                final AirportIATA origin = AirportIATA.valueOf(originCode);
                final AirportIATA destination = AirportIATA.valueOf(destCode);

                final FlightRoute route = flightRouteRepo
                        .findByOriginAndDestinationAndCompany(origin, destination, company)
                        .orElse(null);
                if (route == null) {
                    domainErrors.add("No active route found for "
                            + originCode + " -> " + destCode
                            + " (company " + company + ")");
                }

                // ── Validate aircraft ────────────────────────────────────
                final String aircraftRegStr =
                        ctx.flightDecl().IDENTIFIER(0).getText();

                final var regNum = RegistrationNumber.valueOf(
                        aircraftRegStr, "PT");
                final var found = aircraftRepo.findByRegistrationNumber(regNum);
                if (found.isEmpty()) {
                    domainErrors.add("Aircraft not found: " + aircraftRegStr);
                } else if (!found.get().isActive()) {
                    domainErrors.add("Aircraft is not active: " + aircraftRegStr);
                }

                // ── Extract departure time ───────────────────────────────
                final var firstLeg = ctx.flightDecl().legDecl(0);
                final String tsStr;
                if (!firstLeg.departureDecl().departureSchedule().daySchedule().isEmpty()) {
                    tsStr = firstLeg.departureDecl()
                            .departureSchedule().daySchedule(0).TIMESTAMP().getText();
                } else {
                    tsStr = firstLeg.departureDecl()
                            .departureSchedule().TIMESTAMP().getText();
                }
                final LocalDateTime departureTime = parseTimestamp(tsStr);

                // ── Extract flight type (REGULAR or CHARTER) ──────────────
                final FlightType flightType =
                        ctx.flightDecl().flightType().REGULAR() != null
                                ? FlightType.REGULAR : FlightType.CHARTER;

                // ── Extract and validate pilot license ───────────────────
                final String pilotLicenseStr =
                        ctx.flightDecl().IDENTIFIER(1).getText();
                final PilotId pilotId = PilotId.valueOf(pilotLicenseStr);
                final var pilot = pilotRepo.findByLicenseNumber(pilotId);
                if (pilot.isEmpty()) {
                    domainErrors.add("Pilot not found: " + pilotLicenseStr);
                } else if (!pilot.get().isActive()) {
                    domainErrors.add("Pilot is not active: " + pilotLicenseStr);
                } else if (route != null && !pilot.get().company().equals(route.companyIATA())) {
                    domainErrors.add("Pilot " + pilotLicenseStr
                            + " does not belong to company " + route.companyIATA());
                }

                if (!domainErrors.isEmpty()) {
                    if (tx != null) tx.rollback();
                    return new DslValidationResult(
                            lexicalPassed, lexerErrors.getErrors(),
                            syntacticPassed, parserErrors.getErrors(),
                            false, domainErrors,
                            false, summary, null);
                }

                // ── Create or get Flight ─────────────────────────────────
                final FlightRouteName routeName = route.identity();
                Flight flight = flightRepo.ofIdentity(designator).orElse(null);
                final boolean isNewFlight = flight == null;
                if (isNewFlight) {
                    flight = new Flight(designator, departureTime,
                            routeName, aircraftRegStr, pilotId, flightType);
                }

                if (!isNewFlight) {
                    flight.updateFromDsl(departureTime, routeName,
                            aircraftRegStr, pilotId, flightType);
                }
                final String actualPlanId = "AUTO".equalsIgnoreCase(flightPlanIdStr) ? flightDesigStr : flightPlanIdStr;
                final var fpId = FlightPlanId.valueOf(actualPlanId);
                if (isNewFlight) {
                    flightPlan = flight.addFlightPlan(fpId, dslContent);
                } else {
                    flightPlan = flight.updateFlightPlan(fpId, dslContent);
                }
                flightRepo.save(flight);
                if (tx != null) tx.commit();
            } catch (final Exception e) {
                if (tx != null && tx.isActive()) tx.rollback();
                return new DslValidationResult(
                        lexicalPassed, lexerErrors.getErrors(),
                        syntacticPassed, parserErrors.getErrors(),
                        false, List.of("Error creating flight plan: " + e.getMessage()),
                        false, summary, null);
            } finally {
                if (tx != null) tx.close();
            }
        }

        return new DslValidationResult(
                lexicalPassed, lexerErrors.getErrors(),
                syntacticPassed, parserErrors.getErrors(),
                semanticPassed && domainErrors.isEmpty(),
                semantic != null ? semantic.getErrors() : List.of(),
                allPassed && domainErrors.isEmpty(),
                summary, flightPlan);
    }

    public Iterable<Flight> allFlights() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return flightRepo.findAll();
    }

    public String extractFlightDesignator(final String dslContent) {
        final CharStream input = CharStreams.fromString(dslContent);
        final FlightPlanLexer lexer = new FlightPlanLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final FlightPlanParser parser = new FlightPlanParser(tokens);
        final var ctx = parser.flightFile();
        return ctx.flightDecl().flightId().IDENTIFIER().getText();
    }

    private static String extractFlightDesignator(
            final FlightPlanParser.FlightFileContext ctx) {
        return ctx.flightDecl().flightId().IDENTIFIER().getText();
    }

    private static LocalDateTime parseTimestamp(final String ts) {
        final String withoutTz = ts.replaceAll("[+-]\\d{2}:\\d{2}$", "")
                .replace("Z", "");
        return LocalDateTime.parse(withoutTz, TIMESTAMP_FORMATTER);
    }

    public List<FlightPlan> flightPlansForFlight(final String flightDesignator) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final var designator = FlightDesignator.valueOf(flightDesignator);
        final var flight = flightRepo.ofIdentity(designator)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight not found: " + flightDesignator));
        return flight.flightPlans();
    }
}