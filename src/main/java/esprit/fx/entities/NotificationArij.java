package esprit.fx.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entité Notification — table "notifications" partagée avec Symfony.
 *
 * Mapping colonnes MySQL ↔ champs Java :
 *   id            → id          (Long)
 *   user_id       → destinataireId (Long)
 *   message       → message     (String)
 *   type          → type        (String : "info" | "success" | "warning")
 *   is_read       → lu          (boolean, défaut false)
 *   created_at    → createdAt   (LocalDateTime)
 *   link          → lien        (String, nullable)
 *   title         → title       (String, nullable — champ Symfony existant)
 */
public class NotificationArij {

    // ------------------------------------------------------------------ //
    //  Types autorisés (compatibles Symfony)                              //
    // ------------------------------------------------------------------ //
    public static final String TYPE_INFO    = "info";
    public static final String TYPE_SUCCESS = "success";
    public static final String TYPE_WARNING = "warning";

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------------------------ //
    //  Champs                                                             //
    // ------------------------------------------------------------------ //
    private Long          id;
    private Long          destinataireId;   // → user_id en BDD
    private String        title;            // champ Symfony existant
    private String        message;
    private String        type;             // info | success | warning
    private boolean       lu    = false;    // → is_read en BDD
    private LocalDateTime createdAt;
    private String        lien;             // nullable → link en BDD

    // ------------------------------------------------------------------ //
    //  Constructeurs                                                      //
    // ------------------------------------------------------------------ //
    public NotificationArij() {}

    public NotificationArij(Long destinataireId, String title,
                             String message, String type) {
        this.destinataireId = destinataireId;
        this.title          = title;
        this.message        = message;
        this.type           = type;
        this.lu             = false;
        this.createdAt      = LocalDateTime.now();
    }

    // ------------------------------------------------------------------ //
    //  Getters / Setters                                                  //
    // ------------------------------------------------------------------ //
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getDestinataireId()             { return destinataireId; }
    public void setDestinataireId(Long v)       { this.destinataireId = v; }

    public String getTitle()                    { return title; }
    public void setTitle(String title)          { this.title = title; }

    public String getMessage()                  { return message; }
    public void setMessage(String message)      { this.message = message; }

    public String getType()                     { return type; }
    public void setType(String type)            { this.type = type; }

    public boolean isLu()                       { return lu; }
    public void setLu(boolean lu)               { this.lu = lu; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    public String getLien()                     { return lien; }
    public void setLien(String lien)            { this.lien = lien; }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /** Retourne l'emoji correspondant au type pour l'affichage UI. */
    public String typeEmoji() {
        return switch (type == null ? "" : type.toLowerCase()) {
            case TYPE_SUCCESS -> "✅";
            case TYPE_WARNING -> "⚠️";
            default           -> "ℹ️";
        };
    }

    /** Date formatée pour l'affichage dans les listes. */
    public String formattedDate() {
        return createdAt != null ? createdAt.format(DISPLAY_FMT) : "";
    }

    @Override
    public String toString() {
        return typeEmoji() + " " + (title != null ? title : message);
    }
}
