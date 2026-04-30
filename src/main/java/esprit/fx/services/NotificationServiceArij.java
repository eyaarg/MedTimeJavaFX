package esprit.fx.services;

import esprit.fx.entities.NotificationArij;

import java.time.LocalDateTime;

/**
 * ============================================================
 *  NotificationServiceArij — Service centralisé de notification
 * ============================================================
 *
 *  Rôle : point d'entrée unique pour créer et persister une
 *  notification dans la table "notifications" partagée avec Symfony.
 *
 *  Compatibilité Symfony :
 *  ─────────────────────────
 *  La table "notifications" est lue par Symfony (Doctrine) et par
 *  JavaFX (JDBC) via la même base MySQL. Une notification créée ici
 *  est immédiatement visible côté web sans aucune synchronisation
 *  supplémentaire — les deux applications lisent la même ligne.
 *
 *  Colonnes écrites :
 *    user_id    ← destinataireId
 *    title      ← dérivé du type (ex: "Nouvelle consultation")
 *    message    ← message
 *    type       ← "info" | "success" | "warning"
 *    is_read    ← 0 (non lu par défaut)
 *    created_at ← maintenant
 *    link       ← lien (nullable)
 *
 *  Usage :
 *  ────────
 *    NotificationServiceArij.getInstance()
 *        .notifier(doctorUserId, "Nouvelle consultation de Jean", "info", null);
 */
public class NotificationServiceArij {

    // ------------------------------------------------------------------ //
    //  Singleton — une seule instance partagée dans toute l'application  //
    // ------------------------------------------------------------------ //
    private static NotificationServiceArij instance;

    private final NotificationDAOArij dao = new NotificationDAOArij();

    private NotificationServiceArij() {}

    public static NotificationServiceArij getInstance() {
        if (instance == null) {
            instance = new NotificationServiceArij();
        }
        return instance;
    }

    // ================================================================== //
    //  Méthode principale                                                 //
    // ================================================================== //

    /**
     * Crée une notification et la persiste immédiatement en BDD.
     * Visible côté Symfony dès l'insertion (table partagée).
     *
     * @param destinataireId  id de l'utilisateur destinataire (users.id)
     * @param message         texte du message affiché à l'utilisateur
     * @param type            "info" | "success" | "warning"
     *                        → utiliser les constantes NotificationArij.TYPE_*
     * @param lien            URL ou lien Meet (nullable)
     *
     * @return la notification persistée avec son id généré,
     *         ou null si destinataireId est invalide
     */
    public NotificationArij notifier(Long destinataireId,
                                     String message,
                                     String type,
                                     String lien) {
        // Validation : on ne notifie pas un utilisateur inconnu
        if (destinataireId == null || destinataireId <= 0) {
            System.err.println("[NotificationServiceArij] destinataireId invalide : " + destinataireId);
            return null;
        }
        if (message == null || message.isBlank()) {
            System.err.println("[NotificationServiceArij] message vide, notification ignorée.");
            return null;
        }

        // Normaliser le type (défaut : info)
        String normalizedType = normalizeType(type);

        // Dériver un titre lisible depuis le type
        String title = titleFromType(normalizedType);

        // Construire l'entité
        NotificationArij notif = new NotificationArij();
        notif.setDestinataireId(destinataireId);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(normalizedType);
        notif.setLu(false);
        notif.setCreatedAt(LocalDateTime.now());
        notif.setLien(lien);

        // Persister → visible immédiatement côté Symfony
        dao.save(notif);

        System.out.println("[NotificationServiceArij] ✓ Notification envoyée"
            + " → userId=" + destinataireId
            + " | type=" + normalizedType
            + " | msg=" + message.substring(0, Math.min(60, message.length())));

        return notif;
    }

    /**
     * Surcharge pratique avec int (évite les casts dans les services).
     */
    public NotificationArij notifier(int destinataireId,
                                     String message,
                                     String type,
                                     String lien) {
        return notifier((long) destinataireId, message, type, lien);
    }

    // ================================================================== //
    //  Helpers                                                            //
    // ================================================================== //

    /**
     * Normalise le type vers les valeurs acceptées par Symfony.
     * Tout type inconnu est ramené à "info".
     */
    private String normalizeType(String type) {
        if (type == null) return NotificationArij.TYPE_INFO;
        return switch (type.trim().toLowerCase()) {
            case "success", "succès", "succes" -> NotificationArij.TYPE_SUCCESS;
            case "warning", "warn", "attention" -> NotificationArij.TYPE_WARNING;
            default                              -> NotificationArij.TYPE_INFO;
        };
    }

    /**
     * Génère un titre court et lisible selon le type,
     * affiché en en-tête de la notification côté Symfony et JavaFX.
     */
    private String titleFromType(String type) {
        return switch (type) {
            case NotificationArij.TYPE_SUCCESS -> "✅ Confirmation";
            case NotificationArij.TYPE_WARNING -> "⚠️ Attention";
            default                             -> "ℹ️ Information";
        };
    }
}
