package esprit.fx.entities;

import java.time.LocalDateTime;

public class FactureArij {
    private int id;
    private String numeroFacture;
    private LocalDateTime dateEmission;
    private double montant;
    private String cheminPdf;
    private int paiementId;
    private int ordonnanceId;

    public FactureArij() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }
    public LocalDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDateTime dateEmission) { this.dateEmission = dateEmission; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
    public String getCheminPdf() { return cheminPdf; }
    public void setCheminPdf(String cheminPdf) { this.cheminPdf = cheminPdf; }
    public int getPaiementId() { return paiementId; }
    public void setPaiementId(int paiementId) { this.paiementId = paiementId; }
    public int getOrdonnanceId() { return ordonnanceId; }
    public void setOrdonnanceId(int ordonnanceId) { this.ordonnanceId = ordonnanceId; }
}
