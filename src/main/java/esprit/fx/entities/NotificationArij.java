package esprit.fx.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entité Notification - table "notifications" partagée avec Symfony.
 */
public class NotificationArij {

    public static final String TYPE_INFO = "info";
    public static final String TYPE_SUCCESS = "success";
    public static final String TYPE_WARNING = "warning";

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long id;
    private Long destinataireId;
    private String title;
    private String message;
    private String type;
    private boolean lu;
    private LocalDateTime createdAt;
    private String lien;

    public NotificationArij() {}

    public NotificationArij(Long destinataireId, String title, String message, String type) {
        this.destinataireId = destinataireId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.lu = false;
        this.createdAt = LocalDateTime.now();
    }

    // ─── Getters / Setters ────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDestinataireId() { return destinataireId; }
    public void setDestinataireId(Long v) { this.destinataireId = v; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLien() { return lien; }
    public void setLien(String lien) { this.lien = lien; }

    // ─── Helpers ──────────────────────────────────────────────────────

    public String typeEmoji() {
        return switch (type == null ? "" : type.toLowerCase()) {
            case TYPE_SUCCESS -> "✅";
            case TYPE_WARNING -> "⚠️";
            default -> "ℹ️";
        };
    }

    public String formattedDate() {
        return createdAt != null ? createdAt.format(DISPLAY_FMT) : "";
    }

    @Override
    public String toString() {
        return typeEmoji() + " " + (title != null ? title : message);
    }
}
