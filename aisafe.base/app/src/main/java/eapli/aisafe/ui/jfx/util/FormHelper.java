package eapli.aisafe.ui.jfx.util;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;

public final class FormHelper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String FIELD_ERROR = "field-error";

    private FormHelper() {}

    public static void configureDatePicker(final DatePicker picker) {
        picker.setPromptText("YYYY-MM-DD");
        picker.setShowWeekNumbers(false);
        picker.setEditable(false);
        picker.getStyleClass().add("date-picker");
    }

    public static void configureComboBox(final ComboBox<?> combo, final String prompt) {
        combo.setPromptText(prompt);
        combo.setVisibleRowCount(10);
        combo.getStyleClass().add("searchable-combo");
        combo.setEditable(true);
        combo.getEditor().setText(prompt);
    }

    public static <T> void configureComboBox(final ComboBox<T> combo, final String prompt,
                                              final java.util.List<T> items) {
        configureComboBox(combo, prompt);
        combo.getItems().setAll(items);
    }

    public static Label createFormLabel(final String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    public static TextField createTextField(final String prompt) {
        final TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("form-field");
        return field;
    }

    public static void markFieldError(final Node field) {
        field.getStyleClass().add(FIELD_ERROR);
    }

    public static void clearFieldError(final Node field) {
        field.getStyleClass().remove(FIELD_ERROR);
    }

    public static boolean validateRequired(final TextField field, final String name) {
        if (field.getText() == null || field.getText().isBlank()) {
            markFieldError(field);
            return false;
        }
        clearFieldError(field);
        return true;
    }

    public static boolean validateRequired(final ComboBox<?> field, final String name) {
        if (field.getValue() == null) {
            markFieldError(field);
            return false;
        }
        clearFieldError(field);
        return true;
    }

    public static boolean validateRequired(final DatePicker field, final String name) {
        if (field.getValue() == null) {
            markFieldError(field);
            return false;
        }
        clearFieldError(field);
        return true;
    }

    public static boolean validatePositiveDouble(final TextField field, final String name) {
        try {
            final double val = Double.parseDouble(field.getText().trim());
            if (val <= 0) throw new NumberFormatException();
            clearFieldError(field);
            return true;
        } catch (final NumberFormatException e) {
            markFieldError(field);
            return false;
        }
    }

    public static boolean validatePositiveInt(final TextField field, final String name) {
        try {
            final int val = Integer.parseInt(field.getText().trim());
            if (val <= 0) throw new NumberFormatException();
            clearFieldError(field);
            return true;
        } catch (final NumberFormatException e) {
            markFieldError(field);
            return false;
        }
    }

    public static void addNumericConstraint(final TextField field) {
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            final String text = change.getControlNewText();
            if (text.isEmpty() || text.matches("-?\\d*\\.?\\d*")) {
                return change;
            }
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    public static void addIntegerConstraint(final TextField field) {
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            final String text = change.getControlNewText();
            if (text.isEmpty() || text.matches("-?\\d*")) {
                return change;
            }
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    public static void setMaxWidth(final Region node, final double max) {
        node.setMaxWidth(max);
        node.setPrefWidth(max);
    }

    public static Separator separator() {
        final Separator sep = new Separator();
        sep.getStyleClass().add("form-separator");
        return sep;
    }

    public static Label sectionTitle(final String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}
