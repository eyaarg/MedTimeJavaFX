package esprit.fx.test;


import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {

    static ServiceUser service;
    static int userIdTest;
    static String testEmail;
    static String modifiedEmail;

    @BeforeAll
    static void setup() {
        service = new ServiceUser();
        String suffix = String.valueOf(System.currentTimeMillis());
        testEmail = "test." + suffix + "@gmail.com";
        modifiedEmail = "modified." + suffix + "@gmail.com";
    }


    @Test
    @Order(1)
    void testAjouterUser() throws SQLException {

        User user = new User();
        user.setEmail(testEmail);
        user.setUsername("testuser");
        user.setPassword("test123");
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);
        user.setPhoneNumber("12345678");
        user.setVerified(false);
        user.setFailedAttempts(0);

        service.ajouter(user);

        List<User> users = service.getAll();

        assertFalse(users.isEmpty());

        boolean exists = users.stream()
                .anyMatch(u -> u.getEmail().equals(testEmail));

        assertTrue(exists);

        userIdTest = users.stream()
                .filter(u -> testEmail.equals(u.getEmail()))
                .map(User::getId)
                .max(Integer::compareTo)
                .orElseThrow(() -> new AssertionError("Impossible de recuperer l'ID du user cree"));
        System.out.println("User ID: " + userIdTest);
    }


    @Test
    @Order(2)
    void testModifierUser() throws SQLException {

        User user = new User();
        user.setId(userIdTest);
        user.setEmail(modifiedEmail);
        user.setUsername("modifiedUser");
        user.setPassword("");
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);
        user.setPhoneNumber("87654321");
        user.setVerified(true);
        user.setFailedAttempts(0);

        service.modifier(user);

        List<User> users = service.getAll();

        boolean updated = users.stream()
                .anyMatch(u -> u.getId() == userIdTest && modifiedEmail.equals(u.getEmail()));

        assertTrue(updated);
    }


    @Test
    @Order(3)
    void testLoginUser() throws SQLException {

        User user = service.login("modifiedUser", "test123");

        assertNotNull(user);
        assertEquals("modifiedUser", user.getUsername());
    }


    @Test
    @Order(4)
    void testSupprimerUser() throws SQLException {

        service.supprimer(userIdTest);

        List<User> users = service.getAll();

        boolean exists = users.stream()
                .anyMatch(u -> u.getId() == userIdTest);

        assertFalse(exists);
    }


    @AfterAll
    static void cleanUp() throws SQLException {
        if (userIdTest > 0) {
            List<User> users = service.getAll();
            boolean stillExists = users.stream().anyMatch(u -> u.getId() == userIdTest);
            if (stillExists) {
                service.supprimer(userIdTest);
            }
        }
    }
}
