package com.studytracker.dto;

import java.time.LocalDate;
import java.util.List;

/** Respuestas de estadísticas, agrupadas en un solo archivo porque son pequeñas. */
public final class StatsDtos {

    private StatsDtos() {}

    public record DailyStat(LocalDate date, long minutes, long plannedMinutes) {}

    public record SubjectStat(String subjectId, String name, String color, long minutes, long goalMinutes) {}

    public record HeatmapDay(LocalDate date, long minutes) {}

    public record Dashboard(
            long todayMinutes,
            long todayPlannedMinutes,
            long weekMinutes,
            long weekPlannedMinutes,
            long weekGoalMinutes,
            int currentStreak,
            int bestStreak,
            long totalMinutes,
            long totalSessions,
            Double averageFocus,
            List<DailyStat> last7Days,
            List<DailyStat> last30Days,
            List<SubjectStat> weekBySubject,
            List<SubjectStat> allTimeBySubject
    ) {}
}
