package esprit.fx.entities;

import java.time.LocalDateTime;

/**
 * Entité représentant un créneau de disponibilité pour un médecin.
 * Utilisée pour gérer les disponibilités et les consultations.
 */
public class DisponibiliteMedecinArij {
    
    private int id;
    private int medecinId;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean estOccupee;
    private String titre;

    // ── Constructeurs ─────────────────────────────────────────────────────

    public DisponibiliteMedecinArij() {}

    public DisponibiliteMedecinArij(int medecinId, LocalDateTime dateDebut, LocalDateTime dateFin, String titre) {
        this.medecinId = medecinId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.titre = titre;
        this.estOccupee = false;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public boolean isEstOccupee() { return estOccupee; }
    public void setEstOccupee(boolean estOccupee) { this.estOccupee = estOccupee; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    @Override
    public String toString() {
        return "DisponibiliteMedecinArij{" +
                "id=" + id +
                ", medecinId=" + medecinId +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", estOccupee=" + estOccupee +
                ", titre='" + titre + '\'' +
                '}';
    }
}
