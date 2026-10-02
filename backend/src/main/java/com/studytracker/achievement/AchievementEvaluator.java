package com.studytracker.achievement;

import com.studytracker.entity.StudySession;
import com.studytracker.service.StreakCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Lógica pura (sin base de datos) para decidir qué logros se cumplen. */
public final class AchievementEvaluator {

    static final long MIN_SECONDS_FOR_TIME_OF_DAY = 15 * 60;

    private AchievementEvaluator() {}

    public static AchievementContext buildContext(List<StudySession> finished, ZoneId zone, boolean weeklyGoalMet) {
        long totalSeconds = 0;
        long longest = 0;
        boolean early = false;
        boolean late = false;
        Map<LocalDate, Integer> pomodorosByDay = new HashMap<>();

        for (StudySession s : finished) {
            long seconds = s.accumulatedSeconds;
            totalSeconds += seconds;
            longest = Math.max(longest, seconds);

            LocalDateTime start = LocalDateTime.ofInstant(s.startedAt, zone);
            pomodorosByDay.merge(start.toLocalDate(), s.pomodorosCompleted, Integer::sum);

            if (seconds >= MIN_SECONDS_FOR_TIME_OF_DAY) {
                int hour = start.getHour();
                if (hour >= 4 && hour < 7) early = true;
                if (hour >= 23 || hour < 4) late = true;
            }
        }

        Set<LocalDate> studiedDays = finished.stream()
                .map(s -> LocalDateTime.ofInstant(s.startedAt, zone).toLocalDate())
                .collect(Collectors.toSet());

        int maxPomodoros = pomodorosByDay.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        return new AchievementContext(
                finished.size(),
                totalSeconds,
                StreakCalculator.bestStreak(studiedDays),
                maxPomodoros,
                longest,
                early,
                late,
                weeklyGoalMet);
    }

    public static Set<Achievement> evaluate(AchievementContext context) {
        return Arrays.stream(Achievement.values())
                .filter(a -> a.isMet(context))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Achievement.class)));
    }
}
