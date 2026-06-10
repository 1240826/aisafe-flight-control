package eapli.aisafe.ui.jfx;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.ui.jfx.controller.LoginController;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class AISafeFX extends Application {

    private static Stage primaryStage;

    public static Stage primaryStage() {
        return primaryStage;
    }

    @Override
    public void start(final Stage stage) {
        primaryStage = stage;

        AuthzRegistry.configure(
                PersistenceContext.repositories().users(),
                new eapli.aisafe.usermanagement.domain.AISafePasswordPolicy(),
                new PlainTextEncoder()
        );

        stage.setTitle("AISafe Flight Control System");
        final var iconStream = getClass().getResourceAsStream("/icons/aisafe-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setMaximized(true);

        stage.setOnCloseRequest((WindowEvent event) -> {
            Platform.exit();
            System.exit(0);
        });

        Font.loadFont(getClass().getResourceAsStream("/styles/jetbrains-mono.ttf"), 10);

        SceneManager.showScene("Login", "/fxml/Login.fxml",
                () -> new LoginController());

        stage.show();
    }

    public static void main(final String[] args) {
        launch(args);
    }
}
