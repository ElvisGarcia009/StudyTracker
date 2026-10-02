package com.studytracker.dto;

import com.studytracker.entity.SessionMode;
import com.studytracker.entity.SessionStatus;
import com.studytracker.entity.StudySession;

import java.time.Instant;

/**
 * serverTime es la hora del servidor al responder, para que el navegador
 * pueda corregir si su reloj está desfasado.
 */
public record SessionResponse(
        String id,
        String subjectId,
        SessionStatus status,
        SessionMode mode,
        Instant startedAt,
        Instant endedAt,
        long elapsedSeconds,
        int workMinutes,
        int breakMinutes,
        int longBreakMinutes,
        int pomodorosCompleted,
        long lastPomodoroAtSeconds,
        Instant breakEndsAt,
        String notes,
        Integer focusRating,
        Instant serverTime
) {
    public static SessionResponse from(StudySession s, Instant now) {
        return new SessionResponse(
                s.id.toHexString(), s.subjectId, s.status, s.mode, s.startedAt, s.endedAt,
                s.elapsedSeconds(now), s.workMinutes, s.breakMinutes, s.longBreakMinutes,
                s.pomodorosCompleted, s.lastPomodoroAtSeconds, s.breakEndsAt,
                s.notes, s.focusRating, now);
    }
}
