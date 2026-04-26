package esprit.fx.utils;

import jakarta.mail.Session;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.MessagingException;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "sandbox.smtp.mailtrap.io";
    private static final int SMTP_PORT = 2525;
    private static final String SMTP_USERNAME = "691e29acfee8fe";
    private static final String SMTP_PASSWORD = "47a0807d668f22";

    private static Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
    }

    public static void sendVerificationEmail(String toEmail, String username, String token) throws MessagingException {
        String subject = "Vérification de votre compte MedTimeFX";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Merci de vous être inscrit. Voici votre code de vérification :</p>" +
                "<h2 style='color:blue;'>" + token + "</h2>" +
                "<p>Saisissez ce code dans l'application pour activer votre compte.</p>" +
                "<p>Ce code expire dans 24h.</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendPasswordResetEmail(String toEmail, String username, String token) throws MessagingException {
        String subject = "Réinitialisation de votre mot de passe MedTimeFX";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Voici votre code de réinitialisation :</p>" +
                "<h2 style='color:red;'>" + token + "</h2>" +
                "<p>Saisissez ce code dans l'application. Il expire dans 1h.</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendDoctorApprovedEmail(String toEmail, String username) throws MessagingException {
        String subject = "Votre compte médecin a été approuvé";
        String content = "<html><body>" +
                "<h1>Félicitations, Dr. " + username + "!</h1>" +
                "<p>Votre compte médecin a été approuvé par l'administrateur.</p>" +
                "<p>Vous pouvez maintenant vous connecter à MedTimeFX.</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendDoctorRejectedEmail(String toEmail, String username, String rejectionReason) throws MessagingException {
        String subject = "Votre compte médecin a été refusé";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Votre demande a été refusée pour la raison suivante :</p>" +
                "<p><strong>" + rejectionReason + "</strong></p>" +
                "<p>Vous pouvez re-soumettre un nouveau document depuis l'application.</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendAccountLockedEmail(String toEmail, String username) throws MessagingException {
        String subject = "Votre compte a été verrouillé";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Votre compte a été verrouillé après 5 tentatives de connexion échouées.</p>" +
                "<p>Contactez l'administrateur pour débloquer votre compte.</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    private static void sendEmail(String toEmail, String subject, String content) throws MessagingException {
        Message message = new MimeMessage(getSession());
        message.setFrom(new InternetAddress("no-reply@medtimefx.com"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(content, "text/html; charset=utf-8");
        Transport.send(message);
    }
}