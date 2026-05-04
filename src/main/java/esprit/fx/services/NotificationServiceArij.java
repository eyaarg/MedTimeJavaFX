package esprit.fx.services;

import esprit.fx.entities.NotificationArij;
import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service centralisé de notification.
 * Persiste les notifications dans la table "notifications" partagée avec Symfony.
 */
public class NotificationServiceArij {

    private static NotificationServiceArij instance;

    private NotificationServiceArij() {}

    public static NotificationServiceArij getInstance() {
        if (instance == null) {
            instance = new NotificationServiceArij();
        }
        return instance;
    }

    /**
     * Crée et persiste une notification.
     *
     * @param destinataireId ID utilisateur destinataire
     * @param message texte du message
     * @param type "info" | "success" | "warning"
     * @param lien URL optionnelle
     * @return la notification créée, ou null si erreur
     */
    public NotificationArij notifier(Long destinataireId, String message, String type, String lien) {
        if (destinataireId == null || destinataireId <= 0) {
            System.err.println("[NotificationServiceArij] destinataireId invalide");
            return null;
        }
        if (message == null || message.isBlank()) {
            System.err.println("[NotificationServiceArij] message vide");
            return null;
        }

        try {
            String normalizedType = normalizeType(type);
            String title = titleFromType(normalizedType);

            String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at, link) " +
                    "VALUES (?, ?, ?, ?, 0, ?, ?)";

            Connection conn = MyDB.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, destinataireId);
                ps.setString(2, title);
                ps.setString(3, message);
                ps.setString(4, normalizedType);
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(6, lien);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    NotificationArij notif = new NotificationArij();
                    notif.setId((long) keys.getInt(1));
                    notif.setDestinataireId(destinataireId);
                    notif.setTitle(title);
                    notif.setMessage(message);
                    notif.setType(normalizedType);
                    notif.setLu(false);
                    notif.setCreatedAt(LocalDateTime.now());
                    notif.setLien(lien);

                    System.out.println("[NotificationServiceArij] ✓ Notification envoyée à userId=" + destinataireId);
                    return notif;
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificationServiceArij] Erreur SQL : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les notifications d'un utilisateur.
     */
    public List<NotificationArij> getNotificationsByUser(Long userId) {
        List<NotificationArij> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
            Connection conn = MyDB.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    NotificationArij notif = new NotificationArij();
                    notif.setId(rs.getLong("id"));
                    notif.setDestinataireId(rs.getLong("user_id"));
                    notif.setTitle(rs.getString("title"));
                    notif.setMessage(rs.getString("message"));
                    notif.setType(rs.getString("type"));
                    notif.setLu(rs.getBoolean("is_read"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) notif.setCreatedAt(ts.toLocalDateTime());
                    notif.setLien(rs.getString("link"));
                    list.add(notif);
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificationServiceArij] Erreur lecture : " + e.getMessage());
        }
        return list;
    }

    /**
     * Marque une notification comme lue.
     */
    public void markAsRead(Long notificationId) {
        try {
            String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
            Connection conn = MyDB.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, notificationId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[NotificationServiceArij] Erreur update : " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private String normalizeType(String type) {
        if (type == null) return "info";
        return switch (type.trim().toLowerCase()) {
            case "success", "succès", "succes" -> "success";
            case "warning", "warn", "attention" -> "warning";
            default -> "info";
        };
    }

    private String titleFromType(String type) {
        return switch (type) {
            case "success" -> "✅ Confirmation";
            case "warning" -> "⚠️ Attention";
            default -> "ℹ️ Information";
        };
    }
}
