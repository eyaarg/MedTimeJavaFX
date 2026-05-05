package esprit.fx.services;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.utils.MyDB;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  SmsSchedulerServiceArij — Envoi automatique de SMS de suivi
 * ============================================================
 *
 *  Rôle :
 *  ───────
 *  Vérifie toutes les heures les consultations terminées depuis
 *  24-25h dont le SMS de suivi n'a pas encore été envoyé, puis
 *  envoie un SMS personnalisé au patient via TwilioSmsServiceArij.
 *
 *  Architecture Thread :
 *  ──────────────────────
 *  La Timeline JavaFX s'exécute sur le JavaFX Application Thread.
 *  L'appel réseau Twilio (bloquant) est délégué à un Thread daemon
 *  background pour ne pas geler l'UI pendant l'envoi.
 *
 *  Requête SQL équivalente au HQL demandé :
 *  ─────────────────────────────────────────
 *  SELECT c.*, p.telephone, u.first_name, u.last_name,
 *         du.first_name AS doc_first, du.last_name AS doc_last
 *  FROM   consultations c
 *  JOIN   patients  p  ON p.id  = c.patient_id
 *  JOIN   users     u  ON u.id  = p.user_id
 *  JOIN   doctors   d  ON d.id  = c.doctor_id
 *  JOIN   users     du ON du.id = d.user_id
 *  WHERE  LOWER(c.status) = 'terminee'
 *    AND  c.sms_suivi_envoye = 0
 *    AND  c.consultation_date BETWEEN :hier AND :ilya25h
 *    AND  c.is_deleted = 0
 *
 *  Démarrage :
 *  ────────────
 *  Appeler SmsSchedulerServiceArij.getInstance().demarrer()
 *  dans Main.start() ou MainControllerArij.initialize().
 *
 *  Arrêt :
 *  ────────
 *  Appeler .arreter() au logout ou à la fermeture de l'app.
 */
public class SmsSchedulerServiceArij {

    // ------------------------------------------------------------------ //
    //  Singleton                                                          //
    // ------------------------------------------------------------------ //
    private static SmsSchedulerServiceArij instance;

    public static SmsSchedulerServiceArij getInstance() {
        if (instance == null) {
            instance = new SmsSchedulerServiceArij();
        }
        return instance;
    }

    // ------------------------------------------------------------------ //
    //  Dépendances                                                        //
    // ------------------------------------------------------------------ //
    private final TwilioSmsServiceArij smsService = new TwilioSmsServiceArij();

    // ------------------------------------------------------------------ //
    //  Timeline JavaFX                                                    //
    // ------------------------------------------------------------------ //
    private Timeline timeline;

    private SmsSchedulerServiceArij() {}

    // ================================================================== //
    //  Cycle de vie                                                       //
    // ================================================================== //

    /**
     * Démarre la Timeline qui vérifie les SMS de suivi toutes les heures.
     *
     * Deux KeyFrames :
     *  - Duration.ZERO      → vérification immédiate au démarrage de l'app
     *  - Duration.hours(1)  → puis toutes les heures
     *
     * setCycleCount(INDEFINITE) → boucle infinie jusqu'à arreter().
     *
     * La Timeline s'exécute sur le JavaFX Application Thread.
     * L'appel réseau Twilio est délégué à un thread daemon séparé
     * pour ne pas bloquer l'UI pendant l'envoi des SMS.
     */
    public void demarrer() {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            System.out.println("[SmsSchedulerServiceArij] Déjà en cours d'exécution.");
            return;
        }

        timeline = new Timeline(
            // Tick immédiat au démarrage
            new KeyFrame(Duration.ZERO,    e -> lancerVerificationAsync()),
            // Tick toutes les heures
            new KeyFrame(Duration.hours(1), e -> lancerVerificationAsync())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        System.out.println("[SmsSchedulerServiceArij] ✓ Scheduler démarré"
            + " — vérification toutes les heures.");
    }

