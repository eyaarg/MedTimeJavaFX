package esprit.fx.test;

import java.sql.*;

/**
 * Test rapide : vérifie si la colonne 'region' existe dans la table doctors/users
 */
public class CheckDoctorRegion {
    public static void main(String[] args) throws Exception {
        String url  = "jdbc:mysql://localhost:3306/mediplatform";
        String user = "root";
        String pass = "";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("✅ Connexion OK\n");

            // Vérifier colonnes de la table doctors
            System.out.println("=== TABLE: doctors ===");
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet cols = meta.getColumns(null, null, "doctors", null);
            boolean regionFoundDoctors = false;
            while (cols.next()) {
                String col = cols.getString("COLUMN_NAME");
                String type = cols.getString("TYPE_NAME");
                System.out.println("  " + col + " (" + type + ")");
                if (col.equalsIgnoreCase("region")) regionFoundDoctors = true;
            }
            System.out.println(regionFoundDoctors
                ? "\n✅ Colonne 'region' TROUVÉE dans doctors !"
                : "\n❌ Colonne 'region' NON TROUVÉE dans doctors");

            // Vérifier colonnes de la table users aussi
            System.out.println("\n=== TABLE: users ===");
            ResultSet colsU = meta.getColumns(null, null, "users", null);
            boolean regionFoundUsers = false;
            while (colsU.next()) {
                String col = colsU.getString("COLUMN_NAME");
                String type = colsU.getString("TYPE_NAME");
                System.out.println("  " + col + " (" + type + ")");
                if (col.equalsIgnoreCase("region")) regionFoundUsers = true;
            }
            System.out.println(regionFoundUsers
                ? "\n✅ Colonne 'region' TROUVÉE dans users !"
                : "\n❌ Colonne 'region' NON TROUVÉE dans users");

            // Afficher un exemple de données doctors si region existe
            if (regionFoundDoctors) {
                System.out.println("\n=== DONNÉES doctors (region) ===");
                ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT id, user_id, region FROM doctors LIMIT 5");
                while (rs.next()) {
                    System.out.println("  doctor_id=" + rs.getInt("id")
                        + " | user_id=" + rs.getInt("user_id")
                        + " | region='" + rs.getString("region") + "'");
                }
            }
        }
    }
}
