package ntm.service;

import ntm.exception.DeviceOfflineException;
import ntm.exception.NoRouteException;
import ntm.model.Device;
import ntm.model.Link;
import ntm.model.Network;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Finds the path a packet takes through the network graph.
 *
 * BFS      - fewest hops (number of cables), ignores latency.
 * Dijkstra - lowest total latency (in ms), may use more hops.
 */
public class RoutingService {

    public enum Algorithm {
        BFS("BFS (fewest hops)"),
        DIJKSTRA("Dijkstra (lowest latency)");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** An entry in Dijkstra's priority queue: a device and the distance to reach it. */
    private static class QueueEntry {
        final Device device;
        final int distance;

        QueueEntry(Device device, int distance) {
            this.device = device;
            this.distance = distance;
        }
    }

    public List<Device> findRoute(Network network, Device source, Device destination, Algorithm algorithm)
            throws NoRouteException, DeviceOfflineException {
        if (source == destination) {
            throw new NoRouteException("Source and destination must be different devices.");
        }
        if (!source.isOnline()) {
            throw new DeviceOfflineException(source.getName());
        }
        if (!destination.isOnline()) {
            throw new DeviceOfflineException(destination.getName());
        }

        Map<Device, List<Link>> graph = network.buildAdjacencyList();
        List<Device> path = (algorithm == Algorithm.BFS)
                ? bfs(graph, source, destination)
                : dijkstra(graph, source, destination);

        if (path.isEmpty()) {
            throw new NoRouteException("No route from " + source.getName() + " to " + destination.getName()
                    + ".\nCheck the cables, and that the routers and switches on the way are online.");
        }
        return path;
    }

    /** Adds up the latency of every cable along the path. */
    public int calculateLatency(Network network, List<Device> path) {
        int total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Link link = network.findLink(path.get(i), path.get(i + 1));
            if (link != null) {
                total += link.getLatencyMs();
            }
        }
        return total;
    }

    /**
     * A packet can enter a device if the device is online, and it is either the
     * destination or a device that forwards traffic (router/switch).
     * PCs and servers never pass packets on.
     */
    private boolean canPassThrough(Device next, Device destination) {
        return next.isOnline() && (next == destination || next.canForwardTraffic());
    }

    private List<Device> bfs(Map<Device, List<Link>> graph, Device source, Device destination) {
        Map<Device, Device> previous = new HashMap<>();
        Set<Device> visited = new HashSet<>();
        Queue<Device> queue = new ArrayDeque<>();

        queue.add(source);
        visited.add(source);

        while (!queue.isEmpty()) {
            Device current = queue.poll();
            if (current == destination) {
                return buildPath(previous, source, destination);
            }
            for (Link link : graph.getOrDefault(current, List.of())) {
                Device next = link.getOther(current);
                if (!visited.contains(next) && canPassThrough(next, destination)) {
                    visited.add(next);
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<Device> dijkstra(Map<Device, List<Link>> graph, Device source, Device destination) {
        Map<Device, Integer> distance = new HashMap<>();
        Map<Device, Device> previous = new HashMap<>();
        Set<Device> settled = new HashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>((a, b) -> Integer.compare(a.distance, b.distance));

        distance.put(source, 0);
        queue.add(new QueueEntry(source, 0));

        while (!queue.isEmpty()) {
            QueueEntry entry = queue.poll();
            Device current = entry.device;
            if (!settled.add(current)) {
                continue; // already found a shorter way to this device
            }
            if (current == destination) {
                return buildPath(previous, source, destination);
            }
            for (Link link : graph.getOrDefault(current, List.of())) {
                Device next = link.getOther(current);
                if (settled.contains(next) || !canPassThrough(next, destination)) {
                    continue;
                }
                int newDistance = entry.distance + link.getLatencyMs();
                if (newDistance < distance.getOrDefault(next, Integer.MAX_VALUE)) {
                    distance.put(next, newDistance);
                    previous.put(next, current);
                    queue.add(new QueueEntry(next, newDistance));
                }
            }
        }
        return Collections.emptyList();
    }

    /** Walks backwards from the destination using the "previous" map. */
    private List<Device> buildPath(Map<Device, Device> previous, Device source, Device destination) {
        LinkedList<Device> path = new LinkedList<>();
        Device step = destination;
        while (step != null) {
            path.addFirst(step);
            if (step == source) {
                break;
            }
            step = previous.get(step);
        }
        return path;
    }
}
