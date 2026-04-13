package esprit.fx.services;

import esprit.fx.entities.Article;
import esprit.fx.entities.Categorie;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ArticleService implements IService<Article> {

    private Connection con;

    public ArticleService() {
        con = MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(Article article) throws SQLException {
        String sql = "INSERT INTO article (titre, contenu, date_publication, image, nb_likes, nb_vues, tags, statut, id_categorie) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, article.getTitre());
        ps.setString(2, article.getContenu());
        ps.setDate(3, new java.sql.Date(article.getDatePublication().getTime()));
        ps.setString(4, article.getImage());
        ps.setInt(5, article.getNbLikes());
        ps.setInt(6, article.getNbVues());
        ps.setString(7, article.getTags());
        ps.setString(8, article.getStatut());
        ps.setInt(9, article.getCategorie().getId());
        ps.executeUpdate();
        System.out.println("Article ajouté avec succès !");
    }

    @Override
    public void modifier(Article article) throws SQLException {
        String sql = "UPDATE article SET titre=?, contenu=?, image=?, tags=?, statut=?, id_categorie=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, article.getTitre());
        ps.setString(2, article.getContenu());
        ps.setString(3, article.getImage());
        ps.setString(4, article.getTags());
        ps.setString(5, article.getStatut());
        ps.setInt(6, article.getCategorie().getId());
        ps.setInt(7, article.getId());
        ps.executeUpdate();
        System.out.println("Article modifié avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM article WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Article supprimé avec succès !");
    }

    @Override
    public List<Article> getAll() throws SQLException {
        List<Article> articles = new ArrayList<>();
        String sql = "SELECT a.*, c.id as cat_id, c.nom as cat_nom FROM article a LEFT JOIN categorie c ON a.id_categorie = c.id";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Article article = new Article();
            article.setId(rs.getInt("id"));
            article.setTitre(rs.getString("titre"));
            article.setContenu(rs.getString("contenu"));
            article.setDatePublication(rs.getDate("date_publication"));
            article.setImage(rs.getString("image"));
            article.setNbLikes(rs.getInt("nb_likes"));
            article.setNbVues(rs.getInt("nb_vues"));
            article.setTags(rs.getString("tags"));
            article.setStatut(rs.getString("statut"));

            Categorie categorie = new Categorie();
            categorie.setId(rs.getInt("cat_id"));
            categorie.setNom(rs.getString("cat_nom"));
            article.setCategorie(categorie);

            articles.add(article);
        }
        return articles;
    }

    @Override
    public Article afficherParId(int id) throws SQLException {
        Article article = null;
        String sql = "SELECT a.*, c.id as cat_id, c.nom as cat_nom FROM article a LEFT JOIN categorie c ON a.id_categorie = c.id WHERE a.id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            article = new Article();
            article.setId(rs.getInt("id"));
            article.setTitre(rs.getString("titre"));
            article.setContenu(rs.getString("contenu"));
            article.setDatePublication(rs.getDate("date_publication"));
            article.setImage(rs.getString("image"));
            article.setNbLikes(rs.getInt("nb_likes"));
            article.setNbVues(rs.getInt("nb_vues"));
            article.setTags(rs.getString("tags"));
            article.setStatut(rs.getString("statut"));

            Categorie categorie = new Categorie();
            categorie.setId(rs.getInt("cat_id"));
            categorie.setNom(rs.getString("cat_nom"));
            article.setCategorie(categorie);
        }
        return article;
    }
}