package esprit.fx.services;

import esprit.fx.entities.NotificationArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC pour la table "notifications" partagée avec Symfony.
 *
 * Colonnes BDD attendues :
 *   id, user_id, title, message, type, is_read, created_at, link
 */
public class NotificationDAOArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ================================================================== //
    //  Lecture                                                            //
    // ================================================================== //

    /**
     * Retourne toutes les notifications d'un utilisateur,
     * triées par date décroissante (plus récentes en premier).
     *
     * @param userId identifiant de l'utilisateur (user_id en BDD)
     * @return liste des notifications, vide si userId invalide
     */
    public List<NotificationArij> findByDestinataire(Long userId) {
        List<NotificationArij> list = new ArrayList<>();
        if (userId == null || userId <= 0) return list;

        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[NotificationDAOArij] findByDestinataire: " + e.getMessage());
        }
        return list;
    }

    /**
     * Compte le nombre de notifications non lues pour un utilisateur.
     * Utilisé pour le badge rouge sur la cloche.
     *
     * @param userId identifiant de l'utilisateur
     * @return nombre de notifications non lues (0 si userId invalide)
     */
    public long countNonLues(Long userId) {
        if (userId == null || userId <= 0) return 0L;

        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("[NotificationDAOArij] countNonLues: " + e.getMessage());
        }
        return 0L;
    }

    // ================================================================== //
    //  Écriture                                                           //
    // ================================================================== //

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     * Appelé quand l'utilisateur ouvre le panneau de notifications.
     *
     * @param userId identifiant de l'utilisateur
     */
    public void marquerToutesLues(Long userId) {
        if (userId == null || userId <= 0) return;

        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            int updated = ps.executeUpdate();
            System.out.println("[NotificationDAOArij] marquerToutesLues: "
                + updated + " notification(s) marquée(s) lues pour userId=" + userId);
        } catch (SQLException e) {
            System.err.println("[NotificationDAOArij] marquerToutesLues: " + e.getMessage());
        }
    }

    /**
     * Marque une seule notification comme lue.
     *
     * @param notifId identifiant de la notification
     */
    public void marquerLue(Long notifId) {
        if (notifId == null || notifId <= 0) return;

        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationDAOArij] marquerLue: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde une nouvelle notification en BDD.
     *
     * @param n notification à persister
     */
    public void save(NotificationArij n) {
        if (n == null || n.getDestinataireId() == null) return;

        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at, link) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, n.getDestinataireId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType() != null ? n.getType() : NotificationArij.TYPE_INFO);
            ps.setBoolean(5, n.isLu());
            ps.setTimestamp(6, n.getCreatedAt() != null
                ? Timestamp.valueOf(n.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
            ps.setString(7, n.getLien());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) n.setId(keys.getLong(1));
            NotificationWebSocketArij.getInstance().publishNotification(n.getDestinataireId().intValue());
        } catch (SQLException e) {
            System.err.println("[NotificationDAOArij] save: " + e.getMessage());
        }
    }

    // ================================================================== //
    //  Mapping ResultSet → entité                                        //
    // ================================================================== //

    private NotificationArij mapRow(ResultSet rs) throws SQLException {
        NotificationArij n = new NotificationArij();
        n.setId(rs.getLong("id"));
        n.setDestinataireId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setLu(rs.getBoolean("is_read"));
        Timestamp ca = rs.getTimestamp("created_at");
        n.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        // colonne "link" dans la table Symfony
        try { n.setLien(rs.getString("link")); } catch (SQLException ignored) {}
        return n;
    }
}
