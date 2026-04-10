package tn.esprit.services.consultationonline;

import tn.esprit.entities.consultationonline.NotificationArij;
import tn.esprit.repositories.consultationonline.NotificationRepositoryArij;

import java.util.List;

public class NotificationServiceArij {
    private static final int CURRENT_USER_ID = 1;

    private final NotificationRepositoryArij notificationRepository = new NotificationRepositoryArij();

    public List<NotificationArij> getMyNotifications() {
        return notificationRepository.findByUserId(CURRENT_USER_ID);
    }

    public int getUnreadCount() {
        return notificationRepository.countUnread(CURRENT_USER_ID);
    }

    public void markAsRead(int id) {
        notificationRepository.markAsRead(id);
    }

    public void markAllAsRead() {
        notificationRepository.markAllAsRead(CURRENT_USER_ID);
    }
}
