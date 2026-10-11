package com.aether.engine;

import com.aether.model.city.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class WhatIfEngineTest {

    private WhatIfEngine whatIfEngine;
    private CityManager cityManager;
    private AgentEngine agentEngine;
    private EconomyEngine economyEngine;
    private EventEngine eventEngine;

    @BeforeEach
    void setUp() {
        whatIfEngine = new WhatIfEngine();
        cityManager = new CityManager();
        agentEngine = new AgentEngine();
        economyEngine = new EconomyEngine();
        eventEngine = new EventEngine(null);

        cityManager.initializeNeoMetropolisTopology();
        agentEngine.populateAgents(cityManager.getCityGraph());
    }

    @Test
    void testApplyRoadClosureScenario() {
        whatIfEngine.applyScenario("ROAD_CLOSURE", cityManager.getCityGraph(), agentEngine, economyEngine, eventEngine);

        RoadSegment fwd = cityManager.getCityGraph().getRoad("RD-DOWNTOWN-CENTRAL-FWD");
        assertNotNull(fwd);
        assertTrue(fwd.isClosed(), "Road RD-DOWNTOWN-CENTRAL-FWD should be marked closed");
        assertEquals("ROAD_CLOSURE", whatIfEngine.getActiveScenario());
    }

    @Test
    void testResetToBaselineRestoresRoads() {
        whatIfEngine.applyScenario("ROAD_CLOSURE", cityManager.getCityGraph(), agentEngine, economyEngine, eventEngine);
        whatIfEngine.resetToBaseline(cityManager.getCityGraph(), agentEngine, economyEngine, eventEngine);

        RoadSegment fwd = cityManager.getCityGraph().getRoad("RD-DOWNTOWN-CENTRAL-FWD");
        assertNotNull(fwd);
        assertFalse(fwd.isClosed(), "Road should be re-opened after reset");
        assertEquals("BASELINE", whatIfEngine.getActiveScenario());
    }

    @Test
    void testScenarioComparisonCalculation() {
        SimulationClock clock = new SimulationClock();
        TrafficEngine trafficEngine = new TrafficEngine();

        SimulationSnapshot baseline = whatIfEngine.takeSnapshot("BASELINE", clock, trafficEngine, economyEngine, agentEngine);
        SimulationSnapshot scenario = new SimulationSnapshot(
                "ROAD_CLOSURE",
                java.time.LocalDateTime.now(),
                500,
                0.75, // Higher congestion
                25.0, // Lower speed
                "D",
                70.0, // Lower satisfaction
                90.0,
                1500000.0,
                100,
                300,
                30,
                12, // 12 reroutes
                java.util.Map.of()
        );

        WhatIfEngine.ScenarioComparison comparison = whatIfEngine.evaluateComparison(baseline, scenario);
        assertNotNull(comparison);
        assertEquals("ROAD_CLOSURE", comparison.scenarioKey());
        assertFalse(comparison.metrics().isEmpty());
        assertNotNull(comparison.aiRecommendation());
    }
}
