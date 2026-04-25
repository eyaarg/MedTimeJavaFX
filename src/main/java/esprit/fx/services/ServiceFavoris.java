package esprit.fx.services;

import esprit.fx.entities.Produit;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestion des favoris en mémoire (pas de base de données).
 * Singleton — les favoris persistent pendant toute la session.
 */
public class ServiceFavoris {

    private static ServiceFavoris instance;
    private final List<Produit> favoris = new ArrayList<>();

    private ServiceFavoris() {}

    public static ServiceFavoris getInstance() {
        if (instance == null) {
            instance = new ServiceFavoris();
        }
        return instance;
    }

    public void ajouterFavori(Produit produit) {
        boolean dejaDedans = favoris.stream()
                .anyMatch(p -> p.getId().equals(produit.getId()));
        if (!dejaDedans) {
            favoris.add(produit);
        }
    }

    public void supprimerFavori(Produit produit) {
        favoris.removeIf(p -> p.getId().equals(produit.getId()));
    }

    public boolean estFavori(Produit produit) {
        return favoris.stream().anyMatch(p -> p.getId().equals(produit.getId()));
    }

    public List<Produit> getFavoris() {
        return new ArrayList<>(favoris);
    }

    public void vider() {
        favoris.clear();
    }
}
