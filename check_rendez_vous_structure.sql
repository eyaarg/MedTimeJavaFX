-- Script pour vérifier la structure de la table rendez_vous
USE farah;

-- Afficher la structure complète de la table
DESCRIBE rendez_vous;

-- Afficher quelques exemples de données
SELECT * FROM rendez_vous LIMIT 5;

-- Vérifier les colonnes importantes
SELECT 
    id,
    patient_id,
    doctor_id,
    appointment_date_time,
    duration,
    status,
    consultation_type,
    reason,
    notes
FROM rendez_vous
ORDER BY id DESC
LIMIT 10;
