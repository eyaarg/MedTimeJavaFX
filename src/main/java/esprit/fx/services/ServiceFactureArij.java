package esprit.fx.services;

import esprit.fx.entities.FactureArij;
import esprit.fx.services.NotificationServiceArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceFactureArij {
    private Connection conn() { return MyDB.getInstance().getConnection(); }

    // Accès centralisé au service de notification
    private final NotificationServiceArij notifService = NotificationServiceArij.getInstance();

    public List<FactureArij> getFacturesByPatient(int patientId) {
        if (patientId <= 0) {
            return new ArrayList<>();
        }
        List<FactureArij> list = new ArrayList<>();
        String sql = "SELECT f.* FROM facture f JOIN paiement p ON f.paiement_id = p.id WHERE p.patient_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getFacturesByPatient: " + e.getMessage()); }
        return list;
    }

    public FactureArij findByOrdonnanceId(int ordonnanceId) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM facture WHERE ordonnance_id = ?")) {
            ps.setInt(1, ordonnanceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("findByOrdonnanceId: " + e.getMessage()); }
        return null;
    }

    /**
     * Crée un paiement + une facture liés à la consultation.
     * Appelé par le médecin lors de la création d'une ordonnance.
     */
    public void createFactureForConsultation(int consultationId, int patientId, double montant, int ordonnanceId) {
        // 1. Créer le paiement
        int paiementId = -1;
        String sqlPaiement = "INSERT INTO paiement (montant, methode, status, created_at, consultation_id, patient_id) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sqlPaiement, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, montant);
            ps.setString(2, "EN_ATTENTE");
            ps.setString(3, "EN_ATTENTE");
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, consultationId);
            ps.setInt(6, patientId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) paiementId = keys.getInt(1);
        } catch (SQLException e) { System.err.println("createPaiement: " + e.getMessage()); return; }

        // 2. Créer la facture
        String numero = "FAC-" + System.currentTimeMillis();
        String sqlFacture = "INSERT INTO facture (numero_facture, date_emission, montant, paiement_id, ordonnance_id) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sqlFacture)) {
            ps.setString(1, numero);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(3, montant);
            ps.setInt(4, paiementId);
            ps.setInt(5, ordonnanceId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("createFacture: " + e.getMessage()); return; }

        // 3. Notifier le patient via NotificationServiceArij
        int patientUserId = findPatientUserId(patientId);
        if (patientUserId > 0) {
            String msg = "Facture disponible : " + montant + " TND pour votre consultation #" + consultationId + ".";
            notifService.notifier(patientUserId, msg, "info", null);
        }
    }

    public FactureArij findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM facture WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("findById: " + e.getMessage()); }
        return null;
    }

    private int findPatientUserId(int patientId) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT user_id FROM patients WHERE id = ?")) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("findPatientUserId: " + e.getMessage());
        }
        return 0;
    }

    private FactureArij mapRow(ResultSet rs) throws SQLException {
        FactureArij f = new FactureArij();
        f.setId(rs.getInt("id"));
        f.setNumeroFacture(rs.getString("numero_facture"));
        Timestamp de = rs.getTimestamp("date_emission");
        f.setDateEmission(de != null ? de.toLocalDateTime() : null);
        f.setMontant(rs.getDouble("montant"));
        f.setCheminPdf(rs.getString("chemin_pdf"));
        f.setPaiementId(rs.getInt("paiement_id"));
        f.setOrdonnanceId(rs.getInt("ordonnance_id"));
        return f;
    }
}
