package com.aether.repository;

import com.aether.model.SimulationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationEventRepository extends JpaRepository<SimulationEvent, Long> {
    List<SimulationEvent> findTop50ByOrderByTimestampDesc();
}
