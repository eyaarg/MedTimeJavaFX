package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;

public class NotificationArij {
    private int id;
    private int userId;
    private String title;
    private String message;
    private String type;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;

    public NotificationArij() {
    }

    public NotificationArij(int id, int userId, String title, String message, String type, String link,
                            boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.link = link;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "NotificationArij{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", link='" + link + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}
