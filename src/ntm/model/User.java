package ntm.model;

/**
 * A person who can log in. Abstract because every user is either an Admin or a Viewer,
 * and each kind has different permissions.
 */
public abstract class User {

    private final int id;
    private final String username;

    protected User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public abstract String getRole();

    /** Can this user add, move, edit and delete devices, cables and networks? */
    public abstract boolean canEditNetworks();

    /** Can this user create and delete other user accounts? */
    public abstract boolean canManageUsers();

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return username + " (" + getRole() + ")";
    }
}
