package com.studytracker.achievement;

/** Resumen del historial de estudio con todo lo que necesitan las condiciones de los logros. */
public record AchievementContext(
        long sessionCount,
        long totalSeconds,
        int bestStreak,
        int maxPomodorosInDay,
        long longestSessionSeconds,
        boolean hasEarlySession,
        boolean hasLateSession,
        boolean weeklyGoalMet
) {}
