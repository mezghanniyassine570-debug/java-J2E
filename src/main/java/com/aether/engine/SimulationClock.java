package com.aether.engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the virtual temporal progress of the urban simulation.
 * Translates simulation ticks into virtual days, hours, and minutes.
 */
public class SimulationClock {

    // 1 tick = 1 virtual minute
    private final AtomicLong currentTick = new AtomicLong(480); // Start at 08:00 AM (8 * 60)
    private volatile int speedMultiplier = 1; // 1x, 2x, 5x, 10x

    public long getTick() {
        return currentTick.get();
    }

    public void advanceTick() {
        currentTick.incrementAndGet();
    }

    public void reset() {
        currentTick.set(480); // Reset to Day 1, 08:00 AM
    }

    public int getDay() {
        return (int) (currentTick.get() / (24 * 60)) + 1;
    }

    public int getHour() {
        return (int) ((currentTick.get() % (24 * 60)) / 60);
    }

    public int getMinute() {
        return (int) (currentTick.get() % 60);
    }

    public String getFormattedTime() {
        return String.format("Day %d, %02d:%02d", getDay(), getHour(), getMinute());
    }

    public String getTimePeriodLabel() {
        int h = getHour();
        if (h >= 6 && h < 9) return "Morning Rush Hour";
        if (h >= 9 && h < 12) return "Morning Business Hours";
        if (h >= 12 && h < 14) return "Midday Lunch Rush";
        if (h >= 14 && h < 17) return "Afternoon Productive";
        if (h >= 17 && h < 20) return "Evening Commute & Leisure";
        if (h >= 20 && h < 23) return "Night Life & Entertainment";
        return "Overnight Resting";
    }

    public int getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(int speedMultiplier) {
        this.speedMultiplier = Math.max(1, Math.min(10, speedMultiplier));
    }
}
