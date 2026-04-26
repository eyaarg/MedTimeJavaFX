package esprit.fx;

import esprit.fx.services.SmsSchedulerServiceArij;
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
        // ── Démarrer le scheduler SMS de suivi au lancement de l'app ──
        // La Timeline vérifie toutes les heures les consultations terminées
        // depuis 24-25h et envoie un SMS de suivi aux patients concernés.
        // Le scheduler tourne en arrière-plan pendant toute la session.
        SmsSchedulerServiceArij.getInstance().demarrer();

        // ── Arrêter le scheduler proprement à la fermeture ────────────
        stage.setOnCloseRequest(e -> SmsSchedulerServiceArij.getInstance().arreter());

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
