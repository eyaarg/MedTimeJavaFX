package esprit.fx.services;

import esprit.fx.entities.LignePanier;
import esprit.fx.entities.Order;
import esprit.fx.entities.OrderItem;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceOrder {

    private final Connection conn;
    private final ServicePanier servicePanier;

    public ServiceOrder() {
        this.conn = MyDB.getInstance().getConnection();
        this.servicePanier = new ServicePanier();
    }

    /**
     * Valide le panier et crée une commande.
     * Tout se passe dans une seule transaction :
     * 1. Créer la ligne dans orders
     * 2. Copier chaque ligne_panier vers order_item
     * 3. Mettre à jour le stock des produits
     * 4. Vider le panier
     */
    public Order validerCommande(int userId) throws SQLException {
        // Récupérer le panier et ses lignes
        esprit.fx.entities.Panier panier = servicePanier.getPanierByUser(userId);
        List<LignePanier> lignes = servicePanier.getLignesByPanier(panier.getId());

        if (lignes.isEmpty()) {
            throw new SQLException("Le panier est vide.");
        }

        conn.setAutoCommit(false);
        try {
            // 1. Créer la commande
            int orderId = creerOrder(userId, panier.getMontantTotal());

            // 2. Copier les lignes vers order_item + mettre à jour le stock
            for (LignePanier ligne : lignes) {
                insererOrderItem(orderId, ligne);
                mettreAJourStock(ligne.getProduitId(), ligne.getQuantite());
            }

            // 3. Vider le panier
            servicePanier.viderPanier(panier.getId());

            conn.commit();

            Order order = new Order();
            order.setId(orderId);
            order.setUserId(userId);
            order.setTotal(panier.getMontantTotal());
            order.setStatus("EN_ATTENTE");
            return order;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private int creerOrder(int userId, double total) throws SQLException {
        String sql = "INSERT INTO orders(date_order, status, total, user_id) VALUES(NOW(), 'EN_ATTENTE', ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, total);
            ps.setInt(2, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Impossible de créer la commande.");
    }

    private void insererOrderItem(int orderId, LignePanier ligne) throws SQLException {
        String sql = "INSERT INTO order_item(quantity, price, product_id, order_ref_id) VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ligne.getQuantite());
            ps.setDouble(2, ligne.getPrixUnitaire());
            ps.setInt(3, ligne.getProduitId());
            ps.setInt(4, orderId);
            ps.executeUpdate();
        }
    }

    private void mettreAJourStock(int produitId, int quantite) throws SQLException {
        String sql = "UPDATE product SET stock = stock - ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    // Récupère les items d'une commande avec le nom du produit (JOIN)
    public List<OrderItem> getItemsByOrder(int orderId) throws SQLException {
        String sql = "SELECT oi.*, p.name AS product_name, p.image AS product_image " +
                     "FROM order_item oi JOIN product p ON oi.product_id = p.id " +
                     "WHERE oi.order_ref_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getDouble("price"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setOrderRefId(rs.getInt("order_ref_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductImage(rs.getString("product_image"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    // Met à jour le statut d'une commande
    public void updateStatut(int orderId, String statut) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    // Compte les commandes payées d'un utilisateur
    public int compterCommandesUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = 'PAYEE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // Vérifie si l'utilisateur est éligible à la réduction fidélité (> 2 commandes payées)
    public boolean estEligibleReduction(int userId) throws SQLException {
        return compterCommandesUser(userId) > 2;
    }

    // Applique la réduction de 20% si éligible
    public double appliquerReduction(double prix, int userId) throws SQLException {
        return estEligibleReduction(userId) ? prix * 0.80 : prix;
    }
}
