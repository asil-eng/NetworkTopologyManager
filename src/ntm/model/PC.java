package ntm.model;

import ntm.exception.InvalidIPException;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A personal computer. It is an end device: it sends and receives packets
 * but never forwards other people's traffic.
 */
public class PC extends Device {

    public PC(int id, int networkId, String name, String ipAddress, int x, int y) throws InvalidIPException {
        super(id, networkId, name, ipAddress, x, y);
    }

    @Override
    public DeviceType getType() {
        return DeviceType.PC;
    }

    @Override
    protected Color getColor() {
        return new Color(96, 125, 139);
    }

    @Override
    public int getMaxConnections() {
        return 1; // a PC has a single network card
    }

    @Override
    public boolean canForwardTraffic() {
        return false;
    }

    /** A monitor on a stand. */
    @Override
    protected void drawShape(Graphics2D g, int x, int y, int size) {
        int screenHeight = size - 16;
        g.fillRoundRect(x + 2, y + 2, size - 4, screenHeight, 6, 6);
        g.setColor(OUTLINE);
        g.drawRoundRect(x + 2, y + 2, size - 4, screenHeight, 6, 6);

        g.setColor(new Color(225, 245, 254));
        g.fillRect(x + 7, y + 7, size - 14, screenHeight - 10);

        g.setColor(OUTLINE);
        g.fillRect(x + size / 2 - 3, y + 2 + screenHeight, 6, 7);
        g.fillRect(x + size / 2 - 13, y + size - 7, 26, 4);
    }

    @Override
    public String handlePacket(Packet packet) {
        if (packet.getSource() == this) {
            return getName() + " (PC) created the packet and sent it out of its network card.";
        }
        if (packet.getDestination() == this) {
            return getName() + " (PC) received the packet: \"" + packet.getMessage() + "\"";
        }
        return getName() + " (PC) is not the destination, so it dropped the packet.";
    }
}
