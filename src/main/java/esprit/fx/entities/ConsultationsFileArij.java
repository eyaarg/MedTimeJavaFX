package esprit.fx.entities;

import java.time.LocalDateTime;

public class ConsultationsFileArij {
    private int id;
    private int consultationId;
    private String fileName;
    private String originalName;
    private String mimeType;
    private int fileSize;
    private String category;
    private LocalDateTime uploadedAt;

    public ConsultationsFileArij() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getConsultationId() { return consultationId; }
    public void setConsultationId(int consultationId) { this.consultationId = consultationId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public int getFileSize() { return fileSize; }
    public void setFileSize(int fileSize) { this.fileSize = fileSize; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
