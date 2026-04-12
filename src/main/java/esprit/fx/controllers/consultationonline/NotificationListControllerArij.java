package esprit.fx.controllers.consultationonline;

import esprit.fx.entities.NotificationsArij;
import esprit.fx.services.ServiceNotificationsArij;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationListControllerArij {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    @FXML private VBox notificationsContainer;
    private final ServiceNotificationsArij service = new ServiceNotificationsArij();

    @FXML private void initialize()  { loadNotifications(); }
    @FXML private void markAllRead() { service.markAllAsRead(); loadNotifications(); }

    private void loadNotifications() {
        notificationsContainer.getChildren().clear();
        List<NotificationsArij> list = service.getMyNotifications();
        if (list.isEmpty()) { notificationsContainer.getChildren().add(emptyState()); return; }
        for (NotificationsArij n : list) notificationsContainer.getChildren().add(buildRow(n));
    }

    private HBox buildRow(NotificationsArij n) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.getStyleClass().add(n.isRead() ? "notif-row-read" : "notif-row-unread");

        Label dot = new Label();
        dot.setMinSize(10, 10); dot.setMaxSize(10, 10);
        dot.setStyle("-fx-background-color:" + (n.isRead() ? "#cbd5e1" : "#2563eb") + ";-fx-background-radius:50%;");

        VBox text = new VBox(4);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label title = new Label(n.getTitle() != null ? n.getTitle() : "Notification");
        title.setStyle("-fx-font-size:13px;-fx-font-weight:" + (n.isRead() ? "normal" : "bold") + ";-fx-text-fill:#0f172a;");
        Label message = new Label(n.getMessage() != null ? n.getMessage() : "");
        message.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");
        message.setWrapText(true);
        text.getChildren().addAll(title, message);

        Label ts = new Label(n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
        ts.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");

        row.getChildren().addAll(dot, text, ts);
        row.setOnMouseClicked(e -> { service.markAsRead(n.getId()); loadNotifications(); });
        return row;
    }

    private VBox emptyState() {
        VBox box = new VBox(12); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(60));
        Label icon = new Label("🔔"); icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Vous êtes à jour !"); msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label sub = new Label("Aucune nouvelle notification"); sub.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(icon, msg, sub);
        return box;
    }
}
