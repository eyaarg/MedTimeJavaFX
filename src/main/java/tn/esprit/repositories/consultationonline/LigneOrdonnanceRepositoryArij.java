package tn.esprit.repositories.consultationonline;

import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.utils.consultationonline.DatabaseConnectionArij;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LigneOrdonnanceRepositoryArij {

    public List<LigneOrdonnanceArij> findByOrdonnanceId(int ordonnanceId) {
        List<LigneOrdonnanceArij> list = new ArrayList<>();
        String sql = "SELECT * FROM ligne_ordonnances WHERE ordonnance_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return list;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ordonnanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("findByOrdonnanceId error: " + e.getMessage());
        }
        return list;
    }

    public void create(LigneOrdonnanceArij l) {
        String sql = "INSERT INTO ligne_ordonnances (ordonnance_id, nom_medicament, dosage, quantite, duree_traitement, instructions) VALUES (?,?,?,?,?,?)";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, l.getOrdonnanceId());
            ps.setString(2, l.getNomMedicament());
            ps.setString(3, l.getDosage());
            ps.setInt(4, l.getQuantite());
            ps.setString(5, l.getDureeTraitement());
            ps.setString(6, l.getInstructions());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("create ligne ordonnance error: " + e.getMessage());
        }
    }

    public void update(LigneOrdonnanceArij l) {
        String sql = "UPDATE ligne_ordonnances SET ordonnance_id=?, nom_medicament=?, dosage=?, quantite=?, duree_traitement=?, instructions=? WHERE id=?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, l.getOrdonnanceId());
            ps.setString(2, l.getNomMedicament());
            ps.setString(3, l.getDosage());
            ps.setInt(4, l.getQuantite());
            ps.setString(5, l.getDureeTraitement());
            ps.setString(6, l.getInstructions());
            ps.setInt(7, l.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("update ligne ordonnance error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM ligne_ordonnances WHERE id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("delete ligne ordonnance error: " + e.getMessage());
        }
    }

    public void deleteByOrdonnanceId(int ordonnanceId) {
        String sql = "DELETE FROM ligne_ordonnances WHERE ordonnance_id = ?";
        Connection conn = DatabaseConnectionArij.getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ordonnanceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteByOrdonnanceId error: " + e.getMessage());
        }
    }

    private LigneOrdonnanceArij mapRow(ResultSet rs) throws SQLException {
        LigneOrdonnanceArij l = new LigneOrdonnanceArij();
        l.setId(rs.getInt("id"));
        l.setOrdonnanceId(rs.getInt("ordonnance_id"));
        l.setNomMedicament(rs.getString("nom_medicament"));
        l.setDosage(rs.getString("dosage"));
        l.setQuantite(rs.getInt("quantite"));
        l.setDureeTraitement(rs.getString("duree_traitement"));
        l.setInstructions(rs.getString("instructions"));
        return l;
    }
}
