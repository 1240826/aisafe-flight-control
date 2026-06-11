package eapli.aisafe.ui.jfx.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExportUtil {

    private ExportUtil() {
    }

    public static void exportToCSV(final TableView<?> table, final String defaultFileName) {
        final FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        final File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try (final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            writer.write('\uFEFF');

            final var columns = table.getColumns();

            // header
            for (int i = 0; i < columns.size(); i++) {
                final TableColumn col = (TableColumn) columns.get(i);
                if (i > 0) writer.write(',');
                writer.write(escapeCsv(col.getText()));
            }
            writer.newLine();

            // data rows
            for (final Object row : table.getItems()) {
                for (int i = 0; i < columns.size(); i++) {
                    final TableColumn col = (TableColumn) columns.get(i);
                    if (i > 0) writer.write(',');
                    final Object cellValue = col.getCellData(row);
                    writer.write(escapeCsv(cellValue == null ? "" : cellValue.toString()));
                }
                writer.newLine();
            }

            NotificationManager.success("Export Complete", "Data exported to " + file.getName());

        } catch (final Exception e) {
            NotificationManager.error("Export Failed", e.getMessage());
        }
    }

    private static String escapeCsv(final String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
