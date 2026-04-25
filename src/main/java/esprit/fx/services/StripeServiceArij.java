package esprit.fx.services;

import org.json.JSONObject;

import java.awt.Desktop;
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
 *  StripeServiceArij — Paiement en ligne via l'API Stripe
 * ============================================================
 *
 *  Aucun SDK Stripe — utilise uniquement java.net.http.HttpClient
 *  (Java 11+), cohérent avec GroqApiServiceArij et TwilioSmsServiceArij.
 *
 *  Flux de paiement :
 *  ──────────────────
 *  1. creerCheckoutSession() → POST Stripe → retourne une URL Checkout
 *  2. ouvrirNavigateur(url)  → Desktop.browse() → ouvre le navigateur
 *  3. Patient paie sur la page Stripe hébergée
 *  4. Stripe redirige vers success_url ou cancel_url (Symfony)
 *
 *  Endpoint :
 *    POST https://api.stripe.com/v1/checkout/sessions
 *
 *  Authentification :
 *    Basic Auth → Base64(STRIPE_SECRET_KEY + ":") + ""
 *    (Stripe utilise la clé comme username, mot de passe vide)
 *
 *  Body (application/x-www-form-urlencoded) :
 *    mode=payment
 *    currency=eur
 *    line_items[0][price_data][currency]=eur
 *    line_items[0][price_data][unit_amount]={montant * 100}  ← centimes
 *    line_items[0][price_data][product_data][name]={description}
 *    line_items[0][quantity]=1
 *    success_url=http://localhost:8000/paiement/succes/{consultationId}
 *    cancel_url=http://localhost:8000
 *
 *  Configuration dans config.properties :
 *    stripe.secret.key=sk_test_...
 *    stripe.success.url=http://localhost:8000/paiement/succes
 *    stripe.cancel.url=http://localhost:8000
 *    stripe.currency=eur
 *
 *  Usage :
 *  ────────
 *    StripeServiceArij stripe = new StripeServiceArij();
 *    String url = stripe.creerCheckoutSession(50.0, "Consultation Dr. Martin", 42L);
 *    stripe.ouvrirNavigateur(url);
 */
public class StripeServiceArij {

    // ------------------------------------------------------------------ //
    //  Constantes                                                         //
    // ------------------------------------------------------------------ //
    private static final String STRIPE_API_URL =
        "https://api.stripe.com/v1/checkout/sessions";

    // ------------------------------------------------------------------ //
    //  Configuration chargée depuis config.properties                    //
    // ------------------------------------------------------------------ //
    private final String secretKey;
    private final String successUrl;
    private final String cancelUrl;
    private final String currency;

    // ------------------------------------------------------------------ //
    //  Client HTTP partagé (thread-safe, réutilisable)                   //
    // ------------------------------------------------------------------ //
    private final HttpClient httpClient;

