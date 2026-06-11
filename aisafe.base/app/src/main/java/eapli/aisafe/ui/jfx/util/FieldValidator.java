package eapli.aisafe.ui.jfx.util;

import javafx.scene.control.*;

import java.util.Arrays;

public final class FieldValidator {

    private FieldValidator() {}

    public static void onRequired(final TextField field, final Label errorLabel, final String fieldName) {
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                showError(field, errorLabel, fieldName + " is required.");
            } else {
                clearError(field, errorLabel);
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onNumeric(final TextField field, final Label errorLabel, final String fieldName) {
        FormHelper.addNumericConstraint(field);
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                clearError(field, errorLabel);
                return;
            }
            try {
                Double.parseDouble(val);
                clearError(field, errorLabel);
            } catch (final NumberFormatException e) {
                showError(field, errorLabel, fieldName + " must be a valid number.");
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onRequiredNumeric(final TextField field, final Label errorLabel, final String fieldName) {
        FormHelper.addNumericConstraint(field);
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                showError(field, errorLabel, fieldName + " is required.");
                return;
            }
            try {
                final double d = Double.parseDouble(val);
                if (d <= 0) {
                    showError(field, errorLabel, fieldName + " must be positive.");
                    return;
                }
                clearError(field, errorLabel);
            } catch (final NumberFormatException e) {
                showError(field, errorLabel, fieldName + " must be a valid number.");
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onRequiredInteger(final TextField field, final Label errorLabel, final String fieldName) {
        FormHelper.addIntegerConstraint(field);
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                showError(field, errorLabel, fieldName + " is required.");
                return;
            }
            try {
                final int i = Integer.parseInt(val);
                if (i <= 0) {
                    showError(field, errorLabel, fieldName + " must be positive.");
                    return;
                }
                clearError(field, errorLabel);
            } catch (final NumberFormatException e) {
                showError(field, errorLabel, fieldName + " must be a whole number.");
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onPattern(final TextField field, final Label errorLabel,
                                  final String regex, final String message) {
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                clearError(field, errorLabel);
                return;
            }
            if (val.matches(regex)) {
                clearError(field, errorLabel);
            } else {
                showError(field, errorLabel, message);
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onMinLength(final TextField field, final Label errorLabel,
                                    final int min, final String fieldName) {
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                clearError(field, errorLabel);
                return;
            }
            if (val.trim().length() < min) {
                showError(field, errorLabel, fieldName + " must be at least " + min + " characters.");
            } else {
                clearError(field, errorLabel);
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onMaxLength(final TextField field, final Label errorLabel,
                                    final int max, final String fieldName) {
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                clearError(field, errorLabel);
                return;
            }
            if (val.trim().length() > max) {
                showError(field, errorLabel, fieldName + " must be at most " + max + " characters.");
            } else {
                clearError(field, errorLabel);
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    public static void onRequiredCombo(final ComboBox<?> combo, final Label errorLabel, final String fieldName) {
        combo.valueProperty().addListener((obs, o, n) -> {
            if (n == null) {
                showError(combo, errorLabel, fieldName + " is required.");
            } else {
                clearError(combo, errorLabel);
            }
        });
    }

    public static void onRequiredDate(final DatePicker picker, final Label errorLabel, final String fieldName) {
        picker.valueProperty().addListener((obs, o, n) -> {
            if (n == null) {
                showError(picker, errorLabel, fieldName + " is required.");
            } else {
                clearError(picker, errorLabel);
            }
        });
    }

    public static void onRequiredNumericRange(final TextField field, final Label errorLabel,
                                               final String fieldName, final double min, final double max) {
        FormHelper.addNumericConstraint(field);
        final Runnable validate = () -> {
            final String val = field.getText();
            if (val == null || val.trim().isEmpty()) {
                showError(field, errorLabel, fieldName + " is required.");
                return;
            }
            try {
                final double d = Double.parseDouble(val);
                if (d < min || d > max) {
                    showError(field, errorLabel, fieldName + " must be between " + min + " and " + max + ".");
                    return;
                }
                clearError(field, errorLabel);
            } catch (final NumberFormatException e) {
                showError(field, errorLabel, fieldName + " must be a valid number.");
            }
        };
        field.textProperty().addListener((obs, o, n) -> validate.run());
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) validate.run();
        });
    }

    /**
     * Check if all given error labels are invisible (no errors).
     */
    public static boolean isFormValid(final Label... errorLabels) {
        return Arrays.stream(errorLabels).noneMatch(Label::isVisible);
    }

    /**
     * Validate all required TextFields and show errors if blank.
     * Call this in the action handler before proceeding.
     */
    public static boolean validateAll(final TextField[] fields, final Label[] errorLabels,
                                       final String[] names) {
        boolean valid = true;
        for (int i = 0; i < fields.length; i++) {
            final String val = fields[i].getText();
            if (val == null || val.trim().isEmpty()) {
                showError(fields[i], errorLabels[i], names[i] + " is required.");
                valid = false;
            }
        }
        return valid;
    }

    private static void showError(final TextInputControl field, final Label errorLabel, final String msg) {
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static void showError(final ComboBox<?> field, final Label errorLabel, final String msg) {
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static void showError(final DatePicker field, final Label errorLabel, final String msg) {
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static void clearError(final TextInputControl field, final Label errorLabel) {
        field.getStyleClass().remove("field-error");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private static void clearError(final ComboBox<?> field, final Label errorLabel) {
        field.getStyleClass().remove("field-error");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private static void clearError(final DatePicker field, final Label errorLabel) {
        field.getStyleClass().remove("field-error");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
