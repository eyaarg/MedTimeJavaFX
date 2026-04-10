package tn.esprit.repositories.consultationonline;

import tn.esprit.entities.consultationonline.NotificationArij;
import tn.esprit.utils.consultationonline.DatabaseConnectionArij;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepositoryArij {

    public List<NotificationArij> findByUserId(int userId) {
        List<NotificationArij> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return list;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("findByUserId error: " + e.getMessage());
        }
        return list;
    }

    public int countUnread(int userId) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return count;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("countUnread error: " + e.getMessage());
        }
        return count;
    }

    public void create(NotificationArij n) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, link, is_read, created_at) VALUES (?,?,?,?,?,?,?)";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType());
            ps.setString(5, n.getLink());
            ps.setBoolean(6, n.isRead());
            ps.setTimestamp(7, toTimestamp(n.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("create notification error: " + e.getMessage());
        }
    }

    public void markAsRead(int id) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("markAsRead error: " + e.getMessage());
        }
    }

    public void markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("markAllAsRead error: " + e.getMessage());
        }
    }

    private NotificationArij mapRow(ResultSet rs) throws SQLException {
        NotificationArij n = new NotificationArij();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setLink(rs.getString("link"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp created = rs.getTimestamp("created_at");
        n.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        return n;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }
}
