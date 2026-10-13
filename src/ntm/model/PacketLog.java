package ntm.model;

import java.sql.Timestamp;

/**
 * One row of the packet history (the packet_logs table).
 */
public class PacketLog {

    private int id;
    private final int networkId;
    private final int sourceId;
    private final int destinationId;
    private String sourceName;
    private String destinationName;
    private final String path;
    private final String algorithm;
    private final int hops;
    private final int totalLatency;
    private final String status;
    private Timestamp sentAt;

    /** For a new log entry that is about to be saved. */
    public PacketLog(int networkId, int sourceId, int destinationId, String path, String algorithm,
                     int hops, int totalLatency, String status) {
        this.networkId = networkId;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.path = path;
        this.algorithm = algorithm;
        this.hops = hops;
        this.totalLatency = totalLatency;
        this.status = status;
    }

    /** For a log entry read back from the database. */
    public PacketLog(int id, int networkId, int sourceId, int destinationId, String sourceName, String destinationName,
                     String path, String algorithm, int hops, int totalLatency, String status, Timestamp sentAt) {
        this(networkId, sourceId, destinationId, path, algorithm, hops, totalLatency, status);
        this.id = id;
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.sentAt = sentAt;
    }

    public int getId() {
        return id;
    }

    public int getNetworkId() {
        return networkId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getPath() {
        return path;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getHops() {
        return hops;
    }

    public int getTotalLatency() {
        return totalLatency;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }
}
