package ntm.model;

import ntm.exception.InvalidIPException;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Base class for every network device.
 *
 * It is abstract because a plain "Device" never exists - it is always a
 * Router, Switch, PC or Server. Each subclass decides how it looks
 * (drawShape) and how it reacts to a packet (handlePacket).
 */
public abstract class Device implements Drawable, Connectable {

    public static final int SIZE = 50;
    protected static final Color OUTLINE = new Color(55, 71, 79);
    private static final Color OFFLINE_COLOR = new Color(189, 189, 189);

    // Encapsulation: fields are private and can only change through validated methods.
    private int id;
    private final int networkId;
    private String name;
    private String ipAddress;
    private int x;
    private int y;
    private boolean online = true;

    protected Device(int id, int networkId, String name, String ipAddress, int x, int y) throws InvalidIPException {
        this.id = id;
        this.networkId = networkId;
        setName(name);
        setIpAddress(ipAddress);
        this.x = x;
        this.y = y;
    }

    // ------------------------------------------------------------------
    // Abstract methods - every subclass MUST implement these (polymorphism)
    // ------------------------------------------------------------------

    public abstract DeviceType getType();

    protected abstract Color getColor();

    /** Draws the device's icon. The fill colour is already set on g. */
    protected abstract void drawShape(Graphics2D g, int x, int y, int size);

    /** What this device does when a packet reaches it. Returns a log message. */
    public abstract String handlePacket(Packet packet);

    // ------------------------------------------------------------------
    // Shared behaviour
    // ------------------------------------------------------------------

    /**
     * Template method: the steps are the same for every device, only
     * drawShape() differs between subclasses.
     */
    @Override
    public final void draw(Graphics2D g, boolean selected) {
        if (selected) {
            g.setColor(new Color(33, 150, 243, 50));
            g.fillRoundRect(x - 8, y - 8, SIZE + 16, SIZE + 16, 16, 16);
            g.setColor(new Color(33, 150, 243));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x - 8, y - 8, SIZE + 16, SIZE + 16, 16, 16);
        }

        g.setStroke(new BasicStroke(1.5f));
        g.setColor(online ? getColor() : OFFLINE_COLOR);
        drawShape(g, x, y, SIZE);

        if (!online) {
            g.setColor(new Color(211, 47, 47));
            g.setStroke(new BasicStroke(3f));
            g.drawLine(x + 8, y + 8, x + SIZE - 8, y + SIZE - 8);
            g.drawLine(x + SIZE - 8, y + 8, x + 8, y + SIZE - 8);
        }

        g.setStroke(new BasicStroke(1f));
        drawCenteredText(g, name, y + SIZE + 15, Font.BOLD, 12f, Color.BLACK);
        drawCenteredText(g, ipAddress, y + SIZE + 28, Font.PLAIN, 11f, Color.DARK_GRAY);
    }

    private void drawCenteredText(Graphics2D g, String text, int baseline, int style, float size, Color color) {
        g.setFont(g.getFont().deriveFont(style, size));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, getCenterX() - fm.stringWidth(text) / 2, baseline);
    }

    @Override
    public boolean contains(int px, int py) {
        return px >= x && px <= x + SIZE && py >= y && py <= y + SIZE;
    }

    /**
     * Default cabling rule: at least one side must be a router or switch.
     * Two PCs (or a PC and a server) cannot be plugged directly into each other.
     */
    @Override
    public boolean canConnectTo(Device other) {
        return other != null && other != this && (this.canForwardTraffic() || other.canForwardTraffic());
    }

    /** Checks an IPv4 address such as 192.168.1.10 */
    public static boolean isValidIP(String ip) {
        if (ip == null) {
            return false;
        }
        String[] parts = ip.trim().split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return false;
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    /** Changes name, IP and status together - either all change or none do. */
    public void updateDetails(String newName, String newIp, boolean newOnline) throws InvalidIPException {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Device name cannot be empty.");
        }
        if (!isValidIP(newIp)) {
            throw new InvalidIPException(newIp);
        }
        this.name = newName.trim();
        this.ipAddress = newIp.trim();
        this.online = newOnline;
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNetworkId() {
        return networkId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) throws InvalidIPException {
        if (!isValidIP(ipAddress)) {
            throw new InvalidIPException(ipAddress);
        }
        this.ipAddress = ipAddress.trim();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = Math.max(0, x);
        this.y = Math.max(0, y);
    }

    public int getCenterX() {
        return x + SIZE / 2;
    }

    public int getCenterY() {
        return y + SIZE / 2;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return name + " (" + ipAddress + ")";
    }
}
