package esprit.fx.services;

import esprit.fx.entities.ListeAttente;
import esprit.fx.entities.RendezVous;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gère la notification automatique des patients en liste d'attente
 * lorsqu'un créneau se libère (annulation d'un RDV).
 */
public class ListeAttenteNotificationService {

    private final ListeAttenteService listeAttenteService;
    private final SuggestionService   suggestionService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    public ListeAttenteNotificationService() {
        listeAttenteService = new ListeAttenteService();
        suggestionService   = new SuggestionService();
    }

    /**
     * Appelé automatiquement quand un RDV est annulé.
     * Vérifie la liste d'attente et notifie le premier patient éligible.
     *
     * @param rdvAnnule le RDV qui vient d'être annulé
     * @return le message de notification envoyé, ou null si personne en attente
     */
    public NotificationResult verifierEtNotifier(RendezVous rdvAnnule) {
        try {
            // Marquer d'abord les inscriptions expirées
            listeAttenteService.marquerExpirees();

            // Récupérer la liste d'attente pour ce médecin
            List<ListeAttente> enAttente =
                    listeAttenteService.getAttenteParDocteur(rdvAnnule.getDoctorId());

            if (enAttente.isEmpty()) return null;

            // Trouver le créneau libéré
            String creneauLibere = rdvAnnule.getDateHeure() != null
                    ? rdvAnnule.getDateHeure().format(FMT) : "prochainement";

            // Notifier le premier patient en attente
            ListeAttente premier = enAttente.get(0);

            // Vérifier si le créneau correspond à la plage souhaitée
            boolean correspond = correspondALaPlage(rdvAnnule, premier);

            if (correspond || premier.getPlageHoraire() == null
                           || "Tous".equals(premier.getPlageHoraire())) {

                // Marquer comme NOTIFIE
                listeAttenteService.changerStatut(premier.getId(), "NOTIFIE");

                String message = buildMessageNotification(premier, rdvAnnule, creneauLibere);
                System.out.println("✓ Notification envoyée au patient " + premier.getPatientId()
                        + " : " + message);

                return new NotificationResult(premier, message, enAttente.size());
            }

            // Si le créneau ne correspond pas, chercher le prochain patient compatible
            for (int i = 1; i < enAttente.size(); i++) {
                ListeAttente candidat = enAttente.get(i);
                if (correspondALaPlage(rdvAnnule, candidat)
                        || "Tous".equals(candidat.getPlageHoraire())) {
                    listeAttenteService.changerStatut(candidat.getId(), "NOTIFIE");
                    String message = buildMessageNotification(candidat, rdvAnnule, creneauLibere);
                    return new NotificationResult(candidat, message, enAttente.size());
                }
            }

        } catch (SQLException e) {
            System.err.println("ListeAttenteNotificationService erreur: " + e.getMessage());
        }
        return null;
    }

    /** Vérifie si le créneau libéré correspond à la plage horaire souhaitée. */
    private boolean correspondALaPlage(RendezVous rdv, ListeAttente attente) {
        if (rdv.getDateHeure() == null) return true;
        if (attente.getPlageHoraire() == null || "Tous".equals(attente.getPlageHoraire()))
            return true;

        int heure = rdv.getDateHeure().getHour();
        return switch (attente.getPlageHoraire()) {
            case "Matin"      -> heure >= 8  && heure < 12;
            case "Après-midi" -> heure >= 12 && heure < 17;
            case "Soir"       -> heure >= 17 && heure < 20;
            default           -> true;
        };
    }

    private String buildMessageNotification(ListeAttente attente, RendezVous rdv,
                                            String creneauLibere) {
        String doctorNom = rdv.getDoctorNom() != null ? rdv.getDoctorNom() : "votre médecin";
        return "✅ Un créneau est disponible avec Dr. " + doctorNom
             + " le " + creneauLibere
             + " — Confirmez votre RDV avant expiration !";
    }

    // -------------------------------------------------------------------------

    /** Résultat d'une notification. */
    public static class NotificationResult {
        public final ListeAttente patientNotifie;
        public final String       message;
        public final int          totalEnAttente;

        public NotificationResult(ListeAttente patientNotifie,
                                  String message, int totalEnAttente) {
            this.patientNotifie = patientNotifie;
            this.message        = message;
            this.totalEnAttente = totalEnAttente;
        }
    }
}
