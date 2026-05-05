package esprit.fx.services;

import esprit.fx.entities.Doctor_documents;
import esprit.fx.utils.MyDB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceDoctorDocument {

    private static final String UPLOADS_DIR = "uploads/doctor_documents/";

    public void uploadDocument(int doctorId, File selectedFile) throws SQLException, IOException {
        String storedName = UUID.randomUUID().toString();
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

    public void updateDocumentStatus(int documentId, String status) throws SQLException {
        String sql = "UPDATE doctor_documents SET status = ? WHERE id = ?";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, documentId);
            stmt.executeUpdate();
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
}
