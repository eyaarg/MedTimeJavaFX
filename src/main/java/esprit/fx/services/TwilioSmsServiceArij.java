package esprit.fx.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

/**
 * ============================================================
 *  TwilioSmsServiceArij — Envoi de SMS via l'API REST Twilio
 * ============================================================
 *
 *  Aucun SDK Twilio — utilise uniquement java.net.http.HttpClient
 *  (Java 11+), cohérent avec GroqApiServiceArij.
 *
 *  Endpoint :
 *    POST https://api.twilio.com/2010-04-01/Accounts/{SID}/Messages.json
 *
 *  Authentification :
 *    Basic Auth → Base64(ACCOUNT_SID:AUTH_TOKEN)
 *    Header : Authorization: Basic {base64}
 *
 *  Body (application/x-www-form-urlencoded) :
 *    To={telephone}&From={TWILIO_FROM}&Body={message}
 *
 *  Réponse attendue :
 *    HTTP 201 Created → SMS accepté par Twilio
 *    Tout autre code  → échec (loggé en console)
 *
 *  Configuration dans config.properties :
 *    twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *    twilio.auth.token=your_auth_token
 *    twilio.from=+1XXXXXXXXXX
 */
public class TwilioSmsServiceArij {

    // ------------------------------------------------------------------ //
    //  Constantes                                                         //
    // ------------------------------------------------------------------ //

    /** Template d'URL Twilio — {0} sera remplacé par l'ACCOUNT_SID. */
    private static final String TWILIO_API_URL =
        "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    // ------------------------------------------------------------------ //
    //  Configuration chargée depuis config.properties                    //
    // ------------------------------------------------------------------ //
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String apiUrl;

    // ------------------------------------------------------------------ //
    //  Client HTTP partagé (thread-safe, réutilisable)                   //
    // ------------------------------------------------------------------ //
    private final HttpClient httpClient;

    // ------------------------------------------------------------------ //
    //  Constructeur                                                       //
    // ------------------------------------------------------------------ //
    public TwilioSmsServiceArij() {
        Properties props = loadProperties();

        this.accountSid = props.getProperty("twilio.account.sid", "").trim();
        this.authToken  = props.getProperty("twilio.auth.token",  "").trim();
        this.fromNumber = props.getProperty("twilio.from",        "").trim();

        // Construire l'URL avec le SID injecté
        this.apiUrl = String.format(TWILIO_API_URL, accountSid);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ================================================================== //
    //  Méthode principale                                                 //
    // ================================================================== //

    /**
     * Envoie un SMS via l'API REST Twilio.
     *
     * @param telephone numéro destinataire au format E.164 (ex: "+21698765432")
     * @param message   texte du SMS (max 1600 caractères pour Twilio)
     * @return true  si Twilio a accepté le message (HTTP 201)
     *         false si erreur de config, réseau ou réponse inattendue
     */
    public boolean envoyer(String telephone, String message) {

        // ── Validation de la configuration ────────────────────────────
        if (!isConfigured()) {
            System.err.println("[TwilioSmsServiceArij] ✗ Configuration incomplète."
                + " Vérifiez twilio.account.sid / twilio.auth.token / twilio.from"
                + " dans config.properties.");
            return false;
        }

        // ── Validation des paramètres ──────────────────────────────────
        if (telephone == null || telephone.isBlank()) {
            System.err.println("[TwilioSmsServiceArij] ✗ Numéro de téléphone vide.");
            return false;
        }
        if (message == null || message.isBlank()) {
            System.err.println("[TwilioSmsServiceArij] ✗ Message vide.");
            return false;
        }

        try {
            // ── Construction du body URL-encoded ──────────────────────
            // Twilio attend : application/x-www-form-urlencoded
            // Chaque valeur doit être encodée (URLEncoder.encode)
            // pour gérer les espaces, accents, caractères spéciaux.
            String body = buildFormBody(telephone, message);

            // ── Construction du header Basic Auth ─────────────────────
            // Basic Auth = Base64(ACCOUNT_SID + ":" + AUTH_TOKEN)
            // Twilio utilise ACCOUNT_SID comme "username" et AUTH_TOKEN
            // comme "password" pour l'authentification HTTP Basic.
            String credentials = accountSid + ":" + authToken;
            String basicAuth   = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            // ── Construction de la requête HTTP ───────────────────────
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type",  "application/x-www-form-urlencoded")
                    .header("Authorization", basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            // ── Envoi synchrone (appelé depuis un Task JavaFX) ────────
            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();

            // ── Traitement de la réponse ──────────────────────────────
            if (statusCode == 201) {
                // 201 Created = SMS accepté et mis en file d'envoi par Twilio
                System.out.println("[TwilioSmsServiceArij] ✓ SMS envoyé avec succès"
                    + " → " + telephone
                    + " | HTTP " + statusCode);
                return true;

            } else {
                // Tout autre code = erreur (400 bad request, 401 auth, 429 rate limit…)
                System.err.println("[TwilioSmsServiceArij] ✗ Échec envoi SMS"
                    + " → HTTP " + statusCode
                    + " | Réponse : " + truncate(response.body(), 300));
                return false;
            }

        } catch (IOException e) {
            // Erreur réseau (timeout, DNS, connexion refusée…)
            System.err.println("[TwilioSmsServiceArij] ✗ Erreur réseau : " + e.getMessage());
            return false;

        } catch (InterruptedException e) {
            // Thread interrompu (ex: fermeture de l'application pendant l'envoi)
            Thread.currentThread().interrupt();
            System.err.println("[TwilioSmsServiceArij] ✗ Envoi interrompu : " + e.getMessage());
            return false;

        } catch (Exception e) {
            // Toute autre exception inattendue
            System.err.println("[TwilioSmsServiceArij] ✗ Erreur inattendue : "
                + e.getClass().getSimpleName() + " — " + e.getMessage());
            return false;
        }
    }

    // ================================================================== //
    //  Helpers privés                                                     //
    // ================================================================== //

    /**
     * Construit le body application/x-www-form-urlencoded.
     *
     * Format attendu par Twilio :
     *   To=%2B21698765432&From=%2B1XXXXXXXXXX&Body=Votre+consultation+est+confirm%C3%A9e
     *
     * URLEncoder.encode() avec UTF-8 :
     *   - encode les espaces en "+"
     *   - encode les caractères spéciaux (accents, +, &, =…) en %XX
     *   - obligatoire pour que Twilio parse correctement les valeurs
     */
    private String buildFormBody(String telephone, String message) {
        return "To="   + urlEncode(telephone)  +
               "&From=" + urlEncode(fromNumber) +
               "&Body=" + urlEncode(message);
    }

    /**
     * Encode une valeur pour un body application/x-www-form-urlencoded.
     */
    private String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Vérifie que les 3 clés Twilio sont renseignées dans config.properties.
     * Évite d'envoyer une requête vouée à échouer avec un 401.
     */
    private boolean isConfigured() {
        return !accountSid.isBlank()
            && !authToken.isBlank()
            && !fromNumber.isBlank()
            && !accountSid.startsWith("YOUR_")
            && !authToken.startsWith("YOUR_");
    }

    /**
     * Tronque une chaîne pour les logs (évite d'afficher des réponses JSON énormes).
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    /**
     * Charge config.properties depuis le classpath.
     * Même pattern que GroqApiServiceArij.
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("[TwilioSmsServiceArij] config.properties introuvable dans le classpath.");
            }
        } catch (IOException e) {
            System.err.println("[TwilioSmsServiceArij] Erreur chargement config.properties : " + e.getMessage());
        }
        return props;
    }
}
