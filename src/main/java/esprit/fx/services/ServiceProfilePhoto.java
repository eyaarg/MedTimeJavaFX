package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gestion des photos de profil partagées entre JavaFX et Symfony.
 *
 * Les deux applications utilisent le même répertoire physique :
 *   {SYMFONY_PUBLIC}/uploads/users/{username}/pfp.{ext}
 *
 * Et le même format de chemin en base de données :
 *   /uploads/users/{username}/pfp.{ext}
 *
 * Ainsi, une photo uploadée dans l'une des apps est immédiatement
 * visible dans l'autre.
 */
public class ServiceProfilePhoto {

    /**
     * Répertoire public de Symfony — à adapter si l'installation diffère.
     * Ce chemin est résolu en absolu pour garantir la portabilité.
     */
    private static final String SYMFONY_PUBLIC =
            "C:/xampp/htdocs/MedTime/public";

    private java.sql.Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Copie {@code selectedFile} dans le répertoire Symfony partagé et
     * met à jour la colonne {@code profile_photo} de la table {@code users}.
     *
     * @param userId       identifiant de l'utilisateur
     * @param username     nom d'utilisateur (utilisé pour le chemin)
     * @param selectedFile fichier image choisi par l'utilisateur
     * @return chemin relatif stocké en DB (ex: {@code /uploads/users/alice/pfp.jpg})
     */
    public String uploadProfilePhoto(int userId, String username, File selectedFile)
            throws SQLException, IOException {

        String safeUsername = sanitize(username);

        // Extension du fichier source
        String originalName = selectedFile.getName();
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase()
                : ".jpg";

        // Répertoire cible : {SYMFONY_PUBLIC}/uploads/users/{username}/
        Path userDir = Paths.get(SYMFONY_PUBLIC, "uploads", "users", safeUsername);
        Files.createDirectories(userDir);

        // Fichier cible : pfp.{ext}  (écrase l'ancienne photo)
        String filename = "pfp" + ext;
        Path destination = userDir.resolve(filename);
        Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        // Chemin relatif stocké en DB — même format que Symfony
        String dbPath = "/uploads/users/" + safeUsername + "/" + filename;
        savePhotoPath(userId, dbPath);

        return dbPath;
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    /**
     * Retourne le chemin DB de la photo de profil, ou {@code null} si absent.
     */
    public String getPhotoPath(int userId) throws SQLException {
        String sql = "SELECT profile_photo FROM users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("profile_photo");
            }
        }
        return null;
    }

    /**
     * Retourne le {@link File} correspondant à la photo de profil si elle
     * existe sur le disque, {@code null} sinon.
     *
     * Gère les deux formats de chemin :
     * <ul>
     *   <li>Nouveau (partagé) : {@code /uploads/users/{username}/pfp.jpg}</li>
     *   <li>Ancien (JavaFX)   : {@code uploads/profile_photos/{userId}.jpg}</li>
     * </ul>
     */
    public File getPhotoFile(int userId) throws SQLException {
        String path = getPhotoPath(userId);
        if (path == null || path.isBlank()) return null;

        // Nouveau format : chemin relatif à SYMFONY_PUBLIC
        if (path.startsWith("/uploads/users/")) {
            File f = new File(SYMFONY_PUBLIC + path);
            return f.exists() ? f : null;
        }

        // Ancien format : chemin relatif au répertoire de travail JavaFX
        File f = new File(path);
        return f.exists() ? f : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void savePhotoPath(int userId, String path) throws SQLException {
        String sql = "UPDATE users SET profile_photo = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Sanitise un nom d'utilisateur pour l'utiliser comme nom de répertoire
     * (même logique que Symfony : remplace tout caractère non alphanumérique
     * par un underscore).
     */
    private static String sanitize(String username) {
        if (username == null || username.isBlank()) return "unknown";
        return username.trim().replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
