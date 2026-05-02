package esprit.fx.test;

import esprit.fx.utils.MyDB;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Utilitaire one-shot : ajoute la colonne profile_photo à la table users si elle n'existe pas.
 */
public class AddProfilePhotoColumn {
    public static void main(String[] args) throws Exception {
        Connection conn = MyDB.getInstance().getConnection();

        // MySQL : ALTER TABLE ... ADD COLUMN IF NOT EXISTS (MySQL 8+)
        // Pour compatibilité, on ignore l'erreur si la colonne existe déjà
        try {
            PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE users ADD COLUMN profile_photo VARCHAR(500) NULL DEFAULT NULL");
            ps.executeUpdate();
            System.out.println("✔ Colonne profile_photo ajoutée avec succès.");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate column")) {
                System.out.println("✔ Colonne profile_photo existe déjà — rien à faire.");
            } else {
                throw e;
            }
        }
    }
}
