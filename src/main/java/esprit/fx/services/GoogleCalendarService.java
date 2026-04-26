package esprit.fx.services;

import esprit.fx.entities.RendezVous;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service Google Calendar — Option A : lien pré-rempli.
 * Aucune clé API, aucune dépendance supplémentaire.
 * Ouvre le navigateur avec le formulaire Google Calendar pré-rempli.
 */
public class GoogleCalendarService {

    // Format attendu par Google Calendar : YYYYMMDDTHHmmss
    private static final DateTimeFormatter GC_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    /**
     * Génère le lien Google Calendar et l'ouvre dans le navigateur.
     *
     * @param rdv le rendez-vous confirmé
     * @return true si le navigateur a été ouvert avec succès
     */
    public boolean ouvrirDansGoogleCalendar(RendezVous rdv) {
        try {
            String lien = genererLien(rdv);
            Desktop desktop = Desktop.getDesktop();
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(lien));
                return true;
            } else {
                System.err.println("Desktop.browse() non supporté sur ce système.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("GoogleCalendarService erreur: " + e.getMessage());
            return false;
        }
    }

    /**
     * Construit l'URL Google Calendar avec tous les paramètres pré-remplis.
     */
    public String genererLien(RendezVous rdv) {
        LocalDateTime debut = rdv.getDateHeure();
        // Durée par défaut : 1 heure
        LocalDateTime fin   = debut != null ? debut.plusHours(1) : null;

        String titre       = buildTitre(rdv);
        String description = buildDescription(rdv);
        String lieu        = buildLieu(rdv);
        String dates       = buildDates(debut, fin);

        return "https://calendar.google.com/calendar/render"
             + "?action=TEMPLATE"
             + "&text="    + encode(titre)
             + "&dates="   + dates
             + "&details=" + encode(description)
             + "&location=" + encode(lieu);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildTitre(RendezVous rdv) {
        String doctorNom = rdv.getDoctorNom() != null ? rdv.getDoctorNom() : "Médecin";
        return "RDV avec Dr. " + doctorNom;
    }

    private String buildDescription(RendezVous rdv) {
        StringBuilder sb = new StringBuilder();
        if (rdv.getMotif() != null && !rdv.getMotif().isEmpty()) {
            sb.append("Motif : ").append(rdv.getMotif());
        }
        if (rdv.getNotes() != null && !rdv.getNotes().isEmpty()) {
            sb.append("\nNotes : ").append(rdv.getNotes());
        }
        if (rdv.getPatientNom() != null) {
            sb.append("\nPatient : ").append(rdv.getPatientNom());
        }
        sb.append("\nStatut : ").append(rdv.getStatut());
        return sb.toString();
    }

    private String buildLieu(RendezVous rdv) {
        String doctorNom = rdv.getDoctorNom() != null ? rdv.getDoctorNom() : "Médecin";
        return "Cabinet Dr. " + doctorNom + ", Tunis, Tunisie";
    }

    private String buildDates(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null) {
            // Fallback : maintenant + 1h
            debut = LocalDateTime.now();
            fin   = debut.plusHours(1);
        }
        return debut.format(GC_FORMAT) + "/" + fin.format(GC_FORMAT);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
