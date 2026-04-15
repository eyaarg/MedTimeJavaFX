package esprit.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Démarre sur la page de login
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                Main.class.getResource("/Login.fxml")));
        Scene scene = new Scene(root);
        stage.setTitle("MedTimeFX — Connexion");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setWidth(600);
        stage.setHeight(420);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
