package com.studytracker.repository;

import com.studytracker.entity.ScheduleBlock;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ScheduleBlockRepository implements PanacheMongoRepository<ScheduleBlock> {}
