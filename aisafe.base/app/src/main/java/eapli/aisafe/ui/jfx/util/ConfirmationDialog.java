package eapli.aisafe.ui.jfx.util;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public final class ConfirmationDialog {

    private ConfirmationDialog() {}

    public static boolean confirm(final String title, final String message) {
        return confirm(title, message, "Confirm", "Cancel");
    }

    public static boolean confirm(final String title, final String message,
                                   final String confirmText, final String cancelText) {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        final ButtonType confirmBtn = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        final ButtonType cancelBtn = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        final DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("confirmation-dialog");

        if (alert.getOwner() == null) {
            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getScene().getStylesheets().add(
                    ConfirmationDialog.class.getResource("/styles/dark-theme.css").toExternalForm()
            );
        }

        final Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmBtn;
    }

    public static Optional<String> prompt(final String title, final String message,
                                           final String defaultValue) {
        final TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        final DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("confirmation-dialog");

        if (dialog.getOwner() == null) {
            final Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getScene().getStylesheets().add(
                    ConfirmationDialog.class.getResource("/styles/dark-theme.css").toExternalForm()
            );
        }

        return dialog.showAndWait();
    }

    public static void error(final String title, final String message) {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        final DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("confirmation-dialog");

        if (alert.getOwner() == null) {
            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getScene().getStylesheets().add(
                    ConfirmationDialog.class.getResource("/styles/dark-theme.css").toExternalForm()
            );
        }

        alert.showAndWait();
    }

    public static void info(final String title, final String message) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        final DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("confirmation-dialog");

        if (alert.getOwner() == null) {
            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getScene().getStylesheets().add(
                    ConfirmationDialog.class.getResource("/styles/dark-theme.css").toExternalForm()
            );
        }

        alert.showAndWait();
    }
}
