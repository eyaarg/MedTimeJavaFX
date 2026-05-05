package esprit.fx.services;

import esprit.fx.entities.Article;
import esprit.fx.entities.Commentaire;
import esprit.fx.utils.MyDB;
import esprit.fx.utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService implements IService<Commentaire> {

    private Connection con;

    public CommentaireService() {
        con = MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(Commentaire commentaire) throws SQLException {
        int userId = Session.getCurrentUserId();
        if (userId == 0) {
            userId = getFirstUserId();
        }
        String sql = "INSERT INTO comment (contenu, date_creation, article_id, utilisateur_id) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, commentaire.getContenu());
        ps.setTimestamp(2, new Timestamp(commentaire.getDateCommentaire().getTime()));
        ps.setInt(3, commentaire.getArticle().getId());
        ps.setInt(4, userId);
        ps.executeUpdate();
        System.out.println("Commentaire ajouté avec succès !");
    }

    private int getFirstUserId() throws SQLException {
        String sql = "SELECT id FROM users LIMIT 1";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) {
            return rs.getInt("id");
        }
        return 1;
    }

    @Override
    public void modifier(Commentaire commentaire) throws SQLException {
        String sql = "UPDATE comment SET contenu=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, commentaire.getContenu());
        ps.setInt(2, commentaire.getId());
        ps.executeUpdate();
        System.out.println("Commentaire modifié avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM comment WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Commentaire supprimé avec succès !");
    }

    @Override
    public List<Commentaire> getAll() throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.*, a.id as art_id, a.titre as art_titre FROM comment c LEFT JOIN article a ON c.article_id = a.id";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setId(rs.getInt("id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setDateCommentaire(rs.getTimestamp("date_creation"));
            commentaire.setNbLikes(0);

            Article article = new Article();
            article.setId(rs.getInt("art_id"));
            article.setTitre(rs.getString("art_titre"));
            commentaire.setArticle(article);

            commentaires.add(commentaire);
        }
        return commentaires;
    }
        public Commentaire afficherParId(int id) throws SQLException {
        Commentaire commentaire = null;
        String sql = "SELECT * FROM comment WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            commentaire = new Commentaire();
            commentaire.setId(rs.getInt("id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setDateCommentaire(rs.getTimestamp("date_creation"));
            commentaire.setNbLikes(0);
        }
        return commentaire;
    }

    public List<Commentaire> getByArticle(int idArticle) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.id, c.contenu, c.date_creation, c.utilisateur_id, " +
                     "u.username FROM comment c " +
                     "LEFT JOIN users u ON c.utilisateur_id = u.id " +
                     "WHERE c.article_id = ? ORDER BY c.date_creation ASC";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idArticle);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setId(rs.getInt("id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setDateCommentaire(rs.getTimestamp("date_creation"));
            commentaire.setNbLikes(0);
            commentaire.setUtilisateurId(rs.getInt("utilisateur_id"));
            String username = rs.getString("username");
            commentaire.setUsername(username != null ? username : "Utilisateur");
            commentaires.add(commentaire);
        }
        return commentaires;
    }
}
