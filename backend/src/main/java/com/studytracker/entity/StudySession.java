package com.studytracker.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Duration;
import java.time.Instant;

/**
 * Una sesión de estudio. El timer vive aquí (y no en el navegador) para que
 * sobreviva a recargas o a cerrar la pestaña.
 */
@MongoEntity(collection = "sessions")
public class StudySession {

    public ObjectId id;
    public String subjectId;
    public SessionStatus status;
    public SessionMode mode;

    public Instant startedAt;
    public Instant endedAt;

    /** Segundos estudiados antes de la última reanudación. */
    public long accumulatedSeconds;
    /** Momento en que el timer se puso a correr por última vez (null si está pausado). */
    public Instant lastResumedAt;

    // Pomodoro
    public int workMinutes;
    public int breakMinutes;
    public int longBreakMinutes;
    public int pomodorosCompleted;
    /** Segundos totales en los que se completó el último pomodoro. */
    public long lastPomodoroAtSeconds;
    /** Si está en descanso, cuándo termina. */
    public Instant breakEndsAt;

    public String notes;
    public Integer focusRating;

    /** Segundos efectivos de estudio, incluyendo el tramo que está corriendo ahora. */
    public long elapsedSeconds(Instant now) {
        if (status == SessionStatus.RUNNING && lastResumedAt != null) {
            return accumulatedSeconds + Math.max(0, Duration.between(lastResumedAt, now).getSeconds());
        }
        return accumulatedSeconds;
    }

}
