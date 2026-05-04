package esprit.fx.services;

import esprit.fx.entities.ChatMessageArij;
import esprit.fx.entities.ChatMessageArij.Role;
import esprit.fx.entities.ChatSessionArij;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service JDBC pour la gestion des sessions et messages de chat MediAssist.
 * Tables : chat_session, chat_message — partagées avec Symfony.
 */
public class ServiceChatArij {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ================================================================== //
    //  SESSIONS                                                           //
    // ================================================================== //

    /**
     * Charge toutes les sessions d'un patient, triées par date décroissante.
     */
    public List<ChatSessionArij> getSessionsByPatient(int patientId) {
        List<ChatSessionArij> list = new ArrayList<>();
        if (patientId <= 0) return list;

        String sql = "SELECT * FROM chat_session WHERE patient_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapSession(rs));
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] getSessionsByPatient: " + e.getMessage());
        }
        return list;
    }

    /**
     * Crée une nouvelle session et retourne l'objet avec son id généré.
     */
    public ChatSessionArij createSession(int patientId, String title) {
        ChatSessionArij session = new ChatSessionArij(patientId, title);
        String sql = "INSERT INTO chat_session (patient_id, title, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setString(2, title);
            ps.setTimestamp(3, ts(session.getCreatedAt()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) session.setId(keys.getInt(1));
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] createSession: " + e.getMessage());
        }
        return session;
    }

    /**
     * Met à jour le titre d'une session (ex : premier message tronqué).
     */
    public void updateSessionTitle(int sessionId, String title) {
        String sql = "UPDATE chat_session SET title = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] updateSessionTitle: " + e.getMessage());
        }
    }

    /**
     * Supprime une session et tous ses messages (CASCADE ou suppression manuelle).
     */
    public void deleteSession(int sessionId) {
        // Supprimer d'abord les messages (au cas où pas de FK CASCADE)
        deleteMessagesBySession(sessionId);
        String sql = "DELETE FROM chat_session WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] deleteSession: " + e.getMessage());
        }
    }

    // ================================================================== //
    //  MESSAGES                                                           //
    // ================================================================== //

    /**
     * Charge tous les messages d'une session, triés chronologiquement.
     */
    public List<ChatMessageArij> getMessagesBySession(int sessionId) {
        List<ChatMessageArij> list = new ArrayList<>();
        if (sessionId <= 0) return list;

        String sql = "SELECT * FROM chat_message WHERE session_id = ? ORDER BY created_at ASC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapMessage(rs));
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] getMessagesBySession: " + e.getMessage());
        }
        return list;
    }

    /**
     * Sauvegarde un message et retourne l'objet avec son id généré.
     */
    public ChatMessageArij saveMessage(int sessionId, Role role, String content) {
        ChatMessageArij msg = new ChatMessageArij(sessionId, role, content);
        String sql = "INSERT INTO chat_message (session_id, role, content, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sessionId);
            ps.setString(2, role.name());
            ps.setString(3, content);
            ps.setTimestamp(4, ts(msg.getCreatedAt()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) msg.setId(keys.getInt(1));
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] saveMessage: " + e.getMessage());
        }
        return msg;
    }

    /**
     * Supprime tous les messages d'une session.
     */
    public void deleteMessagesBySession(int sessionId) {
        String sql = "DELETE FROM chat_message WHERE session_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceChatArij] deleteMessagesBySession: " + e.getMessage());
        }
    }

    // ================================================================== //
    //  Mapping                                                            //
    // ================================================================== //

    private ChatSessionArij mapSession(ResultSet rs) throws SQLException {
        ChatSessionArij s = new ChatSessionArij();
        s.setId(rs.getInt("id"));
        s.setPatientId(rs.getInt("patient_id"));
        s.setTitle(rs.getString("title"));
        Timestamp ca = rs.getTimestamp("created_at");
        s.setCreatedAt(ca != null ? ca.toLocalDateTime() : LocalDateTime.now());
        return s;
    }

    private ChatMessageArij mapMessage(ResultSet rs) throws SQLException {
        ChatMessageArij m = new ChatMessageArij();
        m.setId(rs.getInt("id"));
        m.setSessionId(rs.getInt("session_id"));
        m.setRole(Role.from(rs.getString("role")));
        m.setContent(rs.getString("content"));
        Timestamp ca = rs.getTimestamp("created_at");
        m.setCreatedAt(ca != null ? ca.toLocalDateTime() : LocalDateTime.now());
        return m;
    }

    private Timestamp ts(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }
}
