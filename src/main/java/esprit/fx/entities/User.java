package esprit.fx.entities;

import java.time.LocalDateTime;
import java.util.List;

public class User {
    private int id;
    private String email;
    private String username;
    private String password;
    private String plainPassword;
    private String requestedRole;
    private Object profilePhotoFile;

    private LocalDateTime createdAt;
    private boolean isActive;
    private String phoneNumber;
    private boolean isVerified;

    private String emailVerificationToken;
    private LocalDateTime emailVerificationTokenExpiresAt;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiresAt;
    private int failedAttempts;
    private List<Role> roles;
    public User() {
    }

    public User(int id, String email, String username, String password, Object profilePhotoFile, boolean isActive, String phoneNumber, boolean isVerified, String emailVerificationToken, LocalDateTime emailVerificationTokenExpiresAt, String passwordResetToken, LocalDateTime passwordResetTokenExpiresAt, int failedAttempts) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.profilePhotoFile = profilePhotoFile;
        this.isActive = isActive;
        this.phoneNumber = phoneNumber;
        this.isVerified = isVerified;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt;
        this.passwordResetToken = passwordResetToken;
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
        this.failedAttempts = failedAttempts;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlainPassword() {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public Object getProfilePhotoFile() {
        return profilePhotoFile;
    }

    public void setProfilePhotoFile(Object profilePhotoFile) {
        this.profilePhotoFile = profilePhotoFile;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public LocalDateTime getEmailVerificationTokenExpiresAt() {
        return emailVerificationTokenExpiresAt;
    }

    public void setEmailVerificationTokenExpiresAt(LocalDateTime emailVerificationTokenExpiresAt) {
        this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
    }

    public void setPasswordResetTokenExpiresAt(LocalDateTime passwordResetTokenExpiresAt) {
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", isVerified=" + isVerified +
                ", emailVerificationToken='" + emailVerificationToken + '\'' +
                ", emailVerificationTokenExpiresAt=" + emailVerificationTokenExpiresAt +
                ", passwordResetToken='" + passwordResetToken + '\'' +
                ", passwordResetTokenExpiresAt=" + passwordResetTokenExpiresAt +
                ", failedAttempts=" + failedAttempts +
                '}';
    }
}
