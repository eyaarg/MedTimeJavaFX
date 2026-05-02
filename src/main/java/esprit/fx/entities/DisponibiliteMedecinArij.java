package esprit.fx.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entité DisponibiliteMedecin — table "disponibilite_medecin" partagée avec Symfony.
 *
 * Note : le projet utilise JDBC pur (pas Hibernate).
 * Les annotations @Entity/@ManyToOne sont documentées en commentaires
 * pour référence Hibernate, mais non actives.
 *
 * Mapping colonnes MySQL :
 *   id           → id          (Long)
 *   medecin_id   → medecinId   (Long)  — FK vers users.id
 *   date_debut   → dateDebut   (LocalDateTime)
 *   date_fin     → dateFin     (LocalDateTime)
 *   est_occupee  → estOccupee  (boolean, défaut false)
 *   created_at   → createdAt   (LocalDateTime)
 *
 * Champs transients (non persistés, chargés par JOIN) :
 *   medecinNom   → users.username
 */
public class DisponibiliteMedecinArij {

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------------------------ //
    //  Champs persistés                                                   //
    // ------------------------------------------------------------------ //

    /** @Id @GeneratedValue */
    private Long          id;

    /**
     * @ManyToOne
     * @JoinColumn(name = "medecin_id")
     * Référence vers users.id du médecin.
     */
    private Long          medecinId;

    /** Date et heure de début du créneau. */
    private LocalDateTime dateDebut;

    /** Date et heure de fin du créneau. */
    private LocalDateTime dateFin;

    /**
     * true  = créneau occupé (consultation acceptée sur ce créneau)
     * false = créneau libre (défaut)
     *
     * Mis à true automatiquement quand une consultation est acceptée
     * via ServiceConsultationsArij.acceptConsultation().
     */
    private boolean       estOccupee = false;

    private LocalDateTime createdAt;

    // ------------------------------------------------------------------ //
    //  Champs transients (JOIN, non persistés)                           //
    // ------------------------------------------------------------------ //
    private String medecinNom;

    // ------------------------------------------------------------------ //
    //  Constructeurs                                                      //
    // ------------------------------------------------------------------ //
    public DisponibiliteMedecinArij() {
        this.createdAt  = LocalDateTime.now();
        this.estOccupee = false;
    }

    public DisponibiliteMedecinArij(Long medecinId,
                                     LocalDateTime dateDebut,
                                     LocalDateTime dateFin) {
        this();
        this.medecinId = medecinId;
        this.dateDebut = dateDebut;
        this.dateFin   = dateFin;
    }

    // ------------------------------------------------------------------ //
    //  Getters / Setters                                                  //
    // ------------------------------------------------------------------ //
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getMedecinId()                  { return medecinId; }
    public void setMedecinId(Long medecinId)    { this.medecinId = medecinId; }

    public LocalDateTime getDateDebut()                   { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut)     { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin()                     { return dateFin; }
    public void setDateFin(LocalDateTime dateFin)         { this.dateFin = dateFin; }

    public boolean isEstOccupee()               { return estOccupee; }
    public void setEstOccupee(boolean estOccupee) { this.estOccupee = estOccupee; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    public String getMedecinNom()               { return medecinNom; }
    public void setMedecinNom(String medecinNom) { this.medecinNom = medecinNom; }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /** Statut lisible pour la TableView. */
    public String getStatutAffichage() {
        return estOccupee ? "🔴 Occupé" : "🟢 Libre";
    }

    /** Date début formatée pour la TableView. */
    public String getDateDebutFormatee() {
        return dateDebut != null ? dateDebut.format(DISPLAY_FMT) : "—";
    }

    /** Date fin formatée pour la TableView. */
    public String getDateFinFormatee() {
        return dateFin != null ? dateFin.format(DISPLAY_FMT) : "—";
    }

    /** Vérifie si un instant donné tombe dans ce créneau. */
    public boolean contient(LocalDateTime instant) {
        if (dateDebut == null || dateFin == null) return false;
        return !instant.isBefore(dateDebut) && !instant.isAfter(dateFin);
    }

    @Override
    public String toString() {
        return "DisponibiliteMedecinArij{id=" + id
            + ", medecinId=" + medecinId
            + ", debut=" + getDateDebutFormatee()
            + ", fin=" + getDateFinFormatee()
            + ", occupee=" + estOccupee + "}";
    }
}
