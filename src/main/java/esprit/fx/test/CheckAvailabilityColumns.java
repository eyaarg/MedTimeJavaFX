package esprit.fx.test;

import esprit.fx.utils.MyDB;
import java.sql.*;

public class CheckAvailabilityColumns {
    public static void main(String[] args) {
        try {
            Connection conn = MyDB.getInstance().getConnection();
            
            System.out.println("=== COLONNES DE LA TABLE 'availability' ===\n");
            
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "availability", null);
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                String isNullable = columns.getString("IS_NULLABLE");
                String columnDef = columns.getString("COLUMN_DEF");
                
                System.out.printf("%-20s | %-15s | Nullable: %-3s | Default: %s%n", 
                    columnName, columnType, isNullable, columnDef);
            }
            
            columns.close();
            System.out.println("\n===========================================");
            
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
