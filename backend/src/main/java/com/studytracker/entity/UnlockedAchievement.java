package com.studytracker.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;

@MongoEntity(collection = "achievements")
public class UnlockedAchievement {

    public ObjectId id;
    public String code;
    public Instant unlockedAt;

}
