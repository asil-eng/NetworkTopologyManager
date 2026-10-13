package ntm.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A saved network design: a graph where devices are the nodes and links (cables) are the edges.
 */
public class Network {

    private int id;
    private String name;
    private String description;
    private int createdById;
    private String createdByName;
    private Timestamp createdAt;
    private int deviceCount; // filled in by the dashboard query, before devices are loaded

    private final List<Device> devices = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();

    public Network(int id, String name, String description, int createdById) {
        this.id = id;
        setName(name);
        this.description = description;
        this.createdById = createdById;
    }

    // ------------------------------------------------------------------
    // Devices and links
    // ------------------------------------------------------------------

    public void clear() {
        devices.clear();
        links.clear();
    }

    public void addDevice(Device device) {
        devices.add(device);
    }

    /** Removing a device also removes every cable plugged into it. */
    public void removeDevice(Device device) {
        devices.remove(device);
        links.removeIf(link -> link.involves(device));
    }

    public void addLink(Link link) {
        links.add(link);
    }

    public void removeLink(Link link) {
        links.remove(link);
    }

    public List<Device> getDevices() {
        return Collections.unmodifiableList(devices);
    }

    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public Device findById(int deviceId) {
        for (Device device : devices) {
            if (device.getId() == deviceId) {
                return device;
            }
        }
        return null;
    }

    /** Finds the device under the mouse. Searches backwards so the top-most device wins. */
    public Device findDeviceAt(int x, int y) {
        for (int i = devices.size() - 1; i >= 0; i--) {
            if (devices.get(i).contains(x, y)) {
                return devices.get(i);
            }
        }
        return null;
    }

    public Link findLinkNear(int x, int y) {
        for (Link link : links) {
            if (link.isNear(x, y)) {
                return link;
            }
        }
        return null;
    }

    public Link findLink(Device a, Device b) {
        for (Link link : links) {
            if (link.connects(a, b)) {
                return link;
            }
        }
        return null;
    }

    public int countLinks(Device device) {
        int count = 0;
        for (Link link : links) {
            if (link.involves(device)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Builds the adjacency list used by BFS and Dijkstra:
     * every device maps to the list of cables plugged into it.
     */
    public Map<Device, List<Link>> buildAdjacencyList() {
        Map<Device, List<Link>> graph = new LinkedHashMap<>();
        for (Device device : devices) {
            graph.put(device, new ArrayList<>());
        }
        for (Link link : links) {
            graph.get(link.getDeviceA()).add(link);
            graph.get(link.getDeviceB()).add(link);
        }
        return graph;
    }

    /** Checks every cabling rule. Throws IllegalArgumentException explaining what is wrong. */
    public void validateNewLink(Device a, Device b) {
        if (a == b) {
            throw new IllegalArgumentException("A device cannot be connected to itself.");
        }
        if (findLink(a, b) != null) {
            throw new IllegalArgumentException(a.getName() + " and " + b.getName() + " are already connected.");
        }
        if (!a.canConnectTo(b)) {
            String pair = a.getType() == b.getType()
                    ? "Two " + a.getType() + "s"
                    : "A " + a.getType() + " and a " + b.getType();
            throw new IllegalArgumentException(pair
                    + " cannot be plugged directly into each other.\nConnect them through a switch or router.");
        }
        for (Device device : new Device[]{a, b}) {
            if (countLinks(device) >= device.getMaxConnections()) {
                throw new IllegalArgumentException(device.getName() + " already uses all of its "
                        + device.getMaxConnections() + " port(s).");
            }
        }
    }

    public boolean isIpInUse(String ip, Device ignore) {
        for (Device device : devices) {
            if (device != ignore && device.getIpAddress().equals(ip.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean isNameInUse(String deviceName, Device ignore) {
        for (Device device : devices) {
            if (device != ignore && device.getName().equalsIgnoreCase(deviceName.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Suggests the next free name, e.g. R4 if R1-R3 already exist. */
    public String suggestName(DeviceType type) {
        int number = 1;
        while (isNameInUse(type.getNamePrefix() + number, null)) {
            number++;
        }
        return type.getNamePrefix() + number;
    }

    /** Suggests the next free address in 192.168.1.x */
    public String suggestIp() {
        for (int i = 1; i < 255; i++) {
            String ip = "192.168.1." + i;
            if (!isIpInUse(ip, null)) {
                return ip;
            }
        }
        return "10.0.0.1";
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Network name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCreatedById() {
        return createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }
}
