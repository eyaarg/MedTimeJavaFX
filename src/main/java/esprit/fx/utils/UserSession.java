package esprit.fx.utils;

import esprit.fx.entities.User;

public final class UserSession {
    private static User currentUser;
    private static String currentRole = "PATIENT";

    private UserSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(String role) {
        currentRole = role == null || role.isBlank() ? "PATIENT" : role.toUpperCase();
    }

    public static boolean isAdmin() {
        return currentRole != null && currentRole.toUpperCase().contains("ADMIN");
    }

    public static void clear() {
        currentUser = null;
        currentRole = "PATIENT";
    }
}

