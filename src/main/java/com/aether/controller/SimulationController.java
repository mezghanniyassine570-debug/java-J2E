package com.aether.controller;

import com.aether.engine.SimulationEngine;
import com.aether.model.City;
import com.aether.service.CityService;
import com.aether.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class SimulationController {

    private final SimulationService simulationService;
    private final CityService cityService;

    @Autowired
    public SimulationController(SimulationService simulationService, CityService cityService) {
        this.simulationService = simulationService;
        this.cityService = cityService;
    }

    @GetMapping
    public String index(Model model) {
        City defaultCity = cityService.initDefaultCity();
        SimulationEngine engine = simulationService.getEngine();

        model.addAttribute("city", defaultCity);
        model.addAttribute("isRunning", engine.isRunning());
        model.addAttribute("events", simulationService.getRecentEvents());
        model.addAttribute("clock", engine.getClock());
        model.addAttribute("engine", engine);
        model.addAttribute("traffic", engine.getTrafficEngine());
        model.addAttribute("economy", engine.getEconomyEngine());
        model.addAttribute("activeScenario", engine.getWhatIfEngine().getActiveScenario());

        return "simulation/cockpit";
    }

    @PostMapping("/simulation/start")
    public String start() {
        simulationService.startSimulation();
        return "redirect:/";
    }

    @PostMapping("/simulation/pause")
    public String pause() {
        simulationService.pauseSimulation();
        return "redirect:/";
    }

    @PostMapping("/simulation/reset")
    public String reset() {
        simulationService.resetSimulation();
        return "redirect:/";
    }

    @PostMapping("/simulation/step")
    public String step() {
        simulationService.stepSimulation();
        return "redirect:/";
    }

    @PostMapping("/simulation/speed")
    public String speed(@RequestParam int multiplier) {
        simulationService.setSpeed(multiplier);
        return "redirect:/";
    }

    @PostMapping("/simulation/whatif")
    public String triggerWhatIf(@RequestParam String scenario) {
        simulationService.triggerWhatIfScenario(scenario);
        return "redirect:/";
    }
}
