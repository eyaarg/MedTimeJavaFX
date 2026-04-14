package esprit.fx.entities;

import java.time.LocalDate;





public class Produit {

    // Attributs
    private Long id;
    private String nom;
    private String description;
    private String categorie;
    private Double prix;
    private Integer stock;
    private String image;
    private Boolean prescriptionRequise;
    private Boolean disponible;
    private String marque;
    private LocalDate dateExpiration;

    // 1. Constructeur par défaut
    public Produit() {
        this.disponible = true;
        this.stock = 0;
    }

    // 2. Constructeur paramétré complet (avec id)
    public Produit(Long id, String nom, String description, String categorie,
                   Double prix, Integer stock, String image, Boolean prescriptionRequise,
                   Boolean disponible, String marque, LocalDate dateExpiration) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.prix = prix;
        this.stock = stock;
        this.image = image;
        this.prescriptionRequise = prescriptionRequise;
        this.disponible = disponible;
        this.marque = marque;
        this.dateExpiration = dateExpiration;
    }

    // 3. Constructeur paramétré sans l'id (pour la création en base)
    public Produit(String nom, String description, String categorie,
                   Double prix, Integer stock, String image, Boolean prescriptionRequise,
                   Boolean disponible, String marque, LocalDate dateExpiration) {
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.prix = prix;
        this.stock = stock;
        this.image = image;
        this.prescriptionRequise = prescriptionRequise;
        this.disponible = disponible;
        this.marque = marque;
        this.dateExpiration = dateExpiration;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Boolean getPrescriptionRequise() {
        return prescriptionRequise;
    }

    public void setPrescriptionRequise(Boolean prescriptionRequise) {
        this.prescriptionRequise = prescriptionRequise;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    @Override
    public String toString() {
        return "Produit{" +

                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", categorie=" + categorie +
                ", prix=" + prix +
                ", stock=" + stock +
                ", image='" + image + '\'' +
                ", prescriptionRequise=" + prescriptionRequise +
                ", disponible=" + disponible +
                ", marque='" + marque + '\'' +
                ", dateExpiration=" + dateExpiration +
                '}';
    }
}

