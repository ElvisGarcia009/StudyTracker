package com.studytracker.repository;

import com.studytracker.entity.Goal;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GoalRepository implements PanacheMongoRepository<Goal> {}
