package ntm.exception;

/**
 * Thrown when a packet is sent from or to a device that is switched off.
 */
public class DeviceOfflineException extends Exception {

    public DeviceOfflineException(String deviceName) {
        super(deviceName + " is offline. Turn it back on before sending packets to or from it.");
    }
}
