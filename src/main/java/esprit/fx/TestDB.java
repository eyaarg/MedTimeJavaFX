package esprit.fx;

import esprit.fx.utils.MyDB;

/**
 * Test simple de connexion à la base de données
 */
public class TestDB {
    public static void main(String[] args) {
        System.out.println("=== Test de connexion à la base de données ===");
        try {
            MyDB db = MyDB.getInstance();
            if (db.getConnection() != null) {
                System.out.println("✓ Connexion réussie!");
                System.out.println("✓ Base de données accessible");
            } else {
                System.out.println("✗ Connexion échouée - getConnection() retourne null");
            }
        } catch (Exception e) {
            System.out.println("✗ Erreur de connexion:");
            e.printStackTrace();
        }
    }
}
