package esprit.fx.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationUtil {
    
    public enum NotificationType {
        SUCCESS, ERROR, INFO, WARNING
    }
    
    public static void showNotification(Stage owner, String message, NotificationType type) {
        Popup popup = new Popup();
        
        // Container compact
        HBox container = new HBox(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 20, 12, 20));
        container.setMaxWidth(350);
        container.setStyle(getStyleForType(type));
        
        // Icône
        Label icon = new Label(getIconForType(type));
        icon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
        
        // Message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-font-weight: 500;");
        
        container.getChildren().addAll(icon, messageLabel);
        
        popup.getContent().add(container);
        
        // Positionner en bas à droite
        double x = owner.getX() + owner.getWidth() - 370;
        double y = owner.getY() + owner.getHeight() - 100;
        
        popup.setAutoHide(true);
        popup.show(owner, x, y);
        
        // Animation d'entrée (slide up + fade in)
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), container);
        slideIn.setFromY(50);
        slideIn.setToY(0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        slideIn.play();
        fadeIn.play();
        
        // Auto-fermeture après 2.5 secondes
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), container);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> popup.hide());
            fadeOut.play();
        });
        delay.play();
    }
    
    private static String getStyleForType(NotificationType type) {
        String baseStyle = "-fx-background-radius: 8; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 3); " +
                          "-fx-border-radius: 8; " +
                          "-fx-border-width: 0;";
        
        switch (type) {
            case SUCCESS:
                return baseStyle + " -fx-background-color: #10b981;"; // Vert moderne
            case ERROR:
                return baseStyle + " -fx-background-color: #ef4444;"; // Rouge moderne
            case WARNING:
                return baseStyle + " -fx-background-color: #f59e0b;"; // Orange moderne
            case INFO:
            default:
                return baseStyle + " -fx-background-color: #3b82f6;"; // Bleu moderne
        }
    }
    
    private static String getIconForType(NotificationType type) {
        switch (type) {
            case SUCCESS:
                return "✓";
            case ERROR:
                return "✗";
            case WARNING:
                return "⚠";
            case INFO:
            default:
                return "ℹ";
        }
    }
    
    /**
     * Affiche une belle popup de confirmation moderne
     */
    public static boolean showConfirmation(Stage owner, String title, String message, String details) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(title);
        
        // Container principal
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
        );
        container.setPrefWidth(450);
        
        // Header avec icône
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Icône d'avertissement
        Label iconLabel = new Label("⚠");
        iconLabel.setStyle(
            "-fx-font-size: 36px;" +
            "-fx-text-fill: #f59e0b;" +
            "-fx-background-color: #fef3c7;" +
            "-fx-background-radius: 50%;" +
            "-fx-padding: 15;" +
            "-fx-min-width: 70px;" +
            "-fx-min-height: 70px;" +
            "-fx-alignment: center;"
        );
        
        // Titre
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1f2937;"
        );
        
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #6b7280;"
        );
        
        titleBox.getChildren().addAll(titleLabel, messageLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        header.getChildren().addAll(iconLabel, titleBox);
        
        // Détails
        VBox detailsBox = new VBox(8);
        detailsBox.setStyle(
            "-fx-background-color: #f9fafb;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 15;"
        );
        
        if (details != null && !details.isEmpty()) {
            String[] lines = details.split("\n");
            for (String line : lines) {
                Label detailLabel = new Label(line);
                detailLabel.setStyle(
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: #374151;" +
                    "-fx-font-weight: 500;"
                );
                detailsBox.getChildren().add(detailLabel);
            }
        }
        
        // Boutons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle(
            "-fx-background-color: #f3f4f6;" +
            "-fx-text-fill: #374151;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 12 24;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: transparent;" +
            "-fx-border-width: 0;"
        );
        cancelButton.setOnMouseEntered(e -> 
            cancelButton.setStyle(
                "-fx-background-color: #e5e7eb;" +
                "-fx-text-fill: #374151;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            )
        );
        cancelButton.setOnMouseExited(e -> 
            cancelButton.setStyle(
                "-fx-background-color: #f3f4f6;" +
                "-fx-text-fill: #374151;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            )
        );
        
        Button confirmButton = new Button("Confirmer");
        confirmButton.setStyle(
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 12 24;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: transparent;" +
            "-fx-border-width: 0;"
        );
        confirmButton.setOnMouseEntered(e -> 
            confirmButton.setStyle(
                "-fx-background-color: #dc2626;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            )
        );
        confirmButton.setOnMouseExited(e -> 
            confirmButton.setStyle(
                "-fx-background-color: #ef4444;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            )
        );
        
        cancelButton.setOnAction(e -> {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        });
        
        confirmButton.setOnAction(e -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });
        
        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        
        // Assembler
        container.getChildren().addAll(header, detailsBox, buttonBox);
        
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent;");
        
        // Animation d'entrée
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), container);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), container);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        dialog.setOnShowing(e -> {
            scaleIn.play();
            fadeIn.play();
        });
        
        return dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
