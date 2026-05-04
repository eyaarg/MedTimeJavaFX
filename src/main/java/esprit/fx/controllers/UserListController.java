package esprit.fx.controllers;

import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.services.ServiceUser;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserListController {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> activeColumn;
    @FXML private TableColumn<User, String> verifiedColumn;
    @FXML private TableColumn<User, String> createdAtColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ObservableList<User> masterData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupColumns();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter(newValue));
        refreshUsers();
    }

    @FXML
    private void refreshUsers() {
        try {
            List<User> users = serviceUser.getAll();
            masterData.setAll(users);
            applyFilter(searchField.getText());
        } catch (SQLException e) {
            System.err.println("Erreur chargement users: " + e.getMessage());
            masterData.clear();
            usersTable.setItems(FXCollections.observableArrayList());
            resultCountLabel.setText("Erreur de chargement");
        }
    }

    private void setupColumns() {
        usernameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getUsername())));
        emailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getEmail())));
        phoneColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getPhoneNumber())));
        roleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatRoles(data.getValue().getRoles())));
        activeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isActive() ? "Oui" : "Non"));
        verifiedColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isVerified() ? "Oui" : "Non"));
        createdAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt().format(DATE_FMT) : "-"
        ));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(6, editBtn, deleteBtn);

            {
                box.setPadding(new Insets(2, 0, 2, 0));
                editBtn.getStyleClass().addAll("icon-btn", "btn-icon-warning");
                deleteBtn.getStyleClass().addAll("icon-btn", "btn-icon-danger");

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openEditUserDialog(user);
                });
                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void openAddUserDialog() {
        Optional<UserFormData> input = showUserDialog("Ajouter un utilisateur", null);
        if (input.isEmpty()) {
            return;
        }

        try {
            User user = new User();
            user.setUsername(input.get().username);
            user.setEmail(input.get().email);
            user.setPhoneNumber(input.get().phone);
            user.setPassword(input.get().password);
            user.setCreatedAt(LocalDateTime.now());
            user.setActive(input.get().active);
            user.setVerified(input.get().verified);
            user.setFailedAttempts(0);
            serviceUser.registerUser(user, input.get().roleName);
            refreshUsers();
        } catch (SQLException e) {
            showError("Ajout impossible", e.getMessage());
        }
    }

    private void openEditUserDialog(User original) {
        Optional<UserFormData> input = showUserDialog("Modifier l'utilisateur #" + original.getId(), original);
        if (input.isEmpty()) {
            return;
        }

        try {
            User updated = new User();
            updated.setId(original.getId());
            updated.setUsername(input.get().username);
            updated.setEmail(input.get().email);
            updated.setPhoneNumber(input.get().phone);
            // Empty password means keep current password in ServiceUser#modifier.
            updated.setPassword(input.get().password);
            updated.setCreatedAt(original.getCreatedAt() != null ? original.getCreatedAt() : LocalDateTime.now());
            updated.setActive(input.get().active);
            updated.setVerified(input.get().verified);
            updated.setEmailVerificationToken(original.getEmailVerificationToken());
            updated.setEmailVerificationTokenExpiresAt(original.getEmailVerificationTokenExpiresAt());
            updated.setPasswordResetToken(original.getPasswordResetToken());
            updated.setPasswordResetTokenExpiresAt(original.getPasswordResetTokenExpiresAt());
            updated.setFailedAttempts(original.getFailedAttempts());

            serviceUser.modifier(updated);
            serviceUser.updateUserRole(updated.getId(), input.get().roleName);
            refreshUsers();
        } catch (SQLException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private void deleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer utilisateur");
        confirm.setHeaderText("Supprimer " + safe(user.getUsername()) + " ?");
        confirm.setContentText("Cette action est irreversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            serviceUser.supprimer(user.getId());
            refreshUsers();
        } catch (SQLException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    private Optional<UserFormData> showUserDialog(String title, User existing) {
        Dialog<UserFormData> dialog = new Dialog<>();
        dialog.setTitle(title);

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField usernameField = new TextField(existing != null ? safeValue(existing.getUsername()) : "");
        TextField emailField = new TextField(existing != null ? safeValue(existing.getEmail()) : "");
        TextField phoneField = new TextField(existing != null ? safeValue(existing.getPhoneNumber()) : "");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(existing == null ? "Mot de passe" : "Laisser vide pour ne pas changer");
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.getItems().setAll(loadRoleOptions());
        String defaultRole = extractPrimaryRoleName(existing);
        if (defaultRole != null && roleComboBox.getItems().contains(defaultRole)) {
            roleComboBox.setValue(defaultRole);
        } else if (!roleComboBox.getItems().isEmpty()) {
            roleComboBox.setValue(roleComboBox.getItems().get(0));
        }
        CheckBox activeBox = new CheckBox("Actif");
        activeBox.setSelected(existing == null || existing.isActive());
        CheckBox verifiedBox = new CheckBox("Verifie");
        verifiedBox.setSelected(existing != null && existing.isVerified());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Username"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Telephone"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Role"), 0, 3);
        grid.add(roleComboBox, 1, 3);
        grid.add(new Label("Password"), 0, 4);
        grid.add(passwordField, 1, 4);
        grid.add(activeBox, 1, 5);
        grid.add(verifiedBox, 1, 6);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (usernameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty()) {
                showError("Validation", "Username et email sont obligatoires.");
                event.consume();
                return;
            }
            if (existing == null && passwordField.getText().trim().isEmpty()) {
                showError("Validation", "Le mot de passe est obligatoire a la creation.");
                event.consume();
                return;
            }
            if (roleComboBox.getValue() == null || roleComboBox.getValue().isBlank()) {
                showError("Validation", "Veuillez selectionner un role.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != saveType) {
                return null;
            }
            return new UserFormData(
                    usernameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    passwordField.getText().trim(),
                    roleComboBox.getValue(),
                    activeBox.isSelected(),
                    verifiedBox.isSelected()
            );
        });

        return dialog.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? "Operation invalide." : message);
        alert.showAndWait();
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<User> filtered = masterData.stream()
                .filter(user -> matches(user, q))
                .collect(Collectors.toList());
        usersTable.setItems(FXCollections.observableArrayList(filtered));
        resultCountLabel.setText(filtered.size() + " user(s)");
    }

    private boolean matches(User user, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String roles = formatRoles(user.getRoles()).toLowerCase(Locale.ROOT);
        return safe(user.getUsername()).toLowerCase(Locale.ROOT).contains(q)
                || safe(user.getEmail()).toLowerCase(Locale.ROOT).contains(q)
                || safe(user.getPhoneNumber()).toLowerCase(Locale.ROOT).contains(q)
                || roles.contains(q)
                || String.valueOf(user.getId()).contains(q);
    }

    private String formatRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "-";
        }
        return roles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private List<String> loadRoleOptions() {
        try {
            List<String> roles = serviceUser.getAvailableRoleNames();
            if (roles == null || roles.isEmpty()) {
                return List.of("PATIENT");
            }
            return roles;
        } catch (SQLException e) {
            List<String> fallback = new ArrayList<>();
            fallback.add("PATIENT");
            return fallback;
        }
    }

    private String extractPrimaryRoleName(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return null;
        }
        Role role = user.getRoles().get(0);
        return role == null ? null : role.getName();
    }

    private static class UserFormData {
        private final String username;
        private final String email;
        private final String phone;
        private final String password;
        private final String roleName;
        private final boolean active;
        private final boolean verified;

        private UserFormData(String username, String email, String phone, String password, String roleName, boolean active, boolean verified) {
            this.username = username;
            this.email = email;
            this.phone = phone;
            this.password = password;
            this.roleName = roleName;
            this.active = active;
            this.verified = verified;
        }
    }
}

