package esprit.fx.services;

import com.google.protobuf.Message;
import com.google.protobuf.Type;
import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.NotificationArij;
import esprit.fx.utils.MyDB;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceConsultationsArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // Accès centralisé au service de notification
    private final NotificationServiceArij notifService = NotificationServiceArij.getInstance();

    private static final DateTimeFormatter NOTIF_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public List<ConsultationsArij> getConsultationsByPatient(int patientId) {
        if (patientId <= 0) {
            return new ArrayList<>();
        }
        List<ConsultationsArij> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM consultations WHERE patient_id = ? AND is_deleted = 0 ORDER BY consultation_date DESC")) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("getConsultationsByPatient: " + e.getMessage());
        }
        return list;
    }

    public List<ConsultationsArij> getConsultationsByDoctor(int doctorId) {
        if (doctorId <= 0) {
            return new ArrayList<>();
        }
        List<ConsultationsArij> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM consultations WHERE doctor_id = ? AND is_deleted = 0 ORDER BY consultation_date DESC")) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("getConsultationsByDoctor: " + e.getMessage());
        }
        return list;
    }

    public ConsultationsArij findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM consultations WHERE id = ? AND is_deleted = 0")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("findById: " + e.getMessage());
        }
        return null;
    }

    public void createConsultation(ConsultationsArij c) {
        if (c == null) {
            return;
        }
        if (c.getPatientId() <= 0) {
            System.err.println("createConsultation: patientId is required (patients.id).");
            return;
        }
        if (c.getDoctorId() <= 0) {
            System.err.println("createConsultation: doctorId is required (doctors.id).");
            return;
        }
        if (c.getConsultationDate() == null) {
            System.err.println("createConsultation: consultationDate is required.");
            return;
        }

        c.setStatus("EN_ATTENTE");
        if (c.getType() == null || c.getType().isBlank()) {
            c.setType("ONLINE");
        }
        c.setCreatedAt(LocalDateTime.now());
        c.setDeleted(false);

        String sql = "INSERT INTO consultations " +
                "(patient_id, doctor_id, consultation_date, type, status, is_deleted, created_at, updated_at, rejection_reason, consultation_fee, lien_meet) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getDoctorId());
            ps.setTimestamp(3, ts(c.getConsultationDate()));
            ps.setString(4, toDbType(c.getType()));
            ps.setString(5, toDbStatus(c.getStatus()));
            ps.setBoolean(6, c.isDeleted());
            ps.setTimestamp(7, ts(c.getCreatedAt()));
            ps.setTimestamp(8, ts(c.getUpdatedAt()));
            ps.setString(9, c.getRejectionReason());
            ps.setBigDecimal(10, BigDecimal.valueOf(c.getConsultationFee()));
            ps.setString(11, c.getLienMeet());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                c.setId(keys.getInt(1));
            }
            notifyDoctorRequest(c);
        } catch (SQLException e) {
            System.err.println("createConsultation: " + e.getMessage());
        }
    }

    public boolean existsConsultation(int patientId, int doctorId, LocalDateTime date) {
        String sql = "SELECT COUNT(*) FROM consultations " +
                "WHERE patient_id = ? AND doctor_id = ? AND DATE(consultation_date) = DATE(?) AND is_deleted = 0";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setTimestamp(3, ts(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("existsConsultation: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a doctor already has a consultation at the exact date/time.
     * This mirrors the Symfony unique constraint (doctor + consultationDate).
     */
    public boolean isDoctorSlotTaken(int doctorId, LocalDateTime dateTime, int excludeConsultationId) {
        String sql = "SELECT COUNT(*) FROM consultations " +
                "WHERE doctor_id = ? AND consultation_date = ? AND is_deleted = 0";
        if (excludeConsultationId > 0) {
            sql += " AND id <> ?";
        }

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setTimestamp(2, ts(dateTime));
            if (excludeConsultationId > 0) {
                ps.setInt(3, excludeConsultationId);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("isDoctorSlotTaken: " + e.getMessage());
        }
        return false;
    }

    public void updateConsultation(ConsultationsArij c) {
        c.setUpdatedAt(LocalDateTime.now());

        String sql = "UPDATE consultations SET patient_id=?, doctor_id=?, consultation_date=?, type=?, status=?, " +
                "is_deleted=?, updated_at=?, rejection_reason=?, consultation_fee=?, lien_meet=? WHERE id=?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, c.getPatientId());
            ps.setInt(2, c.getDoctorId());
            ps.setTimestamp(3, ts(c.getConsultationDate()));
            ps.setString(4, toDbType(c.getType()));
            ps.setString(5, toDbStatus(c.getStatus()));
            ps.setBoolean(6, c.isDeleted());
            ps.setTimestamp(7, ts(c.getUpdatedAt()));
            ps.setString(8, c.getRejectionReason());
            ps.setBigDecimal(9, BigDecimal.valueOf(c.getConsultationFee()));
            ps.setString(10, c.getLienMeet());
            ps.setInt(11, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateConsultation: " + e.getMessage());
        }
    }

    public void deleteConsultation(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE consultations SET is_deleted = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteConsultation: " + e.getMessage());
        }
    }

    public void acceptConsultation(int id) {
        ConsultationsArij c = findById(id);
        if (c == null) {
            return;
        }
        c.setStatus("CONFIRMEE");
        c.setUpdatedAt(LocalDateTime.now());
        updateConsultation(c);
        notifyPatientApproved(c);

        // ── Marquer automatiquement le créneau de disponibilité comme occupé ──
        // Quand le médecin accepte une consultation, le créneau correspondant
        // dans disponibilite_medecin est marqué est_occupee = true.
        // Cela empêche d'autres patients de réserver le même créneau.
        marquerCreneauOccupe(c);
    }

    public void rejectConsultation(int id, String reason) {
        ConsultationsArij c = findById(id);
        if (c == null) {
            return;
        }
        c.setStatus("REFUSEE");
        c.setRejectionReason(reason);
        c.setUpdatedAt(LocalDateTime.now());
        updateConsultation(c);
        notifyPatientRejected(c);
    }

    public void completeConsultation(int id) {
        ConsultationsArij c = findById(id);
        if (c == null) {
            return;
        }
        c.setStatus("TERMINEE");
        c.setUpdatedAt(LocalDateTime.now());
        updateConsultation(c);
        notifyPatientCompleted(c);
    }

    public List<ConsultationsArij> filterConsultations(int actorId, boolean isDoctor, String status, String type) {
        if (actorId <= 0) {
            return new ArrayList<>();
        }
        if (status != null && !status.isBlank()) {
            return filterByStatus(actorId, isDoctor, status);
        }
        if (type != null && !type.isBlank()) {
            return filterByType(actorId, isDoctor, type);
        }
        return isDoctor ? getConsultationsByDoctor(actorId) : getConsultationsByPatient(actorId);
    }

    private List<ConsultationsArij> filterByStatus(int actorId, boolean isDoctor, String status) {
        List<ConsultationsArij> list = new ArrayList<>();
        String sql = isDoctor
                ? "SELECT * FROM consultations WHERE LOWER(status) = LOWER(?) AND doctor_id = ? AND is_deleted = 0"
                : "SELECT * FROM consultations WHERE LOWER(status) = LOWER(?) AND patient_id = ? AND is_deleted = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, toDbStatus(status));
            ps.setInt(2, actorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("filterByStatus: " + e.getMessage());
        }
        return list;
    }

    private List<ConsultationsArij> filterByType(int actorId, boolean isDoctor, String type) {
        List<ConsultationsArij> list = new ArrayList<>();
        String sql = isDoctor
                ? "SELECT * FROM consultations WHERE LOWER(type) = LOWER(?) AND doctor_id = ? AND is_deleted = 0"
                : "SELECT * FROM consultations WHERE LOWER(type) = LOWER(?) AND patient_id = ? AND is_deleted = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, toDbType(type));
            ps.setInt(2, actorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("filterByType: " + e.getMessage());
        }
        return list;
    }

    public int countByStatus(String status, int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM consultations WHERE LOWER(status) = LOWER(?) AND is_deleted = 0 AND (patient_id = ? OR doctor_id = ?)")) {
            ps.setString(1, toDbStatus(status));
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("countByStatus: " + e.getMessage());
        }
        return 0;
    }

    public int countCancelled(int userId) {
        return countByStatus("REFUSEE", userId);
    }

    public double getConfirmationRate(int userId) {
        int total = countByStatus("EN_ATTENTE", userId)
                + countByStatus("CONFIRMEE", userId)
                + countByStatus("REFUSEE", userId)
                + countByStatus("TERMINEE", userId);
        if (total == 0) {
            return 0;
        }
        return (countByStatus("CONFIRMEE", userId) * 100.0) / total;
    }

    public Map<String, Integer> countByWeek(int userId) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(consultation_date, '%Y-S%u') AS wk, COUNT(*) " +
                "FROM consultations WHERE (patient_id = ? OR doctor_id = ?) " +
                "AND consultation_date >= DATE_SUB(CURDATE(), INTERVAL 28 DAY) " +
                "AND is_deleted = 0 GROUP BY wk ORDER BY wk";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("countByWeek: " + e.getMessage());
        }
        return map;
    }

    public LocalDateTime findNextConsultation(int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT MIN(consultation_date) FROM consultations WHERE consultation_date >= NOW() AND is_deleted = 0 AND patient_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp t = rs.getTimestamp(1);
                if (t != null) {
                    return t.toLocalDateTime();
                }
            }
        } catch (SQLException e) {
            System.err.println("findNextConsultation: " + e.getMessage());
        }
        return null;
    }

    // ================================================================== //
    //  Notifications — délèguent à NotificationServiceArij               //
    // ================================================================== //
    /**
     * Message : "Nouvelle consultation de {patient} pour le {date}"
     * Type    : "info"
     */
    private void notifyDoctorRequest(ConsultationsArij c) {
        if (c == null) return;

        int doctorUserId = lookupDoctorUserId(c.getDoctorId());
        if (doctorUserId <= 0) return;

        String patientName = lookupPatientUsername(c.getPatientId());
        String when        = c.getConsultationDate() != null
            ? c.getConsultationDate().format(NOTIF_FMT) : "";

        String msg = "Nouvelle consultation de "
            + (patientName.isBlank() ? "un patient" : patientName)
            + " pour le " + when + ".";

        notifService.notifier(doctorUserId, msg, NotificationArij.TYPE_INFO, null);
    }

    /**
     * Médecin accepte une consultation → notifier le patient.
     * Message : "Consultation acceptée ! Lien Meet : {lien}"
     * Type    : "success"
     * Lien    : lien Google Meet (nullable)
     */
    private void notifyPatientApproved(ConsultationsArij c) {
        if (c == null) return;

        int patientUserId = lookupPatientUserId(c.getPatientId());
        if (patientUserId <= 0) return;

        String doctorName = lookupDoctorUsername(c.getDoctorId());
        String when       = c.getConsultationDate() != null
            ? c.getConsultationDate().format(NOTIF_FMT) : "";
        String lien       = c.getLienMeet();

        String msg = "Consultation acceptée ! Dr. " + doctorName
            + " vous attend le " + when + "."
            + (lien != null && !lien.isBlank() ? " Lien Meet : " + lien : "");

        notifService.notifier(patientUserId, msg, NotificationArij.TYPE_SUCCESS, lien);
    }

    /**
     * Médecin refuse une consultation → notifier le patient.
     * Type : "warning"
     */
    private void notifyPatientRejected(ConsultationsArij c) {
        if (c == null) return;

        int patientUserId = lookupPatientUserId(c.getPatientId());
        if (patientUserId <= 0) return;

        String doctorName = lookupDoctorUsername(c.getDoctorId());
        String reason     = c.getRejectionReason();

        String msg = "Consultation refusée par Dr. " + doctorName + ". "
            + (reason == null || reason.isBlank() ? "Aucune raison fournie." : reason.trim());

        notifService.notifier(patientUserId, msg, NotificationArij.TYPE_WARNING, null);
    }

    /**
     * Consultation terminée → notifier le patient.
     * Type : "info"
     */
    private void notifyPatientCompleted(ConsultationsArij c) {
        if (c == null) return;

        int patientUserId = lookupPatientUserId(c.getPatientId());
        if (patientUserId <= 0) return;

        String doctorName = lookupDoctorUsername(c.getDoctorId());
        String when       = c.getConsultationDate() != null
            ? c.getConsultationDate().format(NOTIF_FMT) : "";

        String msg = "Votre consultation avec Dr. " + doctorName
            + " du " + when + " est marquée comme terminée.";

        notifService.notifier(patientUserId, msg, NotificationArij.TYPE_INFO, null);
    }

    private int lookupDoctorUserId(int doctorId) {
        if (doctorId <= 0) return 0;
        try (PreparedStatement ps = conn().prepareStatement("SELECT user_id FROM doctors WHERE id = ?")) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("lookupDoctorUserId: " + e.getMessage());
            return 0;
        }
    }

    private String lookupDoctorUsername(int doctorId) {
        if (doctorId <= 0) return "Doctor";
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT u.username FROM doctors d JOIN users u ON u.id = d.user_id WHERE d.id = ?")) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String u = rs.getString(1);
                return u != null && !u.isBlank() ? u : ("Doctor #" + doctorId);
            }
        } catch (SQLException e) {
            System.err.println("lookupDoctorUsername: " + e.getMessage());
        }
        return "Doctor #" + doctorId;
    }

    private int lookupPatientUserId(int patientId) {
        if (patientId <= 0) return 0;
        try (PreparedStatement ps = conn().prepareStatement("SELECT user_id FROM patients WHERE id = ?")) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("lookupPatientUserId: " + e.getMessage());
            return 0;
        }
    }

    private String lookupPatientUsername(int patientId) {
        if (patientId <= 0) return "";
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT u.username FROM patients p JOIN users u ON u.id = p.user_id WHERE p.id = ?")) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String u = rs.getString(1);
                return u != null ? u : "";
            }
        } catch (SQLException e) {
            System.err.println("lookupPatientUsername: " + e.getMessage());
        }
        return "";
    }

    private ConsultationsArij mapRow(ResultSet rs) throws SQLException {
        ConsultationsArij c = new ConsultationsArij();
        c.setId(rs.getInt("id"));
        c.setPatientId(rs.getInt("patient_id"));

        Object doctorObj = rs.getObject("doctor_id");
        c.setDoctorId(doctorObj == null ? 0 : ((Number) doctorObj).intValue());

        Timestamp d = rs.getTimestamp("consultation_date");
        c.setConsultationDate(d != null ? d.toLocalDateTime() : null);
        c.setType(fromDbType(rs.getString("type")));
        c.setStatus(fromDbStatus(rs.getString("status")));
        c.setDeleted(rs.getBoolean("is_deleted"));

        Timestamp ca = rs.getTimestamp("created_at");
        c.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);

        Timestamp ua = rs.getTimestamp("updated_at");
        c.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);

        c.setRejectionReason(rs.getString("rejection_reason"));

        BigDecimal fee = rs.getBigDecimal("consultation_fee");
        c.setConsultationFee(fee != null ? fee.doubleValue() : 0.0);

        c.setLienMeet(rs.getString("lien_meet"));

        // Lire le champ sms_suivi_envoye (peut être absent sur ancienne BDD)
        try { c.setSmsSuiviEnvoye(rs.getBoolean("sms_suivi_envoye")); }
        catch (SQLException ignored) { c.setSmsSuiviEnvoye(false); }

        return c;
    }

    private Timestamp ts(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }

    private String toDbStatus(String status) {
        if (status == null || status.isBlank()) {
            return "en_attente";
        }
        String s = status.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        return switch (s) {
            case "en_attente", "pending" -> "en_attente";
            case "confirmee", "confirmed" -> "confirmee";
            case "refusee", "rejetee", "rejected", "cancelled", "canceled" -> "refusee";
            case "terminee", "completed", "done", "finished" -> "terminee";
            default -> s;
        };
    }

    private String fromDbStatus(String dbStatus) {
        if (dbStatus == null || dbStatus.isBlank()) {
            return "EN_ATTENTE";
        }
        String s = dbStatus.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        return switch (s) {
            case "en_attente", "pending" -> "EN_ATTENTE";
            case "confirmee", "confirmed" -> "CONFIRMEE";
            case "refusee", "rejetee", "rejected", "cancelled", "canceled" -> "REFUSEE";
            case "terminee", "completed", "done", "finished" -> "TERMINEE";
            default -> s.toUpperCase();
        };
    }

    private String toDbType(String type) {
        if (type == null || type.isBlank()) {
            return "online";
        }
        String t = type.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        return switch (t) {
            case "online", "en_ligne", "enligne" -> "online";
            case "cabinet", "in_person", "inperson" -> "cabinet";
            default -> t;
        };
    }

    private String fromDbType(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return "ONLINE";
        }
        String t = dbType.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        return switch (t) {
            case "online", "en_ligne", "enligne" -> "ONLINE";
            case "cabinet", "in_person", "inperson" -> "IN_PERSON";
            default -> t.toUpperCase();
        };
    }
}
