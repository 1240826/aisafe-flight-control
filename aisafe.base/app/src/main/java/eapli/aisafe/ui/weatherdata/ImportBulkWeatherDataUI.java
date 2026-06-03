package eapli.aisafe.ui.weatherdata;

import eapli.aisafe.weatherdata.application.ImportBulkWeatherDataController;
import eapli.aisafe.weatherdata.application.ImportBulkWeatherDataController.ImportResult;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("squid:S106")
public class ImportBulkWeatherDataUI extends AbstractUI {

    private final ImportBulkWeatherDataController controller =
            new ImportBulkWeatherDataController();

    @Override
    protected boolean doShow() {
        System.out.println("\n-- CSV Format (European decimal: comma as decimal separator) --");
        System.out.println("  Lines starting with # are comments.");
        System.out.println("  Define ACA mapping:   # ACA <numericId> = <AreaCode>");
        System.out.println("  Data rows (12 columns, ; separated):");
        System.out.println("    ACA_ID;Lat1;Lon1;Lat2;Lon2;Alt_inf(ft);Alt_Sup(ft);Direction(deg);Value(kt);Day;Start;End");
        System.out.println();
        System.out.println("  Example:");
        System.out.println("    # ACA 121 = LPPC");
        System.out.println("    121;43,840454;-9,795711;40,225;-7,9501;0;1000;90;28,75;22/06/2026;05:00;08:15");

        Path csvPath = null;
        while (csvPath == null) {
            final String pathStr = Console.readLine("\nPath to CSV file");
            if (pathStr == null || pathStr.isBlank()) {
                System.out.println("  [!] No path provided.");
                return false;
            }
            csvPath = Paths.get(pathStr);
            if (!Files.exists(csvPath) || !Files.isReadable(csvPath)) {
                System.out.println("  [!] File not found or not readable: " + pathStr);
                csvPath = null;
            }
        }

        System.out.println("\n  Importing...");
        try {
            final ImportResult result = controller.importFromCsv(csvPath);
            System.out.println("\n  >> " + result);
            if (result.hasErrors()) {
                System.out.println("  Errors:");
                for (final String err : result.errors()) {
                    System.out.println("    - " + err);
                }
            }
        } catch (final IOException e) {
            System.out.println("  [!] Error reading file: " + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Import Bulk Weather Data from CSV (US042)";
    }
}
