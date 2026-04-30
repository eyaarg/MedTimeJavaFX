package esprit.fx.utils;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;
// import javafx.media.AudioClip;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Utilitaire pour gérer le badge de notifications non lues.
 * Met à jour le badge toutes les 30 secondes et joue un son si nouvelle notification.
 */
public class NotificationBadgeArij {

    private Timeline timeline;
    private Label badgeLabel;
    private Long patientId;
    private int lastCount = 0;
    // private AudioClip notificationSound;

    /**
     * Crée un gestionnaire de badge de notifications.
     * 
     * @param badgeLabel Label pour afficher le nombre de notifications
     * @param patientId ID du patient
     */
    public NotificationBadgeArij(Label badgeLabel, Long patientId) {
        this.badgeLabel = badgeLabel;
        this.patientId = patientId;
        
        // Charger le son de notification (optionnel)
        /*
        try {
            String soundPath = getClass().getResource("/sounds/notification.mp3").toExternalForm();
            notificationSound = new AudioClip(soundPath);
        } catch (Exception e) {
            System.out.println("[NotificationBadgeArij] Son de notification non disponible");
        }
        */
    }

    /**
     * Démarre la mise à jour automatique du badge.
     */
    public void start() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> updateBadge())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Afficher immédiatement
        updateBadge();
    }

    /**
     * Met à jour le badge avec le nombre de notifications non lues.
     */
    private void updateBadge() {
        try {
            int count = getUnreadNotificationCount();
            
            Platform.runLater(() -> {
                if (count > 0) {
                    badgeLabel.setText(String.valueOf(count));
                    badgeLabel.setVisible(true);
                    badgeLabel.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; " +
                                      "-fx-font-size: 10px; -fx-font-weight: bold; " +
                                      "-fx-padding: 2 6; -fx-background-radius: 10;");
                    
                    // Jouer le son si nouvelle notification
                    /*
                    if (count > lastCount && notificationSound != null) {
                        notificationSound.play();
                    }
                    */
                } else {
                    badgeLabel.setVisible(false);
                }
                
                lastCount = count;
            });
        } catch (Exception e) {
            System.err.println("[NotificationBadgeArij] Erreur mise à jour badge : " + e.getMessage());
        }
    }

    /**
     * Récupère le nombre de notifications non lues.
     */
    private int getUnreadNotificationCount() throws Exception {
        String sql = "SELECT COUNT(*) as count FROM notifications WHERE destinataire_id = ? AND lu = false";
        
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, patientId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        
        return 0;
    }

    /**
     * Arrête la mise à jour automatique du badge.
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
