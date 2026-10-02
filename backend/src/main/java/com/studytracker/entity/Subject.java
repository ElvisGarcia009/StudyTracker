package com.studytracker.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;

@MongoEntity(collection = "subjects")
public class Subject {

    public ObjectId id;
    public String name;
    public String color;
    public int weeklyGoalMinutes;
    public boolean archived;
    public Instant createdAt;

}
