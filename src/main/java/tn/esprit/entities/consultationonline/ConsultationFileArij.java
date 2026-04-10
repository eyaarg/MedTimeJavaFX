package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;

public class ConsultationFileArij {
    private int id;
    private int consultationId;
    private String fileName;
    private String originalName;
    private String mimeType;
    private int fileSize;
    private String category;
    private LocalDateTime uploadedAt;

    public ConsultationFileArij() {
    }

    public ConsultationFileArij(int id, int consultationId, String fileName, String originalName, String mimeType,
                                int fileSize, String category, LocalDateTime uploadedAt) {
        this.id = id;
        this.consultationId = consultationId;
        this.fileName = fileName;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.category = category;
        this.uploadedAt = uploadedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(int consultationId) {
        this.consultationId = consultationId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "ConsultationFileArij{" +
                "id=" + id +
                ", consultationId=" + consultationId +
                ", fileName='" + fileName + '\'' +
                ", originalName='" + originalName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileSize=" + fileSize +
                ", category='" + category + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
