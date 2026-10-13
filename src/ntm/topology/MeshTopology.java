package ntm.topology;

import ntm.model.DeviceType;

/**
 * Every router cabled to every other router (a full mesh).
 */
public class MeshTopology extends TopologyTemplate {

    @Override
    public String getName() {
        return "Mesh";
    }

    @Override
    public String getDescription() {
        return "Every router is connected to every other router. The most reliable layout, but it needs "
                + "the most cables: n x (n - 1) / 2. Cables across the middle are slower, so Dijkstra matters.";
    }

    @Override
    public String getSizeLabel() {
        return "Number of routers";
    }

    @Override
    public int getMinSize() {
        return 3;
    }

    /** A router has 8 ports, and in a full mesh each router needs (n - 1) of them. */
    @Override
    public int getMaxSize() {
        return 8;
    }

    @Override
    public int getDefaultSize() {
        return 5;
    }

    @Override
    public TopologyPlan createPlan(int size) {
        checkSize(size);
        int radius = Math.max(150, size * 40);
        int center = radius + 60;

        TopologyPlan plan = new TopologyPlan();
        for (int i = 0; i < size; i++) {
            plan.addNode(DeviceType.ROUTER, circleX(center, radius, i, size), circleY(center, radius, i, size));
        }
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                // Neighbours around the circle get fast cables, cables across the middle are slower.
                int distance = Math.min(j - i, size - (j - i));
                plan.addEdge(i, j, 10 * distance);
            }
        }
        return plan;
    }
}
