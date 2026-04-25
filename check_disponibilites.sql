-- Script pour vérifier les disponibilités dans la base de données
-- Exécutez ce script dans votre client MySQL pour voir les données

USE farah;

-- Afficher toutes les disponibilités avec leurs dates
SELECT 
    id,
    doctor_id,
    start_date,
    start_time,
    end_date,
    end_time,
    is_online,
    notes,
    created_at
FROM availability
ORDER BY id DESC
LIMIT 20;

-- Compter les disponibilités par statut
SELECT 
    is_online,
    COUNT(*) as count
FROM availability
GROUP BY is_online;

-- Vérifier les disponibilités avec dates NULL
SELECT 
    COUNT(*) as count_null_dates
FROM availability
WHERE start_date IS NULL OR end_date IS NULL;

-- Afficher les disponibilités avec dates valides
SELECT 
    id,
    doctor_id,
    CONCAT(start_date, ' ', start_time) as date_debut_complete,
    CONCAT(end_date, ' ', end_time) as date_fin_complete,
    is_online,
    notes
FROM availability
WHERE start_date IS NOT NULL AND end_date IS NOT NULL
ORDER BY start_date DESC, start_time DESC
LIMIT 10;
