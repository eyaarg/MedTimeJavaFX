package esprit.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainStatistiques extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/StatistiquesRendezVous.fxml")));
        Scene scene = new Scene(root);
        stage.setTitle("Test - Statistiques des Rendez-Vous");
        stage.setScene(scene);
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
