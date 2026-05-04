package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================
 *  StatistiqueDAOArij — Requêtes statistiques JDBC
 * ============================================================
 *
 *  Le projet utilise JDBC pur (pas Hibernate).
 *  Les requêtes SQL natives ci-dessous sont équivalentes
 *  aux HQL demandés :
 *
 *  HQL → SQL natif :
 *  ─────────────────
 *  MONTH(c.dateConsultation)  → MONTH(c.consultation_date)
 *  YEAR(c.dateConsultation)   → YEAR(c.consultation_date)
 *  c.medecin.specialite       → JOIN doctors d → d.specialite
 *  c.prixConsultation         → c.consultation_fee
 *  c.statut = 'payee'         → LOWER(c.status) = 'payee'
 *  setMaxResults(5)           → LIMIT 5
 *
 *  Tables utilisées :
 *    consultations, doctors, users, paiement
 */
public class StatistiqueDAOArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ================================================================== //
    //  1. consultationsParMois                                           //
    // ================================================================== //

    /**
     * Nombre de consultations par mois pour une année donnée.
     *
     * HQL équivalent :
     *   SELECT MONTH(c.dateConsultation), COUNT(c)
     *   FROM Consultation c
     *   WHERE YEAR(c.dateConsultation) = :annee
     *   GROUP BY MONTH(c.dateConsultation)
     *
     * @param annee année cible (ex: 2026)
     * @return Map<mois (1-12), nombre de consultations>
     *         Les mois sans consultation ne sont pas inclus.
     */
    public Map<Integer, Long> consultationsParMois(int annee) {
        // LinkedHashMap pour conserver l'ordre des mois
        Map<Integer, Long> result = new LinkedHashMap<>();

        String sql = """
            SELECT MONTH(consultation_date) AS mois,
                   COUNT(*)                 AS total
            FROM   consultations
            WHERE  YEAR(consultation_date) = ?
              AND  is_deleted = 0
            GROUP  BY MONTH(consultation_date)
            ORDER  BY mois
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, annee);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("mois"), rs.getLong("total"));
            }
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] consultationsParMois: " + e.getMessage());
        }
        return result;
    }

    // ================================================================== //
    //  2. tauxAcceptation                                                //
    // ================================================================== //

    /**
     * Taux d'acceptation des consultations en pourcentage.
     *
     * HQL équivalent :
     *   (COUNT(c WHERE c.statut='confirmee') / COUNT(c)) * 100
     *
     * Formule :
     *   taux = (nb_confirmees / nb_total) * 100
     *   Retourne 0.0 si aucune consultation.
     *
     * @return pourcentage entre 0.0 et 100.0
     */
    public double tauxAcceptation() {
        String sql = """
            SELECT
                COUNT(*)                                              AS total,
                SUM(CASE WHEN LOWER(status) = 'confirmee' THEN 1
                         ELSE 0 END)                                  AS acceptees
            FROM consultations
            WHERE is_deleted = 0
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long total     = rs.getLong("total");
                long acceptees = rs.getLong("acceptees");
                if (total == 0) return 0.0;
                return (acceptees * 100.0) / total;
            }
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] tauxAcceptation: " + e.getMessage());
        }
        return 0.0;
    }

    // ================================================================== //
    //  3. specialitesLesPlusDemandees                                   //
    // ================================================================== //

    /**
     * Top 5 des spécialités médicales les plus demandées.
     *
     * HQL équivalent :
     *   SELECT m.specialite, COUNT(c)
     *   FROM Consultation c JOIN c.medecin m
     *   GROUP BY m.specialite
     *   ORDER BY COUNT(c) DESC
     *   (setMaxResults(5))
     *
     * Note : si la colonne "specialite" n'existe pas dans doctors,
     * on utilise le nom du médecin comme fallback.
     *
     * @return Map<spécialité, nombre de consultations> — 5 entrées max,
     *         triées par nombre décroissant (LinkedHashMap = ordre garanti)
     */
    public Map<String, Long> specialitesLesPlusDemandees() {
        Map<String, Long> result = new LinkedHashMap<>();

        // Tentative avec colonne specialite dans doctors
        String sql = """
            SELECT COALESCE(d.specialite, u.username, CONCAT('Médecin #', d.id))
                       AS specialite,
                   COUNT(c.id) AS nb
            FROM   consultations c
            JOIN   doctors d ON d.id = c.doctor_id
            LEFT   JOIN users u ON u.id = d.user_id
            WHERE  c.is_deleted = 0
            GROUP  BY COALESCE(d.specialite, u.username, CONCAT('Médecin #', d.id))
            ORDER  BY nb DESC
            LIMIT  5
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String specialite = rs.getString("specialite");
                if (specialite == null || specialite.isBlank()) specialite = "Non renseignée";
                result.put(specialite, rs.getLong("nb"));
            }
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] specialitesLesPlusDemandees: "
                + e.getMessage());
            // Fallback sans colonne specialite
            result.putAll(specialitesFallback());
        }
        return result;
    }

    /** Fallback si la colonne specialite n'existe pas dans doctors. */
    private Map<String, Long> specialitesFallback() {
        Map<String, Long> result = new LinkedHashMap<>();
        String sql = """
            SELECT COALESCE(u.username, CONCAT('Médecin #', d.id)) AS nom,
                   COUNT(c.id) AS nb
            FROM   consultations c
            JOIN   doctors d ON d.id = c.doctor_id
            LEFT   JOIN users u ON u.id = d.user_id
            WHERE  c.is_deleted = 0
            GROUP  BY COALESCE(u.username, CONCAT('Médecin #', d.id))
            ORDER  BY nb DESC
            LIMIT  5
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("nom"), rs.getLong("nb"));
            }
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] specialitesFallback: " + e.getMessage());
        }
        return result;
    }

    // ================================================================== //
    //  4. revenuMensuel                                                  //
    // ================================================================== //

    /**
     * Revenu mensuel (somme des consultation_fee) pour les consultations payées.
     *
     * HQL équivalent :
     *   SELECT MONTH(c.dateConsultation), SUM(c.prixConsultation)
     *   FROM Consultation c
     *   WHERE c.statut = 'payee'
     *     AND YEAR(c.dateConsultation) = :annee
     *   GROUP BY MONTH(c.dateConsultation)
     *
     * Statuts considérés comme "payés" : 'payee', 'terminee'
     * (la consultation est terminée = prestation effectuée).
     *
     * @param annee année cible
     * @return Map<mois (1-12), revenu en TND>
     */
    public Map<Integer, Double> revenuMensuel(int annee) {
        Map<Integer, Double> result = new LinkedHashMap<>();

        String sql = """
            SELECT MONTH(consultation_date) AS mois,
                   SUM(consultation_fee)    AS revenu
            FROM   consultations
            WHERE  YEAR(consultation_date) = ?
              AND  LOWER(status) IN ('payee', 'terminee')
              AND  is_deleted = 0
              AND  consultation_fee > 0
            GROUP  BY MONTH(consultation_date)
            ORDER  BY mois
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, annee);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("mois"), rs.getDouble("revenu"));
            }
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] revenuMensuel: " + e.getMessage());
        }
        return result;
    }

    // ================================================================== //
    //  5. patientsActifs                                                 //
    // ================================================================== //

    /**
     * Nombre de patients distincts ayant eu une consultation
     * dans les 30 derniers jours.
     *
     * HQL équivalent :
     *   SELECT COUNT(DISTINCT c.patient)
     *   FROM Consultation c
     *   WHERE c.dateConsultation >= :ilya30jours
     *
     * @return nombre de patients actifs (>= 0)
     */
    public long patientsActifs() {
        String sql = """
            SELECT COUNT(DISTINCT patient_id) AS nb_patients
            FROM   consultations
            WHERE  consultation_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
              AND  is_deleted = 0
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong("nb_patients");
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] patientsActifs: " + e.getMessage());
        }
        return 0L;
    }

    // ================================================================== //
    //  Méthodes bonus (utiles pour le Dashboard)                        //
    // ================================================================== //

    /**
     * Nombre total de consultations pour une année.
     * Complément de consultationsParMois() pour le titre du graphique.
     */
    public long totalConsultations(int annee) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM   consultations
            WHERE  YEAR(consultation_date) = ?
              AND  is_deleted = 0
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, annee);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("total");
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] totalConsultations: " + e.getMessage());
        }
        return 0L;
    }

    /**
     * Revenu total annuel (toutes consultations payées/terminées).
     * Complément de revenuMensuel() pour l'affichage KPI.
     */
    public double revenuTotal(int annee) {
        String sql = """
            SELECT COALESCE(SUM(consultation_fee), 0) AS total
            FROM   consultations
            WHERE  YEAR(consultation_date) = ?
              AND  LOWER(status) IN ('payee', 'terminee')
              AND  is_deleted = 0
              AND  consultation_fee > 0
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, annee);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] revenuTotal: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Répartition des consultations par statut.
     * Utile pour un graphique camembert.
     *
     * @return Map<statut, count> — ex: {"EN_ATTENTE": 12, "CONFIRMEE": 8, ...}
     */
    public Map<String, Long> repartitionParStatut() {
        Map<String, Long> result = new LinkedHashMap<>();
        String sql = """
            SELECT UPPER(status) AS statut, COUNT(*) AS nb
            FROM   consultations
            WHERE  is_deleted = 0
            GROUP  BY UPPER(status)
            ORDER  BY nb DESC
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("statut"), rs.getLong("nb"));
            }
        } catch (SQLException e) {
            System.err.println("[StatistiqueDAOArij] repartitionParStatut: " + e.getMessage());
        }
        return result;
    }
}
