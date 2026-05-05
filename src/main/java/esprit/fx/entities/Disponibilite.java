package esprit.fx.entities;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Disponibilite {
    private int id;
    private int doctorId;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean estDisponible;
    private String notes;
    private LocalDateTime dateCreation;
    
    // Informations supplémentaires (pour l'affichage)
    private String doctorNom;
    private String doctorEmail;

    // Constructeurs
    public Disponibilite() {
        this.dateCreation = LocalDateTime.now();
        this.estDisponible = true;
    }

    public Disponibilite(int doctorId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        this();
        this.doctorId = doctorId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public boolean isEstDisponible() { return estDisponible; }
    public void setEstDisponible(boolean estDisponible) { this.estDisponible = estDisponible; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getDoctorNom() { return doctorNom; }
    public void setDoctorNom(String doctorNom) { this.doctorNom = doctorNom; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    // Méthodes utilitaires
    public boolean isDisponiblePour(LocalDateTime dateHeure, int dureeMinutes) {
        if (!estDisponible) return false;
        LocalDateTime finRendezVous = dateHeure.plusMinutes(dureeMinutes);
        return !dateHeure.isBefore(dateDebut) && !finRendezVous.isAfter(dateFin);
    }

    @Override
    public String toString() {
        return "Disponibilite{" +
                "id=" + id +
                ", doctorId=" + doctorId +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", estDisponible=" + estDisponible +
                '}';
    }
}