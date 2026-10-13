package ntm.model;

/**
 * The four kinds of device that can be placed on a network.
 */
public enum DeviceType {
    ROUTER("Router", "R"),
    SWITCH("Switch", "SW"),
    PC("PC", "PC"),
    SERVER("Server", "SRV");

    private final String label;
    private final String namePrefix;

    DeviceType(String label, String namePrefix) {
        this.label = label;
        this.namePrefix = namePrefix;
    }

    public String getLabel() {
        return label;
    }

    /** Used to suggest names such as R1, SW2, PC3. */
    public String getNamePrefix() {
        return namePrefix;
    }

    @Override
    public String toString() {
        return label;
    }
}
