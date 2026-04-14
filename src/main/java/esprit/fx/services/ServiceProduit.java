package esprit.fx.services;

import esprit.fx.entities.Produit;
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
                "is_available, is_prescription_required, brand, expire_at, category_id_id) " +
                "VALUES('" +
                produit.getNom() + "','" +
                produit.getDescription() + "'," +
                produit.getPrix() + "," +
                produit.getStock() + ",'" +
                produit.getImage() + "'," +
                produit.getDisponible() + "," +
                produit.getPrescriptionRequise() + ",'" +
                produit.getMarque() + "','" +
                produit.getDateExpiration() + "'," +
                produit.getCategorie() + ")";

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(requete);

    }



    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM product WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();

    }
    @Override
    public void modifier(Produit produit) throws SQLException {
        String requete = "UPDATE product SET name = '" + produit.getNom() + "', " +
                "description = '" + produit.getDescription() + "', " +
                "price = " + produit.getPrix() + ", " +
                "stock = " + produit.getStock() + ", " +
                "image = '" + produit.getImage() + "', " +
                "is_available = " + produit.getDisponible() + ", " +
                "is_prescription_required = " + produit.getPrescriptionRequise() + ", " +
                "brand = '" + produit.getMarque() + "', " +
                "expire_at = '" + produit.getDateExpiration() + "', " +
                "category_id_id = " + (produit.getCategorie() + " " +
                "WHERE id = " + produit.getId());

        try (Statement stmt = conn.createStatement()) {
            int lignesAffectees = stmt.executeUpdate(requete);

            if (lignesAffectees > 0) {
                System.out.println("Produit modifié avec succès !");
            } else {
                System.out.println("Aucun produit trouvé avec l'ID : " + produit.getId());
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du produit : " + e.getMessage());
        }
    }

    @Override
    public List<Produit> getAll() throws SQLException {

        String requete="select * from product";
        Statement stmt= conn.createStatement();
        ResultSet rs= stmt.executeQuery(requete);
        List<Produit> listproduits= new ArrayList<Produit>();
        while(rs.next())
        {
            Produit p = new Produit(
                    rs.getString("name"),                    // nom
                    rs.getString("description"),             // description
                    rs.getString("categorie"),               // categorie (String)
                    rs.getDouble("price"),                   // prix
                    rs.getInt("stock"),                      // stock
                    rs.getString("image"),                   // image
                    rs.getBoolean("is_prescription_required"), // prescriptionRequise
                    rs.getBoolean("is_available"),           // disponible
                    rs.getString("brand"),                   // marque
                    rs.getDate("expire_at") != null ? rs.getDate("expire_at").toLocalDate() : null // dateExpiration
            );
            listproduits.add(p);
        }
        return listproduits;
    }

}

