-- Table liste d'attente des rendez-vous
CREATE TABLE IF NOT EXISTS liste_attente (
    id               INT PRIMARY KEY AUTO_INCREMENT,
    patient_id       INT NOT NULL,
    doctor_id        INT NOT NULL,
    date_souhaitee   DATE,
    plage_horaire    VARCHAR(20),
    date_inscription DATETIME DEFAULT CURRENT_TIMESTAMP,
    statut           VARCHAR(20) DEFAULT 'EN_ATTENTE',
    date_expiration  DATETIME,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id)  REFERENCES users(id) ON DELETE CASCADE
);
