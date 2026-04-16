-- Script SQL pour créer les tables nécessaires au module Rendez-vous et Disponibilités
-- Base de données: farah

-- Table des rendez-vous
CREATE TABLE IF NOT EXISTS rendez_vous (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    date_heure DATETIME NOT NULL,
    motif TEXT,
    statut ENUM('DEMANDE', 'CONFIRME', 'ANNULE', 'TERMINE') DEFAULT 'DEMANDE',
    notes TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NULL,
    
    INDEX idx_patient_id (patient_id),
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_date_heure (date_heure),
    INDEX idx_statut (statut)
);

-- Table des disponibilités
CREATE TABLE IF NOT EXISTS disponibilites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    est_disponible BOOLEAN DEFAULT TRUE,
    notes TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_date_debut (date_debut),
    INDEX idx_date_fin (date_fin),
    INDEX idx_disponible (est_disponible)
);

-- Table des utilisateurs (si elle n'existe pas déjà)
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

-- Table des rôles (si elle n'existe pas déjà)
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Table de liaison utilisateur-rôle (si elle n'existe pas déjà)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Insertion des rôles de base
INSERT IGNORE INTO roles (name) VALUES ('PATIENT'), ('DOCTOR'), ('ADMIN');

-- Données de test (optionnel)
-- Utilisateur médecin de test
INSERT IGNORE INTO users (email, username, password, is_active, is_verified) 
VALUES ('docteur@test.com', 'Dr. Martin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/VcAtWtfIm', TRUE, TRUE);

-- Utilisateur patient de test  
INSERT IGNORE INTO users (email, username, password, is_active, is_verified)
VALUES ('patient@test.com', 'Jean Dupont', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/VcAtWtfIm', TRUE, TRUE);

-- Attribution des rôles
INSERT IGNORE INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.email = 'docteur@test.com' AND r.name = 'DOCTOR';

INSERT IGNORE INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.email = 'patient@test.com' AND r.name = 'PATIENT';

-- Disponibilités de test pour le médecin
INSERT IGNORE INTO disponibilites (doctor_id, date_debut, date_fin, notes)
SELECT u.id, '2024-04-16 09:00:00', '2024-04-16 12:00:00', 'Consultation matinale'
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id 
WHERE r.name = 'DOCTOR' LIMIT 1;

INSERT IGNORE INTO disponibilites (doctor_id, date_debut, date_fin, notes)
SELECT u.id, '2024-04-16 14:00:00', '2024-04-16 18:00:00', 'Consultation après-midi'
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id 
WHERE r.name = 'DOCTOR' LIMIT 1;