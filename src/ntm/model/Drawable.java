package ntm.model;

import java.awt.Graphics2D;

/**
 * Anything that can paint itself on the network canvas.
 */
public interface Drawable {

    void draw(Graphics2D g, boolean selected);

    /** True if the point (px, py) is inside this object - used for mouse clicks. */
    boolean contains(int px, int py);
}
