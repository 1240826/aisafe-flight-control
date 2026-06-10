package eapli.aisafe.ui.jfx.util;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

public final class NotificationManager {

    private static final Queue<Notification> queue = new LinkedList<>();
    private static StackPane container;
    private static boolean showing = false;

    private NotificationManager() {}

    public static void init(final Pane root) {
        container = new StackPane();
        container.setPickOnBounds(false);
        container.setMouseTransparent(true);
        container.setAlignment(Pos.TOP_RIGHT);
        container.setPadding(new Insets(70, 20, 20, 20));
        container.setMaxWidth(420);
        container.setTranslateX(0);
        root.getChildren().add(container);
    }

    public static void success(final String title, final String message) {
        show(title, message, NotificationType.SUCCESS);
    }

    public static void error(final String title, final String message) {
        show(title, message, NotificationType.ERROR);
    }

    public static void warning(final String title, final String message) {
        show(title, message, NotificationType.WARNING);
    }

    public static void info(final String title, final String message) {
        show(title, message, NotificationType.INFO);
    }

    private static void show(final String title, final String message, final NotificationType type) {
        if (container == null) return;
        queue.add(new Notification(title, message, type));
        if (!showing) {
            showNext();
        }
    }

    private static void showNext() {
        if (queue.isEmpty()) {
            showing = false;
            return;
        }
        showing = true;
        final Notification notif = queue.poll();
        final HBox card = createCard(notif);
        card.setOpacity(0);
        card.setTranslateX(400);
        container.getChildren().add(card);

        final Timeline entrance = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(card.opacityProperty(), 0), new KeyValue(card.translateXProperty(), 400)),
                new KeyFrame(Duration.millis(300), new KeyValue(card.opacityProperty(), 1), new KeyValue(card.translateXProperty(), 0))
        );

        final PauseTransition pause = new PauseTransition(Duration.seconds(4));

        final Timeline exit = new Timeline(
                new KeyFrame(Duration.millis(300), new KeyValue(card.opacityProperty(), 0), new KeyValue(card.translateXProperty(), 400))
        );

        exit.setOnFinished(e -> {
            container.getChildren().remove(card);
            showNext();
        });

        entrance.play();
        entrance.setOnFinished(e -> {
            pause.play();
            pause.setOnFinished(e2 -> exit.play());
        });
    }

    private static HBox createCard(final Notification notif) {
        final Circle dot = new Circle(6);
        dot.setFill(notif.type.color);

        final Label titleLbl = new Label(notif.title);
        titleLbl.getStyleClass().add("notif-title");

        final Label msgLbl = new Label(notif.message);
        msgLbl.getStyleClass().add("notif-message");

        final VBox textBox = new VBox(2, titleLbl, msgLbl);

        final HBox card = new HBox(12, dot, textBox);
        card.getStyleClass().addAll("notification-card", notif.type.styleClass);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        return card;
    }

    private enum NotificationType {
        SUCCESS("#3fb950", "notif-success"),
        ERROR("#f85149", "notif-error"),
        WARNING("#d29922", "notif-warning"),
        INFO("#58a6ff", "notif-info");

        final Color color;
        final String styleClass;

        NotificationType(final String hex, final String styleClass) {
            this.color = Color.web(hex);
            this.styleClass = styleClass;
        }
    }

    private record Notification(String title, String message, NotificationType type) {}
}
