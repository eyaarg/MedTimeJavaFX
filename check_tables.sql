-- Script pour vérifier la structure des tables existantes
-- Exécutez ces commandes dans votre base "farah" pour voir la structure

-- Vérifier la structure de la table availability
DESCRIBE availability;

-- Vérifier la structure de la table rendez_vous
DESCRIBE rendez_vous;

-- Vérifier la structure de la table users
DESCRIBE users;

-- Vérifier la structure de la table roles
DESCRIBE roles;

-- Vérifier la structure de la table user_roles
DESCRIBE user_roles;

-- Voir quelques données de test
SELECT * FROM users LIMIT 5;
SELECT * FROM roles;
SELECT * FROM user_roles LIMIT 5;