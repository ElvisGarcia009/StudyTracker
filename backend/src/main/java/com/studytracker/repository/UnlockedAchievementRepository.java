package com.studytracker.repository;

import com.studytracker.entity.UnlockedAchievement;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UnlockedAchievementRepository implements PanacheMongoRepository<UnlockedAchievement> {}
