package ntm.topology;

import java.util.List;

/**
 * A ready-made network shape (star, ring, mesh, tree, bus).
 *
 * Abstract because each topology lays out and cables its devices differently:
 * every subclass overrides createPlan() - polymorphism again.
 */
public abstract class TopologyTemplate {

    public abstract String getName();

    public abstract String getDescription();

    /** What the size number means for this template, e.g. "Number of PCs". */
    public abstract String getSizeLabel();

    public abstract int getMinSize();

    public abstract int getMaxSize();

    public abstract int getDefaultSize();

    /** Builds the blueprint for a topology of the given size. */
    public abstract TopologyPlan createPlan(int size);

    /** Every available template, in the order shown in the dialog. */
    public static List<TopologyTemplate> all() {
        return List.of(new StarTopology(), new RingTopology(), new MeshTopology(),
                new TreeTopology(), new BusTopology());
    }

    protected void checkSize(int size) {
        if (size < getMinSize() || size > getMaxSize()) {
            throw new IllegalArgumentException(getSizeLabel() + " must be between "
                    + getMinSize() + " and " + getMaxSize() + ".");
        }
    }

    // Helpers for placing devices evenly around a circle, starting at the top.

    protected static int circleX(int centerX, int radius, int index, int count) {
        return centerX + (int) Math.round(radius * Math.cos(angle(index, count)));
    }

    protected static int circleY(int centerY, int radius, int index, int count) {
        return centerY + (int) Math.round(radius * Math.sin(angle(index, count)));
    }

    private static double angle(int index, int count) {
        return -Math.PI / 2 + 2 * Math.PI * index / count;
    }

    @Override
    public String toString() {
        return getName();
    }
}
