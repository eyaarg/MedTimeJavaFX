-- Script SQL simple pour créer les tables nécessaires
-- À exécuter dans votre base de données "farah"

-- Table des rendez-vous
CREATE TABLE IF NOT EXISTS rendez_vous (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    date_heure DATETIME NOT NULL,
    motif TEXT,
    statut VARCHAR(20) DEFAULT 'DEMANDE',
    notes TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NULL
);

-- Table des disponibilités
CREATE TABLE IF NOT EXISTS disponibilites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    est_disponible BOOLEAN DEFAULT TRUE,
    notes TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Vérifier si la table users existe, sinon la créer
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    phone_number VARCHAR(20),
    is_verified BOOLEAN DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    email_verification_token_expires_at DATETIME,
    password_reset_token VARCHAR(255),
    password_reset_token_expires_at DATETIME,
    failed_attempts INT DEFAULT 0
);

-- Table des rôles
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Table de liaison utilisateur-rôle
CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id)
);

-- Insérer les rôles de base
INSERT IGNORE INTO roles (name) VALUES ('PATIENT'), ('DOCTOR'), ('ADMIN');

-- Créer un utilisateur médecin de test (mot de passe: "password")
INSERT IGNORE INTO users (email, username, password, is_active, is_verified) 
VALUES ('docteur@test.com', 'Dr. Martin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/VcAtWtfIm', TRUE, TRUE);

-- Créer un utilisateur patient de test (mot de passe: "password")
INSERT IGNORE INTO users (email, username, password, is_active, is_verified)
VALUES ('patient@test.com', 'Jean Dupont', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/VcAtWtfIm', TRUE, TRUE);

-- Attribuer le rôle DOCTOR au médecin
INSERT IGNORE INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.email = 'docteur@test.com' AND r.name = 'DOCTOR';

-- Attribuer le rôle PATIENT au patient
INSERT IGNORE INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.email = 'patient@test.com' AND r.name = 'PATIENT';