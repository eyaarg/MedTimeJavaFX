import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.NotificationsArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceConsultationsArij {

    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_ROLE = "PATIENT";

    private Connection conn() { return MyDB.getInstance().getConnection(); }

    // ── CRUD ──────────────────────────────────────────────────────────────

    public List<ConsultationsArij> getMyConsultations() {
        String sql = "PATIENT".equalsIgnoreCase(CURRENT_USER_ROLE)
                ? "SELECT * FROM consultations WHERE patient_id = ? AND is_deleted = 0"
                : "SELECT * FROM consultations WHERE doctor_id = ? AND is_deleted = 0";
        List<ConsultationsArij> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, CURRENT_USER_ID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getMyConsultations: " + e.getMessage()); }
        return list;
    }

    public ConsultationsArij findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM consultations WHERE id = ? AND is_deleted = 0")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("findById: " + e.getMessage()); }
        return null;
    }

    public void createConsultation(ConsultationsArij c) {
        c.setStatus("EN_ATTENTE");
        c.setCreatedAt(LocalDateTime.now());
        c.setDeleted(false);
        String sql = "INSERT INTO consultations (patient_id, doctor_id, consultation_date, type, status, is_deleted, created_at, updated_at, rejection_reason, consultation_fee) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getDoctorId());
            ps.setTimestamp(3, ts(c.getConsultationDate()));
            ps.setString(4, c.getType());
            ps.setString(5, c.getStatus());
            ps.setBoolean(6, c.isDeleted());
            ps.setTimestamp(7, ts(c.getCreatedAt()));
            ps.setTimestamp(8, ts(c.getUpdatedAt()));
            ps.setString(9, c.getRejectionReason());
            ps.setDouble(10, c.getConsultationFee());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
            notifyDoctor(c.getDoctorId(), c.getId());
        } catch (SQLException e) { System.err.println("createConsultation: " + e.getMessage()); }
    }

    /**
     * Vérifie l'unicité : un patient ne peut pas avoir deux consultations
     * avec le même médecin le même jour.
     */
    public boolean existsConsultation(int patientId, int doctorId, LocalDateTime date) {
        String sql = "SELECT COUNT(*) FROM consultations " +
                     "WHERE patient_id=? AND doctor_id=? AND DATE(consultation_date)=DATE(?) " +
                     "AND is_deleted=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setTimestamp(3, ts(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { System.err.println("existsConsultation: " + e.getMessage()); }
        return false;
    }

    public void updateConsultation(ConsultationsArij c) {
        c.setUpdatedAt(LocalDateTime.now());
        String sql = "UPDATE consultations SET patient_id=?, doctor_id=?, consultation_date=?, type=?, status=?, is_deleted=?, updated_at=?, rejection_reason=?, consultation_fee=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getDoctorId());
            ps.setTimestamp(3, ts(c.getConsultationDate()));
            ps.setString(4, c.getType());
            ps.setString(5, c.getStatus());
            ps.setBoolean(6, c.isDeleted());
            ps.setTimestamp(7, ts(c.getUpdatedAt()));
            ps.setString(8, c.getRejectionReason());
            ps.setDouble(9, c.getConsultationFee());
            ps.setInt(10, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("updateConsultation: " + e.getMessage()); }
    }

    public void deleteConsultation(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE consultations SET is_deleted = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("deleteConsultation: " + e.getMessage()); }
    }

    public void acceptConsultation(int id) {
        ConsultationsArij c = findById(id);
        if (c == null) return;
        c.setStatus("CONFIRMEE");
        c.setUpdatedAt(LocalDateTime.now());
        updateConsultation(c);
        notifyPatient(c.getPatientId(), "CONFIRMATION", id);
    }

    public void rejectConsultation(int id, String reason) {
        ConsultationsArij c = findById(id);
        if (c == null) return;
        c.setStatus("REFUSEE");
        c.setRejectionReason(reason);
        c.setUpdatedAt(LocalDateTime.now());
        updateConsultation(c);
        notifyPatient(c.getPatientId(), "REFUS", id);
    }

    public List<ConsultationsArij> filterConsultations(String status, String type) {
        if (status != null && !status.isEmpty()) return filterByStatus(status);
        if (type != null && !type.isEmpty()) return filterByType(type);
        return getMyConsultations();
    }

    private List<ConsultationsArij> filterByStatus(String status) {
        List<ConsultationsArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE status = ? AND (patient_id = ? OR doctor_id = ?) AND is_deleted = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, CURRENT_USER_ID); ps.setInt(3, CURRENT_USER_ID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("filterByStatus: " + e.getMessage()); }
        return list;
    }

    private List<ConsultationsArij> filterByType(String type) {
        List<ConsultationsArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE type = ? AND (patient_id = ? OR doctor_id = ?) AND is_deleted = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, type); ps.setInt(2, CURRENT_USER_ID); ps.setInt(3, CURRENT_USER_ID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("filterByType: " + e.getMessage()); }
        return list;
    }

    // ── Notifications ─────────────────────────────────────────────────────

    private void notifyDoctor(int doctorId, int consultationId) {
        saveNotification(doctorId, "Nouvelle consultation",
                "Vous avez une nouvelle demande de consultation #" + consultationId, "CONSULTATION");
    }

    private void notifyPatient(int patientId, String type, int consultationId) {
        saveNotification(patientId, "Mise à jour consultation",
                "Votre consultation #" + consultationId + " est " + type, type);
    }

    private void saveNotification(int userId, String title, String message, String type) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) VALUES (?,?,?,?,0,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title); ps.setString(3, message);
            ps.setString(4, type); ps.setTimestamp(5, ts(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("saveNotification: " + e.getMessage()); }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    public int countByStatus(String status, int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM consultations WHERE status = ? AND is_deleted = 0 AND (patient_id = ? OR doctor_id = ?)")) {
            ps.setString(1, status); ps.setInt(2, userId); ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("countByStatus: " + e.getMessage()); }
        return 0;
    }

    public LocalDateTime findNextConsultation(int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT MIN(consultation_date) FROM consultations WHERE consultation_date >= NOW() AND is_deleted = 0 AND patient_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { Timestamp t = rs.getTimestamp(1); if (t != null) return t.toLocalDateTime(); }
        } catch (SQLException e) { System.err.println("findNextConsultation: " + e.getMessage()); }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ConsultationsArij mapRow(ResultSet rs) throws SQLException {
        ConsultationsArij c = new ConsultationsArij();
        c.setId(rs.getInt("id"));
        c.setPatientId(rs.getInt("patient_id"));
        c.setDoctorId(rs.getInt("doctor_id"));
        Timestamp d = rs.getTimestamp("consultation_date");
        c.setConsultationDate(d != null ? d.toLocalDateTime() : null);
        c.setType(rs.getString("type"));
        c.setStatus(rs.getString("status"));
        c.setDeleted(rs.getBoolean("is_deleted"));
        Timestamp ca = rs.getTimestamp("created_at");
        c.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        c.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        c.setRejectionReason(rs.getString("rejection_reason"));
        c.setConsultationFee(rs.getDouble("consultation_fee"));
        return c;
    }

    private Timestamp ts(LocalDateTime dt) { return dt == null ? null : Timestamp.valueOf(dt); }
}

    /** Nombre de consultations annulées (REFUSEE) par le patient */
    public int countCancelled(int userId) {
        return countByStatus("REFUSEE", userId);
    }

    /** Taux de consultations confirmées sur le total */
    public double getConfirmationRate(int userId) {
        int total = countByStatus("EN_ATTENTE", userId)
                  + countByStatus("CONFIRMEE",  userId)
                  + countByStatus("REFUSEE",    userId)
                  + countByStatus("TERMINEE",   userId);
        if (total == 0) return 0;
        return (countByStatus("CONFIRMEE", userId) * 100.0) / total;
    }

    /** Consultations par semaine (4 dernières semaines) */
    public java.util.Map<String, Integer> countByWeek(int userId) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(consultation_date,'%Y-S%u') AS wk, COUNT(*) " +
                     "FROM consultations WHERE (patient_id=? OR doctor_id=?) " +
                     "AND consultation_date >= DATE_SUB(CURDATE(), INTERVAL 28 DAY) " +
                     "AND is_deleted=0 GROUP BY wk ORDER BY wk";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) { System.err.println("countByWeek: " + e.getMessage()); }
        return map;
    }

void main() {
}
