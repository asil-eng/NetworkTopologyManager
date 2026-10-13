package ntm.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

/**
 * A network cable between two devices (stored in the "connections" table).
 * The latency is how long a packet takes to cross it, used by Dijkstra.
 */
public class Link {

    private int id;
    private final int networkId;
    private final Device deviceA;
    private final Device deviceB;
    private int latencyMs;

    public Link(int id, int networkId, Device deviceA, Device deviceB, int latencyMs) {
        this.id = id;
        this.networkId = networkId;
        this.deviceA = deviceA;
        this.deviceB = deviceB;
        setLatencyMs(latencyMs);
    }

    /** Given one end of the cable, returns the other end. */
    public Device getOther(Device device) {
        return device == deviceA ? deviceB : deviceA;
    }

    public boolean involves(Device device) {
        return device == deviceA || device == deviceB;
    }

    public boolean connects(Device a, Device b) {
        return (a == deviceA && b == deviceB) || (a == deviceB && b == deviceA);
    }

    /** A cable only works if the devices at both ends are switched on. */
    public boolean isActive() {
        return deviceA.isOnline() && deviceB.isOnline();
    }

    public void draw(Graphics2D g, boolean highlighted) {
        if (highlighted) {
            g.setColor(new Color(76, 175, 80));
            g.setStroke(new BasicStroke(5f));
        } else if (isActive()) {
            g.setColor(new Color(120, 144, 156));
            g.setStroke(new BasicStroke(2.5f));
        } else {
            g.setColor(new Color(239, 154, 154));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{8f, 6f}, 0f));
        }
        g.drawLine(deviceA.getCenterX(), deviceA.getCenterY(), deviceB.getCenterX(), deviceB.getCenterY());
        g.setStroke(new BasicStroke(1f));

        String label = latencyMs + " ms";
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
        FontMetrics fm = g.getFontMetrics();
        int midX = (deviceA.getCenterX() + deviceB.getCenterX()) / 2;
        int midY = (deviceA.getCenterY() + deviceB.getCenterY()) / 2;
        int width = fm.stringWidth(label) + 6;
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(midX - width / 2, midY - 8, width, 14, 6, 6);
        g.setColor(Color.DARK_GRAY);
        g.drawString(label, midX - width / 2 + 3, midY + 3);
    }

    /** True if the point is within a few pixels of the cable - used for clicking on cables. */
    public boolean isNear(int px, int py) {
        return Line2D.ptSegDist(deviceA.getCenterX(), deviceA.getCenterY(),
                deviceB.getCenterX(), deviceB.getCenterY(), px, py) <= 6;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNetworkId() {
        return networkId;
    }

    public Device getDeviceA() {
        return deviceA;
    }

    public Device getDeviceB() {
        return deviceB;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(int latencyMs) {
        if (latencyMs < 1 || latencyMs > 1000) {
            throw new IllegalArgumentException("Latency must be between 1 and 1000 ms.");
        }
        this.latencyMs = latencyMs;
    }
}
