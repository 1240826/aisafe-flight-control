package aisafe.lprog.dsl;

import aisafe.lprog.FlightPlanLexer;
import aisafe.lprog.FlightPlanParser;
import aisafe.lprog.errors.FlightPlanErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Entry point for Flight DSL processing — Sprint 2, Phase 1.
 *   CharStream → Lexer → CommonTokenStream → Parser → ParseTree
 *
 * The pattern is identical to the Expressions.java example in the slides:
 *
 *   ExpressionsLexer lexer = new ExpressionsLexer(CharStreams.fromFileName(...));
 *   CommonTokenStream tokens = new CommonTokenStream(lexer);
 *   ExpressionsParser parser = new ExpressionsParser(tokens);
 *   ParseTree tree = parser.start();
 *   System.out.println(tree.toStringTree(parser));
 *
 * Phase 1 scope (deadline: 17 May 2026):
 *   - Lexical analysis
 *   - Syntactic analysis
 *   - Error messages with line and column
 *
 * Phase 2 (Sprint 3 — 14 June 2026):
 *   - Semantic validation
 *   - Visitor (IR construction)
 *   - Listener (trace/debug)
 */
public class FlightPlanRunner {

    /**
     * Parse a .flightplan file and report lexical/syntactic errors.
     *
     * @param filePath path to the DSL file
     * @param verbose  if true, prints the parse tree (useful for testing)
     * @return true if valid, false if errors were found
     */
    public static boolean run(Path filePath, boolean verbose) throws IOException {

        // Step 1 — CharStream from file
        CharStream input = CharStreams.fromPath(filePath);

        // Step 2 — Lexer
        FlightPlanLexer lexer = new FlightPlanLexer(input);
        lexer.removeErrorListeners();
        FlightPlanErrorListener lexerErrors = new FlightPlanErrorListener("LEXER");
        lexer.addErrorListener(lexerErrors);

        // Step 3 — Token stream
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4 — Parser
        FlightPlanParser parser = new FlightPlanParser(tokens);
        parser.removeErrorListeners();
        FlightPlanErrorListener parserErrors = new FlightPlanErrorListener("PARSER");
        parser.addErrorListener(parserErrors);

        // Step 5 — Parse from the start rule
        ParseTree tree = parser.flightFile();

        // Step 6 — Report
        if (lexerErrors.hasErrors() || parserErrors.hasErrors()) {
            System.err.println("Validation FAILED: " + filePath.getFileName());
            lexerErrors.printErrors();
            parserErrors.printErrors();
            return false;
        }

        System.out.println("Validation OK: " + filePath.getFileName());

        if (verbose) {
            // Same output as the professor's examples in the slides
            System.out.println(tree.toStringTree(parser));
        }

        return true;
    }

    /**
     * Quick test from command line.
     * Usage: java FlightPlanRunner <path/to/file.flightplan>
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: FlightPlanRunner <file.flightplan>");
            System.exit(1);
        }
        boolean valid = run(Path.of(args[0]), true);
        System.exit(valid ? 0 : 1);
    }
}
