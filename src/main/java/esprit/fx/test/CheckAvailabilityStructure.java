package esprit.fx.test;

import esprit.fx.utils.MyDB;
import java.sql.*;

public class CheckAvailabilityStructure {
    public static void main(String[] args) {
        try {
            Connection conn = MyDB.getInstance().getConnection();
            
            System.out.println("=== Structure de la table 'availability' ===\n");
            
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "availability", null);
            
            System.out.println("Colonnes:");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                String isNullable = columns.getString("IS_NULLABLE");
                String columnDef = columns.getString("COLUMN_DEF");
                
                System.out.printf("  - %s: %s(%d), Nullable: %s, Default: %s%n", 
                    columnName, columnType, columnSize, isNullable, columnDef);
            }
            
            System.out.println("\n=== Exemple de données ===\n");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM availability LIMIT 1");
            
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnCount = rsMetaData.getColumnCount();
            
            System.out.println("Colonnes dans le ResultSet:");
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("  %d. %s (%s)%n", 
                    i, 
                    rsMetaData.getColumnName(i), 
                    rsMetaData.getColumnTypeName(i));
            }
            
            if (rs.next()) {
                System.out.println("\nPremière ligne de données:");
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("  %s = %s%n", 
                        rsMetaData.getColumnName(i), 
                        rs.getString(i));
                }
            }
            
            rs.close();
            stmt.close();
            columns.close();
            
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
