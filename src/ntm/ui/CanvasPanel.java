package ntm.ui;

import ntm.model.Device;
import ntm.model.Link;
import ntm.model.Network;
import ntm.model.Packet;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The drawing area. It paints the grid, cables, devices and the moving packet,
 * and passes mouse clicks to the editor window.
 */
public class CanvasPanel extends JPanel {

    private static final int GRID = 20;
    private static final double PACKET_SPEED = 0.03; // fraction of a cable crossed per frame

    private final Network network;
    private final NetworkEditorFrame editor;
    private Device selectedDevice;

    // Dragging
    private Device draggedDevice;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean dragged;

    // Packet animation
    private List<Device> animationPath;
    private int segment;
    private double progress;
    private Timer timer;

    public CanvasPanel(Network network, NetworkEditorFrame editor) {
        this.network = network;
        this.editor = editor;
        setBackground(Color.WHITE);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Device device = network.findDeviceAt(e.getX(), e.getY());
                if (device != null && editor.isDragAllowed()) {
                    draggedDevice = device;
                    dragOffsetX = e.getX() - device.getX();
                    dragOffsetY = e.getY() - device.getY();
                    dragged = false;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedDevice != null) {
                    draggedDevice.setPosition(e.getX() - dragOffsetX, e.getY() - dragOffsetY);
                    dragged = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedDevice != null && dragged) {
                    editor.onDeviceMoved(draggedDevice);
                }
                draggedDevice = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                editor.onCanvasClicked(e.getX(), e.getY(), e.getClickCount());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** The canvas grows with the network, so the scroll bars can reach every device. */
    @Override
    public Dimension getPreferredSize() {
        int width = 1400;
        int height = 900;
        for (Device device : network.getDevices()) {
            width = Math.max(width, device.getX() + Device.SIZE + 200);
            height = Math.max(height, device.getY() + Device.SIZE + 200);
        }
        return new Dimension(width, height);
    }

    public Device getSelectedDevice() {
        return selectedDevice;
    }

    public void setSelectedDevice(Device device) {
        this.selectedDevice = device;
        repaint();
    }

    public boolean isAnimating() {
        return timer != null && timer.isRunning();
    }

    /**
     * Moves a dot along the packet's path using a Swing Timer (about 60 frames per second).
     * onArrive is called with the index of each device the packet reaches,
     * onFinished when it reaches the destination.
     */
    public void animatePacket(Packet packet, IntConsumer onArrive, Runnable onFinished) {
        stopAnimation();
        animationPath = packet.getPath();
        segment = 0;
        progress = 0;
        onArrive.accept(0);

        timer = new Timer(16, e -> {
            progress += PACKET_SPEED;
            if (progress >= 1.0) {
                progress = 0;
                segment++;
                onArrive.accept(segment);
                if (segment >= animationPath.size() - 1) {
                    timer.stop();
                    onFinished.run();
                    // keep the route highlighted for a moment, then clear it
                    Timer clear = new Timer(1500, ev -> {
                        if (!isAnimating()) {
                            animationPath = null;
                            repaint();
                        }
                    });
                    clear.setRepeats(false);
                    clear.start();
                }
            }
            repaint();
        });
        timer.start();
    }

    public void stopAnimation() {
        if (timer != null) {
            timer.stop();
        }
        animationPath = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        for (Link link : network.getLinks()) {
            link.draw(g2, isOnAnimationPath(link));
        }
        for (Device device : network.getDevices()) {
            device.draw(g2, device == selectedDevice); // polymorphism: each device draws its own shape
        }
        drawPacket(g2);

        if (network.getDevices().isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 16f));
            String text = "Empty network - pick a device in the toolbar (e.g. \"+ Router\") and click here.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getVisibleRect().width - fm.stringWidth(text)) / 2, getVisibleRect().height / 2);
        }
        g2.dispose();
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(240, 242, 245));
        for (int x = 0; x < getWidth(); x += GRID) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += GRID) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private boolean isOnAnimationPath(Link link) {
        if (animationPath == null) {
            return false;
        }
        for (int i = 0; i < animationPath.size() - 1; i++) {
            if (link.connects(animationPath.get(i), animationPath.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private void drawPacket(Graphics2D g) {
        if (animationPath == null) {
            return;
        }
        int last = animationPath.size() - 1;
        Device from = animationPath.get(Math.min(segment, last));
        Device to = animationPath.get(Math.min(segment + 1, last));
        int px = (int) (from.getCenterX() + (to.getCenterX() - from.getCenterX()) * progress);
        int py = (int) (from.getCenterY() + (to.getCenterY() - from.getCenterY()) * progress);

        g.setColor(new Color(255, 193, 7));
        g.fillOval(px - 9, py - 9, 18, 18);
        g.setColor(new Color(230, 81, 0));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(px - 9, py - 9, 18, 18);
        g.setStroke(new BasicStroke(1f));
    }
}
