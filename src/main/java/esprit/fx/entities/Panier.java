package esprit.fx.entities;

public class Panier {

    private int id;
    private double montantTotal;
    private int quantiteTotal;
    private int userId;

    public Panier() {}

    public Panier(int userId) {
        this.userId = userId;
        this.montantTotal = 0;
        this.quantiteTotal = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public int getQuantiteTotal() { return quantiteTotal; }
    public void setQuantiteTotal(int quantiteTotal) { this.quantiteTotal = quantiteTotal; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Panier{id=" + id + ", userId=" + userId +
                ", montantTotal=" + montantTotal + ", quantiteTotal=" + quantiteTotal + '}';
    }
}
