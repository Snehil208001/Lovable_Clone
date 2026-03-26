package com.snehil.project.lovable_clone.repository;

import com.snehil.project.lovable_clone.entity.Plan;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan,Long> {
    Optional<Plan> findByStripePriceId(String id);
}
