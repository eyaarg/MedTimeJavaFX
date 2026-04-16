-- Vérifier les disponibilités dans la base de données
USE farah;

-- 1. Compter toutes les disponibilités
SELECT COUNT(*) as total_disponibilites FROM availability;

-- 2. Afficher toutes les disponibilités avec leurs dates
SELECT 
    id,
    doctor_id,
    start_date,
    start_time,
    end_date,
    end_time,
    is_online,
    notes
FROM availability
ORDER BY id DESC;

-- 3. Vérifier combien ont des dates NULL
SELECT 
    COUNT(*) as avec_dates_null
FROM availability
WHERE start_date IS NULL OR end_date IS NULL;

-- 4. Vérifier combien ont des dates valides
SELECT 
    COUNT(*) as avec_dates_valides
FROM availability
WHERE start_date IS NOT NULL AND end_date IS NOT NULL;

-- 5. Afficher les disponibilités avec dates valides
SELECT 
    id,
    doctor_id,
    CONCAT(start_date, ' ', start_time) as date_debut_complete,
    CONCAT(end_date, ' ', end_time) as date_fin_complete,
    CASE WHEN is_online = 0 THEN 'Disponible' ELSE 'Occupé' END as statut
FROM availability
WHERE start_date IS NOT NULL AND end_date IS NOT NULL
ORDER BY start_date DESC, start_time DESC;
