package esprit.fx.entities;

import java.time.LocalDateTime;

public class Doctor extends User {
        private int id;
        private int userId;
        private String licenseCode;
        private boolean isCertified;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String adresse;

    public Doctor() {
    }

    public Doctor(int id, String email, String username, String password, Object profilePhotoFile,
                  boolean isActive, String phoneNumber, boolean isVerified, String emailVerificationToken,
                  LocalDateTime emailVerificationTokenExpiresAt, String passwordResetToken,
                  LocalDateTime passwordResetTokenExpiresAt, int failedAttempts, int id1, int userId,
                  String licenseCode, boolean isCertified, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, email, username, password, profilePhotoFile, isActive, phoneNumber, isVerified,
                emailVerificationToken, emailVerificationTokenExpiresAt, passwordResetToken,
                passwordResetTokenExpiresAt, failedAttempts);
        this.id = id1;
        this.userId = userId;
        this.licenseCode = licenseCode;
        this.isCertified = isCertified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getLicenseCode() {
            return licenseCode;
        }

        public void setLicenseCode(String licenseCode) {
            this.licenseCode = licenseCode;
        }

        public boolean isCertified() {
            return isCertified;
        }

        public void setCertified(boolean certified) {
            isCertified = certified;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getAdresse() {
            return adresse;
        }

        public void setAdresse(String adresse) {
            this.adresse = adresse;
        }

}
