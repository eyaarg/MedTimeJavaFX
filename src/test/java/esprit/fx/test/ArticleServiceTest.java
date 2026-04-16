package esprit.fx.test;

import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;
import java.util.List;

class ArticleServiceTest {

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService();
    }

    @Test
    void testGetAllArticles() throws SQLException {
        List<Article> articles = articleService.getAll();
        assertNotNull(articles);
    }

    @Test
    void testArticleFields() throws SQLException {
        List<Article> articles = articleService.getAll();
        
        if (!articles.isEmpty()) {
            Article article = articles.get(0);
            assertNotNull(article.getTitre());
            assertNotNull(article.getStatut());
        }
    }

    @Test
    void testArticleSpecialiteId() throws SQLException {
        List<Article> articles = articleService.getAll();
        
        for (Article article : articles) {
            assertTrue(article.getSpecialiteId() >= 0);
        }
    }
}