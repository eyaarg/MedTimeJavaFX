package esprit.fx.utils;

import jakarta.mail.Session;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.MessagingException;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "sandbox.smtp.mailtrap.io";
    private static final int SMTP_PORT = 2525;
    private static final String SMTP_USERNAME = "your_username"; // Replace with Mailtrap username
    private static final String SMTP_PASSWORD = "your_password"; // Replace with Mailtrap password

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
        String subject = "Vérification de votre compte";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Merci de vous être inscrit. Veuillez vérifier votre compte en cliquant sur le lien ci-dessous :</p>" +
                "<a href='http://localhost:8080/verify?token=" + token + "'>Vérifier mon compte</a>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendPasswordResetEmail(String toEmail, String username, String token) throws MessagingException {
        String subject = "Réinitialisation de votre mot de passe";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Vous avez demandé une réinitialisation de votre mot de passe. Cliquez sur le lien ci-dessous pour le réinitialiser :</p>" +
                "<a href='http://localhost:8080/reset-password?token=" + token + "'>Réinitialiser mon mot de passe</a>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendDoctorApprovedEmail(String toEmail, String username) throws MessagingException {
        String subject = "Votre compte de docteur a été approuvé";
        String content = "<html><body>" +
                "<h1>Félicitations, " + username + "!</h1>" +
                "<p>Votre compte de docteur a été approuvé. Vous pouvez maintenant accéder à toutes les fonctionnalités.</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendDoctorRejectedEmail(String toEmail, String username, String rejectionReason) throws MessagingException {
        String subject = "Votre compte de docteur a été rejeté";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Nous sommes désolés, mais votre compte de docteur a été rejeté pour la raison suivante :</p>" +
                "<p>" + rejectionReason + "</p>" +
                "</body></html>";
        sendEmail(toEmail, subject, content);
    }

    public static void sendAccountLockedEmail(String toEmail, String username) throws MessagingException {
        String subject = "Votre compte a été verrouillé";
        String content = "<html><body>" +
                "<h1>Bonjour, " + username + "!</h1>" +
                "<p>Votre compte a été verrouillé en raison de plusieurs tentatives de connexion échouées. Veuillez contacter le support pour plus d'informations.</p>" +
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
