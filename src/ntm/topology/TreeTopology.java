package ntm.topology;

import ntm.model.DeviceType;

/**
 * A hierarchy: a router at the top, switches under it, three PCs under each switch.
 */
public class TreeTopology extends TopologyTemplate {

    private static final int PCS_PER_SWITCH = 3;
    private static final int GROUP_WIDTH = 340;

    @Override
    public String getName() {
        return "Tree";
    }

    @Override
    public String getDescription() {
        return "A hierarchy: one router at the top, switches below it, and " + PCS_PER_SWITCH
                + " PCs under each switch. Easy to grow, and common in schools and offices.";
    }

    @Override
    public String getSizeLabel() {
        return "Number of switches";
    }

    @Override
    public int getMinSize() {
        return 2;
    }

    @Override
    public int getMaxSize() {
        return 5;
    }

    @Override
    public int getDefaultSize() {
        return 3;
    }

    @Override
    public TopologyPlan createPlan(int size) {
        checkSize(size);
        int left = 40;
        int width = size * GROUP_WIDTH;

        TopologyPlan plan = new TopologyPlan();
        int root = plan.addNode(DeviceType.ROUTER, left + width / 2, 60);
        for (int s = 0; s < size; s++) {
            int switchX = left + s * GROUP_WIDTH + GROUP_WIDTH / 2;
            int sw = plan.addNode(DeviceType.SWITCH, switchX, 230);
            plan.addEdge(root, sw, 5);
            for (int p = 0; p < PCS_PER_SWITCH; p++) {
                int pc = plan.addNode(DeviceType.PC, switchX + (p - 1) * 110, 400);
                plan.addEdge(sw, pc, 2);
            }
        }
        return plan;
    }
}
