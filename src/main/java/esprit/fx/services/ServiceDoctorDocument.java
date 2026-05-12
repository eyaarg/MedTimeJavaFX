package esprit.fx.services;

import esprit.fx.entities.Doctor_documents;
import esprit.fx.utils.MyDB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceDoctorDocument {

    private static final String UPLOADS_DIR = "uploads/doctor_documents/";

    public void uploadDocument(int doctorId, File selectedFile) throws SQLException, IOException {
        ensureCompatibleSchema();

        String storedName = UUID.randomUUID() + getFileExtension(selectedFile.getName());
        Path doctorDir = Paths.get(UPLOADS_DIR, String.valueOf(doctorId));
        Files.createDirectories(doctorDir);

        Path destination = doctorDir.resolve(storedName);
        try (FileOutputStream out = new FileOutputStream(destination.toFile())) {
            Files.copy(selectedFile.toPath(), out);
        }

        try (Connection conn = MyDB.getInstance().getConnection()) {
            String sql = "INSERT INTO doctor_documents (original_name, stored_name, folder_name, mime_type, size, status, uploaded_at, doctor_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, selectedFile.getName());
                stmt.setString(2, storedName);
                stmt.setString(3, doctorDir.toString());
                stmt.setString(4, Files.probeContentType(selectedFile.toPath()));
                stmt.setLong(5, Files.size(selectedFile.toPath()));
                stmt.setString(6, "pending");
                stmt.setInt(7, doctorId);
                stmt.executeUpdate();
            }
        }
    }

    public Doctor_documents getLatestDocumentByDoctorId(int doctorId) throws SQLException {
        String sql = "SELECT * FROM doctor_documents WHERE doctor_id = ? ORDER BY uploaded_at DESC LIMIT 1";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDoctorDocument(rs);
                }
            }
        }
        return null;
    }

    public List<Doctor_documents> getAllDocumentsByDoctorId(int doctorId) throws SQLException {
        List<Doctor_documents> documents = new ArrayList<>();
        String sql = "SELECT * FROM doctor_documents WHERE doctor_id = ?";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDoctorDocument(rs));
                }
            }
        }
        return documents;
    }

    public File getDocumentFile(Doctor_documents doc) {
        return Paths.get(doc.getFolder_name(), doc.getStored_name()).toFile();
    }

    public File getOpenableDocumentFile(Doctor_documents doc) throws IOException {
        if (doc == null || doc.getFolder_name() == null || doc.getStored_name() == null) {
            throw new FileNotFoundException("Document introuvable.");
        }

        String folderName = doc.getFolder_name();
        String storedName = doc.getStored_name();

        // List of base paths to try (JavaFX uploads, Symfony uploads, absolute)
        String[] basePaths = {
            "uploads/doctor_documents/",                                    // JavaFX DoctorRegistrationController
            "C:/xampp/htdocs/MedTime/public/uploads/doctors/",             // Symfony web upload
            "C:/xampp/htdocs/MedTime/uploads/doctors/",                    // Symfony alt path
            ""                                                              // folder_name is already absolute
        };

        for (String base : basePaths) {
            Path candidate = Paths.get(base + folderName, storedName).toAbsolutePath().normalize();
            if (java.nio.file.Files.exists(candidate)) {
                // Ensure .pdf extension
                if (!candidate.getFileName().toString().toLowerCase().endsWith(".pdf")) {
                    Path pdfPath = candidate.resolveSibling(candidate.getFileName() + ".pdf");
                    java.nio.file.Files.copy(candidate, pdfPath, StandardCopyOption.REPLACE_EXISTING);
                    return pdfPath.toFile();
                }
                return candidate.toFile();
            }
        }

        // Also try folder_name as absolute path directly
        Path direct = Paths.get(folderName, storedName);
        if (java.nio.file.Files.exists(direct)) {
            return direct.toFile();
        }

        throw new FileNotFoundException(
            "Fichier PDF introuvable. Chemins essayés:\n" +
            "- uploads/doctor_documents/" + folderName + "/" + storedName + "\n" +
            "- C:/xampp/htdocs/MedTime/public/uploads/doctors/" + folderName + "/" + storedName
        );
    }

    public void updateDocumentStatus(int documentId, String status) throws SQLException {
        String sql = "UPDATE doctor_documents SET status = ? WHERE id = ?";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, documentId);
            stmt.executeUpdate();
        }
    }

    private void ensureCompatibleSchema() throws SQLException {
        try (Connection conn = MyDB.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS doctor_documents (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "original_name VARCHAR(255) NOT NULL, " +
                            "stored_name VARCHAR(255) NOT NULL, " +
                            "folder_name VARCHAR(500) NOT NULL, " +
                            "mime_type VARCHAR(120) NULL, " +
                            "size BIGINT NOT NULL DEFAULT 0, " +
                            "status VARCHAR(30) NOT NULL DEFAULT 'pending', " +
                            "uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "doctor_id INT NOT NULL, " +
                            "INDEX idx_doctor_documents_doctor_id (doctor_id), " +
                            "CONSTRAINT fk_doctor_documents_doctor FOREIGN KEY (doctor_id) " +
                            "REFERENCES doctors(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            addColumnIfMissing(conn, "doctor_documents", "original_name", "VARCHAR(255) NULL");
            addColumnIfMissing(conn, "doctor_documents", "stored_name", "VARCHAR(255) NULL");
            addColumnIfMissing(conn, "doctor_documents", "folder_name", "VARCHAR(500) NULL");
            addColumnIfMissing(conn, "doctor_documents", "mime_type", "VARCHAR(120) NULL");
            addColumnIfMissing(conn, "doctor_documents", "size", "BIGINT NOT NULL DEFAULT 0");
            addColumnIfMissing(conn, "doctor_documents", "status", "VARCHAR(30) NOT NULL DEFAULT 'pending'");
            addColumnIfMissing(conn, "doctor_documents", "uploaded_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfMissing(conn, "doctor_documents", "doctor_id", "INT NULL");
        }
    }

    private void addColumnIfMissing(Connection conn, String tableName, String columnName, String definition)
            throws SQLException {
        if (!columnExists(conn, tableName, columnName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
            }
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private Doctor_documents mapResultSetToDoctorDocument(ResultSet rs) throws SQLException {
        Doctor_documents doc = new Doctor_documents();
        doc.setId(rs.getInt("id"));
        doc.setOriginal_name(rs.getString("original_name"));
        doc.setStored_name(rs.getString("stored_name"));
        doc.setFolder_name(rs.getString("folder_name"));
        doc.setMime_type(rs.getString("mime_type"));
        doc.setSize((int) rs.getLong("size")); // Cast long to int
        doc.setStatus(rs.getString("status"));
        doc.setUploaded_at(rs.getTimestamp("uploaded_at").toLocalDateTime()); // Convert Timestamp to LocalDateTime
        doc.setDoctor_id(rs.getInt("doctor_id"));
        return doc;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) return "";
        return fileName.substring(dotIndex).toLowerCase();
    }
}
