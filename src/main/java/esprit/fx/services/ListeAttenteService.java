package esprit.fx.services;

import esprit.fx.entities.ListeAttente;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ListeAttenteService {

    private Connection conn;

    public ListeAttenteService() {
        conn = MyDB.getInstance().getConnection();
    }

    // =========================================================================
    // INSCRIPTION
    // =========================================================================

    /**
     * Inscrit un patient en liste d'attente pour un médecin.
     * Vérifie d'abord qu'il n'est pas déjà inscrit (EN_ATTENTE) pour ce médecin.
     *
     * @return l'entrée créée, ou null si déjà inscrit
     */
    public ListeAttente inscrire(int patientId, int doctorId,
                                 LocalDate dateSouhaitee, String plageHoraire)
            throws SQLException {

        // Vérifier doublon actif
        if (estDejaInscrit(patientId, doctorId)) {
            System.out.println("Patient " + patientId + " déjà en attente pour médecin " + doctorId);
            return null;
        }

        ListeAttente la = new ListeAttente(patientId, doctorId, dateSouhaitee, plageHoraire);

        String sql = "INSERT INTO liste_attente " +
                     "(patient_id, doctor_id, date_souhaitee, plage_horaire, " +
                     " date_inscription, statut, date_expiration) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            if (dateSouhaitee != null)
                ps.setDate(3, Date.valueOf(dateSouhaitee));
            else
                ps.setNull(3, Types.DATE);
            ps.setString(4, plageHoraire);
            ps.setTimestamp(5, Timestamp.valueOf(la.getDateInscription()));
            ps.setString(6, la.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(la.getDateExpiration()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) la.setId(rs.getInt(1));
            }
        }
        System.out.println("✓ Patient " + patientId + " inscrit en liste d'attente — ID: " + la.getId());
        return la;
    }

    /** Vérifie si le patient est déjà en attente active pour ce médecin. */
    public boolean estDejaInscrit(int patientId, int doctorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM liste_attente " +
                     "WHERE patient_id=? AND doctor_id=? AND statut='EN_ATTENTE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // =========================================================================
    // LECTURE
    // =========================================================================

    /** Tous les patients EN_ATTENTE pour un médecin, triés par date inscription. */
    public List<ListeAttente> getAttenteParDocteur(int doctorId) throws SQLException {
        String sql = """
            SELECT la.*, u.username AS patient_nom
            FROM liste_attente la
            LEFT JOIN users u ON la.patient_id = u.id
            WHERE la.doctor_id = ? AND la.statut = 'EN_ATTENTE'
            ORDER BY la.date_inscription ASC
            """;
        return executeQuery(sql, doctorId);
    }

    /** Toutes les inscriptions d'un patient (toutes statuts). */
    public List<ListeAttente> getAttenteParPatient(int patientId) throws SQLException {
        String sql = """
            SELECT la.*, u.username AS patient_nom, d.username AS doctor_nom
            FROM liste_attente la
            LEFT JOIN users u ON la.patient_id = u.id
            LEFT JOIN users d ON la.doctor_id  = d.id
            WHERE la.patient_id = ?
            ORDER BY la.date_inscription DESC
            """;
        return executeQueryAvecDoctor(sql, patientId);
    }

    /** Nombre de patients en attente pour un médecin. */
    public int getNombreEnAttente(int doctorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM liste_attente WHERE doctor_id=? AND statut='EN_ATTENTE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // =========================================================================
    // MISE À JOUR STATUT
    // =========================================================================

    public void changerStatut(int id, String nouveauStatut) throws SQLException {
        String sql = "UPDATE liste_attente SET statut=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Marque les inscriptions expirées (date_expiration dépassée). */
    public void marquerExpirees() throws SQLException {
        String sql = "UPDATE liste_attente SET statut='EXPIRE' " +
                     "WHERE statut='EN_ATTENTE' AND date_expiration < NOW()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int nb = ps.executeUpdate();
            if (nb > 0) System.out.println("✓ " + nb + " inscription(s) expirée(s) marquées.");
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM liste_attente WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private List<ListeAttente> executeQuery(String sql, int param) throws SQLException {
        List<ListeAttente> liste = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapRow(rs, false));
            }
        }
        return liste;
    }

    private List<ListeAttente> executeQueryAvecDoctor(String sql, int param) throws SQLException {
        List<ListeAttente> liste = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapRow(rs, true));
            }
        }
        return liste;
    }

    private ListeAttente mapRow(ResultSet rs, boolean avecDoctor) throws SQLException {
        ListeAttente la = new ListeAttente();
        la.setId(rs.getInt("id"));
        la.setPatientId(rs.getInt("patient_id"));
        la.setDoctorId(rs.getInt("doctor_id"));
        Date d = rs.getDate("date_souhaitee");
        if (d != null) la.setDateSouhaitee(d.toLocalDate());
        la.setPlageHoraire(rs.getString("plage_horaire"));
        Timestamp ti = rs.getTimestamp("date_inscription");
        if (ti != null) la.setDateInscription(ti.toLocalDateTime());
        la.setStatut(rs.getString("statut"));
        Timestamp te = rs.getTimestamp("date_expiration");
        if (te != null) la.setDateExpiration(te.toLocalDateTime());
        try { la.setPatientNom(rs.getString("patient_nom")); } catch (SQLException ignored) {}
        if (avecDoctor) {
            try { la.setDoctorNom(rs.getString("doctor_nom")); } catch (SQLException ignored) {}
        }
        return la;
    }
}
