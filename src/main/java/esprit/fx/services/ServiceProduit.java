package esprit.fx.services;

import esprit.fx.entities.Produit;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit implements IService<Produit> {

    private Connection conn;

    public ServiceProduit(Connection conn) {
        this.conn = MyDB.getInstance().getConnection();
    }

    // ── Category helpers ──────────────────────────────────────────────────────

    /**
     * Returns all category names from product_category, ordered by name.
     * Used to populate the ComboBox in the UI.
     */
    public List<String> getAllCategoryNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM product_category ORDER BY name";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    /**
     * Resolves a category name to its id in product_category.
     * Returns null if not found.
     */
    public Integer resolveCategoryId(String categoryName) throws SQLException {
        if (categoryName == null || categoryName.isBlank()) return null;
        String sql = "SELECT id FROM product_category WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return null;
    }

    /**
     * Resolves a category id to its name. Returns null if not found.
     */
    public String resolveCategoryName(int categoryId) throws SQLException {
        String sql = "SELECT name FROM product_category WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        }
        return null;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public void ajouter(Produit produit) throws SQLException {
        String sql = "INSERT INTO product (name, description, price, stock, image, " +
                "is_available, is_prescription_required, brand, expire_at, category_id_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getDescription());
            ps.setDouble(3, produit.getPrix());
            ps.setInt(4, produit.getStock());
            ps.setString(5, produit.getImage());
            ps.setBoolean(6, produit.getDisponible());
            ps.setBoolean(7, produit.getPrescriptionRequise());
            ps.setString(8, produit.getMarque());
            ps.setObject(9, produit.getDateExpiration()); // LocalDate → DATE
            ps.setObject(10, produit.getCategoryId());    // nullable Integer
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Produit produit) throws SQLException {
        String sql = "UPDATE product SET name = ?, description = ?, price = ?, stock = ?, " +
                "image = ?, is_available = ?, is_prescription_required = ?, " +
                "brand = ?, expire_at = ?, category_id_id = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getDescription());
            ps.setDouble(3, produit.getPrix());
            ps.setInt(4, produit.getStock());
            ps.setString(5, produit.getImage());
            ps.setBoolean(6, produit.getDisponible());
            ps.setBoolean(7, produit.getPrescriptionRequise());
            ps.setString(8, produit.getMarque());
            ps.setObject(9, produit.getDateExpiration());
            ps.setObject(10, produit.getCategoryId());
            ps.setLong(11, produit.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Produit modifié avec succès !");
            } else {
                System.out.println("Aucun produit trouvé avec l'ID : " + produit.getId());
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM product WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Produit> getAll() throws SQLException {
        String sql = "SELECT p.*, pc.name AS category_name " +
                "FROM product p " +
                "LEFT JOIN product_category pc ON p.category_id_id = pc.id";
        List<Produit> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Produit afficherParId(int id) throws SQLException {
        String sql = "SELECT p.*, pc.name AS category_name " +
                "FROM product p " +
                "LEFT JOIN product_category pc ON p.category_id_id = pc.id " +
                "WHERE p.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Produit mapRow(ResultSet rs) throws SQLException {
        java.sql.Date expireDate = rs.getDate("expire_at");
        java.time.LocalDate dateExpiration = expireDate != null ? expireDate.toLocalDate() : null;

        int rawCatId = rs.getInt("category_id_id");
        Integer categoryId = rs.wasNull() ? null : rawCatId;

        Produit p = new Produit(
                rs.getString("name"),
                rs.getString("description"),
                categoryId,
                rs.getDouble("price"),
                rs.getInt("stock"),
                rs.getString("image"),
                rs.getBoolean("is_prescription_required"),
                rs.getBoolean("is_available"),
                rs.getString("brand"),
                dateExpiration
        );
        p.setId(rs.getLong("id"));
        p.setCategorieName(rs.getString("category_name")); // may be null
        return p;
    }
}
