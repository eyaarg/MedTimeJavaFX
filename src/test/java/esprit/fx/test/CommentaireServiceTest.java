package esprit.fx.test;

import esprit.fx.entities.Commentaire;
import esprit.fx.services.CommentaireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;
import java.util.List;

class CommentaireServiceTest {

    private CommentaireService commentaireService;

    @BeforeEach
    void setUp() {
        commentaireService = new CommentaireService();
    }

    @Test
    void testGetAllCommentaires() throws SQLException {
        List<Commentaire> commentaires = commentaireService.getAll();
        assertNotNull(commentaires);
    }

    @Test
    void testCommentaireFields() throws SQLException {
        List<Commentaire> commentaires = commentaireService.getAll();
        
        for (Commentaire commentaire : commentaires) {
            assertNotNull(commentaire.getContenu());
            assertNotNull(commentaire.getDateCommentaire());
        }
    }
}