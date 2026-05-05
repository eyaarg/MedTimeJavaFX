package esprit.fx.utils;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utilitaire pour afficher un compte à rebours jusqu'à une consultation.
 */
public class CountdownTimerArij {

    private Timeline timeline;
    private Label label;
    private LocalDateTime targetDateTime;

    /**
     * Crée un compte à rebours.
     * 
     * @param label Label où afficher le compte à rebours
     * @param targetDateTime Date/heure cible
     */
    public CountdownTimerArij(Label label, LocalDateTime targetDateTime) {
        this.label = label;
        this.targetDateTime = targetDateTime;
    }

    /**
     * Démarre le compte à rebours.
     */
    public void start() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> updateCountdown())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Afficher immédiatement
        updateCountdown();
    }

    /**
     * Met à jour l'affichage du compte à rebours.
     */
    private void updateCountdown() {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            
            if (now.isAfter(targetDateTime)) {
                label.setText("La consultation a commencé");
                if (timeline != null) {
                    timeline.stop();
                }
                return;
            }

            long secondsRemaining = ChronoUnit.SECONDS.between(now, targetDateTime);
            
            long hours = secondsRemaining / 3600;
            long minutes = (secondsRemaining % 3600) / 60;
            long seconds = secondsRemaining % 60;

            String countdown = String.format("Votre consultation commence dans : %02d:%02d:%02d", 
                                            hours, minutes, seconds);
            label.setText(countdown);
        });
    }

    /**
     * Arrête le compte à rebours.
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
