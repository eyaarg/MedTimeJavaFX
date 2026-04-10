package tn.esprit.repositories.consultationonline;

import tn.esprit.entities.consultationonline.ConsultationArij;
import tn.esprit.utils.consultationonline.DatabaseConnectionArij;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ConsultationRepositoryArij {

    public List<ConsultationArij> findByPatientId(int patientId) {
        List<ConsultationArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE patient_id = ? AND is_deleted = 0";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return list;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("findByPatientId error: " + e.getMessage());
        }
        return list;
    }

    public List<ConsultationArij> findByDoctorId(int doctorId) {
        List<ConsultationArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE doctor_id = ? AND is_deleted = 0";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return list;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("findByDoctorId error: " + e.getMessage());
        }
        return list;
    }

    public ConsultationArij findById(int id) {
        ConsultationArij consultation = null;
        String sql = "SELECT * FROM consultations WHERE id = ? AND is_deleted = 0";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    consultation = mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("findById error: " + e.getMessage());
        }
        return consultation;
    }

    public void create(ConsultationArij c) {
        String sql = "INSERT INTO consultations (patient_id, doctor_id, consultation_date, type, status, is_deleted, created_at, updated_at, rejection_reason, consultation_fee, lien_meet) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getDoctorId());
            ps.setTimestamp(3, toTimestamp(c.getConsultationDate()));
            ps.setString(4, c.getType());
            ps.setString(5, c.getStatus());
            ps.setBoolean(6, c.isDeleted());
            ps.setTimestamp(7, toTimestamp(c.getCreatedAt()));
            ps.setTimestamp(8, toTimestamp(c.getUpdatedAt()));
            ps.setString(9, c.getRejectionReason());
            ps.setDouble(10, c.getConsultationFee());
            ps.setString(11, c.getLienMeet());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("create consultation error: " + e.getMessage());
        }
    }

    public void update(ConsultationArij c) {
        String sql = "UPDATE consultations SET patient_id=?, doctor_id=?, consultation_date=?, type=?, status=?, is_deleted=?, created_at=?, updated_at=?, rejection_reason=?, consultation_fee=?, lien_meet=? WHERE id=?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getDoctorId());
            ps.setTimestamp(3, toTimestamp(c.getConsultationDate()));
            ps.setString(4, c.getType());
            ps.setString(5, c.getStatus());
            ps.setBoolean(6, c.isDeleted());
            ps.setTimestamp(7, toTimestamp(c.getCreatedAt()));
            ps.setTimestamp(8, toTimestamp(c.getUpdatedAt()));
            ps.setString(9, c.getRejectionReason());
            ps.setDouble(10, c.getConsultationFee());
            ps.setString(11, c.getLienMeet());
            ps.setInt(12, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("update consultation error: " + e.getMessage());
        }
    }

    public void softDelete(int id) {
        String sql = "UPDATE consultations SET is_deleted = 1 WHERE id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("softDelete error: " + e.getMessage());
        }
    }

    public List<ConsultationArij> filterByStatus(String status, int userId) {
        List<ConsultationArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE status = ? AND (patient_id = ? OR doctor_id = ?) AND is_deleted = 0";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return list;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("filterByStatus error: " + e.getMessage());
        }
        return list;
    }

    public List<ConsultationArij> filterByType(String type, int userId) {
        List<ConsultationArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE type = ? AND (patient_id = ? OR doctor_id = ?) AND is_deleted = 0";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return list;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("filterByType error: " + e.getMessage());
        }
        return list;
    }

    private ConsultationArij mapRow(ResultSet rs) throws SQLException {
        ConsultationArij c = new ConsultationArij();
        c.setId(rs.getInt("id"));
        c.setPatientId(rs.getInt("patient_id"));
        c.setDoctorId(rs.getInt("doctor_id"));
        Timestamp consultationDate = rs.getTimestamp("consultation_date");
        c.setConsultationDate(consultationDate != null ? consultationDate.toLocalDateTime() : null);
        c.setType(rs.getString("type"));
        c.setStatus(rs.getString("status"));
        c.setDeleted(rs.getBoolean("is_deleted"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        c.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        c.setRejectionReason(rs.getString("rejection_reason"));
        c.setConsultationFee(rs.getDouble("consultation_fee"));
        c.setLienMeet(rs.getString("lien_meet"));
        return c;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }
}
