package esprit.fx.test;

import esprit.fx.entities.Produit;
import esprit.fx.entities.CategorieEnum;
import esprit.fx.services.ServiceProduit;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceProduitTest {

    private static Connection connection;
    private static ServiceProduit serviceProduit;
    private static Long testProduitId;

    @BeforeAll
    static void setUpBeforeClass() throws SQLException {
        // Connexion à la base de données de TEST
        connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mediplatform_test_test",  // Base de test
                "root",
                ""
        );

        // Créer la table si elle n'existe pas
        createTestTable();

        serviceProduit = new ServiceProduit(connection);
    }

    private static void createTestTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS product (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                price DOUBLE NOT NULL,
                stock INT DEFAULT 0,
                image VARCHAR(500),
                is_available BOOLEAN DEFAULT TRUE,
                is_prescription_required BOOLEAN DEFAULT FALSE,
                brand VARCHAR(100),
                expire_at DATE,
                categorie VARCHAR(50)
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Nettoyer la table avant chaque test
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM product");
            stmt.execute("ALTER TABLE product AUTO_INCREMENT = 1");
        }
    }


    // ==================== TEST CREATE ====================

    @Test
    @Order(1)
    @DisplayName("Test ajouter un produit")
    void testAjouter() throws SQLException {
        Produit p = new Produit(
                "Produit Test",
                "Description du produit test",
                CategorieEnum.MEDICAMENT,
                9.99,
                50,
                "test.jpg",
                false,
                true,
                "Marque Test",
                LocalDate.of(2026, 12, 31)
        );

        serviceProduit.ajouter(p);

        assertNotNull(p.getId());
        assertTrue(p.getId() > 0);

        // Vérifier que le produit a bien été ajouté
        List<Produit> produits = serviceProduit.getAll();
        assertTrue(produits.size() > 0);

        testProduitId = p.getId();
    }


    // ==================== TEST READ ====================

    @Test
    @Order(2)
    @DisplayName("Test récupérer tous les produits")
    void testGetAll() throws SQLException {
        // Ajouter des produits de test
        Produit p1 = new Produit("Produit 1", "Desc 1", CategorieEnum.MEDICAMENT, 10.0, 10, null, false, true, "Marque 1", null);
        Produit p2 = new Produit("Produit 2", "Desc 2", CategorieEnum.HYGIENE, 20.0, 20, null, false, true, "Marque 2", null);
        serviceProduit.ajouter(p1);
        serviceProduit.ajouter(p2);

        List<Produit> produits = serviceProduit.getAll();

        assertNotNull(produits);
        assertEquals(2, produits.size());
    }


    // ==================== TEST UPDATE ====================

    @Test
    @Order(3)
    @DisplayName("Test modifier un produit")
    void testModifier() throws SQLException {
        // Créer un produit
        Produit p = new Produit(
                "Avant Modification",
                "Description avant",
                CategorieEnum.MEDICAMENT,
                10.0,
                100,
                "image.jpg",
                false,
                true,
                "Marque",
                LocalDate.of(2025, 12, 31)
        );
        serviceProduit.ajouter(p);

        // Modifier le produit
        p.setNom("Après Modification");
        p.setPrix(15.0);
        p.setStock(200);
        p.setCategorie(CategorieEnum.PARAPHARMACIE);

        serviceProduit.modifier(p);

        // Vérifier que la liste contient le produit modifié
        List<Produit> produits = serviceProduit.getAll();
        Produit modified = produits.stream()
                .filter(prod -> prod.getId().equals(p.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(modified);
        assertEquals("Après Modification", modified.getNom());
        assertEquals(15.0, modified.getPrix());
        assertEquals(200, modified.getStock());
        assertEquals(CategorieEnum.PARAPHARMACIE, modified.getCategorie());
    }

    // ==================== TEST DELETE ====================

    @Test
    @Order(4)
    @DisplayName("Test supprimer un produit")
    void testSupprimer() throws SQLException {
        // Créer un produit
        Produit p = new Produit(
                "Produit à supprimer",
                "Description",
                CategorieEnum.MEDICAMENT,
                10.0,
                10,
                null,
                false,
                true,
                "Marque",
                null
        );
        serviceProduit.ajouter(p);

        int id = p.getId().intValue();

        // Supprimer le produit
        serviceProduit.supprimer(id);

        // Vérifier que le produit n'existe plus
        List<Produit> produits = serviceProduit.getAll();
        boolean exists = produits.stream().anyMatch(prod -> prod.getId().equals(p.getId()));
        assertFalse(exists);
    }


    @Test
    @Order(5)
    @DisplayName("Test produit indisponible")
    void testProduitIndisponible() throws SQLException {
        Produit p = new Produit(
                "Produit Rupture",
                "Temporairement indisponible",
                CategorieEnum.MATERIEL_MEDICAL,
                50.0,
                0,
                null,
                false,
                false,  // indisponible
                "Marque Y",
                null
        );

        serviceProduit.ajouter(p);

        List<Produit> produits = serviceProduit.getAll();
        Produit saved = produits.stream()
                .filter(prod -> prod.getNom().equals("Produit Rupture"))
                .findFirst()
                .orElse(null);

        assertNotNull(saved);
        assertFalse(saved.getDisponible());
        assertEquals(0, saved.getStock());
    }


}