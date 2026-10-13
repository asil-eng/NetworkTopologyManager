package ntm.model;

import ntm.exception.InvalidIPException;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A router joins different networks together and forwards packets between them.
 */
public class Router extends Device {

    public Router(int id, int networkId, String name, String ipAddress, int x, int y) throws InvalidIPException {
        super(id, networkId, name, ipAddress, x, y);
    }

    @Override
    public DeviceType getType() {
        return DeviceType.ROUTER;
    }

    @Override
    protected Color getColor() {
        return new Color(25, 118, 210);
    }

    @Override
    public int getMaxConnections() {
        return 8;
    }

    @Override
    public boolean canForwardTraffic() {
        return true;
    }

    /** A circle with arrows - the standard router symbol. */
    @Override
    protected void drawShape(Graphics2D g, int x, int y, int size) {
        g.fillOval(x, y, size, size);
        g.setColor(OUTLINE);
        g.drawOval(x, y, size, size);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int arm = size / 4;
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(cx - arm, cy, cx + arm, cy);
        g.drawLine(cx, cy - arm, cx, cy + arm);
        g.drawLine(cx + arm, cy, cx + arm - 5, cy - 5);
        g.drawLine(cx + arm, cy, cx + arm - 5, cy + 5);
        g.drawLine(cx - arm, cy, cx - arm + 5, cy - 5);
        g.drawLine(cx - arm, cy, cx - arm + 5, cy + 5);
    }

    @Override
    public String handlePacket(Packet packet) {
        if (packet.getSource() == this) {
            return getName() + " (router) created the packet and sent it.";
        }
        if (packet.getDestination() == this) {
            return getName() + " (router) received the packet: \"" + packet.getMessage() + "\"";
        }
        return getName() + " (router) checked its routing table and forwarded the packet towards "
                + packet.getDestination().getIpAddress();
    }
}
