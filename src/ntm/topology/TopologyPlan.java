package ntm.topology;

import ntm.model.Device;
import ntm.model.DeviceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The blueprint a template produces: which devices to create, where to put them,
 * and which ones to cable together. Nothing is saved until TopologyBuilder runs it.
 */
public class TopologyPlan {

    /** A device to create: its type and top-left position relative to the plan's corner. */
    public record Node(DeviceType type, int x, int y) {
    }

    /** A cable between two nodes, given by their positions in the node list. */
    public record Edge(int from, int to, int latencyMs) {
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    /** Adds a device centred on (centerX, centerY) and returns its index. */
    public int addNode(DeviceType type, int centerX, int centerY) {
        nodes.add(new Node(type, centerX - Device.SIZE / 2, centerY - Device.SIZE / 2));
        return nodes.size() - 1;
    }

    public void addEdge(int from, int to, int latencyMs) {
        edges.add(new Edge(from, to, latencyMs));
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public int getWidth() {
        int width = 0;
        for (Node node : nodes) {
            width = Math.max(width, node.x() + Device.SIZE);
        }
        return width;
    }

    /** Includes room for the name and IP labels drawn under each device. */
    public int getHeight() {
        int height = 0;
        for (Node node : nodes) {
            height = Math.max(height, node.y() + Device.SIZE + 30);
        }
        return height;
    }
}
