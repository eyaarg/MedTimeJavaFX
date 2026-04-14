package esprit.fx.entities;

public class User_roles {
    private User user;      // l'objet User complet
    private Role role;      // l'objet Role complet

    public User_roles() {}

    public User_roles(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public String toString() {
        return "User_roles{user=" + user.getUsername() + ", role=" + role.getName() + '}';
    }
}