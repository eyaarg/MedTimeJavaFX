package esprit.fx.services;

import esprit.fx.entities.NotificationsArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceNotificationsArij {

    private static final int CURRENT_USER_ID = 1;
    private Connection conn() { return MyDB.getInstance().getConnection(); }

    public List<NotificationsArij> getMyNotifications() {
        List<NotificationsArij> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC")) {
            ps.setInt(1, CURRENT_USER_ID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getMyNotifications: " + e.getMessage()); }
        return list;
    }

    public int getUnreadCount() {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, CURRENT_USER_ID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("getUnreadCount: " + e.getMessage()); }
        return 0;
    }

    public void markAsRead(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE notifications SET is_read = 1 WHERE id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("markAsRead: " + e.getMessage()); }
    }

    public void markAllAsRead() {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE notifications SET is_read = 1 WHERE user_id = ?")) {
            ps.setInt(1, CURRENT_USER_ID); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("markAllAsRead: " + e.getMessage()); }
    }

    private NotificationsArij mapRow(ResultSet rs) throws SQLException {
        NotificationsArij n = new NotificationsArij();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setLink(rs.getString("link"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ca = rs.getTimestamp("created_at");
        n.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        return n;
    }
}
