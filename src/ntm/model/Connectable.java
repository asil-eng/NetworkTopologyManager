package ntm.model;

/**
 * Rules for how a device can be cabled to other devices.
 */
public interface Connectable {

    /** How many cables this device can have (number of ports). */
    int getMaxConnections();

    /** True if packets can pass THROUGH this device on the way to somewhere else. */
    boolean canForwardTraffic();

    /** True if a cable between this device and the other one is allowed. */
    boolean canConnectTo(Device other);
}
