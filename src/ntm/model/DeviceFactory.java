package ntm.model;

import ntm.exception.InvalidIPException;

/**
 * Factory pattern: creates the right Device subclass for a DeviceType,
 * so the rest of the program never needs "new Router(...)" etc.
 */
public final class DeviceFactory {

    private DeviceFactory() {
        // utility class - no objects
    }

    public static Device create(DeviceType type, int id, int networkId, String name, String ip, int x, int y)
            throws InvalidIPException {
        return switch (type) {
            case ROUTER -> new Router(id, networkId, name, ip, x, y);
            case SWITCH -> new Switch(id, networkId, name, ip, x, y);
            case PC -> new PC(id, networkId, name, ip, x, y);
            case SERVER -> new Server(id, networkId, name, ip, x, y);
        };
    }
}
