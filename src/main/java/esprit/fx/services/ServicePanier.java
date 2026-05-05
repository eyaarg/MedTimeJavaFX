package esprit.fx.services;

import esprit.fx.entities.LignePanier;
import esprit.fx.entities.Panier;
import esprit.fx.entities.Produit;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePanier {

    private final Connection conn;

    public ServicePanier() {
        this.conn = MyDB.getInstance().getConnection();
    }

    // Récupère ou crée le panier de l'utilisateur
    public Panier getPanierByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM panier WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Panier p = new Panier();
                    p.setId(rs.getInt("id"));
                    p.setMontantTotal(rs.getDouble("montant_total"));
                    p.setQuantiteTotal(rs.getInt("quantite_total"));
                    p.setUserId(rs.getInt("user_id"));
                    return p;
                }
            }
        }
        // Pas de panier → on en crée un
        return creerPanier(userId);
    }

    private Panier creerPanier(int userId) throws SQLException {
        String sql = "INSERT INTO panier(montant_total, quantite_total, user_id) VALUES(0, 0, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Panier p = new Panier(userId);
                    p.setId(keys.getInt(1));
                    return p;
                }
            }
        }
        throw new SQLException("Impossible de créer le panier.");
    }

    // Ajoute un produit au panier (ou incrémente la quantité si déjà présent)
    public void ajouterAuPanier(int userId, Produit produit, int quantite) throws SQLException {
        Panier panier = getPanierByUser(userId);

        // Vérifier l'éligibilité à la réduction fidélité
        ServiceOrder serviceOrder = new ServiceOrder();
        double prixFinal = serviceOrder.appliquerReduction(produit.getPrix(), userId);

        // Vérifier si le produit est déjà dans le panier
        String checkSql = "SELECT * FROM ligne_panier WHERE panier_id = ? AND produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, panier.getId());
            ps.setInt(2, produit.getId().intValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Déjà présent → on incrémente
                    int newQte = rs.getInt("quantite") + quantite;
                    double newSousTotal = newQte * prixFinal;
                    String updateSql = "UPDATE ligne_panier SET quantite = ?, sous_total = ? WHERE id = ?";
                    try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                        ups.setInt(1, newQte);
                        ups.setDouble(2, newSousTotal);
                        ups.setInt(3, rs.getInt("id"));
                        ups.executeUpdate();
                    }
                } else {
                    // Nouveau produit → on insère
                    String insertSql = "INSERT INTO ligne_panier(quantite, prix_unitaire, sous_total, produit_id, panier_id) VALUES(?, ?, ?, ?, ?)";
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setInt(1, quantite);
                        ins.setDouble(2, prixFinal);
                        ins.setDouble(3, quantite * prixFinal);
                        ins.setInt(4, produit.getId().intValue());
                        ins.setInt(5, panier.getId());
                        ins.executeUpdate();
                    }
                }
            }
        }
        recalculerPanier(panier.getId());
    }

    // Récupère les lignes du panier avec le nom et l'image du produit (JOIN)
    public List<LignePanier> getLignesByPanier(int panierId) throws SQLException {
        String sql = "SELECT lp.*, p.name AS produit_nom, p.image AS produit_image " +
                     "FROM ligne_panier lp JOIN product p ON lp.produit_id = p.id " +
                     "WHERE lp.panier_id = ?";
        List<LignePanier> lignes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LignePanier l = new LignePanier();
                    l.setId(rs.getInt("id"));
                    l.setQuantite(rs.getInt("quantite"));
                    l.setPrixUnitaire(rs.getDouble("prix_unitaire"));
                    l.setSousTotal(rs.getDouble("sous_total"));
                    l.setProduitId(rs.getInt("produit_id"));
                    l.setPanierId(rs.getInt("panier_id"));
                    l.setProduitNom(rs.getString("produit_nom"));
                    l.setProduitImage(rs.getString("produit_image"));
                    lignes.add(l);
                }
            }
        }
        return lignes;
    }

    public void supprimerLigne(int ligneId, int panierId) throws SQLException {
        String sql = "DELETE FROM ligne_panier WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ligneId);
            ps.executeUpdate();
        }
        recalculerPanier(panierId);
    }

    public void viderPanier(int panierId) throws SQLException {
        String sql = "DELETE FROM ligne_panier WHERE panier_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ps.executeUpdate();
        }
        // Remettre les totaux à zéro
        String reset = "UPDATE panier SET montant_total = 0, quantite_total = 0 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(reset)) {
            ps.setInt(1, panierId);
            ps.executeUpdate();
        }
    }

    // Recalcule montant_total et quantite_total du panier
    private void recalculerPanier(int panierId) throws SQLException {
        String sql = "UPDATE panier SET " +
                     "montant_total = (SELECT COALESCE(SUM(sous_total), 0) FROM ligne_panier WHERE panier_id = ?), " +
                     "quantite_total = (SELECT COALESCE(SUM(quantite), 0) FROM ligne_panier WHERE panier_id = ?) " +
                     "WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ps.setInt(2, panierId);
            ps.setInt(3, panierId);
            ps.executeUpdate();
        }
    }
}
