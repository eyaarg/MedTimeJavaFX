package esprit.fx.test;

import esprit.fx.entities.CategorieEnum;
import esprit.fx.entities.Produit;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProduitTest {

    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit();
    }



    @Test
    @Order(1)
    @DisplayName("Test setter/getter Nom")
    void testSetGetNom() {
        produit.setNom("Aspirine");
        assertEquals("Aspirine", produit.getNom());
    }

    @Test
    @Order(2)
    @DisplayName("Test setter/getter Prix")
    void testSetGetPrix() {
        produit.setPrix(12.99);
        assertEquals(12.99, produit.getPrix());
    }

    @Test
    @Order(3)
    @DisplayName("Test setter/getter Stock")
    void testSetGetStock() {
        produit.setStock(75);
        assertEquals(75, produit.getStock());
    }

    @Test
    @Order(4)
    @DisplayName("Test setter/getter Catégorie")
    void testSetGetCategorie() {
        produit.setCategorie(CategorieEnum.PARAPHARMACIE);
        assertEquals(CategorieEnum.PARAPHARMACIE, produit.getCategorie());
    }

    @Test
    @Order(5)
    @DisplayName("Test setter/getter DateExpiration")
    void testSetGetDateExpiration() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        produit.setDateExpiration(date);
        assertEquals(date, produit.getDateExpiration());
    }

    @Test
    @Order(6)
    @DisplayName("Test valeurs par défaut")
    void testValeursParDefaut() {
        Produit p = new Produit();
        assertTrue(p.getDisponible());
        assertEquals(0, p.getStock());
    }

    @Test
    @Order(7)
    @DisplayName("Test toString ne retourne pas null")
    void testToString() {
        produit.setNom("Test");
        String result = produit.toString();
        assertNotNull(result);
        assertTrue(result.contains("Test"));
    }
}