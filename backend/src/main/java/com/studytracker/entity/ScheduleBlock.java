package com.studytracker.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.DayOfWeek;

/**
 * Un bloque del plan semanal: "los lunes estudio 60 min de Java a las 19:00".
 */
@MongoEntity(collection = "schedule")
public class ScheduleBlock {

    public ObjectId id;
    public String subjectId;
    public DayOfWeek dayOfWeek;
    /** Hora de inicio opcional en formato HH:mm. */
    public String startTime;
    public int plannedMinutes;

}