    // ------------------------------------------------------------------ //
    //  Constructeur                                                       //
    // ------------------------------------------------------------------ //
    public StripeServiceArij() {
        Properties props = loadProperties();

        this.secretKey  = props.getProperty("stripe.secret.key",  "").trim();
        this.successUrl = props.getProperty("stripe.success.url", "http://localhost:8000/paiement/succes").trim();
        this.cancelUrl  = props.getProperty("stripe.cancel.url",  "http://localhost:8000").trim();
        this.currency   = props.getProperty("stripe.currency",    "eur").trim().toLowerCase();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ================================================================== //
    //  Méthode 1 : creerCheckoutSession                                  //
    // ================================================================== //

    /**
     * Crée une session de paiement Stripe Checkout et retourne l'URL
     * de la page de paiement hébergée par Stripe.
     *
     * @param montant        montant en euros (ex: 50.0 → 5000 centimes)
     * @param description    nom du produit affiché sur la page Stripe
     *                       (ex: "Consultation Dr. Martin")
     * @param consultationId id de la consultation pour la success_url
     *                       (ex: success_url = .../paiement/succes/42)
     * @return URL Checkout Stripe (https://checkout.stripe.com/...)
     *         ou null en cas d'erreur
     * @throws Exception en cas d'erreur réseau ou de réponse inattendue
     */
    public String creerCheckoutSession(double montant,
                                        String description,
                                        Long consultationId) throws Exception {
        // ── Validation ────────────────────────────────────────────────
        if (!isConfigured()) {
            throw new IllegalStateException(
                "[StripeServiceArij] Clé secrète Stripe non configurée. " +
                "Ajoutez stripe.secret.key dans config.properties."
            );
        }
        if (montant <= 0) {
            throw new IllegalArgumentException(
                "[StripeServiceArij] Le montant doit être positif. Reçu : " + montant
            );
        }
        if (description == null || description.isBlank()) {
            description = "Consultation médicale";
        }

        // ── Conversion montant → centimes (Stripe attend des entiers) ─
        // Ex: 50.00 EUR → 5000 centimes
        long montantCentimes = Math.round(montant * 100);

        // ── Construction de l'URL de succès avec consultationId ───────
        String urlSucces = (consultationId != null && consultationId > 0)
            ? successUrl + "/" + consultationId
            : successUrl;

        // ── Construction du body URL-encoded ──────────────────────────
        String body = buildFormBody(montantCentimes, description, urlSucces);

        // ── Construction du header Basic Auth ─────────────────────────
        // Stripe : username = secret_key, password = "" (vide)
        // Base64(sk_test_xxx + ":") — le ":" sépare user:password
        String credentials = secretKey + ":";
        String basicAuth   = "Basic " + Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // ── Requête HTTP POST ──────────────────────────────────────────
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type",  "application/x-www-form-urlencoded")
                .header("Authorization", basicAuth)
                // Stripe-Version épingle l'API pour éviter les breaking changes
                .header("Stripe-Version", "2023-10-16")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        int statusCode = response.statusCode();

        System.out.println("[StripeServiceArij] HTTP " + statusCode
            + " | montant=" + montant + " EUR"
            + " | consultationId=" + consultationId);

        // ── Traitement de la réponse ──────────────────────────────────
        if (statusCode == 200) {
            // Stripe retourne 200 (pas 201) pour les sessions Checkout
            return extraireUrlCheckout(response.body());
        } else {
            // Extraire le message d'erreur Stripe depuis le JSON
            String erreur = extraireErreurStripe(response.body());
            throw new IOException(
                "[StripeServiceArij] Erreur Stripe [HTTP " + statusCode + "] : " + erreur
            );
        }
    }

    // ================================================================== //
    //  Méthode 2 : ouvrirNavigateur                                      //
    // ================================================================== //

    /**
     * Ouvre l'URL Checkout Stripe dans le navigateur système par défaut.
     *
     * Utilise java.awt.Desktop.browse() — compatible Windows, macOS, Linux.
     *
     * À appeler depuis le JavaFX Application Thread ou depuis un Task.
     * Desktop.browse() est non-bloquant : retourne immédiatement après
     * avoir lancé le navigateur.
     *
     * @param urlCheckout URL retournée par creerCheckoutSession()
     * @return true si le navigateur a été ouvert avec succès
     */
    public boolean ouvrirNavigateur(String urlCheckout) {
        if (urlCheckout == null || urlCheckout.isBlank()) {
            System.err.println("[StripeServiceArij] URL Checkout vide.");
            return false;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                System.err.println("[StripeServiceArij] Desktop non supporté sur ce système.");
                return false;
            }

            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                System.err.println("[StripeServiceArij] Action BROWSE non supportée.");
                return false;
            }

            desktop.browse(new URI(urlCheckout));

            System.out.println("[StripeServiceArij] ✓ Navigateur ouvert → " + urlCheckout);
            return true;

        } catch (Exception e) {
            System.err.println("[StripeServiceArij] ✗ Impossible d'ouvrir le navigateur : "
                + e.getMessage());
            return false;
        }
    }

    /**
     * Raccourci : crée la session ET ouvre le navigateur en une seule étape.
     *
     * Exemple d'utilisation depuis un Task JavaFX :
     * <pre>
     *   Task<Void> task = new Task<>() {
     *       protected Void call() throws Exception {
     *           new StripeServiceArij()
     *               .payerConsultation(50.0, "Consultation Dr. Martin", 42L);
     *           return null;
     *       }
     *   };
     *   new Thread(task).start();
     * </pre>
     *
     * @param montant        montant en euros
     * @param description    description affichée sur la page Stripe
     * @param consultationId id de la consultation
     * @throws Exception en cas d'erreur réseau ou de configuration
     */
    public void payerConsultation(double montant,
                                   String description,
                                   Long consultationId) throws Exception {
        String urlCheckout = creerCheckoutSession(montant, description, consultationId);
        ouvrirNavigateur(urlCheckout);
    }

    // ================================================================== //
    //  Helpers privés                                                     //
    // ================================================================== //

    /**
     * Construit le body application/x-www-form-urlencoded pour Stripe.
     *
     * Stripe Checkout Sessions API attend ces paramètres :
     *   mode                                          = payment
     *   line_items[0][price_data][currency]           = eur
     *   line_items[0][price_data][unit_amount]        = 5000  (centimes)
     *   line_items[0][price_data][product_data][name] = "Consultation..."
     *   line_items[0][quantity]                       = 1
     *   success_url                                   = http://...
     *   cancel_url                                    = http://...
     *
     * URLEncoder.encode() est obligatoire pour les valeurs contenant
     * des espaces, accents, crochets ([]) ou caractères spéciaux.
     */
    private String buildFormBody(long montantCentimes,
                                  String description,
                                  String urlSucces) {
        return "mode=" + enc("payment")
            + "&line_items[0][price_data][currency]="                    + enc(currency)
            + "&line_items[0][price_data][unit_amount]="                 + montantCentimes
            + "&line_items[0][price_data][product_data][name]="          + enc(description)
            + "&line_items[0][quantity]=1"
            + "&success_url="                                            + enc(urlSucces)
            + "&cancel_url="                                             + enc(cancelUrl);
    }

    /**
     * Extrait session.url depuis la réponse JSON de Stripe.
     *
     * Réponse Stripe typique :
     * {
     *   "id": "cs_test_...",
     *   "url": "https://checkout.stripe.com/c/pay/cs_test_...",
     *   "status": "open",
     *   ...
     * }
     */
    private String extraireUrlCheckout(String responseBody) throws IOException {
        try {
            JSONObject json = new JSONObject(responseBody);

            if (!json.has("url") || json.isNull("url")) {
                throw new IOException(
                    "Champ 'url' absent dans la réponse Stripe : "
                    + truncate(responseBody, 200)
                );
            }

            String url = json.getString("url");
            System.out.println("[StripeServiceArij] ✓ Session créée → " + truncate(url, 80));
            return url;

        } catch (org.json.JSONException e) {
            throw new IOException(
                "Réponse Stripe non parseable : " + truncate(responseBody, 200)
            );
        }
    }

    /**
     * Extrait le message d'erreur depuis une réponse d'erreur Stripe.
     *
     * Réponse d'erreur Stripe typique :
     * {
     *   "error": {
     *     "code": "api_key_expired",
     *     "message": "No such API key: 'sk_test_...'",
     *     "type": "invalid_request_error"
     *   }
     * }
     */
    private String extraireErreurStripe(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                return error.optString("message", "Erreur inconnue")
                    + " (code: " + error.optString("code", "?") + ")";
            }
        } catch (Exception ignored) {}
        return truncate(responseBody, 300);
    }

    /**
     * Vérifie que la clé secrète Stripe est configurée et non placeholder.
     */
    private boolean isConfigured() {
        return secretKey != null
            && !secretKey.isBlank()
            && !secretKey.startsWith("YOUR_")
            && (secretKey.startsWith("sk_test_") || secretKey.startsWith("sk_live_"));
    }

    /**
     * Encode une valeur pour application/x-www-form-urlencoded.
     * URLEncoder encode les espaces en "+" et les caractères spéciaux en %XX.
     */
    private String enc(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Tronque une chaîne pour les logs.
     */
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * Charge config.properties depuis le classpath.
     * Même pattern que GroqApiServiceArij et TwilioSmsServiceArij.
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("[StripeServiceArij] config.properties introuvable.");
            }
        } catch (IOException e) {
            System.err.println("[StripeServiceArij] Erreur chargement config : " + e.getMessage());
        }
        return props;
    }
}
