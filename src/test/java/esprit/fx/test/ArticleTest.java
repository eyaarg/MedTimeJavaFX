package esprit.fx.test;

import esprit.fx.entities.Article;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArticleTest {

    @Test
    void testArticleCreation() {
        Article article = new Article();
        article.setId(1);
        article.setTitre("Test Article");
        article.setContenu("Test Content");
        article.setStatut("publié");
        article.setSpecialiteId(1);
        article.setNbVues(0);
        
        assertEquals(1, article.getId());
        assertEquals("Test Article", article.getTitre());
        assertEquals("Test Content", article.getContenu());
        assertEquals("publié", article.getStatut());
        assertEquals(1, article.getSpecialiteId());
        assertEquals(0, article.getNbVues());
    }

    @Test
    void testArticleToString() {
        Article article = new Article();
        article.setTitre("Test Article");
        
        assertEquals("Test Article", article.toString());
    }

    @Test
    void testArticleSpecialiteId() {
        Article article = new Article();
        article.setSpecialiteId(5);
        
        assertEquals(5, article.getSpecialiteId());
    }
}
