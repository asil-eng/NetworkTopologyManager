package ntm.topology;

import ntm.model.DeviceType;

/**
 * Routers connected in a circle, each one to its two neighbours.
 */
public class RingTopology extends TopologyTemplate {

    @Override
    public String getName() {
        return "Ring";
    }

    @Override
    public String getDescription() {
        return "Routers connected in a circle. Each router has two neighbours, so there are always "
                + "two ways around: one broken router does not split the network.";
    }

    @Override
    public String getSizeLabel() {
        return "Number of routers";
    }

    @Override
    public int getMinSize() {
        return 3;
    }

    @Override
    public int getMaxSize() {
        return 12;
    }

    @Override
    public int getDefaultSize() {
        return 5;
    }

    @Override
    public TopologyPlan createPlan(int size) {
        checkSize(size);
        int radius = Math.max(150, size * 35);
        int center = radius + 60;

        TopologyPlan plan = new TopologyPlan();
        for (int i = 0; i < size; i++) {
            plan.addNode(DeviceType.ROUTER, circleX(center, radius, i, size), circleY(center, radius, i, size));
        }
        for (int i = 0; i < size; i++) {
            plan.addEdge(i, (i + 1) % size, 10);
        }
        return plan;
    }
}
