package esprit.fx.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("MedTime - Login");
        stage.setScene(scene);
        stage.setWidth(1200);
        stage.setHeight(750);
        stage.show();
    }


}