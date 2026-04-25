package esprit.fx.entities;

import java.time.LocalDateTime;

/**
 * Représente un message dans une session de chat.
 * role : "user" (patient) ou "assistant" (MediAssist IA).
 * Compatible avec la table chat_message partagée avec Symfony.
 */
public class ChatMessageArij {

    /** Rôle du message — ENUM stocké en VARCHAR dans MySQL. */
    public enum Role {
        user, assistant;

        public static Role from(String s) {
            if (s == null) return user;
            return switch (s.toLowerCase().trim()) {
                case "assistant" -> assistant;
                default          -> user;
            };
        }
    }

    private int           id;
    private int           sessionId;
    private Role          role;
    private String        content;
    private LocalDateTime createdAt;

    public ChatMessageArij() {}

    public ChatMessageArij(int sessionId, Role role, String content) {
        this.sessionId = sessionId;
        this.role      = role;
        this.content   = content;
        this.createdAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------------ //
    //  Getters / Setters                                                  //
    // ------------------------------------------------------------------ //
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public int getSessionId()                 { return sessionId; }
    public void setSessionId(int sessionId)   { this.sessionId = sessionId; }

    public Role getRole()                     { return role; }
    public void setRole(Role role)            { this.role = role; }

    public String getContent()                { return content; }
    public void setContent(String content)    { this.content = content; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isUser()      { return role == Role.user; }
    public boolean isAssistant() { return role == Role.assistant; }
}
