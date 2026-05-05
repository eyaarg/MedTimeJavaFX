package esprit.fx.services;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

import java.util.ArrayList;
import java.util.List;

/**
 * Service PayPal pour la gestion des paiements.
 * Utilise le SDK REST PayPal.
 */
public class PayPalService {

    // Credentials PayPal Sandbox — à remplacer par les vraies clés en production
    private static final String CLIENT_ID     = "Adv7qMFA_bJBQmHNX8wM5EITTuKUyalzV_0xcj-Nz7ON-EvTauDR4HlrGvkiKPcMPQlALeliPRcPijlj";
    private static final String CLIENT_SECRET = "ECFQsGaLRCfN4kimntiTOzb9vYQu51QUYMR2qwGjOnBbAHKQwsRSlP8bCaD8gxUXhtlJrDCrjTDu52oM";
    private static final String MODE          = "sandbox";

    private final APIContext apiContext;

    public PayPalService() {
        this.apiContext = new APIContext(CLIENT_ID, CLIENT_SECRET, MODE);
    }

    /**
     * Crée un paiement PayPal et retourne l'objet Payment avec l'URL d'approbation.
     *
     * @param total      montant total
     * @param currency   devise (ex: "USD", "EUR")
     * @param successUrl URL de redirection après succès
     * @param cancelUrl  URL de redirection après annulation
     * @return Payment créé
     * @throws PayPalRESTException en cas d'erreur API
     */
    public Payment creerPaiement(double total, String currency,
                                  String successUrl, String cancelUrl) throws PayPalRESTException {
        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setTotal(String.format(java.util.Locale.US, "%.2f", total));

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Commande MedTime");

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(successUrl);
        redirectUrls.setCancelUrl(cancelUrl);

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        payment.setRedirectUrls(redirectUrls);

        return payment.create(apiContext);
    }

    /**
     * Exécute un paiement PayPal après approbation de l'utilisateur.
     *
     * @param paymentId ID du paiement PayPal
     * @param payerId   ID du payeur PayPal
     * @return Payment exécuté
     * @throws PayPalRESTException en cas d'erreur API
     */
    public Payment executerPaiement(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution execution = new PaymentExecution();
        execution.setPayerId(payerId);

        return payment.execute(apiContext, execution);
    }

    /**
     * Extrait l'URL d'approbation PayPal depuis un Payment créé.
     *
     * @param payment Payment créé
     * @return URL d'approbation, ou null si non trouvée
     */
    public String getApprovalUrl(Payment payment) {
        if (payment == null || payment.getLinks() == null) {
            return null;
        }
        for (Links link : payment.getLinks()) {
            if ("approval_url".equalsIgnoreCase(link.getRel())) {
                return link.getHref();
            }
        }
        return null;
    }
}
