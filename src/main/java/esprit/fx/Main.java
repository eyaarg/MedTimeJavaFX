package esprit.fx;

import esprit.fx.utils.InitDatabaseArij;
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
        // Initialiser la base de données
        InitDatabaseArij.initializeTables();

        // Démarre sur la page de login
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                Main.class.getResource("/Login.fxml")));
        Scene scene = new Scene(root, 1100, 760);
        stage.setTitle("MedTimeFX — Connexion");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(600);
        stage.setMinHeight(420);
        stage.setWidth(1100);
        stage.setHeight(760);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
