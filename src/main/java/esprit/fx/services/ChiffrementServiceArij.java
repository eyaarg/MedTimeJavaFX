package esprit.fx.services;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * Service de chiffrement des données médicales sensibles.
 * Utilise AES pour chiffrer/déchiffrer les données en base de données.
 * Pattern Singleton thread-safe.
 */
public class ChiffrementServiceArij {

    private static ChiffrementServiceArij instance;
    private SecretKey secretKey;
    private String cleSecrete;
    private String algorithme;
    private static final String ALGORITHM = "AES";

    /**
     * Constructeur privé (Singleton).
     */
    private ChiffrementServiceArij() {
        chargerConfiguration();
        initialiserCle();
    }

    /**
     * Récupère l'instance unique du service.
     */
    public static synchronized ChiffrementServiceArij getInstance() {
        if (instance == null) {
            instance = new ChiffrementServiceArij();
        }
        return instance;
    }

    /**
     * Charge la configuration depuis config.properties.
     */
    private void chargerConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            if (input != null) {
                props.load(input);
                this.cleSecrete = props.getProperty("ENCRYPTION_KEY", "medConsult2025");
                this.algorithme = props.getProperty("ENCRYPTION_ALGORITHM", "AES");
            } else {
                System.err.println("[ChiffrementServiceArij] config.properties non trouvé");
                this.cleSecrete = "medConsult2025";
                this.algorithme = "AES";
            }
            System.out.println("[ChiffrementServiceArij] Configuration chargée");
        } catch (IOException e) {
            System.err.println("[ChiffrementServiceArij] Erreur chargement config : " + e.getMessage());
            this.cleSecrete = "medConsult2025";
            this.algorithme = "AES";
        }
    }

    /**
     * Initialise la clé de chiffrement AES.
     */
    private void initialiserCle() {
        try {
            // Créer une clé AES à partir de la clé secrète
            byte[] decodedKey = cleSecrete.getBytes();
            // Assurer que la clé fait 16 bytes (128 bits) pour AES
            byte[] keyBytes = new byte[16];
            System.arraycopy(decodedKey, 0, keyBytes, 0, Math.min(decodedKey.length, 16));
            
            this.secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
            System.out.println("[ChiffrementServiceArij] Clé AES initialisée avec succès");
        } catch (Exception e) {
            System.err.println("[ChiffrementServiceArij] Erreur initialisation clé : " + e.getMessage());
        }
    }

    /**
     * Chiffre un texte.
     * 
     * @param texte Texte à chiffrer
     * @return Texte chiffré en Base64, ou texte original si null/vide
     */
    public String chiffrer(String texte) {
        if (texte == null || texte.isBlank()) {
            return texte;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(texte.getBytes());
            String chiffre = Base64.getEncoder().encodeToString(encryptedBytes);
            System.out.println("[ChiffrementServiceArij] Texte chiffré avec succès");
            return chiffre;
        } catch (Exception e) {
            System.err.println("[ChiffrementServiceArij] Erreur chiffrement : " + e.getMessage());
            return texte; // Fallback: retourner le texte original
        }
    }

    /**
     * Déchiffre un texte.
     * 
     * @param texteCrypte Texte chiffré en Base64
     * @return Texte déchiffré, ou texte original si erreur (données legacy)
     */
    public String dechiffrer(String texteCrypte) {
        if (texteCrypte == null || texteCrypte.isBlank()) {
            return texteCrypte;
        }

        // Vérifier si le texte est réellement chiffré
        if (!estChiffre(texteCrypte)) {
            return texteCrypte; // Données legacy non chiffrées
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(texteCrypte);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            String dechiffre = new String(decryptedBytes);
            System.out.println("[ChiffrementServiceArij] Texte déchiffré avec succès");
            return dechiffre;
        } catch (Exception e) {
            System.err.println("[ChiffrementServiceArij] Erreur déchiffrement : " + e.getMessage());
            return texteCrypte; // Fallback: retourner le texte original
        }
    }

    /**
     * Détecte si un texte est chiffré (Base64 valide).
     * 
     * @param texte Texte à vérifier
     * @return true si chiffré, false sinon
     */
    public boolean estChiffre(String texte) {
        if (texte == null || texte.isBlank()) {
            return false;
        }

        try {
            // Essayer de décoder en Base64
            Base64.getDecoder().decode(texte);
            // Essayer de déchiffrer
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(texte);
            cipher.doFinal(decodedBytes);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retourne l'algorithme utilisé.
     */
    public String getAlgorithme() {
        return ALGORITHM;
    }

    /**
     * Retourne la clé masquée pour l'affichage.
     */
    public String getCleSecreteMasquee() {
        return "••••••••••";
    }
}
