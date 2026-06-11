package eapli.aisafe.ui.jfx.util;

import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class LoadingOverlay {

    private static final String OVERLAY_KEY = "LoadingOverlay::overlay";
    private static final String ANIM_KEY = "LoadingOverlay::animation";

    private static final String[] SPINNER_CHARS = {"◐", "◓", "◑", "◒"};

    private LoadingOverlay() {
    }

    public static void show(Node parent, String message) {
        hide(parent);

        StackPane overlay = new StackPane();
        overlay.setBackground(new Background(new BackgroundFill(
                Color.web("#0d1117", 0.7), CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)));

        Label spinner = new Label(SPINNER_CHARS[0]);
        spinner.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> spinner.setText(SPINNER_CHARS[0])),
                new KeyFrame(Duration.millis(250), e -> spinner.setText(SPINNER_CHARS[1])),
                new KeyFrame(Duration.millis(500), e -> spinner.setText(SPINNER_CHARS[2])),
                new KeyFrame(Duration.millis(750), e -> spinner.setText(SPINNER_CHARS[3]))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        Label label = new Label(message);
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox vbox = new VBox(20, spinner, label);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.getChildren().add(vbox);

        if (parent instanceof Region) {
            Region r = (Region) parent;
            overlay.prefWidthProperty().bind(r.widthProperty());
            overlay.prefHeightProperty().bind(r.heightProperty());
        }

        if (parent instanceof Pane) {
            ((Pane) parent).getChildren().add(overlay);
        } else if (parent instanceof Region) {
            final var parentPane = new StackPane();
            parentPane.getChildren().addAll((Region) parent, overlay);
        } else {
            throw new IllegalArgumentException("Unsupported parent type: " + parent.getClass());
        }

        overlay.getProperties().put(ANIM_KEY, timeline);
        parent.getProperties().put(OVERLAY_KEY, overlay);
    }

    public static void hide(Node parent) {
        StackPane overlay = (StackPane) parent.getProperties().remove(OVERLAY_KEY);
        if (overlay != null) {
            Animation anim = (Animation) overlay.getProperties().remove(ANIM_KEY);
            if (anim != null) {
                anim.stop();
            }
            if (parent instanceof Pane) {
                ((Pane) parent).getChildren().remove(overlay);
            }
        }
    }
}
