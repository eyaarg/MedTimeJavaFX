package esprit.fx.entities;

public class LignePanier {

    private int id;
    private int quantite;
    private double prixUnitaire;
    private double sousTotal;
    private int produitId;
    private int panierId;
    private String produitNom;
    private String produitImage;

    public LignePanier() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public double getSousTotal() { return sousTotal; }
    public void setSousTotal(double sousTotal) { this.sousTotal = sousTotal; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    public int getPanierId() { return panierId; }
    public void setPanierId(int panierId) { this.panierId = panierId; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public String getProduitImage() { return produitImage; }
    public void setProduitImage(String produitImage) { this.produitImage = produitImage; }

    @Override
    public String toString() {
        return "LignePanier{id=" + id + ", produitId=" + produitId +
                ", quantite=" + quantite + ", prixUnitaire=" + prixUnitaire + '}';
    }
}
