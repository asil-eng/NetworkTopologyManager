package ntm.topology;

import ntm.model.DeviceType;

/**
 * A simulated bus: a line of switches is the "backbone cable", with one PC tapped off each.
 * A real bus is ONE shared cable; this app only has point-to-point cables, so the
 * chain of switches stands in for it.
 */
public class BusTopology extends TopologyTemplate {

    private static final int SPACING = 170;

    @Override
    public String getName() {
        return "Bus (simulated)";
    }

    @Override
    public String getDescription() {
        return "A line of switches acts as the shared backbone cable, with one PC tapped off each. "
                + "A real bus is a single shared cable, which point-to-point cables can only imitate. "
                + "One break in the backbone splits the network in two.";
    }

    @Override
    public String getSizeLabel() {
        return "Number of PCs";
    }

    @Override
    public int getMinSize() {
        return 2;
    }

    @Override
    public int getMaxSize() {
        return 8;
    }

    @Override
    public int getDefaultSize() {
        return 4;
    }

    @Override
    public TopologyPlan createPlan(int size) {
        checkSize(size);
        TopologyPlan plan = new TopologyPlan();
        int previousSwitch = -1;
        for (int i = 0; i < size; i++) {
            int x = 80 + i * SPACING;
            int sw = plan.addNode(DeviceType.SWITCH, x, 80);
            int pc = plan.addNode(DeviceType.PC, x, 250);
            plan.addEdge(sw, pc, 2);
            if (previousSwitch >= 0) {
                plan.addEdge(previousSwitch, sw, 5);
            }
            previousSwitch = sw;
        }
        return plan;
    }
}
