package com.aether.engine;

import com.aether.model.SimulationEvent;
import com.aether.repository.SimulationEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * MOD-06 EventEngine.
 * Generates, distributes, filters, and stores simulation events.
 */
@Component
public class EventEngine {

    private final SimulationEventRepository eventRepository;
    private final Deque<SimulationEvent> liveEvents = new ConcurrentLinkedDeque<>();
    private final List<Consumer<SimulationEvent>> eventListeners = new CopyOnWriteArrayList<>();
    private static final int MAX_BUFFERED_EVENTS = 200;

    public EventEngine(SimulationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void emit(String eventType, String description, String severity) {
        SimulationEvent event = new SimulationEvent(eventType, description, severity);
        event.setTimestamp(LocalDateTime.now());

        // Keep in fast live memory buffer
        liveEvents.addFirst(event);
        while (liveEvents.size() > MAX_BUFFERED_EVENTS) {
            liveEvents.removeLast();
        }

        // Notify active WebSocket listeners
        for (Consumer<SimulationEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception ignored) {
            }
        }

        // Persist to database asynchronously or safely
        try {
            if (eventRepository != null && ("CRITICAL".equalsIgnoreCase(severity) || "WARNING".equalsIgnoreCase(severity))) {
                eventRepository.save(event);
            }
        } catch (Exception ignored) {
        }
    }

    public List<SimulationEvent> getRecentEvents(int limit) {
        List<SimulationEvent> list = new ArrayList<>();
        int count = 0;
        for (SimulationEvent e : liveEvents) {
            list.add(e);
            count++;
            if (count >= limit) break;
        }
        return list;
    }

    public List<SimulationEvent> filterEvents(String severity) {
        if (severity == null || severity.isBlank() || "ALL".equalsIgnoreCase(severity)) {
            return getRecentEvents(50);
        }
        List<SimulationEvent> filtered = new ArrayList<>();
        for (SimulationEvent e : liveEvents) {
            if (severity.equalsIgnoreCase(e.getSeverity())) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    public void registerListener(Consumer<SimulationEvent> listener) {
        eventListeners.add(listener);
    }

    public void unregisterListener(Consumer<SimulationEvent> listener) {
        eventListeners.remove(listener);
    }

    public void clear() {
        liveEvents.clear();
    }
}
