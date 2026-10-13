package ntm.model;

/**
 * Full access: can edit networks and manage user accounts.
 */
public class Admin extends User {

    public Admin(int id, String username) {
        super(id, username);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public boolean canEditNetworks() {
        return true;
    }

    @Override
    public boolean canManageUsers() {
        return true;
    }
}
