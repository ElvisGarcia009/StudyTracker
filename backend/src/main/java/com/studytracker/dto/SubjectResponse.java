package com.studytracker.dto;

import com.studytracker.entity.Subject;

import java.time.Instant;

public record SubjectResponse(
        String id,
        String name,
        String color,
        int weeklyGoalMinutes,
        boolean archived,
        Instant createdAt
) {
    public static SubjectResponse from(Subject s) {
        return new SubjectResponse(s.id.toHexString(), s.name, s.color, s.weeklyGoalMinutes, s.archived, s.createdAt);
    }
}
