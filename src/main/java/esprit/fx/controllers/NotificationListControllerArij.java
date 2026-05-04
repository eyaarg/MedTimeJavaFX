package esprit.fx.controllers;

import esprit.fx.entities.NotificationsArij;
import esprit.fx.services.ServiceNotificationsArij;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationListControllerArij {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    @FXML private VBox notificationsContainer;
    private final ServiceNotificationsArij service = new ServiceNotificationsArij();
    private int userId = 0;

    @FXML private void initialize()  { /* context is set after load */ }
    @FXML private void markAllRead() { service.markAllAsRead(userId); loadNotifications(); }

    private void loadNotifications() {
        notificationsContainer.getChildren().clear();
        List<NotificationsArij> list = service.getNotificationsByUser(userId);
        if (list.isEmpty()) {
            notificationsContainer.getChildren().add(emptyState());
            return;
        }
        for (NotificationsArij n : list) {
            notificationsContainer.getChildren().add(buildRow(n));
        }
    }

    private VBox buildRow(NotificationsArij n) {
        // Carte principale
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
            "-fx-background-color:" + (n.isRead() ? "white" : "#eff6ff") + ";" +
            "-fx-border-color:" + (n.isRead() ? "#e2e8f0" : "#93c5fd") + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:10;" +
            "-fx-background-radius:10;" +
            "-fx-cursor:hand;"
        );

        // Ligne du haut: point + titre + date
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label();
        dot.setMinSize(10, 10);
        dot.setMaxSize(10, 10);
        dot.setStyle("-fx-background-color:" + (n.isRead() ? "#cbd5e1" : "#2563eb") + ";-fx-background-radius:50%;");

        Label title = new Label(n.getTitle() != null ? n.getTitle() : "Notification");
        title.setStyle("-fx-font-size:13px;-fx-font-weight:" + (n.isRead() ? "normal" : "bold") + ";-fx-text-fill:#0f172a;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label ts = new Label(n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
        ts.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");

        topRow.getChildren().addAll(dot, title, ts);

        // Message
        Label message = new Label(n.getMessage() != null ? n.getMessage() : "");
        message.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");
        message.setWrapText(true);

        card.getChildren().addAll(topRow, message);

        // Bouton Google Meet si lien présent
        String link = n.getLink();
        if (link != null && !link.isBlank()) {
            HBox meetBox = new HBox(10);
            meetBox.setAlignment(Pos.CENTER_LEFT);
            meetBox.setPadding(new Insets(8, 12, 8, 12));
            meetBox.setStyle(
                "-fx-background-color:#dbeafe;" +
                "-fx-border-radius:8;" +
                "-fx-background-radius:8;"
            );

            Label meetIcon = new Label("📞");
            meetIcon.setStyle("-fx-font-size:14px;");

            Label meetLabel = new Label("Lien Google Meet disponible");
            meetLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#1d4ed8;-fx-font-weight:bold;");
            HBox.setHgrow(meetLabel, Priority.ALWAYS);

            Button joinBtn = new Button("Rejoindre la consultation");
            joinBtn.setStyle(
                "-fx-background-color:#1d4ed8;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:11px;" +
                "-fx-font-weight:bold;" +
                "-fx-padding:6 14;" +
                "-fx-background-radius:6;" +
                "-fx-cursor:hand;"
            );
            joinBtn.setOnAction(e -> {
                e.consume();
                openLink(link);
            });

            meetBox.getChildren().addAll(meetIcon, meetLabel, joinBtn);
            card.getChildren().add(meetBox);
        }

        // Clic sur la carte → marquer comme lu
        card.setOnMouseClicked(e -> {
            if (!n.isRead()) {
                service.markAsRead(n.getId());
                loadNotifications();
            }
        });

        return card;
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback: ouvrir via Runtime
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            }
        } catch (Exception ex) {
            System.err.println("[NotificationListControllerArij] Impossible d'ouvrir le lien: " + ex.getMessage());
        }
    }

    private VBox emptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        Label icon = new Label("🔔");
        icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Vous êtes à jour !");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label sub = new Label("Aucune nouvelle notification");
        sub.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        loadNotifications();
    }
}
