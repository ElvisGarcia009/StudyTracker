package com.studytracker.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.LocalDate;

@MongoEntity(collection = "goals")
public class Goal {

    public ObjectId id;
    public String title;
    /** null = cuenta todas las materias. */
    public String subjectId;
    public int targetMinutes;
    public GoalPeriod period;
    public LocalDate deadline;
    public Instant createdAt;

}
