package tn.esprit.repositories.consultationonline;

import tn.esprit.entities.consultationonline.FactureArij;
import tn.esprit.utils.consultationonline.DatabaseConnectionArij;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FactureRepositoryArij {

    public List<FactureArij> findByPatientId(int patientId) {
        List<FactureArij> list = new ArrayList<>();
        String sql = "SELECT f.* FROM factures f JOIN paiements p ON f.paiement_id = p.id WHERE p.patient_id = ?";
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

    public FactureArij findById(int id) {
        FactureArij facture = null;
        String sql = "SELECT * FROM factures WHERE id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    facture = mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("findById error: " + e.getMessage());
        }
        return facture;
    }

    public void create(FactureArij f) {
        String sql = "INSERT INTO factures (numero_facture, date_emission, montant, chemin_pdf, paiement_id, ordonnance_id) VALUES (?,?,?,?,?,?)";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getNumeroFacture());
            ps.setTimestamp(2, toTimestamp(f.getDateEmission()));
            ps.setDouble(3, f.getMontant());
            ps.setString(4, f.getCheminPdf());
            ps.setInt(5, f.getPaiementId());
            ps.setInt(6, f.getOrdonnanceId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("create facture error: " + e.getMessage());
        }
    }

    private FactureArij mapRow(ResultSet rs) throws SQLException {
        FactureArij f = new FactureArij();
        f.setId(rs.getInt("id"));
        f.setNumeroFacture(rs.getString("numero_facture"));
        Timestamp emission = rs.getTimestamp("date_emission");
        f.setDateEmission(emission != null ? emission.toLocalDateTime() : null);
        f.setMontant(rs.getDouble("montant"));
        f.setCheminPdf(rs.getString("chemin_pdf"));
        f.setPaiementId(rs.getInt("paiement_id"));
        f.setOrdonnanceId(rs.getInt("ordonnance_id"));
        return f;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }
}
