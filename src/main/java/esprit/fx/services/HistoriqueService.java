package esprit.fx.services;

import esprit.fx.entities.HistoriqueRendezVous;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoriqueService {

    private Connection conn;

    public HistoriqueService() {
        conn = MyDB.getInstance().getConnection();
    }

    /**
     * Enregistre un changement de statut dans historique_rdv.
     *
     * @param rdvId        ID du rendez-vous
     * @param ancienStatut statut avant changement (null si création)
     * @param nouveauStatut statut après changement
     * @param modifiePar   ID de l'utilisateur qui a fait le changement
     */
    public void enregistrerChangement(int rdvId, String ancienStatut,
                                      String nouveauStatut, int modifiePar) {
        enregistrerChangement(rdvId, ancienStatut, nouveauStatut, modifiePar, null);
    }

    public void enregistrerChangement(int rdvId, String ancienStatut,
                                      String nouveauStatut, int modifiePar,
                                      String commentaire) {
        String sql = "INSERT INTO historique_rdv " +
                     "(rdv_id, ancien_statut, nouveau_statut, date_changement, modifie_par, commentaire) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rdvId);
            ps.setString(2, ancienStatut);
            ps.setString(3, nouveauStatut);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            if (modifiePar > 0) ps.setInt(5, modifiePar);
            else                ps.setNull(5, Types.INTEGER);
            ps.setString(6, commentaire);
            ps.executeUpdate();
            System.out.println("✓ Historique enregistré : " + ancienStatut + " → " + nouveauStatut);
        } catch (SQLException e) {
            System.err.println("HistoriqueService erreur: " + e.getMessage());
        }
    }

    /**
     * Retourne tout l'historique d'un RDV, trié par date croissante.
     */
    public List<HistoriqueRendezVous> getHistoriqueParRdv(int rdvId) throws SQLException {
        String sql = """
            SELECT h.*,
                   u.username AS modifie_par_nom
            FROM historique_rdv h
            LEFT JOIN users u ON h.modifie_par = u.id
            WHERE h.rdv_id = ?
            ORDER BY h.date_changement ASC
            """;

        List<HistoriqueRendezVous> liste = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rdvId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistoriqueRendezVous h = mapRow(rs);
                    liste.add(h);
                }
            }
        }
        return liste;
    }

    /**
     * Nombre total de changements de statut pour un RDV.
     */
    public int getNombreChangements(int rdvId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM historique_rdv WHERE rdv_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rdvId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Supprime tout l'historique d'un RDV (utile avant suppression du RDV).
     */
    public void supprimerHistorique(int rdvId) throws SQLException {
        String sql = "DELETE FROM historique_rdv WHERE rdv_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rdvId);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------

    private HistoriqueRendezVous mapRow(ResultSet rs) throws SQLException {
        HistoriqueRendezVous h = new HistoriqueRendezVous();
        h.setId(rs.getInt("id"));
        h.setRdvId(rs.getInt("rdv_id"));
        h.setAncienStatut(rs.getString("ancien_statut"));
        h.setNouveauStatut(rs.getString("nouveau_statut"));
        Timestamp ts = rs.getTimestamp("date_changement");
        if (ts != null) h.setDateChangement(ts.toLocalDateTime());
        h.setModifiePar(rs.getInt("modifie_par"));
        h.setCommentaire(rs.getString("commentaire"));
        try { h.setModifieParNom(rs.getString("modifie_par_nom")); }
        catch (SQLException ignored) {}
        return h;
    }
}
