package esprit.fx.services;

import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.OrdonnanceArij;
import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceOrdonnanceArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    public OrdonnanceArij getByConsultationId(int consultationId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM ordonnances WHERE consultation_id = ?")) {
            ps.setInt(1, consultationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("getByConsultationId: " + e.getMessage());
        }
        return null;
    }

    public OrdonnanceArij findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM ordonnances WHERE id = ?")) {
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

    public void createOrdonnance(OrdonnanceArij o, List<LigneOrdonnanceArij> lignes) {
        LocalDateTime now = LocalDateTime.now();
        o.setNumeroOrdonnance("ORD-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + randomChars(5));
        o.setTokenVerification(randomHex(16));
        o.setDateEmission(now);
        o.setCreatedAt(now);

        String sql = "INSERT INTO ordonnances (consultation_id, doctor_id, content, diagnosis, numero_ordonnance, date_emission, date_validite, signature_path, instructions, created_at, updated_at, token_verification, document_nom, document_size, document_mime_type, document_original_name) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getConsultationId());
            ps.setInt(2, o.getDoctorId());
            ps.setString(3, o.getContent());
            ps.setString(4, o.getDiagnosis());
            ps.setString(5, o.getNumeroOrdonnance());
            ps.setTimestamp(6, ts(o.getDateEmission()));
            ps.setTimestamp(7, ts(o.getDateValidite()));
            ps.setString(8, o.getSignaturePath());
            ps.setString(9, o.getInstructions());
            ps.setTimestamp(10, ts(o.getCreatedAt()));
            ps.setTimestamp(11, ts(o.getUpdatedAt()));
            ps.setString(12, o.getTokenVerification());
            ps.setString(13, o.getDocumentNom());
            ps.setObject(14, o.getDocumentSize() == 0 ? null : o.getDocumentSize());
            ps.setString(15, o.getDocumentMimeType());
            ps.setString(16, o.getDocumentOriginalName());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int ordId = keys.next() ? keys.getInt(1) : o.getId();

            if (lignes != null) {
                for (LigneOrdonnanceArij l : lignes) {
                    l.setOrdonnanceId(ordId);
                    createLigne(l);
                }
            }
            notifyPatient(o.getConsultationId(), ordId);
        } catch (SQLException e) {
            System.err.println("createOrdonnance: " + e.getMessage());
        }
    }

    public void updateOrdonnance(OrdonnanceArij o, List<LigneOrdonnanceArij> lignes) {
        deleteLignesByOrdonnanceId(o.getId());

        String sql = "UPDATE ordonnances SET consultation_id=?, doctor_id=?, content=?, diagnosis=?, date_validite=?, instructions=?, updated_at=?, document_nom=?, document_size=?, document_mime_type=?, document_original_name=? WHERE id=?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, o.getConsultationId());
            ps.setInt(2, o.getDoctorId());
            ps.setString(3, o.getContent());
            ps.setString(4, o.getDiagnosis());
            ps.setTimestamp(5, ts(o.getDateValidite()));
            ps.setString(6, o.getInstructions());
            ps.setTimestamp(7, ts(LocalDateTime.now()));
            ps.setString(8, o.getDocumentNom());
            ps.setObject(9, o.getDocumentSize() == 0 ? null : o.getDocumentSize());
            ps.setString(10, o.getDocumentMimeType());
            ps.setString(11, o.getDocumentOriginalName());
            ps.setInt(12, o.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateOrdonnance: " + e.getMessage());
        }

        if (lignes != null) {
            for (LigneOrdonnanceArij l : lignes) {
                l.setOrdonnanceId(o.getId());
                createLigne(l);
            }
        }
    }

    public void deleteOrdonnance(int id) {
        deleteLignesByOrdonnanceId(id);
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM ordonnances WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteOrdonnance: " + e.getMessage());
        }
    }

    public List<LigneOrdonnanceArij> getLignesByOrdonnanceId(int ordonnanceId) {
        List<LigneOrdonnanceArij> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM ligne_ordonnance WHERE ordonnance_id = ?")) {
            ps.setInt(1, ordonnanceId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapLigne(rs));
            }
        } catch (SQLException e) {
            System.err.println("getLignesByOrdonnanceId: " + e.getMessage());
        }
        return list;
    }

    public int countOrdonnancesByDoctor(int doctorId) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM ordonnances WHERE doctor_id = ?")) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("countOrdonnancesByDoctor: " + e.getMessage());
        }
        return 0;
    }

    public void updateConsultationFee(int consultationId, double fee) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE consultations SET consultation_fee = ? WHERE id = ?")) {
            ps.setDouble(1, fee);
            ps.setInt(2, consultationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateConsultationFee: " + e.getMessage());
        }
    }

    private void createLigne(LigneOrdonnanceArij l) {
        String sql = "INSERT INTO ligne_ordonnance (ordonnance_id, nom_medicament, dosage, quantite, duree_traitement, instructions) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, l.getOrdonnanceId());
            ps.setString(2, l.getNomMedicament());
            ps.setString(3, l.getDosage());
            ps.setInt(4, l.getQuantite());
            ps.setString(5, l.getDureeTraitement());
            ps.setString(6, l.getInstructions());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("createLigne: " + e.getMessage());
        }
    }

    private void deleteLignesByOrdonnanceId(int ordonnanceId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM ligne_ordonnance WHERE ordonnance_id = ?")) {
            ps.setInt(1, ordonnanceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteLignesByOrdonnanceId: " + e.getMessage());
        }
    }

    private void notifyPatient(int consultationId, int ordonnanceId) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) VALUES (?,?,?,?,0,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            int patientUserId = findPatientUserIdByConsultationId(consultationId);
            if (patientUserId <= 0) {
                return;
            }
            ps.setInt(1, patientUserId);
            ps.setString(2, "Nouvelle ordonnance");
            ps.setString(3, "Une ordonnance #" + ordonnanceId + " a ete ajoutee pour votre consultation #" + consultationId);
            ps.setString(4, "ORDONNANCE");
            ps.setTimestamp(5, ts(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("notifyPatient: " + e.getMessage());
        }
    }

    private int findPatientUserIdByConsultationId(int consultationId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT p.user_id FROM consultations c JOIN patients p ON p.id = c.patient_id WHERE c.id = ?")) {
            ps.setInt(1, consultationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("findPatientUserIdByConsultationId: " + e.getMessage());
        }
        return 0;
    }

    private OrdonnanceArij mapRow(ResultSet rs) throws SQLException {
        OrdonnanceArij o = new OrdonnanceArij();
        o.setId(rs.getInt("id"));
        o.setConsultationId(rs.getInt("consultation_id"));
        o.setDoctorId(rs.getInt("doctor_id"));
        o.setContent(rs.getString("content"));
        o.setDiagnosis(rs.getString("diagnosis"));
        o.setNumeroOrdonnance(rs.getString("numero_ordonnance"));

        Timestamp de = rs.getTimestamp("date_emission");
        o.setDateEmission(de != null ? de.toLocalDateTime() : null);

        Timestamp dv = rs.getTimestamp("date_validite");
        o.setDateValidite(dv != null ? dv.toLocalDateTime() : null);

        o.setSignaturePath(rs.getString("signature_path"));
        o.setInstructions(rs.getString("instructions"));

        Timestamp ca = rs.getTimestamp("created_at");
        o.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);

        Timestamp ua = rs.getTimestamp("updated_at");
        o.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);

        o.setTokenVerification(rs.getString("token_verification"));
        o.setDocumentNom(rs.getString("document_nom"));

        Object sizeObj = rs.getObject("document_size");
        o.setDocumentSize(sizeObj == null ? 0 : ((Number) sizeObj).intValue());

        o.setDocumentMimeType(rs.getString("document_mime_type"));
        o.setDocumentOriginalName(rs.getString("document_original_name"));
        return o;
    }

    private LigneOrdonnanceArij mapLigne(ResultSet rs) throws SQLException {
        LigneOrdonnanceArij l = new LigneOrdonnanceArij();
        l.setId(rs.getInt("id"));
        l.setOrdonnanceId(rs.getInt("ordonnance_id"));
        l.setNomMedicament(rs.getString("nom_medicament"));
        l.setDosage(rs.getString("dosage"));
        l.setQuantite(rs.getInt("quantite"));
        l.setDureeTraitement(rs.getString("duree_traitement"));
        l.setInstructions(rs.getString("instructions"));
        return l;
    }

    private Timestamp ts(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }

    private String randomChars(int n) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, n).toUpperCase();
    }

    private String randomHex(int n) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, n);
    }
}
