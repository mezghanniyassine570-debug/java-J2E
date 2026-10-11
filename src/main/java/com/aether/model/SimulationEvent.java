package com.aether.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_events")
public class SimulationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String eventType; // TRAFFIC_JAM, POWER_OUTAGE, WHAT_IF_SCENARIO, ECONOMIC_SHIFT

    private String description;

    private String severity; // INFO, WARNING, CRITICAL

    public SimulationEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public SimulationEvent(String eventType, String description, String severity) {
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.description = description;
        this.severity = severity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
