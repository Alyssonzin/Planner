package com.rocketseat.planner.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivitieRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByTripId(UUID tripId);
}
