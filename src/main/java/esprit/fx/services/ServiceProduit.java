package esprit.fx.services;

import esprit.fx.entities.CategorieEnum;
import esprit.fx.entities.Produit;
import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit implements IService<Produit> {

    private final Connection conn;

    public ServiceProduit(Connection conn) {
        this.conn = conn != null ? conn : MyDB.getInstance().getConnection();
        ensureCompatibleSchema();
    }

    @Override
    public void ajouter(Produit produit) throws SQLException {
        String requete = "INSERT INTO product(name, description, price, stock, " +
                "is_available, is_prescription_required, brand, category_id_id, image, expire_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(requete)) {
            pstmt.setString(1, produit.getNom());
            pstmt.setString(2, produit.getDescription());
            pstmt.setDouble(3, produit.getPrix());
            pstmt.setInt(4, produit.getStock());
            pstmt.setBoolean(5, Boolean.TRUE.equals(produit.getDisponible()));
            pstmt.setBoolean(6, Boolean.TRUE.equals(produit.getPrescriptionRequise()));
            pstmt.setString(7, produit.getMarque());
            pstmt.setInt(8, resolveCategoryId(produit.getCategorie()));
            pstmt.setString(9, produit.getImage());
            pstmt.setObject(10, produit.getDateExpiration());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Supprimer d'abord les références dans ligne_panier
        String deleteLignePanier = "DELETE FROM ligne_panier WHERE produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteLignePanier)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        // Supprimer les références dans order_item
        String deleteOrderItem = "DELETE FROM order_item WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteOrderItem)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        // Supprimer le produit
        String sql = "DELETE FROM product WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Produit produit) throws SQLException {
        String requete = "UPDATE product SET name = ?, description = ?, price = ?, stock = ?, image = ?, " +
                "is_available = ?, is_prescription_required = ?, brand = ?, category_id_id = ?, expire_at = ? " +
                "WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(requete)) {
            pstmt.setString(1, produit.getNom());
            pstmt.setString(2, produit.getDescription());
            pstmt.setDouble(3, produit.getPrix());
            pstmt.setInt(4, produit.getStock());
            pstmt.setString(5, produit.getImage());
            pstmt.setBoolean(6, Boolean.TRUE.equals(produit.getDisponible()));
            pstmt.setBoolean(7, Boolean.TRUE.equals(produit.getPrescriptionRequise()));
            pstmt.setString(8, produit.getMarque());
            pstmt.setInt(9, resolveCategoryId(produit.getCategorie()));
            pstmt.setObject(10, produit.getDateExpiration());
            pstmt.setLong(11, produit.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Produit> getAll() throws SQLException {
        String requete = "SELECT p.*, pc.name AS category_name FROM product p " +
                "LEFT JOIN product_category pc ON pc.id = p.category_id_id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(requete)) {
            List<Produit> listproduits = new ArrayList<>();
            while (rs.next()) {
                listproduits.add(mapProduit(rs));
            }
            return listproduits;
        }
    }

    @Override
    public Produit afficherParId(int id) throws SQLException {
        String requete = "SELECT p.*, pc.name AS category_name FROM product p " +
                "LEFT JOIN product_category pc ON pc.id = p.category_id_id WHERE p.id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(requete)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapProduit(rs);
                }
            }
        }
        return null;
    }

    private Produit mapProduit(ResultSet rs) throws SQLException {
        LocalDate dateExpiration = null;
        java.sql.Date expireDate = rs.getDate("expire_at");
        if (expireDate != null) {
            dateExpiration = expireDate.toLocalDate();
        }

        CategorieEnum categorie = mapCategorie(rs.getString("category_name"));
        Produit p = new Produit(
                rs.getString("name"),
                rs.getString("description"),
                categorie,
                rs.getDouble("price"),
                rs.getInt("stock"),
                rs.getString("image"),
                rs.getBoolean("is_prescription_required"),
                rs.getBoolean("is_available"),
                rs.getString("brand"),
                dateExpiration
        );
        p.setId(rs.getLong("id"));
        return p;
    }

    private CategorieEnum mapCategorie(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        try {
            return CategorieEnum.valueOf(categoryName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Categorie inconnue: " + categoryName);
            return null;
        }
    }

    private int resolveCategoryId(CategorieEnum categorie) throws SQLException {
        if (categorie == null) {
            throw new SQLException("Categorie produit manquante.");
        }

        seedDefaultCategories();
        String sql = "SELECT id FROM product_category WHERE UPPER(name) = UPPER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categorie.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new SQLException("Categorie introuvable en base: " + categorie.name());
    }

    private void ensureCompatibleSchema() {
        try {
            if (!columnExists("product", "expire_at")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE product ADD COLUMN expire_at DATE DEFAULT NULL");
                }
            }
            seedDefaultCategories();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser le schema produit: " + e.getMessage(), e);
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void seedDefaultCategories() throws SQLException {
        String sql = "INSERT INTO product_category(name, description, created_at, updated_at) " +
                "SELECT ?, ?, ?, ? WHERE NOT EXISTS (" +
                "SELECT 1 FROM product_category WHERE UPPER(name) = UPPER(?))";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CategorieEnum categorie : CategorieEnum.values()) {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                ps.setString(1, categorie.name());
                ps.setString(2, categorie.getDescription());
                ps.setTimestamp(3, now);
                ps.setTimestamp(4, now);
                ps.setString(5, categorie.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
