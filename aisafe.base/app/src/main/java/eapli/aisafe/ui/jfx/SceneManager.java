package eapli.aisafe.ui.jfx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Supplier;

public final class SceneManager {

    private static final String STYLESHEET = "/styles/dark-theme.css";

    private SceneManager() {
    }

    public static void showScene(final String title, final String fxmlPath,
                                 final Supplier<Object> controllerSupplier) {
        try {
            final var loader = new FXMLLoader();
            loader.setLocation(SceneManager.class.getResource(fxmlPath));
            loader.setController(controllerSupplier.get());
            final Parent root = loader.load();
            configureAndShow(title, root);
        } catch (final Exception e) {
            System.err.println("[SceneManager] Failed to load FXML: " + fxmlPath);
            e.printStackTrace(System.err);
        }
    }

    public static void showScene(final String title, final String fxmlPath) {
        try {
            final var loader = new FXMLLoader();
            loader.setLocation(SceneManager.class.getResource(fxmlPath));
            final Parent root = loader.load();
            configureAndShow(title, root);
        } catch (final Exception e) {
            System.err.println("[SceneManager] Failed to load FXML: " + fxmlPath);
            e.printStackTrace(System.err);
        }
    }

    public static void switchScene(final String title, final String fxmlPath,
                                   final Supplier<Object> controllerSupplier) {
        try {
            final var loader = new FXMLLoader();
            loader.setLocation(SceneManager.class.getResource(fxmlPath));
            loader.setController(controllerSupplier.get());
            final Parent root = loader.load();
            final Stage stage = AISafeFX.primaryStage();
            stage.setTitle(title);
            final var iconStream = SceneManager.class.getResourceAsStream("/icons/aisafe-icon.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
            final Scene scene = stage.getScene();
            if (scene != null) {
                scene.setRoot(root);
            } else {
                final Scene newScene = new Scene(root);
                newScene.getStylesheets().add(STYLESHEET);
                stage.setScene(newScene);
            }
            stage.setMaximized(true);
            if (!stage.isShowing()) {
                stage.show();
            }
        } catch (final Exception e) {
            System.err.println("[SceneManager] Failed to switch scene to: " + fxmlPath);
            e.printStackTrace(System.err);
        }
    }

    public static void showInContentArea(final String fxmlPath,
                                         final Supplier<Object> controllerSupplier,
                                         final AnchorPane contentArea) {
        try {
            final var loader = new FXMLLoader();
            loader.setLocation(SceneManager.class.getResource(fxmlPath));
            loader.setController(controllerSupplier.get());
            final Node view = loader.load();
            contentArea.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (final Exception e) {
            System.err.println("[SceneManager] Failed to load FXML: " + fxmlPath);
            e.printStackTrace(System.err);
        }
    }

    public static void openZoomWindow(final String title, final Node content) {
        final Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(AISafeFX.primaryStage());
        final BorderPane root = new BorderPane(content);
        final Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLESHEET);
        final var iconStream = SceneManager.class.getResourceAsStream("/icons/aisafe-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setTitle(title + " — Zoom View");
        stage.setMaximized(true);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private static void configureAndShow(final String title, final Parent root) {
        final Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLESHEET);
        final Stage stage = AISafeFX.primaryStage();
        stage.setTitle(title);
        final var iconStream = SceneManager.class.getResourceAsStream("/icons/aisafe-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setMaximized(true);
        stage.setScene(scene);
        if (!stage.isShowing()) {
            stage.show();
        }
    }
}
