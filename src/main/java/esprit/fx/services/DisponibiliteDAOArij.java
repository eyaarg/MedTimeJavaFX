package esprit.fx.services;

import esprit.fx.entities.DisponibiliteMedecinArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC pour la table "disponibilite_medecin" partagée avec Symfony.
 *
 * Colonnes BDD :
 *   id, medecin_id, date_debut, date_fin, est_occupee, created_at
 *
 * Méthodes principales :
 *   findByMedecin(medecinId)    → tous les créneaux d'un médecin
 *   findCreneauxLibres(medecinId) → créneaux non occupés
 *   marquerOccupee(id)          → est_occupee = 1 (appelé à l'acceptation)
 *   save(dispo)                 → INSERT
 *   delete(id)                  → DELETE (si non occupé)
 */
public class DisponibiliteDAOArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ================================================================== //
    //  Lecture                                                            //
    // ================================================================== //

    /**
     * Retourne tous les créneaux d'un médecin (libres + occupés),
     * triés par date de début croissante.
     *
     * @param medecinId id de l'utilisateur médecin (users.id)
     * @return liste des créneaux, vide si medecinId invalide
     */
    public List<DisponibiliteMedecinArij> findByMedecin(Long medecinId) {
        List<DisponibiliteMedecinArij> list = new ArrayList<>();
        if (medecinId == null || medecinId <= 0) return list;

        String sql = """
            SELECT d.*, u.username AS medecin_nom
            FROM disponibilite_medecin d
            LEFT JOIN users u ON u.id = d.medecin_id
            WHERE d.medecin_id = ?
            ORDER BY d.date_debut ASC
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] findByMedecin: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retourne uniquement les créneaux libres (est_occupee = false)
     * d'un médecin, triés par date de début.
     *
     * Utilisé par le calendrier patient pour afficher les créneaux disponibles.
     *
     * @param medecinId id de l'utilisateur médecin
     * @return liste des créneaux libres
     */
    public List<DisponibiliteMedecinArij> findCreneauxLibres(Long medecinId) {
        List<DisponibiliteMedecinArij> list = new ArrayList<>();
        if (medecinId == null || medecinId <= 0) return list;

        String sql = """
            SELECT d.*, u.username AS medecin_nom
            FROM disponibilite_medecin d
            LEFT JOIN users u ON u.id = d.medecin_id
            WHERE d.medecin_id = ?
              AND d.est_occupee = 0
              AND d.date_debut >= NOW()
            ORDER BY d.date_debut ASC
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] findCreneauxLibres: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retourne tous les créneaux libres de tous les médecins.
     * Utilisé par le calendrier global.
     */
    public List<DisponibiliteMedecinArij> findTousCreneauxLibres() {
        List<DisponibiliteMedecinArij> list = new ArrayList<>();
        String sql = """
            SELECT d.*, u.username AS medecin_nom
            FROM disponibilite_medecin d
            LEFT JOIN users u ON u.id = d.medecin_id
            WHERE d.est_occupee = 0
              AND d.date_debut >= NOW()
            ORDER BY d.date_debut ASC
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] findTousCreneauxLibres: " + e.getMessage());
        }
        return list;
    }

    /**
     * Trouve le créneau libre d'un médecin qui contient une date/heure donnée.
     * Utilisé pour marquer automatiquement le créneau lors de l'acceptation.
     *
     * @param medecinId   id du médecin
     * @param dateConsult date/heure de la consultation acceptée
     * @return le créneau correspondant, ou null si aucun
     */
    public DisponibiliteMedecinArij findCreneauPourConsultation(Long medecinId,
                                                                  LocalDateTime dateConsult) {
        if (medecinId == null || dateConsult == null) return null;

        String sql = """
            SELECT d.*, u.username AS medecin_nom
            FROM disponibilite_medecin d
            LEFT JOIN users u ON u.id = d.medecin_id
            WHERE d.medecin_id = ?
              AND d.est_occupee = 0
              AND d.date_debut <= ?
              AND d.date_fin   >= ?
            ORDER BY d.date_debut ASC
            LIMIT 1
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setTimestamp(2, Timestamp.valueOf(dateConsult));
            ps.setTimestamp(3, Timestamp.valueOf(dateConsult));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] findCreneauPourConsultation: "
                + e.getMessage());
        }
        return null;
    }

    // ================================================================== //
    //  Écriture                                                           //
    // ================================================================== //

    /**
     * Insère un nouveau créneau de disponibilité.
     *
     * @param dispo créneau à persister (medecinId, dateDebut, dateFin requis)
     * @return true si l'insertion a réussi
     */
    public boolean save(DisponibiliteMedecinArij dispo) {
        if (dispo == null || dispo.getMedecinId() <= 0
                || dispo.getDateDebut() == null || dispo.getDateFin() == null) {
            System.err.println("[DisponibiliteDAOArij] save: paramètres invalides.");
            return false;
        }
        if (!dispo.getDateFin().isAfter(dispo.getDateDebut())) {
            System.err.println("[DisponibiliteDAOArij] save: dateFin doit être après dateDebut.");
            return false;
        }

        String sql = """
            INSERT INTO disponibilite_medecin
                (medecin_id, date_debut, date_fin, est_occupee, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, dispo.getMedecinId());
            ps.setTimestamp(2, Timestamp.valueOf(dispo.getDateDebut()));
            ps.setTimestamp(3, Timestamp.valueOf(dispo.getDateFin()));
            ps.setBoolean(4, dispo.isEstOccupee());
            ps.setTimestamp(5, Timestamp.valueOf(
                dispo.getCreatedAt() != null ? dispo.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) dispo.setId(keys.getInt(1));

            System.out.println("[DisponibiliteDAOArij] ✓ Créneau créé id=" + dispo.getId());
            return true;

        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] save: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marque un créneau comme occupé (est_occupee = 1).
     *
     * Appelé automatiquement par ServiceConsultationsArij.acceptConsultation()
     * quand le médecin accepte une consultation sur ce créneau.
     *
     * @param disponibiliteId id du créneau à marquer
     */
    public void marquerOccupee(Long disponibiliteId) {
        if (disponibiliteId == null || disponibiliteId <= 0) return;

        String sql = "UPDATE disponibilite_medecin SET est_occupee = 1 WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, disponibiliteId);
            int updated = ps.executeUpdate();
            System.out.println("[DisponibiliteDAOArij] ✓ Créneau #" + disponibiliteId
                + " marqué occupé (" + updated + " ligne).");
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] marquerOccupee: " + e.getMessage());
        }
    }

    /**
     * Supprime un créneau uniquement s'il est libre (est_occupee = false).
     * Un créneau occupé ne peut pas être supprimé.
     *
     * @param disponibiliteId id du créneau
     * @return true si supprimé, false si occupé ou introuvable
     */
    public boolean delete(Long disponibiliteId) {
        if (disponibiliteId == null || disponibiliteId <= 0) return false;

        // Vérifier que le créneau est libre avant suppression
        String checkSql = "SELECT est_occupee FROM disponibilite_medecin WHERE id = ?";
        try (PreparedStatement check = conn().prepareStatement(checkSql)) {
            check.setLong(1, disponibiliteId);
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                System.err.println("[DisponibiliteDAOArij] delete: créneau #"
                    + disponibiliteId + " introuvable.");
                return false;
            }
            if (rs.getBoolean("est_occupee")) {
                System.err.println("[DisponibiliteDAOArij] delete: créneau #"
                    + disponibiliteId + " occupé — suppression refusée.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] delete (check): " + e.getMessage());
            return false;
        }

        String sql = "DELETE FROM disponibilite_medecin WHERE id = ? AND est_occupee = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, disponibiliteId);
            int deleted = ps.executeUpdate();
            System.out.println("[DisponibiliteDAOArij] ✓ Créneau #" + disponibiliteId
                + " supprimé (" + deleted + " ligne).");
            return deleted > 0;
        } catch (SQLException e) {
            System.err.println("[DisponibiliteDAOArij] delete: " + e.getMessage());
            return false;
        }
    }

    // ================================================================== //
    //  Mapping                                                            //
    // ================================================================== //

    private DisponibiliteMedecinArij mapRow(ResultSet rs) throws SQLException {
        DisponibiliteMedecinArij d = new DisponibiliteMedecinArij();
        d.setId(rs.getInt("id"));
        d.setMedecinId(rs.getInt("medecin_id"));

        Timestamp debut = rs.getTimestamp("date_debut");
        d.setDateDebut(debut != null ? debut.toLocalDateTime() : null);

        Timestamp fin = rs.getTimestamp("date_fin");
        d.setDateFin(fin != null ? fin.toLocalDateTime() : null);

        d.setEstOccupee(rs.getBoolean("est_occupee"));

        Timestamp ca = rs.getTimestamp("created_at");
        d.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);

        // Champ transient (JOIN)
        try { d.setMedecinNom(rs.getString("medecin_nom")); }
        catch (SQLException ignored) {}

        return d;
    }
}
