package esprit.fx.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ListeAttente {

    private int           id;
    private int           patientId;
    private int           doctorId;
    private LocalDate     dateSouhaitee;
    private String        plageHoraire;   // "Matin" | "Après-midi" | "Soir" | "Tous"
    private LocalDateTime dateInscription;
    private String        statut;         // "EN_ATTENTE" | "NOTIFIE" | "EXPIRE"
    private LocalDateTime dateExpiration;

    // Infos affichage
    private String patientNom;
    private String doctorNom;

    public ListeAttente() {
        this.dateInscription = LocalDateTime.now();
        this.statut          = "EN_ATTENTE";
    }

    public ListeAttente(int patientId, int doctorId,
                        LocalDate dateSouhaitee, String plageHoraire) {
        this();
        this.patientId     = patientId;
        this.doctorId      = doctorId;
        this.dateSouhaitee = dateSouhaitee;
        this.plageHoraire  = plageHoraire;
        // Expiration : 48h après inscription
        this.dateExpiration = this.dateInscription.plusHours(48);
    }

    // Getters / Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public LocalDate getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(LocalDate dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }

    public String getPlageHoraire() { return plageHoraire; }
    public void setPlageHoraire(String plageHoraire) { this.plageHoraire = plageHoraire; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getDoctorNom() { return doctorNom; }
    public void setDoctorNom(String doctorNom) { this.doctorNom = doctorNom; }

    /** Vérifie si l'inscription est encore active (non expirée). */
    public boolean isActive() {
        return "EN_ATTENTE".equals(statut)
            && (dateExpiration == null || LocalDateTime.now().isBefore(dateExpiration));
    }

    @Override
    public String toString() {
        return "ListeAttente{id=" + id + ", patientId=" + patientId
             + ", doctorId=" + doctorId + ", statut=" + statut + "}";
    }
}
