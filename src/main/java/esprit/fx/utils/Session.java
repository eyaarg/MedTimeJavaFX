package esprit.fx.utils;

import esprit.fx.entities.User;

public class Session {
    private static User currentUser;
    
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    public static User getCurrentUser() {
        return currentUser;
    }
    
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }
    
    public static void logout() {
        currentUser = null;
    }
    
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}