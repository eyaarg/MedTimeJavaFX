package esprit.fx.services;

import esprit.fx.entities.Produit;
import esprit.fx.entities.CategorieEnum;  // ← Ajouter l'import
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProduit implements IService<Produit>{

    private Connection conn;

    public ServiceProduit(Connection conn) {
        this.conn = MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(Produit produit) throws SQLException {
        String requete = "INSERT INTO product(name, description, price, stock, image, " +
                "is_available, is_prescription_required, brand, expire_at, categorie) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = conn.prepareStatement(requete);
        pstmt.setString(1, produit.getNom());
        pstmt.setString(2, produit.getDescription());
        pstmt.setDouble(3, produit.getPrix());
        pstmt.setInt(4, produit.getStock());
        pstmt.setString(5, produit.getImage());
        pstmt.setBoolean(6, produit.getDisponible());
        pstmt.setBoolean(7, produit.getPrescriptionRequise());
        pstmt.setString(8, produit.getMarque());
        pstmt.setObject(9, produit.getDateExpiration());
        // Conversion Enum → String
        pstmt.setString(10,produit.getCategorie().name());

        pstmt.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM product WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public void modifier(Produit produit) throws SQLException {
        String requete = "UPDATE product SET name = ?, description = ?, price = ?, stock = ?, " +
                "image = ?, is_available = ?, is_prescription_required = ?, " +
                "brand = ?, expire_at = ?, categorie = ? WHERE id = ?";

        PreparedStatement pstmt = conn.prepareStatement(requete);
        pstmt.setString(1, produit.getNom());
        pstmt.setString(2, produit.getDescription());
        pstmt.setDouble(3, produit.getPrix());
        pstmt.setInt(4, produit.getStock());
        pstmt.setString(5, produit.getImage());
        pstmt.setBoolean(6, produit.getDisponible());
        pstmt.setBoolean(7, produit.getPrescriptionRequise());
        pstmt.setString(8, produit.getMarque());
        pstmt.setObject(9, produit.getDateExpiration());
        // Conversion Enum → String
        pstmt.setString(10,produit.getCategorie().name());

        int lignesAffectees = pstmt.executeUpdate();

        if (lignesAffectees > 0) {
            System.out.println("Produit modifié avec succès !");
        } else {
            System.out.println("Aucun produit trouvé avec l'ID : " + produit.getId());
        }
    }

    @Override
    public List<Produit> getAll() throws SQLException {
        String requete = "SELECT * FROM product";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(requete);
        List<Produit> listproduits = new ArrayList<Produit>();

        while (rs.next()) {
            java.sql.Date expireDate = rs.getDate("expire_at");
            java.time.LocalDate dateExpiration = expireDate != null ? expireDate.toLocalDate() : null;

            // Conversion String → Enum
            String categorieStr = rs.getString("categorie");
            CategorieEnum categorie = null;
            if (categorieStr != null) {
                try {
                    categorie = CategorieEnum.valueOf(categorieStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("Catégorie inconnue: " + categorieStr);
                }
            }

            Produit p = new Produit(
                    rs.getString("name"),
                    rs.getString("description"),
                    categorie,                                      // ← Enum
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getString("image"),
                    rs.getBoolean("is_prescription_required"),
                    rs.getBoolean("is_available"),
                    rs.getString("brand"),
                    dateExpiration
            );
            p.setId(rs.getLong("id"));
            listproduits.add(p);
        }
        return listproduits;
    }

    @Override
    public Produit afficherParId(int id) throws SQLException {
        String requete = "SELECT * FROM product WHERE id=?";
        PreparedStatement pstmt = conn.prepareStatement(requete);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            java.sql.Date expireDate = rs.getDate("expire_at");
            java.time.LocalDate dateExpiration = expireDate != null ? expireDate.toLocalDate() : null;

            String categorieStr = rs.getString("categorie");
            CategorieEnum categorie = null;
            if (categorieStr != null) {
                try {
                    categorie = CategorieEnum.valueOf(categorieStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("Catégorie inconnue: " + categorieStr);
                }
            }

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

        return null;
    }
}
