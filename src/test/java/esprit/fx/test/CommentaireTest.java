package esprit.fx.test;

import esprit.fx.entities.Article;
import esprit.fx.entities.Commentaire;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

class CommentaireTest {

    @Test
    void testCommentaireCreation() {
        Commentaire commentaire = new Commentaire();
        commentaire.setId(1);
        commentaire.setContenu("Test Commentaire");
        commentaire.setNbLikes(5);
        
        Article article = new Article();
        article.setId(1);
        article.setTitre("Test Article");
        commentaire.setArticle(article);
        
        assertEquals(1, commentaire.getId());
        assertEquals("Test Commentaire", commentaire.getContenu());
        assertEquals(5, commentaire.getNbLikes());
        assertNotNull(commentaire.getArticle());
        assertEquals("Test Article", commentaire.getArticle().getTitre());
    }

    @Test
    void testCommentaireToString() {
        Commentaire commentaire = new Commentaire();
        commentaire.setContenu("Test");
        commentaire.setNbLikes(0);
        
        String result = commentaire.toString();
        assertTrue(result.contains("Test"));
        assertTrue(result.contains("nbLikes=0"));
    }
}