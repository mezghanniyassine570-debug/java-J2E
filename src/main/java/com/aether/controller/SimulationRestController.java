package com.aether.controller;

import com.aether.engine.SimulationEngine;
import com.aether.engine.SimulationSnapshot;
import com.aether.engine.WhatIfEngine;
import com.aether.model.Inspectable;
import com.aether.model.city.CityGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API Controller for urban simulation state, canvas topology,
 * real-time inspection, and What-If scenario evaluations.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationRestController {

    private final SimulationEngine simulationEngine;

    @Autowired
    public SimulationRestController(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    @GetMapping("/state")
    public Map<String, Object> getSimulationState() {
        return simulationEngine.getSimulationStatePacket();
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        simulationEngine.start();
        return ResponseEntity.ok(simulationEngine.getSimulationStatePacket());
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pause() {
        simulationEngine.pause();
        return ResponseEntity.ok(simulationEngine.getSimulationStatePacket());
    }

    @PostMapping("/step")
    public ResponseEntity<Map<String, Object>> step() {
        simulationEngine.step();
        return ResponseEntity.ok(simulationEngine.getSimulationStatePacket());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        simulationEngine.reset();
        return ResponseEntity.ok(simulationEngine.getSimulationStatePacket());
    }

    @PostMapping("/speed/{multiplier}")
    public ResponseEntity<Map<String, Object>> setSpeed(@PathVariable int multiplier) {
        simulationEngine.setSpeed(multiplier);
        return ResponseEntity.ok(Map.of("speedMultiplier", multiplier));
    }

    @GetMapping("/inspect/{id}")
    public ResponseEntity<?> inspectEntity(@PathVariable String id) {
        Inspectable entity = simulationEngine.inspect(id);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getInspectableId());
        response.put("name", entity.getInspectableName());
        response.put("type", entity.getInspectableType());
        response.put("details", entity.getInspectionDetails());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/city-graph")
    public Map<String, Object> getCityGraph() {
        CityGraph graph = simulationEngine.getCityGraph();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nodes", graph.getAllNodes());
        map.put("roads", graph.getAllRoads());
        map.put("buildings", graph.getAllBuildings());
        map.put("neighborhoods", graph.getAllNeighborhoods());
        return map;
    }

    @PostMapping("/whatif/apply")
    public ResponseEntity<Map<String, Object>> applyWhatIf(@RequestParam String scenario) {
        simulationEngine.getWhatIfEngine().applyScenario(
                scenario,
                simulationEngine.getCityGraph(),
                simulationEngine.getAgentEngine(),
                simulationEngine.getEconomyEngine(),
                simulationEngine.getEventEngine()
        );
        simulationEngine.step();
        return ResponseEntity.ok(Map.of("activeScenario", scenario, "status", "APPLIED"));
    }

    @GetMapping("/whatif/compare")
    public ResponseEntity<?> compareScenarios() {
        WhatIfEngine whatIfEngine = simulationEngine.getWhatIfEngine();
        SimulationSnapshot baseline = whatIfEngine.getBaselineSnapshot();
        SimulationSnapshot current = whatIfEngine.takeSnapshot(
                whatIfEngine.getActiveScenario(),
                simulationEngine.getClock(),
                simulationEngine.getTrafficEngine(),
                simulationEngine.getEconomyEngine(),
                simulationEngine.getAgentEngine()
        );

        WhatIfEngine.ScenarioComparison comparison = whatIfEngine.evaluateComparison(baseline, current);
        if (comparison == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Baseline snapshot not available"));
        }
        return ResponseEntity.ok(comparison);
    }
}
