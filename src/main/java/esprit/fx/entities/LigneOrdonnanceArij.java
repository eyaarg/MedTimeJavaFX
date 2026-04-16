package esprit.fx.entities;

public class LigneOrdonnanceArij {
    private int id;
    private int ordonnanceId;
    private String nomMedicament;
    private String dosage;
    private int quantite;
    private String dureeTraitement;
    private String instructions;

    public LigneOrdonnanceArij() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrdonnanceId() { return ordonnanceId; }
    public void setOrdonnanceId(int ordonnanceId) { this.ordonnanceId = ordonnanceId; }
    public String getNomMedicament() { return nomMedicament; }
    public void setNomMedicament(String nomMedicament) { this.nomMedicament = nomMedicament; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public String getDureeTraitement() { return dureeTraitement; }
    public void setDureeTraitement(String dureeTraitement) { this.dureeTraitement = dureeTraitement; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
