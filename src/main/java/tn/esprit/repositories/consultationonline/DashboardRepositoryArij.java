package tn.esprit.repositories.consultationonline;

import tn.esprit.utils.consultationonline.DatabaseConnectionArij;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardRepositoryArij {

    public int countByStatus(String status, int userId) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM consultations WHERE status = ? AND is_deleted = 0 AND (patient_id = ? OR doctor_id = ?)";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return count;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("countByStatus error: " + e.getMessage());
        }
        return count;
    }

    public Map<String, Integer> countByWeek(int doctorId) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(consultation_date, '%Y-%u') AS wk, COUNT(*) " +
                "FROM consultations WHERE doctor_id = ? AND consultation_date >= DATE_SUB(CURDATE(), INTERVAL 28 DAY) " +
                "GROUP BY wk ORDER BY wk";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return map;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String weekLabel = rs.getString(1);
                    int count = rs.getInt(2);
                    map.put(weekLabel, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("countByWeek error: " + e.getMessage());
        }
        return map;
    }

    public int countOrdonnances(int doctorId) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM ordonnances WHERE doctor_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return count;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("countOrdonnances error: " + e.getMessage());
        }
        return count;
    }

    public double getAverageSatisfaction(int userId) {
        double avg = 0;
        String sql = "SELECT AVG(score) FROM satisfactions WHERE patient_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return avg;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avg = rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("getAverageSatisfaction error: " + e.getMessage());
        }
        return avg;
    }

    public LocalDateTime findNextConsultation(int userId) {
        LocalDateTime date = null;
        String sql = "SELECT MIN(consultation_date) FROM consultations WHERE consultation_date >= NOW() AND is_deleted = 0 AND patient_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    if (ts != null) {
                        date = ts.toLocalDateTime();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("findNextConsultation error: " + e.getMessage());
        }
        return date;
    }
}
