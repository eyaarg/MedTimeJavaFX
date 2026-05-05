package esprit.fx.entities;

import java.time.LocalDate;

public class Produit {

    // Attributs
    private Long id;
    private String nom;
    private String description;
    private Integer categoryId;       // FK → product_category.id  (column: category_id_id)
    private CategorieEnum categorie;  // enum representation of the category
    private String categorieName;     // display-only, not persisted directly
    private Double prix;
    private Integer stock;
    private String image;
    private Boolean prescriptionRequise;
    private Boolean disponible;
    private String marque;
    private LocalDate dateExpiration; // column: expire_at

    // Constructeur par défaut
    public Produit() {
        this.disponible = true;
        this.stock = 0;
    }

    // Constructeur complet (avec id) — categoryId version
    public Produit(Long id, String nom, String description, Integer categoryId,
                   Double prix, Integer stock, String image, Boolean prescriptionRequise,
                   Boolean disponible, String marque, LocalDate dateExpiration) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.categoryId = categoryId;
        this.prix = prix;
        this.stock = stock;
        this.image = image;
        this.prescriptionRequise = prescriptionRequise;
        this.disponible = disponible;
        this.marque = marque;
        this.dateExpiration = dateExpiration;
    }

    // Constructeur sans id (pour la création en base) — avec CategorieEnum
    public Produit(String nom, String description, CategorieEnum categorie,
                   Double prix, Integer stock, String image, Boolean prescriptionRequise,
                   Boolean disponible, String marque, LocalDate dateExpiration) {
        this.nom = nom;
        this.description = description;
        this.categorie = categorie;
        this.categorieName = categorie != null ? categorie.name() : null;
        this.prix = prix;
        this.stock = stock;
        this.image = image;
        this.prescriptionRequise = prescriptionRequise;
        this.disponible = disponible;
        this.marque = marque;
        this.dateExpiration = dateExpiration;
    }

    // Constructeur sans id (pour la création en base) — avec Integer categoryId
    public Produit(String nom, String description, Integer categoryId,
                   Double prix, Integer stock, String image, Boolean prescriptionRequise,
                   Boolean disponible, String marque, LocalDate dateExpiration) {
        this.nom = nom;
        this.description = description;
        this.categoryId = categoryId;
        this.prix = prix;
        this.stock = stock;
        this.image = image;
        this.prescriptionRequise = prescriptionRequise;
        this.disponible = disponible;
        this.marque = marque;
        this.dateExpiration = dateExpiration;
    }

    // Constructeur minimal pour ProduitRepository (id, nom, prix, stock)
    public Produit(int id, String nom, double prix, int stock) {
        this.id = (long) id;
        this.nom = nom;
        this.prix = prix;
        this.stock = stock;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    /** CategorieEnum representation of the category. */
    public CategorieEnum getCategorie() { return categorie; }
    public void setCategorie(CategorieEnum categorie) {
        this.categorie = categorie;
        if (categorie != null) {
            this.categorieName = categorie.name();
        }
    }

    /** Display name loaded from product_category — not a DB column on this table. */
    public String getCategorieName() { return categorieName; }
    public void setCategorieName(String categorieName) {
        this.categorieName = categorieName;
        if (categorieName != null && !categorieName.isBlank()) {
            try {
                this.categorie = CategorieEnum.valueOf(categorieName.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Boolean getPrescriptionRequise() { return prescriptionRequise; }
    public void setPrescriptionRequise(Boolean prescriptionRequise) { this.prescriptionRequise = prescriptionRequise; }

    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    @Override
    public String toString() {
        return "Produit{" +
                "nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", categorie=" + categorie +
                ", categoryId=" + categoryId +
                ", categorieName='" + categorieName + '\'' +
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
