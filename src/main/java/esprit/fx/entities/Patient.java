package esprit.fx.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient extends User{
    private int id;
    private int userId;
    private String region;
    private String allergies;
    private String medicalHistory;
    private int previousCancellations;
    private LocalDate birthDate;
    private LocalDateTime createdAt;

    public Patient() {
    }

    public Patient(int id, String email, String username, String password, Object profilePhotoFile,
                   boolean isActive, String phoneNumber, boolean isVerified, String emailVerificationToken,
                   LocalDateTime emailVerificationTokenExpiresAt, String passwordResetToken,
                   LocalDateTime passwordResetTokenExpiresAt, int failedAttempts, int id1, int userId,
                   String region, String allergies, String medicalHistory,
                   int previousCancellations, LocalDate birthDate, LocalDateTime createdAt) {
        super(id, email, username, password, profilePhotoFile, isActive, phoneNumber, isVerified,
                emailVerificationToken, emailVerificationTokenExpiresAt, passwordResetToken,
                passwordResetTokenExpiresAt, failedAttempts);
        this.id = id1;
        this.userId = userId;
        this.region = region;
        this.allergies = allergies;
        this.medicalHistory = medicalHistory;
        this.previousCancellations = previousCancellations;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public int getPreviousCancellations() {
        return previousCancellations;
    }

    public void setPreviousCancellations(int previousCancellations) {
        this.previousCancellations = previousCancellations;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
