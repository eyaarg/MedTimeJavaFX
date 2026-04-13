package esprit.fx.services;

import esprit.fx.entities.Article;
import esprit.fx.entities.Commentaire;
import esprit.fx.utils.MyDB;

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
        String sql = "INSERT INTO commentaire (contenu, date_commentaire, nb_likes, id_article) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, commentaire.getContenu());
        ps.setDate(2, new java.sql.Date(commentaire.getDateCommentaire().getTime()));
        ps.setInt(3, commentaire.getNbLikes());
        ps.setInt(4, commentaire.getArticle().getId());
        ps.executeUpdate();
        System.out.println("Commentaire ajouté avec succès !");
    }

    @Override
    public void modifier(Commentaire commentaire) throws SQLException {
        String sql = "UPDATE commentaire SET contenu=?, nb_likes=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, commentaire.getContenu());
        ps.setInt(2, commentaire.getNbLikes());
        ps.setInt(3, commentaire.getId());
        ps.executeUpdate();
        System.out.println("Commentaire modifié avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM commentaire WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Commentaire supprimé avec succès !");
    }

    @Override
    public List<Commentaire> getAll() throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.*, a.id as art_id, a.titre as art_titre FROM commentaire c LEFT JOIN article a ON c.id_article = a.id";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setId(rs.getInt("id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setDateCommentaire(rs.getDate("date_commentaire"));
            commentaire.setNbLikes(rs.getInt("nb_likes"));

            Article article = new Article();
            article.setId(rs.getInt("art_id"));
            article.setTitre(rs.getString("art_titre"));
            commentaire.setArticle(article);

            commentaires.add(commentaire);
        }
        return commentaires;
    }

    @Override
    public Commentaire afficherParId(int id) throws SQLException {
        Commentaire commentaire = null;
        String sql = "SELECT * FROM commentaire WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            commentaire = new Commentaire();
            commentaire.setId(rs.getInt("id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setDateCommentaire(rs.getDate("date_commentaire"));
            commentaire.setNbLikes(rs.getInt("nb_likes"));
        }
        return commentaire;
    }

    public List<Commentaire> getByArticle(int idArticle) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE id_article=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idArticle);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setId(rs.getInt("id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setDateCommentaire(rs.getDate("date_commentaire"));
            commentaire.setNbLikes(rs.getInt("nb_likes"));
            commentaires.add(commentaire);
        }
        return commentaires;
    }
}