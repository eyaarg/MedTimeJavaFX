package esprit.fx.services;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  ConsultationFiltreDAOArij — Requête SQL dynamique
 * ============================================================
 *
 *  Équivalent du HQL demandé :
 *  ────────────────────────────
 *  StringBuilder sql = new StringBuilder(
 *      "SELECT c FROM Consultation c WHERE 1=1");
 *  if (debut != null)    sql.append(" AND c.dateConsultation >= :debut");
 *  if (fin != null)      sql.append(" AND c.dateConsultation <= :fin");
 *  if (statut != null)   sql.append(" AND c.statut = :statut");
 *  if (medecinId != null) sql.append(" AND c.medecin.id = :medecinId");
 *  if (motCle != null)   sql.append(" AND c.patient.nom LIKE :motCle");
 *
 *  Traduit en SQL natif JDBC (pas Hibernate dans ce projet).
 *
 *  Méthode principale :
 *    filtrer(debut, fin, statut, medecinId, motCle) → List<ConsultationsArij>
 */
public class ConsultationFiltreDAOArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ================================================================== //
    //  Méthode principale : filtrage dynamique                           //
    // ================================================================== //

    /**
     * Construit et exécute une requête SQL dynamique selon les critères fournis.
     * Chaque condition n'est ajoutée que si la valeur est non-null/non-vide.
     *
     * @param debut     date de début (inclusive) — null = pas de filtre
     * @param fin       date de fin (inclusive)   — null = pas de filtre
     * @param statut    statut exact ("EN_ATTENTE", "CONFIRMEE"…) — null/"Tous" = tous
     * @param medecinId id du médecin (doctors.id) — null/0 = tous les médecins
     * @param motCle    mot-clé recherché dans le nom du patient — null/"" = pas de filtre
     * @return liste des consultations correspondant aux critères, triée par date DESC
     */
    public List<ConsultationsArij> filtrer(LocalDate debut,
                                            LocalDate fin,
                                            String statut,
                                            Long medecinId,
                                            String motCle) {

        // ── Construction dynamique de la requête ──────────────────────
        // Même principe que le StringBuilder HQL demandé,
        // adapté en SQL natif avec paramètres positionnels (?)
        StringBuilder sql = new StringBuilder("""
            SELECT c.*
            FROM   consultations c
            LEFT   JOIN patients  p  ON p.id  = c.patient_id
            LEFT   JOIN users     u  ON u.id  = p.user_id
            WHERE  c.is_deleted = 0
            """);

        List<Object> params = new ArrayList<>();

        // ── Condition 1 : date de début ───────────────────────────────
        if (debut != null) {
            sql.append("  AND c.consultation_date >= ?\n");
            params.add(Timestamp.valueOf(debut.atStartOfDay()));
        }

        // ── Condition 2 : date de fin ─────────────────────────────────
        if (fin != null) {
            sql.append("  AND c.consultation_date <= ?\n");
            params.add(Timestamp.valueOf(fin.atTime(23, 59, 59)));
        }

        // ── Condition 3 : statut ──────────────────────────────────────
        if (statut != null && !statut.isBlank() && !"Tous".equalsIgnoreCase(statut)) {
            sql.append("  AND LOWER(c.status) = LOWER(?)\n");
            params.add(statut.trim());
        }

        // ── Condition 4 : médecin ─────────────────────────────────────
        if (medecinId != null && medecinId > 0) {
            sql.append("  AND c.doctor_id = ?\n");
            params.add(medecinId.intValue());
        }

        // ── Condition 5 : mot-clé (nom patient) ──────────────────────
        // Recherche dans username du patient (LIKE %motCle%)
        if (motCle != null && !motCle.isBlank()) {
            sql.append("  AND LOWER(u.username) LIKE LOWER(?)\n");
            params.add("%" + motCle.trim() + "%");
        }

        sql.append("ORDER BY c.consultation_date DESC");

        // ── Exécution ─────────────────────────────────────────────────
        List<ConsultationsArij> result = new ArrayList<>();

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            // Injecter les paramètres dans l'ordre
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp t) ps.setTimestamp(i + 1, t);
                else if (p instanceof Integer v) ps.setInt(i + 1, v);
                else if (p instanceof String s)  ps.setString(i + 1, s);
                else ps.setObject(i + 1, p);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[ConsultationFiltreDAOArij] filtrer: " + e.getMessage());
        }

        return result;
    }

    // ================================================================== //
    //  Mapping ResultSet → entité                                        //
    // ================================================================== //

    private ConsultationsArij mapRow(ResultSet rs) throws SQLException {
        ConsultationsArij c = new ConsultationsArij();
        c.setId(rs.getInt("id"));
        c.setPatientId(rs.getInt("patient_id"));

        Object doctorObj = rs.getObject("doctor_id");
        c.setDoctorId(doctorObj == null ? 0 : ((Number) doctorObj).intValue());

        Timestamp d = rs.getTimestamp("consultation_date");
        c.setConsultationDate(d != null ? d.toLocalDateTime() : null);

        c.setStatus(rs.getString("status"));
        c.setDeleted(rs.getBoolean("is_deleted"));

        Timestamp ca = rs.getTimestamp("created_at");
        c.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);

        Timestamp ua = rs.getTimestamp("updated_at");
        c.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);

        c.setRejectionReason(rs.getString("rejection_reason"));

        BigDecimal fee = rs.getBigDecimal("consultation_fee");
        c.setConsultationFee(fee != null ? fee.doubleValue() : 0.0);

        c.setLienMeet(rs.getString("lien_meet"));

        try { c.setSmsSuiviEnvoye(rs.getBoolean("sms_suivi_envoye")); }
        catch (SQLException ignored) {}

        return c;
    }
}
