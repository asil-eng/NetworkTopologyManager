package ntm.model;

import ntm.exception.InvalidIPException;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A switch connects many devices inside the same local network.
 */
public class Switch extends Device {

    public Switch(int id, int networkId, String name, String ipAddress, int x, int y) throws InvalidIPException {
        super(id, networkId, name, ipAddress, x, y);
    }

    @Override
    public DeviceType getType() {
        return DeviceType.SWITCH;
    }

    @Override
    protected Color getColor() {
        return new Color(0, 150, 136);
    }

    @Override
    public int getMaxConnections() {
        return 24;
    }

    @Override
    public boolean canForwardTraffic() {
        return true;
    }

    /** A flat box with ports. */
    @Override
    protected void drawShape(Graphics2D g, int x, int y, int size) {
        int top = y + size / 4;
        int height = size / 2;
        g.fillRoundRect(x, top, size, height, 10, 10);
        g.setColor(OUTLINE);
        g.drawRoundRect(x, top, size, height, 10, 10);

        g.setColor(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            g.fillRect(x + 7 + i * 10, top + height / 2 - 3, 6, 6);
        }
    }

    @Override
    public String handlePacket(Packet packet) {
        if (packet.getSource() == this) {
            return getName() + " (switch) created the packet and sent it.";
        }
        if (packet.getDestination() == this) {
            return getName() + " (switch) received the packet: \"" + packet.getMessage() + "\"";
        }
        return getName() + " (switch) looked up its MAC address table and sent the frame out of the correct port.";
    }
}
