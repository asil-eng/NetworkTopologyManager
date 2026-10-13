package ntm.model;

/**
 * Read-only access: can open networks, send test packets and view reports,
 * but cannot change anything.
 */
public class Viewer extends User {

    public Viewer(int id, String username) {
        super(id, username);
    }

    @Override
    public String getRole() {
        return "VIEWER";
    }

    @Override
    public boolean canEditNetworks() {
        return false;
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }
}
