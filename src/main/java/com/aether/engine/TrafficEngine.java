package com.aether.engine;

import com.aether.model.city.CityGraph;
import com.aether.model.city.RoadSegment;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MOD-04 TrafficEngine.
 * Computes road network usage, congestion indices, and Level of Service (LOS).
 */
@Component
public class TrafficEngine {

    private final Map<String, Long> lastAlertTimestamps = new ConcurrentHashMap<>();
    private volatile double averageCongestionRatio = 0.0;
    private volatile double averageNetworkSpeedKmH = 45.0;
    private volatile String levelOfService = "A";

    public void updateTrafficMetrics(CityGraph graph, EventEngine eventEngine) {
        if (graph == null) return;

        Collection<RoadSegment> roads = graph.getAllRoads();
        if (roads.isEmpty()) return;

        double totalCongestion = 0.0;
        double totalSpeed = 0.0;
        int activeRoads = 0;

        long now = System.currentTimeMillis();

        for (RoadSegment road : roads) {
            double ratio = road.getCongestionRatio();
            totalCongestion += ratio;
            totalSpeed += road.getEffectiveSpeedKmH();
            activeRoads++;

            // Alert for extreme congestion (> 85% capacity) every 30 seconds max
            if (ratio >= 0.85 && !road.isClosed()) {
                Long lastAlert = lastAlertTimestamps.get(road.getId());
                if (lastAlert == null || (now - lastAlert) > 30_000) {
                    lastAlertTimestamps.put(road.getId(), now);
                    eventEngine.emit("TRAFFIC_JAM",
                            String.format("Heavy congestion on %s (Load: %.0f%%, Speed: %.1f km/h)",
                                    road.getName(), ratio * 100, road.getEffectiveSpeedKmH()),
                            "WARNING");
                }
            }
        }

        this.averageCongestionRatio = totalCongestion / Math.max(1, activeRoads);
        this.averageNetworkSpeedKmH = totalSpeed / Math.max(1, activeRoads);
        this.levelOfService = calculateLOS(averageCongestionRatio);
    }

    private String calculateLOS(double congestion) {
        if (congestion < 0.30) return "A (Free Flow)";
        if (congestion < 0.50) return "B (Reasonably Free Flow)";
        if (congestion < 0.70) return "C (Stable Flow)";
        if (congestion < 0.85) return "D (Approaching Unstable)";
        if (congestion < 1.00) return "E (Unstable / Bottleneck)";
        return "F (Forced Breakdown / Gridlock)";
    }

    public double getAverageCongestionRatio() {
        return averageCongestionRatio;
    }

    public double getAverageNetworkSpeedKmH() {
        return averageNetworkSpeedKmH;
    }

    public String getLevelOfService() {
        return levelOfService;
    }

    public double getCongestionPenaltyMultiplier() {
        return 1.0 + Math.pow(averageCongestionRatio, 1.8);
    }
}
