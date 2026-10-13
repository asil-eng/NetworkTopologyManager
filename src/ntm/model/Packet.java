package ntm.model;

import java.util.List;

/**
 * A piece of data travelling from a source device to a destination device.
 */
public class Packet {

    private final Device source;
    private final Device destination;
    private final String message;
    private final List<Device> path;

    public Packet(Device source, Device destination, String message, List<Device> path) {
        this.source = source;
        this.destination = destination;
        this.message = message;
        this.path = List.copyOf(path);
    }

    public Device getSource() {
        return source;
    }

    public Device getDestination() {
        return destination;
    }

    public String getMessage() {
        return message;
    }

    public List<Device> getPath() {
        return path;
    }

    /** Number of cables the packet crosses. */
    public int getHopCount() {
        return path.size() - 1;
    }
}
