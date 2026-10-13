package ntm.topology;

import ntm.model.DeviceType;

/**
 * One switch in the middle, every PC cabled to it.
 */
public class StarTopology extends TopologyTemplate {

    @Override
    public String getName() {
        return "Star";
    }

    @Override
    public String getDescription() {
        return "A central switch with PCs around it. All traffic goes through the centre, "
                + "so if the switch fails, every PC is cut off. The most common layout in offices and labs.";
    }

    @Override
    public String getSizeLabel() {
        return "Number of PCs";
    }

    @Override
    public int getMinSize() {
        return 3;
    }

    @Override
    public int getMaxSize() {
        return 20;
    }

    @Override
    public int getDefaultSize() {
        return 6;
    }

    @Override
    public TopologyPlan createPlan(int size) {
        checkSize(size);
        int radius = Math.max(170, size * 22);
        int center = radius + 60;

        TopologyPlan plan = new TopologyPlan();
        int hub = plan.addNode(DeviceType.SWITCH, center, center);
        for (int i = 0; i < size; i++) {
            int pc = plan.addNode(DeviceType.PC, circleX(center, radius, i, size), circleY(center, radius, i, size));
            plan.addEdge(hub, pc, 2);
        }
        return plan;
    }
}
