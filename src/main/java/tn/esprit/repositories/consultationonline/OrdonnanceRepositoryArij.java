package tn.esprit.repositories.consultationonline;

import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.entities.consultationonline.OrdonnanceArij;
import tn.esprit.utils.consultationonline.DatabaseConnectionArij;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceRepositoryArij {

    public OrdonnanceArij findByConsultationId(int consultationId) {
        OrdonnanceArij ordonnance = null;
        String sql = "SELECT * FROM ordonnances WHERE consultation_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, consultationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ordonnance = mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("findByConsultationId error: " + e.getMessage());
        }
        return ordonnance;
    }

    public OrdonnanceArij findById(int id) {
        OrdonnanceArij ordonnance = null;
        String sql = "SELECT * FROM ordonnances WHERE id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ordonnance = mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("findById error: " + e.getMessage());
        }
        return ordonnance;
    }

    public void create(OrdonnanceArij o) {
        String sql = "INSERT INTO ordonnances (consultation_id, doctor_id, content, diagnosis, numero_ordonnance, date_emission, date_validite, signature_path, instructions, created_at, updated_at, token_verification, document_nom, document_size, document_mime_type, document_original_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, o.getConsultationId());
            ps.setInt(2, o.getDoctorId());
            ps.setString(3, o.getContent());
            ps.setString(4, o.getDiagnosis());
            ps.setString(5, o.getNumeroOrdonnance());
            ps.setTimestamp(6, toTimestamp(o.getDateEmission()));
            ps.setTimestamp(7, toTimestamp(o.getDateValidite()));
            ps.setString(8, o.getSignaturePath());
            ps.setString(9, o.getInstructions());
            ps.setTimestamp(10, toTimestamp(o.getCreatedAt()));
            ps.setTimestamp(11, toTimestamp(o.getUpdatedAt()));
            ps.setString(12, o.getTokenVerification());
            ps.setString(13, o.getDocumentNom());
            ps.setInt(14, o.getDocumentSize());
            ps.setString(15, o.getDocumentMimeType());
            ps.setString(16, o.getDocumentOriginalName());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("create ordonnance error: " + e.getMessage());
        }
    }

    public void update(OrdonnanceArij o) {
        String sql = "UPDATE ordonnances SET consultation_id=?, doctor_id=?, content=?, diagnosis=?, numero_ordonnance=?, date_emission=?, date_validite=?, signature_path=?, instructions=?, created_at=?, updated_at=?, token_verification=?, document_nom=?, document_size=?, document_mime_type=?, document_original_name=? WHERE id=?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, o.getConsultationId());
            ps.setInt(2, o.getDoctorId());
            ps.setString(3, o.getContent());
            ps.setString(4, o.getDiagnosis());
            ps.setString(5, o.getNumeroOrdonnance());
            ps.setTimestamp(6, toTimestamp(o.getDateEmission()));
            ps.setTimestamp(7, toTimestamp(o.getDateValidite()));
            ps.setString(8, o.getSignaturePath());
            ps.setString(9, o.getInstructions());
            ps.setTimestamp(10, toTimestamp(o.getCreatedAt()));
            ps.setTimestamp(11, toTimestamp(o.getUpdatedAt()));
            ps.setString(12, o.getTokenVerification());
            ps.setString(13, o.getDocumentNom());
            ps.setInt(14, o.getDocumentSize());
            ps.setString(15, o.getDocumentMimeType());
            ps.setString(16, o.getDocumentOriginalName());
            ps.setInt(17, o.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("update ordonnance error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM ordonnances WHERE id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("delete ordonnance error: " + e.getMessage());
        }
    }

    public List<OrdonnanceArij> findByDoctorId(int doctorId) {
        List<OrdonnanceArij> list = new ArrayList<>();
        String sql = "SELECT * FROM ordonnances WHERE doctor_id = ?";
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

    private OrdonnanceArij mapRow(ResultSet rs) throws SQLException {
        OrdonnanceArij o = new OrdonnanceArij();
        o.setId(rs.getInt("id"));
        o.setConsultationId(rs.getInt("consultation_id"));
        o.setDoctorId(rs.getInt("doctor_id"));
        o.setContent(rs.getString("content"));
        o.setDiagnosis(rs.getString("diagnosis"));
        o.setNumeroOrdonnance(rs.getString("numero_ordonnance"));
        Timestamp emission = rs.getTimestamp("date_emission");
        o.setDateEmission(emission != null ? emission.toLocalDateTime() : null);
        Timestamp validite = rs.getTimestamp("date_validite");
        o.setDateValidite(validite != null ? validite.toLocalDateTime() : null);
        o.setSignaturePath(rs.getString("signature_path"));
        o.setInstructions(rs.getString("instructions"));
        Timestamp created = rs.getTimestamp("created_at");
        o.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        Timestamp updated = rs.getTimestamp("updated_at");
        o.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);
        o.setTokenVerification(rs.getString("token_verification"));
        o.setDocumentNom(rs.getString("document_nom"));
        o.setDocumentSize(rs.getInt("document_size"));
        o.setDocumentMimeType(rs.getString("document_mime_type"));
        o.setDocumentOriginalName(rs.getString("document_original_name"));
        o.setLignes(new ArrayList<LigneOrdonnanceArij>());
        return o;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }
}
