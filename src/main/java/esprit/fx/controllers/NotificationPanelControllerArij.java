package esprit.fx.controllers;

import esprit.fx.entities.NotificationArij;
import esprit.fx.services.NotificationServiceArij;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour afficher les notifications avec support des liens Google Meet.
 */
public class NotificationPanelControllerArij {

    @FXML private VBox notificationsContainer;
    @FXML private Label noNotificationsLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private final NotificationServiceArij notificationService = NotificationServiceArij.getInstance();
    private int userId;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Charge les notifications pour un utilisateur.
     */
    public void loadNotifications(int userId) {
        this.userId = userId;
        loadingIndicator.setVisible(true);
        notificationsContainer.getChildren().clear();

        Task<List<NotificationArij>> task = new Task<List<NotificationArij>>() {
            @Override
            protected List<NotificationArij> call() {
                return notificationService.getNotificationsByUser((long) userId);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                List<NotificationArij> notifications = task.getValue();
                loadingIndicator.setVisible(false);

                if (notifications.isEmpty()) {
                    noNotificationsLabel.setVisible(true);
                    noNotificationsLabel.setText("Aucune notification");
                } else {
                    noNotificationsLabel.setVisible(false);
                    for (NotificationArij notif : notifications) {
                        notificationsContainer.getChildren().add(createNotificationCard(notif));
                    }
                }
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                noNotificationsLabel.setVisible(true);
                noNotificationsLabel.setText("Erreur lors du chargement");
            });
        });

        new Thread(task).start();
    }

    /**
     * Crée une carte de notification.
     */
    private VBox createNotificationCard(NotificationArij notif) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color:white;" +
                "-fx-border-color:#e0e0e0;" +
                "-fx-border-width:1;" +
                "-fx-border-radius:8;" +
                "-fx-background-radius:8;" +
                "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.08), 3, 0, 0, 1);"
        );

        // Header avec titre et date
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(notif.getTitle());
        titleLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1a1a1a;");

        Label dateLabel = new Label(notif.getCreatedAt().format(dateFormatter));
        dateLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#999;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer, dateLabel);

        // Message
        Label messageLabel = new Label(notif.getMessage());
        messageLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#333;-fx-wrap-text:true;");
        messageLabel.setWrapText(true);

        card.getChildren().addAll(header, messageLabel);

        // Ajouter le bouton Google Meet si le lien existe
        if (notif.getLien() != null && !notif.getLien().isBlank()) {
            HBox meetBox = new HBox(10);
            meetBox.setStyle("-fx-background-color:#f0f7ff;-fx-padding:10;-fx-border-radius:6;-fx-background-radius:6;");

            Label meetLabel = new Label("📞 Lien Google Meet disponible");
            meetLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#1976d2;-fx-font-weight:bold;");

            Button joinButton = new Button("Rejoindre");
            joinButton.setStyle(
                    "-fx-font-size:11px;" +
                    "-fx-padding:6 12 6 12;" +
                    "-fx-background-color:#1976d2;" +
                    "-fx-text-fill:white;" +
                    "-fx-background-radius:6;" +
                    "-fx-cursor:hand;"
            );

            String meetLink = notif.getLien();
            joinButton.setOnAction(e -> openMeetLink(meetLink));

            meetBox.getChildren().addAll(meetLabel, new Region(), joinButton);
            HBox.setHgrow(meetBox.getChildren().get(1), Priority.ALWAYS);

            card.getChildren().add(meetBox);
        }

        // Marquer comme lu au clic
        if (!notif.isLu()) {
            card.setStyle(card.getStyle() + "-fx-border-color:#1976d2;-fx-border-width:2;");
            card.setOnMouseClicked(e -> {
                notificationService.markAsRead(notif.getId());
                card.setStyle(card.getStyle().replace("-fx-border-width:2;", "-fx-border-width:1;"));
            });
        }

        return card;
    }

    /**
     * Ouvre le lien Google Meet dans le navigateur.
     */
    private void openMeetLink(String link) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(link));
                System.out.println("[NotificationPanelControllerArij] Ouverture du lien: " + link);
            }
        } catch (Exception e) {
            System.err.println("[NotificationPanelControllerArij] Erreur ouverture lien: " + e.getMessage());
            showAlert("Erreur", "Impossible d'ouvrir le lien Google Meet", Alert.AlertType.ERROR);
        }
    }

    /**
     * Affiche une alerte.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
