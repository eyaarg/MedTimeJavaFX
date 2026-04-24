package esprit.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import esprit.fx.services.SchedulerService;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Démarrer le scheduler de publication automatique
        SchedulerService.getInstance().start();

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/SplashScreenArij.fxml")));
        Scene scene = new Scene(root);
        stage.setTitle("MedTime");
        stage.setScene(scene);
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
