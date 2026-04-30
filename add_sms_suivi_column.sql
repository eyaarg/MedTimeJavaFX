-- =====================================================================
--  Migration : ajout colonne sms_suivi_envoye à la table consultations
--  Compatible Symfony (Doctrine) — partagée JavaFX / Symfony
-- =====================================================================

ALTER TABLE consultations
    ADD COLUMN IF NOT EXISTS sms_suivi_envoye TINYINT(1) NOT NULL DEFAULT 0
    COMMENT 'true si le SMS de suivi post-consultation a été envoyé';

-- Index pour accélérer la requête du scheduler (filtre sur statut + sms_suivi_envoye)
CREATE INDEX IF NOT EXISTS idx_consultations_sms_suivi
    ON consultations (status, sms_suivi_envoye, consultation_date);
