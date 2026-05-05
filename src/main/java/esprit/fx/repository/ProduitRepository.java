package esprit.fx.repository;

import esprit.fx.entities.Produit; // Importe ton entité
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitRepository {

    // On récupère les infos de connexion
    private final String url = "jdbc:mysql://localhost:3306/mediplatform";
    private final String user = "root";
    private final String password = "";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // 1. Récupérer uniquement les noms pour l'IA
    public List<String> findAllNames() {
        List<String> names = new ArrayList<>();
        String query = "SELECT name FROM products";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL (findAllNames) : " + e.getMessage());
        }
        return names;
    }

    // 2. Trouver le produit complet après la décision de l'IA
    public Produit findByName(String name) {
        String query = "SELECT * FROM products WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // On suppose que ton entité Product a un constructeur (id, name, price, stock)
                return new Produit(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL (findByName) : " + e.getMessage());
        }
        return null;
    }
}