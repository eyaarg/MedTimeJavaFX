package esprit.fx.services;

import esprit.fx.entities.Article;
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
        String sql = "INSERT INTO article (titre, contenu, image, date_creation, nb_vues, statut, specialite_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, article.getTitre());
        ps.setString(2, article.getContenu());
        ps.setString(3, article.getImage());
        ps.setTimestamp(4, new Timestamp(new java.util.Date().getTime()));
        ps.setInt(5, article.getNbVues());
        ps.setString(6, article.getStatut());
        ps.setInt(7, article.getSpecialiteId());
        ps.executeUpdate();
        System.out.println("Article ajouté avec succès !");
    }

    @Override
    public void modifier(Article article) throws SQLException {
        String sql = "UPDATE article SET titre=?, contenu=?, image=?, statut=?, specialite_id=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, article.getTitre());
        ps.setString(2, article.getContenu());
        ps.setString(3, article.getImage());
        ps.setString(4, article.getStatut());
        ps.setInt(5, article.getSpecialiteId());
        ps.setInt(6, article.getId());
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
        String sql = "SELECT id, titre, contenu, image, date_creation, nb_vues, statut, specialite_id FROM article";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Article article = new Article();
            article.setId(rs.getInt("id"));
            article.setTitre(rs.getString("titre"));
            article.setContenu(rs.getString("contenu"));
            article.setDatePublication(rs.getTimestamp("date_creation"));
            article.setImage(rs.getString("image"));
            article.setNbVues(rs.getInt("nb_vues"));
            article.setStatut(rs.getString("statut"));
            article.setSpecialiteId(rs.getInt("specialite_id"));
            articles.add(article);
        }
        return articles;
    }

    @Override
    public Article afficherParId(int id) throws SQLException {
        Article article = null;
        String sql = "SELECT id, titre, contenu, image, date_creation, nb_vues, statut, specialite_id FROM article WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            article = new Article();
            article.setId(rs.getInt("id"));
            article.setTitre(rs.getString("titre"));
            article.setContenu(rs.getString("contenu"));
            article.setDatePublication(rs.getTimestamp("date_creation"));
            article.setImage(rs.getString("image"));
            article.setNbVues(rs.getInt("nb_vues"));
            article.setStatut(rs.getString("statut"));
            article.setSpecialiteId(rs.getInt("specialite_id"));
        }

        return article;
    }
}