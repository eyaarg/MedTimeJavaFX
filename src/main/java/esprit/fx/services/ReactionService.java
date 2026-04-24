package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gère les réactions emoji sur les articles (style Facebook).
 * Chaque utilisateur ne peut avoir qu'une seule réaction par article.
 * Rappeler upsertReaction avec le même type supprime la réaction (toggle).
 */
public class ReactionService {

    public static final String[] TYPES = {"LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY"};

    public static String toEmoji(String type) {
        if (type == null) return "";
        return switch (type) {
            case "LIKE"  -> "👍";
            case "LOVE"  -> "❤️";
            case "HAHA"  -> "😂";
            case "WOW"   -> "😮";
            case "SAD"   -> "😢";
            case "ANGRY" -> "😡";
            default      -> "👍";
        };
    }

    public static String toLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "LIKE"  -> "J'aime";
            case "LOVE"  -> "J'adore";
            case "HAHA"  -> "Haha";
            case "WOW"   -> "Wow";
            case "SAD"   -> "Triste";
            case "ANGRY" -> "Grrr";
            default      -> "J'aime";
        };
    }

    private Connection con() {
        return MyDB.getInstance().getConnection();
    }

    public void upsertReaction(int articleId, int userId, String type) throws SQLException {
        String current = getUserReaction(articleId, userId);
        if (type.equals(current)) {
            deleteReaction(articleId, userId);
        } else if (current == null) {
            String sql = "INSERT INTO article_reactions (article_id, user_id, reaction) VALUES (?, ?, ?)";
            PreparedStatement ps = con().prepareStatement(sql);
            ps.setInt(1, articleId);
            ps.setInt(2, userId);
            ps.setString(3, type);
            ps.executeUpdate();
        } else {
            String sql = "UPDATE article_reactions SET reaction=?, created_at=NOW() WHERE article_id=? AND user_id=?";
            PreparedStatement ps = con().prepareStatement(sql);
            ps.setString(1, type);
            ps.setInt(2, articleId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void deleteReaction(int articleId, int userId) throws SQLException {
        String sql = "DELETE FROM article_reactions WHERE article_id=? AND user_id=?";
        PreparedStatement ps = con().prepareStatement(sql);
        ps.setInt(1, articleId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    public String getUserReaction(int articleId, int userId) throws SQLException {
        String sql = "SELECT reaction FROM article_reactions WHERE article_id=? AND user_id=?";
        PreparedStatement ps = con().prepareStatement(sql);
        ps.setInt(1, articleId);
        ps.setInt(2, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("reaction");
        return null;
    }

    public Map<String, Integer> getCountsByArticle(int articleId) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT reaction, COUNT(*) as cnt FROM article_reactions " +
                     "WHERE article_id=? GROUP BY reaction ORDER BY cnt DESC";
        PreparedStatement ps = con().prepareStatement(sql);
        ps.setInt(1, articleId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            counts.put(rs.getString("reaction"), rs.getInt("cnt"));
        }
        return counts;
    }

    public int getTotalReactions(int articleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM article_reactions WHERE article_id=?";
        PreparedStatement ps = con().prepareStatement(sql);
        ps.setInt(1, articleId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }
}
