package aisafe.lprog.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom ANTLR error listener for the Flight DSL.
 *
 * Replaces ANTLR's default ConsoleErrorListener so we control
 * how errors are formatted and stored.
 *
 * Required by the LPROG spec (section 1.5):
 *   "Produce clear and informative error messages, including:
 *    - Error type
 *    - Line and column information"
 *
 * Usage (same pattern as the LPROG slides):
 *
 *   FlightPlanErrorListener errors = new FlightPlanErrorListener("LEXER");
 *   lexer.removeErrorListeners();
 *   lexer.addErrorListener(errors);
 */
public class FlightPlanErrorListener extends BaseErrorListener {

    private final String phase;                  // "LEXER" or "PARSER"
    private final List<String> errors = new ArrayList<>();

    public FlightPlanErrorListener(String phase) {
        this.phase = phase;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {

        // Format: [LEXER] line 3:10 - token recognition error at: '@'
        String error = String.format("[%s] line %d:%d - %s",
                phase, line, charPositionInLine, msg);
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void printErrors() {
        errors.forEach(System.err::println);
    }
}
