-- =====================================================================
--  Tables MediAssist Chatbot
--  Compatibles avec Symfony (Doctrine) — partagées JavaFX / Symfony
-- =====================================================================

-- Session de chat (une conversation = une session)
CREATE TABLE IF NOT EXISTS chat_session (
    id          INT          NOT NULL AUTO_INCREMENT,
    patient_id  INT          NOT NULL,
    title       VARCHAR(255) NOT NULL DEFAULT 'Nouvelle conversation',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_chat_session_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Message dans une session
CREATE TABLE IF NOT EXISTS chat_message (
    id          INT          NOT NULL AUTO_INCREMENT,
    session_id  INT          NOT NULL,
    role        VARCHAR(20)  NOT NULL COMMENT 'user | assistant',
    content     TEXT         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_chat_message_session (session_id),
    CONSTRAINT fk_chat_message_session
        FOREIGN KEY (session_id) REFERENCES chat_session(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
