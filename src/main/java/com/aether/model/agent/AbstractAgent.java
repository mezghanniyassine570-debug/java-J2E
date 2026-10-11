package com.aether.model.agent;

import com.aether.engine.SimulationContext;
import com.aether.model.Agent.AgentType;
import com.aether.model.Inspectable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Appendix B Conceptual UML - AbstractAgent base class.
 * Foundation for all autonomous agents in AETHER Lite.
 */
public abstract class AbstractAgent implements Inspectable {

    protected final String id;
    protected String name;
    protected final AgentType type;
    protected volatile double posX;
    protected volatile double posY;
    protected volatile String status;
    protected volatile double satisfaction; // 0.0 - 100.0
    protected volatile boolean active = true;

    public AbstractAgent(String id, String name, AgentType type, double posX, double posY, String status, double satisfaction) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.posX = posX;
        this.posY = posY;
        this.status = status;
        this.satisfaction = Math.max(0.0, Math.min(100.0, satisfaction));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AgentType getType() { return type; }
    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }
    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getSatisfaction() { return satisfaction; }
    public void setSatisfaction(double satisfaction) {
        this.satisfaction = Math.max(0.0, Math.min(100.0, satisfaction));
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Executes one autonomous simulation cycle for this agent.
     */
    public abstract void step(SimulationContext context);

    @Override
    public String getInspectableId() { return id; }

    @Override
    public String getInspectableName() { return name; }

    @Override
    public String getInspectableType() { return type.name(); }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Agent ID", id);
        details.put("Name", name);
        details.put("Type", type.name());
        details.put("Status", status);
        details.put("Satisfaction", String.format("%.1f%%", satisfaction));
        details.put("Coordinates", String.format("(%.1f, %.1f)", posX, posY));
        details.put("Active", active);
        return details;
    }
}
