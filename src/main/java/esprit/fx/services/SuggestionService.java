package esprit.fx.services;

import esprit.fx.entities.Disponibilite;
import esprit.fx.entities.RendezVous;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Algorithme de suggestion de créneaux libres pour un médecin donné.
 * Durée fixe d'un créneau : 30 minutes.
 */
public class SuggestionService {

    private static final int DUREE_CRENEAU_MIN = 30;

    // Plages horaires
    public static final LocalTime MATIN_DEBUT      = LocalTime.of(8,  0);
    public static final LocalTime MATIN_FIN        = LocalTime.of(12, 0);
    public static final LocalTime APREM_DEBUT      = LocalTime.of(12, 0);
    public static final LocalTime APREM_FIN        = LocalTime.of(17, 0);
    public static final LocalTime SOIR_DEBUT       = LocalTime.of(17, 0);
    public static final LocalTime SOIR_FIN         = LocalTime.of(20, 0);

    /** Représente un créneau libre suggéré. */
    public static class Creneau {
        public final LocalDateTime debut;
        public final LocalDateTime fin;
        public final String        label;

        public Creneau(LocalDateTime debut, LocalDateTime fin) {
            this.debut = debut;
            this.fin   = fin;
            this.label = buildLabel(debut, fin);
        }

        private String buildLabel(LocalDateTime d, LocalDateTime f) {
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("EEEE dd/MM à HH:mm",
                            java.util.Locale.FRENCH);
            return d.format(fmt) + " — " + f.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }

        @Override
        public String toString() { return label; }
    }

    private final ServiceDisponibilite serviceDisponibilite;
    private final ServiceRendezVous    serviceRendezVous;

    public SuggestionService() {
        serviceDisponibilite = new ServiceDisponibilite();
        serviceRendezVous    = new ServiceRendezVous();
    }

    // -------------------------------------------------------------------------
    // Méthode principale
    // -------------------------------------------------------------------------

    /**
     * Retourne les TOP {@code limit} créneaux libres pour un médecin.
     *
     * @param doctorId  ID du médecin
     * @param plage     "Matin" | "Après-midi" | "Soir" | "Tous"
     * @param semaine   lundi de la semaine souhaitée (ou null = semaine courante)
     * @param limit     nombre max de suggestions (généralement 3)
     */
    public List<Creneau> suggerer(int doctorId, String plage,
                                  LocalDate semaine, int limit) throws SQLException {

        // Étape 1 : disponibilités du médecin
        List<Disponibilite> dispos =
                serviceDisponibilite.getDisponibilitesParDocteur(doctorId);

        // Étape 2 : RDV déjà pris
        List<RendezVous> rdvExistants =
                serviceRendezVous.getRendezVousParDocteur(doctorId);

        // Étape 3 : générer tous les créneaux de 30 min
        List<Creneau> tousLesCreneaux = genererCreneaux(dispos);

        // Étape 4 : éliminer créneaux occupés
        List<Creneau> libres = tousLesCreneaux.stream()
                .filter(c -> !estOccupe(c, rdvExistants))
                .filter(c -> c.debut.isAfter(LocalDateTime.now())) // pas dans le passé
                .toList();

        // Étape 5 : filtrer par plage horaire
        libres = filtrerParPlage(libres, plage);

        // Étape 6 : filtrer par semaine
        if (semaine != null) {
            libres = filtrerParSemaine(libres, semaine);
        }

        // Étape 7 : trier par date la plus proche
        libres = libres.stream()
                .sorted(Comparator.comparing(c -> c.debut))
                .toList();

        // Étape 8 : top N
        return libres.stream().limit(limit).toList();
    }

    // -------------------------------------------------------------------------
    // Étape 3 : générer créneaux de 30 min dans chaque disponibilité
    // -------------------------------------------------------------------------

    private List<Creneau> genererCreneaux(List<Disponibilite> dispos) {
        List<Creneau> creneaux = new ArrayList<>();
        for (Disponibilite d : dispos) {
            if (!d.isEstDisponible()) continue;
            if (d.getDateDebut() == null || d.getDateFin() == null) continue;

            LocalDateTime cursor = d.getDateDebut();
            while (!cursor.plusMinutes(DUREE_CRENEAU_MIN).isAfter(d.getDateFin())) {
                creneaux.add(new Creneau(cursor, cursor.plusMinutes(DUREE_CRENEAU_MIN)));
                cursor = cursor.plusMinutes(DUREE_CRENEAU_MIN);
            }
        }
        return creneaux;
    }

    // -------------------------------------------------------------------------
    // Étape 4 : vérifier si un créneau est occupé par un RDV existant
    // -------------------------------------------------------------------------

    private boolean estOccupe(Creneau creneau, List<RendezVous> rdvs) {
        for (RendezVous rdv : rdvs) {
            if (rdv.getDateHeure() == null) continue;
            // Ignorer les RDV annulés
            if ("ANNULE".equals(rdv.getStatut())) continue;

            LocalDateTime rdvDebut = rdv.getDateHeure();
            LocalDateTime rdvFin   = rdvDebut.plusMinutes(DUREE_CRENEAU_MIN);

            // Chevauchement : creneau.debut < rdvFin && creneau.fin > rdvDebut
            boolean chevauche = creneau.debut.isBefore(rdvFin)
                             && creneau.fin.isAfter(rdvDebut);
            if (chevauche) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Étape 5 : filtrer par plage horaire
    // -------------------------------------------------------------------------

    private List<Creneau> filtrerParPlage(List<Creneau> creneaux, String plage) {
        if (plage == null || "Tous".equals(plage)) return creneaux;

        LocalTime debut, fin;
        switch (plage) {
            case "Matin"       -> { debut = MATIN_DEBUT; fin = MATIN_FIN; }
            case "Après-midi"  -> { debut = APREM_DEBUT; fin = APREM_FIN; }
            case "Soir"        -> { debut = SOIR_DEBUT;  fin = SOIR_FIN;  }
            default            -> { return creneaux; }
        }

        final LocalTime d = debut, f = fin;
        return creneaux.stream()
                .filter(c -> {
                    LocalTime h = c.debut.toLocalTime();
                    return !h.isBefore(d) && h.isBefore(f);
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // Étape 6 : filtrer par semaine
    // -------------------------------------------------------------------------

    private List<Creneau> filtrerParSemaine(List<Creneau> creneaux, LocalDate lundi) {
        LocalDate dimanche = lundi.plusDays(6);
        return creneaux.stream()
                .filter(c -> {
                    LocalDate jour = c.debut.toLocalDate();
                    return !jour.isBefore(lundi) && !jour.isAfter(dimanche);
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // Utilitaire : lundi de la semaine d'une date donnée
    // -------------------------------------------------------------------------

    public static LocalDate getLundiDeSemaine(LocalDate date) {
        return date.with(java.time.DayOfWeek.MONDAY);
    }
}
