package esprit.fx.test;

import esprit.fx.utils.MyDB;
import java.sql.*;

public class CheckDoctorIds {
    public static void main(String[] args) {
        try {
            Connection conn = MyDB.getInstance().getConnection();
            
            System.out.println("=== Vérification des médecins dans la base de données ===\n");
            
            // Requête 1: Tous les utilisateurs
            System.out.println("1. Tous les utilisateurs:");
            String sql1 = "SELECT id, username, email FROM users ORDER BY id";
            Statement stmt1 = conn.createStatement();
            ResultSet rs1 = stmt1.executeQuery(sql1);
            
            while (rs1.next()) {
                System.out.printf("   ID: %d, Username: %s, Email: %s%n", 
                    rs1.getInt("id"), 
                    rs1.getString("username"), 
                    rs1.getString("email"));
            }
            rs1.close();
            stmt1.close();
            
            System.out.println("\n2. Utilisateurs avec rôle DOCTOR:");
            String sql2 = """
                SELECT DISTINCT u.id, u.username, u.email, r.name as role_name
                FROM users u
                INNER JOIN user_roles ur ON u.id = ur.user_id
                INNER JOIN roles r ON ur.role_id = r.id
                WHERE r.name LIKE '%DOCTOR%' OR r.name LIKE '%Medecin%'
                ORDER BY u.id
                """;
            
            Statement stmt2 = conn.createStatement();
            ResultSet rs2 = stmt2.executeQuery(sql2);
            
            boolean foundDoctors = false;
            while (rs2.next()) {
                foundDoctors = true;
                System.out.printf("   ID: %d, Username: %s, Email: %s, Role: %s%n", 
                    rs2.getInt("id"), 
                    rs2.getString("username"), 
                    rs2.getString("email"),
                    rs2.getString("role_name"));
            }
            
            if (!foundDoctors) {
                System.out.println("   ⚠ Aucun médecin trouvé!");
            }
            
            rs2.close();
            stmt2.close();
            
            System.out.println("\n3. Disponibilités existantes et leurs doctor_id:");
            String sql3 = "SELECT DISTINCT doctor_id FROM availability ORDER BY doctor_id";
            Statement stmt3 = conn.createStatement();
            ResultSet rs3 = stmt3.executeQuery(sql3);
            
            while (rs3.next()) {
                int doctorId = rs3.getInt("doctor_id");
                System.out.printf("   doctor_id: %d", doctorId);
                
                // Vérifier si cet ID existe dans users
                String checkSql = "SELECT username FROM users WHERE id = ?";
                PreparedStatement checkPs = conn.prepareStatement(checkSql);
                checkPs.setInt(1, doctorId);
                ResultSet checkRs = checkPs.executeQuery();
                
                if (checkRs.next()) {
                    System.out.printf(" ✓ (existe: %s)%n", checkRs.getString("username"));
                } else {
                    System.out.println(" ✗ (n'existe PAS dans users!)");
                }
                
                checkRs.close();
                checkPs.close();
            }
            rs3.close();
            stmt3.close();
            
            System.out.println("\n4. Structure de la table availability:");
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Vérifier les clés étrangères
            ResultSet fks = metaData.getImportedKeys(null, null, "availability");
            System.out.println("   Contraintes de clés étrangères:");
            while (fks.next()) {
                System.out.printf("     - Colonne: %s -> Table: %s.%s%n",
                    fks.getString("FKCOLUMN_NAME"),
                    fks.getString("PKTABLE_NAME"),
                    fks.getString("PKCOLUMN_NAME"));
            }
            fks.close();
            
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
