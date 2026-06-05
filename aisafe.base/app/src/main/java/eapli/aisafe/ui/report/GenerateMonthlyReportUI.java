package eapli.aisafe.ui.report;

import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@SuppressWarnings("squid:S106")
public class GenerateMonthlyReportUI extends AbstractUI {

    private final GenerateMonthlyReportController controller =
            new GenerateMonthlyReportController();

    @Override
    protected boolean doShow() {
        YearMonth period = null;
        while (period == null) {
            final String input = Console.readLine("Year-Month (e.g. 2026-06)");
            if (input == null || input.isBlank()) {
                return false;
            }
            try {
                period = YearMonth.parse(input.trim());
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Use YYYY-MM (e.g. 2026-06).");
            }
        }

        try {
            final MonthlyReport report = controller.generateForMonth(period);
            System.out.println("\n" + report);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Generate Monthly Report (US112)";
    }
}
