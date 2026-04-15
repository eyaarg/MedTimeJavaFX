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
        // Démarre sur la page de login avec une taille lisible.
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                Main.class.getResource("/Login.fxml")));
        Scene scene = new Scene(root);
        stage.setTitle("MedTimeFX — Connexion");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(680);
        stage.setWidth(980);
        stage.setHeight(720);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
