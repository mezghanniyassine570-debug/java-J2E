package com.aether.engine;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Encapsulates a simulation state checkpoint for baseline vs What-If scenario comparisons.
 */
public record SimulationSnapshot(
        String scenarioName,
        LocalDateTime timestamp,
        long tick,
        double averageCongestionRatio,
        double averageSpeedKmH,
        String levelOfService,
        double averageSatisfaction,
        double employmentRate,
        double grossCityProduct,
        int activeVehicles,
        int activeCitizens,
        int activeBusinesses,
        int totalReroutes,
        Map<String, Object> extraMetadata
) {}
