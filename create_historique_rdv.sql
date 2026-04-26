-- Table historique des changements de statut des rendez-vous
CREATE TABLE IF NOT EXISTS historique_rdv (
    id               INT PRIMARY KEY AUTO_INCREMENT,
    rdv_id           INT NOT NULL,
    ancien_statut    VARCHAR(20),
    nouveau_statut   VARCHAR(20) NOT NULL,
    date_changement  DATETIME DEFAULT CURRENT_TIMESTAMP,
    modifie_par      INT,
    commentaire      TEXT,
    FOREIGN KEY (rdv_id)      REFERENCES rendez_vous(id) ON DELETE CASCADE,
    FOREIGN KEY (modifie_par) REFERENCES users(id)       ON DELETE SET NULL
);
