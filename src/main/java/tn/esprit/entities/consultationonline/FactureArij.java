package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;

public class FactureArij {
    private int id;
    private String numeroFacture;
    private LocalDateTime dateEmission;
    private double montant;
    private String cheminPdf;
    private int paiementId;
    private int ordonnanceId;

    public FactureArij() {
    }

    public FactureArij(int id, String numeroFacture, LocalDateTime dateEmission, double montant, String cheminPdf,
                       int paiementId, int ordonnanceId) {
        this.id = id;
        this.numeroFacture = numeroFacture;
        this.dateEmission = dateEmission;
        this.montant = montant;
        this.cheminPdf = cheminPdf;
        this.paiementId = paiementId;
        this.ordonnanceId = ordonnanceId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public LocalDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getCheminPdf() {
        return cheminPdf;
    }

    public void setCheminPdf(String cheminPdf) {
        this.cheminPdf = cheminPdf;
    }

    public int getPaiementId() {
        return paiementId;
    }

    public void setPaiementId(int paiementId) {
        this.paiementId = paiementId;
    }

    public int getOrdonnanceId() {
        return ordonnanceId;
    }

    public void setOrdonnanceId(int ordonnanceId) {
        this.ordonnanceId = ordonnanceId;
    }

    @Override
    public String toString() {
        return "FactureArij{" +
                "id=" + id +
                ", numeroFacture='" + numeroFacture + '\'' +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", cheminPdf='" + cheminPdf + '\'' +
                ", paiementId=" + paiementId +
                ", ordonnanceId=" + ordonnanceId +
                '}';
    }
}
