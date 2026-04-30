package esprit.fx.services;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.DisponibiliteMedecinArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer le calendrier des disponibilités médicales.
 * Gère les créneaux libres et les consultations confirmées.
 */
public class CalendrierServiceArij {

    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();

    /**
     * Récupère tous les créneaux de disponibilité pour un médecin.
     */
    public List<DisponibiliteMedecinArij> getDisponibilitesByMedecin(int medecinId) {
        List<DisponibiliteMedecinArij> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilite_medecin WHERE medecin_id = ? ORDER BY date_debut ASC";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                DisponibiliteMedecinArij dispo = new DisponibiliteMedecinArij();
                dispo.setId(rs.getInt("id"));
                dispo.setMedecinId(rs.getInt("medecin_id"));
                dispo.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                dispo.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                dispo.setEstOccupee(rs.getBoolean("est_occupee"));
                dispo.setTitre(rs.getString("titre"));
                list.add(dispo);
            }
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur getDisponibilitesByMedecin: " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère les créneaux libres (non occupés) pour un médecin.
     */
    public List<DisponibiliteMedecinArij> getCreneauxLibres(int medecinId) {
        List<DisponibiliteMedecinArij> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilite_medecin WHERE medecin_id = ? AND est_occupee = FALSE ORDER BY date_debut ASC";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                DisponibiliteMedecinArij dispo = new DisponibiliteMedecinArij();
                dispo.setId(rs.getInt("id"));
                dispo.setMedecinId(rs.getInt("medecin_id"));
                dispo.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                dispo.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                dispo.setEstOccupee(rs.getBoolean("est_occupee"));
                dispo.setTitre(rs.getString("titre"));
                list.add(dispo);
            }
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur getCreneauxLibres: " + e.getMessage());
        }
        return list;
    }

    /**
     * Ajoute un nouveau créneau de disponibilité.
     */
    public boolean ajouterCreneau(int medecinId, LocalDateTime debut, LocalDateTime fin, String titre) {
        String sql = "INSERT INTO disponibilite_medecin (medecin_id, date_debut, date_fin, est_occupee, titre) VALUES (?, ?, ?, FALSE, ?)";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ps.setTimestamp(2, Timestamp.valueOf(debut));
            ps.setTimestamp(3, Timestamp.valueOf(fin));
            ps.setString(4, titre != null ? titre : "Créneau libre");
            
            int rows = ps.executeUpdate();
            System.out.println("[CalendrierServiceArij] ✅ Créneau ajouté: " + rows + " ligne(s)");
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur ajouterCreneau: " + e.getMessage());
        }
        return false;
    }

    /**
     * Supprime un créneau de disponibilité (seulement s'il n'est pas occupé).
     */
    public boolean supprimerCreneau(int disponibiliteId) {
        String sql = "DELETE FROM disponibilite_medecin WHERE id = ? AND est_occupee = FALSE";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disponibiliteId);
            
            int rows = ps.executeUpdate();
            System.out.println("[CalendrierServiceArij] ✅ Créneau supprimé: " + rows + " ligne(s)");
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur supprimerCreneau: " + e.getMessage());
        }
        return false;
    }

    /**
     * Marque un créneau comme occupé.
     */
    public boolean marquerOccupee(int disponibiliteId) {
        String sql = "UPDATE disponibilite_medecin SET est_occupee = TRUE WHERE id = ?";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disponibiliteId);
            
            int rows = ps.executeUpdate();
            System.out.println("[CalendrierServiceArij] ✅ Créneau marqué occupé: " + rows + " ligne(s)");
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur marquerOccupee: " + e.getMessage());
        }
        return false;
    }

    /**
     * Récupère les consultations confirmées pour un médecin.
     */
    public List<ConsultationsArij> getConsultationsConfirmees(int medecinId) {
        List<ConsultationsArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE doctor_id = ? AND status = 'CONFIRMEE' ORDER BY consultation_date ASC";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                ConsultationsArij c = new ConsultationsArij();
                c.setId(rs.getInt("id"));
                c.setDoctorId(rs.getInt("doctor_id"));
                c.setPatientId(rs.getInt("patient_id"));
                c.setConsultationDate(rs.getTimestamp("consultation_date").toLocalDateTime());
                c.setStatus(rs.getString("status"));
                list.add(c);
            }
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur getConsultationsConfirmees: " + e.getMessage());
        }
        return list;
    }

    /**
     * Compte le nombre de créneaux libres pour un médecin.
     */
    public int countCreneauxLibres(int medecinId) {
        String sql = "SELECT COUNT(*) FROM disponibilite_medecin WHERE medecin_id = ? AND est_occupee = FALSE";
        
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[CalendrierServiceArij] Erreur countCreneauxLibres: " + e.getMessage());
        }
        return 0;
    }
}
