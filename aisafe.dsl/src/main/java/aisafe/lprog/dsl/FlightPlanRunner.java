package aisafe.lprog.dsl;

import aisafe.lprog.FlightPlanLexer;
import aisafe.lprog.FlightPlanParser;
import aisafe.lprog.errors.FlightPlanErrorListener;
import aisafe.lprog.listener.SemanticValidationListener;
import aisafe.lprog.visitor.FlightPlanPrinterVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Entry point for Flight DSL processing.
 *
 * Three-phase pipeline (from LPROG slides):
 *
 *   Phase 1 — Lexical analysis
 *     CharStream -> FlightPlanLexer -> CommonTokenStream
 *
 *   Phase 2 — Syntactic analysis
 *     CommonTokenStream -> FlightPlanParser -> ParseTree
 *
 *   Phase 3 — Semantic analysis
 *     ParseTree -> ParseTreeWalker + SemanticValidationListener
 *     ParseTree -> FlightPlanPrinterVisitor (if verbose)
 *
 * Phases 1 and 2 use FlightPlanErrorListener (replaces ANTLR's default ConsoleErrorListener).
 * Phase 3 uses SemanticValidationListener (extends FlightPlanBaseListener).
 */
public class FlightPlanRunner {

    /**
     * Parse and validate a .flightplan file through all three phases.
     *
     * @param filePath path to the DSL source file
     * @param verbose  if true, prints the formatted flight plan summary (via Visitor)
     * @return true if the file is lexically, syntactically, and semantically valid
     */
    public static boolean run(Path filePath, boolean verbose) throws IOException {

        // ── Phase 1: Lexical analysis
        CharStream input = CharStreams.fromPath(filePath);

        FlightPlanLexer lexer = new FlightPlanLexer(input);
        lexer.removeErrorListeners();
        FlightPlanErrorListener lexerErrors = new FlightPlanErrorListener("LEXER");
        lexer.addErrorListener(lexerErrors);

        // ── Phase 2: Syntactic analysis
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        FlightPlanParser parser = new FlightPlanParser(tokens);
        parser.removeErrorListeners();
        FlightPlanErrorListener parserErrors = new FlightPlanErrorListener("PARSER");
        parser.addErrorListener(parserErrors);

        ParseTree tree = parser.flightFile();

        if (lexerErrors.hasErrors() || parserErrors.hasErrors()) {
            System.err.println("Validation FAILED (syntactic): " + filePath.getFileName());
            lexerErrors.printErrors();
            parserErrors.printErrors();
            return false;
        }

        // ── Phase 3: Semantic analysis
        SemanticValidationListener semantic = new SemanticValidationListener();
        ParseTreeWalker.DEFAULT.walk(semantic, tree);

        if (semantic.hasErrors()) {
            System.err.println("Validation FAILED (semantic): " + filePath.getFileName());
            semantic.printErrors();
            return false;
        }

        System.out.println("Validation OK: " + filePath.getFileName());

        if (verbose) {
            // Visitor traversal — formats the flight plan as a readable summary
            String summary = new FlightPlanPrinterVisitor().visit(tree);
            System.out.println(summary);
        }

        return true;
    }

    /** Quick test from command line: java FlightPlanRunner <file.flightplan> */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: FlightPlanRunner <file.flightplan>");
            System.exit(1);
        }
        System.exit(run(Path.of(args[0]), true) ? 0 : 1);
    }
}