    /**
     * Arrête la Timeline proprement.
     * À appeler au logout ou à la fermeture de l'application.
     */
    public void arreter() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
            System.out.println("[SmsSchedulerServiceArij] Scheduler arrêté.");
        }
    }

    // ================================================================== //
    //  Vérification asynchrone                                           //
    // ================================================================== //

    /**
     * Lance verifierSmsSuivi() dans un thread daemon background.
     *
     * POURQUOI un thread séparé ?
     * La Timeline s'exécute sur le JavaFX Application Thread.
     * Les appels JDBC + HTTP Twilio sont bloquants (peuvent durer
     * plusieurs secondes). Les exécuter directement sur le JavaFX
     * Thread gèlerait l'interface utilisateur.
     *
     * Thread daemon = s'arrête automatiquement quand la JVM se ferme,
     * sans bloquer la fermeture de l'application.
     */
    private void lancerVerificationAsync() {
        Thread worker = new Thread(this::verifierSmsSuivi, "sms-scheduler-arij");
        worker.setDaemon(true);   // ne bloque pas la fermeture de l'app
        worker.start();
    }

    // ================================================================== //
    //  Logique métier                                                     //
    // ================================================================== //

    /**
     * Requête les consultations terminées depuis 24-25h sans SMS envoyé,
     * envoie le SMS de suivi à chaque patient, puis marque la consultation.
     *
     * Équivalent HQL :
     *   SELECT c FROM Consultation c
     *   WHERE  c.statut = 'terminee'
     *     AND  c.smsSuiviEnvoye = false
     *     AND  c.dateConsultation BETWEEN :hier AND :ilya25h
     *
     * La fenêtre [hier, ilya25h] = [now-25h, now-24h] cible exactement
     * les consultations terminées "hier" (±30 min de tolérance).
     * Cela évite d'envoyer le SMS trop tôt ou trop tard.
     */
    private void verifierSmsSuivi() {
        System.out.println("[SmsSchedulerServiceArij] Vérification SMS suivi — "
            + LocalDateTime.now());

        // Fenêtre temporelle : consultations terminées il y a 24h à 25h
        LocalDateTime ilya24h  = LocalDateTime.now().minusHours(24);
        LocalDateTime ilya25h  = LocalDateTime.now().minusHours(25);

        List<ConsultationSuiviDto> consultations =
            findConsultationsARelancer(ilya25h, ilya24h);

        if (consultations.isEmpty()) {
            System.out.println("[SmsSchedulerServiceArij] Aucune consultation à relancer.");
            return;
        }

        System.out.println("[SmsSchedulerServiceArij] "
            + consultations.size() + " consultation(s) à relancer.");

        for (ConsultationSuiviDto dto : consultations) {
            traiterConsultation(dto);
        }
    }

    /**
     * Traite une consultation : envoie le SMS puis marque smsSuiviEnvoye=true.
     */
    private void traiterConsultation(ConsultationSuiviDto dto) {
        // Vérifier que le patient a un numéro de téléphone
        if (dto.telephone == null || dto.telephone.isBlank()) {
            System.err.println("[SmsSchedulerServiceArij] Patient #"
                + dto.patientId + " sans numéro de téléphone — SMS ignoré.");
            marquerSmsSuiviEnvoye(dto.consultationId); // éviter de retenter indéfiniment
            return;
        }

        // Construire le message personnalisé
        String message = buildMessage(dto.patientPrenom, dto.medecinNom);

        System.out.println("[SmsSchedulerServiceArij] Envoi SMS suivi"
            + " → consultation #" + dto.consultationId
            + " | patient: " + dto.patientPrenom
            + " | tél: " + dto.telephone);

        // Envoyer le SMS via Twilio
        boolean envoye = smsService.envoyer(dto.telephone, message);

        if (envoye) {
            // Marquer smsSuiviEnvoye = true en BDD
            marquerSmsSuiviEnvoye(dto.consultationId);
            System.out.println("[SmsSchedulerServiceArij] ✓ SMS suivi envoyé"
                + " — consultation #" + dto.consultationId + " marquée.");
        } else {
            // Ne pas marquer → sera retenté au prochain tick (dans 1h)
            System.err.println("[SmsSchedulerServiceArij] ✗ Échec SMS"
                + " — consultation #" + dto.consultationId
                + " sera retentée dans 1h.");
        }
    }

    /**
     * Construit le message SMS personnalisé.
     *
     * @param patientPrenom prénom du patient (ex: "Jean")
     * @param medecinNom    nom complet du médecin (ex: "Dr. Martin")
     */
    private String buildMessage(String patientPrenom, String medecinNom) {
        String prenom  = (patientPrenom != null && !patientPrenom.isBlank())
            ? patientPrenom : "cher(e) patient(e)";
        String medecin = (medecinNom != null && !medecinNom.isBlank())
            ? medecinNom : "votre médecin";

        return "Bonjour " + prenom + ", merci pour votre consultation avec "
            + medecin + ". Prenez soin de vous !";
    }

    // ================================================================== //
    //  Requêtes JDBC                                                      //
    // ================================================================== //

    /**
     * Requête SQL équivalente au HQL demandé.
     *
     * Jointures nécessaires (JDBC pur, pas Hibernate) :
     *   consultations → patients → users (pour prénom + téléphone)
     *   consultations → doctors  → users (pour nom du médecin)
     *
     * Fenêtre temporelle :
     *   consultation_date BETWEEN :ilya25h AND :ilya24h
     *   → cible les consultations terminées il y a exactement 24h (±1h)
     */
    private List<ConsultationSuiviDto> findConsultationsARelancer(
            LocalDateTime ilya25h, LocalDateTime ilya24h) {

        List<ConsultationSuiviDto> list = new ArrayList<>();

        String sql = """
            SELECT
                c.id                AS consultation_id,
                c.patient_id,
                COALESCE(u.first_name, u.username, 'Patient') AS patient_prenom,
                COALESCE(u.phone, u.telephone, '')             AS telephone,
                CONCAT('Dr. ',
                    COALESCE(du.last_name, du.username, 'Médecin'))  AS medecin_nom
            FROM consultations c
            JOIN patients  p  ON p.id  = c.patient_id
            JOIN users     u  ON u.id  = p.user_id
            JOIN doctors   d  ON d.id  = c.doctor_id
            JOIN users     du ON du.id = d.user_id
            WHERE LOWER(c.status) = 'terminee'
              AND c.sms_suivi_envoye = 0
              AND c.consultation_date BETWEEN ? AND ?
              AND c.is_deleted = 0
            """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(ilya25h));
            ps.setTimestamp(2, Timestamp.valueOf(ilya24h));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ConsultationSuiviDto dto = new ConsultationSuiviDto();
                dto.consultationId  = rs.getInt("consultation_id");
                dto.patientId       = rs.getInt("patient_id");
                dto.patientPrenom   = rs.getString("patient_prenom");
                dto.telephone       = rs.getString("telephone");
                dto.medecinNom      = rs.getString("medecin_nom");
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("[SmsSchedulerServiceArij] findConsultationsARelancer: "
                + e.getMessage());
        }
        return list;
    }

    /**
     * Marque une consultation comme "SMS suivi envoyé".
     * UPDATE consultations SET sms_suivi_envoye = 1 WHERE id = ?
     */
    private void marquerSmsSuiviEnvoye(int consultationId) {
        String sql = "UPDATE consultations SET sms_suivi_envoye = 1 WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, consultationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SmsSchedulerServiceArij] marquerSmsSuiviEnvoye: "
                + e.getMessage());
        }
    }

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ================================================================== //
    //  DTO interne — données nécessaires pour l'envoi du SMS             //
    // ================================================================== //

    /**
     * Objet de transfert léger contenant uniquement les données
     * nécessaires pour construire et envoyer le SMS de suivi.
     * Évite de charger l'entité ConsultationsArij complète.
     */
    private static class ConsultationSuiviDto {
        int    consultationId;
        int    patientId;
        String patientPrenom;
        String telephone;
        String medecinNom;
    }
}
