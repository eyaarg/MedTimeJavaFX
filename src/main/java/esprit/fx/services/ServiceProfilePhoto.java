package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service séparé pour la gestion des photos de profil.
 * Stockage local : uploads/profile_photos/{userId}.{ext}
 */
public class ServiceProfilePhoto {

    private static final String UPLOADS_DIR = "uploads/profile_photos/";

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    /**
     * Upload une photo de profil pour un utilisateur.
     * Copie le fichier dans uploads/profile_photos/ et met à jour la DB.
     *
     * @param userId       id de l'utilisateur
     * @param selectedFile fichier image sélectionné (jpg/png)
     * @return chemin relatif du fichier stocké
     */
    public String uploadProfilePhoto(int userId, File selectedFile) throws SQLException, IOException {
        // Créer le dossier si nécessaire
        Path dir = Paths.get(UPLOADS_DIR);
        Files.createDirectories(dir);

        // Déterminer l'extension
        String originalName = selectedFile.getName();
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";

        // Nom de fichier : userId.ext (écrase l'ancienne photo)
        String storedName = userId + ext;
        Path destination = dir.resolve(storedName);

        Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = UPLOADS_DIR + storedName;

        // Sauvegarder le chemin dans la colonne profile_photo de la table users
        savePhotoPath(userId, relativePath);

        return relativePath;
    }

    /**
     * Récupère le chemin de la photo de profil depuis la DB.
     * Retourne null si aucune photo n'est définie.
     */
    public String getPhotoPath(int userId) throws SQLException {
        String sql = "SELECT profile_photo FROM users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("profile_photo");
                }
            }
        }
        return null;
    }

    /**
     * Met à jour le chemin de la photo dans la DB.
     */
    private void savePhotoPath(int userId, String path) throws SQLException {
        String sql = "UPDATE users SET profile_photo = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Retourne le fichier image si il existe sur le disque, null sinon.
     */
    public File getPhotoFile(int userId) throws SQLException {
        String path = getPhotoPath(userId);
        if (path == null || path.isBlank()) return null;
        File f = new File(path);
        return f.exists() ? f : null;
    }
}
