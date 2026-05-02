package esprit.fx.test;

import esprit.fx.services.ServiceUser;
import esprit.fx.entities.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LoginServiceTest {

    private final ServiceUser serviceUser = new ServiceUser();

    @Test
    public void testLoginWithValidCredentials() {
        try {
            User user = serviceUser.login("validUsername", "validPassword");
            assertNotNull(user, "L'utilisateur ne devrait pas être null pour des identifiants valides.");
        } catch (Exception e) {
            fail("Exception inattendue : " + e.getMessage());
        }
    }

    @Test
    public void testLoginWithInvalidCredentials() {
        try {
            User user = serviceUser.login("invalidUsername", "invalidPassword");
            assertNull(user, "L'utilisateur devrait être null pour des identifiants invalides.");
        } catch (Exception e) {
            fail("Exception inattendue : " + e.getMessage());
        }
    }

    @Test
    public void testLoginWithLockedAccount() {
        try {
            User user = serviceUser.login("lockedUser", "password");
            assertNull(user, "L'utilisateur devrait être null pour un compte verrouillé.");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("verrouillé"), "Le message d'erreur devrait indiquer que le compte est verrouillé.");
        }
    }

    @Test
    public void testLoginWithInactiveAccount() {
        try {
            User user = serviceUser.login("inactiveUser", "password");
            assertNull(user, "L'utilisateur devrait être null pour un compte inactif.");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("inactif"), "Le message d'erreur devrait indiquer que le compte est inactif.");
        }
    }
}
