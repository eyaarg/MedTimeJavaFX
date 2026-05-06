package esprit.fx.services;

import esprit.fx.entities.Disponibilite;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service JDBC pour la table `availability`.
 * Colonnes : id, start_date, start_time, end_date, end_time,
 *            is_online, notes(absent→""), doctor_id, created_at(absent→now)
 */
public class ServiceDisponibilite implements IService<Disponibilite> {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    public ServiceDisponibilite() {}

    // ── INSERT ────────────────────────────────────────────────────────────────
    @Override
    public void ajouter(Disponibilite d) throws SQLException {
        String sql = "INSERT INTO availability " +
                     "(doctor_id, start_date, start_time, end_date, end_time, is_online) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, d.getDoctorId());

            if (d.getDateDebut() != null) {
                ps.setDate(2, Date.valueOf(d.getDateDebut().toLocalDate()));
                ps.setTime(3, Time.valueOf(d.getDateDebut().toLocalTime()));
            } else {
                ps.setDate(2, Date.valueOf(LocalDateTime.now().toLocalDate()));
                ps.setTime(3, Time.valueOf(LocalDateTime.now().toLocalTime()));
            }
            if (d.getDateFin() != null) {
                ps.setDate(4, Date.valueOf(d.getDateFin().toLocalDate()));
                ps.setTime(5, Time.valueOf(d.getDateFin().toLocalTime()));
            } else {
                ps.setDate(4, Date.valueOf(LocalDateTime.now().toLocalDate()));
                ps.setTime(5, Time.valueOf(LocalDateTime.now().plusHours(1).toLocalTime()));
            }
            ps.setBoolean(6, !d.isEstDisponible());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) d.setId(rs.getInt(1));
            }
        }
        System.out.println("✓ Disponibilité ajoutée — ID: " + d.getId());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public void modifier(Disponibilite d) throws SQLException {
        String sql = "UPDATE availability SET " +
                     "doctor_id=?, start_date=?, start_time=?, end_date=?, end_time=?, is_online=? " +
                     "WHERE id=?";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, d.getDoctorId());
            if (d.getDateDebut() != null) {
                ps.setDate(2, Date.valueOf(d.getDateDebut().toLocalDate()));
                ps.setTime(3, Time.valueOf(d.getDateDebut().toLocalTime()));
            } else {
                ps.setNull(2, Types.DATE);
                ps.setNull(3, Types.TIME);
            }
            if (d.getDateFin() != null) {
                ps.setDate(4, Date.valueOf(d.getDateFin().toLocalDate()));
                ps.setTime(5, Time.valueOf(d.getDateFin().toLocalTime()));
            } else {
                ps.setNull(4, Types.DATE);
                ps.setNull(5, Types.TIME);
            }
            ps.setBoolean(6, !d.isEstDisponible());
            ps.setInt(7, d.getId());
            ps.executeUpdate();
        }
        System.out.println("✓ Disponibilité modifiée — ID: " + d.getId());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public void supprimer(int id) throws SQLException {
        // Check for linked rendez_vous first
        try (Connection c = conn();
             PreparedStatement check = c.prepareStatement(
                     "SELECT COUNT(*) FROM rendez_vous WHERE disponibilite_id=?")) {
            check.setInt(1, id);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException(
                        "Impossible de supprimer : " + rs.getInt(1) +
                        " rendez-vous lié(s) à cette disponibilité.");
                }
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("Unknown column")) throw e;
            // column doesn't exist — safe to delete
        }

        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM availability WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        System.out.println("✓ Disponibilité supprimée — ID: " + id);
    }

    // ── SELECT ALL ────────────────────────────────────────────────────────────
    @Override
    public List<Disponibilite> getAll() throws SQLException {
        String sql = "SELECT a.*, u.username AS doctor_nom, u.email AS doctor_email " +
                     "FROM availability a " +
                     "LEFT JOIN users u ON a.doctor_id = u.id " +
                     "ORDER BY a.id DESC";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Disponibilite> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    // ── SELECT BY ID ──────────────────────────────────────────────────────────
    @Override
    public Disponibilite afficherParId(int id) throws SQLException {
        String sql = "SELECT a.*, u.username AS doctor_nom, u.email AS doctor_email " +
                     "FROM availability a " +
                     "LEFT JOIN users u ON a.doctor_id = u.id " +
                     "WHERE a.id=?";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ── SELECT BY DOCTOR ──────────────────────────────────────────────────────
    public List<Disponibilite> getDisponibilitesParDocteur(int doctorId) throws SQLException {
        String sql = "SELECT a.*, u.username AS doctor_nom, u.email AS doctor_email " +
                     "FROM availability a " +
                     "LEFT JOIN users u ON a.doctor_id = u.id " +
                     "WHERE a.doctor_id=? ORDER BY a.start_date ASC, a.start_time ASC";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Disponibilite> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    // ── SELECT FREE SLOTS ─────────────────────────────────────────────────────
    public List<Disponibilite> getDisponibilitesLibres(LocalDateTime dateDebut,
                                                        LocalDateTime dateFin) throws SQLException {
        String sql = "SELECT a.*, u.username AS doctor_nom, u.email AS doctor_email " +
                     "FROM availability a " +
                     "LEFT JOIN users u ON a.doctor_id = u.id " +
                     "WHERE a.is_online = 0 " +
                     "AND a.start_date >= ? ORDER BY a.start_date ASC, a.start_time ASC";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dateDebut.toLocalDate()));
            try (ResultSet rs = ps.executeQuery()) {
                List<Disponibilite> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    // ── SELECT BY DOCTOR + DATE ───────────────────────────────────────────────
    public List<Disponibilite> getDisponibilitesParDocteurEtDate(int doctorId,
                                                                   LocalDateTime date) throws SQLException {
        String sql = "SELECT a.*, u.username AS doctor_nom, u.email AS doctor_email " +
                     "FROM availability a " +
                     "LEFT JOIN users u ON a.doctor_id = u.id " +
                     "WHERE a.doctor_id=? AND a.start_date=? " +
                     "ORDER BY a.start_time ASC";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, Date.valueOf(date.toLocalDate()));
            try (ResultSet rs = ps.executeQuery()) {
                List<Disponibilite> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    // ── MARK UNAVAILABLE ─────────────────────────────────────────────────────
    public void marquerIndisponible(int id) throws SQLException {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE availability SET is_online=1 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────
    private Disponibilite map(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setId(rs.getInt("id"));
        d.setDoctorId(rs.getInt("doctor_id"));

        // Combine start_date + start_time → dateDebut
        try {
            Date sd = rs.getDate("start_date");
            Time st = rs.getTime("start_time");
            if (sd != null && st != null)
                d.setDateDebut(LocalDateTime.of(sd.toLocalDate(), st.toLocalTime()));
            else if (sd != null)
                d.setDateDebut(sd.toLocalDate().atStartOfDay());
        } catch (SQLException ignored) {}

        // Combine end_date + end_time → dateFin
        try {
            Date ed = rs.getDate("end_date");
            Time et = rs.getTime("end_time");
            if (ed != null && et != null)
                d.setDateFin(LocalDateTime.of(ed.toLocalDate(), et.toLocalTime()));
            else if (ed != null)
                d.setDateFin(ed.toLocalDate().atStartOfDay().plusHours(1));
        } catch (SQLException ignored) {}

        // is_online = true → not available in person
        try { d.setEstDisponible(!rs.getBoolean("is_online")); }
        catch (SQLException ignored) { d.setEstDisponible(true); }

        try { d.setDoctorNom(rs.getString("doctor_nom")); }
        catch (SQLException ignored) { d.setDoctorNom("Médecin " + d.getDoctorId()); }

        try { d.setDoctorEmail(rs.getString("doctor_email")); }
        catch (SQLException ignored) {}

        return d;
    }
}
