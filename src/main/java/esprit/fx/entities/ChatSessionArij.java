package esprit.fx.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Représente une session de chat entre un patient et MediAssist.
 * Compatible avec la table chat_session partagée avec Symfony.
 */
public class ChatSessionArij {

    private int id;
    private int patientId;
    private String title;          // titre court affiché dans la ListView
    private LocalDateTime createdAt;

    public ChatSessionArij() {}

    public ChatSessionArij(int patientId, String title) {
        this.patientId = patientId;
        this.title     = title;
        this.createdAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------------ //
    //  Getters / Setters                                                  //
    // ------------------------------------------------------------------ //
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public int getPatientId()                 { return patientId; }
    public void setPatientId(int patientId)   { this.patientId = patientId; }

    public String getTitle()                  { return title; }
    public void setTitle(String title)        { this.title = title; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Texte affiché dans la ListView des sessions. */
    @Override
    public String toString() {
        String date = createdAt != null
            ? createdAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
            : "";
        String t = (title != null && !title.isBlank()) ? title : "Session #" + id;
        return "💬 " + t + (date.isBlank() ? "" : "  •  " + date);
    }
}
