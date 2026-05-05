package esprit.fx.entities;

import java.time.LocalDateTime;

public class HistoriqueRendezVous {
    private int id;
    private int rdvId;
    private String ancienStatut;
    private String nouveauStatut;
    private LocalDateTime dateChangement;
    private int modifiePar;
    private String commentaire;

    // Infos supplémentaires pour l'affichage
    private String modifieParNom;
    private String patientNom;
    private String doctorNom;

    public HistoriqueRendezVous() {
        this.dateChangement = LocalDateTime.now();
    }

    public HistoriqueRendezVous(int rdvId, String ancienStatut, String nouveauStatut, int modifiePar) {
        this();
        this.rdvId        = rdvId;
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.modifiePar   = modifiePar;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRdvId() { return rdvId; }
    public void setRdvId(int rdvId) { this.rdvId = rdvId; }

    public String getAncienStatut() { return ancienStatut; }
    public void setAncienStatut(String ancienStatut) { this.ancienStatut = ancienStatut; }

    public String getNouveauStatut() { return nouveauStatut; }
    public void setNouveauStatut(String nouveauStatut) { this.nouveauStatut = nouveauStatut; }

    public LocalDateTime getDateChangement() { return dateChangement; }
    public void setDateChangement(LocalDateTime dateChangement) { this.dateChangement = dateChangement; }

    public int getModifiePar() { return modifiePar; }
    public void setModifiePar(int modifiePar) { this.modifiePar = modifiePar; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getModifieParNom() { return modifieParNom; }
    public void setModifieParNom(String modifieParNom) { this.modifieParNom = modifieParNom; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getDoctorNom() { return doctorNom; }
    public void setDoctorNom(String doctorNom) { this.doctorNom = doctorNom; }
}
