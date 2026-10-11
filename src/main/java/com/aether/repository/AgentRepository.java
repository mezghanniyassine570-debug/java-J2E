package com.aether.repository;

import com.aether.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByCityId(Long cityId);
    List<Agent> findByType(Agent.AgentType type);
}
