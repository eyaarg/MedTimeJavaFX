package esprit.fx.utils;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Utilitaire pour initialiser les tables de la base de données.
 * Crée les tables et colonnes nécessaires si elles n'existent pas.
 */
public class InitDatabaseArij {

    public static void initializeTables() {
        try (Connection conn = MyDB.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // ── 1. Table disponibilite_medecin ────────────────────────────
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS disponibilite_medecin (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "medecin_id INT NOT NULL, " +
                "date_debut DATETIME NOT NULL, " +
                "date_fin DATETIME NOT NULL, " +
                "est_occupee BOOLEAN DEFAULT FALSE, " +
                "titre VARCHAR(255) DEFAULT 'Créneau libre', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_disponibilite_medecin FOREIGN KEY (medecin_id) " +
                "REFERENCES users(id) ON DELETE CASCADE, " +
                "INDEX idx_medecin_id (medecin_id), " +
                "INDEX idx_date_debut (date_debut), " +
                "INDEX idx_est_occupee (est_occupee)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
            System.out.println("[InitDatabaseArij] ✅ Table disponibilite_medecin OK");

            // ── 2. Colonne lien_meet dans consultations ───────────────────
            try {
                stmt.executeUpdate(
                    "ALTER TABLE consultations ADD COLUMN lien_meet VARCHAR(500) NULL"
                );
                System.out.println("[InitDatabaseArij] ✅ Colonne lien_meet ajoutée à consultations");
            } catch (Exception e) {
                // Colonne déjà existante — ignorer
                System.out.println("[InitDatabaseArij] ℹ️ Colonne lien_meet déjà présente");
            }

            // ── 3. Colonne link dans notifications ────────────────────────
            try {
                stmt.executeUpdate(
                    "ALTER TABLE notifications ADD COLUMN link VARCHAR(500) NULL"
                );
                System.out.println("[InitDatabaseArij] ✅ Colonne link ajoutée à notifications");
            } catch (Exception e) {
                // Colonne déjà existante — ignorer
                System.out.println("[InitDatabaseArij] ℹ️ Colonne link déjà présente dans notifications");
            }

        } catch (Exception e) {
            System.err.println("[InitDatabaseArij] ❌ Erreur initialisation: " + e.getMessage());
        }
    }
}
