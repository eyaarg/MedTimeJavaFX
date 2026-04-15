package esprit.fx.entities;


public enum CategorieEnum {
    MEDICAMENT( "Médicaments sur ordonnance ou non"),
    MATERIEL_MEDICAL("Équipements et dispositifs médicaux"),
    PARAPHARMACIE( "Produits de parapharmacie"),
    HYGIENE("Produits d'hygiène"),
    COMPLEMENT_ALIMENTAIRE( "Compléments alimentaires et vitamines");

    private final String description;

    CategorieEnum(String description) {
        this.description = description;
    }



    public String getDescription() {
        return description;
    }

    public String getNom() {
        return this.name();
    }

    @Override
    public String toString() {
        return this.name();  // Affiche MEDICAMENT, etc.
    }
}
