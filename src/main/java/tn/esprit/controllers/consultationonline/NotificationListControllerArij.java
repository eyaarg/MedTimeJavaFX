package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.entities.consultationonline.NotificationArij;
import tn.esprit.services.consultationonline.NotificationServiceArij;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationListControllerArij {
    @FXML
    private VBox notificationsContainer;

    private final NotificationServiceArij notificationService = new NotificationServiceArij();

    @FXML
    private void initialize() {
        loadNotifications();
    }

    @FXML
    private void markAllRead() {
        notificationService.markAllAsRead();
        loadNotifications();
    }

    private void loadNotifications() {
        notificationsContainer.getChildren().clear();
        List<NotificationArij> notifications = notificationService.getMyNotifications();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (NotificationArij n : notifications) {
            VBox card = new VBox(4);
            card.getStyleClass().add("card");
            Label title = new Label(n.getTitle());
            Label message = new Label(n.getMessage());
            Label date = new Label(n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : "");
            if (!n.isRead()) {
                title.setStyle("-fx-font-weight: bold;");
                message.setStyle("-fx-font-weight: bold;");
            }
            card.setOnMouseClicked(e -> {
                notificationService.markAsRead(n.getId());
                loadNotifications();
            });
            card.getChildren().addAll(title, message, date);
            notificationsContainer.getChildren().add(card);
        }
    }
}
