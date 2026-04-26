package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UserStatsService {

    public int getTotalUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getTotalDoctors() throws SQLException {
        String sql = "SELECT COUNT(*) FROM doctors";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getTotalPatients() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id NOT IN (SELECT user_id FROM doctors)";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getPendingDoctors() throws SQLException {
        String sql = "SELECT COUNT(*) FROM doctors d JOIN users u ON d.user_id = u.id WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getActiveUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = true";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getLockedAccounts() throws SQLException {
        // Comptes bloqués = is_active=false ET is_verified=true (pas en attente de vérif email)
        // Exclure les médecins en attente de validation (ils ont is_active=false mais c'est normal)
        String sql = "SELECT COUNT(*) FROM users u WHERE u.is_active = false AND u.is_verified = true " +
                "AND u.id NOT IN (SELECT user_id FROM doctors WHERE is_certified = false)";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getUnverifiedEmails() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE is_verified = false";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Map<String, Integer> getUsersPerRole() throws SQLException {
        String sql = "SELECT r.name, COUNT(ur.user_id) AS user_count FROM roles r LEFT JOIN user_roles ur ON r.id = ur.role_id GROUP BY r.name";
        Map<String, Integer> usersPerRole = new HashMap<>();
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usersPerRole.put(rs.getString("name"), rs.getInt("user_count"));
            }
        }
        return usersPerRole;
    }
}
