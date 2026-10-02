package com.studytracker.dto;

import com.studytracker.entity.ScheduleBlock;

import java.time.DayOfWeek;

public record ScheduleBlockResponse(
        String id,
        String subjectId,
        DayOfWeek dayOfWeek,
        String startTime,
        int plannedMinutes
) {
    public static ScheduleBlockResponse from(ScheduleBlock b) {
        return new ScheduleBlockResponse(b.id.toHexString(), b.subjectId, b.dayOfWeek, b.startTime, b.plannedMinutes);
    }
}
