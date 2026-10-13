package ntm.model;

import ntm.exception.InvalidIPException;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A server provides services (websites, files, databases) to other devices.
 */
public class Server extends Device {

    public Server(int id, int networkId, String name, String ipAddress, int x, int y) throws InvalidIPException {
        super(id, networkId, name, ipAddress, x, y);
    }

    @Override
    public DeviceType getType() {
        return DeviceType.SERVER;
    }

    @Override
    protected Color getColor() {
        return new Color(123, 31, 162);
    }

    @Override
    public int getMaxConnections() {
        return 2; // servers often have two network cards for backup
    }

    @Override
    public boolean canForwardTraffic() {
        return false;
    }

    /** A tower with drive bays and a status light. */
    @Override
    protected void drawShape(Graphics2D g, int x, int y, int size) {
        int width = size * 3 / 5;
        int left = x + (size - width) / 2;
        g.fillRoundRect(left, y, width, size, 6, 6);
        g.setColor(OUTLINE);
        g.drawRoundRect(left, y, width, size, 6, 6);

        g.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) {
            g.fillRect(left + 5, y + 8 + i * 13, width - 10, 5);
        }
        g.setColor(new Color(118, 255, 3));
        g.fillOval(left + width - 10, y + size - 9, 5, 5);
    }

    @Override
    public String handlePacket(Packet packet) {
        if (packet.getSource() == this) {
            return getName() + " (server) sent a response packet.";
        }
        if (packet.getDestination() == this) {
            return getName() + " (server) received the request \"" + packet.getMessage()
                    + "\" and sent back a reply (HTTP 200 OK).";
        }
        return getName() + " (server) is not the destination, so it dropped the packet.";
    }
}
