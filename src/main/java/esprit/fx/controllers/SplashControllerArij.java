package esprit.fx.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

public class SplashControllerArij {

    @FXML private javafx.scene.layout.StackPane rootPane;
    @FXML private VBox      splashRoot;
    @FXML private ProgressBar progressBar;
    @FXML private Label     countdownLabel;
    @FXML private Label     logoEmoji;

    private static final int DURATION_SECONDS = 5;

    @FXML
    private void initialize() {
        progressBar.setProgress(0);
        splashRoot.setOpacity(0);

        // ── 1. Fade-in (0.8s) ──────────────────────────────────────────────
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), splashRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // ── 2. Légère animation du logo (pulse) ────────────────────────────
        ScaleTransition pulse = new ScaleTransition(Duration.millis(900), logoEmoji);
        pulse.setFromX(0.85); pulse.setFromY(0.85);
        pulse.setToX(1.0);    pulse.setToY(1.0);
        pulse.setInterpolator(Interpolator.EASE_OUT);

        // ── 3. Progression + countdown ────────────────────────────────────
        final int[] elapsed = {0};
        Timeline progress = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    elapsed[0]++;
                    progressBar.setProgress((double) elapsed[0] / DURATION_SECONDS);
                    int remaining = DURATION_SECONDS - elapsed[0];
                    countdownLabel.setText(remaining > 0
                            ? remaining + "s"
                            : "Bienvenue !");
                })
        );
        progress.setCycleCount(DURATION_SECONDS);

        // ── 4. Fade-out (0.6s) puis ouvrir login ──────────────────────────
        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), splashRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> javafx.application.Platform.runLater(this::openLogin));

        // ── Séquence complète ─────────────────────────────────────────────
        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(fadeIn, pulse),
                progress,
                fadeOut
        );
        sequence.play();
    }

    private void openLogin() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/Login.fxml")));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("MedTime – Login");
            stage.setWidth(950);
            stage.setHeight(680);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
