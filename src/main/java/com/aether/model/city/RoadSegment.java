package com.aether.model.city;

import com.aether.model.Inspectable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a directional or bidirectional road segment between two city nodes.
 * Used in MOD-03 RoutingEngine and MOD-04 TrafficEngine.
 */
public class RoadSegment implements Inspectable {

    private final String id;
    private final String name;
    private final String fromNodeId;
    private final String toNodeId;
    private final double lengthMeters;
    private final double speedLimitKmH;
    private final int capacity;
    private final int lanes;
    private final AtomicInteger currentVehicles = new AtomicInteger(0);
    private volatile boolean isClosed = false;

    public RoadSegment(String id, String name, String fromNodeId, String toNodeId,
                       double lengthMeters, double speedLimitKmH, int capacity, int lanes) {
        this.id = id;
        this.name = name;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.lengthMeters = lengthMeters;
        this.speedLimitKmH = speedLimitKmH;
        this.capacity = capacity;
        this.lanes = lanes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public double getLengthMeters() { return lengthMeters; }
    public double getSpeedLimitKmH() { return speedLimitKmH; }
    public int getCapacity() { return capacity; }
    public int getLanes() { return lanes; }

    public int getCurrentVehicles() {
        return currentVehicles.get();
    }

    public void incrementVehicles() {
        currentVehicles.incrementAndGet();
    }

    public void decrementVehicles() {
        currentVehicles.updateAndGet(v -> Math.max(0, v - 1));
    }

    public void resetVehicles() {
        currentVehicles.set(0);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    /**
     * Congestion ratio (0.0 to >1.0).
     */
    public double getCongestionRatio() {
        if (capacity <= 0) return 0.0;
        return (double) currentVehicles.get() / capacity;
    }

    /**
     * Effective vehicle speed given current congestion.
     * Slows down progressively as capacity is exceeded.
     */
    public double getEffectiveSpeedKmH() {
        if (isClosed) return 0.0;
        double ratio = getCongestionRatio();
        // Speed drops as congestion ratio grows, down to 10% of speed limit
        double reduction = 1.0 / (1.0 + Math.pow(ratio, 2.5));
        return Math.max(5.0, speedLimitKmH * reduction);
    }

    /**
     * Travel time cost in seconds, used for A* dynamic edge weight.
     */
    public double getTravelTimeSeconds() {
        if (isClosed) return Double.POSITIVE_INFINITY;
        double speedMps = (getEffectiveSpeedKmH() * 1000.0) / 3600.0;
        return lengthMeters / speedMps;
    }

    @Override
    public String getInspectableId() { return id; }

    @Override
    public String getInspectableName() { return name; }

    @Override
    public String getInspectableType() { return "ROAD_SEGMENT"; }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Road ID", id);
        details.put("Name", name);
        details.put("Connection", fromNodeId + " ➔ " + toNodeId);
        details.put("Length (m)", String.format("%.0f m", lengthMeters));
        details.put("Speed Limit", speedLimitKmH + " km/h");
        details.put("Effective Speed", String.format("%.1f km/h", getEffectiveSpeedKmH()));
        details.put("Active Vehicles", getCurrentVehicles());
        details.put("Capacity", capacity);
        details.put("Congestion Ratio", String.format("%.1f%%", getCongestionRatio() * 100));
        details.put("Status", isClosed ? "⛔ CLOSED (What-If Scenario)" : "OPEN");
        return details;
    }
}
