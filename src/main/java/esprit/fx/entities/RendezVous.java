package esprit.fx.entities;

import java.time.LocalDateTime;

public class RendezVous {
    private int id;
    private int patientId;
    private int doctorId;
    private LocalDateTime dateHeure;
    private String motif;
    private String statut; // "DEMANDE", "CONFIRME", "ANNULE", "TERMINE"
    private String notes;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Informations supplémentaires (pour l'affichage)
    private String patientNom;
    private String doctorNom;
    private String patientEmail;
    private String doctorEmail;

    // Constructeurs
    public RendezVous() {
        this.dateCreation = LocalDateTime.now();
        this.statut = "DEMANDE";
    }

    public RendezVous(int patientId, int doctorId, LocalDateTime dateHeure, String motif) {
        this();
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.dateHeure = dateHeure;
        this.motif = motif;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getDoctorNom() { return doctorNom; }
    public void setDoctorNom(String doctorNom) { this.doctorNom = doctorNom; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    @Override
    public String toString() {
        return "RendezVous{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", doctorId=" + doctorId +
                ", dateHeure=" + dateHeure +
                ", motif='" + motif + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}