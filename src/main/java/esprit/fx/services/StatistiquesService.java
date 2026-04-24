package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatistiquesService {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // 1. Nombre de RDV par mois
    public Map<String, Integer> getNombreRDVParMois() {
        Map<String, Integer> map = new LinkedHashMap<>();
        // NB: Remplace 'date_rdv' par le vrai nom de la colonne de date dans ta table 'rendez_vous'
        String sql = "SELECT DATE_FORMAT(date_rdv, '%Y-%m') as mois, COUNT(*) as total FROM rendez_vous GROUP BY mois ORDER BY mois";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String mois = rs.getString("mois");
                if (mois != null) {
                    map.put(mois, rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("getNombreRDVParMois: " + e.getMessage());
        }
        return map;
    }

    // 2. Nombre de RDV par statut
    public Map<String, Integer> getNombreRDVParStatut() {
        Map<String, Integer> map = new LinkedHashMap<>();
        // NB: Remplace 'statut' par le vrai nom de la colonne de statut dans ta table 'rendez_vous'
        String sql = "SELECT statut, COUNT(*) as total FROM rendez_vous GROUP BY statut";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String statut = rs.getString("statut");
                if (statut != null) {
                    map.put(statut, rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("getNombreRDVParStatut: " + e.getMessage());
        }
        return map;
    }

    // 3. Nombre de RDV par médecin
    public Map<String, Integer> getNombreRDVParMedecin() {
        Map<String, Integer> map = new LinkedHashMap<>();
        // NB: Remplace 'doctor_id' par le vrai nom de la colonne dans ta table 'rendez_vous'
        String sql = "SELECT doctor_id, COUNT(*) as total FROM rendez_vous GROUP BY doctor_id";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String doctorId = String.valueOf(rs.getInt("doctor_id"));
                map.put("Médecin ID " + doctorId, rs.getInt("total"));
            }
        } catch (SQLException e) {
            System.err.println("getNombreRDVParMedecin: " + e.getMessage());
        }
        return map;
    }

    // 4. Taux de confirmation vs annulation
    public double getTauxConfirmation() {
        int total = 0;
        int confirmes = 0;
        String sqlTotal = "SELECT COUNT(*) FROM rendez_vous";
        // NB: Adapte 'statut' et 'CONFIRME'/'CONFIRMEE' selon les valeurs réelles de ta base
        String sqlConfirmes = "SELECT COUNT(*) FROM rendez_vous WHERE statut = 'CONFIRME' OR statut = 'CONFIRMEE'";
        
        try {
            try (PreparedStatement ps = conn().prepareStatement(sqlTotal);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn().prepareStatement(sqlConfirmes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    confirmes = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("getTauxConfirmation: " + e.getMessage());
        }
        
        if (total == 0) return 0.0;
        return ((double) confirmes / total) * 100;
    }
}
